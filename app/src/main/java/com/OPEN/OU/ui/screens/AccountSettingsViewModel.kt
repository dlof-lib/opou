package com.OPEN.OU.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.model.User
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.util.PinHash
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountSettingsViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _room = MutableStateFlow<User?>(null)
    val room: StateFlow<User?> = _room.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    val currentEmail: String? get() = authRepo.currentUserEmail

    fun clearMessages() { _errorMessage.value = null; _successMessage.value = null }

    fun load() {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            userRepo.observeUser(uid).collect { _room.value = it }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _errorMessage.value = "كلمة المرور الجديدة يجب أن تكون 6 أحرف على الأقل"
            return
        }
        if (newPassword != confirmPassword) {
            _errorMessage.value = "كلمتا المرور الجديدتان غير متطابقتين"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            authRepo.changePassword(currentPassword, newPassword)
                .onSuccess { _successMessage.value = "تم تغيير كلمة المرور بنجاح" }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر تغيير كلمة المرور" }
            _isLoading.value = false
        }
    }

    /** تعطيل الحساب مؤقتًا: يُخفى المستخدم عن الآخرين ويُسجَّل خروجه، ويُعاد تفعيله تلقائيًا عند دخوله مجددًا. */
    fun deactivateAccount(onDone: () -> Unit) {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { userRepo.setAccountStatus(uid, "DEACTIVATED") }
                .onSuccess {
                    authRepo.logout()
                    onDone()
                }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر تعطيل الحساب" }
            _isLoading.value = false
        }
    }

    /** حذف الحساب نهائيًا: يحذف بيانات Realtime Database ثم حساب المصادقة نفسه، بعد إعادة التوثيق بكلمة المرور. */
    fun deleteAccountPermanently(password: String, onDone: () -> Unit) {
        val uid = authRepo.currentUserId ?: return
        val username = _room.value?.username.orEmpty()
        viewModelScope.launch {
            _isLoading.value = true
            val reauth = authRepo.reauthenticate(password)
            if (reauth.isFailure) {
                _errorMessage.value = reauth.exceptionOrNull()?.message ?: "كلمة المرور غير صحيحة"
                _isLoading.value = false
                return@launch
            }
            runCatching { userRepo.deleteUserDataPermanently(uid, username) }
            authRepo.deleteAuthAccount(password)
                .onSuccess { onDone() }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر حذف الحساب نهائيًا" }
            _isLoading.value = false
        }
    }

    fun enableTwoFactor(pin: String, confirmPin: String) {
        val uid = authRepo.currentUserId ?: return
        if (pin.length < 4) {
            _errorMessage.value = "رمز PIN يجب أن يكون 4 أرقام على الأقل"
            return
        }
        if (pin != confirmPin) {
            _errorMessage.value = "رمزا PIN غير متطابقين"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { userRepo.enableTwoFactor(uid, PinHash.hash(uid, pin)) }
                .onSuccess { _successMessage.value = "تم تفعيل التحقق بخطوتين" }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر تفعيل التحقق بخطوتين" }
            _isLoading.value = false
        }
    }

    fun disableTwoFactor() {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { userRepo.disableTwoFactor(uid) }
                .onSuccess { _successMessage.value = "تم إيقاف التحقق بخطوتين" }
                .onFailure { _errorMessage.value = it.message ?: "تعذّر إيقاف التحقق بخطوتين" }
            _isLoading.value = false
        }
    }

    fun logout() = authRepo.logout()
}
