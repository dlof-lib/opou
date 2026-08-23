package com.OPEN.OU.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * نظام تدوير زوايا موحّد لكل التطبيق — بدل الأرقام المتفرقة (16.dp, 20.dp...)
 * المستخدمة سابقًا في كل شاشة على حدة. أي مكوّن يعتمد على MaterialTheme.shapes
 * يتبع تلقائيًا نفس لغة التصميم.
 */
val OpouShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
