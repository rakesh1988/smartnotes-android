package com.smartnotes.app

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

private const val TAG = "NoteEditorScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    navController: NavController,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAiSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.clearAiState()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete note?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteNote {
                        navController.popBackStack()
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isNewNote) "New Note" else "Edit Note")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!uiState.isNewNote) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete note",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = {
                        Log.d(TAG, "AI button tapped, body length=${uiState.body.length}")
                        showAiSheet = true
                    }) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI features"
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.saveNote {
                                navController.popBackStack()
                            }
                        },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Save note"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (!uiState.isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Note title") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.body,
                    onValueChange = viewModel::onBodyChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp),
                    placeholder = { Text("Start writing...") },
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                if (uiState.category != "UNCATEGORIZED") {
                    Spacer(modifier = Modifier.height(12.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("Category: ${uiState.category}") },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // ModalBottomSheet must be inside the Scaffold content lambda so it
        // receives correct window insets and renders above the system bars.
        if (showAiSheet) {
            AiBottomSheet(
                isProcessing = uiState.isAiProcessing,
                summary = uiState.aiSummary,
                errorMessage = uiState.aiError,
                isBodyEmpty = uiState.body.isBlank(),
                onDismiss = { showAiSheet = false },
                onSummarize = {
                    Log.d(TAG, "Summarize tapped")
                    viewModel.summarize()
                },
                onSuggestTitle = {
                    Log.d(TAG, "Suggest title tapped")
                    viewModel.suggestTitle()
                },
                onCategorize = {
                    Log.d(TAG, "Categorize tapped")
                    viewModel.categorize()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiBottomSheet(
    isProcessing: Boolean,
    summary: String?,
    errorMessage: String?,
    isBodyEmpty: Boolean,
    onDismiss: () -> Unit,
    onSummarize: () -> Unit,
    onSuggestTitle: () -> Unit,
    onCategorize: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "AI Features",
                style = MaterialTheme.typography.titleLarge
            )

            if (isBodyEmpty) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Write some content in your note first to use AI features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                return@ModalBottomSheet
            }

            Spacer(modifier = Modifier.height(16.dp))

            AiActionButton(
                title = "Summarize",
                description = "Generate a 3-bullet summary of your note",
                icon = Icons.Default.Summarize,
                onClick = onSummarize,
                enabled = !isProcessing
            )

            if (summary != null) {
                AiResultCard(text = summary)
            }

            AiActionButton(
                title = "Suggest Title",
                description = "AI-generated title suggestion",
                icon = Icons.Default.Edit,
                onClick = onSuggestTitle,
                enabled = !isProcessing
            )

            AiActionButton(
                title = "Categorize",
                description = "Auto-detect note category (IDEA, TODO, etc.)",
                icon = Icons.AutoMirrored.Filled.Label,
                onClick = onCategorize,
                enabled = !isProcessing
            )

            if (isProcessing) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Processing with Gemini Nano...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AiActionButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiResultCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
