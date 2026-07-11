package com.smartnotes.core.ai

import android.content.Context
import android.util.Log
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.StreamingCallback
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import com.google.mlkit.genai.summarization.SummarizerOptions.InputType
import com.google.mlkit.genai.summarization.SummarizerOptions.Language
import com.google.mlkit.genai.summarization.SummarizerOptions.OutputType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "OnDeviceAiService"

/**
 * On-device AI implementation using ML Kit's GenAI APIs — the official
 * high-level interface to Gemini Nano via Android's AICore system service.
 *
 * Architecture:
 *   SmartNotes → ML Kit GenAI APIs → AICore (system service) → Gemini Nano
 *
 * - Fully offline after feature download; no API key required
 * - Model weights are shared system-wide by AICore (not bundled in the APK)
 * - Requires API 26+ on a supported device (Pixel 9/10, Samsung S25/S26, etc.)
 * - AICore enforces per-app inference quota and blocks background use
 *
 * Summarization uses the purpose-built [Summarizer] (fine-tuned LoRA adapter).
 * Title generation and categorization use the ML Kit Prompt API ([GenerativeModel]).
 */
@Singleton
class OnDeviceAiService @Inject constructor(
    @ApplicationContext private val context: Context
) : AiService {

    // --- Summarization client (ML Kit feature-specific API, ListenableFuture-based) ---
    private val summarizer: Summarizer = Summarization.getClient(
        SummarizerOptions.builder(context)
            .setInputType(InputType.ARTICLE)
            .setOutputType(OutputType.THREE_BULLETS)
            .setLanguage(Language.ENGLISH)
            .setLongInputAutoTruncationEnabled(true) // auto-truncate at the 4000 token limit
            .build()
    )

    // --- Prompt API client (Kotlin coroutines suspend API) ---
    private val generativeModel: GenerativeModel = Generation.getClient()

    // ------------------------------------------------------------------
    // summarize() — ML Kit Summarization API (streaming)
    // ------------------------------------------------------------------

    override fun summarize(text: String): Flow<AiResult> = flow {
        val start = System.currentTimeMillis()

        val featureStatus = try {
            summarizer.checkFeatureStatus().await() // ListenableFuture → coroutine
        } catch (e: Exception) {
            emit(AiResult(isAvailable = false, errorMessage = "Could not check AI status: ${e.message}"))
            return@flow
        }

        when (featureStatus) {
            FeatureStatus.UNAVAILABLE -> {
                emit(AiResult(
                    isAvailable = false,
                    errorMessage = "Gemini Nano is not supported on this device. " +
                        "Supported devices include Pixel 9/10, Samsung Galaxy S25/S26, and select flagships."
                ))
                return@flow
            }
            FeatureStatus.DOWNLOADABLE -> {
                try {
                    downloadSummarizationFeature()
                } catch (e: GenAiException) {
                    emit(AiResult(errorMessage = "Feature download failed: ${e.message}"))
                    return@flow
                }
            }
            FeatureStatus.DOWNLOADING,
            FeatureStatus.AVAILABLE -> { /* proceed to inference */ }
        }

        try {
            var lastText = ""
            // Streaming: onNewText delivers cumulative (not delta) text on each callback
            summarizer.runInference(
                SummarizationRequest.builder(text).build(),
                StreamingCallback { cumulativeText -> lastText = cumulativeText }
            ).await() // await ListenableFuture completion
            emit(AiResult(text = lastText, latencyMs = System.currentTimeMillis() - start))
        } catch (e: Exception) {
            emit(AiResult(errorMessage = "Summarization failed: ${e.message}"))
        }
    }

    // ------------------------------------------------------------------
    // generateTitle() — ML Kit Prompt API (non-streaming, short output)
    // ------------------------------------------------------------------

    override fun generateTitle(text: String): Flow<AiResult> = promptFlow(
        prompt = "Generate a concise title (maximum 8 words) for the following note. " +
            "Return only the title text, no quotes or extra punctuation:\n\n$text",
        fallback = "Untitled"
    )

    // ------------------------------------------------------------------
    // categorize() — ML Kit Prompt API (non-streaming, 1 token output)
    // ------------------------------------------------------------------

    override fun categorize(text: String): Flow<AiResult> = promptFlow(
        prompt = "Classify the following note into exactly one category: " +
            "IDEA, TODO, REFERENCE, JOURNAL, or OTHER. " +
            "Return only the single category word, nothing else:\n\n$text",
        fallback = "OTHER"
    )

    // ------------------------------------------------------------------
    // Shared Prompt API helper (suspend coroutines)
    // ------------------------------------------------------------------

    private fun promptFlow(prompt: String, fallback: String = ""): Flow<AiResult> = flow {
        val start = System.currentTimeMillis()

        // Prompt API: checkStatus() is a suspend fun returning @FeatureStatus Int
        val featureStatus = try {
            generativeModel.checkStatus()
        } catch (e: Exception) {
            emit(AiResult(isAvailable = false, errorMessage = "Could not check Prompt API status: ${e.message}"))
            return@flow
        }

        when (featureStatus) {
            FeatureStatus.UNAVAILABLE -> {
                emit(AiResult(
                    isAvailable = false,
                    errorMessage = "Gemini Nano Prompt API is not available on this device."
                ))
                return@flow
            }
            FeatureStatus.DOWNLOADABLE -> {
                // Prompt API: download() returns Flow<DownloadStatus>
                try {
                    generativeModel.download().collect { status ->
                        when (status) {
                            is DownloadStatus.DownloadStarted ->
                                Log.d(TAG, "Prompt API download started: ${status.bytesToDownload} bytes")
                            is DownloadStatus.DownloadProgress ->
                                Log.d(TAG, "Prompt API downloaded: ${status.totalBytesDownloaded} bytes")
                            DownloadStatus.DownloadCompleted ->
                                Log.d(TAG, "Prompt API download complete")
                            is DownloadStatus.DownloadFailed ->
                                throw status.e
                        }
                    }
                } catch (e: Exception) {
                    emit(AiResult(errorMessage = "Prompt API download failed: ${e.message}"))
                    return@flow
                }
            }
            FeatureStatus.DOWNLOADING,
            FeatureStatus.AVAILABLE -> { /* proceed */ }
        }

        try {
            // Non-streaming: title (~8 tokens) and category (1 token) are short outputs.
            // generateContent(String) is a suspend fun on GenerativeModel.
            val response = generativeModel.generateContent(prompt)
            val text = response.candidates.firstOrNull()?.text?.trim()?.takeIf { it.isNotBlank() } ?: fallback
            emit(AiResult(text = text, latencyMs = System.currentTimeMillis() - start))
        } catch (e: Exception) {
            emit(AiResult(text = fallback, errorMessage = "Prompt inference failed: ${e.message}"))
        }
    }

    // ------------------------------------------------------------------
    // Summarizer download helper — converts DownloadCallback to coroutine suspend
    // ------------------------------------------------------------------

    private suspend fun downloadSummarizationFeature() = suspendCancellableCoroutine { cont ->
        summarizer.downloadFeature(object : DownloadCallback {
            override fun onDownloadStarted(bytesToDownload: Long) {
                Log.d(TAG, "Summarization download started: $bytesToDownload bytes")
            }
            override fun onDownloadProgress(totalBytesDownloaded: Long) {}
            override fun onDownloadCompleted() = cont.resume(Unit)
            override fun onDownloadFailed(e: GenAiException) = cont.resumeWithException(e)
        })
    }

    // ------------------------------------------------------------------
    // Lifecycle — called from ViewModel.onCleared()
    // ------------------------------------------------------------------

    override fun close() {
        summarizer.close()
        generativeModel.close()
    }
}
