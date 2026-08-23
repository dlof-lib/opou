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
import androidx.compose.ui.unit.dp
import com.OPEN.OU.R
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.ui.components.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    currentUsername: String,
    currentAvatar: String,
    onOpenProfile: (String) -> Unit,
    onOpenComments: (Post) -> Unit,
    onCreatePost: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val feed by viewModel.feed.collectAsState()
    val shaabiyat by viewModel.shaabiyat.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("OPOU") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreatePost) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.post_button))
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.feed_title)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.shaabiyat_title)) })
            }

            val list = if (tab == 0) feed else shaabiyat

            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.postId }) { post ->
                    PostCard(
                        post = post,
                        currentReaction = ReactionType.NONE, // TODO: اربطها بحالة تفاعل المستخدم الفعلية من /reactions
                        onReact = { type -> viewModel.react(post, type) },
                        onComment = { onOpenComments(post) },
                        onTek = { viewModel.tek(post, currentUsername, currentAvatar) },
                        onOpenProfile = onOpenProfile
                    )
                }
            }
        }
    }
}
