package com.smartnotes.core.ai

import kotlinx.coroutines.flow.Flow

/**
 * Result emitted by an AI operation.
 *
 * [text] is built up incrementally for streaming operations — each emission
 * replaces the previous value (the ML Kit streaming callback delivers
 * cumulative text, not deltas).
 *
 * [isAvailable] is false when the feature is UNAVAILABLE on this device.
 * [errorMessage] is non-null when the operation failed.
 */
data class AiResult(
    val text: String = "",
    val isAvailable: Boolean = true,
    val errorMessage: String? = null,
    val latencyMs: Long = 0
)

interface AiService {
    /**
     * Summarize [text] into bullet points using ML Kit Summarization API.
     * Emits partial results during streaming; final emission has the complete summary.
     * Requires Android API 26+ and a supported device with Gemini Nano.
     */
    fun summarize(text: String): Flow<AiResult>

    /**
     * Generate a short title for [text] using ML Kit Prompt API.
     */
    fun generateTitle(text: String): Flow<AiResult>

    /**
     * Classify [text] into one of: IDEA, TODO, REFERENCE, JOURNAL, OTHER.
     * Uses ML Kit Prompt API.
     */
    fun categorize(text: String): Flow<AiResult>

    /**
     * Release underlying ML Kit clients. Call from ViewModel.onCleared().
     */
    fun close()
}
