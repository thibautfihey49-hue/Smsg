package com.smsg.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smsg.ui.theme.WaGreen
import java.io.File

@Composable
fun StatusScreen(filesDir: File, onAddStatus: () -> Unit) {
    val statuses = remember { filesDir.listFiles()?.filter { it.name.contains("status") } ?: emptyList() }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            ListItem(headlineContent = { Text("Mon statut") }, supportingContent = { Text("Appuyez pour ajouter") },
                leadingContent = {
                    Box {
                        Surface(Modifier.size(50.dp).clip(CircleShape), color = Color.LightGray) {}
                        Surface(Modifier.size(20.dp).align(Alignment.BottomEnd), color = WaGreen, shape = CircleShape) { Icon(Icons.Default.Add, null, tint = Color.White) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Divider()
            Text("Mises à jour récentes", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 12.dp))
        }
        items(statuses) { f ->
            ListItem(headlineContent = { Text(f.name) }, leadingContent = { Surface(Modifier.size(50.dp).clip(CircleShape), color = WaGreen) {} })
        }
    }
}
