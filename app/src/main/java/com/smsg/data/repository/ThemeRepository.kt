package com.smsg.data.repository
import android.content.Context
import com.smsg.data.model.ChatThemes
import com.smsg.data.model.ChatTheme
class ThemeRepository(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("chat_themes", Context.MODE_PRIVATE)
    fun getTheme(address: String): ChatTheme {
        val id = prefs.getString("theme_${address}", "default") ?: "default"
        return ChatThemes.byId(id)
    }
    fun setTheme(address: String, id: String) {
        prefs.edit().putString("theme_${address}", id).apply()
    }
}
