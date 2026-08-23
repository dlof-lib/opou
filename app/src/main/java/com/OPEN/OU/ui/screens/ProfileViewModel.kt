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
            if (_isTeking.value) {
                userRepo.unTek(myUid, tekerId)
                _isTeking.value = false
            } else {
                userRepo.tek(myUid, tekerId)
                _isTeking.value = true
            }
        }
    }

    /** يحدّث الصورة الرمزية للغرفة فور جاهزيتها من ImageCodec (مضغوطة ومُرمّزة). */
    fun updateAvatar(uid: String, base64: String) {
        viewModelScope.launch { userRepo.updateAvatar(uid, base64) }
    }

    /** يحدّث صورة بانر الغرفة. */
    fun updateBanner(uid: String, base64: String) {
        viewModelScope.launch { userRepo.updateBanner(uid, base64) }
    }

    /** يحدّث اسم المجتمع والسيرة الذاتية معًا في تحديث واحد (Realtime). */
    fun updateRoomInfo(uid: String, communityName: String, bio: String) {
        viewModelScope.launch {
            userRepo.updateRoom(
                uid,
                mapOf(
                    "communityName" to communityName,
                    "bio" to bio
                )
            )
        }
    }
}
