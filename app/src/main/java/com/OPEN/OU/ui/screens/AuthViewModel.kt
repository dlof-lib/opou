package com.OPEN.OU.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    var isLoading by mutableStateOf(false); private set
    var errorMessage by mutableStateOf<String?>(null); private set

    fun login(email: String, password: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = repo.login(email, password)
            isLoading = false
            result.onSuccess(onSuccess)
                .onFailure { errorMessage = it.message }
        }
    }

    fun register(
        email: String,
        password: String,
        username: String,
        communityName: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = repo.register(email, password, username, communityName)
            isLoading = false
            result.onSuccess(onSuccess)
                .onFailure { errorMessage = it.message }
        }
    }
}
