package com.smsg.ui.screens
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.smsg.data.model.Conversation
@Composable
fun CallsScreen(convs: List<Conversation>) {
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize()) {
        items(convs) { c ->
            ListItem(headlineContent = { Text(c.contactName ?: c.address) }, supportingContent = { Text("Appel vocal") },
                trailingContent = { IconButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.address}"))) }) { Icon(Icons.Default.Call, null) } })
            Divider()
        }
    }
}
