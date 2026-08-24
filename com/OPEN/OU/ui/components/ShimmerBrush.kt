package com.OPEN.OU.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.OPEN.OU.ui.theme.OpouGreenLight
import com.OPEN.OU.ui.theme.OpouSurfaceElevated

/**
 * فرشاة "التحميل الهيكلي" (Skeleton) الخاصة بهوية أوبو البصرية.
 *
 * بدل الوميض الرمادي الأحادي الشائع في معظم التطبيقات (شريط أبيض شفاف
 * يعبر أفقيًا فوق خلفية رمادية)، نستخدم هنا:
 *
 *  1) مسحًا **قطريًا** (وليس أفقيًا بحتًا) بدرجة أخضر أوبو الفاتح
 *     [OpouGreenLight] بشفافية منخفضة — فيحمل الوميض هوية العلامة
 *     التجارية بدل أن يكون رماديًا محايدًا كأي تطبيق آخر.
 *  2) إزاحة زمنية (`staggerIndex`) تُبطئ بدء الحركة لكل عنصر لاحق في
 *     القائمة، فتظهر البطاقات وكأنها "تستيقظ" الواحدة تلو الأخرى بموجة
 *     منسابة من الأعلى للأسفل، بدل أن تومض جميعها معًا بشكل رتيب متزامن.
 *
 * النتيجة: تحميل هيكلي مميز وقابل للتعرّف عليه كجزء من هوية أوبو، وليس
 * مجرد نسخة مكرّرة من مكتبة Shimmer الجاهزة.
 */
@Composable
fun rememberOpouShimmerBrush(staggerIndex: Int = 0): Brush {
    val transition = rememberInfiniteTransition(label = "opou_shimmer_wave")

    val translate by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1300,
                delayMillis = staggerIndex * 110,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "opou_shimmer_translate"
    )

    val base = OpouSurfaceElevated
    val glow = OpouGreenLight.copy(alpha = 0.22f)

    // مسح قطري: المحور y يتحرك بنصف سرعة المحور x فيعطي زاوية ميل لطيفة
    // بدل خط أفقي/رأسي صريح.
    return Brush.linearGradient(
        colors = listOf(base, glow, base),
        start = Offset(translate - 500f, (translate - 500f) * 0.5f),
        end = Offset(translate, translate * 0.5f)
    )
}
