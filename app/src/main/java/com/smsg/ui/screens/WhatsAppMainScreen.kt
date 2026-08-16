package com.smsg.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.smsg.ui.theme.SignalBlue
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun WhatsAppMainScreen(chatContent: @Composable () -> Unit, statusContent: @Composable () -> Unit, commuContent: @Composable () -> Unit, callsContent: @Composable () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Discussions", "Appels", "Stories")
    Scaffold(topBar = {
        TopAppBar(title = { Text("Signal", fontWeight = FontWeight.Bold, color = Color.White) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = SignalBlue),
            actions = { IconButton(onClick = {}){ Icon(Icons.Default.Search, null, tint = Color.White) }; IconButton(onClick = {}){ Icon(Icons.Default.MoreVert, null, tint = Color.White) } })
    }) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(selectedTabIndex = tab, containerColor = SignalBlue, contentColor = Color.White) {
                tabs.forEachIndexed { i, t -> Tab(selected = tab==i, onClick = { tab=i }, text = { Text(t) }) }
            }
            when(tab){0->chatContent();1->callsContent();2->statusContent()}
        }
    }
}
