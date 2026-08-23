package com.OPEN.OU.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.languageDataStore by preferencesDataStore(name = "opou_settings")
private val LANGUAGE_KEY = stringPreferencesKey("app_language")

/** اللغات المدعومة حاليًا في أوبو */
enum class AppLanguage(val tag: String, val displayNameAr: String, val displayNameEn: String) {
    ARABIC("ar", "العربية", "Arabic"),
    ENGLISH("en", "الإنجليزية", "English");

    companion object {
        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: ARABIC
    }
}

/**
 * يدير تفضيل لغة الواجهة (عربي/إنجليزي) بشكل مستقل عن لغة نظام الجهاز،
 * عبر آلية "لغة لكل تطبيق" الرسمية من AndroidX (AppCompatDelegate + LocaleListCompat)،
 * مع حفظ محلي دائم عبر DataStore حتى يُطبَّق فورًا عند فتح التطبيق من جديد.
 */
object LanguagePrefs {

    /** يقرأ اللغة المحفوظة محليًا كتدفّق (Flow) لعرضها فورًا في شاشة الإعدادات. */
    fun observe(context: Context): Flow<AppLanguage> =
        context.languageDataStore.data.map { prefs ->
            AppLanguage.fromTag(prefs[LANGUAGE_KEY] ?: "ar")
        }

    /** يحفظ اللغة المختارة محليًا، ثم يطبّقها فورًا على التطبيق بالكامل (يُعيد بناء الشاشات تلقائيًا). */
    suspend fun setLanguage(context: Context, language: AppLanguage) {
        context.languageDataStore.edit { prefs -> prefs[LANGUAGE_KEY] = language.tag }
        applyLocale(language)
    }

    /** يطبّق لغة معيّنة فورًا على مستوى التطبيق كامل عبر AppCompatDelegate (متوافق مع كل إصدارات أندرويد المدعومة). */
    fun applyLocale(language: AppLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
