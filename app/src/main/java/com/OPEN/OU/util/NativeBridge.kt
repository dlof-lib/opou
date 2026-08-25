package com.OPEN.OU.util

import android.util.Base64
import android.util.Log

/**
 * جسر JNI إلى مكتبة أوبو الأصلية (C++) — يُستخدم لحساب نقاط الشعبية بسرعة عالية،
 * توليد بصمة (hash) للفقرات لمنع النشر المكرر السريع، وترميز/فك ترميز Base64
 * بأداء أعلى بكثير من الطبقة القياسية في Android عند التعامل مع الصور.
 *
 * كل دالة أصلية محمية بـ try/catch مع نسخة احتياطية (fallback) تعمل بلغة Kotlin
 * الخالصة، حتى لو فشل تحميل المكتبة الأصلية على جهاز أو معمارية معينة (قوة/موثوقية).
 */
object NativeBridge {

    private const val TAG = "OPOU-Native"
    var isNativeAvailable: Boolean = false
        private set

    init {
        isNativeAvailable = try {
            System.loadLibrary("opou_native")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "تعذّر تحميل المكتبة الأصلية، سيتم الاعتماد على النسخة الاحتياطية (Kotlin)", e)
            false
        }
    }

    // ---------------------------------------------------------------------
    // نقاط الدخول الأصلية (native) — قد تُرمي UnsatisfiedLinkError إن لم تُحمَّل المكتبة
    // ---------------------------------------------------------------------
    private external fun computeShaabiyaScoreNative(likes: Int, dislikes: Int, teks: Int, comments: Int): Long
    private external fun fingerprintNative(content: String): Long
    private external fun encodeBase64Native(data: ByteArray): String
    private external fun decodeBase64Native(encoded: String): ByteArray
    private external fun reactionDeltaNative(oldType: Int, newType: Int): Long
    private external fun fastByteHashNative(data: ByteArray): Long
    private external fun validateImageBytesNative(data: ByteArray): Boolean

    // ---------------------------------------------------------------------
    // واجهة عامة آمنة (Public API) — تُستخدم من بقية التطبيق
    // ---------------------------------------------------------------------

    fun computeShaabiyaScore(likes: Int, dislikes: Int, teks: Int, comments: Int): Long =
        try {
            if (isNativeAvailable) computeShaabiyaScoreNative(likes, dislikes, teks, comments)
            else fallbackShaabiyaScore(likes, dislikes, teks, comments)
        } catch (e: Throwable) {
            fallbackShaabiyaScore(likes, dislikes, teks, comments)
        }

    fun fingerprint(content: String): Long =
        try {
            if (isNativeAvailable) fingerprintNative(content) else fallbackFingerprint(content)
        } catch (e: Throwable) {
            fallbackFingerprint(content)
        }

    /**
     * ترميز مصفوفة بايتات إلى Base64 (بدون التفافات أسطر) باستخدام المكتبة الأصلية،
     * مع نسخة احتياطية تلقائية عبر android.util.Base64 عند أي فشل.
     */
    fun encodeBase64(data: ByteArray): String =
        try {
            if (isNativeAvailable) encodeBase64Native(data)
            else Base64.encodeToString(data, Base64.NO_WRAP)
        } catch (e: Throwable) {
            Log.w(TAG, "فشل الترميز الأصلي، التحويل للنسخة الاحتياطية", e)
            Base64.encodeToString(data, Base64.NO_WRAP)
        }

    /** فك ترميز نص Base64 إلى مصفوفة بايتات، مع نسخة احتياطية آمنة. */
    fun decodeBase64(encoded: String): ByteArray {
        // Image decoding stays on Android's managed Base64 path. C++ remains
        // a helper for native scoring, fingerprints and encoding, while a
        // malformed/large profile image cannot terminate the app through JNI.
        return try {
            val payload = encoded.substringAfter("base64,", encoded).trim()
            Base64.decode(payload, Base64.DEFAULT)
        } catch (e: Throwable) {
            Log.w(TAG, "فشل فك Base64، سيتم إرجاع صورة فارغة", e)
            ByteArray(0)
        }
    }

    /** فرق الشعبية لتفاعل واحد: NONE=0, LIKE=1, DISLIKE=2. */
    fun reactionDelta(oldType: Int, newType: Int): Long =
        try {
            if (isNativeAvailable) reactionDeltaNative(oldType, newType)
            else fallbackReactionDelta(oldType, newType)
        } catch (_: Throwable) {
            fallbackReactionDelta(oldType, newType)
        }

    /** بصمة سريعة للبيانات المنزلة لمنع معالجة نفس الملف مرتين. */
    fun fastByteHash(data: ByteArray): Long =
        try {
            if (isNativeAvailable) fastByteHashNative(data) else fallbackByteHash(data)
        } catch (_: Throwable) {
            fallbackByteHash(data)
        }

    /** تحقق سريع من ترويسة صور JPEG/PNG/WebP قبل تمريرها إلى BitmapFactory. */
    fun validateImageBytes(data: ByteArray): Boolean =
        try {
            if (isNativeAvailable) validateImageBytesNative(data) else fallbackValidateImage(data)
        } catch (_: Throwable) {
            fallbackValidateImage(data)
        }

    // ---------------------------------------------------------------------
    // نسخ احتياطية بلغة Kotlin خالصة (لا تعتمد على JNI إطلاقًا)
    // ---------------------------------------------------------------------

    private fun fallbackShaabiyaScore(likes: Int, dislikes: Int, teks: Int, comments: Int): Long =
        (likes.toLong() * 3) + (teks.toLong() * 5) + comments - dislikes

    private fun fallbackReactionDelta(oldType: Int, newType: Int): Long {
        fun weight(type: Int) = when (type) { 1 -> 3L; 2 -> -1L; else -> 0L }
        return weight(newType) - weight(oldType)
    }

    private fun fallbackByteHash(data: ByteArray): Long {
        var h = -3750763034362895579L
        for (b in data) h = (h xor (b.toLong() and 0xffL)) * 1099511628211L
        return h
    }

    private fun fallbackValidateImage(data: ByteArray): Boolean {
        if (data.size < 8) return false
        val png = data[0] == 0x89.toByte() && data[1] == 0x50.toByte() && data[2] == 0x4E.toByte() && data[3] == 0x47.toByte()
        val jpg = data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte()
        val webp = data.size >= 12 && data[0] == 'R'.code.toByte() && data[1] == 'I'.code.toByte() && data[2] == 'F'.code.toByte() && data[3] == 'F'.code.toByte() && data[8] == 'W'.code.toByte() && data[9] == 'E'.code.toByte() && data[10] == 'B'.code.toByte() && data[11] == 'P'.code.toByte()
        return png || jpg || webp
    }

    private fun fallbackFingerprint(content: String): Long {
        var hash = 5381L
        for (c in content) hash = ((hash shl 5) + hash) + c.code
        return hash
    }
}
