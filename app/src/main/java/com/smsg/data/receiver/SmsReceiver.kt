package com.smsg.data.receiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val msgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) Telephony.Sms.Intents.getMessagesFromIntent(intent) else emptyArray()
            if (msgs.isNotEmpty()) {
                val from = msgs[0].originatingAddress?: "Inconnu"
                val body = msgs.joinToString("") { it.messageBody }
                // Petite notif visuelle si app pas au premier plan
                Toast.makeText(context, "Nouveau SMS de $from: $body", Toast.LENGTH_LONG).show()
                // On force le refresh des conversations en renvoyant un broadcast interne
                context.sendBroadcast(Intent("com.smsg.NEW_SMS").setPackage(context.packageName))
            }
        }
    }
}
