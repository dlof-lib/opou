package com.OPEN.OU.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.OPEN.OU.R
import com.OPEN.OU.data.model.Comment
import com.OPEN.OU.ui.screens.CommentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    postId: String,
    currentUsername: String,
    currentAvatar: String,
    viewModel: CommentsViewModel,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val comments by viewModel.comments.collectAsState()
    var text by remember { mutableStateOf("") }

    LaunchedEffect(postId) { viewModel.load(postId) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
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

@Composable
private fun CommentRow(comment: Comment) {
    Column {
        Text(comment.authorUsername, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(comment.content, style = MaterialTheme.typography.bodyMedium)
    }
}

