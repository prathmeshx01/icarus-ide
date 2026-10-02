package com.example.ui.workspace

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FileRepository
import com.example.data.ProjectRepository
import com.example.editor.CodeDiagnostic
import com.example.editor.CodeFormatter
import com.example.editor.DiagnosticSeverity
import com.example.editor.ErrorDetector
import com.example.editor.QuickFix
import com.example.model.CommandItem
import com.example.model.CommandPaletteMode
import com.example.model.CommandPaletteState
import com.example.model.ConsoleLogEntry
import com.example.model.EditorTab
import com.example.model.FileNode
import com.example.model.Project
import com.example.model.ProjectFile
import com.example.model.ProjectType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.regex.Pattern

data class WorkspaceUiState(
    val project: Project? = null,
    val files: List<ProjectFile> = emptyList(),
    val fileTree: List<FileNode> = emptyList(),
    val openTabs: List<EditorTab> = emptyList(),
    val activeTabIndex: Int = 0,
    val currentEditorValue: TextFieldValue = TextFieldValue(""),
    val cursorLine: Int = 1,
    val cursorColumn: Int = 1,
    val diagnostics: List<CodeDiagnostic> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isExplorerOpen: Boolean = false,
    val isPreviewOpen: Boolean = false,
    val isConsoleOpen: Boolean = false,
    val isProblemsOpen: Boolean = false,
    val isSearchOpen: Boolean = false,
    val isTerminalOpen: Boolean = false,
    val showPreflightErrorDialog: Boolean = false,
    val preflightErrors: List<CodeDiagnostic> = emptyList(),
    val commandPaletteState: CommandPaletteState = CommandPaletteState(),
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val searchMatchCount: Int = 0,
    val consoleLogs: List<ConsoleLogEntry> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class WorkspaceViewModel(
    private val projectRepository: ProjectRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()

    fun loadProject(project: Project) {
        _uiState.update { it.copy(project = project, isPreviewOpen = false, showPreflightErrorDialog = false) }
        refreshFiles(autoOpenEntry = true)
    }

    fun refreshFiles(autoOpenEntry: Boolean = false) {
        val proj = _uiState.value.project ?: return
        viewModelScope.launch {
            val root = File(proj.rootDirPath)
            val list = fileRepository.listFiles(root)
            val tree = fileRepository.loadFileTree(root)
            _uiState.update { it.copy(files = list, fileTree = tree) }

            if (autoOpenEntry && _uiState.value.openTabs.isEmpty()) {
                val entryFile = when (proj.type) {
                    ProjectType.WEB, ProjectType.CANVAS, ProjectType.PORTFOLIO, ProjectType.BLANK ->
                        list.find { it.name.equals("index.html", ignoreCase = true) }
                    ProjectType.KOTLIN ->
                        list.find { it.name.equals("main.kt", ignoreCase = true) }
                    ProjectType.PYTHON ->
                        list.find { it.name.equals("main.py", ignoreCase = true) }
                } ?: list.firstOrNull { !it.isDirectory }

                entryFile?.let { openFile(it) }
            }
        }
    }

    fun openFile(file: ProjectFile) {
        val existingIndex = _uiState.value.openTabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex >= 0) {
            switchTab(existingIndex)
            return
        }

        viewModelScope.launch {
            val result = fileRepository.readFile(File(file.absolutePath))
            result.onSuccess { content ->
                val newTab = EditorTab(file = file, content = content, isDirty = false)
                val newTabs = _uiState.value.openTabs + newTab
                val newIndex = newTabs.lastIndex
                val errors = ErrorDetector.detectErrors(content, file.extension)

                undoStack.clear()
                redoStack.clear()

                _uiState.update {
                    it.copy(
                        openTabs = newTabs,
                        activeTabIndex = newIndex,
                        currentEditorValue = TextFieldValue(text = content, selection = TextRange(0)),
                        cursorLine = 1,
                        cursorColumn = 1,
                        diagnostics = errors,
                        canUndo = false,
                        canRedo = false
                    )
                }
            }.onFailure { err ->
                emitMessage("Failed to open file: ${err.message}")
            }
        }
    }

    fun switchTab(index: Int) {
        val tabs = _uiState.value.openTabs
        if (index !in tabs.indices) return
        val tab = tabs[index]
        val (line, col) = computeLineCol(tab.content, tab.content.length)
        val errors = ErrorDetector.detectErrors(tab.content, tab.file.extension)

        undoStack.clear()
        redoStack.clear()

        _uiState.update {
            it.copy(
                activeTabIndex = index,
                currentEditorValue = TextFieldValue(text = tab.content, selection = TextRange(tab.content.length)),
                cursorLine = line,
                cursorColumn = col,
                diagnostics = errors,
                canUndo = false,
                canRedo = false
            )
        }
    }

    fun closeTab(index: Int) {
        val currentTabs = _uiState.value.openTabs.toMutableList()
        if (index !in currentTabs.indices) return
        currentTabs.removeAt(index)

        val newActiveIndex = if (currentTabs.isEmpty()) {
            0
        } else if (index <= _uiState.value.activeTabIndex) {
            maxOf(0, _uiState.value.activeTabIndex - 1)
        } else {
            _uiState.value.activeTabIndex
        }

        val newEditorValue = if (currentTabs.isNotEmpty()) {
            TextFieldValue(text = currentTabs[newActiveIndex].content)
        } else {
            TextFieldValue("")
        }

        val newDiagnostics = if (currentTabs.isNotEmpty()) {
            val activeTab = currentTabs[newActiveIndex]
            ErrorDetector.detectErrors(activeTab.content, activeTab.file.extension)
        } else {
            emptyList()
        }

        undoStack.clear()
        redoStack.clear()

        _uiState.update {
            it.copy(
                openTabs = currentTabs,
                activeTabIndex = newActiveIndex,
                currentEditorValue = newEditorValue,
                diagnostics = newDiagnostics,
                canUndo = false,
                canRedo = false
            )
        }
    }

    fun closeOtherTabs(index: Int) {
        val tabs = _uiState.value.openTabs
        if (index !in tabs.indices) return
        val keep = tabs[index]
        _uiState.update {
            it.copy(
                openTabs = listOf(keep),
                activeTabIndex = 0
            )
        }
    }

    fun closeAllTabs() {
        undoStack.clear()
        redoStack.clear()
        _uiState.update {
            it.copy(
                openTabs = emptyList(),
                activeTabIndex = 0,
                currentEditorValue = TextFieldValue(""),
                diagnostics = emptyList(),
                canUndo = false,
                canRedo = false
            )
        }
    }

    fun onEditorTextChange(newValue: TextFieldValue) {
        val prev = _uiState.value.currentEditorValue
        if (prev.text != newValue.text) {
            undoStack.add(prev)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
        }

        applyNewValue(newValue)
    }

    private fun applyNewValue(newValue: TextFieldValue) {
        val activeIndex = _uiState.value.activeTabIndex
        val tabs = _uiState.value.openTabs.toMutableList()

        val (line, col) = computeLineCol(newValue.text, newValue.selection.min)

        if (activeIndex in tabs.indices) {
            val currentTab = tabs[activeIndex]
            val isDirty = newValue.text != currentTab.lastSavedContent
            tabs[activeIndex] = currentTab.copy(content = newValue.text, isDirty = isDirty)

            val errors = ErrorDetector.detectErrors(newValue.text, currentTab.file.extension)

            _uiState.update {
                it.copy(
                    openTabs = tabs,
                    currentEditorValue = newValue,
                    cursorLine = line,
                    cursorColumn = col,
                    diagnostics = errors,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    currentEditorValue = newValue,
                    cursorLine = line,
                    cursorColumn = col,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value.currentEditorValue
        val last = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(current)
        applyNewValue(last)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value.currentEditorValue
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(current)
        applyNewValue(next)
    }

    fun insertSymbolWithOffset(symbol: String, cursorOffset: Int) {
        val currentValue = _uiState.value.currentEditorValue
        val selection = currentValue.selection
        val text = currentValue.text

        val newText = buildString {
            append(text.substring(0, selection.min))
            append(symbol)
            append(text.substring(selection.max))
        }
        val newCursor = selection.min + cursorOffset
        onEditorTextChange(TextFieldValue(newText, TextRange(newCursor)))
    }

    fun indentCurrentLine() {
        val currentValue = _uiState.value.currentEditorValue
        val cursor = currentValue.selection.min
        val text = currentValue.text

        val lastNewline = text.lastIndexOf('\n', startIndex = (cursor - 1).coerceAtLeast(0))
        val lineStart = if (lastNewline < 0) 0 else lastNewline + 1

        val newText = text.substring(0, lineStart) + "  " + text.substring(lineStart)
        val newCursor = cursor + 2
        onEditorTextChange(TextFieldValue(newText, TextRange(newCursor)))
    }

    fun formatDocument() {
        val activeIndex = _uiState.value.activeTabIndex
        val tabs = _uiState.value.openTabs
        if (activeIndex !in tabs.indices) return

        val currentTab = tabs[activeIndex]
        val formatted = CodeFormatter.format(currentTab.content, currentTab.file.extension)
        if (formatted != currentTab.content) {
            onEditorTextChange(TextFieldValue(formatted, TextRange(0)))
            emitMessage("Formatted document")
        } else {
            emitMessage("Document already formatted")
        }
    }

    fun applyQuickFix(fix: QuickFix) {
        val currentText = _uiState.value.currentEditorValue.text
        val fixedText = fix.applyFix(currentText)
        onEditorTextChange(TextFieldValue(fixedText, TextRange(fixedText.length)))
        emitMessage("Applied fix: ${fix.title}")
    }

    fun jumpToDiagnostic(diag: CodeDiagnostic) {
        val text = _uiState.value.currentEditorValue.text
        val lines = text.split("\n")
        var charIdx = 0
        val targetLine = (diag.line - 1).coerceIn(0, maxOf(0, lines.lastIndex))

        for (i in 0 until targetLine) {
            charIdx += lines[i].length + 1
        }
        charIdx += (diag.column - 1).coerceIn(0, lines.getOrElse(targetLine) { "" }.length)
        val safeIdx = charIdx.coerceIn(0, text.length)

        _uiState.update {
            it.copy(
                currentEditorValue = TextFieldValue(text, TextRange(safeIdx)),
                isProblemsOpen = false,
                showPreflightErrorDialog = false
            )
        }
    }

    fun saveCurrentTab(formatOnSave: Boolean = false, onSaved: (() -> Unit)? = null) {
        val activeIndex = _uiState.value.activeTabIndex
        val tabs = _uiState.value.openTabs.toMutableList()
        if (activeIndex !in tabs.indices) return

        val currentTab = tabs[activeIndex]
        val file = File(currentTab.file.absolutePath)
        var textToSave = _uiState.value.currentEditorValue.text

        if (formatOnSave) {
            textToSave = CodeFormatter.format(textToSave, currentTab.file.extension)
            _uiState.update {
                it.copy(currentEditorValue = TextFieldValue(textToSave, it.currentEditorValue.selection))
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val res = fileRepository.saveFile(file, textToSave)
            res.onSuccess {
                tabs[activeIndex] = currentTab.copy(
                    content = textToSave,
                    isDirty = false,
                    lastSavedContent = textToSave
                )
                _uiState.update { it.copy(openTabs = tabs, isSaving = false) }
                _uiState.value.project?.let { p -> projectRepository.touchProject(p) }
                emitMessage("Saved ${currentTab.file.name}")
                onSaved?.invoke()
            }.onFailure { err ->
                _uiState.update { it.copy(isSaving = false) }
                emitMessage("Error saving file: ${err.message}")
            }
        }
    }

    fun toggleProblems() {
        _uiState.update { it.copy(isProblemsOpen = !it.isProblemsOpen) }
    }

    fun toggleTerminal() {
        _uiState.update { it.copy(isTerminalOpen = !it.isTerminalOpen) }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(isSearchOpen = !it.isSearchOpen) }
    }

    fun openCommandPalette(mode: CommandPaletteMode = CommandPaletteMode.COMMANDS) {
        _uiState.update {
            it.copy(
                commandPaletteState = CommandPaletteState(
                    isOpen = true,
                    searchQuery = if (mode == CommandPaletteMode.COMMANDS) ">" else "",
                    mode = mode
                )
            )
        }
    }

    fun closeCommandPalette() {
        _uiState.update {
            it.copy(commandPaletteState = it.commandPaletteState.copy(isOpen = false))
        }
    }

    fun dismissPreflightDialog() {
        _uiState.update { it.copy(showPreflightErrorDialog = false) }
    }

    fun setSearchQuery(query: String) {
        val text = _uiState.value.currentEditorValue.text
        val count = if (query.isNotBlank()) {
            query.toRegex(RegexOption.IGNORE_CASE).findAll(text).count()
        } else 0
        _uiState.update { it.copy(searchQuery = query, searchMatchCount = count) }
    }

    fun setReplaceQuery(query: String) {
        _uiState.update { it.copy(replaceQuery = query) }
    }

    fun findNext(isRegex: Boolean) {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return
        val text = _uiState.value.currentEditorValue.text
        val currentCursor = _uiState.value.currentEditorValue.selection.max

        val pattern = if (isRegex) {
            try { Pattern.compile(query, Pattern.CASE_INSENSITIVE) } catch (_: Exception) { null }
        } else null

        val (targetStart, targetEnd) = if (pattern != null) {
            val matcher = pattern.matcher(text)
            if (matcher.find(currentCursor)) {
                matcher.start() to matcher.end()
            } else if (matcher.find(0)) {
                matcher.start() to matcher.end()
            } else -1 to -1
        } else {
            val nextIndex = text.indexOf(query, startIndex = currentCursor, ignoreCase = true)
            val idx = if (nextIndex >= 0) nextIndex else text.indexOf(query, startIndex = 0, ignoreCase = true)
            if (idx >= 0) idx to (idx + query.length) else -1 to -1
        }

        if (targetStart >= 0) {
            _uiState.update {
                it.copy(
                    currentEditorValue = TextFieldValue(
                        text = text,
                        selection = TextRange(targetStart, targetEnd)
                    )
                )
            }
        } else {
            emitMessage("No matches found for '$query'")
        }
    }

    fun replaceAll(isRegex: Boolean) {
        val query = _uiState.value.searchQuery
        val replaceWith = _uiState.value.replaceQuery
        if (query.isBlank()) return
        val text = _uiState.value.currentEditorValue.text

        val replaced = if (isRegex) {
            try { text.replace(Regex(query, RegexOption.IGNORE_CASE), replaceWith) } catch (_: Exception) { text }
        } else {
            text.replace(query, replaceWith, ignoreCase = true)
        }

        onEditorTextChange(TextFieldValue(replaced, TextRange(0)))
        emitMessage("Replaced matches for '$query'")
    }

    fun runProject(force: Boolean = false) {
        val proj = _uiState.value.project ?: return
        if (!proj.type.isExecutableV01) {
            emitMessage("${proj.type.displayName} execution runtime is coming soon. Use Terminal Shell to inspect code.")
            return
        }

        // Check if there are active syntax/parsing errors in current editor or tabs
        val currentCode = _uiState.value.currentEditorValue.text
        val activeExt = _uiState.value.openTabs.getOrNull(_uiState.value.activeTabIndex)?.file?.extension ?: "html"
        val detectedErrors = ErrorDetector.detectErrors(currentCode, activeExt)
        val severeErrors = detectedErrors.filter { it.severity == DiagnosticSeverity.ERROR }

        if (!force && severeErrors.isNotEmpty()) {
            // Show pre-flight error validation dialog explaining errors
            _uiState.update {
                it.copy(
                    showPreflightErrorDialog = true,
                    preflightErrors = severeErrors,
                    diagnostics = detectedErrors
                )
            }
            return
        }

        // Proceed to save and launch offline preview
        _uiState.update { it.copy(showPreflightErrorDialog = false) }
        saveCurrentTab {
            _uiState.update {
                it.copy(
                    isPreviewOpen = true,
                    consoleLogs = listOf(
                        ConsoleLogEntry("INFO", "Starting offline WebView rendering engine..."),
                        ConsoleLogEntry("INFO", "Workspace: ${File(proj.rootDirPath).name}")
                    )
                )
            }
        }
    }

    fun closePreview() {
        _uiState.update { it.copy(isPreviewOpen = false) }
    }

    fun addConsoleLog(level: String, message: String, sourceId: String? = null, lineNumber: Int? = null) {
        val entry = ConsoleLogEntry(level, message, sourceId, lineNumber)
        _uiState.update { it.copy(consoleLogs = it.consoleLogs + entry) }
    }

    fun clearConsoleLogs() {
        _uiState.update { it.copy(consoleLogs = emptyList()) }
    }

    fun toggleConsole() {
        _uiState.update { it.copy(isConsoleOpen = !it.isConsoleOpen) }
    }

    fun toggleExplorer() {
        _uiState.update { it.copy(isExplorerOpen = !it.isExplorerOpen) }
    }

    fun createFile(parentDir: String, name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            emitMessage("File name cannot be empty")
            return
        }
        viewModelScope.launch {
            val res = fileRepository.createFile(File(parentDir), cleanName)
            res.onSuccess { newFile ->
                emitMessage("Created $cleanName")
                refreshFiles()
                val proj = _uiState.value.project
                val relPath = if (proj != null) newFile.relativeTo(File(proj.rootDirPath)).path else cleanName
                val projectFile = ProjectFile(
                    name = newFile.name,
                    relativePath = relPath,
                    extension = newFile.extension.lowercase(),
                    isDirectory = false,
                    absolutePath = newFile.absolutePath,
                    sizeBytes = 0L
                )
                openFile(projectFile)
            }.onFailure { err ->
                emitMessage("Failed to create file: ${err.message}")
            }
        }
    }

    fun createFolder(parentDir: String, name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            emitMessage("Folder name cannot be empty")
            return
        }
        viewModelScope.launch {
            val res = fileRepository.createFolder(File(parentDir), cleanName)
            res.onSuccess {
                emitMessage("Created folder $cleanName")
                refreshFiles()
            }.onFailure { err ->
                emitMessage("Failed to create folder: ${err.message}")
            }
        }
    }

    fun renameItem(node: FileNode, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) {
            emitMessage("New name cannot be empty")
            return
        }
        viewModelScope.launch {
            val res = fileRepository.renameFile(File(node.absolutePath), cleanName)
            res.onSuccess { renamedFile ->
                emitMessage("Renamed to $cleanName")
                val tabIdx = _uiState.value.openTabs.indexOfFirst { it.file.absolutePath == node.absolutePath }
                if (tabIdx >= 0) {
                    val currentTabs = _uiState.value.openTabs.toMutableList()
                    val oldTab = currentTabs[tabIdx]
                    val updatedFile = oldTab.file.copy(
                        name = renamedFile.name,
                        relativePath = cleanName,
                        extension = renamedFile.extension.lowercase(),
                        absolutePath = renamedFile.absolutePath
                    )
                    currentTabs[tabIdx] = oldTab.copy(file = updatedFile)
                    _uiState.update { it.copy(openTabs = currentTabs) }
                }
                refreshFiles()
            }.onFailure { err ->
                emitMessage("Failed to rename: ${err.message}")
            }
        }
    }

    fun deleteItem(node: FileNode) {
        viewModelScope.launch {
            val res = fileRepository.deleteFile(File(node.absolutePath))
            res.onSuccess {
                emitMessage("Deleted ${node.name}")
                val tabIdx = _uiState.value.openTabs.indexOfFirst { it.file.absolutePath == node.absolutePath }
                if (tabIdx >= 0) {
                    closeTab(tabIdx)
                }
                refreshFiles()
            }.onFailure { err ->
                emitMessage("Failed to delete: ${err.message}")
            }
        }
    }

    private fun computeLineCol(text: String, charIndex: Int): Pair<Int, Int> {
        var line = 1
        var col = 1
        val safe = charIndex.coerceIn(0, text.length)
        for (i in 0 until safe) {
            if (text[i] == '\n') {
                line++
                col = 1
            } else {
                col++
            }
        }
        return line to col
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch {
            _toastEvents.emit(msg)
        }
    }
}
