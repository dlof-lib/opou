package com.OPEN.OU

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.OPEN.OU.navigation.OpouNavGraph
import com.OPEN.OU.ui.theme.OpouTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpouTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OpouNavGraph()
                }
            }
        }
    }
}
