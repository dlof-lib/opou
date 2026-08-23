package com.OPEN.OU.network

import com.OPEN.OU.BuildConfig
import com.OPEN.OU.notifications.FcmTopics
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * نقطة الاتصال الوحيدة بين تطبيق Kotlin وخادم PHP المساعد (server/php).
 *
 * سابقًا لم يكن هناك أي رابط فعلي بين التطبيق وهذا الخادم رغم وجوده جاهزًا،
 * مما كان يترك مسارات مهمة (إشعارات FCM) بلا استخدام. معالجة الصور (Base64) لم
 * تعد تمر عبر PHP إطلاقًا وأصبحت كاملة داخل التطبيق عبر Kotlin + C++ (JNI) —
 * راجع util/ImageCodec.kt وutil/NativeBridge.kt وapp/src/main/cpp/base64.cpp.
 * هذا الملف يبني عميل Retrofit حقيقي، يُرفق دومًا Firebase ID Token الحالي،
 * ويعرض واجهة بسيطة (PhpBridgeRepository) يمكن لأي ViewModel استدعاؤها بأمان
 * (فشل الشبكة هنا لا يجب أبدًا أن يوقف تدفق Firebase الأساسي — لذلك هو Best-effort).
 */
object PhpApiClient {

    private val gson: Gson = GsonBuilder().setLenient().create()

    private val loggingInterceptor = HttpLoggingInterceptor { message -> Timber.tag("PHP-API").d(message) }.apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.PHP_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val service: PhpApiService by lazy { retrofit.create(PhpApiService::class.java) }
}

/**
 * يوفّر عمليات آمنة (لا ترمي أبدًا) تربط أحداث Firebase (تيك جديد، تعليق جديد)
 * بخادم PHP لإرسال إشعارات FCM فعلية عبر notify.php، بعد إرفاق Firebase ID Token
 * حقيقي يتحقق منه الخادم بنفسه (راجع firebase_auth.php) — وليس مجرد ثقة عمياء بالعميل.
 */
class PhpBridgeRepository(
    private val service: PhpApiService = PhpApiClient.service,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    /** يجلب Firebase ID Token الحالي منسّقًا كـ "Bearer ...", أو null إن لم يوجد مستخدم مسجّل. */
    private suspend fun bearerTokenOrNull(): String? = runCatching {
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: return null
        "Bearer $token"
    }.onFailure { Timber.tag("PHP-API").w(it, "تعذّر جلب Firebase ID Token") }.getOrNull()

    /**
     * يرسل إشعارًا لمستخدم آخر عبر PHP + FCM. Best-effort بالكامل:
     * أي فشل (شبكة، مفتاح FCM غير مهيأ على الخادم، إلخ) يُسجَّل فقط عبر Timber
     * ولا يُعيد استثناءً أبدًا، حتى لا يتأثر تدفق Firebase الأساسي (تيك/تعليق) بذلك.
     */
    suspend fun notifyBestEffort(targetFcmToken: String, title: String, body: String) {
        if (targetFcmToken.isBlank()) return
        val token = bearerTokenOrNull() ?: return
        runCatching {
            service.notify(token, NotifyRequest(targetToken = targetFcmToken, title = title, body = body))
        }.onFailure { Timber.tag("PHP-API").w(it, "فشل إرسال إشعار عبر notify.php") }
    }

    /**
     * يبث إشعار "فقرة جديدة" لكل مستخدمي التطبيق دفعة واحدة عبر موضوع
     * [FcmTopics.NEW_PARAGRAPHS]، بعد نشر فقرة عامة (PUBLIC) بنجاح.
     * Best-effort بالكامل: فشل الشبكة هنا لا يجب أبدًا أن يمنع نشر الفقرة فعليًا،
     * لذلك يُستدعى دائمًا بعد نجاح createPost ولا يرمي استثناءً مطلقًا.
     */
    suspend fun notifyNewParagraphBestEffort(
        postId: String,
        authorUsername: String,
        contentPreview: String
    ) {
        val token = bearerTokenOrNull() ?: return
        val safePreview = contentPreview.trim().let {
            if (it.length > 120) it.take(117) + "…" else it
        }
        runCatching {
            service.notify(
                token,
                NotifyRequest(
                    topic = FcmTopics.NEW_PARAGRAPHS,
                    title = "فقرة جديدة من $authorUsername",
                    body = safePreview.ifBlank { "افتح أوبو لقراءة الفقرة الجديدة" },
                    type = "new_paragraph",
                    postId = postId,
                    authorUsername = authorUsername,
                    preview = safePreview
                )
            )
        }.onFailure { Timber.tag("PHP-API").w(it, "فشل بث إشعار الفقرة الجديدة عبر notify.php") }
    }
}
