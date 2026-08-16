package com.smsg.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
@Composable fun StatusScreen(filesDir: File, onAddStatus: () -> Unit){
    Column(Modifier.fillMaxSize().padding(16.dp)){ Text("Stories Signal - bientôt", style = MaterialTheme.typography.titleMedium) }
}
