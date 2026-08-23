package com.OPEN.OU.service

import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.notifications.NotificationDisplay
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * يستقبل إشعارات أوبو ويوجّهها للتصميم الاحترافي المناسب في [NotificationDisplay]:
 * - type = "new_paragraph" : فقرة عامة جديدة (بث لكل المستخدمين عبر FcmTopics.NEW_PARAGRAPHS).
 * - غير ذلك (تعليق جديد / تيك جديد / تفاعل على فقرة) : إشعار تفاعل شخصي مباشر.
 * (إشعارات "مقترحات لك" لا تمر من هنا؛ تُبنى وتُعرض محليًا بالكامل عبر SuggestionsWorker
 * لأنها توصيات محسوبة على الجهاز ولا تحتاج شبكة FCM إطلاقًا.)
 */
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
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "OPOU"
        val body = message.notification?.body ?: data["body"] ?: ""

        when (data["type"]) {
            "new_paragraph" -> NotificationDisplay.showNewParagraph(
                context = this,
                postId = data["postId"],
                authorUsername = data["authorUsername"],
                preview = data["preview"] ?: body
            )
            else -> NotificationDisplay.showInteraction(this, title, body)
        }
    }
}
