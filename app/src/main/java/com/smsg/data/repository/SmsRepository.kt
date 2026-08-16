package com.smsg.data.repository
import android.content.Context; import android.provider.ContactsContract; import android.provider.Telephony; import com.smsg.data.model.Conversation; import com.smsg.data.model.Message; import kotlinx.coroutines.Dispatchers; import kotlinx.coroutines.withContext
class SmsRepository(private val context: Context) {
    suspend fun getConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Conversation>(); val cursor = context.contentResolver.query(Telephony.Threads.CONTENT_URI, null, null, null, "${Telephony.Threads.DATE} DESC")
        cursor?.use { while (it.moveToNext()) { val id = it.getLong(it.getColumnIndex(Telephony.Threads._ID)); val snip = it.getString(it.getColumnIndex(Telephony.Threads.SNIPPET))?: ""; val date = it.getLong(it.getColumnIndex(Telephony.Threads.DATE)); val addr = getAddr(id); list.add(Conversation(id, addr, getName(addr), snip, date, 0)) } }; list
    }
    suspend fun getMessagesForThread(tid: Long): List<Message> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Message>(); val c = context.contentResolver.query(Telephony.Sms.CONTENT_URI, null, "${Telephony.Sms.THREAD_ID} =?", arrayOf(tid.toString()), "${Telephony.Sms.DATE} ASC")
        c?.use { while (it.moveToNext()) { list.add(Message(it.getLong(it.getColumnIndex(Telephony.Sms._ID)), tid, it.getString(it.getColumnIndex(Telephony.Sms.ADDRESS))?: "", it.getString(it.getColumnIndex(Telephony.Sms.BODY))?: "", it.getLong(it.getColumnIndex(Telephony.Sms.DATE)), it.getInt(it.getColumnIndex(Telephony.Sms.TYPE)) == 2, it.getInt(it.getColumnIndex(Telephony.Sms.READ)) == 1, it.getInt(it.getColumnIndex(Telephony.Sms.TYPE)))) } }; list
    }
    fun sendSms(a: String, b: String) { context.getSystemService(android.telephony.SmsManager::class.java).sendTextMessage(a, null, b, null, null) }
    private fun getAddr(tid: Long): String { val c = context.contentResolver.query(Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms.ADDRESS), "${Telephony.Sms.THREAD_ID} =?", arrayOf(tid.toString()), null); c?.use { if (it.moveToFirst()) return it.getString(0)?: "Inconnu" }; return "Inconnu" }
    private fun getName(n: String): String? { val u = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon().appendPath(n).build(); val c = context.contentResolver.query(u, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null); c?.use { if (it.moveToFirst()) return it.getString(0) }; return null }
}
