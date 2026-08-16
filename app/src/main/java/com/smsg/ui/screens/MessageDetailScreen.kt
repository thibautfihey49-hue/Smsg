package com.smsg.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smsg.data.model.Message
import com.smsg.data.repository.ContactsRepository
import com.smsg.data.repository.SmsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(threadId: Long, address: String, onBack: () -> Unit, onAddContact: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SmsRepository(ctx) }
    val contactRepo = remember { ContactsRepository(ctx) }
    var msgs by remember { mutableStateOf<List<Message>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var contactExists by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(address) {
        contactExists = contactRepo.exists(address)
        msgs = if (threadId!= 0L) repo.getMessagesForThread(threadId) else emptyList()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(address) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { if (!contactExists) IconButton(onClick = { onAddContact(address) }) { Icon(Icons.Default.PersonAdd, null) } }
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(12.dp).imePadding(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message") }, shape = RoundedCornerShape(24.dp))
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = {
                    if (input.isNotBlank()) {
                        repo.sendSms(address, input)
                        scope.launch {
                            msgs = msgs + Message(System.currentTimeMillis(), threadId, address, input, System.currentTimeMillis(), true, true, 2)
                            input = ""
                        }
                    }
                }) { Icon(Icons.Default.Send, null) }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(12.dp), reverseLayout = true) {
            if (!contactExists) {
                item {
                    AssistChip(onClick = { onAddContact(address) }, label = { Text("Ajouter $address aux contacts") }, leadingIcon = { Icon(Icons.Default.PersonAdd, null) })
                    Spacer(Modifier.height(12.dp))
                }
            }
            items(msgs.reversed()) { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.isMe) Arrangement.End else Arrangement.Start) {
                    Surface(shape = RoundedCornerShape(20.dp, 20.dp, if (m.isMe) 20.dp else 4.dp, if (m.isMe) 4.dp else 20.dp), color = if (m.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.widthIn(max = 300.dp)) {
                        Text(m.body, Modifier.padding(12.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
