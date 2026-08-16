package com.smsg.ui.screens
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.smsg.data.model.ChatThemes
import com.smsg.data.model.Message
import com.smsg.data.repository.ContactsRepository
import com.smsg.data.repository.MmsRepository
import com.smsg.data.repository.SmsRepository
import com.smsg.data.repository.ThemeRepository
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun MessageDetailScreen(threadId: Long, address: String, onBack: () -> Unit, onAddContact: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SmsRepository(ctx) }
    val mmsRepo = remember { MmsRepository(ctx) }
    val contactRepo = remember { ContactsRepository(ctx) }
    val themeRepo = remember { ThemeRepository(ctx) }
    var msgs by remember { mutableStateOf<List<Message>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var contactExists by remember { mutableStateOf(true) }
    var displayName by remember { mutableStateOf(address) }
    var realThreadId by remember { mutableStateOf(threadId) }
    var theme by remember { mutableStateOf(themeRepo.getTheme(address)) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var msgToDelete by remember { mutableStateOf<Message?>(null) }
    var showDeleteConv by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()
    val recordPerm = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { scope.launch { val ok = mmsRepo.sendMms(address, it, "image/jpeg"); Toast.makeText(ctx, if (ok) "Image envoyée" else "Échec MMS", Toast.LENGTH_SHORT).show() } }
    }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { scope.launch { val ok = mmsRepo.sendMms(address, it, "video/mp4"); Toast.makeText(ctx, if (ok) "Vidéo envoyée" else "Échec MMS", Toast.LENGTH_SHORT).show() } }
    }

    suspend fun refresh() {
        realThreadId = if (threadId == 0L) repo.getOrCreateThreadId(address) else threadId
        contactExists = contactRepo.exists(address)
        displayName = repo.getContactName(address)?: address
        theme = themeRepo.getTheme(address)
        msgs = repo.getMessagesForThread(realThreadId, address)
    }
    LaunchedEffect(address) { refresh() }
    DisposableEffect(address) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean) { scope.launch { refresh() } } }
        ctx.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
        val br = object : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) { scope.launch { refresh() } } }
        ctx.registerReceiver(br, IntentFilter("com.smsg.NEW_SMS"), Context.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.contentResolver.unregisterContentObserver(observer); ctx.unregisterReceiver(br) }
    }

    if (msgToDelete != null) {
        AlertDialog(onDismissRequest = { msgToDelete = null }, title = { Text("Supprimer ?") },
            confirmButton = { TextButton(onClick = { scope.launch { repo.deleteMessage(msgToDelete!!.id); refresh(); msgToDelete = null } }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { msgToDelete = null }) { Text("Annuler") } })
    }
    if (showDeleteConv) {
        AlertDialog(onDismissRequest = { showDeleteConv = false }, title = { Text("Supprimer conversation") },
            confirmButton = { TextButton(onClick = { scope.launch { repo.deleteThread(realThreadId, address); showDeleteConv = false; onBack() } }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showDeleteConv = false }) { Text("Annuler") } })
    }
    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Thème de ${displayName}", style = MaterialTheme.typography.titleMedium)
                ChatThemes.all.forEach { t ->
                    Row(Modifier.fillMaxWidth().combinedClickable(onClick = { themeRepo.setTheme(address, t.id); theme = t; showThemeSheet = false }).padding(12.dp)) {
                        Surface(Modifier.size(24.dp), color = t.avatar, shape = RoundedCornerShape(12.dp)) {}
                        Spacer(Modifier.width(12.dp)); Text(t.name)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    if (showAttachSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachSheet = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilledTonalButton(onClick = { showAttachSheet = false; pickImage.launch("image/*") }) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("Image") }
                    FilledTonalButton(onClick = { showAttachSheet = false; pickVideo.launch("video/*") }) { Icon(Icons.Default.Videocam, null); Spacer(Modifier.width(8.dp)); Text("Vidéo") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilledTonalButton(onClick = {
                        if (!recordPerm.status.isGranted) { recordPerm.launchPermissionRequest(); return@FilledTonalButton }
                        if (!isRecording) {
                            try {
                                val f = File(ctx.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                                audioFile = f
                                val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else MediaRecorder()
                                mr.apply {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                    setOutputFile(f.absolutePath)
                                    prepare(); start()
                                }
                                recorder = mr
                                isRecording = true
                                Toast.makeText(ctx, "Enregistrement... appuie Stop pour envoyer", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) { Toast.makeText(ctx, "Erreur micro: ${e.message}", Toast.LENGTH_SHORT).show() }
                        } else {
                            try { recorder?.stop(); recorder?.release() } catch (e: Exception) {}
                            isRecording = false; recorder = null
                            audioFile?.let { file ->
                                scope.launch {
                                    val uri = Uri.fromFile(file)
                                    val ok = mmsRepo.sendMms(address, uri, "audio/mp4")
                                    Toast.makeText(ctx, if (ok) "Note vocale envoyée" else "Échec vocale", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showAttachSheet = false
                        }
                    }) { Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text(if (isRecording) "Stop & Envoyer" else "Vocale") }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    Scaffold(containerColor = theme.bg,
        topBar = {
            TopAppBar(title = { Text(displayName) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { try { ctx.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$address"))) } catch (e: Exception) {} }) { Icon(Icons.Default.Call, null) }
                    IconButton(onClick = { try { val i = Intent(Intent.ACTION_CALL, Uri.parse("tel:$address")); i.putExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 3); ctx.startActivity(i) } catch (e: Exception) {} }) { Icon(Icons.Default.VideoCall, null) }
                    IconButton(onClick = { showThemeSheet = true }) { Icon(Icons.Default.Palette, null) }
                    IconButton(onClick = { showDeleteConv = true }) { Icon(Icons.Default.Delete, null) }
                })
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(12.dp).imePadding(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick = { showAttachSheet = true }) { Icon(Icons.Default.AttachFile, null) }
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message") }, shape = RoundedCornerShape(24.dp))
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { if (input.isNotBlank()) { repo.sendSms(address, input); input = ""; scope.launch { refresh() } } }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = theme.me)) { Icon(Icons.Default.Send, null) }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(12.dp), reverseLayout = true) {
            if (!contactExists) { item { AssistChip(onClick = { onAddContact(address) }, label = { Text("Ajouter $address") }) } }
            items(msgs.reversed()) { m ->
                Row(Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { msgToDelete = m }), horizontalArrangement = if (m.isMe) Arrangement.End else Arrangement.Start) {
                    Surface(shape = RoundedCornerShape(20.dp, 20.dp, if (m.isMe) 20.dp else 4.dp, if (m.isMe) 4.dp else 20.dp), color = if (m.isMe) theme.me else theme.other, modifier = Modifier.widthIn(max = 300.dp)) {
                        Text(m.body, Modifier.padding(12.dp), color = if (m.isMe) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
