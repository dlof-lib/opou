package com.OPEN.OU.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * لوحة ألوان أوبو الحديثة — مزيج بين حيادية يوتيوب (خلفيات داكنة نظيفة، تباين عالٍ للقراءة)
 * وحيوية إنستغرام (تدرّج لوني مميز للعلامة التجارية وحلقات الصور الرمزية والأزرار الرئيسية).
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

// تدرّج العلامة التجارية على طراز إنستغرام (نستخدمه في الشعار، الحلقات، وزر النشر)
val OpouGradientStart = Color(0xFFFFC24B) // ذهبي
val OpouGradientMid = Color(0xFFE23E7E)   // وردي
val OpouGradientEnd = Color(0xFF7B3FE4)   // بنفسجي

val OpouBrandGradient = Brush.linearGradient(
    colors = listOf(OpouGradientStart, OpouGradientMid, OpouGradientEnd)
)

// أخضر أوبو الأصلي (يبقى للعلامة كلون ثانوي هادئ في العناصر غير التفاعلية)
val OpouGreen = Color(0xFF0B7A4A)
val OpouGreenLight = Color(0xFF3FA873)

// ألوان التفاعلات
val OpouStar = Color(0xFFFFC93C)      // إعجاب ⭐ -> أيقونة نجمة
val OpouBrokenHeart = Color(0xFFFF3B5C) // لم يعجبني 💔 -> أيقونة قلب مكسور
val OpouAccentBlue = Color(0xFF4E9EFF) // تعليقات
val OpouAccentGreen = Color(0xFF3FD07A) // تيك (إعادة نشر)
