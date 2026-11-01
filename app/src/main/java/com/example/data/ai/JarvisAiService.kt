package com.example.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "JarvisAiService"

class JarvisAiService {

    private fun buildSystemInstruction(callsign: String): String {
        return """
            You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the iconic personal AI created by Tony Stark.
            Address the user as "$callsign".
            Your manner is cultured, polite, British-accented in cadence, supremely competent, slightly witty, and unconditionally loyal.
            Speak with confidence and clarity.
            Provide concise, direct, and insightful answers suited for both an executive heads-up display (HUD) and natural speech delivery.
            When presenting tactical protocols, calculations, or system recommendations, use structured bullet points.
            Never refer to yourself as a generic large language model. You are J.A.R.V.I.S.
        """.trimIndent()
    }

    private fun getModel(callsign: String): GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(buildSystemInstruction(callsign)) }
        )
    }

    fun streamJarvisResponse(prompt: String, callsign: String): Flow<String> = flow {
        val model = getModel(callsign)
        withTimeout(60_000L) {
            val stream = model.generateContentStream(prompt)
            stream.collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    emit(chunkText)
                }
            }
        }
    }

    suspend fun generateSingleResponse(prompt: String, callsign: String): String {
        val model = getModel(callsign)
        return try {
            withTimeout(60_000L) {
                val response = model.generateContent(prompt)
                response.text ?: "At your service, $callsign. All diagnostic protocols are functioning normally."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Jarvis response", e)
            throw e
        }
    }
}
