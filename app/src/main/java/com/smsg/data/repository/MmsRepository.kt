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
                put("m_type", 128) // MESSAGE_TYPE_SEND_REQ
                put("msg_box", 4) // MESSAGE_BOX_OUTBOX
                put("read", 1)
                put("seen", 1)
                put("sub", "")
                put("ct_t", "application/vnd.wap.multipart.related")
                put("m_cls", "personal")
                put("d_rpt", 0)
                put("rr", 0)
                put("date", System.currentTimeMillis() / 1000)
                put("thread_id", threadId)
            }) ?: return false
            val mmsId = mmsUri.lastPathSegment?.toLong() ?: return false

            // TO
            ctx.contentResolver.insert(Uri.parse("content://mms/$mmsId/addr"), ContentValues().apply {
                put("address", address)
                put("type", 151) // TYPE_TO
                put("charset", 106)
            })

            // PART
            val input = ctx.contentResolver.openInputStream(fileUri) ?: return false
            val bytes = input.readBytes()
            input.close()

            val partUri = ctx.contentResolver.insert(Uri.parse("content://mms/$mmsId/part"), ContentValues().apply {
                put("seq", 0)
                put("ct", mime)
                put("name", "file")
                put("cl", "file")
                put("cid", "<file>")
            }) ?: return false

            ctx.contentResolver.openOutputStream(partUri)?.use { it.write(bytes) }

            true
        } catch (e: Exception) { Log.e("MMS", "err", e); false }
    }
}
