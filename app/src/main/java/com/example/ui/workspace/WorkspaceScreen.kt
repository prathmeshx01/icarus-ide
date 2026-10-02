package com.example.ui.workspace

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Css
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsManager
import com.example.editor.CodeDiagnostic
import com.example.editor.DiagnosticSeverity
import com.example.model.CommandItem
import com.example.model.CommandPaletteMode
import com.example.model.EditorTab
import com.example.model.ProjectType
import com.example.ui.theme.AppEditorTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel,
    settingsManager: SettingsManager,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val settings by settingsManager.settings.collectAsState()
    val theme = settings.theme

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Intercept hardware/gesture Back button
    BackHandler {
        when {
            state.isPreviewOpen -> viewModel.closePreview()
            state.isProblemsOpen -> viewModel.toggleProblems()
            state.isTerminalOpen -> viewModel.toggleTerminal()
            state.isSearchOpen -> viewModel.toggleSearch()
            state.commandPaletteState.isOpen -> viewModel.closeCommandPalette()
            drawerState.isOpen -> scope.launch { drawerState.close() }
            else -> onNavigateBack()
        }
    }

    // List of Commands for Command Palette
    val commands = remember(theme, state.openTabs) {
        listOf(
            CommandItem("cmd_run", "Run: Live Web Preview", "Execution", "F5") { viewModel.runProject() },
            CommandItem("cmd_format", "Format Document", "Editor", "Shift+Alt+F") { viewModel.formatDocument() },
            CommandItem("cmd_find", "Find & Replace", "Editor", "Ctrl+F") { viewModel.toggleSearch() },
            CommandItem("cmd_terminal", "Toggle Terminal Shell", "Terminal", "Ctrl+`") { viewModel.toggleTerminal() },
            CommandItem("cmd_problems", "Toggle Problems Panel", "View") { viewModel.toggleProblems() },
            CommandItem("cmd_save", "File: Save", "File", "Ctrl+S") { viewModel.saveCurrentTab(formatOnSave = settings.formatOnSave) },
            CommandItem("cmd_close_all", "Close All Tabs", "Tabs") { viewModel.closeAllTabs() },
            CommandItem("cmd_theme_dark", "Preferences: Color Theme - Obsidian Dark", "Theme") { settingsManager.setTheme(AppEditorTheme.OBSIDIAN_DARK) },
            CommandItem("cmd_theme_one", "Preferences: Color Theme - One Dark", "Theme") { settingsManager.setTheme(AppEditorTheme.ONE_DARK) },
            CommandItem("cmd_theme_dracula", "Preferences: Color Theme - Dracula", "Theme") { settingsManager.setTheme(AppEditorTheme.DRACULA) },
            CommandItem("cmd_theme_monokai", "Preferences: Color Theme - Monokai Pro", "Theme") { settingsManager.setTheme(AppEditorTheme.MONOKAI_PRO) },
            CommandItem("cmd_theme_night_owl", "Preferences: Color Theme - Night Owl", "Theme") { settingsManager.setTheme(AppEditorTheme.NIGHT_OWL) },
            CommandItem("cmd_settings", "Open Settings", "Preferences", "Ctrl+,") { onNavigateToSettings() }
        )
    }

    // Web Live Preview Pane
    if (state.isPreviewOpen && state.project != null) {
        WebPreviewPane(
            project = state.project!!,
            consoleLogs = state.consoleLogs,
            onConsoleLog = { level, msg, src, line ->
                viewModel.addConsoleLog(level, msg, src, line)
            },
            onClearLogs = { viewModel.clearConsoleLogs() },
            onClose = { viewModel.closePreview() }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = theme.sidebarBg,
                modifier = Modifier.width(290.dp)
            ) {
                if (state.project != null) {
                    val activeFilePath = state.openTabs.getOrNull(state.activeTabIndex)?.file?.absolutePath
                    ProjectExplorerDrawer(
                        project = state.project!!,
                        fileTree = state.fileTree,
                        activeFilePath = activeFilePath,
                        theme = theme,
                        onFileSelected = { file ->
                            viewModel.openFile(file)
                            scope.launch { drawerState.close() }
                        },
                        onCreateFile = { parentDir, name -> viewModel.createFile(parentDir, name) },
                        onCreateFolder = { parentDir, name -> viewModel.createFolder(parentDir, name) },
                        onRenameItem = { node, newName -> viewModel.renameItem(node, newName) },
                        onDeleteItem = { node -> viewModel.deleteItem(node) },
                        onRefresh = { viewModel.refreshFiles() },
                        onClose = { scope.launch { drawerState.close() } }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // High-Density Dark Matte Zinc Title Bar
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = theme.titleBarBg,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("workspace_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Project")
                        }
                    },
                    title = {
                        Column {
                            val currentTab = state.openTabs.getOrNull(state.activeTabIndex)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.project?.name ?: "ICARUS",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = " > ${currentTab?.file?.name ?: ""}",
                                    fontSize = 12.sp,
                                    color = theme.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    actions = {
                        // Command Palette Quick Action Trigger (Ctrl+Shift+P)
                        IconButton(
                            onClick = { viewModel.openCommandPalette(CommandPaletteMode.COMMANDS) },
                            modifier = Modifier.testTag("command_palette_trigger_btn")
                        ) {
                            Icon(
                                Icons.Default.KeyboardCommandKey,
                                contentDescription = "Command Palette",
                                tint = theme.accentColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Global File Search (Ctrl+P)
                        IconButton(
                            onClick = { viewModel.openCommandPalette(CommandPaletteMode.FILES) },
                            modifier = Modifier.testTag("quick_open_file_btn")
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Quick Open File",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Terminal Shell Toggle
                        IconButton(
                            onClick = { viewModel.toggleTerminal() },
                            modifier = Modifier.testTag("workspace_terminal_btn")
                        ) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = "Terminal Shell",
                                tint = if (state.isTerminalOpen) theme.accentColor else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Project Explorer Drawer
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("workspace_explorer_btn")
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Explorer", tint = Color.White)
                        }

                        // Save Button
                        val hasDirty = state.openTabs.any { it.isDirty }
                        IconButton(
                            onClick = { viewModel.saveCurrentTab(formatOnSave = settings.formatOnSave) },
                            modifier = Modifier.testTag("workspace_save_btn")
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = if (hasDirty) theme.accentColor else theme.textSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                                if (hasDirty) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(theme.accentColor, CircleShape)
                                    )
                                }
                            }
                        }

                        // Run / Preview Button with Error Status Indication
                        val severeErrorCount = remember(state.diagnostics) {
                            state.diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
                        }
                        val buttonColor = if (severeErrorCount > 0) Color(0xFFD29922) else theme.accentColor

                        Button(
                            onClick = { viewModel.runProject() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(30.dp)
                                .testTag("workspace_run_btn")
                        ) {
                            if (severeErrorCount > 0) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Preview ($severeErrorCount)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (state.project?.type?.isExecutableV01 == true) "Preview" else "Run",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                // High-Density Status Bar
                EditorStatusBar(
                    cursorLine = state.cursorLine,
                    cursorColumn = state.cursorColumn,
                    diagnosticsCount = state.diagnostics.size,
                    fileExtension = state.openTabs.getOrNull(state.activeTabIndex)?.file?.extension ?: "txt",
                    theme = theme,
                    tabSize = settings.tabSize,
                    onToggleProblems = { viewModel.toggleProblems() },
                    onFormat = { viewModel.formatDocument() },
                    onOpenSettings = onNavigateToSettings
                )
            },
            containerColor = theme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Horizontal Scrollable File Tab Bar with Context Menu
                    AcodeTabBar(
                        tabs = state.openTabs,
                        activeTabIndex = state.activeTabIndex,
                        theme = theme,
                        onTabSelected = { viewModel.switchTab(it) },
                        onTabClosed = { viewModel.closeTab(it) },
                        onCloseOthers = { viewModel.closeOtherTabs(it) },
                        onCloseAll = { viewModel.closeAllTabs() }
                    )

                    // Code Editor or Empty State
                    if (state.openTabs.isNotEmpty() && state.activeTabIndex in state.openTabs.indices) {
                        val currentTab = state.openTabs[state.activeTabIndex]
                        CodeEditor(
                            editorValue = state.currentEditorValue,
                            extension = currentTab.file.extension,
                            theme = theme,
                            fontSizeSp = settings.fontSizeSp,
                            showLineNumbers = settings.showLineNumbers,
                            wordWrap = settings.wordWrap,
                            autoCloseBrackets = settings.autoCloseBrackets,
                            diagnostics = state.diagnostics,
                            canUndo = state.canUndo,
                            canRedo = state.canRedo,
                            onUndo = { viewModel.undo() },
                            onRedo = { viewModel.redo() },
                            isSearchOpen = state.isSearchOpen,
                            searchQuery = state.searchQuery,
                            replaceQuery = state.replaceQuery,
                            searchMatchCount = state.searchMatchCount,
                            onValueChange = { viewModel.onEditorTextChange(it) },
                            onInsertSymbol = { text, offset -> viewModel.insertSymbolWithOffset(text, offset) },
                            onIndentLine = { viewModel.indentCurrentLine() },
                            onApplyQuickFix = { viewModel.applyQuickFix(it) },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onReplaceQueryChange = { viewModel.setReplaceQuery(it) },
                            onFindNext = { isRegex -> viewModel.findNext(isRegex) },
                            onReplaceAll = { isRegex -> viewModel.replaceAll(isRegex) },
                            onCloseSearch = { viewModel.toggleSearch() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        EmptyEditorState(
                            theme = theme,
                            onOpenExplorer = { scope.launch { drawerState.open() } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Slide-up Terminal Shell Panel
                if (state.isTerminalOpen && state.project != null) {
                    TerminalShellPanel(
                        project = state.project!!,
                        theme = theme,
                        onClose = { viewModel.toggleTerminal() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.48f)
                    )
                }

                // Slide-up Problems Panel
                if (state.isProblemsOpen) {
                    ProblemsPanel(
                        diagnostics = state.diagnostics,
                        theme = theme,
                        onJumpTo = { diag -> viewModel.jumpToDiagnostic(diag) },
                        onQuickFix = { fix -> viewModel.applyQuickFix(fix) },
                        onClose = { viewModel.toggleProblems() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.42f)
                    )
                }

                // Command Palette Floating Modal
                if (state.commandPaletteState.isOpen) {
                    CommandPaletteDialog(
                        theme = theme,
                        commands = commands,
                        files = state.files,
                        initialQuery = state.commandPaletteState.searchQuery,
                        onDismiss = { viewModel.closeCommandPalette() },
                        onFileSelected = { file -> viewModel.openFile(file) }
                    )
                }

                // Pre-flight Error Validation Dialog
                if (state.showPreflightErrorDialog) {
                    PreflightErrorDialog(
                        errors = state.preflightErrors,
                        theme = theme,
                        onFixErrors = {
                            state.preflightErrors.firstOrNull()?.let { viewModel.jumpToDiagnostic(it) }
                            viewModel.dismissPreflightDialog()
                        },
                        onPreviewAnyway = {
                            viewModel.runProject(force = true)
                        },
                        onDismiss = { viewModel.dismissPreflightDialog() }
                    )
                }
            }
        }
    }
}

@Composable
fun PreflightErrorDialog(
    errors: List<CodeDiagnostic>,
    theme: AppEditorTheme,
    onFixErrors: () -> Unit,
    onPreviewAnyway: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.sidebarBg,
        shape = RoundedCornerShape(8.dp),
        icon = {
            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFD29922), modifier = Modifier.size(28.dp))
        },
        title = {
            Text(
                text = "Syntax Errors Detected",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = "Found ${errors.size} error(s) in your code that may cause execution issues:",
                    fontSize = 12.sp,
                    color = theme.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .height(140.dp)
                        .background(theme.background, RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(errors) { err ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "Line ${err.line}:",
                                color = Color(0xFFF85149),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = err.message,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The offline preview engine will catch runtime errors and log them to the console.",
                    fontSize = 11.sp,
                    color = theme.textSecondary.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onFixErrors,
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("fix_errors_preflight_btn")
            ) {
                Text("Fix in Editor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            Button(
                onClick = onPreviewAnyway,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.border),
                modifier = Modifier.testTag("preview_anyway_btn")
            ) {
                Text("Preview Anyway", color = theme.textSecondary, fontSize = 12.sp)
            }
        },
        modifier = modifier
    )
}

@Composable
fun AcodeTabBar(
    tabs: List<EditorTab>,
    activeTabIndex: Int,
    theme: AppEditorTheme,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    onCloseOthers: (Int) -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp),
        color = theme.tabInactiveBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isActive = index == activeTabIndex
                AcodeTabItem(
                    tab = tab,
                    isActive = isActive,
                    theme = theme,
                    onSelect = { onTabSelected(index) },
                    onClose = { onTabClosed(index) },
                    onCloseOthers = { onCloseOthers(index) },
                    onCloseAll = onCloseAll
                )
            }
        }
    }
}

@Composable
fun AcodeTabItem(
    tab: EditorTab,
    isActive: Boolean,
    theme: AppEditorTheme,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onCloseOthers: () -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val icon = when (tab.file.extension) {
        "html", "htm" -> Icons.Default.Code
        "css" -> Icons.Default.Css
        "js", "javascript" -> Icons.Default.Javascript
        "kt", "kts" -> Icons.Default.Code
        else -> Icons.Default.Description
    }

    val iconColor = when (tab.file.extension) {
        "html", "htm" -> Color(0xFFE44D26)
        "css" -> Color(0xFF569CD6)
        "js", "javascript" -> Color(0xFFF7DF1E)
        "kt", "kts" -> Color(0xFFB125EA)
        else -> theme.textSecondary
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isActive) theme.tabActiveBg else theme.tabInactiveBg)
            .clickable(onClick = onSelect)
            .border(1.dp, theme.border)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(theme.accentColor)
                    .align(Alignment.TopCenter)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))

            if (tab.isDirty) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(theme.accentColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = tab.file.name,
                color = if (isActive) Color.White else theme.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Tab Close Icon
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close Tab",
                    tint = if (isActive) Color.White else theme.textSecondary,
                    modifier = Modifier.size(11.dp)
                )
            }

            // Tab Options Dropdown
            Box {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Tab options",
                    tint = Color.Transparent,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { menuExpanded = true }
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(theme.sidebarBg)
                ) {
                    DropdownMenuItem(
                        text = { Text("Close", color = Color.White, fontSize = 12.sp) },
                        onClick = {
                            menuExpanded = false
                            onClose()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Close Others", color = Color.White, fontSize = 12.sp) },
                        onClick = {
                            menuExpanded = false
                            onCloseOthers()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Close All", color = Color(0xFFF14C4C), fontSize = 12.sp) },
                        onClick = {
                            menuExpanded = false
                            onCloseAll()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EditorStatusBar(
    cursorLine: Int,
    cursorColumn: Int,
    diagnosticsCount: Int,
    fileExtension: String,
    theme: AppEditorTheme,
    tabSize: Int,
    onToggleProblems: () -> Unit,
    onFormat: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp),
        color = theme.statusBarBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Problems Badge
            Row(
                modifier = Modifier
                    .clickable(onClick = onToggleProblems)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$diagnosticsCount",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (diagnosticsCount == 0) "No Problems" else "Problems",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }

            // Right: Ln, Col, Spaces, UTF-8, Lang
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ln $cursorLine, Col $cursorColumn",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Spaces: $tabSize",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable(onClick = onFormat)
                )
                Text(
                    text = "UTF-8",
                    color = Color.White,
                    fontSize = 11.sp
                )
                Text(
                    text = when (fileExtension.lowercase()) {
                        "html", "htm" -> "HTML"
                        "css" -> "CSS"
                        "js", "javascript" -> "JavaScript"
                        "kt", "kts" -> "Kotlin"
                        "py" -> "Python"
                        else -> fileExtension.uppercase()
                    },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onOpenSettings)
                )
            }
        }
    }
}

