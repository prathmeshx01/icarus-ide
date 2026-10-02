package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
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
                            text = "CORE",
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
            // Alive, Intriguing Hero Banner with Ambient Gradient
            item {
                VibrantHeroCard(
                    theme = theme,
                    onCreateProject = { viewModel.openNewProjectDialog() },
                    onQuickScratchpad = { viewModel.createProject("Quick Scratchpad", ProjectType.BLANK) }
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
                            text = "(${state.projects.size})",
                            fontSize = 12.sp,
                            color = theme.textSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Projects List or Empty State
            if (state.projects.isEmpty()) {
                item {
                    EmptyProjectsCard(theme = theme, onCreateProject = { viewModel.openNewProjectDialog() })
                }
            } else {
                items(state.projects, key = { it.id }) { project ->
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

    // Modal: New Project with 6 Diverse Options
    if (state.showNewProjectDialog) {
        NewProjectDialog(
            theme = theme,
            onDismiss = { viewModel.closeNewProjectDialog() },
            onCreate = { name, type -> viewModel.createProject(name, type) }
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
                        text = "Code Workspace",
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
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3FB950)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "High-precision syntax highlighting, live preview, and code folding.",
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
                        Text("Scratchpad", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 11.sp)
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
        Triple("Canvas Simulation", ProjectType.CANVAS, Icons.Default.SportsEsports),
        Triple("Web App", ProjectType.WEB, Icons.Default.Code),
        Triple("Portfolio", ProjectType.PORTFOLIO, Icons.Default.Language),
        Triple("Kotlin Algorithm", ProjectType.KOTLIN, Icons.Default.Terminal)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Quick Starters",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textSecondary,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            starters.forEach { (label, type, icon) ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectType(type, label) },
                    color = theme.sidebarBg,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.border)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(icon, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1
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
                text = "Pick a template above or create a new project.",
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
    onCreate: (String, ProjectType) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ProjectType.CANVAS) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val options = listOf(
        ProjectType.CANVAS to (Color(0xFF39C5BB) to Icons.Default.SportsEsports),
        ProjectType.WEB to (theme.accentColor to Icons.Default.Code),
        ProjectType.PORTFOLIO to (Color(0xFF60A5FA) to Icons.Default.Language),
        ProjectType.KOTLIN to (Color(0xFFBD93F9) to Icons.Default.Terminal),
        ProjectType.PYTHON to (Color(0xFFFFD866) to Icons.Default.Terminal),
        ProjectType.BLANK to (Color(0xFF9CA3AF) to Icons.Default.Description)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.sidebarBg,
        shape = RoundedCornerShape(8.dp),
        title = {
            Text("Create Workspace", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                    placeholder = { Text("my-workspace", fontSize = 12.sp, color = theme.textSecondary.copy(alpha = 0.5f)) },
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Select Starter Template",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(options) { (type, meta) ->
                        val (badgeColor, icon) = meta
                        val isSelected = selectedType == type

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = type },
                            color = if (isSelected) theme.background else theme.sidebarBg,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) theme.accentColor else theme.border
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
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
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier
                                                .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = type.description,
                                        fontSize = 10.sp,
                                        color = theme.textSecondary,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = projectName.trim().ifBlank { selectedType.displayName }
                    onCreate(clean, selectedType)
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
