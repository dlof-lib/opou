package com.OPEN.OU.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.OPEN.OU.data.repository.AuthRepository
import com.OPEN.OU.data.repository.UserRepository
import com.OPEN.OU.util.AppLanguage
import com.OPEN.OU.util.LanguagePrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
) : AndroidViewModel(application) {

    private val _language = MutableStateFlow(AppLanguage.ARABIC)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    init {
        viewModelScope.launch {
            LanguagePrefs.observe(getApplication()).collect { _language.value = it }
        }
    }

    /** يحفظ اللغة محليًا (وتُطبَّق فورًا)، ثم يزامنها اختياريًا مع Firebase إن كان المستخدم مسجّلًا. */
    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch {
            LanguagePrefs.setLanguage(getApplication(), language)
            authRepo.currentUserId?.let { uid ->
                runCatching { userRepo.saveLanguage(uid, language.tag) }
            }
        }
    }

    fun logout() = authRepo.logout()
}
