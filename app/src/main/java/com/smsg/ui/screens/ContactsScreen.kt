package com.smsg.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smsg.data.model.ContactInfo
import com.smsg.data.repository.ContactsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(onBack: () -> Unit, onContactClick: (ContactInfo) -> Unit, onAddContact: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ContactsRepository(ctx) }
    var contacts by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { contacts = repo.getAllContacts() }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Contacts (${contacts.size})") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }, actions = { IconButton(onClick = onAddContact) { Icon(Icons.Default.PersonAdd, null) } }) },
        floatingActionButton = { FloatingActionButton(onClick = onAddContact) { Icon(Icons.Default.PersonAdd, null) } }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(16.dp), placeholder = { Text("Rechercher contacts") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = MaterialTheme.shapes.extraLarge)
            LazyColumn {
                items(contacts.filter { it.name.contains(query, true) || it.numbers.any { n -> n.contains(query) } }) { c ->
                    Row(Modifier.fillMaxWidth().clickable { onContactClick(c) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(40.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Text(c.name.firstOrNull()?.uppercase() ?: "?") } }
                        Spacer(Modifier.width(12.dp))
                        Column { Text(c.name); Text(c.numbers.firstOrNull() ?: "Pas de numéro", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
