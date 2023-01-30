package com.programmersbox.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

public actual fun getPlatformName(): String {
    return "JavaScript"
}

@Composable
public fun UIShow() {
    App()
}

@Composable
internal actual fun M3MaterialThemeSetup(isDarkMode: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}