package com.OPEN.OU.util

import java.security.MessageDigest

/**
 * تجزئة أحادية الاتجاه (SHA-256) لرمز PIN الخاص بالتحقق بخطوتين، بحيث لا يُخزَّن
 * الرمز نفسه أبدًا داخل Realtime Database — فقط بصمته. مربوطة برمز المستخدم (uid)
 * كـ "ملح" (Salt) بسيط لمنع مقارنة البصمات المتطابقة بين حسابين مختلفين.
 */
object PinHash {
    fun hash(uid: String, pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("opou:$uid:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun matches(uid: String, pin: String, storedHash: String): Boolean =
        storedHash.isNotBlank() && hash(uid, pin) == storedHash
}
