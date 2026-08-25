package com.OPEN.OU.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.OPEN.OU.ui.theme.OpouGreen
import com.OPEN.OU.ui.theme.OpouGreenLight

/**
 * ============================================================
 *  نظام "التحميل الهيكلي" (Skeleton Loading) الخاص بأوبو
 * ============================================================
 * تصميم مميّز ومختلف عن الشيمر الرمادي التقليدي المستخدم في معظم التطبيقات:
 * - تدرّج بلون العلامة التجارية (أخضر أوبو) يمسح قطريًا فوق كتلة رمادية هادئة،
 *   بدل الشيمر الأبيض/الرمادي العام.
 * - "نبض" خفيف إضافي (Pulse) على الشفافية يمنح إحساسًا بالحيوية دون إزعاج.
 * - ظهور متدرّج (Staggered Reveal): كل عنصر في القائمة يبدأ مسحته بتأخير بسيط
 *   عن الذي قبله، فتبدو القائمة "تتنفّس" من الأعلى للأسفل بدل الوميض الموحّد
 *   الرتيب الذي تراه في كل مكان.
 * - أشكال الهيكل مطابقة تمامًا لمقاسات المكوّنات الحقيقية (PostCard، بطاقة
 *   الغرفة، صف تيكر...) حتى لا تقفز الواجهة عند وصول البيانات الفعلية.
 */

/** Brush متحرك بمسحة قطرية بلون أوبو الأخضر فوق قاعدة رمادية هادئة، مع إزاحة أفقية للحركة. */
@Composable
private fun opouShimmerBrush(widthPx: Float, sweepOffset: Float): Brush {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val highlight = OpouGreenLight.copy(alpha = 0.22f)
    val highlightCore = OpouGreen.copy(alpha = 0.30f)
    return Brush.linearGradient(
        colors = listOf(base, highlight, highlightCore, highlight, base),
        start = Offset(sweepOffset - widthPx, 0f),
        end = Offset(sweepOffset, widthPx)
    )
}

/**
 * لبنة الهيكل الأساسية: مستطيل (أو دائرة) بحركة مسح قطرية + نبض شفافية،
 * بتأخير بدء [staggerIndex] يضربه [staggerDelayMs] لخلق تأثير الظهور المتدرّج.
 */
@Composable
fun OpouSkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    staggerIndex: Int = 0,
    staggerDelayMs: Int = 70
) {
    val transition = rememberInfiniteTransition(label = "opou_skeleton")

    val sweep by transition.animateFloat(
        initialValue = -400f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, delayMillis = staggerIndex * staggerDelayMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, delayMillis = staggerIndex * staggerDelayMs, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .alpha(pulse)
            .clip(shape)
            .background(opouShimmerBrush(widthPx = 260f, sweepOffset = sweep))
    )
}

/** سطر نص هيكلي بعرض واحتفال محدَّدين — يُستخدم لمحاكاة الأسماء والفقرات. */
@Composable
fun OpouSkeletonLine(
    modifier: Modifier = Modifier.fillMaxWidth(),
    height: Dp = 12.dp,
    staggerIndex: Int = 0
) {
    OpouSkeletonBlock(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(50),
        staggerIndex = staggerIndex
    )
}

/** دائرة هيكلية — للصور الرمزية. */
@Composable
fun OpouSkeletonCircle(size: Dp, staggerIndex: Int = 0) {
    OpouSkeletonBlock(
        modifier = Modifier.size(size),
        shape = CircleShape,
        staggerIndex = staggerIndex
    )
}

/**
 * هيكل بطاقة فقرة كاملة — يحاكي PostCard تمامًا (رأس بصورة رمزية + اسم +
 * وقت، سطرا محتوى، صف تفاعلات) حتى لا تتحرك الواجهة عند استبداله بالبطاقة
 * الحقيقية.
 */
