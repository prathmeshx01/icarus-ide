package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.FileRepository
import com.example.data.ProjectRepository
import com.example.data.SettingsManager
import com.example.model.Project
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.workspace.WorkspaceScreen
import com.example.ui.workspace.WorkspaceViewModel

enum class IcarusScreen {
    HOME,
    WORKSPACE,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val projectRepository by lazy { ProjectRepository(applicationContext) }
    private val fileRepository by lazy { FileRepository() }
    private val settingsManager by lazy { SettingsManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by settingsManager.settings.collectAsState()
            val theme = settings.theme

            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = theme.background
                ) {
                    IcarusApp(
                        projectRepository = projectRepository,
                        fileRepository = fileRepository,
                        settingsManager = settingsManager
                    )
                }
            }
        }
    }
}

@Composable
fun IcarusApp(
    projectRepository: ProjectRepository,
    fileRepository: FileRepository,
    settingsManager: SettingsManager
) {
    var currentScreen by remember { mutableStateOf(IcarusScreen.HOME) }
    var previousScreen by remember { mutableStateOf(IcarusScreen.HOME) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }

    val homeViewModel = remember { HomeViewModel(projectRepository) }
    val workspaceViewModel = remember { WorkspaceViewModel(projectRepository, fileRepository) }

    when (currentScreen) {
        IcarusScreen.HOME -> {
            HomeScreen(
                viewModel = homeViewModel,
                settingsManager = settingsManager,
                onProjectClick = { project ->
                    selectedProject = project
                    workspaceViewModel.loadProject(project)
                    currentScreen = IcarusScreen.WORKSPACE
                },
                onNavigateToSettings = {
                    previousScreen = IcarusScreen.HOME
                    currentScreen = IcarusScreen.SETTINGS
                }
            )
        }

        IcarusScreen.WORKSPACE -> {
            BackHandler {
                homeViewModel.loadProjects()
                currentScreen = IcarusScreen.HOME
            }

            WorkspaceScreen(
                viewModel = workspaceViewModel,
                settingsManager = settingsManager,
                onNavigateBack = {
                    homeViewModel.loadProjects()
                    currentScreen = IcarusScreen.HOME
                },
                onNavigateToSettings = {
                    previousScreen = IcarusScreen.WORKSPACE
                    currentScreen = IcarusScreen.SETTINGS
                }
            )
        }

        IcarusScreen.SETTINGS -> {
            BackHandler {
                currentScreen = previousScreen
            }

            SettingsScreen(
                settingsManager = settingsManager,
                onNavigateBack = {
                    currentScreen = previousScreen
                }
            )
        }
    }
}
