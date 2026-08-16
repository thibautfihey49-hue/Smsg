package com.smsg.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.smsg.ui.theme.WaGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppMainScreen(
    chatContent: @Composable () -> Unit,
    statusContent: @Composable () -> Unit,
    commuContent: @Composable () -> Unit,
    callsContent: @Composable () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Disc.", "Actus", "Commu.", "Appels")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsApp", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WaGreen),
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, null, tint = Color.White) }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                }
            )
        },
        bottomBar = {
            // Pour ressembler au nouveau WhatsApp on met les onglets en bas
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(selectedTabIndex = tab, containerColor = WaGreen, contentColor = Color.White) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal) })
                }
            }
            when (tab) {
                0 -> chatContent()
                1 -> statusContent()
                2 -> commuContent()
                3 -> callsContent()
            }
        }
    }
}
