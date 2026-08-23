package com.OPEN.OU.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * لوحة ألوان أوبو — مطابقة لأيقونة التطبيق الفعلية: أخضر داكن (#0B7A4A) على
 * خلفيات يوتيوب الداكنة النظيفة، بدل التدرّج البرتقالي/الوردي/البنفسجي المستخدم سابقًا.
 */

// خلفيات على طراز يوتيوب الداكن
val OpouBackground = Color(0xFF0E0F0F)
val OpouSurface = Color(0xFF181A19)
val OpouSurfaceElevated = Color(0xFF212423)
val OpouOutline = Color(0xFF2E3130)

// خلفيات الوضع الفاتح
val OpouBackgroundLight = Color(0xFFF7F8F7)
val OpouSurfaceLight = Color(0xFFFFFFFF)

// نص
val OpouTextPrimary = Color(0xFFF1F3F1)
val OpouTextSecondary = Color(0xFF9AA29D)
val OpouTextPrimaryLight = Color(0xFF14140F)
val OpouTextSecondaryLight = Color(0xFF5B6B62)

// أخضر أوبو — نفس لوني أيقونة التطبيق بالضبط
val OpouGreen = Color(0xFF0B7A4A)       // خلفية الأيقونة
val OpouGreenDark = Color(0xFF095E39)   // تظليل الأيقونة الغامق
val OpouGreenLight = Color(0xFF14A876)  // درجة أفتح لإبراز التدرّج

// تدرّج العلامة التجارية — الآن أخضر مطابق للأيقونة (كان قوس قزح إنستغرام سابقًا)
val OpouGradientStart = OpouGreenLight
val OpouGradientMid = OpouGreen
val OpouGradientEnd = OpouGreenDark

val OpouBrandGradient = Brush.linearGradient(
    colors = listOf(OpouGradientStart, OpouGradientMid, OpouGradientEnd)
)

// ألوان التفاعلات (تبقى مميزة عن اللون الأساسي لسهولة التمييز الوظيفي)
val OpouStar = Color(0xFFFFC93C)      // إعجاب ⭐ -> أيقونة نجمة
val OpouBrokenHeart = Color(0xFFFF3B5C) // لم يعجبني 💔 -> أيقونة قلب مكسور
val OpouAccentBlue = Color(0xFF4E9EFF) // تعليقات
val OpouAccentGreen = OpouGreenLight    // تيك (إعادة نشر) — نفس عائلة الأخضر الآن