@Composable
fun ProblemsPanel(
    diagnostics: List<CodeDiagnostic>,
    theme: AppEditorTheme,
    onJumpTo: (CodeDiagnostic) -> Unit,
    onQuickFix: (com.example.editor.QuickFix) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = theme.sidebarBg,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            // Problems Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFF85149),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PROBLEMS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${diagnostics.size})",
                        fontSize = 11.sp,
                        color = theme.textSecondary
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(26.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close Problems",
                        tint = theme.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (diagnostics.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No problems have been detected in the workspace.",
                        fontSize = 12.sp,
                        color = theme.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(diagnostics) { item ->
                        val isError = item.severity == DiagnosticSeverity.ERROR
                        val iconColor = if (isError) Color(0xFFF85149) else Color(0xFFD29922)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onJumpTo(item) },
                            color = theme.background,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.message,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Line ${item.line}, Column ${item.column}",
                                        color = theme.textSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                item.quickFix?.let { fix ->
                                    Button(
                                        onClick = { onQuickFix(fix) },
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                                        shape = RoundedCornerShape(3.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text(
                                            text = "Fix",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyEditorState(
    theme: AppEditorTheme,
    onOpenExplorer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = theme.accentColor,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No Files Open",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Open the Explorer to select or create a file to start editing.",
                fontSize = 12.sp,
                color = theme.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onOpenExplorer,
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Open Explorer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

