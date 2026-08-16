package com.smsg.ui.screens
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
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
import com.smsg.data.model.ChatThemes
import com.smsg.data.model.Message
import com.smsg.data.repository.ContactsRepository
import com.smsg.data.repository.SmsRepository
import com.smsg.data.repository.ThemeRepository
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessageDetailScreen(threadId: Long, address: String, onBack: () -> Unit, onAddContact: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SmsRepository(ctx) }
    val contactRepo = remember { ContactsRepository(ctx) }
    val themeRepo = remember { ThemeRepository(ctx) }
    var msgs by remember { mutableStateOf<List<Message>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var contactExists by remember { mutableStateOf(true) }
    var displayName by remember { mutableStateOf(address) }
    var realThreadId by remember { mutableStateOf(threadId) }
    var theme by remember { mutableStateOf(themeRepo.getTheme(address)) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var msgToDelete by remember { mutableStateOf<Message?>(null) }
    var showDeleteConv by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
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
        AlertDialog(onDismissRequest = { msgToDelete = null }, title = { Text("Supprimer ce message ?") }, text = { Text(msgToDelete!!.body) },
            confirmButton = { TextButton(onClick = { scope.launch { repo.deleteMessage(msgToDelete!!.id); refresh(); msgToDelete = null } }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { msgToDelete = null }) { Text("Annuler") } })
    }
    if (showDeleteConv) {
        AlertDialog(onDismissRequest = { showDeleteConv = false }, title = { Text("Supprimer conversation") }, text = { Text("Supprimer toute la conversation avec $displayName ?") },
            confirmButton = { TextButton(onClick = { scope.launch { repo.deleteThread(realThreadId, address); showDeleteConv = false; onBack() } }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { showDeleteConv = false }) { Text("Annuler") } })
    }
    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choisir un thème", style = MaterialTheme.typography.titleMedium)
                ChatThemes.all.forEach { t ->
                    Row(Modifier.fillMaxWidth().combinedClickable(onClick = { themeRepo.setTheme(address, t.id); theme = t; showThemeSheet = false }) .padding(12.dp)) {
                        Surface(Modifier.size(24.dp), color = t.avatar, shape = RoundedCornerShape(12.dp)) {}
                        Spacer(Modifier.width(12.dp))
                        Text(t.name)
                    }
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
                    IconButton(onClick = { showThemeSheet = true }) { Icon(Icons.Default.Palette, null) }
                    IconButton(onClick = { showDeleteConv = true }) { Icon(Icons.Default.Delete, null) }
                    if (!contactExists) IconButton(onClick = { onAddContact(address) }) { Icon(Icons.Default.PersonAdd, null) }
                })
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(12.dp).imePadding(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message") }, shape = RoundedCornerShape(24.dp))
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { if (input.isNotBlank()) { repo.sendSms(address, input); input = ""; scope.launch { refresh() } } }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = theme.me)) { Icon(Icons.Default.Send, null) }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(12.dp), reverseLayout = true) {
            if (!contactExists) { item { AssistChip(onClick = { onAddContact(address) }, label = { Text("Ajouter $address") }, leadingIcon = { Icon(Icons.Default.PersonAdd, null) }); Spacer(Modifier.height(12.dp)) } }
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
