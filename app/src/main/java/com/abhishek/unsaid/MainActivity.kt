package com.abhishek.unsaid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.abhishek.unsaid.ui.theme.UnsaidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // There should only be ONE setContent block
        setContent {
            UnsaidTheme {
                // We call our navigation here directly
                UnsaidNavigation()
            }
        }
    }
}