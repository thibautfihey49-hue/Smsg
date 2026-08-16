package com.smsg.data.repository
import android.content.ContentProviderOperation
import android.content.Context
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
                val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)) ?: "Sans nom"
                val photo = it.getString(it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI))
                val hasPhone = it.getInt(it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                val numbers = mutableListOf<String>()
                if (hasPhone > 0) {
                    val pCur = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?", arrayOf(id.toString()), null)
                    pCur?.use { pc -> while (pc.moveToNext()) { pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))?.let { n -> numbers.add(n) } } }
                }
                list.add(ContactInfo(id, name, photo, numbers))
            }
        }
        list
    }

    suspend fun addContact(name: String, phone: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ops = ArrayList<ContentProviderOperation>()
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null).withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build())
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name).build())
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone).withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build())
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) { false }
    }
}
