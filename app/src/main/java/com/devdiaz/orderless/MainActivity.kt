package com.devdiaz.orderless

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.devdiaz.orderless.ui.screens.BubbleScreen
import com.devdiaz.orderless.ui.theme.OrderlessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OrderlessTheme { BubbleScreen() } }
    }
}
