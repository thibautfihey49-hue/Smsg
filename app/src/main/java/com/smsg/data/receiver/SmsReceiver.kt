package com.smsg.data.receiver
import android.content.BroadcastReceiver; import android.content.Context; import android.content.Intent; import android.provider.Telephony
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent?.action) { Telephony.Sms.Intents.getMessagesFromIntent(intent) }
    }
}
