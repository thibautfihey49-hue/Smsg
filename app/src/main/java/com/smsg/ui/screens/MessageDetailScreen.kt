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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.smsg.data.model.ChatThemes
import com.smsg.data.model.Message
import com.smsg.data.repository.AttachmentRepository
import com.smsg.data.repository.ContactsRepository
import com.smsg.data.repository.SmsRepository
import com.smsg.data.repository.ThemeRepository
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun MessageDetailScreen(threadId: Long, address: String, onBack: () -> Unit, onAddContact: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SmsRepository(ctx) }
    val attachRepo = remember { AttachmentRepository(ctx) }
    val themeRepo = remember { ThemeRepository(ctx) }
    var msgs by remember { mutableStateOf<List<Message>>(emptyList()) }
    var localFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf(address) }
    var realThreadId by remember { mutableStateOf(threadId) }
    var theme by remember { mutableStateOf(themeRepo.getTheme(address)) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    val scope = rememberCoroutineScope()
    val recordPerm = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val imagePerm = if (Build.VERSION.SDK_INT >= 33) rememberPermissionState(android.Manifest.permission.READ_MEDIA_IMAGES) else rememberPermissionState(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try { val f = attachRepo.copyToInternal(it, "${realThreadId}_img", "jpg"); localFiles = localFiles + f; Toast.makeText(ctx, "Image ajoutée", Toast.LENGTH_SHORT).show() } catch (e: Exception) {}
        }
    }
    suspend fun refresh() {
        realThreadId = if (threadId == 0L) repo.getOrCreateThreadId(address) else threadId
        displayName = repo.getContactName(address) ?: address
        theme = themeRepo.getTheme(address)
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
    Scaffold(containerColor = theme.bg,
        topBar = {
            TopAppBar(title = { Text(displayName) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$address"))) }) { Icon(Icons.Default.Call, null) }
                    IconButton(onClick = { showThemeSheet = true }) { Icon(Icons.Default.Palette, null) }
                })
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(12.dp).imePadding(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick = { if (!imagePerm.status.isGranted) imagePerm.launchPermissionRequest(); showAttachSheet = true }) { Icon(Icons.Default.AttachFile, null) }
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message") }, shape = RoundedCornerShape(24.dp))
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { if (input.isNotBlank()) { repo.sendSms(address, input); input = ""; scope.launch { refresh() } } }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = theme.me)) { Icon(Icons.Default.Send, null) }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(12.dp)) {
            items(msgs) { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.isMe) Arrangement.End else Arrangement.Start) {
                    Surface(shape = RoundedCornerShape(18.dp), color = if (m.isMe) theme.me else theme.other, modifier = Modifier.widthIn(max = 300.dp)) {
                        Text(m.body, Modifier.padding(12.dp), color = if (m.isMe) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            items(localFiles) { file ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    when {
                        file.name.contains("_img") -> {
                            val bmp = remember(file.absolutePath) { try { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() } catch (e: Exception) { null } }
                            if (bmp != null) Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(220.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                        }
                        file.name.contains("_voice") -> {
                            FilledTonalButton(onClick = {
                                try { player?.release(); player = MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start() } } catch (e: Exception) {}
                            }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Vocale") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    if (showAttachSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachSheet = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilledTonalButton(onClick = { showAttachSheet = false; pickImage.launch("image/*") }) { Icon(Icons.Default.Image, null); Text(" Image") }
                }
                FilledTonalButton(modifier = Modifier.fillMaxWidth(), onClick = {
                    if (!recordPerm.status.isGranted) { recordPerm.launchPermissionRequest(); return@FilledTonalButton }
                    if (!isRecording) {
                        try {
                            val f = File.createTempFile("${realThreadId}_voice_", ".m4a", ctx.cacheDir)
                            audioFile = f
                            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else MediaRecorder()
                            mr.apply { setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(f.absolutePath); prepare(); start() }
                            recorder = mr; isRecording = true
                        } catch (e: Exception) {}
                    } else {
                        try { recorder?.stop(); recorder?.release() } catch (e: Exception) {}
                        isRecording = false
                        audioFile?.let { tmp ->
                            val dest = File(ctx.filesDir, "${realThreadId}_voice_${System.currentTimeMillis()}.m4a")
                            tmp.copyTo(dest, overwrite = true)
                            localFiles = localFiles + dest
                        }
                        showAttachSheet = false
                    }
                }) { Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null); Text(if (isRecording) " Arrêter" else " Note vocale") }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Column(Modifier.padding(16.dp)) {
                ChatThemes.all.forEach { t ->
                    TextButton(onClick = { themeRepo.setTheme(address, t.id); theme = t; showThemeSheet = false }) { Text(t.name) }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
