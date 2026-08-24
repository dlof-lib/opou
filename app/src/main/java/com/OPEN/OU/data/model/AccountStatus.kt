package com.OPEN.OU.data.model

/**
 * حالة الحساب:
 * - ACTIVE       : نشط.
 * - DEACTIVATED  : معطّل مؤقتًا من قبل صاحبه (يُخفى من الآخرين، ويُعاد تفعيله تلقائيًا
 *                  عند تسجيل الدخول مجددًا).
 */
enum class AccountStatus {
    ACTIVE, DEACTIVATED;

    companion object {
        fun fromValue(value: String?): AccountStatus =
            entries.firstOrNull { it.name == value } ?: ACTIVE
    }
}

/**
 * من يُسمح له بالتعليق على فقرات المستخدم:
 * - EVERYONE : الجميع.
 * - TEKERS   : المتابعون (التيكرز) فقط.
 * - NOBODY   : لا أحد (تعليقات مغلقة).
 */
enum class CommentPermission {
    EVERYONE, TEKERS, NOBODY;

    companion object {
        fun fromValue(value: String?): CommentPermission =
            entries.firstOrNull { it.name == value } ?: EVERYONE
    }
}
