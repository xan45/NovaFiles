package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

enum class AppThemeMode(val displayName: String, val description: String) {
    MATERIAL_YOU("Material You", "Dynamic colors adapting to your device wallpaper"),
    FROSTED_GLASS("Frosted Glass UI", "Glassmorphic blurred translucent cards with glowing borders"),
    CLEAR_TRANSPARENT("Clear UI", "Minimalist sleek ultra-clean transparent floating containers")
}

enum class DarkModeOption(val displayName: String) {
    SYSTEM("Follow System"),
    LIGHT("Light"),
    DARK("Dark")
}

class ThemePreferencesRepository(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
    }

    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_THEME_MODE] ?: AppThemeMode.MATERIAL_YOU.name
        try {
            AppThemeMode.valueOf(name)
        } catch (e: Exception) {
            AppThemeMode.MATERIAL_YOU
        }
    }

    val darkModeFlow: Flow<DarkModeOption> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_DARK_MODE] ?: DarkModeOption.SYSTEM.name
        try {
            DarkModeOption.valueOf(name)
        } catch (e: Exception) {
            DarkModeOption.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setDarkMode(option: DarkModeOption) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = option.name
        }
    }
}
