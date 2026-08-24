package com.OPEN.OU.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * حاوية موحّدة تجعل تخطيط الشاشات متجاوبًا مع جميع الأجهزة: على الهواتف
 * تمتد المحتوى بعرض الشاشة كاملًا كالمعتاد، أمّا على الشاشات الأوسع
 * (تابلت/أجهزة قابلة للطي/شاشات كبيرة) يُحدّ عرض المحتوى بحد أقصى مريح
 * (600dp) ويُتوسّط أفقيًا بدل تمدّده وتشوّه عناصره على كامل العرض —
 * بنفس الأسلوب المتّبع في تطبيقات التواصل الاجتماعي الكبرى.
 */
@Composable
fun ResponsiveContent(
    modifier: Modifier = Modifier,
    maxContentWidth: androidx.compose.ui.unit.Dp = 600.dp,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        val targetWidth = if (maxWidth > maxContentWidth) maxContentWidth else maxWidth
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .width(targetWidth)
                .fillMaxHeight()
        ) {
            content()
        }
    }
}
