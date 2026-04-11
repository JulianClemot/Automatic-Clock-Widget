package com.julian.automaticclockwidget.ui.theme

import androidx.compose.runtime.Composable
import com.julian.automaticclockwidget.designsystem.AppTheme

@Composable
fun AutomaticClockWidgetTheme(content: @Composable () -> Unit) {
    AppTheme(fontFamily = InterFontFamily, content = content)
}
