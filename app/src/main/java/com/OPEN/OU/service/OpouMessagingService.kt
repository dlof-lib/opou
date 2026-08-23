package com.OPEN.OU.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** يستقبل إشعارات أوبو: تعليق جديد، تيك جديد (متابع)، تفاعل على فقرة، إلخ. */
class OpouMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: احفظ التوكن في /users/{uid}/fcmToken عبر UserRepository عند تسجيل الدخول
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: "OPOU"
        val body = message.notification?.body ?: ""
        // TODO: بناء وعرض إشعار نظام (NotificationCompat) بعنوان title ونص body
    }
}
