package com.programmersbox.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

public actual fun getPlatformName(): String {
    return "Desktop"
}

@Composable
public fun UIShow() {
    App()
}

@Composable
internal actual fun M3MaterialThemeSetup(isDarkMode: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme(), content = content)
}