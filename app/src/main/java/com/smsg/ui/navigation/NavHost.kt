package com.smsg.ui.navigation
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smsg.ui.screens.*
@Composable fun AppNavHost(){
    val nav = rememberNavController()
    val ctx = LocalContext.current
    NavHost(navController = nav, startDestination = "signal"){
        composable("signal"){
            WhatsAppMainScreen(
                chatContent = { ConversationListScreen(onConversationClick = { id, addr -> nav.navigate("chat/$id/$addr") }, onNewMessageClick = { nav.navigate("new") }, onAddContactClick = {}) },
                statusContent = { StatusScreen(filesDir = ctx.filesDir, onAddStatus = {}) },
                commuContent = { StatusScreen(filesDir = ctx.filesDir, onAddStatus = {}) },
                callsContent = { CallsScreen(convs = emptyList()) }
            )
        }
        composable("chat/{id}/{addr}"){ b ->
            val id = b.arguments?.getString("id")?.toLong()?:0L
            val addr = b.arguments?.getString("addr")?:""
            MessageDetailScreen(threadId = id, address = addr, onBack = { nav.popBackStack() }, onAddContact = {})
        }
        composable("new"){ NewConversationScreen(onConversationCreated = { id, addr -> nav.navigate("chat/$id/$addr"){ popUpTo("signal") } }, onBack = { nav.popBackStack() }) }
    }
}
