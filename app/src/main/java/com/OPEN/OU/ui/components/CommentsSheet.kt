package com.OPEN.OU.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.OPEN.OU.R
import com.OPEN.OU.data.model.Comment
import com.OPEN.OU.ui.screens.CommentsViewModel

/**
 * ورقة تعليقات مبنية على Dialog (API مستقر تماماً)، بدلاً من
 * ModalBottomSheet من Material3 الذي لا يزال مُصنَّفًا كـ Experimental.
 * تُحاكي شكل وسلوك الـ bottom sheet: تظهر من الأسفل، وتُغلق عند
 * الضغط خارجها أو عند استدعاء onDismiss.
 */
@Composable
fun CommentsSheet(
    postId: String,
    currentUsername: String,
    currentAvatar: String,
    viewModel: CommentsViewModel,
    onDismiss: () -> Unit
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
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* استهلاك النقر حتى لا يُغلق عند الضغط داخل الورقة */ },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )

                    Text(
                        stringResource(R.string.comments_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(comments, key = { it.commentId }) { comment ->
                            CommentRow(comment)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text(stringResource(R.string.write_comment_hint)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                viewModel.send(postId, text, currentUsername, currentAvatar)
                                text = ""
                            },
                            enabled = text.isNotBlank()
                        ) { Text(stringResource(R.string.post_button)) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment) {
    Column {
        Text(comment.authorUsername, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(comment.content, style = MaterialTheme.typography.bodyMedium)
    }
}
