package com.OPEN.OU

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.OPEN.OU.navigation.OpouNavGraph
import com.OPEN.OU.ui.theme.OpouTheme
import com.OPEN.OU.util.LanguagePrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // يطبّق فورًا لغة الواجهة المحفوظة محليًا (عربي افتراضيًا) قبل رسم أي شاشة،
        // بحيث لا تظهر ولو للحظة الشاشة الأولى بلغة نظام الجهاز إن كانت مختلفة عن اختيار المستخدم.
        lifecycleScope.launch {
            val savedLanguage = LanguagePrefs.observe(this@MainActivity).first()
            LanguagePrefs.applyLocale(savedLanguage)
        }

        setContent {
            OpouTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OpouNavGraph()
                }
            }
        }
    }
}
