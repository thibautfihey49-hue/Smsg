package com.smsg.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smsg.data.repository.SmsRepository
import androidx.compose.ui.platform.LocalContext
import com.smsg.ui.theme.WaGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationScreen(onConversationCreated: (Long, String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SmsRepository(ctx) }
    var number by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouveau message", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WaGreen)
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Numéro ou contact") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                if (number.isNotBlank()) {
                    val threadId = repo.getOrCreateThreadId(number)
                    onConversationCreated(threadId, number)
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = WaGreen), modifier = Modifier.fillMaxWidth()) {
                Text("Démarrer la discussion")
            }
        }
    }
}
