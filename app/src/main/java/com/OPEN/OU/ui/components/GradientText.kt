package com.OPEN.OU.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import com.OPEN.OU.ui.theme.OpouBrandGradient

/**
 * نص يُرسم بتدرّج ألوان العلامة التجارية (ذهبي → وردي → بنفسجي) بدل لون واحد مسطّح.
 * يُستخدم في المواضع التي نريد فيها إبراز هوية "أوبو" البصرية: شعار الشريط العلوي،
 * اسم المستخدم في صفحة الحساب، عناوين شاشات الترحيب... إلخ.
 */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    gradient: Brush = OpouBrandGradient
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(brush = gradient)
    )
}
