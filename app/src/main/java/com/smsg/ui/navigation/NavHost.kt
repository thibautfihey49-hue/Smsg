package com.smsg.ui.navigation
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smsg.ui.screens.*
import java.io.File

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    NavHost(navController = nav, startDestination = "whatsapp") {
        composable("whatsapp") {
            var convs by remember { mutableStateOf(listOf<com.smsg.data.model.Conversation>()) }
            WhatsAppMainScreen(
                chatContent = {
                    ConversationListScreen(
                        onConversationClick = { id, addr -> nav.navigate("chat/$id/$addr") },
                        onNewMessageClick = { nav.navigate("new") },
                        onAddContactClick = {}
                    )
                },
                statusContent = { StatusScreen(filesDir = ctx.filesDir, onAddStatus = {}) },
                commuContent = { CallsScreen(convs = convs) },
                callsContent = { CallsScreen(convs = convs) }
            )
        }
        composable("chat/{id}/{addr}") { backStack ->
            val id = backStack.arguments?.getString("id")?.toLong() ?: 0L
            val addr = backStack.arguments?.getString("addr") ?: ""
            MessageDetailScreen(threadId = id, address = addr, onBack = { nav.popBackStack() }, onAddContact = {})
        }
        composable("new") {
            NewConversationScreen(onConversationCreated = { id, addr -> nav.navigate("chat/$id/$addr") { popUpTo("whatsapp") } }, onBack = { nav.popBackStack() })
        }
    }
}
