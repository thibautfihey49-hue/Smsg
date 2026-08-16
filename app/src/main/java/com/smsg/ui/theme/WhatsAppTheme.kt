package com.smsg.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
val WaGreen = Color(0xFF128C7E); val WaBubbleMe = Color(0xFFDCF8C6); val WaBubbleOther = Color.White; val WaChatBg = Color(0xFFE5DDD5)
@Composable fun WhatsAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = WaGreen, secondary = WaGreen), content = content)
}
