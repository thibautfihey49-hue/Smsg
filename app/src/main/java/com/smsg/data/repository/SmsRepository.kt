package com.smsg.data.repository
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import com.smsg.data.model.Conversation
import com.smsg.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsRepository(private val context: Context) {
    fun isDefaultSmsApp(): Boolean {
        return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    }
    fun getDefaultIntent(): Intent {
        return Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
    }
    suspend fun getConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Conversation>()
        val uri = Telephony.Threads.CONTENT_URI
        val cur: Cursor? = context.contentResolver.query(uri, null, null, null, "${Telephony.Threads.DATE} DESC")
        cur?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex(Telephony.Threads._ID))
                val snippet = it.getString(it.getColumnIndex(Telephony.Threads.SNIPPET))?: ""
                val date = it.getLong(it.getColumnIndex(Telephony.Threads.DATE))
                val recipientIds = it.getString(it.getColumnIndex(Telephony.Threads.RECIPIENT_IDS))?: ""
                val address = getAddressFromRecipientIds(recipientIds)
                val name = getContactName(address)
                list.add(Conversation(id, address, name, snippet, date, 0))
            }
        }
        list
    }
    private fun getAddressFromRecipientIds(ids: String): String {
        try {
            val uri = Uri.parse("content://mms-sms/canonical-addresses")
            val cur = context.contentResolver.query(uri, null, "_id =?", arrayOf(ids.split(" ").first()), null)
            cur?.use { if (it.moveToFirst()) return it.getString(1) }
        } catch (e: Exception) {}
        return ids
    }
    private fun getContactName(phone: String): String? {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
            val cur = context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
            cur?.use { if (it.moveToFirst()) return it.getString(0) }
        } catch (e: Exception) {}
        return null
    }
    suspend fun getMessagesForThread(threadId: Long): List<Message> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Message>()
        val uri = Telephony.Sms.CONTENT_URI
        val cur = context.contentResolver.query(uri, null, "${Telephony.Sms.THREAD_ID} =?", arrayOf(threadId.toString()), "${Telephony.Sms.DATE} ASC")
        cur?.use {
            while (it.moveToNext()) {
                list.add(Message(it.getLong(it.getColumnIndex(Telephony.Sms._ID)), it.getLong(it.getColumnIndex(Telephony.Sms.THREAD_ID)), it.getString(it.getColumnIndex(Telephony.Sms.ADDRESS))?: "", it.getString(it.getColumnIndex(Telephony.Sms.BODY))?: "", it.getLong(it.getColumnIndex(Telephony.Sms.DATE)), it.getInt(it.getColumnIndex(Telephony.Sms.TYPE)) == 2, it.getInt(it.getColumnIndex(Telephony.Sms.READ)) == 1, it.getInt(it.getColumnIndex(Telephony.Sms.TYPE))))
            }
        }
        list
    }
    fun sendSms(address: String, body: String) {
        SmsManager.getDefault().sendTextMessage(address, null, body, null, null)
    }
}
