package com.smsg.data.repository
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.telephony.SmsManager
import com.smsg.data.model.Conversation
import com.smsg.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class SmsRepository(private val context: Context) {
    fun isDefaultSmsApp(): Boolean = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    fun getDefaultIntent(): Intent {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = context.getSystemService(RoleManager::class.java)
                rm.createRequestRoleIntent(RoleManager.ROLE_SMS)
            } else {
                Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        } catch (e: Exception) {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
        }
    }
    fun getSettingsIntent(): Intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    fun getOrCreateThreadId(address: String): Long {
        return try { Telephony.Threads.getOrCreateThreadId(context, address) } catch (e: Exception) { 0L }
    }
    fun getContactName(phone: String): String? {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
            val cur = context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
            cur?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (e: Exception) { null }
    }
    suspend fun getConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        val map = linkedMapOf<Long, Conversation>()
        try {
            val uri = Telephony.Sms.CONTENT_URI
            val proj = arrayOf(Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
            val cur = context.contentResolver.query(uri, proj, null, null, "${Telephony.Sms.DATE} DESC")
            cur?.use {
                val idxThread = it.getColumnIndex(Telephony.Sms.THREAD_ID)
                val idxAddr = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val idxBody = it.getColumnIndex(Telephony.Sms.BODY)
                val idxDate = it.getColumnIndex(Telephony.Sms.DATE)
                while (it.moveToNext()) {
                    if (idxThread == -1) continue
                    val threadId = it.getLong(idxThread)
                    if (map.containsKey(threadId)) continue
                    val address = if (idxAddr != -1) it.getString(idxAddr)?: "" else ""
                    if (address.isBlank()) continue
                    val body = if (idxBody != -1) it.getString(idxBody)?: "" else ""
                    val date = if (idxDate != -1) it.getLong(idxDate) else System.currentTimeMillis()
                    val name = getContactName(address)
                    map[threadId] = Conversation(threadId, address, name, body, date, 0)
                }
            }
        } catch (e: Exception) {}
        map.values.toList()
    }
    suspend fun getMessagesForThread(threadId: Long, address: String): List<Message> = withContext(Dispatchers.IO) {
        val realId = if (threadId == 0L) getOrCreateThreadId(address) else threadId
        val list = mutableListOf<Message>()
        try {
            val uri = Telephony.Sms.CONTENT_URI
            val sel = if (realId != 0L) "${Telephony.Sms.THREAD_ID} =?" else "${Telephony.Sms.ADDRESS} =?"
            val args = if (realId != 0L) arrayOf(realId.toString()) else arrayOf(address)
            val cur = context.contentResolver.query(uri, null, sel, args, "${Telephony.Sms.DATE} ASC")
            cur?.use {
                val idxId = it.getColumnIndex(Telephony.Sms._ID)
                val idxThread = it.getColumnIndex(Telephony.Sms.THREAD_ID)
                val idxAddr = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val idxBody = it.getColumnIndex(Telephony.Sms.BODY)
                val idxDate = it.getColumnIndex(Telephony.Sms.DATE)
                val idxType = it.getColumnIndex(Telephony.Sms.TYPE)
                val idxRead = it.getColumnIndex(Telephony.Sms.READ)
                while (it.moveToNext()) {
                    val id = if (idxId != -1) it.getLong(idxId) else 0L
                    val tId = if (idxThread != -1) it.getLong(idxThread) else realId
                    val addr = if (idxAddr != -1) it.getString(idxAddr)?: "" else ""
                    val body = if (idxBody != -1) it.getString(idxBody)?: "" else ""
                    val date = if (idxDate != -1) it.getLong(idxDate) else 0L
                    val type = if (idxType != -1) it.getInt(idxType) else 1
                    val read = if (idxRead != -1) it.getInt(idxRead) == 1 else true
                    list.add(Message(id, tId, addr, body, date, type == 2, read, type))
                }
            }
        } catch (e: Exception) {}
        list
    }
    fun sendSms(address: String, body: String) {
        try {
            SmsManager.getDefault().sendTextMessage(address, null, body, null, null)
            // Si pas app par défaut, on insert nous même dans SENT pour affichage instantané
            if (!isDefaultSmsApp()) {
                val values = android.content.ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, address)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                    put(Telephony.Sms.READ, 1)
                }
                try { context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values) } catch (e: Exception) {}
            }
        } catch (e: Exception) {}
    }
}
