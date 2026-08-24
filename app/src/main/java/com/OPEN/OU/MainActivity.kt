package com.OPEN.OU

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.OPEN.OU.navigation.OpouNavGraph
import com.OPEN.OU.ui.theme.OpouTheme
import com.OPEN.OU.util.LanguagePrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * مفتاح الـ extra الذي تمرّره اختصارات التطبيق الثابتة (App Shortcuts —
 * راجع res/xml/shortcuts.xml) لتحديد الشاشة المطلوب فتحها مباشرة. القيم
 * الممكنة: "new_post" / "tekers" / "account" — تتم قراءتها ومعالجتها في
 * [OpouNavGraph].
 */
const val EXTRA_SHORTCUT_ACTION = "opou_shortcut"

class MainActivity : ComponentActivity() {

    // حالة قابلة للملاحظة تحمل اختصار التطبيق الذي أُطلق منه التطبيق (إن وُجد)،
    // تُقرأ عند onCreate وتُحدَّث أيضًا عند onNewIntent (بفضل launchMode="singleTask"
    // في AndroidManifest، الذي يمنع إنشاء نسخة جديدة من النشاط عند الضغط على
    // اختصار والتطبيق يعمل بالفعل، ويستدعي onNewIntent بدلًا من ذلك).
    private val shortcutActionState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        shortcutActionState.value = intent?.getStringExtra(EXTRA_SHORTCUT_ACTION)

        // يطبّق فورًا لغة الواجهة المحفوظة محليًا (عربي افتراضيًا) قبل رسم أي شاشة،
        // بحيث لا تظهر ولو للحظة الشاشة الأولى بلغة نظام الجهاز إن كانت مختلفة عن اختيار المستخدم.
        lifecycleScope.launch {
            val savedLanguage = LanguagePrefs.observe(this@MainActivity).first()
            LanguagePrefs.applyLocale(savedLanguage)
        }

        setContent {
            OpouTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val pendingShortcutAction by shortcutActionState
                    OpouNavGraph(
                        pendingShortcutAction = pendingShortcutAction,
                        onShortcutConsumed = { shortcutActionState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutActionState.value = intent.getStringExtra(EXTRA_SHORTCUT_ACTION)
    }
}
