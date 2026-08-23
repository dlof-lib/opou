package com.OPEN.OU.util

import androidx.compose.ui.graphics.Color

/** يحوّل نص Hex (مثل "#0B7A4A") إلى Color بأمان، أو يعيد null إذا كان فارغًا/غير صالح. */
fun String.toColorOrNull(): Color? {
    if (isBlank()) return null
    return runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrNull()
}

/** لوحة ألوان جاهزة مقترحة لخلفية الفقرة/النص — سريعة الاختيار من واجهة إنشاء الفقرة. */
object ParagraphColorPalette {
    val PRESETS: List<String> = listOf(
        "#0B7A4A", // أخضر أوبو
        "#14A876",
        "#4E9EFF", // أزرق
        "#FFC93C", // أصفر/نجمة
        "#FF3B5C", // أحمر
        "#9B59B6", // بنفسجي
        "#212423", // رمادي داكن
        "#F7F8F7"  // فاتح جدًا
    )
}
