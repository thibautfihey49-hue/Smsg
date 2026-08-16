package com.smsg.ui.screens
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.smsg.data.model.Message
import com.smsg.data.repository.AttachmentRepository
import com.smsg.data.repository.EphemeralRepository
import com.smsg.data.repository.SmsRepository
import com.smsg.ui.theme.GmBlue
import com.smsg.ui.theme.GmBubbleMe
import com.smsg.ui.theme.GmBubbleOther
import com.smsg.ui.theme.GmBg
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MessageDetailScreen(threadId: Long, address: String, onBack: () -> Unit, onAddContact: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SmsRepository(ctx) }
    val attachRepo = remember { AttachmentRepository(ctx) }
    val ephRepo = remember { EphemeralRepository(ctx) }
    var msgs by remember { mutableStateOf<List<Message>>(emptyList()) }
    var localFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf(address) }
    var realThreadId by remember { mutableStateOf(threadId) }
    var showAttach by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentPlaying by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showEphDialog by remember { mutableStateOf(false) }
    var isEphEnabled by remember { mutableStateOf(false) }
    var ephDuration by remember { mutableStateOf(24*60*60*1000L) }
    val scope = rememberCoroutineScope()
    val recordPerm = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val imagePerm = if (Build.VERSION.SDK_INT >= 33) rememberPermissionState(android.Manifest.permission.READ_MEDIA_IMAGES) else rememberPermissionState(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                if (!imagePerm.status.isGranted) { imagePerm.launchPermissionRequest(); return@let }
                val f = attachRepo.copyToInternal(it, "${realThreadId}_img", "jpg")
                localFiles = localFiles + f
                Toast.makeText(ctx, "Image ajoutée", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(ctx, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    suspend fun refresh() {
        realThreadId = if (threadId==0L) repo.getOrCreateThreadId(address) else threadId
        displayName = repo.getContactName(address)?: address
        isEphEnabled = ephRepo.isEphemeralEnabled(address)
        ephDuration = ephRepo.getDurationMs(address)
        var allMsgs = repo.getMessagesForThread(realThreadId, address)
        // FILTRE EPHEMERE : si activé et contact papa, supprime les vieux
        if (ephRepo.isPapaContact(address, displayName) && isEphEnabled) {
            val cutoff = System.currentTimeMillis() - ephDuration
            val toDelete = allMsgs.filter { it.date < cutoff }
            toDelete.forEach { m ->
                try { ctx.contentResolver.delete(Uri.parse("content://sms/${m.id}"), null, null) } catch (e: Exception) {}
            }
            allMsgs = allMsgs.filter { it.date >= cutoff }
        }
        msgs = allMsgs
        localFiles = ctx.filesDir.listFiles()?.filter { it.name.startsWith(realThreadId.toString()) }?.sortedBy { it.lastModified() }?: emptyList()
    }

    LaunchedEffect(address) { refresh() }
    DisposableEffect(address) {
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())){ override fun onChange(c: Boolean){ scope.launch{ refresh() } } }
        ctx.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, obs)
        val br = object : BroadcastReceiver(){ override fun onReceive(c: Context, i: Intent){ scope.launch{ refresh() } } }
        ctx.registerReceiver(br, IntentFilter("com.smsg.NEW_SMS"), Context.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.contentResolver.unregisterContentObserver(obs); ctx.unregisterReceiver(br); try{ player?.release() }catch(e:Exception){} }
    }

    val isPapa = ephRepo.isPapaContact(address, displayName)

    Scaffold(containerColor = GmBg,
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically){
                    Surface(Modifier.size(36.dp).clip(CircleShape), color = GmBlue){ Box(contentAlignment = Alignment.Center){ Text(displayName.firstOrNull()?.toString()?.uppercase()?: "?", color = Color.White) } }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(displayName, style = MaterialTheme.typography.titleMedium)
                        if (isPapa && isEphEnabled) Text("Messages éphémères • ${ephDuration/1000/60}min", style = MaterialTheme.typography.labelSmall, color = GmBlue)
                        else Text(if (isPapa) "Appuyez pour options Papa" else "Chiffré de bout en bout", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }, navigationIcon = { IconButton(onClick = onBack){ Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    if (isPapa) { IconButton(onClick = { showEphDialog = true }){ Icon(Icons.Default.Timer, null, tint = if (isEphEnabled) GmBlue else Color.Gray) } }
                    IconButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$address"))) }){ Icon(Icons.Default.Call, null) }
                    Box {
                        IconButton(onClick = { showMenu = true }){ Icon(Icons.Default.MoreVert, null) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (isPapa) {
                                DropdownMenuItem(text = { Text(if (isEphEnabled) "Désactiver messages éphémères" else "Activer messages éphémères") }, onClick = { showMenu=false; showEphDialog=true }, leadingIcon = { Icon(Icons.Default.Timer, null) })
                                DropdownMenuItem(text = { Text("Durée: ${ephDuration/1000/60} min") }, onClick = { showMenu=false; showEphDialog=true })
                                Divider()
                            }
                            DropdownMenuItem(text = { Text("Détails") }, onClick = { showMenu=false })
                            DropdownMenuItem(text = { Text("Bloquer") }, onClick = { showMenu=false })
                        }
                    }
                })
        },
        bottomBar = {
            Column {
                if (isPapa && isEphEnabled) {
                    Surface(color = Color(0xFFE8F0FE), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically){
                            Icon(Icons.Default.Timer, null, tint = GmBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Les messages disparaîtront après ${ephDuration/1000/60} minutes", style = MaterialTheme.typography.labelSmall, color = GmBlue)
                        }
                    }
                }
                if (showAttach) {
                    Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly){
                        FilledTonalButton(onClick = { showAttach=false; pickImage.launch("image/*") }){ Icon(Icons.Default.Image, null); Text(" Galerie") }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(8.dp).imePadding(), verticalAlignment = Alignment.Bottom){
                    Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.weight(1f)){
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)){
                            IconButton(onClick = {}){ Icon(Icons.Default.EmojiEmotions, null, tint = Color.Gray) }
                            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
                            IconButton(onClick = { showAttach=!showAttach }){ Icon(Icons.Default.AttachFile, null, tint = Color.Gray) }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = {
                        if (input.isNotBlank()){ repo.sendSms(address, input); input=""; scope.launch{ refresh() } }
                        else {
                            if (!recordPerm.status.isGranted){ recordPerm.launchPermissionRequest(); return@FilledIconButton }
                            if (!isRecording){
                                try{
                                    val tmp = File(ctx.cacheDir, "${realThreadId}_voice_${System.currentTimeMillis()}.m4a")
                                    val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else MediaRecorder()
                                    mr.setAudioSource(MediaRecorder.AudioSource.MIC); mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                    mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); mr.setAudioEncodingBitRate(128000); mr.setAudioSamplingRate(44100)
                                    mr.setOutputFile(tmp.absolutePath); mr.prepare(); mr.start()
                                    recorder=mr; isRecording=true
                                } catch (e: Exception){}
                            } else {
                                try{ recorder?.apply{ stop(); release() } } catch (e: Exception){}
                                isRecording=false
                                val last = ctx.cacheDir.listFiles()?.filter { it.name.contains("${realThreadId}_voice") }?.maxByOrNull { it.lastModified() }
                                if (last!=null && last.length()>1000){
                                    val dest = File(ctx.filesDir, "${realThreadId}_voice_${System.currentTimeMillis()}.m4a")
                                    last.copyTo(dest, true); last.delete()
                                    localFiles = localFiles + dest
                                }
                            }
                        }
                    }, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = GmBlue)){
                        Icon(if (input.isNotBlank()) Icons.Default.Send else if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null, tint = Color.White)
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(8.dp)){
            if (isPapa && isEphEnabled) {
                item {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F0FE), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)){
                        Row(Modifier.padding(12.dp)){ Icon(Icons.Default.Lock, null, tint = GmBlue); Spacer(Modifier.width(8.dp)); Text("Messages éphémères activés pour Papa. Les messages disparaîtront automatiquement.", style = MaterialTheme.typography.bodySmall, color = GmBlue) }
                    }
                }
            }
            items(msgs){ m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.isMe) Arrangement.End else Arrangement.Start){
                    Surface(shape = RoundedCornerShape(18.dp), color = if (m.isMe) GmBubbleMe else GmBubbleOther, modifier = Modifier.widthIn(max=300.dp)){
                        Column(Modifier.padding(12.dp)){
                            Text(m.body, color = Color.Black)
                            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically){
                                Text(SimpleDateFormat("HH:mm").format(Date(m.date)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                if (isPapa && isEphEnabled) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Timer, null, modifier = Modifier.size(12.dp), tint = Color.Gray) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            items(localFiles){ file ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End){
                    when {
                        file.name.contains("_img") -> {
                            val bmp = remember(file.absolutePath){ try{ BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }catch(e:Exception){ null } }
                            if (bmp!=null) Image(bmp, null, Modifier.size(240.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
                        }
                        file.name.contains("_voice") -> {
                            val isPlaying = currentPlaying==file.absolutePath
                            Surface(shape = RoundedCornerShape(18.dp), color = GmBubbleMe, modifier = Modifier.width(260.dp)){
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically){
                                    FilledIconButton(onClick = {
                                        try{
                                            if (isPlaying){ player?.stop(); player?.release(); player=null; currentPlaying=null }
                                            else { player?.release(); player=MediaPlayer().apply{ setDataSource(file.absolutePath); prepare(); start(); setOnCompletionListener{ currentPlaying=null } }; currentPlaying=file.absolutePath }
                                        } catch (e: Exception){}
                                    }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White), modifier = Modifier.size(36.dp)){
                                        Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null, tint = GmBlue)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Vocale ${file.length()/1024}KB", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (showEphDialog) {
            AlertDialog(onDismissRequest = { showEphDialog=false },
                title = { Text("Messages éphémères - Papa") },
                text = {
                    Column {
                        Text("Pour le contact Papa uniquement, les messages disparaîtront automatiquement après la durée choisie.")
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically){
                            Text("Activer"); Spacer(Modifier.width(8.dp))
                            Switch(checked = isEphEnabled, onCheckedChange = { isEphEnabled = it; ephRepo.setEphemeralEnabled(address, it); scope.launch{ refresh() } })
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Durée:", style = MaterialTheme.typography.labelMedium)
                        listOf(30*1000L to "30 sec", 5*60*1000L to "5 min", 60*60*1000L to "1 heure", 24*60*60*1000L to "24 heures").forEach { (ms, label) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically){
                                RadioButton(selected = ephDuration==ms, onClick = { ephDuration=ms; ephRepo.setDurationMs(address, ms) })
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showEphDialog=false; scope.launch{ refresh() } }){ Text("OK") } }
            )
        }
    }
}
