package com.OPEN.OU.ui.components

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * تحميل هيكلي (Skeleton) لبطاقة فقرة واحدة — يطابق تخطيط وأبعاد [PostCard]
 * الحقيقية حرفيًا (صورة رمزية دائرية 38dp، سطر اسم + سطر بيانات وصفية،
 * ثلاثة أسطر محتوى بعروض متفاوتة تحاكي طول نص طبيعي، صورة اختيارية بارتفاع
 * 200dp، وشريط تفاعلات من 4 كبسولات) حتى لا يحدث أي "قفز" أو إعادة ترتيب
 * للتخطيط لحظة استبداله بالبطاقة الحقيقية بعد وصول البيانات.
 *
 * [staggerIndex] يُمرَّر لموجة الوميض [rememberOpouShimmerBrush] لإنتاج
 * تأثير الموجة المتتابعة عبر القائمة (راجع توثيق تلك الدالة).
 */
@Composable
fun PostCardSkeleton(
    modifier: Modifier = Modifier,
    staggerIndex: Int = 0,
    showImage: Boolean = false
) {
    val shimmer = rememberOpouShimmerBrush(staggerIndex)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Column(Modifier.padding(12.dp)) {

                // رأس البطاقة: صورة رمزية + اسم + بيانات وصفية
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(shimmer)
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.fillMaxWidth(0.6f)) {
                        Box(
                            Modifier
                                .height(13.dp)
                                .fillMaxWidth(0.55f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmer)
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .height(10.dp)
                                .fillMaxWidth(0.35f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmer)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // أسطر المحتوى — عروض متفاوتة لمحاكاة نص طبيعي غير منتظم
                Box(
                    Modifier
                        .height(14.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.55f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )

                if (showImage) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(shimmer)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // شريط التفاعلات: 4 كبسولات موزّعة كما في ReactionBar الحقيقي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        Box(
                            Modifier
                                .height(20.dp)
                                .width(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(shimmer)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                thickness = 0.6.dp
            )
        }
    }
}

/**
 * قائمة تحميل هيكلي كاملة تحاكي التغذية أثناء أول تحميل. تُبدَّل صورة كل
 * بطاقة ثالثة (index % 3 == 1) لتفادي شكل رتيب متطابق بين كل العناصر،
 * ويحمل كل عنصر [staggerIndex] مختلفًا لإنتاج موجة الوميض المتتابعة.
 */
@Composable
fun FeedSkeletonList(count: Int = 6, modifier: Modifier = Modifier) {
    Column(modifier) {
        repeat(count) { index ->
            PostCardSkeleton(
                staggerIndex = index,
                showImage = index % 3 == 1
            )
        }
    }
}
