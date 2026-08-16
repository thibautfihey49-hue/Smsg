package com.smsg.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
val WaGreen = Color(0xFF128C7E)
val WaLightGreen = Color(0xFF25D366)
val WaDark = Color(0xFF111B21)
val WaChatBg = Color(0xFFE5DDD5)
val WaBubbleMe = Color(0xFFDCF8C6)
val WaBubbleOther = Color.White
@Composable
fun WhatsAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = WaGreen, secondary = WaLightGreen, background = Color(0xFFF0F2F5)),
        content = content
    )
}
