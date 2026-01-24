package com.devdiaz.orderless

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devdiaz.orderless.ui.screens.BubbleScreen
import com.devdiaz.orderless.ui.theme.OrderlessTheme
import com.devdiaz.orderless.viewmodel.BubbleViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
            registerForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission granted
                } else {
                    // Permission denied
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            OrderlessTheme {
                BubbleScreen(viewModel = viewModel(factory = BubbleViewModel.Factory))
            }
        }
    }
}
