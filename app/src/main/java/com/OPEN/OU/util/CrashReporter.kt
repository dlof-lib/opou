package com.OPEN.OU.util

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * معالج شامل للأخطاء غير المُمسكة: بدل أن يُغلق النظام التطبيق فجأة ويعرض
 * حوار "حدث خطأ" العام (بدون أي تفاصيل تقنية)، يلتقط هذا المعالج الاستثناء
 * الفعلي بنص وصفه الكامل (Stack Trace) ويعرضه في شاشة بسيطة يمكن قراءتها
 * أو أخذ لقطة شاشة/نسخ نصها مباشرة — دون الحاجة لـ adb logcat.
 *
 * التفعيل: استدعِ [CrashReporter.install] مرة واحدة في [Application.onCreate].
 */
object CrashReporter {

    fun install(app: Application) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = throwable.stackTraceToString()
                val intent = Intent(app, CrashDetailsActivity::class.java).apply {
                    putExtra(CrashDetailsActivity.EXTRA_TRACE, trace)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                app.startActivity(intent)
            } catch (_: Throwable) {
                // إن فشل حتى عرض شاشة الخطأ، لا نُخفي الاستثناء الأصلي — نُسلّمه للمعالج
                // الافتراضي (نفس السلوك القديم: حوار النظام العام) بدل تعليق التطبيق بصمت.
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
                    ?: run {
                        android.os.Process.killProcess(android.os.Process.myPid())
                        kotlin.system.exitProcess(1)
                    }
            }
        }
    }
}

/** شاشة بسيطة (View قديم عمدًا، بلا Compose) تعرض نص الانهيار كاملًا قابلًا للتحديد والنسخ. */
class CrashDetailsActivity : Activity() {
    companion object {
        const val EXTRA_TRACE = "trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent?.getStringExtra(EXTRA_TRACE).orEmpty()

        val textView = TextView(this).apply {
            text = "حدث خطأ غير متوقع — انسخ النص التالي وأرسله:\n\n$trace"
            setTextIsSelectable(true)
            setPadding(32, 64, 32, 64)
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@CrashDetailsActivity, android.R.color.white))
            setBackgroundColor(ContextCompat.getColor(this@CrashDetailsActivity, android.R.color.black))
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@CrashDetailsActivity, android.R.color.black))
            addView(textView)
        })
    }
}
