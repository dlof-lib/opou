package com.OPEN.OU.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.OPEN.OU.data.model.ParagraphPrivacy
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.ui.theme.OpouAccentGreen
import com.OPEN.OU.util.SafeHtml
import com.OPEN.OU.util.toColorOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * بطاقة فقرة (منشور) بتصميم احترافي مُحسَّن: رأس مُتقن (صورة بإطار متدرّج +
 * اسم + بيانات وصفية في سطر واحد مضغوط)، محتوى بمسافات ونمط طباعة مريحين
 * للقراءة، وسائط (صور/روابط) معروضة بأسلوب بطاقات فرعية أنيقة، وشريط
 * تفاعلات على هيئة "كبسولة" مفصولة بخطوط دقيقة لإبراز البنية دون إثقال
 * الواجهة. التصميم موحّد ومتّسق مع هوية أخضر أوبو البصرية.
 */
@Composable
fun PostCard(
    post: Post,
    currentReaction: ReactionType,
    onReact: (ReactionType) -> Unit,
    onComment: () -> Unit,
    onTek: () -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val customBackground = post.backgroundColor.toColorOrNull()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = customBackground ?: MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {

            if (post.isTek) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "أعاد ${post.authorUsername} النشر عن ${post.originalAuthorUsername}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(Modifier.padding(12.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val avatarModifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onOpenProfile(post.authorId) }

                    if (post.authorAvatarBase64.isNotBlank()) {
                        Base64Image(base64 = post.authorAvatarBase64, modifier = avatarModifier, cornerRadiusDp = 19)
                    } else {
                        AsyncImage(
                            model = post.authorAvatarUrl.ifBlank { null },
                            contentDescription = null,
                            modifier = avatarModifier.background(Color(0xFF0B7A4A))
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                post.authorUsername,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.clickable { onOpenProfile(post.authorId) }
                            )
                            if (post.emoji.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Text(post.emoji, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        MetaLine(post)
                    }
                }

                if (post.content.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    ExpandableParagraphText(post)
                }

                if (post.customHtml.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = SafeHtml.render(post.customHtml),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }

                if (post.links.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        post.links.filter { it.isNotBlank() }.forEach { link ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    val normalized = if (link.startsWith("http://") || link.startsWith("https://")) link else "https://$link"
                                    runCatching { uriHandler.openUri(normalized) }
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Link,
                                    contentDescription = null,
                                    tint = OpouAccentGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = link,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OpouAccentGreen,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                    }
                }
                if (post.imageBase64.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Base64Image(
                            base64 = post.imageBase64,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                ReactionBar(
                    likesCount = post.likesCount,
                    dislikesCount = post.dislikesCount,
                    commentsCount = post.commentsCount,
                    teksCount = post.teksCount,
                    currentReaction = currentReaction,
                    onReact = onReact,
                    onComment = onComment,
                    onTek = onTek,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
            }

            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                thickness = 0.6.dp
            )
        }
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("d MMM yyyy - HH:mm", Locale("ar")).format(Date(millis))

/**
 * سطر البيانات الوصفية أسفل الاسم: الوقت، وعند الحاجة نقطة فاصلة تليها
 * أيقونة وتسمية مضغوطة للخصوصية و/أو الجدولة — بدل صفّ شرائح منفصل يُثقل
 * الرأس. يحافظ على واجهة نظيفة بسطر واحد قدر الإمكان.
 */
@Composable
private fun MetaLine(post: Post) {
    val privacy = ParagraphPrivacy.fromValue(post.privacy)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatTime(post.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (privacy != ParagraphPrivacy.PUBLIC) {
            val (icon, label) = when (privacy) {
                ParagraphPrivacy.PRIVATE -> Icons.Filled.Lock to "خاص"
                ParagraphPrivacy.LIMITED -> Icons.Filled.People to "محدود"
                ParagraphPrivacy.CUSTOM -> Icons.Filled.Tune to "مخصّص"
                else -> Icons.Filled.Public to "عام"
            }
            Dot()
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (post.isScheduledForFuture()) {
            Dot()
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                tint = OpouAccentGreen,
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                "ستُنشر ${formatTime(post.scheduledAt ?: 0L)}",
                style = MaterialTheme.typography.labelSmall,
                color = OpouAccentGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .padding(horizontal = 5.dp)
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    )
}

/**
 * محتوى الفقرة مع دعم التنسيق (لون/تعريض/تسطير/خلفية النص) وميزة "عرض المزيد" —
 * يُطوى النص تلقائيًا بعد MAX_COLLAPSED_LINES سطرًا مع زر لتوسيعه.
 */
@Composable
private fun ExpandableParagraphText(post: Post) {
    val MAX_COLLAPSED_LINES = 6
    var expanded by remember(post.postId) { mutableStateOf(false) }
    var isOverflowing by remember(post.postId) { mutableStateOf(false) }

    val textColor = post.textColor.toColorOrNull() ?: MaterialTheme.colorScheme.onSurface
    val textBg = post.textBackgroundColor.toColorOrNull()
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = if (post.textBold) FontWeight.Bold else FontWeight.Normal,
        textDecoration = if (post.textUnderline) TextDecoration.Underline else TextDecoration.None,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
    )

    Column(
        modifier = if (textBg != null) {
            Modifier
                .background(textBg, shape = RoundedCornerShape(10.dp))
                .padding(10.dp)
        } else Modifier
    ) {
        Text(
            post.content,
            style = textStyle,
            color = textColor,
            maxLines = if (expanded) Int.MAX_VALUE else MAX_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) isOverflowing = result.hasVisualOverflow
            }
        )
        if (isOverflowing) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    if (expanded) "عرض أقل" else "عرض المزيد",
                    style = MaterialTheme.typography.labelMedium,
                    color = OpouAccentGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
