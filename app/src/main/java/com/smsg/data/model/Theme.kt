package com.smsg.data.model
import androidx.compose.ui.graphics.Color
data class ChatTheme(val id: String, val name: String, val me: Color, val other: Color, val bg: Color, val avatar: Color)
object ChatThemes {
    val Default = ChatTheme("default","Défaut", Color(0xFF6750A4), Color(0xFFE8DEF8), Color(0xFFFFFBFE), Color(0xFF6750A4))
    val Ocean = ChatTheme("ocean","Océan", Color(0xFF0061A4), Color(0xFFD1E4FF), Color(0xFFFAFCFF), Color(0xFF0061A4))
    val Forest = ChatTheme("forest","Forêt", Color(0xFF006D3A), Color(0xFF9CF7B6), Color(0xFFFBFFFB), Color(0xFF006D3A))
    val Sunset = ChatTheme("sunset","Sunset", Color(0xFF9C4300), Color(0xFFFFDBCA), Color(0xFFFFFBFF), Color(0xFF9C4300))
    val Lavender = ChatTheme("lavender","Lavande", Color(0xFF6D5DD6), Color(0xFFE4DDFF), Color(0xFFFFFBFF), Color(0xFF6D5DD6))
    val Rose = ChatTheme("rose","Rose", Color(0xFF9D3A64), Color(0xFFFFD8E4), Color(0xFFFFFBFF), Color(0xFF9D3A64))
    val all = listOf(Default,Ocean,Forest,Sunset,Lavender,Rose)
    fun byId(id: String) = all.find { it.id == id } ?: Default
}
