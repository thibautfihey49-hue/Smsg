package com.smsg.ui.navigation
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smsg.ui.screens.AddContactScreen
import com.smsg.ui.screens.ContactsScreen
import com.smsg.ui.screens.ConversationListScreen
import com.smsg.ui.screens.MessageDetailScreen

@Composable
fun SmsgNavHost() {
    val nav = rememberNavController()
    NavHost(nav, "list") {
        composable("list") {
            ConversationListScreen(
                onConversationClick = { id, a -> nav.navigate("detail/$id/$a") },
                onNewMessageClick = { nav.navigate("contacts") },
                onAddContactClick = { num -> nav.navigate("add_contact/$num") }
            )
        }
        composable("detail/{threadId}/{address}", arguments = listOf(navArgument("threadId") { type = NavType.LongType }, navArgument("address") { type = NavType.StringType })) {
            val tid = it.arguments?.getLong("threadId") ?: 0L
            val addr = it.arguments?.getString("address") ?: ""
            MessageDetailScreen(tid, addr, onBack = { nav.popBackStack() }, onAddContact = { num -> nav.navigate("add_contact/$num") })
        }
        composable("contacts") {
            ContactsScreen(onBack = { nav.popBackStack() }, onContactClick = { c -> c.numbers.firstOrNull()?.let { num -> nav.navigate("detail/0/$num") } }, onAddContact = { nav.navigate("add_contact/") })
        }
        composable("add_contact/{phone}", arguments = listOf(navArgument("phone") { type = NavType.StringType; defaultValue = "" })) {
            val phone = it.arguments?.getString("phone") ?: ""
            AddContactScreen(initialPhone = phone, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
        }
    }
}
