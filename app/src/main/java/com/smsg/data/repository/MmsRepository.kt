package com.smsg.data.repository
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
class MmsRepository(private val ctx: Context) {
    fun sendMms(address: String, fileUri: Uri, mime: String): Boolean {
        return try {
            val threadId = Telephony.Threads.getOrCreateThreadId(ctx, address)
            val mmsUri = ctx.contentResolver.insert(Telephony.Mms.CONTENT_URI, ContentValues().apply {
                put(Telephony.Mms.MESSAGE_TYPE, Telephony.Mms.MESSAGE_TYPE_SEND_REQ)
                put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_OUTBOX)
                put(Telephony.Mms.READ, 1)
                put(Telephony.Mms.SEEN, 1)
                put(Telephony.Mms.SUBJECT, "")
                put(Telephony.Mms.CONTENT_TYPE, "application/vnd.wap.multipart.related")
                put(Telephony.Mms.MESSAGE_CLASS, "personal")
                put(Telephony.Mms.DELIVERY_REPORT, 0)
                put(Telephony.Mms.READ_REPORT, 0)
                put(Telephony.Mms.DATE, System.currentTimeMillis()/1000)
                put(Telephony.Mms.THREAD_ID, threadId)
            }) ?: return false
            val mmsId = mmsUri.lastPathSegment?.toLong() ?: return false

            // adresse
            ctx.contentResolver.insert(Uri.parse("content://mms/$mmsId/addr"), ContentValues().apply {
                put(Telephony.Mms.Addr.ADDRESS, address)
                put(Telephony.Mms.Addr.TYPE, Telephony.Mms.Addr.TYPE_TO)
                put(Telephony.Mms.Addr.CHARSET, 106)
            })

            // part
            val input = ctx.contentResolver.openInputStream(fileUri) ?: return false
            val bytes = input.readBytes()
            input.close()

            val partUri = ctx.contentResolver.insert(Uri.parse("content://mms/$mmsId/part"), ContentValues().apply {
                put(Telephony.Mms.Part.SEQ, 0)
                put(Telephony.Mms.Part.CONTENT_TYPE, mime)
                put(Telephony.Mms.Part.NAME, "file")
                put(Telephony.Mms.Part.CONTENT_LOCATION, "file")
                put(Telephony.Mms.Part.CONTENT_ID, "<file>")
            }) ?: return false

            ctx.contentResolver.openOutputStream(partUri)?.use { it.write(bytes) }

            // Pour déclencher l'envoi par le système (car on est app par défaut)
            ctx.contentResolver.insert(Uri.parse("content://mms-sms/send"), ContentValues())
            true
        } catch (e: Exception) { Log.e("MMS", "err", e); false }
    }
}
