package com.smsg
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.smsg.ui.navigation.AppNavHost
import com.smsg.ui.theme.WhatsAppTheme
class MainActivity : ComponentActivity() {
    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val perms = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CONTACTS, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.READ_MEDIA_IMAGES) else perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        launcher.launch(perms.toTypedArray())
        setContent { WhatsAppTheme { AppNavHost() } }
    }
}
