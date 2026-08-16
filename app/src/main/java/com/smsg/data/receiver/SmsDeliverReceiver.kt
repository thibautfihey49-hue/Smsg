package com.smsg.data.receiver
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import com.smsg.data.util.NotifHelper
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action!= Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val bundle = intent.extras?: return
        val pdus = bundle.get("pdus") as? Array<*>?: return
        val format = bundle.getString("format")
        for (pdu in pdus) {
            val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) SmsMessage.createFromPdu(pdu as ByteArray, format) else SmsMessage.createFromPdu(pdu as ByteArray)
            val addr = msg.originatingAddress?: continue
            val body = msg.messageBody?: ""
            val v = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, addr); put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis()); put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX); put(Telephony.Sms.READ, 0)
            }
            try { context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, v) } catch (e: Exception) { try { context.contentResolver.insert(Telephony.Sms.CONTENT_URI, v) } catch (e2: Exception) {} }
            NotifHelper.showNewSms(context, addr, body)
        }
        context.sendBroadcast(Intent("com.smsg.NEW_SMS").setPackage(context.packageName))
    }
}
