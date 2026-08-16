package com.smsg.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
val SignalBlue = Color(0xFF2C6BED)
val SignalBlueLight = Color(0xFF3A76F0)
val SignalBubbleMe = Color(0xFF2C6BED)
val SignalBubbleOther = Color(0xFFE9E9EB)
val SignalBg = Color(0xFFF6F6F6)
val WaChatBg = Color(0xFFE5DDD5)
val WaBubbleMe = SignalBubbleMe
val WaBubbleOther = SignalBubbleOther
val WaGreen = SignalBlue
@Composable fun WhatsAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = SignalBlue, secondary = SignalBlueLight, background = SignalBg), content = content)
}
