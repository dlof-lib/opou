package com.OPEN.OU.network

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** جسم طلب ضغط صورة Base64 عبر server/php/base64_image.php */
data class Base64ImageRequest(val base64: String)

/** استجابة ضغط الصورة القادمة من base64_image.php */
data class Base64ImageResponse(
    val success: Boolean = false,
    val error: String? = null,
    val base64: String? = null,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val byteSize: Int? = null,
    val quality: Int? = null
)

/** استجابة رفع الصورة الرمزية عبر server/php/upload_avatar.php */
data class UploadAvatarResponse(
    val success: Boolean = false,
    val error: String? = null,
    val filename: String? = null,
    val url: String? = null
)

/** جسم طلب إرسال إشعار عبر server/php/notify.php */
data class NotifyRequest(
    val targetToken: String,
    val title: String,
    val body: String
)

/** استجابة إرسال الإشعار */
data class NotifyResponse(
    val success: Boolean = false,
    val error: String? = null
)

/**
 * واجهة Retrofit التي تصف نقاط نهاية خادم أوبو المساعد بلغة PHP (server/php/*).
 * هذه هي "الجسر" الفعلي بين تطبيق Kotlin وخادم PHP: كل استدعاء يُرفق رمز
 * Firebase ID Token الخاص بالمستخدم الحالي (Authorization: Bearer ...)، والذي
 * يتحقق منه firebase_auth.php فعليًا (توقيع RS256 + iss + aud + exp)، بدل
 * الاعتماد فقط على قواعد Firebase من جهة العميل.
 */
interface PhpApiService {

    /** يرسل إشعار FCM لمستخدم آخر (تعليق جديد / تيك جديد / تفاعل) عبر notify.php. */
    @POST("server/php/notify.php")
    suspend fun notify(
        @Header("Authorization") bearerToken: String,
        @Body request: NotifyRequest
    ): NotifyResponse

    /** يضغط صورة Base64 من جهة الخادم كطبقة تحقق/ضغط ثانية (Defense in Depth). */
    @POST("server/php/base64_image.php")
    suspend fun compressBase64Image(
        @Header("Authorization") bearerToken: String,
        @Body request: Base64ImageRequest
    ): Base64ImageResponse

    /** يرفع صورة أفتار/بانر إلى الخادم (بديل اختياري عن التخزين المباشر Base64). */
    @Multipart
    @POST("server/php/upload_avatar.php")
    suspend fun uploadAvatar(
        @Header("Authorization") bearerToken: String,
        @Part image: MultipartBody.Part
    ): UploadAvatarResponse
}
