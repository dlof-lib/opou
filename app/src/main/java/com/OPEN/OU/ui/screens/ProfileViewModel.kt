package com.OPEN.OU.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.model.CustomButton
import com.OPEN.OU.data.model.Post
import com.OPEN.OU.data.model.User
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.BlockRepository
import com.OPEN.OU.data.repository.PostRepository
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.network.PhpBridgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class ProfileViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val authRepo: AuthRepository = AuthRepository(),
    private val postRepo: PostRepository = PostRepository(),
    private val blockRepo: BlockRepository = BlockRepository(),
    private val phpBridge: PhpBridgeRepository = PhpBridgeRepository()
) : ViewModel() {

    private val _room = MutableStateFlow<User?>(null)
    private var loadJob: Job? = null
    private var postsJob: Job? = null
    private var loadedUid: String? = null
    val room: StateFlow<User?> = _room.asStateFlow()

    private val _isTeking = MutableStateFlow(false)
    val isTeking: StateFlow<Boolean> = _isTeking.asStateFlow()

    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }

    val currentUid: String? get() = authRepo.currentUserId

    fun load(uid: String) {
        if (uid.isBlank()) {
            _room.value = null
            _errorMessage.value = "معرّف الحساب غير صالح"
            return
        }
        if (loadedUid == uid && loadJob?.isActive == true) return
        loadedUid = uid

        loadJob?.cancel()
        postsJob?.cancel()

        loadJob = viewModelScope.launch {
            userRepo.observeUser(uid).collect { _room.value = it }
        }
        val myUid = authRepo.currentUserId
        if (myUid != null && myUid != uid) {
            viewModelScope.launch {
                _isTeking.value = runCatching { userRepo.isTeking(myUid, uid) }.getOrDefault(false)
            }
            viewModelScope.launch {
                _isBlocked.value = runCatching { blockRepo.isBlocked(myUid, uid) }.getOrDefault(false)
            }
        }
        postsJob = viewModelScope.launch {
            runCatching {
                val followingIds = myUid?.let {
                    runCatching { userRepo.getTekingIds(it).toSet() }.getOrDefault(emptySet())
                } ?: emptySet()
                val mutedIds = myUid?.let {
                    runCatching { blockRepo.getMutedIds(it) }.getOrDefault(emptySet())
                } ?: emptySet()
                postRepo.observeUserPosts(uid, myUid, followingIds, mutedIds)
                    .collect { _posts.value = it }
            }.onFailure {
                if (it !is kotlinx.coroutines.CancellationException) {
                    _posts.value = emptyList()
                }
            }
        }
    }

    fun toggleBlock(targetUid: String) {
        val myUid = authRepo.currentUserId ?: return
        if (myUid == targetUid) return
        viewModelScope.launch {
            val goingToBlock = !_isBlocked.value
            _isBlocked.value = goingToBlock
            runCatching {
                if (goingToBlock) {
                    blockRepo.block(myUid, targetUid)
                    if (_isTeking.value) { userRepo.unTek(myUid, targetUid); _isTeking.value = false }
                } else {
                    blockRepo.unblock(myUid, targetUid)
                }
            }.onFailure {
                _isBlocked.value = !goingToBlock
                _errorMessage.value = it.message ?: "تعذّر إتمام العملية"
            }
        }
    }

    fun togglePin(post: Post) {
        val uid = authRepo.currentUserId ?: return
        if (uid != post.authorId) return
        viewModelScope.launch {
            runCatching {
                postRepo.setPinned(uid, post.postId, !post.isPinned, _room.value?.pinnedPostId)
            }.onFailure { _errorMessage.value = it.message ?: "تعذّر تثبيت الفقرة" }
        }
    }

    fun toggleTek(tekerId: String) {
        val myUid = authRepo.currentUserId ?: return
        if (myUid == tekerId) return
        viewModelScope.launch {
            // تحديث متفائل فورًا لواجهة سلسة، مع تراجع تلقائي إن فشل الطلب
            val goingToTek = !_isTeking.value
            _isTeking.value = goingToTek
            try {
                if (goingToTek) {
                    userRepo.tek(myUid, tekerId)
                    notifyNewTekerBestEffort(tekerId)
                } else {
                    userRepo.unTek(myUid, tekerId)
                }
            } catch (e: Exception) {
                // لا نُسقط التطبيق أبدًا بسبب خطأ شبكة/صلاحيات — نتراجع ونعرض رسالة بدلًا من ذلك
                _isTeking.value = !goingToTek
                _errorMessage.value = e.message ?: "تعذّر إتمام العملية، حاول مجددًا"
            }
        }
    }

    /** إشعار PHP + FCM لمستخدم بأن أحدهم بدأ متابعته (تيك جديد) — Best-effort بالكامل. */
    private suspend fun notifyNewTekerBestEffort(tekerId: String) {
        runCatching {
            val myUid = authRepo.currentUserId ?: return
            val me = userRepo.getUser(myUid) ?: return
            val target = userRepo.getUser(tekerId) ?: return
            phpBridge.notifyBestEffort(
                targetFcmToken = target.fcmToken,
                title = "متابع جديد على أوبو",
                body = "${me.username} بدأ متابعتك (تيك)"
            )
        }
    }

    /** يحدّث الصورة الرمزية للغرفة فور جاهزيتها من ImageCodec (مضغوطة ومُرمّزة). */
    fun updateAvatar(uid: String, base64: String) {
        viewModelScope.launch {
            runCatching { userRepo.updateAvatar(uid, base64) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر حفظ الصورة الرمزية" }
        }
    }

    /** يحدّث صورة بانر الغرفة. */
    fun updateBanner(uid: String, base64: String) {
        viewModelScope.launch {
            runCatching { userRepo.updateBanner(uid, base64) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر حفظ صورة البانر" }
        }
    }

    /** يحدّث اسم المجتمع والسيرة الذاتية معًا في تحديث واحد (Realtime). */
    fun updateRoomInfo(uid: String, communityName: String, bio: String) {
        viewModelScope.launch {
            runCatching {
                userRepo.updateRoom(
                    uid,
                    mapOf(
                        "communityName" to communityName,
                        "bio" to bio
                    )
                )
            }.onFailure { _errorMessage.value = it.message ?: "تعذّر حفظ بيانات الغرفة" }
        }
    }

    fun updateCategories(uid: String, categories: List<String>) {
        viewModelScope.launch {
            runCatching { userRepo.updateCategories(uid, categories) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر حفظ التصنيفات" }
        }
    }

    fun updateSocialLinks(uid: String, links: Map<String, String>) {
        viewModelScope.launch {
            runCatching { userRepo.updateSocialLinks(uid, links) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر حفظ روابط التواصل" }
        }
    }

    fun updateCustomButtons(uid: String, buttons: List<CustomButton>) {
        viewModelScope.launch {
            runCatching { userRepo.updateCustomButtons(uid, buttons) }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر حفظ الأزرار المخصّصة" }
        }
    }
}
