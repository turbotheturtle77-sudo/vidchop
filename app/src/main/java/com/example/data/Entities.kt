package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VideoClip(
    val id: String, // Unique UUID for clip-level selection in timeline
    val title: String,
    val type: String, // "VIDEO" or "IMAGE"
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val filterType: String = "NONE", // "NONE", "GRAYSCALE", "SEPIA", "WARM", "COOL", "CYBERPUNK", "DRAMATIC"
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val speed: Float = 1.0f,
    val colorHex: String = "#2A2A3A" // For solid placeholders
) {
    val activeDurationMs: Long
        get() = ((trimEndMs - trimStartMs) / speed).toLong()
}

@JsonClass(generateAdapter = true)
data class AudioClip(
    val id: String,
    val title: String,
    val musicTrack: String, // "LOFI", "CINEMATIC", "UPBEAT", "FUTURE", "NONE"
    val durationMs: Long,
    val startOffsetMs: Long,
    val volume: Float = 1.0f
)

@JsonClass(generateAdapter = true)
data class TextOverlay(
    val id: String,
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val colorHex: String = "#FFFFFF",
    val fontSizeSp: Float = 18f,
    val positionY: Float = 0.8f, // 0.0 (top) to 1.0 (bottom)
    val animationType: String = "NONE" // "NONE", "FADE", "ZOOM"
)

@Entity(tableName = "video_projects")
data class VideoProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val aspectRatio: String = "16:9", // "16:9", "9:16", "1:1"
    val createdAt: Long = System.currentTimeMillis(),
    val clips: List<VideoClip> = emptyList(),
    val audioClips: List<AudioClip> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList()
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.activeDurationMs }
}
