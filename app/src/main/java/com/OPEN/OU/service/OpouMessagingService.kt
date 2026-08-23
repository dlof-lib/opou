package com.OPEN.OU.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.OPEN.OU.R
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

private const val CHANNEL_ID = "opou_default_channel"

/** يستقبل إشعارات أوبو: تعليق جديد، تيك جديد (متابع)، تفاعل على فقرة، إلخ. */
class OpouMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val userRepo = UserRepository()
    private val authRepo = AuthRepository()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // يحفظ التوكن الجديد في /users/{uid}/fcmToken فور توفر مستخدم مسجّل دخول
        val uid = authRepo.currentUserId ?: return
        serviceScope.launch {
            runCatching { userRepo.saveFcmToken(uid, token) }
                .onFailure { Timber.tag("FCM").w(it, "فشل حفظ رمز FCM") }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "OPOU"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        showSystemNotification(title, body)
    }

    private fun showSystemNotification(title: String, body: String) {
        ensureChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(this)
                .notify(System.currentTimeMillis().toInt(), notification)
        }.onFailure { Timber.tag("FCM").w(it, "تعذّر عرض الإشعار (قد تكون صلاحية الإشعارات غير ممنوحة)") }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val existing = manager?.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "OPOU",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager?.createNotificationChannel(channel)
            }
        }
    }
}
