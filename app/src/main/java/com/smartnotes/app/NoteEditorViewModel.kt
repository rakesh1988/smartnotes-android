package com.smartnotes.app

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnotes.core.ai.AiService
import com.smartnotes.core.database.Note
import com.smartnotes.core.database.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "NoteEditorViewModel"

data class NoteEditorUiState(
    val title: String = "",
    val body: String = "",
    val category: String = "UNCATEGORIZED",
    val isNewNote: Boolean = true,
    val noteId: Long? = null,
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false,
    // AI state
    val aiSummary: String? = null,
    val aiSuggestedTitle: String? = null,
    val aiCategory: String? = null,
    val isAiProcessing: Boolean = false,
    // null = not yet checked, true/false = result of checkFeatureStatus
    val isAiAvailable: Boolean? = null,
    val aiError: String? = null
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val aiService: AiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: Long? = savedStateHandle.get<Long>("noteId")

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        if (noteId != null && noteId > 0) {
            loadNote(noteId)
        } else {
            _uiState.update { it.copy(isLoaded = true, isNewNote = true) }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            repository.getNoteById(id).collect { note ->
                if (note != null) {
                    _uiState.update {
                        it.copy(
                            title = note.title,
                            body = note.body,
                            category = note.category,
                            noteId = note.id,
                            isNewNote = false,
                            isLoaded = true
                        )
                    }
                }
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onBodyChanged(body: String) {
        _uiState.update { it.copy(body = body) }
    }

    fun saveNote(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank() && state.body.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val note = Note(
                id = state.noteId ?: 0,
                title = state.title.ifBlank { "Untitled" },
                body = state.body,
                category = state.category,
                updatedAt = System.currentTimeMillis()
            )
            repository.upsertNote(note)
            _uiState.update { it.copy(isSaving = false) }
            onSaved()
        }
    }

    fun summarize() {
        val body = _uiState.value.body
        Log.d(TAG, "summarize() called, body blank=${body.isBlank()}")
        if (body.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true, aiSummary = null, aiError = null) }
            Log.d(TAG, "summarize() collecting flow")
            aiService.summarize(body).collect { result ->
                Log.d(TAG, "summarize() result: text='${result.text}' available=${result.isAvailable} error=${result.errorMessage}")
                when {
                    !result.isAvailable -> _uiState.update {
                        it.copy(isAiProcessing = false, isAiAvailable = false, aiError = result.errorMessage)
                    }
                    result.errorMessage != null -> _uiState.update {
                        it.copy(isAiProcessing = false, aiError = result.errorMessage)
                    }
                    else -> _uiState.update {
                        it.copy(
                            // streaming: each emission is cumulative text, update in real-time
                            aiSummary = result.text.takeIf { t -> t.isNotBlank() },
                            isAiProcessing = false,
                            isAiAvailable = true
                        )
                    }
                }
            }
        }
    }

    fun suggestTitle() {
        val body = _uiState.value.body
        Log.d(TAG, "suggestTitle() called, body blank=${body.isBlank()}")
        if (body.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true, aiSuggestedTitle = null, aiError = null) }
            aiService.generateTitle(body).collect { result ->
                when {
                    !result.isAvailable -> _uiState.update {
                        it.copy(isAiProcessing = false, isAiAvailable = false, aiError = result.errorMessage)
                    }
                    result.errorMessage != null -> _uiState.update {
                        it.copy(isAiProcessing = false, aiError = result.errorMessage)
                    }
                    else -> {
                        val suggested = result.text
                        _uiState.update {
                            it.copy(
                                aiSuggestedTitle = suggested,
                                // auto-fill title if still empty
                                title = if (it.title.isBlank()) suggested else it.title,
                                isAiProcessing = false,
                                isAiAvailable = true
                            )
                        }
                    }
                }
            }
        }
    }

    fun categorize() {
        val body = _uiState.value.body
        Log.d(TAG, "categorize() called, body blank=${body.isBlank()}")
        if (body.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true, aiCategory = null, aiError = null) }
            aiService.categorize(body).collect { result ->
                when {
                    !result.isAvailable -> _uiState.update {
                        it.copy(isAiProcessing = false, isAiAvailable = false, aiError = result.errorMessage)
                    }
                    result.errorMessage != null -> _uiState.update {
                        it.copy(isAiProcessing = false, aiError = result.errorMessage)
                    }
                    else -> _uiState.update {
                        it.copy(
                            aiCategory = result.text,
                            category = result.text.ifBlank { "OTHER" },
                            isAiProcessing = false,
                            isAiAvailable = true
                        )
                    }
                }
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        val id = _uiState.value.noteId ?: return
        viewModelScope.launch {
            repository.deleteNoteById(id)
            onDeleted()
        }
    }

    fun clearAiState() {
        _uiState.update {
            it.copy(aiSummary = null, aiSuggestedTitle = null, aiCategory = null, aiError = null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        aiService.close()
    }
}
