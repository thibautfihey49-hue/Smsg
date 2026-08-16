package com.smsg.data.model
data class Conversation(val id: Long, val address: String, val contactName: String?, val snippet: String, val date: Long, val unreadCount: Int)
data class Message(val id: Long, val threadId: Long, val address: String, val body: String, val date: Long, val isMe: Boolean, val isRead: Boolean, val type: Int)
