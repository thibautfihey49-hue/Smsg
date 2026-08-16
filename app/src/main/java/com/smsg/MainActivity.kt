package com.smsg
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.smsg.data.util.NotifHelper
import com.smsg.ui.navigation.SmsgNavHost
import com.smsg.ui.theme.SmsgTheme
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotifHelper.ensureChannel(this)
        setContent { SmsgTheme { SmsgNavHost() } }
    }
}
