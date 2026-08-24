package com.OPEN.OU.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.model.Comment
import com.OPEN.OU.data.model.CommentPermission
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.PostRepository
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.network.PhpBridgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentsViewModel(
    private val postRepo: PostRepository = PostRepository(),
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository(),
    private val phpBridge: PhpBridgeRepository = PhpBridgeRepository()
) : ViewModel() {

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _likedCommentIds = MutableStateFlow<Set<String>>(emptySet())
    val likedCommentIds: StateFlow<Set<String>> = _likedCommentIds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** معرّف المستخدم الحالي — تُستخدم في CommentsSheet لتحديد من يملك صلاحية حذف تعليق. */
    val currentUid: String? get() = authRepo.currentUserId

    fun clearError() { _errorMessage.value = null }

    fun load(postId: String) {
        viewModelScope.launch {
            postRepo.observeComments(postId).collect { _comments.value = it }
        }
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            postRepo.observeMyCommentLikes(uid).collect { _likedCommentIds.value = it }
        }
    }

    /**
     * يرسل تعليقًا جديدًا، بعد التحقق من إعداد "من يمكنه التعليق" لصاحب الفقرة
     * (الجميع/المتابعون فقط/لا أحد)، ثم يحاول (best-effort) إشعار صاحب الفقرة الأصلية
     * عبر الجسر Kotlin -> PHP -> FCM. [postAuthorId] اختياري: إن لم يُمرَّر (أو
     * كان هو نفسه المعلّق) لا يُرسل أي إشعار.
     */
    fun send(
        postId: String,
        content: String,
        username: String,
        avatar: String,
        postAuthorId: String? = null,
        avatarBase64: String = "",
        /** التعليق الذي يُردّ عليه هذا التعليق الجديد (إن وُجد) — يُنشئ خيط ردّ بمستوى واحد. */
        replyTo: Comment? = null
    ) {
        val uid = authRepo.currentUserId ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            if (postAuthorId != null && postAuthorId != uid) {
                val allowed = runCatching { isAllowedToComment(uid, postAuthorId) }.getOrDefault(true)
                if (!allowed) {
                    _errorMessage.value = "صاحب الفقرة قيّد من يمكنه التعليق"
                    return@launch
                }
            }
            postRepo.addComment(
                Comment(
                    postId = postId,
                    authorId = uid,
                    authorUsername = username,
                    authorAvatarUrl = avatar,
                    authorAvatarBase64 = avatarBase64,
                    content = content,
                    parentCommentId = replyTo?.commentId.orEmpty(),
                    replyToUsername = replyTo?.authorUsername.orEmpty()
                )
            )
            if (postAuthorId != null && postAuthorId != uid) {
                notifyPostAuthorBestEffort(postAuthorId, username)
            }
        }
    }

    /** إعجاب/إلغاء إعجاب ⭐ بتعليق. */
    fun toggleLike(postId: String, comment: Comment, likerUsername: String) {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            runCatching { postRepo.toggleCommentLike(postId, comment.commentId, uid) }
        }
    }

    /** يحذف تعليقًا — الصلاحية (صاحب التعليق أو صاحب الفقرة) تُتحقق منها الواجهة (CommentsSheet) قبل استدعائها. */
    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            runCatching { postRepo.deleteComment(postId, commentId) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر حذف التعليق" }
        }
    }

    private suspend fun isAllowedToComment(commenterUid: String, postAuthorId: String): Boolean {
        val author = userRepo.getUser(postAuthorId) ?: return true
        return when (CommentPermission.fromValue(author.whoCanComment)) {
            CommentPermission.EVERYONE -> true
            CommentPermission.NOBODY -> false
            CommentPermission.TEKERS -> userRepo.isTeking(commenterUid, postAuthorId)
        }
    }

    private suspend fun notifyPostAuthorBestEffort(postAuthorId: String, commenterUsername: String) {
        runCatching {
            val targetUser = userRepo.getUser(postAuthorId) ?: return
            phpBridge.notifyBestEffort(
                targetFcmToken = targetUser.fcmToken,
                title = "تعليق جديد على أوبو",
                body = "$commenterUsername علّق على فقرتك"
            )
        }
    }
}
