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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.smsg.data.model.Conversation
import com.smsg.data.repository.SmsRepository
import com.smsg.ui.theme.SignalBlue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationListScreen(onConversationClick: (Long, String) -> Unit, onNewMessageClick: () -> Unit, onAddContactClick: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SmsRepository(ctx) }
    var convs by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var isDefault by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    suspend fun load(){ convs = repo.getConversations(); isDefault = repo.isDefaultSmsApp() }
    LaunchedEffect(Unit){ load() }
    DisposableEffect(lifecycleOwner){
        val obs = LifecycleEventObserver { _, e -> if (e==Lifecycle.Event.ON_RESUME) scope.launch { load() } }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    DisposableEffect(Unit){
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())){ override fun onChange(c: Boolean){ scope.launch{ load() } } }
        ctx.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
        val br = object : BroadcastReceiver(){ override fun onReceive(c: Context, i: Intent){ scope.launch{ load() } } }
        ctx.registerReceiver(br, IntentFilter("com.smsg.NEW_SMS"), Context.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.contentResolver.unregisterContentObserver(observer); ctx.unregisterReceiver(br) }
    }
    Column(Modifier.fillMaxSize()) {
        if (!isDefault) {
            Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(Modifier.padding(12.dp)){
                    Text("Faire de Smsg votre app SMS par défaut comme Signal", fontWeight = FontWeight.Bold, color = SignalBlue)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { try{ ctx.startActivity(repo.getDefaultIntent()) }catch(e:Exception){} }, colors = ButtonDefaults.buttonColors(containerColor = SignalBlue)) { Text("Définir par défaut") }
                }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(convs) { c ->
                Row(Modifier.fillMaxWidth().combinedClickable(onClick = { onConversationClick(c.id, c.address) }).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(48.dp).clip(CircleShape), color = SignalBlue) {
                        Box(contentAlignment = Alignment.Center){ Text((c.contactName?.firstOrNull()?:c.address.firstOrNull()?: "?").toString().uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)){
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                            Text(c.contactName?:c.address, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(SimpleDateFormat("HH:mm").format(java.util.Date(c.date)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Text(c.snippet, maxLines = 1, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
            }
        }
    }
}
