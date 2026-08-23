package com.OPEN.OU.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.model.User
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _room = MutableStateFlow<User?>(null)
    val room: StateFlow<User?> = _room.asStateFlow()

    private val _isTeking = MutableStateFlow(false)
    val isTeking: StateFlow<Boolean> = _isTeking.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }

    fun load(uid: String) {
        viewModelScope.launch {
            userRepo.observeUser(uid).collect { _room.value = it }
        }
        val myUid = authRepo.currentUserId
        if (myUid != null && myUid != uid) {
            viewModelScope.launch {
                _isTeking.value = userRepo.isTeking(myUid, uid)
            }
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
                if (goingToTek) userRepo.tek(myUid, tekerId) else userRepo.unTek(myUid, tekerId)
            } catch (e: Exception) {
                // لا نُسقط التطبيق أبدًا بسبب خطأ شبكة/صلاحيات — نتراجع ونعرض رسالة بدلًا من ذلك
                _isTeking.value = !goingToTek
                _errorMessage.value = e.message ?: "تعذّر إتمام العملية، حاول مجددًا"
            }
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
}
