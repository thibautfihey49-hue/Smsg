package com.smsg.data.receiver
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.smsg.data.util.NotifHelper
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (m in msgs) {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, m.originatingAddress)
                    put(Telephony.Sms.BODY, m.messageBody)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.READ, 0)
                    put(Telephony.Sms.SEEN, 0)
                }
                try { context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values) } catch (e: Exception) {}
                NotifHelper.showNewSms(context, m.originatingAddress?: "Inconnu", m.messageBody?: "")
            }
            context.sendBroadcast(Intent("com.smsg.NEW_SMS").setPackage(context.packageName))
        }
    }
}
