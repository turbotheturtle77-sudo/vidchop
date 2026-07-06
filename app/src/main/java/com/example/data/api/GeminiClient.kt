package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = "application/json",
    val temperature: Float? = 0.7f
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

// --- Local representation of generated video script to parse ---
@JsonClass(generateAdapter = true)
data class AiClip(
    val title: String,
    val type: String, // "VIDEO" or "IMAGE"
    val durationMs: Long,
    val filterType: String, // "NONE", "GRAYSCALE", "SEPIA", "WARM", "COOL", "CYBERPUNK", "DRAMATIC"
    val colorHex: String
)

@JsonClass(generateAdapter = true)
data class AiSubtitle(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val colorHex: String
)

@JsonClass(generateAdapter = true)
data class AiVideoComposition(
    val projectName: String,
    val clips: List<AiClip>,
    val subtitles: List<AiSubtitle>
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun generateVideoComposition(prompt: String): AiVideoComposition? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("API Key is missing or default placeholder. Please enter your key in the Secrets panel.")
        }

        val systemPrompt = """
            You are a creative Video Director & Scriptwriter Assistant.
            Given a user prompt describing a video concept, you must write a structured 4-scene video script composition.
            You MUST return ONLY a JSON object that matches this structure:
            {
              "projectName": "Name of project based on prompt",
              "clips": [
                {
                  "title": "Short descriptive scene title",
                  "type": "VIDEO", or "IMAGE"
                  "durationMs": duration in milliseconds (between 3000 and 6000),
                  "filterType": "CYBERPUNK", "GRAYSCALE", "SEPIA", "WARM", "COOL", "DRAMATIC" or "NONE",
                  "colorHex": "hex color representing scene mood (e.g. #FF1493)"
                }
              ],
              "subtitles": [
                {
                  "text": "Subtitles or voiceover script to display",
                  "startMs": start timestamp in milliseconds,
                  "endMs": end timestamp in milliseconds
                }
              ]
            }
            Ensure timeline timestamps align properly. Do not include markdown code block characters like ```json.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Create a script composition for: $prompt")))),
            generationConfig = GenerationConfig(),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val response = service.generateContent(apiKey, request)
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Empty response from AI")

        // Clean up text if any markdown wrap-around exists
        val cleanJson = responseText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return moshi.adapter(AiVideoComposition::class.java).fromJson(cleanJson)
    }
}
