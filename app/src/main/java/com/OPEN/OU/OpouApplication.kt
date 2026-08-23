package com.OPEN.OU

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class OpouApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // تفعيل الاستمرارية دون اتصال (Offline persistence) لقاعدة Realtime Database
        runCatching { FirebaseDatabase.getInstance().setPersistenceEnabled(true) }

        // تحميل مكتبة أوبو الأصلية (C++) يتم تلقائيًا عبر NativeBridge عند أول استخدام
        // (راجع util/NativeBridge.kt) — لا حاجة لتحميلها هنا مرة أخرى.
    }
}
