package com.OPEN.OU.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.OPEN.OU.MainActivity
import com.OPEN.OU.R
import com.OPEN.OU.data.model.Post
import timber.log.Timber

/**
 * نقطة موحّدة لبناء وعرض كل إشعارات أوبو بتصميم احترافي ومتّسق:
 * - أيقونة شريط حالة صحيحة (silhouette أبيض، وليس أيقونة التطبيق الملوّنة).
 * - لون العلامة التجارية (أخضر أوبو) على كل الأجهزة التي تدعم تلوين الإشعار.
 * - أنماط موسّعة (BigTextStyle لفقرة واحدة، InboxStyle لعدة اقتراحات) بدل سطر واحد مقتصّ.
 * - قنوات منفصلة حسب النوع، بحيث يستطيع المستخدم التحكم بكل نوع إشعار من إعدادات النظام.
 */
object NotificationDisplay {

    /** فقرة عامة جديدة من مستخدم آخر (بث لكل المستخدمين عبر FcmTopics.NEW_PARAGRAPHS) */
    private const val CHANNEL_NEW_PARAGRAPHS = "opou_new_paragraphs_channel"

    /** فقرات مقترحة لك بناءً على اهتماماتك (محسوبة محليًا على الجهاز) */
    private const val CHANNEL_SUGGESTIONS = "opou_suggestions_channel"

    /** تفاعل شخصي مباشر: تعليق جديد / تيك جديد / تفاعل على فقرتك */
    private const val CHANNEL_INTERACTIONS = "opou_interactions_channel"

    private const val GROUP_NEW_PARAGRAPHS = "opou_group_new_paragraphs"

    /** ينشئ كل قنوات الإشعارات مرة واحدة عند إقلاع التطبيق (لا تأثير إن كانت موجودة مسبقًا). */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_NEW_PARAGRAPHS,
                "فقرات جديدة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "إشعار عند نشر فقرة عامة جديدة من أي مستخدم في أوبو" },
            NotificationChannel(
                CHANNEL_SUGGESTIONS,
                "مقترحات لك",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "فقرات مقترحة بناءً على اهتماماتك داخل أوبو" },
            NotificationChannel(
                CHANNEL_INTERACTIONS,
                "تفاعلات",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "تعليق جديد، تيك جديد، أو تفاعل على إحدى فقراتك" }
        )
        channels.forEach { manager.createNotificationChannel(it) }
    }

    /** إشعار "فقرة جديدة" ببث عام — يُبنى من بيانات (data payload) رسالة FCM. */
    fun showNewParagraph(context: Context, postId: String?, authorUsername: String?, preview: String?) {
        ensureChannels(context)
        val title = if (authorUsername.isNullOrBlank()) "فقرة جديدة في أوبو" else "فقرة جديدة من $authorUsername"
        val body = preview?.takeIf { it.isNotBlank() } ?: "افتح أوبو لقراءة الفقرة الجديدة"

        val notification = baseBuilder(context, CHANNEL_NEW_PARAGRAPHS, postId)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText("أوبو • فقرة جديدة")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setGroup(GROUP_NEW_PARAGRAPHS)
            .build()

        safeNotify(context, (postId ?: title).hashCode(), notification)
    }

    /** إشعار "مقترحات لك" — يجمع حتى 3 فقرات مقترحة محسوبة محليًا في إشعار موسّع واحد. */
    fun showSuggestions(context: Context, posts: List<Post>) {
        if (posts.isEmpty()) return
        ensureChannels(context)

        val topPost = posts.first()
        val title = "فقرات مقترحة لك"
        val summary = if (posts.size == 1) {
            "بقلم ${topPost.authorUsername}"
        } else {
            "${posts.size} فقرات قد تعجبك، بدءًا من ${topPost.authorUsername}"
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("أوبو")
        posts.forEach { post ->
            val snippet = post.content.trim().let { if (it.length > 60) it.take(57) + "…" else it }
            inboxStyle.addLine("${post.authorUsername}: $snippet")
        }

        val notification = baseBuilder(context, CHANNEL_SUGGESTIONS, topPost.postId)
            .setContentTitle(title)
            .setContentText(summary)
            .setSubText("أوبو • مقترح لك")
            .setStyle(inboxStyle)
            .build()

        safeNotify(context, "suggestions".hashCode(), notification)
    }

    /** إشعار تفاعل شخصي عام (تعليق/تيك/تفاعل) — يُستخدم من OpouMessagingService لبقية الرسائل. */
    fun showInteraction(context: Context, title: String, body: String) {
        ensureChannels(context)
        val notification = baseBuilder(context, CHANNEL_INTERACTIONS, postId = null)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        safeNotify(context, System.currentTimeMillis().toInt(), notification)
    }

    /** بنّاء أساسي مشترك يضبط هوية أوبو البصرية: أيقونة صحيحة، لون العلامة، فتح التطبيق عند النقر. */
    private fun baseBuilder(context: Context, channelId: String, postId: String?): NotificationCompat.Builder {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (postId != null) putExtra(EXTRA_POST_ID, postId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            postId?.hashCode() ?: 0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_opou)
            .setColor(ContextCompat.getColor(context, R.color.opou_green))
            .setAutoCancel(true)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
    }

    private fun safeNotify(context: Context, id: Int, notification: android.app.Notification) {
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }.onFailure { Timber.tag("Notifications").w(it, "تعذّر عرض الإشعار (قد تكون الصلاحية غير ممنوحة)") }
    }

    /** مفتاح Extra يحمل معرّف الفقرة المستهدفة عند فتح التطبيق من الإشعار (لاستخدام مستقبلي في التنقل المباشر). */
    const val EXTRA_POST_ID = "opou_extra_post_id"
}
