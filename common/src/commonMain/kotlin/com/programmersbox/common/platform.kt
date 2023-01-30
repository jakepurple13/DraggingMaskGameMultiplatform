package com.programmersbox.common

import androidx.compose.runtime.Composable

public expect fun getPlatformName(): String

@Composable
internal expect fun M3MaterialThemeSetup(isDarkMode: Boolean, content: @Composable () -> Unit)