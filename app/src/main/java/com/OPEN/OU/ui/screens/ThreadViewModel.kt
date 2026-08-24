package com.OPEN.OU.ui.screens

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

/**
 * يراقب فقرات سلسلة واحدة (Thread) — أي مجموعة فقرات نشرها نفس المستخدم كموضوع
 * واحد متسلسل (راجع Post.threadId) — مرتّبة بترتيب النشر (الأقدم أولًا)، ويدعم
 * نفس تفاعلات ⭐/💔 المتاحة في التغذية العادية.
 */
class ThreadViewModel(
    private val postRepo: PostRepository = PostRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _myReactions = MutableStateFlow<Map<String, ReactionType>>(emptyMap())
    val myReactions: StateFlow<Map<String, ReactionType>> = _myReactions.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val currentUid: String? get() = authRepo.currentUserId

    fun clearError() { _errorMessage.value = null }

    fun load(threadId: String) {
        viewModelScope.launch {
            postRepo.observeThread(threadId).collect {
                _posts.value = it
                _isLoading.value = false
            }
        }
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            postRepo.observeMyReactions(uid).collect { _myReactions.value = it }
        }
    }

    fun react(post: Post, type: ReactionType) {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            runCatching { postRepo.react(post.postId, uid, type) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر تسجيل تفاعلك" }
        }
    }

    /** يحذف فقرة يملكها المستخدم الحالي فقط (يُستخدم من داخل شاشة السلسلة أيضًا). */
    fun deletePost(post: Post) {
        val uid = authRepo.currentUserId ?: return
        if (uid != post.authorId) return
        viewModelScope.launch {
            runCatching { postRepo.deletePost(post.postId, uid) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر حذف الفقرة" }
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
