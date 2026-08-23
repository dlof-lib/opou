package com.OPEN.OU.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.model.Comment
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentsViewModel(
    private val postRepo: PostRepository = PostRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    fun load(postId: String) {
        viewModelScope.launch {
            postRepo.observeComments(postId).collect { _comments.value = it }
        }
    }

    fun send(postId: String, content: String, username: String, avatar: String) {
        val uid = authRepo.currentUserId ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            postRepo.addComment(
                Comment(
                    postId = postId,
                    authorId = uid,
                    authorUsername = username,
                    authorAvatarUrl = avatar,
                    content = content
                )
            )
        }
    }
}
