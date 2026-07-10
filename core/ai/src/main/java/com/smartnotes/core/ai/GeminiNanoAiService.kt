package com.smartnotes.core.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiNanoAiService @Inject constructor() : AiService {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val model: GenerativeModel? =
        apiKey.takeIf { it.isNotBlank() && it != PLACEHOLDER }?.let {
            GenerativeModel(modelName = "gemini-2.0-flash-lite", apiKey = it)
        }

    override fun summarize(text: String): Flow<AiResult> =
        generate("Summarize the following note in 1-2 sentences:\n\n$text")

    override fun generateTitle(text: String): Flow<AiResult> =
        generate(
            "Generate a concise title (max 8 words) for this note:\n\n$text",
            fallback = "Untitled"
        )

    override fun categorize(text: String): Flow<AiResult> = generate(
        """
            Categorize the following note into exactly one category:
            IDEA, TODO, REFERENCE, JOURNAL, or OTHER.
            Return only the category name, no additional text.

            $text
        """.trimIndent(),
        fallback = "OTHER"
    )

    private fun generate(prompt: String, fallback: String = ""): Flow<AiResult> = flow {
        val activeModel = model
        if (activeModel == null) {
            emit(AiResult(text = MISSING_KEY_MESSAGE))
            return@flow
        }
        try {
            val response = activeModel.generateContent(prompt)
            emit(AiResult(text = response.text?.trim() ?: fallback))
        } catch (e: Exception) {
            emit(AiResult(text = "AI request failed: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    companion object {
        private const val PLACEHOLDER = "YOUR_GEMINI_API_KEY"
        private const val MISSING_KEY_MESSAGE =
            "AI unavailable: set GEMINI_API_KEY in local.properties"
    }
}
