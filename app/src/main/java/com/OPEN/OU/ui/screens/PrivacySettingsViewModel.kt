package com.OPEN.OU.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.model.CommentPermission
import com.OPEN.OU.data.model.User
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrivacySettingsViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _room = MutableStateFlow<User?>(null)
    val room: StateFlow<User?> = _room.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }

    fun load() {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            userRepo.observeUser(uid).collect { _room.value = it }
        }
    }

    fun setPrivateRoom(isPrivate: Boolean) = persist(isPrivate = isPrivate)

    fun setHideLastSeen(hide: Boolean) = persist(hideLastSeen = hide)

    fun setWhoCanComment(permission: CommentPermission) = persist(whoCanComment = permission)

    private fun persist(
        isPrivate: Boolean? = null,
        hideLastSeen: Boolean? = null,
        whoCanComment: CommentPermission? = null
    ) {
        val uid = authRepo.currentUserId ?: return
        val current = _room.value ?: return
        // تحديث متفائل فوري
        _room.value = current.copy(
            isPrivateRoom = isPrivate ?: current.isPrivateRoom,
            hideLastSeen = hideLastSeen ?: current.hideLastSeen,
            whoCanComment = whoCanComment?.name ?: current.whoCanComment
        )
        viewModelScope.launch {
            runCatching {
                userRepo.updatePrivacySettings(
                    uid,
                    isPrivateRoom = isPrivate ?: current.isPrivateRoom,
                    hideLastSeen = hideLastSeen ?: current.hideLastSeen,
                    whoCanComment = whoCanComment?.name ?: current.whoCanComment
                )
            }.onFailure { _errorMessage.value = it.message ?: "تعذّر حفظ إعدادات الخصوصية" }
        }
    }
}
