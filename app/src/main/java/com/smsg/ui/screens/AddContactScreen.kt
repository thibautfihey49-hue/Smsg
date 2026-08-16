package com.smsg.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smsg.data.repository.ContactsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(initialPhone: String = "", onBack: () -> Unit, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ContactsRepository(ctx) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(initialPhone) }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Ajouter contact") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Numéro") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = { scope.launch { saving = true; if (repo.addContact(name, phone)) onSaved(); saving = false } }, enabled = name.isNotBlank() && phone.isNotBlank() && !saving, modifier = Modifier.fillMaxWidth()) {
                Text(if (saving) "Enregistrement..." else "Enregistrer")
            }
        }
    }
}
