package com.OPEN.OU.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.OPEN.OU.R
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.ui.components.GradientText
import com.OPEN.OU.ui.components.PostCard
import com.OPEN.OU.ui.components.ResponsiveContent
import com.OPEN.OU.ui.theme.OpouGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    currentUsername: String,
    currentAvatar: String,
    currentAvatarBase64: String = "",
    onOpenProfile: (String) -> Unit,
    onOpenComments: (Post) -> Unit,
    onCreatePost: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val feed by viewModel.feed.collectAsState()
    val shaabiyat by viewModel.shaabiyat.collectAsState()
    val myReactions by viewModel.myReactions.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        // هذه الشاشة تُعرض دائمًا كتبويب داخل الـ Scaffold الخارجي (الذي
        // يحجز مساحة الشريط السفلي فعليًا)، فنمنع هذا الـ Scaffold الداخلي
        // من حجز مساحة إضافية لشريط التنقّل السفلي للنظام لتفادي فراغ مضاعف
        // أسفل الشاشة (بنفس منطق إصلاح الشريط العلوي).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    GradientText(
                        text = "OPOU",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePost,
                containerColor = OpouGreen,
                contentColor = androidx.compose.ui.graphics.Color.White,
                elevation = FloatingActionButtonDefaults.elevation(2.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.post_button)
                )
            }
        }
    ) { padding ->
        ResponsiveContent(modifier = Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = tab) {
                    Tab(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        text = { Text(stringResource(R.string.feed_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge) }
                    )
                    Tab(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        text = { Text(stringResource(R.string.shaabiyat_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge) }
                    )
                }

                val list = if (tab == 0) feed else shaabiyat

                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(
                            text = if (tab == 0) "لا توجد فقرات بعد — كن أول من ينشر!" else "لا توجد فقرات شعبية بعد",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(list, key = { it.postId }) { post ->
                            PostCard(
                                post = post,
                                currentReaction = myReactions[post.postId] ?: ReactionType.NONE,
                                onReact = { type -> viewModel.react(post, type) },
                                onComment = { onOpenComments(post) },
                                onTek = { viewModel.tek(post, currentUsername, currentAvatar, currentAvatarBase64) },
                                onOpenProfile = onOpenProfile,
                                isOwnPost = viewModel.currentUid != null && viewModel.currentUid == post.authorId,
                                onTogglePin = { viewModel.togglePin(post) },
                                onBlockAuthor = { viewModel.blockAuthor(post.authorId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
