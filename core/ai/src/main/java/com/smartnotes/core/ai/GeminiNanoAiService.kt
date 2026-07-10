package com.smartnotes.core.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiNanoAiService @Inject constructor() : AiService {

    override fun summarize(text: String): Flow<AiResult> = flow {
        val startTime = System.currentTimeMillis()
        emit(AiResult(text = "", confidence = null, latencyMs = 0))
        // TODO: Implement Gemini Nano inference
        delay(100)
        emit(
            AiResult(
                text = "- Summary point 1\n- Summary point 2\n- Summary point 3",
                confidence = 0.85f,
                latencyMs = System.currentTimeMillis() - startTime
            )
        )
    }

    override fun generateTitle(text: String): Flow<AiResult> = flow {
        val startTime = System.currentTimeMillis()
        emit(AiResult(text = "", confidence = null, latencyMs = 0))
        delay(50)
        emit(
            AiResult(
                text = "Smart Note",
                confidence = 0.75f,
                latencyMs = System.currentTimeMillis() - startTime
            )
        )
    }

    override fun categorize(text: String): Flow<AiResult> = flow {
        val startTime = System.currentTimeMillis()
        emit(AiResult(text = "", confidence = null, latencyMs = 0))
        delay(50)
        emit(
            AiResult(
                text = "IDEA",
                confidence = 0.9f,
                latencyMs = System.currentTimeMillis() - startTime
            )
        )
    }
}
