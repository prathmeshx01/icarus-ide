package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.AppEditorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EditorSettings(
    val theme: AppEditorTheme = AppEditorTheme.OBSIDIAN_DARK,
    val fontSizeSp: Float = 13.0f,
    val tabSize: Int = 2,
    val wordWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val autoCloseBrackets: Boolean = true,
    val formatOnSave: Boolean = false
)

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("icarus_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<EditorSettings> = _settings.asStateFlow()

    private fun loadSettings(): EditorSettings {
        val themeId = prefs.getString("editor_theme", AppEditorTheme.OBSIDIAN_DARK.id)
        val theme = AppEditorTheme.values().find { it.id == themeId } ?: AppEditorTheme.OBSIDIAN_DARK
        val fontSize = prefs.getFloat("font_size", 13.0f)
        val tabSize = prefs.getInt("tab_size", 2)
        val wordWrap = prefs.getBoolean("word_wrap", false)
        val showLineNumbers = prefs.getBoolean("show_line_numbers", true)
        val autoClose = prefs.getBoolean("auto_close_brackets", true)
        val formatOnSave = prefs.getBoolean("format_on_save", false)

        return EditorSettings(
            theme = theme,
            fontSizeSp = fontSize,
            tabSize = tabSize,
            wordWrap = wordWrap,
            showLineNumbers = showLineNumbers,
            autoCloseBrackets = autoClose,
            formatOnSave = formatOnSave
        )
    }

    fun setTheme(theme: AppEditorTheme) {
        prefs.edit().putString("editor_theme", theme.id).apply()
        _settings.update { it.copy(theme = theme) }
    }

    fun setFontSize(size: Float) {
        prefs.edit().putFloat("font_size", size).apply()
        _settings.update { it.copy(fontSizeSp = size) }
    }

    fun setTabSize(spaces: Int) {
        prefs.edit().putInt("tab_size", spaces).apply()
        _settings.update { it.copy(tabSize = spaces) }
    }

    fun setWordWrap(enabled: Boolean) {
        prefs.edit().putBoolean("word_wrap", enabled).apply()
        _settings.update { it.copy(wordWrap = enabled) }
    }

    fun setShowLineNumbers(enabled: Boolean) {
        prefs.edit().putBoolean("show_line_numbers", enabled).apply()
        _settings.update { it.copy(showLineNumbers = enabled) }
    }

    fun setAutoCloseBrackets(enabled: Boolean) {
        prefs.edit().putBoolean("auto_close_brackets", enabled).apply()
        _settings.update { it.copy(autoCloseBrackets = enabled) }
    }

    fun setFormatOnSave(enabled: Boolean) {
        prefs.edit().putBoolean("format_on_save", enabled).apply()
        _settings.update { it.copy(formatOnSave = enabled) }
    }
}
