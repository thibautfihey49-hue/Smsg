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
import com.smsg.data.repository.SmsRepository
import com.smsg.ui.theme.SignalBlue
import com.smsg.ui.theme.SignalBubbleMe
import com.smsg.ui.theme.SignalBubbleOther
import com.smsg.ui.theme.SignalBg
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
    var msgs by remember { mutableStateOf<List<Message>>(emptyList()) }
    var localFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf(address) }
    var realThreadId by remember { mutableStateOf(threadId) }
    var showAttach by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    val scope = rememberCoroutineScope()
    val recordPerm = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { try { val f = attachRepo.copyToInternal(it, "${realThreadId}_img", "jpg"); localFiles = localFiles + f } catch (e: Exception) {} }
    }
    suspend fun refresh() {
        realThreadId = if (threadId == 0L) repo.getOrCreateThreadId(address) else threadId
        displayName = repo.getContactName(address) ?: address
        msgs = repo.getMessagesForThread(realThreadId, address)
        localFiles = ctx.filesDir.listFiles()?.filter { it.name.startsWith(realThreadId.toString()) }?.sortedBy { it.lastModified() } ?: emptyList()
    }
    LaunchedEffect(address) { refresh() }
    DisposableEffect(address) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(c: Boolean) { scope.launch { refresh() } } }
        ctx.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
        val br = object : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) { scope.launch { refresh() } } }
        ctx.registerReceiver(br, IntentFilter("com.smsg.NEW_SMS"), Context.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.contentResolver.unregisterContentObserver(observer); ctx.unregisterReceiver(br); player?.release() }
    }
    Scaffold(containerColor = SignalBg,
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(36.dp).clip(CircleShape), color = SignalBlue) { Box(contentAlignment = Alignment.Center){ Text(displayName.firstOrNull()?.toString()?.uppercase() ?: "?", color = Color.White) } }
                    Spacer(Modifier.width(8.dp))
                    Column { Text(displayName, style = MaterialTheme.typography.titleMedium, color = Color.White); Text("Chiffrement de bout en bout • SMS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f)) }
                }
            }, navigationIcon = { IconButton(onClick = onBack){ Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SignalBlue),
                actions = {
                    IconButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$address"))) }) { Icon(Icons.Default.Call, null, tint = Color.White) }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                })
        },
        bottomBar = {
            Column {
                if (showAttach) {
                    Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FilledTonalButton(onClick = { showAttach = false; pickImage.launch("image/*") }) { Icon(Icons.Default.Image, null); Text(" Galerie") }
                        FilledTonalButton(onClick = { pickImage.launch("image/*") }) { Icon(Icons.Default.PhotoCamera, null); Text(" Caméra") }
                        FilledTonalButton(onClick = {}) { Icon(Icons.Default.LocationOn, null); Text(" Lieu") }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(8.dp).imePadding(), verticalAlignment = Alignment.Bottom) {
                    Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                            IconButton(onClick = {}) { Icon(Icons.Default.EmojiEmotions, null, tint = Color.Gray) }
                            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message Signal") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent), maxLines = 4)
                            IconButton(onClick = { showAttach = !showAttach }) { Icon(Icons.Default.AttachFile, null, tint = Color.Gray) }
                            IconButton(onClick = { pickImage.launch("image/*") }) { Icon(Icons.Default.PhotoCamera, null, tint = Color.Gray) }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = {
                        if (input.isNotBlank()) { repo.sendSms(address, input); input = ""; scope.launch { refresh() } }
                        else {
                            if (!recordPerm.status.isGranted) { recordPerm.launchPermissionRequest(); return@FilledIconButton }
                            if (!isRecording) {
                                try {
                                    val f = File.createTempFile("${realThreadId}_voice_", ".m4a", ctx.cacheDir); audioFile = f
                                    val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else MediaRecorder()
                                    mr.apply { setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(f.absolutePath); prepare(); start() }
                                    recorder = mr; isRecording = true
                                } catch (e: Exception) {}
                            } else {
                                try { recorder?.stop(); recorder?.release() } catch (e: Exception) {}
                                isRecording = false
                                audioFile?.let { tmp -> val dest = File(ctx.filesDir, "${realThreadId}_voice_${System.currentTimeMillis()}.m4a"); tmp.copyTo(dest, true); localFiles = localFiles + dest; Toast.makeText(ctx, "Message vocal", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    }, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = SignalBlue)) {
                        Icon(if (input.isNotBlank()) Icons.Default.Send else if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null, tint = Color.White)
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(8.dp)) {
            item { Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFF9C4), modifier = Modifier.padding(8.dp)) { Text("🔒 Messages chiffrés de bout en bout • Mode SMS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp)) } }
            items(msgs) { m ->
                val isMe = m.isMe
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
                    Surface(shape = RoundedCornerShape(18.dp), color = if (isMe) SignalBubbleMe else SignalBubbleOther, shadowElevation = 0.5.dp, modifier = Modifier.widthIn(max = 280.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Text(m.body, color = if (isMe) Color.White else Color.Black)
                            Text(SimpleDateFormat("HH:mm").format(Date(m.date)), style = MaterialTheme.typography.labelSmall, color = if (isMe) Color.White.copy(0.7f) else Color.Gray, modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            items(localFiles) { file ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    when {
                        file.name.contains("_img") -> {
                            val bmp = remember(file.absolutePath) { try { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() } catch (e: Exception) { null } }
                            if (bmp != null) Image(bmp, null, Modifier.size(240.dp).clip(RoundedCornerShape(12.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        }
                        file.name.contains("_voice") -> {
                            Surface(shape = RoundedCornerShape(18.dp), color = SignalBubbleMe, modifier = Modifier.width(260.dp)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FilledIconButton(onClick = { try { player?.release(); player = MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start() } } catch (e: Exception) {} }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White), modifier = Modifier.size(36.dp)) { Icon(Icons.Default.PlayArrow, null, tint = SignalBlue) }
                                    Spacer(Modifier.width(8.dp)); Text("Message vocal", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
