package com.OPEN.OU.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.OPEN.OU.R
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.ui.components.PostCard
import com.OPEN.OU.ui.theme.OpouBrandGradient

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "OPOU",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
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
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(OpouBrandGradient, shape = MaterialTheme.shapes.large),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.post_button),
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(stringResource(R.string.feed_title), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(stringResource(R.string.shaabiyat_title), fontWeight = FontWeight.Bold) }
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
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(list, key = { it.postId }) { post ->
                        PostCard(
                            post = post,
                            currentReaction = myReactions[post.postId] ?: ReactionType.NONE,
                            onReact = { type -> viewModel.react(post, type) },
                            onComment = { onOpenComments(post) },
                            onTek = { viewModel.tek(post, currentUsername, currentAvatar, currentAvatarBase64) },
                            onOpenProfile = onOpenProfile
                        )
                    }
                }
            }
        }
    }
}
