package com.smsg.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
val GmBlue = Color(0xFF0B57D0)
val GmBubbleMe = Color(0xFFD3E3FD)
val GmBubbleOther = Color(0xFFF0F0F0)
val GmBg = Color(0xFFF8F9FF)
val SignalBlue = GmBlue
val SignalBubbleMe = GmBubbleMe
val SignalBubbleOther = GmBubbleOther
val SignalBg = GmBg
val WaChatBg = GmBg
val WaBubbleMe = GmBubbleMe
val WaBubbleOther = GmBubbleOther
val WaGreen = GmBlue
@Composable fun WhatsAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = GmBlue, secondary = GmBlue, background = GmBg, surface = Color.White), content = content)
}
