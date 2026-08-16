package com.smsg.data.repository
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.smsg.data.model.ContactInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class ContactsRepository(private val context: Context) {
    suspend fun getAllContacts(): List<ContactInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ContactInfo>()
        val cursor = context.contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, "${ContactsContract.Contacts.DISPLAY_NAME} ASC")
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex(ContactsContract.Contacts._ID))
                val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))?: "Sans nom"
                val photo = it.getString(it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI))
                val hasPhone = it.getInt(it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                val numbers = mutableListOf<String>()
                if (hasPhone > 0) {
                    val pCur = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} =?", arrayOf(id.toString()), null)
                    pCur?.use { pc -> while (pc.moveToNext()) { pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))?.let { n -> numbers.add(n) } } }
                }
                list.add(ContactInfo(id, name, photo, numbers))
            }
        }
        list
    }
    suspend fun exists(phone: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
            val cur = context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            cur?.use { it.moveToFirst() }?: false
        } catch (e: Exception) { false }
    }
    suspend fun addContact(name: String, phone: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val rawUri = context.contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, ContentValues().apply { put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?); put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?) })?: return@withContext false
            val rawId = ContentUris.parseId(rawUri)
            context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, ContentValues().apply { put(ContactsContract.Data.RAW_CONTACT_ID, rawId); put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE); put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name) })
            context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, ContentValues().apply { put(ContactsContract.Data.RAW_CONTACT_ID, rawId); put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE); put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone); put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) })
            true
        } catch (e: Exception) { false }
    }
}
