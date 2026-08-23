package com.OPEN.OU.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.OPEN.OU.R
import com.OPEN.OU.data.model.Comment
import com.OPEN.OU.ui.screens.CommentsViewModel
import com.OPEN.OU.ui.theme.OpouBrandGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ورقة تعليقات مبنية على Dialog (API مستقر تماماً)، بدلاً من
 * ModalBottomSheet من Material3 الذي لا يزال مُصنَّفًا كـ Experimental.
 * تُحاكي شكل وسلوك الـ bottom sheet: تظهر من الأسفل، وتُغلق عند
 * الضغط خارجها أو عند استدعاء onDismiss.
 *
 * تصميم مُحدَّث: فقاعات تعليقات مرتّبة بصورة رمزية + اسم + وقت نسبي،
 * حقل كتابة عائم على طراز تطبيقات التواصل الحديثة، وحالة فارغة أنيقة.
 */
@Composable
fun CommentsSheet(
    postId: String,
    currentUsername: String,
    currentAvatar: String,
    viewModel: CommentsViewModel,
    onDismiss: () -> Unit,
    postAuthorId: String? = null,
    currentAvatarBase64: String = ""
) {
    val comments by viewModel.comments.collectAsState()
    var text by remember { mutableStateOf("") }

    LaunchedEffect(postId) { viewModel.load(postId) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* استهلاك النقر حتى لا يُغلق عند الضغط داخل الورقة */ },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(Modifier.fillMaxSize()) {
                    // مقبض السحب + العنوان
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 14.dp)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.comments_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (comments.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = comments.size.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // قائمة التعليقات
                    if (comments.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.no_comments_yet),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(comments, key = { it.commentId }) { comment ->
                                CommentRow(comment)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // حقل الكتابة
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CommentAvatar(url = currentAvatar, base64 = currentAvatarBase64, size = 34.dp)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.write_comment_hint),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(22.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        val canSend = text.isNotBlank()
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) OpouBrandGradient
                                    else solidBrush(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                )
                                .clickable(enabled = canSend) {
                                    viewModel.send(
                                        postId,
                                        text,
                                        currentUsername,
                                        currentAvatar,
                                        postAuthorId,
                                        currentAvatarBase64
                                    )
                                    text = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = stringResource(R.string.post_button),
                                tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

private fun solidBrush(color: Color) = androidx.compose.ui.graphics.Brush.linearGradient(listOf(color, color))

@Composable
private fun CommentRow(comment: Comment) {
    Row(verticalAlignment = Alignment.Top) {
        CommentAvatar(url = comment.authorAvatarUrl, base64 = comment.authorAvatarBase64, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        comment.authorUsername,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        comment.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatRelativeTime(comment.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun CommentAvatar(url: String, base64: String, size: androidx.compose.ui.unit.Dp) {
    val avatarModifier = Modifier
        .size(size)
        .clip(CircleShape)
        .border(1.5.dp, OpouBrandGradient, CircleShape)
        .padding(1.5.dp)
        .clip(CircleShape)

    if (base64.isNotBlank()) {
        Base64Image(base64 = base64, modifier = avatarModifier, cornerRadiusDp = (size.value / 2).toInt())
    } else {
        AsyncImage(
            model = url.ifBlank { null },
            contentDescription = null,
            modifier = avatarModifier.background(Color(0xFF0B7A4A))
        )
    }
}

private fun formatRelativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "الآن"
        minutes < 60 -> "منذ ${minutes} د"
        hours < 24 -> "منذ ${hours} س"
        days < 7 -> "منذ ${days} يوم"
        else -> SimpleDateFormat("d MMM", Locale("ar")).format(Date(millis))
    }
}
