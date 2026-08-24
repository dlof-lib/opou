package com.OPEN.OU.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// مقاسات مصغّرة ومضغوطة أقرب لتطبيقات التواصل الاجتماعي المعروفة (بدل الخط
// الكبير السابق)، مع الحفاظ على تباين وزن الخط لسهولة القراءة.
val OpouTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
)
