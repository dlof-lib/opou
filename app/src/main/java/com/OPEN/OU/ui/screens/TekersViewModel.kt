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

/** يدير شاشة "التيكرز": من يتابعك (تيكرز) ومن تتابعهم (تيكينغ). */
class TekersViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    val myUid: String? get() = authRepo.currentUserId

    private val _tekers = MutableStateFlow<List<User>>(emptyList())
    val tekers: StateFlow<List<User>> = _tekers.asStateFlow()

    private val _teking = MutableStateFlow<List<User>>(emptyList())
    val teking: StateFlow<List<User>> = _teking.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun load() {
        val uid = myUid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val tekerIds = userRepo.getTekerIds(uid)
                val tekingIds = userRepo.getTekingIds(uid)
                _tekers.value = userRepo.getUsersByIds(tekerIds)
                _teking.value = userRepo.getUsersByIds(tekingIds)
            }
            _isLoading.value = false
        }
    }
}
