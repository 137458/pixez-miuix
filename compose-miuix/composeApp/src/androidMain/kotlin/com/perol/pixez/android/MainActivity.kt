package com.perol.pixez.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.perol.pixez.PixEzApp

/**
 * Android 应用入口。
 * 使用 ComponentActivity + setContent 承载 Compose Multiplatform 应用。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixEzApp()
        }
    }
}

@Preview
@Composable
private fun PixEzAppPreview() {
    PixEzApp()
}
