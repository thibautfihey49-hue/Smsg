package com.smsg.data.receiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import com.smsg.data.util.NotifHelper
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action!= Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (Telephony.Sms.getDefaultSmsPackage(context) == context.packageName) return
        val bundle = intent.extras?: return
        val pdus = bundle.get("pdus") as? Array<*>?: return
        val format = bundle.getString("format")
        for (pdu in pdus) {
            val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) SmsMessage.createFromPdu(pdu as ByteArray, format) else SmsMessage.createFromPdu(pdu as ByteArray)
            val addr = msg.originatingAddress?: continue
            val body = msg.messageBody?: ""
            NotifHelper.showNewSms(context, addr, body)
        }
        context.sendBroadcast(Intent("com.smsg.NEW_SMS").setPackage(context.packageName))
    }
}
