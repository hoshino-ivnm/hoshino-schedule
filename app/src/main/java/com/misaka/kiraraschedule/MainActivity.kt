package com.misaka.kiraraschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import com.misaka.kiraraschedule.ui.KiraraScheduleApp
import com.misaka.kiraraschedule.ui.theme.KirarascheduleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KirarascheduleTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    KiraraScheduleApp()
                }
            }
        }
    }
}
