package com.OPEN.OU

import android.app.Application
import com.OPEN.OU.notifications.FcmTopics
import com.OPEN.OU.notifications.NotificationDisplay
import com.OPEN.OU.notifications.SuggestionsWorker
import com.OPEN.OU.util.CrashReporter
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class OpouApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // أول شيء دائمًا: قبل أي تهيئة أخرى قد تفشل هي نفسها، حتى نلتقط انهيارها أيضًا.
        CrashReporter.install(this)

        FirebaseApp.initializeApp(this)
        // تفعيل الاستمرارية دون اتصال (Offline persistence) لقاعدة Realtime Database
        runCatching { FirebaseDatabase.getInstance().setPersistenceEnabled(true) }

        // تجهيز قنوات الإشعارات مسبقًا (فقرات جديدة / مقترحات / تفاعلات) بتصميم موحّد
        NotificationDisplay.ensureChannels(this)

        // اشتراك الجهاز في موضوع بث "فقرة جديدة" فور الإقلاع، ليصل الإشعار لكل المستخدمين
        FcmTopics.subscribeToAll()

        // جدولة عامل "فقرات مقترحة لك" الدوري (توصيات محلية، كل 12 ساعة)
        SuggestionsWorker.schedule(this)

        // تحميل مكتبة أوبو الأصلية (C++) يتم تلقائيًا عبر NativeBridge عند أول استخدام
        // (راجع util/NativeBridge.kt) — لا حاجة لتحميلها هنا مرة أخرى.
    }
}
