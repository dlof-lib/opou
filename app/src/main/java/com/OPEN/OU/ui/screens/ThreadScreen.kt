package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.ui.components.FeedSkeletonList
import com.OPEN.OU.ui.components.PostCard
import com.OPEN.OU.ui.theme.OpouAccentGreen

/**
 * تعرض كل فقرات سلسلة واحدة (Thread) بترتيب نشرها — رأس السلسلة أولًا فآخر
 * إضافة. تدعم نفس تفاعلات التغذية العادية (⭐/💔/تعليقات/تيك)، بالإضافة إلى
 * تعديل/حذف الفقرات لصاحبها ومتابعة السلسلة بفقرة جديدة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    threadId: String,
    viewModel: ThreadViewModel,
    currentUsername: String,
    currentAvatar: String,
    currentAvatarBase64: String = "",
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenComments: (Post) -> Unit,
    onEditPost: (Post) -> Unit = {},
    onContinueThread: (Post) -> Unit = {}
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val myReactions by viewModel.myReactions.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(threadId) { viewModel.load(threadId) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.FormatListNumbered,
                            contentDescription = null,
                            tint = OpouAccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("سلسلة فقرات", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            if (posts.isNotEmpty()) {
                                Text(
                                    "${posts.size} ${if (posts.size == 1) "فقرة" else "فقرات"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && posts.isEmpty()) {
                LazyColumn(Modifier.fillMaxSize()) {
                    item { FeedSkeletonList() }
                }
            } else if (posts.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "تعذّر العثور على هذه السلسلة — ربما حُذفت فقراتها",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(posts, key = { it.postId }) { post ->
                        Column {
                            // خط رفيع رابط بين كل فقرة وسابقتها لإبراز أنها سلسلة متصلة
                            if (post.threadPosition > 1) {
                                Box(
                                    Modifier
                                        .padding(start = 30.dp)
                                        .width(2.dp)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(OpouAccentGreen.copy(alpha = 0.25f))
                                )
                            }
                            PostCard(
                                post = post,
                                currentReaction = myReactions[post.postId] ?: ReactionType.NONE,
                                onReact = { type -> viewModel.react(post, type) },
                                onComment = { onOpenComments(post) },
                                onTek = { viewModel.tek(post, currentUsername, currentAvatar, currentAvatarBase64) },
                                onOpenProfile = onOpenProfile,
                                isOwnPost = viewModel.currentUid != null && viewModel.currentUid == post.authorId,
                                onEditPost = if (viewModel.currentUid == post.authorId) {
                                    { onEditPost(post) }
                                } else null,
                                onDeletePost = if (viewModel.currentUid == post.authorId) {
                                    { viewModel.deletePost(post) }
                                } else null,
                                onContinueThread = if (viewModel.currentUid == post.authorId && post.postId == posts.last().postId) {
                                    { onContinueThread(post) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}
