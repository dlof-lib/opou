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
import com.OPEN.OU.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val postRepo: PostRepository = PostRepository(),
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
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
        val uid = authRepo.currentUserId
        viewModelScope.launch {
            // نجلب "من يتابعهم" المستخدم الحالي مرة واحدة لتطبيق خصوصية "محدود" (LIMITED) على فقرات التغذية
            val followingIds = uid?.let { runCatching { userRepo.getTekingIds(it).toSet() }.getOrDefault(emptySet()) } ?: emptySet()
            launch {
                postRepo.observeFeed(viewerId = uid, viewerFollowingIds = followingIds).collect { _feed.value = it }
            }
            launch {
                postRepo.observeShaabiyat(viewerId = uid, viewerFollowingIds = followingIds).collect { _shaabiyat.value = it }
            }
        }
        uid?.let {
            viewModelScope.launch {
                postRepo.observeMyReactions(it).collect { reactions -> _myReactions.value = reactions }
            }
        }
    }

    fun publish(
        content: String,
        authorUsername: String,
        authorAvatar: String,
        authorAvatarBase64: String = "",
        imageBase64: String = "",
        backgroundColor: String = "",
        emoji: String = "",
        textColor: String = "",
        textBold: Boolean = false,
        textUnderline: Boolean = false,
        textBackgroundColor: String = "",
        links: List<String> = emptyList(),
        customHtml: String = "",
        privacy: String = "PUBLIC",
        allowedViewerIds: List<String> = emptyList(),
        scheduledAt: Long? = null,
        onDone: () -> Unit
    ) {
        val uid = authRepo.currentUserId ?: return
        if (content.isBlank() && imageBase64.isBlank()) return
        // القيمة الآمنة النهائية للإيموجي — تتجاهل أي قيمة خارج المجموعة المتاحة حاليًا (الميزة قيد التطوير)
        val safeEmoji = if (com.OPEN.OU.data.model.ParagraphEmoji.isValid(emoji)) emoji else ""
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
                        imageBase64 = imageBase64,
                        backgroundColor = backgroundColor,
                        emoji = safeEmoji,
                        textColor = textColor,
                        textBold = textBold,
                        textUnderline = textUnderline,
                        textBackgroundColor = textBackgroundColor,
                        links = links,
                        customHtml = customHtml,
                        privacy = privacy,
                        allowedViewerIds = allowedViewerIds,
                        scheduledAt = scheduledAt
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
