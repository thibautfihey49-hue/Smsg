package com.smsg.ui.screens
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.smsg.data.model.Conversation
import com.smsg.data.repository.SmsRepository
import java.text.SimpleDateFormat
import kotlinx.coroutines.launch
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(onConversationClick: (Long, String) -> Unit, onNewMessageClick: () -> Unit, onAddContactClick: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SmsRepository(ctx) }
    var convs by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(repo.isDefaultSmsApp()) }
    val scope = rememberCoroutineScope()
    val perms = rememberMultiplePermissionsState(listOf(android.Manifest.permission.READ_SMS, android.Manifest.permission.SEND_SMS, android.Manifest.permission.RECEIVE_SMS, android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_CONTACTS))
    val notifPerm = if (Build.VERSION.SDK_INT >= 33) rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS) else null
    suspend fun load() { if (perms.allPermissionsGranted) convs = repo.getConversations() }
    LaunchedEffect(perms.allPermissionsGranted) { load() }
    LaunchedEffect(Unit) { perms.launchMultiplePermissionRequest(); notifPerm?.launchPermissionRequest(); isDefault = repo.isDefaultSmsApp() }
    DisposableEffect(Unit) {
        val br = object : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) { scope.launch { load() } } }
        val f = IntentFilter("com.smsg.NEW_SMS")
        ctx.registerReceiver(br, f, Context.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.unregisterReceiver(br) }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Messages", fontWeight = FontWeight.Medium) }, actions = { IconButton(onClick = onNewMessageClick) { Icon(Icons.Default.Contacts, null) } }) }, floatingActionButton = { ExtendedFloatingActionButton(onClick = onNewMessageClick, icon = { Icon(Icons.Default.Contacts, null) }, text = { Text("Nouveau") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (!isDefault) {
                Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Pour recevoir les SMS, définis Smsg comme app par défaut")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { try { ctx.startActivity(repo.getDefaultIntent()) } catch (e: Exception) {} }) { Text("Définir par défaut") }
                    }
                }
            }
            if (notifPerm!= null && !notifPerm.status.isGranted) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Activer les notifications", Modifier.weight(1f))
                        Button(onClick = { notifPerm.launchPermissionRequest() }) { Text("Activer") }
                    }
                }
            }
            OutlinedTextField(value = q, onValueChange = { q = it }, modifier = Modifier.fillMaxWidth().padding(16.dp), placeholder = { Text("Rechercher") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = MaterialTheme.shapes.extraLarge, singleLine = true)
            LazyColumn {
                items(convs.filter { it.address.contains(q, true) || (it.contactName?.contains(q, true)?: false) || it.snippet.contains(q, true) }) { c ->
                    Row(Modifier.fillMaxWidth().clickable { onConversationClick(c.id, c.address) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(40.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Text((c.contactName?.firstOrNull()?: c.address.firstOrNull()?: "?").toString().uppercase()) } }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(c.contactName?: c.address, fontWeight = FontWeight.Medium)
                                Text(SimpleDateFormat("HH:mm", java.util.Locale.FRANCE).format(java.util.Date(c.date)), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(c.snippet, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
