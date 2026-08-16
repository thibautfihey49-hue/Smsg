package com.smsg.data.receiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import android.content.ContentValues
class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        val reply = results.getCharSequence("key_text_reply")?.toString() ?: return
        val address = intent.getStringExtra("address") ?: return
        val notifId = intent.getIntExtra("notif_id", 0)
        try {
            SmsManager.getDefault().sendTextMessage(address, null, reply, null, null)
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address); put(Telephony.Sms.BODY, reply)
                put(Telephony.Sms.DATE, System.currentTimeMillis()); put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT); put(Telephony.Sms.READ, 1)
            }
            try { context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values) } catch (e: Exception) {}
            NotificationManagerCompat.from(context).cancel(notifId)
            context.sendBroadcast(Intent("com.smsg.NEW_SMS").setPackage(context.packageName))
        } catch (e: Exception) {}
    }
}
