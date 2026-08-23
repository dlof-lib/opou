package com.OPEN.OU.data.model

/**
 * مستوى خصوصية الفقرة عند النشر:
 * - PUBLIC  : عام — يظهر للجميع في التغذية.
 * - PRIVATE : خاص — يظهر لصاحب الفقرة فقط.
 * - LIMITED : محدود — يظهر لصاحب الفقرة + التيكرز (متابعوه) فقط.
 * - CUSTOM  : مخصّص — يظهر لصاحب الفقرة + قائمة مستخدمين محدَّدة (allowedViewerIds في Post).
 *
 * يُخزَّن كنص (name) داخل Realtime Database لسهولة القراءة والتوافق مع الإصدارات القديمة
 * (الفقرات القديمة بلا حقل privacy تُعامَل تلقائيًا كـ PUBLIC عبر القيمة الافتراضية في Post).
 */
enum class ParagraphPrivacy {
    PUBLIC, PRIVATE, LIMITED, CUSTOM;

    companion object {
        fun fromValue(value: String?): ParagraphPrivacy =
            entries.firstOrNull { it.name == value } ?: PUBLIC
    }
}