@Composable
fun OpouPostCardSkeleton(staggerIndex: Int = 0) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OpouSkeletonCircle(size = 38.dp, staggerIndex = staggerIndex)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OpouSkeletonLine(modifier = Modifier.fillMaxWidth(0.32f), height = 12.dp, staggerIndex = staggerIndex)
                OpouSkeletonLine(modifier = Modifier.fillMaxWidth(0.22f), height = 9.dp, staggerIndex = staggerIndex + 1)
            }
        }
        Spacer(Modifier.height(12.dp))
        OpouSkeletonLine(height = 13.dp, staggerIndex = staggerIndex + 1)
        Spacer(Modifier.height(7.dp))
        OpouSkeletonLine(modifier = Modifier.fillMaxWidth(0.75f), height = 13.dp, staggerIndex = staggerIndex + 2)
        Spacer(Modifier.height(7.dp))
        OpouSkeletonLine(modifier = Modifier.fillMaxWidth(0.5f), height = 13.dp, staggerIndex = staggerIndex + 2)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(4) { i ->
                OpouSkeletonLine(modifier = Modifier.width(34.dp), height = 16.dp, staggerIndex = staggerIndex + i)
            }
        }
    }
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        thickness = 0.6.dp
    )
}

/** قائمة من هياكل بطاقات الفقرات — تُستخدم كحالة تحميل أولية للتغذية/الشعبيات. */
@Composable
fun FeedSkeletonList(count: Int = 5) {
    Column(Modifier.fillMaxWidth()) {
        repeat(count) { index ->
            OpouPostCardSkeleton(staggerIndex = index * 2)
        }
    }
}

/** صف هيكلي لعنصر "تيكر" (صورة رمزية + سطرا نص) — نفس مقاس TekerRow الحقيقي. */
@Composable
fun TekerRowSkeleton(staggerIndex: Int = 0) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        OpouSkeletonCircle(size = 40.dp, staggerIndex = staggerIndex)
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OpouSkeletonLine(modifier = Modifier.width(120.dp), height = 12.dp, staggerIndex = staggerIndex)
            OpouSkeletonLine(modifier = Modifier.width(80.dp), height = 9.dp, staggerIndex = staggerIndex + 1)
        }
    }
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        thickness = 0.6.dp
    )
}

/** قائمة هياكل صفوف التيكرز. */
@Composable
fun TekersSkeletonList(count: Int = 8) {
    Column(Modifier.fillMaxWidth()) {
        repeat(count) { index -> TekerRowSkeleton(staggerIndex = index) }
    }
}

/**
 * هيكل شاشة "الغرفة" (الملف الشخصي) الكامل: بانر، صورة رمزية متراكبة، اسم،
 * بطاقة إحصائيات، وقسمان (لمحة/سيرة ذاتية) — بنفس أبعاد ProfileScreen الحقيقية.
 */
@Composable
fun OpouProfileHeaderSkeleton() {
    Column(Modifier.fillMaxWidth()) {
        // البانر
        OpouSkeletonBlock(
            modifier = Modifier.fillMaxWidth().height(130.dp),
            shape = RoundedCornerShape(0.dp),
            staggerIndex = 0
        )

        Column(Modifier.padding(horizontal = 16.dp)) {
            Box(Modifier.padding(top = (-32).dp)) {
                OpouSkeletonCircle(size = 72.dp, staggerIndex = 1)
            }

            Spacer(Modifier.height(10.dp))
            OpouSkeletonLine(modifier = Modifier.width(140.dp), height = 18.dp, staggerIndex = 1)
            Spacer(Modifier.height(10.dp))
            OpouSkeletonLine(modifier = Modifier.width(100.dp), height = 26.dp, staggerIndex = 2)

            Spacer(Modifier.height(18.dp))
            OpouSkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(66.dp),
                shape = RoundedCornerShape(18.dp),
                staggerIndex = 2
            )

            Spacer(Modifier.height(16.dp))
            OpouSkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(18.dp),
                staggerIndex = 3
            )

            Spacer(Modifier.height(12.dp))
            OpouSkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(90.dp),
                shape = RoundedCornerShape(18.dp),
                staggerIndex = 4
            )
        }
    }
}
