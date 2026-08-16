package com.smsg.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smsg.ui.theme.GmBlue
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Contacts", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack){ Icon(Icons.Default.ArrowBack, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = GmBlue)) }) { pad ->
        Box(Modifier.padding(pad).padding(16.dp)) { Text("Liste contacts") }
    }
}
