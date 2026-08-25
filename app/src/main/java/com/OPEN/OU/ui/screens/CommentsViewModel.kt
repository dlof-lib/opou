package com.OPEN.OU.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.model.Comment
import com.OPEN.OU.data.model.CommentPermission
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.PostRepository
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.network.PhpBridgeRepository
import com.OPEN.OU.ui.components.MentionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CommentsViewModel(
    private val postRepo: PostRepository = PostRepository(),
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository(),
    private val phpBridge: PhpBridgeRepository = PhpBridgeRepository()
) : ViewModel() {

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    /** معرّفات التعليقات (ضمن الفقرة المفتوحة حاليًا) التي أعجبت المستخدم الحالي */
    private val _likedCommentIds = MutableStateFlow<Set<String>>(emptySet())
    val likedCommentIds: StateFlow<Set<String>> = _likedCommentIds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // تبقى true حتى تصل أول دفعة تعليقات، لعرض تحميل هيكلي بدل قائمة فارغة مؤقتة.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val currentUid: String? get() = authRepo.currentUserId

    fun clearError() { _errorMessage.value = null }

    fun load(postId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            postRepo.observeComments(postId)
                // يمنع أي خطأ قراءة (صلاحيات/شبكة) من إسقاط التطبيق — يعرض قائمة فارغة بدل الانهيار
                .catch { _errorMessage.value = it.message; _comments.value = emptyList(); _isLoading.value = false }
                .collect { _comments.value = it; _isLoading.value = false }
        }
        authRepo.currentUserId?.let { uid ->
            viewModelScope.launch {
                postRepo.observeMyCommentLikes(postId, uid)
                    .catch { _likedCommentIds.value = emptySet() }
                    .collect { _likedCommentIds.value = it }
            }
        }
    }

    /** إعجاب/إلغاء إعجاب بتعليق، مع إشعار Best-effort لصاحب التعليق إن لم يكن هو المُعجِب. */
    fun toggleLike(postId: String, comment: Comment, likerUsername: String) {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            runCatching { postRepo.toggleCommentLike(postId, comment.commentId, uid) }
                .onSuccess { nowLiked ->
                    if (nowLiked && comment.authorId != uid) {
                        notifyUserBestEffort(comment.authorId, "أعجب $likerUsername بتعليقك", likerUsername)
                    }
                }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر تسجيل إعجابك بالتعليق" }
        }
    }

    /** يحذف تعليقًا — يُستدعى فقط عند التحقق أن المستخدم صاحب التعليق أو صاحب الفقرة في واجهة الاستدعاء. */
    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            runCatching { postRepo.deleteComment(postId, commentId) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر حذف التعليق" }
        }
    }

    private suspend fun notifyUserBestEffort(targetUid: String, title: String, byUsername: String) {
        runCatching {
            val targetUser = userRepo.getUser(targetUid) ?: return
            phpBridge.notifyBestEffort(targetFcmToken = targetUser.fcmToken, title = "أوبو", body = title)
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
            // إشعار صاحب التعليق الأصل بالرد عليه (إن وُجد وكان مختلفًا عن صاحب الفقرة والمُعلّق نفسه)
            if (replyTo != null && replyTo.authorId != uid && replyTo.authorId != postAuthorId) {
                notifyUserBestEffort(replyTo.authorId, "ردّ $username على تعليقك", username)
            }
            if (postAuthorId != null && postAuthorId != uid) {
                notifyPostAuthorBestEffort(postAuthorId, username)
            }
            runCatching { notifyMentionedUsersBestEffort(content, uid, username) }
        }
    }

    private suspend fun notifyMentionedUsersBestEffort(content: String, authorUid: String, authorUsername: String) {
        MentionUtils.extractMentions(content).forEach { mentionedUsername ->
            runCatching {
                val mentionedUid = userRepo.getUidByUsername(mentionedUsername) ?: return@runCatching
                if (mentionedUid == authorUid) return@runCatching
                notifyUserBestEffort(mentionedUid, "ذكرك $authorUsername في تعليق", authorUsername)
            }
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
