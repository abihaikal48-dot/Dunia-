package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- Retrofit API Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- DTO Classes ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

// --- Retrofit Client ---

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- Gemini client accessor ---

object GeminiClient {
    suspend fun generateAdvisorResponse(prompt: String, systemPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Konfigurasi API Key Gemini tidak ditemukan atau masih default di panel AI Studio. Mohon atur API Key yang valid terlebih dahulu untuk mengaktifkan DUNIA AI."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        var lastErr: Exception? = null
        for (attempt in 1..2) {
            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "DUNIA AI tidak mengembalikan respon. Silakan coba tanyakan kembali."
            } catch (e: Exception) {
                lastErr = e
                if (attempt < 2) {
                    // Quick sleep before retry for transient 503
                    kotlinx.coroutines.delay(1200)
                }
            }
        }

        val errMsg = lastErr?.localizedMessage ?: "Koneksi terputus"
        if (errMsg.contains("503") || errMsg.contains("Service Unavailable")) {
            "DUNIA AI (Gemini) sedang mengalami kepadatan lalu lintas server (HTTP 503). Silakan tunggu sekitar 5 detik lalu kirim ulang pesan Anda ✨"
        } else {
            "Gagal mendapatkan respon dari DUNIA AI: $errMsg. Silakan periksa koneksi internet Anda atau coba kembali."
        }
    }
}
