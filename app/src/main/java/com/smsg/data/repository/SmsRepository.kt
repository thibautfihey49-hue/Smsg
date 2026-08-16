package com.smsg.data.repository
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import com.smsg.data.model.Conversation
import com.smsg.data.model.Message
class SmsRepository(private val ctx: Context) {
    fun isDefaultSmsApp(): Boolean = Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName
    fun getDefaultIntent(): Intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, ctx.packageName)
    fun getContactName(phone: String): String? {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (e: Exception) { null }
    }
    fun getOrCreateThreadId(address: String): Long { return try { Telephony.Threads.getOrCreateThreadId(ctx, address) } catch (e: Exception) { System.currentTimeMillis() } }
    fun getConversations(): List<Conversation> {
        val map = LinkedHashMap<Long, Conversation>()
        try {
            ctx.contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, "${Telephony.Sms.DATE} DESC")?.use { c ->
                val tIdx = c.getColumnIndex(Telephony.Sms.THREAD_ID); val aIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bIdx = c.getColumnIndex(Telephony.Sms.BODY); val dIdx = c.getColumnIndex(Telephony.Sms.DATE)
                while (c.moveToNext()) {
                    val tid = if (tIdx>=0) c.getLong(tIdx) else 0L
                    if (map.containsKey(tid)) continue
                    val addr = c.getString(aIdx)?: continue
                    if (addr.isBlank()) continue
                    val body = c.getString(bIdx)?: ""
                    val date = if (dIdx>=0) c.getLong(dIdx) else 0L
                    map[tid] = Conversation(tid, addr, getContactName(addr), body, date, 1)
                }
            }
        } catch (e: Exception) {}
        return map.values.toList()
    }
    fun getMessagesForThread(threadId: Long, address: String): List<Message> {
        val list = mutableListOf<Message>()
        try {
            val sel = if (threadId!=0L) "${Telephony.Sms.THREAD_ID}=$threadId" else "${Telephony.Sms.ADDRESS}=?"
            val args = if (threadId!=0L) null else arrayOf(address)
            ctx.contentResolver.query(Telephony.Sms.CONTENT_URI, null, sel, args, "${Telephony.Sms.DATE} ASC")?.use { c ->
                val idIdx = c.getColumnIndex(Telephony.Sms._ID); val tIdx = c.getColumnIndex(Telephony.Sms.THREAD_ID)
                val aIdx = c.getColumnIndex(Telephony.Sms.ADDRESS); val bIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dIdx = c.getColumnIndex(Telephony.Sms.DATE); val tyIdx = c.getColumnIndex(Telephony.Sms.TYPE)
                val rIdx = c.getColumnIndex(Telephony.Sms.READ)
                while (c.moveToNext()) {
                    val id = c.getLong(idIdx); val tid = if (tIdx>=0) c.getLong(tIdx) else threadId
                    val addr = c.getString(aIdx)?: address; val body = c.getString(bIdx)?: ""
                    val date = c.getLong(dIdx); val type = c.getInt(tyIdx); val read = if (rIdx>=0) c.getInt(rIdx)==1 else true
                    list.add(Message(id, tid, addr, body, date, type==Telephony.Sms.MESSAGE_TYPE_SENT, read))
                }
            }
        } catch (e: Exception) {}
        return list
    }
    fun sendSms(address: String, body: String) {
        try {
            val mgr = ctx.getSystemService(SmsManager::class.java)?: SmsManager.getDefault()
            val parts = mgr.divideMessage(body)
            if (parts.size>1) mgr.sendMultipartTextMessage(address, null, parts, null, null) else mgr.sendTextMessage(address, null, body, null, null)
            val v = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address); put(Telephony.Sms.BODY, body); put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT); put(Telephony.Sms.READ, 1); put(Telephony.Sms.THREAD_ID, getOrCreateThreadId(address))
            }
            try { ctx.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, v) } catch (e: Exception) { ctx.contentResolver.insert(Telephony.Sms.CONTENT_URI, v) }
            ctx.sendBroadcast(Intent("com.smsg.NEW_SMS").setPackage(ctx.packageName))
        } catch (e: Exception) { e.printStackTrace() }
    }
}
