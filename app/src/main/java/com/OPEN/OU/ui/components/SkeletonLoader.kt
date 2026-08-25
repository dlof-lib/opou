package com.OPEN.OU.ui.components

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * التحميل الهيكلي (Skeleton Loading): مجموعة عناصر بصرية بديلة تُعرض أثناء
 * جلب البيانات بدلًا من مؤشر دوّار واحد، لتُعطي المستخدم فكرة فورية عن شكل
 * المحتوى القادم (بطاقة فقرة، صف مستخدم، رأس ملف شخصي...) وتُشعره بأن
 * التطبيق سريع الاستجابة. تُستخدم في كل الأقسام والصفحات ذات التحميل غير
 * الفوري: التغذية، الشعبيات، الملف الشخصي، التيكرز، التعليقات، والمحظورين.
 */

/** تدرّج لوني متحرّك (Shimmer) يُمرَّر كخلفية لأي عنصر هيكلي. */
@Composable
private fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate, 0f),
        end = Offset(translate + 400f, 400f)
    )
}

/** كتلة هيكلية مستطيلة عامة بحواف دائرية، تُستخدم كأساس لبقية العناصر. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp)
) {
    Box(
        modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

/** دائرة هيكلية (لصور المستخدمين الرمزية). */
@Composable
fun SkeletonCircle(size: Dp) {
    SkeletonBlock(modifier = Modifier.size(size), shape = CircleShape)
}

/** سطر نصّي هيكلي بعرض وارتفاع قابلين للتخصيص. */
@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier.fillMaxWidth(),
    height: Dp = 14.dp
) {
    SkeletonBlock(modifier = modifier.height(height), shape = RoundedCornerShape(4.dp))
}

/** هيكل بطاقة فقرة (منشور) يحاكي تخطيط PostCard: رأس بصورة واسم، سطور نص، وشريط تفاعلات. */
@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonCircle(size = 42.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    SkeletonLine(modifier = Modifier.width(110.dp), height = 13.dp)
                    Spacer(Modifier.height(6.dp))
                    SkeletonLine(modifier = Modifier.width(70.dp), height = 10.dp)
                }
            }
            Spacer(Modifier.height(14.dp))
            SkeletonLine(modifier = Modifier.fillMaxWidth(), height = 13.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.85f), height = 13.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.55f), height = 13.dp)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(3) {
                    SkeletonLine(modifier = Modifier.width(56.dp), height = 22.dp)
                }
            }
        }
    }
}

/** قائمة هيكلية من بطاقات الفقرات، تُستخدم أثناء تحميل التغذية/الشعبيات/فقرات الملف الشخصي. */
@Composable
fun PostListSkeleton(count: Int = 3, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        repeat(count) { PostCardSkeleton() }
    }
}

/** هيكل صف مستخدم (تيكرز/محظورين): صورة رمزية دائرية + اسم + سطر ثانوي، مع زر جانبي اختياري. */
@Composable
fun UserRowSkeleton(modifier: Modifier = Modifier, showTrailingButton: Boolean = true) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonCircle(size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            SkeletonLine(modifier = Modifier.width(120.dp), height = 14.dp)
            Spacer(Modifier.height(6.dp))
            SkeletonLine(modifier = Modifier.width(80.dp), height = 11.dp)
        }
        if (showTrailingButton) {
            Spacer(Modifier.width(10.dp))
            SkeletonBlock(
                modifier = Modifier.width(78.dp).height(32.dp),
                shape = RoundedCornerShape(50)
            )
        }
    }
}

/** قائمة هيكلية من صفوف المستخدمين (تيكرز/محظورين/نتائج بحث). */
@Composable
fun UserListSkeleton(count: Int = 6, modifier: Modifier = Modifier, showTrailingButton: Boolean = true) {
    Column(modifier.fillMaxWidth()) {
        repeat(count) { UserRowSkeleton(showTrailingButton = showTrailingButton) }
    }
}

/** هيكل رأس الملف الشخصي (الغرفة): بانر + صورة رمزية متراكبة + اسم + إحصائيات + سيرة ذاتية. */
@Composable
fun ProfileHeaderSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        // البانر
        SkeletonBlock(
            modifier = Modifier.fillMaxWidth().height(130.dp),
            shape = RoundedCornerShape(0.dp)
        )
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(0.dp))
            Box(Modifier.padding(top = (-36).dp)) {
                SkeletonCircle(size = 84.dp)
            }
            Spacer(Modifier.height(10.dp))
            SkeletonLine(modifier = Modifier.width(150.dp), height = 18.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonLine(modifier = Modifier.width(100.dp), height = 12.dp)
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(3) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SkeletonLine(modifier = Modifier.width(30.dp), height = 16.dp)
                        Spacer(Modifier.height(6.dp))
                        SkeletonLine(modifier = Modifier.width(46.dp), height = 10.dp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(90.dp),
                shape = RoundedCornerShape(18.dp)
            )
            Spacer(Modifier.height(14.dp))
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

/** هيكل صف تعليق: صورة رمزية صغيرة + اسم + سطرا نص. */
@Composable
fun CommentRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        SkeletonCircle(size = 34.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            SkeletonLine(modifier = Modifier.width(90.dp), height = 11.dp)
            Spacer(Modifier.height(6.dp))
            SkeletonLine(modifier = Modifier.fillMaxWidth(), height = 12.dp)
            Spacer(Modifier.height(5.dp))
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.6f), height = 12.dp)
        }
    }
}

/** قائمة هيكلية من صفوف التعليقات. */
@Composable
fun CommentListSkeleton(count: Int = 4, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        repeat(count) { CommentRowSkeleton() }
    }
}

/** هيكل عام لصفحة إعدادات/نموذج: عدة حقول وأزرار مستطيلة. */
@Composable
fun FormSkeleton(fieldCount: Int = 3, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(16.dp)) {
        repeat(fieldCount) {
            SkeletonLine(modifier = Modifier.width(90.dp), height = 12.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
