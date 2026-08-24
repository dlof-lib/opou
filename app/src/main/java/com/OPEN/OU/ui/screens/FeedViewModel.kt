package com.OPEN.OU.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.model.ReactionType
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.BlockRepository
import com.OPEN.OU.data.repository.PostRepository
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.network.PhpBridgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val postRepo: PostRepository = PostRepository(),
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository(),
    private val blockRepo: BlockRepository = BlockRepository(),
    private val phpBridge: PhpBridgeRepository = PhpBridgeRepository()
) : ViewModel() {

    private val _feed = MutableStateFlow<List<Post>>(emptyList())
    val feed: StateFlow<List<Post>> = _feed.asStateFlow()

    private val _shaabiyat = MutableStateFlow<List<Post>>(emptyList())
    val shaabiyat: StateFlow<List<Post>> = _shaabiyat.asStateFlow()

    // صحيحة أثناء الجلب الأول فقط — تُستخدَم لعرض التحميل الهيكلي (Skeleton)
    // بدل قائمة فارغة "مضلِّلة" قد تُفهَم خطأً على أنها "لا توجد فقرات".
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var feedLoaded = false
    private var shaabiyatLoaded = false

    private fun markLoadedIfReady() {
        if (feedLoaded && shaabiyatLoaded) _isLoading.value = false
    }

    private val _myReactions = MutableStateFlow<Map<String, ReactionType>>(emptyMap())
    val myReactions: StateFlow<Map<String, ReactionType>> = _myReactions.asStateFlow()

    var isPosting by mutableStateOf(false); private set

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }

    val currentUid: String? get() = authRepo.currentUserId

    init {
        val uid = authRepo.currentUserId
        viewModelScope.launch {
            // نجلب "من يتابعهم" المستخدم الحالي ومن حظرهم/حظروه مرة واحدة لتطبيق الخصوصية والحظر على التغذية
            val followingIds = uid?.let { runCatching { userRepo.getTekingIds(it).toSet() }.getOrDefault(emptySet()) } ?: emptySet()
            val mutedIds = uid?.let { runCatching { blockRepo.getMutedIds(it) }.getOrDefault(emptySet()) } ?: emptySet()
            launch {
                postRepo.observeFeed(viewerId = uid, viewerFollowingIds = followingIds, mutedIds = mutedIds).collect {
                    _feed.value = it
                    feedLoaded = true
                    markLoadedIfReady()
                }
            }
            launch {
                postRepo.observeShaabiyat(viewerId = uid, viewerFollowingIds = followingIds, mutedIds = mutedIds).collect {
                    _shaabiyat.value = it
                    shaabiyatLoaded = true
                    markLoadedIfReady()
                }
            }
        }
        uid?.let {
            viewModelScope.launch {
                postRepo.observeMyReactions(it).collect { reactions -> _myReactions.value = reactions }
            }
        }
    }

    fun togglePin(post: Post) {
        val uid = authRepo.currentUserId ?: return
        if (uid != post.authorId) return
        viewModelScope.launch {
            runCatching {
                val me = userRepo.getUser(uid)
                postRepo.setPinned(uid, post.postId, !post.isPinned, me?.pinnedPostId)
            }.onFailure { _errorMessage.value = it.message ?: "تعذّر تثبيت الفقرة" }
        }
    }

    fun blockAuthor(authorId: String) {
        val uid = authRepo.currentUserId ?: return
        if (uid == authorId) return
        viewModelScope.launch {
            runCatching { blockRepo.block(uid, authorId) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر حظر المستخدم" }
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
        replyCommentId: String = "",
        replyCommentAuthorId: String = "",
        replyCommentAuthorUsername: String = "",
        replyCommentContent: String = "",
        /** إن كانت غير null، تُنشر الفقرة الجديدة كمتابعة لسلسلة تبدأ من (أو تمر عبر) هذه الفقرة. */
        continueFromPost: Post? = null,
        onDone: () -> Unit
    ) {
        val uid = authRepo.currentUserId ?: return
        if (content.isBlank() && imageBase64.isBlank()) return
        // القيمة الآمنة النهائية للإيموجي — تتجاهل أي قيمة خارج المجموعة المتاحة حاليًا (الميزة قيد التطوير)
        val safeEmoji = if (com.OPEN.OU.data.model.ParagraphEmoji.isValid(emoji)) emoji else ""
        // معرّف السلسلة: إن كانت الفقرة المصدر جزءًا من سلسلة موجودة نتابع نفس المعرّف،
        // وإلا فرأس السلسلة الجديدة هو معرّف تلك الفقرة نفسها.
        val threadId = continueFromPost?.let { it.threadId.ifBlank { it.postId } } ?: ""
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
                        scheduledAt = scheduledAt,
                        replyCommentId = replyCommentId,
                        replyCommentAuthorId = replyCommentAuthorId,
                        replyCommentAuthorUsername = replyCommentAuthorUsername,
                        replyCommentContent = replyCommentContent,
                        threadId = threadId
                    )
                )
            }.onSuccess { postId ->
                onDone()
                // بث إشعار "فقرة جديدة" لكل المستخدمين — فقط للفقرات العامة المنشورة فورًا
                // (الفقرات المجدولة scheduledAt تُستثنى: لا توجد آلية خلفية لإطلاق الإشعار عند حلول موعدها لاحقًا)
                val isImmediatePublic = privacy == "PUBLIC" && (scheduledAt == null || scheduledAt <= System.currentTimeMillis())
                if (isImmediatePublic) {
                    viewModelScope.launch {
                        runCatching {
                            phpBridge.notifyNewParagraphBestEffort(postId, authorUsername, content)
                        }.onFailure { _errorMessage.value = null } // Best-effort: لا نعرض أي خطأ للمستخدم بسببه
                    }
                }
                // إشعار المستخدمين المذكورين بمنشن (@اسم) — Best-effort، ولا يُرسل لصاحب الفقرة نفسه
                viewModelScope.launch {
                    runCatching { notifyMentionedUsersBestEffort(content, uid, authorUsername) }
                }
            }.onFailure {
                _errorMessage.value = it.message ?: "تعذّر نشر الفقرة، حاول مجددًا"
            }
            isPosting = false
        }
    }

    /** يعدّل محتوى فقرة يملكها المستخدم الحالي فقط. */
    fun editPost(postId: String, newContent: String, onDone: () -> Unit) {
        val uid = authRepo.currentUserId ?: return
        if (newContent.isBlank()) return
        viewModelScope.launch {
            runCatching { postRepo.updatePostContent(postId, uid, newContent) }
                .onSuccess { onDone() }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر تعديل الفقرة" }
        }
    }

    /** يحذف فقرة يملكها المستخدم الحالي فقط. */
    fun deletePost(post: Post) {
        val uid = authRepo.currentUserId ?: return
        if (uid != post.authorId) return
        viewModelScope.launch {
            runCatching { postRepo.deletePost(post.postId, uid) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر حذف الفقرة" }
        }
    }

    /** يبحث عن كل @منشنز في نص الفقرة ويرسل إشعار FCM Best-effort لكل مستخدم مذكور فعليًا موجود. */
    private suspend fun notifyMentionedUsersBestEffort(content: String, authorUid: String, authorUsername: String) {
        val usernames = com.OPEN.OU.ui.components.MentionUtils.extractMentions(content)
        if (usernames.isEmpty()) return
        usernames.forEach { username ->
            runCatching {
                val mentionedUid = userRepo.getUidByUsername(username) ?: return@runCatching
                if (mentionedUid == authorUid) return@runCatching
                val mentionedUser = userRepo.getUser(mentionedUid) ?: return@runCatching
                phpBridge.notifyBestEffort(
                    targetFcmToken = mentionedUser.fcmToken,
                    title = "منشن جديد على أوبو",
                    body = "ذكرك $authorUsername في فقرة"
                )
            }
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
