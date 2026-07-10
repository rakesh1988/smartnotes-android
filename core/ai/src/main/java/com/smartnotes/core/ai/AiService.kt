package com.smartnotes.core.ai

import kotlinx.coroutines.flow.Flow

data class AiResult(
    val text: String,
    val confidence: Float? = null,
    val latencyMs: Long = 0
)

interface AiService {
    fun summarize(text: String): Flow<AiResult>
    fun generateTitle(text: String): Flow<AiResult>
    fun categorize(text: String): Flow<AiResult>
}
