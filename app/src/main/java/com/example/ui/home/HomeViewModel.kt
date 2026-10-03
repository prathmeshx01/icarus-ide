package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectRepository
import com.example.model.Project
import com.example.model.ProjectType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val showNewProjectDialog: Boolean = false,
    val projectToDelete: Project? = null
)

class HomeViewModel(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            projectRepository.ensureInitialProjectIfEmpty()
            projectRepository.refreshProjects()
            projectRepository.projects.collect { list ->
                _uiState.update { it.copy(projects = list, isLoading = false) }
            }
        }
    }

    fun openNewProjectDialog() {
        _uiState.update { it.copy(showNewProjectDialog = true) }
    }

    fun closeNewProjectDialog() {
        _uiState.update { it.copy(showNewProjectDialog = false) }
    }

    fun createProject(
        name: String,
        type: ProjectType,
        includeHtml: Boolean = true,
        includeCss: Boolean = true,
        includeJs: Boolean = true,
        includePy: Boolean = false,
        includeReadme: Boolean = false
    ) {
        viewModelScope.launch {
            val project = projectRepository.createProject(
                name = name,
                type = type,
                includeHtml = includeHtml,
                includeCss = includeCss,
                includeJs = includeJs,
                includePy = includePy,
                includeReadme = includeReadme
            )
            _uiState.update { it.copy(showNewProjectDialog = false) }
            _events.emit(HomeEvent.NavigateToWorkspace(project))
        }
    }

    fun confirmDelete(project: Project) {
        _uiState.update { it.copy(projectToDelete = project) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(projectToDelete = null) }
    }

    fun deleteProjectConfirmed() {
        val target = _uiState.value.projectToDelete ?: return
        viewModelScope.launch {
            projectRepository.deleteProject(target.id)
            _uiState.update { it.copy(projectToDelete = null) }
        }
    }
}

sealed class HomeEvent {
    data class NavigateToWorkspace(val project: Project) : HomeEvent()
}
