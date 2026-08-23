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
    fun decodeBase64(encoded: String): ByteArray =
        try {
            if (isNativeAvailable) decodeBase64Native(encoded)
            else Base64.decode(encoded, Base64.DEFAULT)
        } catch (e: Throwable) {
            Log.w(TAG, "فشل فك الترميز الأصلي، التحويل للنسخة الاحتياطية", e)
            try {
                Base64.decode(encoded, Base64.DEFAULT)
            } catch (inner: Throwable) {
                ByteArray(0)
            }
        }

    // ---------------------------------------------------------------------
    // نسخ احتياطية بلغة Kotlin خالصة (لا تعتمد على JNI إطلاقًا)
    // ---------------------------------------------------------------------

    private fun fallbackShaabiyaScore(likes: Int, dislikes: Int, teks: Int, comments: Int): Long =
        (likes.toLong() * 3) + (teks.toLong() * 5) + comments - dislikes

    private fun fallbackFingerprint(content: String): Long {
        var hash = 5381L
        for (c in content) hash = ((hash shl 5) + hash) + c.code
        return hash
    }
}
