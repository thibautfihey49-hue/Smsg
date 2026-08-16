package com.smsg.ui.theme
import android.os.Build; import androidx.compose.foundation.isSystemInDarkTheme; import androidx.compose.material3.*; import androidx.compose.runtime.Composable; import androidx.compose.ui.graphics.Color; import androidx.compose.ui.platform.LocalContext
@Composable
fun SmsgTheme(dark: Boolean = isSystemInDarkTheme(), dyn: Boolean = true, content: @Composable () -> Unit) {
    val scheme = when { dyn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { val ctx = LocalContext.current; if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx) }; dark -> darkColorScheme(primary = Color(0xFFA8C7FA), background = Color(0xFF131314)); else -> lightColorScheme(primary = Color(0xFF0B57D0), primaryContainer = Color(0xFFD3E3FD), background = Color(0xFFFAF8FF)) }
    MaterialTheme(colorScheme = scheme, content = content)
}
