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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
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
import com.OPEN.OU.ui.theme.OpouAccentGreen
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
 * تصميم مُحدَّث ومميّز: كل تعليق يُعرض كبطاقة مستقلة أنيقة (لا فقاعة
 * محادثة تقليدية) بشريط تمييز رفيع بلون العلامة، صورة رمزية بإطار
 * متدرّج، واسم يبرز بلون العلامة — يمنح مظهرًا احترافيًا أقرب لمنصات
 * النقاش الجادة منه لتطبيقات الدردشة. حقل الكتابة عائم بأسلوب حديث،
 * وحالة فارغة أنيقة مع أيقونة داخل دائرة متدرّجة.
 */
@Composable
fun CommentsSheet(
    postId: String,
    currentUsername: String,
    currentAvatar: String,
    viewModel: CommentsViewModel,
    onDismiss: () -> Unit,
    postAuthorId: String? = null,
    currentAvatarBase64: String = "",
    onOpenProfile: (String) -> Unit = {},
    /** يُستدعى عند اختيار "الرد بفقرة" على تعليق — يحمل الغرض للشاشة الأم لفتح إنشاء فقرة جديدة مقتبسة عنه. */
    onQuoteAsParagraph: ((Comment) -> Unit)? = null
) {
    val comments by viewModel.comments.collectAsState()
    val likedCommentIds by viewModel.likedCommentIds.collectAsState()
    var text by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }

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
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* استهلاك النقر حتى لا يُغلق عند الضغط داخل الورقة */ },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.fillMaxSize()) {
                    // مقبض السحب + العنوان
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 16.dp)
                                .width(38.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(OpouAccentGreen.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = OpouAccentGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    stringResource(R.string.comments_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (comments.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = comments.size.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    // قائمة التعليقات
                    if (comments.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(
                                                    OpouAccentGreen.copy(alpha = 0.18f),
                                                    OpouAccentGreen.copy(alpha = 0.06f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = OpouAccentGreen,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    stringResource(R.string.no_comments_yet),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(comments, key = { it.commentId }) { comment ->
                                CommentRow(
                                    comment = comment,
                                    isPostAuthor = postAuthorId != null && comment.authorId == postAuthorId,
                                    isLiked = comment.commentId in likedCommentIds,
                                    canDelete = viewModel.currentUid != null &&
                                        (viewModel.currentUid == comment.authorId || viewModel.currentUid == postAuthorId),
                                    onLike = { viewModel.toggleLike(postId, comment, currentUsername) },
                                    onReply = { replyingTo = comment },
                                    onQuoteAsParagraph = onQuoteAsParagraph?.let { { it(comment) } },
                                    onDelete = { viewModel.deleteComment(postId, comment.commentId) },
                                    onOpenProfile = onOpenProfile
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    replyingTo?.let { reply ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Reply,
                                contentDescription = null,
                                tint = OpouAccentGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "ردًا على @${reply.authorUsername}",
                                style = MaterialTheme.typography.labelMedium,
                                color = OpouAccentGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "إلغاء الرد", modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // حقل الكتابة
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CommentAvatar(url = currentAvatar, base64 = currentAvatarBase64, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
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
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                focusedBorderColor = OpouAccentGreen.copy(alpha = 0.7f)
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        val canSend = text.isNotBlank()
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) OpouBrandGradient
                                    else solidBrush(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f))
                                )
                                .clickable(enabled = canSend) {
                                    viewModel.send(
                                        postId = postId,
                                        content = text,
                                        username = currentUsername,
                                        avatar = currentAvatar,
                                        postAuthorId = postAuthorId,
                                        avatarBase64 = currentAvatarBase64,
                                        replyTo = replyingTo
                                    )
                                    text = ""
                                    replyingTo = null
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

/**
 * بطاقة تعليق مستقلة — بديل مميّز عن فقاعة المحادثة التقليدية: شريط رفيع
 * بلون العلامة على الحافة، صورة رمزية بإطار متدرّج، اسم بلون أساسي بارز،
 * ووقت نسبي أنيق في نفس السطر. تعليقات صاحب الفقرة تحمل شارة صغيرة مميّزة.
 */
@Composable
private fun CommentRow(
    comment: Comment,
    isPostAuthor: Boolean = false,
    isLiked: Boolean = false,
    canDelete: Boolean = false,
    onLike: () -> Unit = {},
    onReply: () -> Unit = {},
    onQuoteAsParagraph: (() -> Unit)? = null,
    onDelete: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = if (comment.isReply) Modifier.padding(start = 24.dp) else Modifier
    ) {
        Row(Modifier.height(IntrinsicSize.Min).padding(end = 12.dp)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        if (isPostAuthor) OpouAccentGreen else OpouAccentGreen.copy(alpha = 0.25f)
                    )
            )
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp)
            ) {
                CommentAvatar(url = comment.authorAvatarUrl, base64 = comment.authorAvatarBase64, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            comment.authorUsername,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = OpouAccentGreen,
                            modifier = Modifier.clickable { onOpenProfile(comment.authorId) }
                        )
                        if (isPostAuthor) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = OpouAccentGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "صاحب الفقرة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OpouAccentGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = formatRelativeTime(comment.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (comment.isReply && comment.replyToUsername.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "ردًا على @${comment.replyToUsername}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    MentionAwareText(
                        text = comment.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        ),
                        onOpenProfile = onOpenProfile
                    )

                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onLike)
                        ) {
                            Icon(
                                if (isLiked) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "إعجاب",
                                tint = if (isLiked) com.OPEN.OU.ui.theme.OpouStar else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            if (comment.likesCount > 0) {
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    comment.likesCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isLiked) com.OPEN.OU.ui.theme.OpouStar else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onReply)
                        ) {
                            Icon(
                                Icons.Filled.Reply,
                                contentDescription = "رد",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text("رد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (onQuoteAsParagraph != null) {
                            Spacer(Modifier.width(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(onClick = onQuoteAsParagraph)
                            ) {
                                Icon(
                                    Icons.Filled.FormatQuote,
                                    contentDescription = "الرد بفقرة",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("رد بفقرة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (canDelete) {
                            Spacer(Modifier.width(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showDeleteConfirm = true }
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "حذف",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف التعليق؟") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") } }
        )
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
