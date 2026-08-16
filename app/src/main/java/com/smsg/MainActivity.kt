package com.smsg
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.smsg.ui.navigation.SmsgNavHost
import com.smsg.ui.theme.SmsgTheme
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmsgTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { SmsgNavHost() } } }
    }
}
