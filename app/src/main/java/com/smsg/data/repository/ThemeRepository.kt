package com.smsg.data.repository
import android.content.Context
import androidx.compose.ui.graphics.Color
import com.smsg.ui.theme.SignalBlue
data class Theme(val id: String, val name: String, val bg: Color, val me: Color, val other: Color, val avatar: Color, val accent: Color)
class ThemeRepository(ctx: Context) {
    fun getTheme(address: String) = Theme("signal","Signal", Color(0xFFF6F6F6), SignalBlue, Color.White, SignalBlue, SignalBlue)
    fun setTheme(a: String, id: String) {}
}
