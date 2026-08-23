package com.OPEN.OU.notifications

import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber

/**
 * مواضيع (Topics) بث FCM المستخدمة في أوبو.
 *
 * البث عبر Topic هو الطريقة القياسية لإرسال إشعار واحد "لكل المستخدمين" دفعة واحدة
 * دون الحاجة لتخزين/تكرار الحلقات على آلاف توكنات FCM الفردية من جهة الخادم.
 * كل جهاز يشترك تلقائيًا في هذه المواضيع عند إقلاع التطبيق (راجع OpouApplication).
 */
object FcmTopics {
    /** يُبث عند نشر أي فقرة عامة (PUBLIC) جديدة — يصل لكل مستخدمي التطبيق. */
    const val NEW_PARAGRAPHS = "opou_new_paragraphs"

    /**
     * يشترك الجهاز الحالي في كل مواضيع البث العامة، بأمان (Best-effort):
     * فشل الاشتراك (لا يوجد إنترنت مثلًا) لا يجب أن يوقف إقلاع التطبيق أبدًا.
     */
    fun subscribeToAll() {
        listOf(NEW_PARAGRAPHS).forEach { topic ->
            runCatching {
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnFailureListener { Timber.tag("FCM-Topics").w(it, "فشل الاشتراك في الموضوع: $topic") }
            }.onFailure { Timber.tag("FCM-Topics").w(it, "تعذّر بدء الاشتراك في الموضوع: $topic") }
        }
    }
}
