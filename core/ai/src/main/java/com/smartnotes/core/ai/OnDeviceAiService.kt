package com.smartnotes.core.ai

import com.google.ai.edge.aicore.GenerativeAIException
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device AI using Google's Gemini Nano through the Android AICore system
 * service (Google AI Edge SDK, `com.google.ai.edge.aicore`).
 *
 * Fully offline and keyless: the model is delivered and managed by Android
 * AICore (no API key, no network, no bundled model file). Requires a device
 * that ships AICore (e.g. Pixel 9+ or recent Samsung Galaxy) with the model
 * downloaded. Degrades gracefully with an explanatory message otherwise.
 */
@Singleton
class OnDeviceAiService @Inject constructor() : AiService {

    private val model = GenerativeModel(
        generationConfig {
            temperature = 0.8f
            topK = 40
            maxOutputTokens = 256
        }
    )

    override fun summarize(text: String): Flow<AiResult> =
        generate("Summarize the following note in 1-2 sentences:\n\n$text")

    override fun generateTitle(text: String): Flow<AiResult> =
        generate(
            "Generate a concise title (max 8 words) for this note:\n\n$text",
            fallback = "Untitled"
        )

    override fun categorize(text: String): Flow<AiResult> = generate(
        "Categorize the following note into exactly one category: " +
            "IDEA, TODO, REFERENCE, JOURNAL, or OTHER. " +
            "Return only the category name, no additional text.\n\n$text",
        fallback = "OTHER"
    )

    private fun generate(prompt: String, fallback: String = ""): Flow<AiResult> = flow {
        val start = System.currentTimeMillis()
        try {
            val response = withContext(Dispatchers.IO) {
                model.generateContent(prompt)
            }
            val text = response.text?.trim()?.takeIf { it.isNotBlank() } ?: fallback
            emit(AiResult(text = text, latencyMs = System.currentTimeMillis() - start))
        } catch (e: GenerativeAIException) {
            emit(AiResult(text = unavailableMessage(e)))
        } catch (e: Exception) {
            emit(AiResult(text = "On-device AI failed: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    private fun unavailableMessage(e: Exception): String =
        "On-device AI (Gemini Nano via Android AICore) is unavailable on this device: " +
            "${e.message ?: e.javaClass.simpleName}. Gemini Nano requires a device with the " +
            "Android AICore system service (e.g. Pixel 9+ or recent Samsung Galaxy) and the " +
            "model downloaded via Play Services."
}
