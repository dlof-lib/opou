package com.OPEN.OU.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.model.AccountStatus
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.util.PinHash
import kotlinx.coroutines.launch

/** خطوات تسجيل الدخول: بيانات الاعتماد العادية، أو خطوة رمز PIN إضافية إن كان التحقق بخطوتين مفعّلاً. */
enum class LoginStep { CREDENTIALS, TWO_FACTOR_PIN }

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    var isLoading by mutableStateOf(false); private set
    var errorMessage by mutableStateOf<String?>(null); private set
    var loginStep by mutableStateOf(LoginStep.CREDENTIALS); private set
    var reactivatedNotice by mutableStateOf(false); private set

    private var pendingUid: String? = null

    fun login(email: String, password: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = repo.login(email, password)
            result.onSuccess { uid ->
                val user = runCatching { userRepo.getUser(uid) }.getOrNull()

                // تعطيل مؤقت؟ إعادة تفعيل تلقائية عند تسجيل الدخول
                if (user != null && AccountStatus.fromValue(user.accountStatus) == AccountStatus.DEACTIVATED) {
                    runCatching { userRepo.setAccountStatus(uid, "ACTIVE") }
                    reactivatedNotice = true
                }

                if (user?.twoFactorEnabled == true) {
                    pendingUid = uid
                    loginStep = LoginStep.TWO_FACTOR_PIN
                    isLoading = false
                } else {
                    isLoading = false
                    onSuccess(uid)
                }
            }.onFailure {
                isLoading = false
                errorMessage = it.message
            }
        }
    }

    /** التحقق من رمز PIN في خطوة التحقق بخطوتين. */
    fun verifyTwoFactorPin(pin: String, onSuccess: (String) -> Unit) {
        val uid = pendingUid ?: return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val user = runCatching { userRepo.getUser(uid) }.getOrNull()
            isLoading = false
            if (user != null && PinHash.matches(uid, pin, user.twoFactorPinHash)) {
                onSuccess(uid)
            } else {
                errorMessage = "رمز التحقق غير صحيح"
            }
        }
    }

    fun cancelTwoFactor() {
        pendingUid = null
        loginStep = LoginStep.CREDENTIALS
        errorMessage = null
        repo.logout()
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
