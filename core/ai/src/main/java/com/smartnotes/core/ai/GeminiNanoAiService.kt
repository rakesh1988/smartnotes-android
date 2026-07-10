package com.smartnotes.core.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiNanoAiService @Inject constructor() : AiService {

    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash-lite",
        apiKey = BUILD_CONFIG_API_KEY
    )

    override fun summarize(text: String): Flow<AiResult> = flow {
        val prompt = "Summarize the following note in 1-2 sentences:\n\n$text"
        val response = model.generateContent(prompt)
        val resultText = response.text?.trim() ?: ""
        emit(AiResult(text = resultText, confidence = null, latencyMs = 0))
    }

    override fun generateTitle(text: String): Flow<AiResult> = flow {
        val prompt = "Generate a concise title (max 8 words) for this note:\n\n$text"
        val response = model.generateContent(prompt)
        val resultText = response.text?.trim() ?: "Untitled"
        emit(AiResult(text = resultText, confidence = null, latencyMs = 0))
    }

    override fun categorize(text: String): Flow<AiResult> = flow {
        val prompt = """
            Categorize the following note into exactly one category:
            IDEA, TODO, REFERENCE, JOURNAL, or OTHER.
            Return only the category name, no additional text.
            
            $text
        """.trimIndent()
        val response = model.generateContent(prompt)
        val resultText = response.text?.trim() ?: "OTHER"
        emit(AiResult(text = resultText, confidence = null, latencyMs = 0))
    }

    companion object {
        private const val BUILD_CONFIG_API_KEY = "YOUR_GEMINI_API_KEY"
    }
}
