package com.OPEN.OU.data.model

/**
 * إيموجي الفقرة — ميزة "قيد التطوير": يتوفر حاليًا 3 إيموجيات فقط قابلة للاختيار
 * لتعليق مزاج/طابع الفقرة (تُعرض بجانب اسم الناشر في بطاقة الفقرة).
 * سيتم توسيع القائمة لاحقًا.
 */
object ParagraphEmoji {
    /** القيمة الفارغة = بلا إيموجي */
    const val NONE = ""

    /** القائمة المتاحة حاليًا (3 فقط — الميزة قيد التطوير) */
    val AVAILABLE: List<String> = listOf("🔥", "💬", "✨")

    fun isValid(emoji: String): Boolean = emoji.isBlank() || emoji in AVAILABLE
}
