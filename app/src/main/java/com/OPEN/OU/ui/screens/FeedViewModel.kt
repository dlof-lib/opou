package com.OPEN.OU.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val postRepo: PostRepository = PostRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _feed = MutableStateFlow<List<Post>>(emptyList())
    val feed: StateFlow<List<Post>> = _feed.asStateFlow()

    private val _shaabiyat = MutableStateFlow<List<Post>>(emptyList())
    val shaabiyat: StateFlow<List<Post>> = _shaabiyat.asStateFlow()

    private val _myReactions = MutableStateFlow<Map<String, ReactionType>>(emptyMap())
    val myReactions: StateFlow<Map<String, ReactionType>> = _myReactions.asStateFlow()

    var isPosting by mutableStateOf(false); private set

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }

    init {
        viewModelScope.launch {
            postRepo.observeFeed().collect { _feed.value = it }
        }
        viewModelScope.launch {
            postRepo.observeShaabiyat().collect { _shaabiyat.value = it }
        }
        authRepo.currentUserId?.let { uid ->
            viewModelScope.launch {
                postRepo.observeMyReactions(uid).collect { _myReactions.value = it }
            }
        }
    }

    fun publish(
        content: String,
        authorUsername: String,
        authorAvatar: String,
        authorAvatarBase64: String = "",
        imageBase64: String = "",
        onDone: () -> Unit
    ) {
        val uid = authRepo.currentUserId ?: return
        if (content.isBlank() && imageBase64.isBlank()) return
        viewModelScope.launch {
            isPosting = true
            runCatching {
                postRepo.createPost(
                    Post(
                        authorId = uid,
                        authorUsername = authorUsername,
                        authorAvatarUrl = authorAvatar,
                        authorAvatarBase64 = authorAvatarBase64,
                        content = content,
                        imageBase64 = imageBase64
                    )
                )
            }.onSuccess {
                onDone()
            }.onFailure {
                _errorMessage.value = it.message ?: "تعذّر نشر الفقرة، حاول مجددًا"
            }
            isPosting = false
        }
    }

    fun react(post: Post, type: ReactionType) {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            runCatching { postRepo.react(post.postId, uid, type) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر تسجيل تفاعلك" }
        }
    }

    fun tek(post: Post, tekingUsername: String, tekingAvatar: String, tekingAvatarBase64: String = "") {
        val uid = authRepo.currentUserId ?: return
        if (uid == post.authorId) return // لا يمكن عمل تيك على فقرتك الخاصة
        viewModelScope.launch {
            runCatching { postRepo.tekPost(post, uid, tekingUsername, tekingAvatar, tekingAvatarBase64) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر إعادة النشر (تيك)، حاول مجددًا" }
        }
    }
}
