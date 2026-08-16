package com.smsg.data.repository
import android.content.Context
class ContactsRepository(private val ctx: Context) {
    fun getAllContacts(): List<Pair<String,String>> = emptyList()
    fun addContact(name: String, phone: String) {}
}
