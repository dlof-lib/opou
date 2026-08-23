package com.OPEN.OU.network

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** استجابة رفع الصورة الرمزية عبر server/php/upload_avatar.php */
data class UploadAvatarResponse(
    val success: Boolean = false,
    val error: String? = null,
    val filename: String? = null,
    val url: String? = null
)

/**
 * جسم طلب إرسال إشعار عبر server/php/notify.php.
 * يُرسل إما لجهاز واحد (targetToken) أو لكل المشتركين في موضوع بث (topic) — أحدهما مطلوب.
 * حقول type/postId/authorUsername/preview بيانات إضافية (data payload) تُستخدم في
 * OpouMessagingService لبناء إشعار غني (BigTextStyle، لون العلامة، فتح الفقرة المعنية...).
 */
data class NotifyRequest(
    val targetToken: String? = null,
    val topic: String? = null,
    val title: String,
    val body: String,
    val type: String? = null,
    val postId: String? = null,
    val authorUsername: String? = null,
    val preview: String? = null
)

/** استجابة إرسال الإشعار */
data class NotifyResponse(
    val success: Boolean = false,
    val error: String? = null
)

/**
 * واجهة Retrofit التي تصف نقاط نهاية خادم أوبو المساعد بلغة PHP (مجلد server/php).
 * هذه هي "الجسر" الفعلي بين تطبيق Kotlin وخادم PHP: كل استدعاء يُرفق رمز
 * Firebase ID Token الخاص بالمستخدم الحالي (Authorization: Bearer ...)، والذي
 * يتحقق منه firebase_auth.php فعليًا (توقيع RS256 + iss + aud + exp)، بدل
 * الاعتماد فقط على قواعد Firebase من جهة العميل.
 *
 * ملاحظة: معالجة الصور (ترميز/فك ترميز/ضغط Base64) لم تعد تمر عبر PHP إطلاقًا؛
 * أصبحت تعتمد بالكامل على الطبقة الأصلية Kotlin + C++ (راجع ImageCodec.kt،
 * NativeBridge.kt، وapp/src/main/cpp/base64.cpp)، وبذلك أصبح هذا الخادم مسؤولاً
 * فقط عن الإشعارات (notify.php) والرفع الاختياري لملف خام (upload_avatar.php).
 */
interface PhpApiService {

    /** يرسل إشعار FCM لمستخدم آخر (تعليق جديد / تيك جديد / تفاعل) عبر notify.php. */
    @POST("server/php/notify.php")
    suspend fun notify(
        @Header("Authorization") bearerToken: String,
        @Body request: NotifyRequest
    ): NotifyResponse

    /** يرفع صورة أفتار/بانر إلى الخادم (بديل اختياري عن التخزين المباشر Base64). */
    @Multipart
    @POST("server/php/upload_avatar.php")
    suspend fun uploadAvatar(
        @Header("Authorization") bearerToken: String,
        @Part image: MultipartBody.Part
    ): UploadAvatarResponse
}
