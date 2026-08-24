package com.OPEN.OU.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.model.User
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.BlockRepository
import com.OPEN.OU.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlockedUsersViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository(),
    private val blockRepo: BlockRepository = BlockRepository()
) : ViewModel() {

    private val _blockedUsers = MutableStateFlow<List<User>>(emptyList())
    val blockedUsers: StateFlow<List<User>> = _blockedUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun load() {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            blockRepo.observeBlockedIds(uid).collect { ids ->
                _isLoading.value = true
                _blockedUsers.value = runCatching { userRepo.getUsersByIds(ids) }.getOrDefault(emptyList())
                _isLoading.value = false
            }
        }
    }

    fun unblock(targetUid: String) {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            runCatching { blockRepo.unblock(uid, targetUid) }
        }
    }
}
