package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsManager
import com.example.model.Project
import com.example.model.ProjectType
import com.example.ui.theme.AppEditorTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    settingsManager: SettingsManager,
    onProjectClick: (Project) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val settings by settingsManager.settings.collectAsState()
    val theme = settings.theme

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<ProjectType?>(null) }

    val filteredProjects = remember(state.projects, searchQuery, selectedCategoryFilter) {
        state.projects.filter { p ->
            val matchesQuery = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == null || p.type == selectedCategoryFilter
            matchesQuery && matchesCategory
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToWorkspace -> onProjectClick(event.project)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.titleBarBg,
                    titleContentColor = Color.White
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(theme.accentColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ICARUS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "IDE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = theme.accentColor,
                            modifier = Modifier
                                .background(theme.accentColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                .border(0.5.dp, theme.accentColor.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openNewProjectDialog() },
                containerColor = theme.accentColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("fab_new_project")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Project", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        },
        containerColor = theme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Card
            item {
                VibrantHeroCard(
                    theme = theme,
                    onCreateProject = { viewModel.openNewProjectDialog() },
                    onQuickScratchpad = { viewModel.createProject("HTML5 Starter", ProjectType.WEB) }
                )
            }

            // Quick Starter Templates Carousel
            item {
                StarterPillRow(
                    theme = theme,
                    onSelectType = { type, defaultName ->
                        viewModel.createProject(defaultName, type)
                    }
                )
            }

            // Search & Category Filter Row
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter workspaces...", fontSize = 12.sp, color = theme.textSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = theme.textSecondary, modifier = Modifier.size(16.dp))
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accentColor,
                            unfocusedBorderColor = theme.border,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Category Pills
                    val filterScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(filterScrollState),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryFilterChip(
                            label = "All (${state.projects.size})",
                            isSelected = selectedCategoryFilter == null,
                            theme = theme,
                            onClick = { selectedCategoryFilter = null }
                        )

                        ProjectType.values().forEach { type ->
                            val count = state.projects.count { it.type == type }
                            if (count > 0 || state.projects.isEmpty()) {
                                CategoryFilterChip(
                                    label = "${type.badge} ($count)",
                                    isSelected = selectedCategoryFilter == type,
                                    theme = theme,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == type) null else type }
                                )
                            }
                        }
                    }
                }
            }

            // Workspaces Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Workspaces",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${filteredProjects.size})",
                            fontSize = 12.sp,
                            color = theme.textSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Projects List or Empty State
            if (filteredProjects.isEmpty()) {
                item {
                    EmptyProjectsCard(theme = theme, onCreateProject = { viewModel.openNewProjectDialog() })
                }
            } else {
                items(filteredProjects, key = { it.id }) { project ->
                    WorkspaceCard(
                        project = project,
                        theme = theme,
                        onClick = { onProjectClick(project) },
                        onDelete = { viewModel.confirmDelete(project) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Modal: New Project with Boilerplate Templates and Language Options
    if (state.showNewProjectDialog) {
        NewProjectDialog(
            theme = theme,
            onDismiss = { viewModel.closeNewProjectDialog() },
            onCreate = { name, type, html, css, js, py, readme ->
                viewModel.createProject(
                    name = name,
                    type = type,
                    includeHtml = html,
                    includeCss = css,
                    includeJs = js,
                    includePy = py,
                    includeReadme = readme
                )
            }
        )
    }

    // Modal: Delete Confirmation
    state.projectToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDelete() },
            containerColor = theme.sidebarBg,
            shape = RoundedCornerShape(8.dp),
            title = { Text("Delete Workspace?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text(
                    "Are you sure you want to delete '${target.name}' and all associated files?",
                    color = theme.textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteProjectConfirmed() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF85149)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Delete", color = Color.White, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDelete() }) {
                    Text("Cancel", color = theme.textSecondary, fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    theme: AppEditorTheme,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (isSelected) theme.accentColor else theme.sidebarBg,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) theme.accentColor else theme.border)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else theme.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun VibrantHeroCard(
    theme: AppEditorTheme,
    onCreateProject: () -> Unit,
    onQuickScratchpad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = remember(theme) {
        Brush.horizontalGradient(
            colors = listOf(
                theme.sidebarBg,
                theme.sidebarBg.copy(alpha = 0.95f)
            )
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Icarus IDE Workspace",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF3FB950), CircleShape)
                        )
                        Text(
                            text = "OFFLINE ENGINE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3FB950)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "High-speed syntax highlighting, boilerplate templates, and live preview runtime.",
                    fontSize = 12.sp,
                    color = theme.textSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCreateProject,
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Project", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = onQuickScratchpad,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.border),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HTML5 Basic", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StarterPillRow(
    theme: AppEditorTheme,
    onSelectType: (ProjectType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val starters = listOf(
        Triple("HTML5 Starter", ProjectType.WEB, Icons.Default.Code),
        Triple("React 18", ProjectType.REACT, Icons.Default.Language),
        Triple("Vue 3", ProjectType.VUE, Icons.Default.Language),
        Triple("Tailwind", ProjectType.TAILWIND, Icons.Default.Description),
        Triple("Canvas 2D", ProjectType.CANVAS, Icons.Default.SportsEsports),
        Triple("Python Script", ProjectType.PYTHON, Icons.Default.Terminal)
    )

    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Boilerplate Templates",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textSecondary,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            starters.forEach { (label, type, icon) ->
                Surface(
                    modifier = Modifier
                        .clickable { onSelectType(type, label) },
                    color = theme.sidebarBg,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspaceCard(
    project: Project,
    theme: AppEditorTheme,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeColor, icon) = when (project.type) {
        ProjectType.CANVAS -> Color(0xFF39C5BB) to Icons.Default.SportsEsports
        ProjectType.WEB -> theme.accentColor to Icons.Default.Code
        ProjectType.REACT -> Color(0xFF61DAFB) to Icons.Default.Language
        ProjectType.VUE -> Color(0xFF42B883) to Icons.Default.Language
        ProjectType.TAILWIND -> Color(0xFF38BDF8) to Icons.Default.Description
        ProjectType.PORTFOLIO -> Color(0xFF60A5FA) to Icons.Default.Language
        ProjectType.KOTLIN -> Color(0xFFBD93F9) to Icons.Default.Terminal
        ProjectType.PYTHON -> Color(0xFFFFD866) to Icons.Default.Terminal
        ProjectType.BLANK -> Color(0xFF9CA3AF) to Icons.Default.Description
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = theme.sidebarBg),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(theme.background, RoundedCornerShape(6.dp))
                    .border(1.dp, theme.border, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = project.type.badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val formattedDate = remember(project.updatedAt) {
                    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                    sdf.format(Date(project.updatedAt))
                }

                Text(
                    text = "${project.fileCount} files • $formattedDate",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = theme.textSecondary
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete Project",
                    tint = theme.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyProjectsCard(
    theme: AppEditorTheme,
    onCreateProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = theme.sidebarBg,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                tint = theme.accentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No Workspaces Found",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick a starter template above or create a new project.",
                fontSize = 12.sp,
                color = theme.textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCreateProject,
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Create Project", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun NewProjectDialog(
    theme: AppEditorTheme,
    onDismiss: () -> Unit,
    onCreate: (name: String, type: ProjectType, html: Boolean, css: Boolean, js: Boolean, py: Boolean, readme: Boolean) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ProjectType.WEB) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Language file selection checkboxes
    var incHtml by remember { mutableStateOf(true) }
    var incCss by remember { mutableStateOf(true) }
    var incJs by remember { mutableStateOf(true) }
    var incPy by remember { mutableStateOf(false) }
    var incReadme by remember { mutableStateOf(false) }

    val options = listOf(
        ProjectType.WEB to (theme.accentColor to Icons.Default.Code),
        ProjectType.REACT to (Color(0xFF61DAFB) to Icons.Default.Language),
        ProjectType.VUE to (Color(0xFF42B883) to Icons.Default.Language),
        ProjectType.TAILWIND to (Color(0xFF38BDF8) to Icons.Default.Description),
        ProjectType.CANVAS to (Color(0xFF39C5BB) to Icons.Default.SportsEsports),
        ProjectType.PORTFOLIO to (Color(0xFF60A5FA) to Icons.Default.Language),
        ProjectType.PYTHON to (Color(0xFFFFD866) to Icons.Default.Terminal),
        ProjectType.KOTLIN to (Color(0xFFBD93F9) to Icons.Default.Terminal),
        ProjectType.BLANK to (Color(0xFF9CA3AF) to Icons.Default.Description)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.sidebarBg,
        shape = RoundedCornerShape(8.dp),
        title = {
            Text("Create New Workspace", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text(
                    "Workspace Name",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it
                        errorMsg = null
                    },
                    placeholder = { Text("my-app", fontSize = 12.sp, color = theme.textSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { { Text(it, color = Color(0xFFF85149)) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("new_project_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    "Select Boilerplate Template",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(options) { (type, meta) ->
                        val (badgeColor, icon) = meta
                        val isSelected = selectedType == type

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedType = type
                                    when (type) {
                                        ProjectType.PYTHON -> {
                                            incHtml = false; incCss = false; incJs = false; incPy = true
                                        }
                                        ProjectType.KOTLIN -> {
                                            incHtml = false; incCss = false; incJs = false; incPy = false
                                        }
                                        else -> {
                                            incHtml = true; incCss = true; incJs = true; incPy = false
                                        }
                                    }
                                },
                            color = if (isSelected) theme.background else theme.sidebarBg,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) theme.accentColor else theme.border
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = type.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = type.badge,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier
                                                .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = type.description,
                                        fontSize = 10.sp,
                                        color = theme.textSecondary,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Language Files Selection
                Text(
                    "Include Language Files",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                val fileOptions = listOf(
                    Triple("HTML", incHtml) { incHtml = !incHtml },
                    Triple("CSS", incCss) { incCss = !incCss },
                    Triple("JS", incJs) { incJs = !incJs },
                    Triple("Python", incPy) { incPy = !incPy },
                    Triple("README", incReadme) { incReadme = !incReadme }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    fileOptions.forEach { (label, isChecked, onToggle) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onToggle() },
                            color = if (isChecked) theme.accentColor.copy(alpha = 0.2f) else Color(0x14FFFFFF),
                            shape = RoundedCornerShape(3.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isChecked) theme.accentColor else theme.border
                            )
                        ) {
                            Text(
                                text = (if (isChecked) "✓ " else "") + label,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                color = if (isChecked) theme.accentColor else theme.textSecondary,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = projectName.trim().ifBlank { selectedType.displayName }
                    onCreate(clean, selectedType, incHtml, incCss, incJs, incPy, incReadme)
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("confirm_create_project_btn")
            ) {
                Text("Create Workspace", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = theme.textSecondary, fontSize = 12.sp)
            }
        }
    )
}
