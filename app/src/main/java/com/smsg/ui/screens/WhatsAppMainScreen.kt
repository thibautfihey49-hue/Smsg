package com.smsg.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smsg.ui.theme.GmBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppMainScreen(chatContent: @Composable () -> Unit, statusContent: @Composable () -> Unit, commuContent: @Composable () -> Unit, callsContent: @Composable () -> Unit) {
    var search by remember { mutableStateOf("") }
    Scaffold(
        containerColor = Color(0xFFF8F9FF),
        topBar = {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                // Barre de recherche Google Messages exacte
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row { Icon(Icons.Default.Menu, null); Spacer(Modifier.width(12.dp)); Text("Messages") }
                        Icon(Icons.Default.AccountCircle, null)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = true, onClick = {}, label = { Text("Tous") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Personnel") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Professionnel") })
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {}, containerColor = GmBlue, contentColor = Color.White, icon = { Icon(Icons.Default.Edit, null) }, text = { Text("Nouveau message") })
        }
    ) { pad ->
        Box(Modifier.padding(pad)) { chatContent() }
    }
}
