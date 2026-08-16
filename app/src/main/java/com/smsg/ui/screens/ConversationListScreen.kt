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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.smsg.data.model.Conversation
import com.smsg.data.repository.SmsRepository
import com.smsg.data.repository.ThemeRepository
import java.text.SimpleDateFormat
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationListScreen(onConversationClick: (Long, String) -> Unit, onNewMessageClick: () -> Unit, onAddContactClick: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SmsRepository(ctx) }
    val themeRepo = remember { ThemeRepository(ctx) }
    var convs by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<Conversation?>(null) }
    var isDefault by remember { mutableStateOf(repo.isDefaultSmsApp()) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    suspend fun load() { convs = repo.getConversations(); isDefault = repo.isDefaultSmsApp() }
    LaunchedEffect(Unit) { load() }
    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { isDefault = repo.isDefaultSmsApp(); scope.launch { load() } }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
    }
    DisposableEffect(Unit) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean) { scope.launch { load() } } }
        ctx.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
        val br = object : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) { scope.launch { load() } } }
        ctx.registerReceiver(br, IntentFilter("com.smsg.NEW_SMS"), Context.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.contentResolver.unregisterContentObserver(observer); ctx.unregisterReceiver(br) }
    }
    if (toDelete != null) {
        AlertDialog(onDismissRequest = { toDelete = null }, title = { Text("Supprimer ?") }, text = { Text("Supprimer toute la conversation avec ${toDelete!!.contactName ?: toDelete!!.address} ?") },
            confirmButton = { TextButton(onClick = { scope.launch { repo.deleteThread(toDelete!!.id, toDelete!!.address); load(); toDelete = null } }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuler") } })
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Messages") }, actions = { IconButton(onClick = onNewMessageClick) { Icon(Icons.Default.Contacts, null) } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onNewMessageClick, icon = { Icon(Icons.Default.Contacts, null) }, text = { Text("Nouveau") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (!isDefault) {
                Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Pour recevoir les SMS instantanément, définis Smsg comme app par défaut.")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { try { ctx.startActivity(repo.getDefaultIntent()) } catch (e: Exception) {} }) { Text("Essayer auto") }
                            OutlinedButton(onClick = { try { ctx.startActivity(repo.getSettingsIntent()) } catch (e: Exception) {} }) { Text("Paramètres") }
                        }
                    }
                }
            }
            OutlinedTextField(value = q, onValueChange = { q = it }, modifier = Modifier.fillMaxWidth().padding(16.dp), placeholder = { Text("Rechercher") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = MaterialTheme.shapes.extraLarge, singleLine = true)
            LazyColumn {
                items(convs.filter { it.address.contains(q, true) || (it.contactName?.contains(q, true)?: false) }) { c ->
                    val theme = themeRepo.getTheme(c.address)
                    Row(Modifier.fillMaxWidth().combinedClickable(onClick = { onConversationClick(c.id, c.address) }, onLongClick = { toDelete = c }).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(40.dp).clip(CircleShape), color = theme.avatar) { Box(contentAlignment = Alignment.Center) { Text((c.contactName?.firstOrNull()?: c.address.firstOrNull()?: "?").toString().uppercase(), color = androidx.compose.ui.graphics.Color.White) } }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(c.contactName?: c.address, fontWeight = FontWeight.Medium)
                                Text(SimpleDateFormat("HH:mm").format(java.util.Date(c.date)), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(c.snippet, maxLines = 1)
                        }
                        IconButton(onClick = { toDelete = c }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
    }
}
