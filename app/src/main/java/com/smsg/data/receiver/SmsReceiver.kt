package com.smsg.data.receiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.smsg.data.util.NotifHelper
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (m in msgs) { NotifHelper.showNewSms(context, m.originatingAddress?: "Inconnu", m.messageBody?: "") }
            context.sendBroadcast(Intent("com.smsg.NEW_SMS").setPackage(context.packageName))
        }
    }
}
