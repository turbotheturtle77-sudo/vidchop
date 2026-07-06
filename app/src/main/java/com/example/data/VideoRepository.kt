package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class VideoRepository(private val dao: VideoProjectDao) {

    val allProjects: Flow<List<VideoProject>> = dao.getAllProjects()

    suspend fun getProjectById(id: Int): VideoProject? {
        return dao.getProjectById(id)
    }

    suspend fun insertProject(project: VideoProject): Long {
        return dao.insertProject(project)
    }

    suspend fun updateProject(project: VideoProject) {
        dao.updateProject(project)
    }

    suspend fun deleteProject(project: VideoProject) {
        dao.deleteProject(project)
    }

    suspend fun createDemoProject(name: String, aspectRatio: String): Long {
        val demoClips = listOf(
            VideoClip(
                id = UUID.randomUUID().toString(),
                title = "Cyber City Neon Drone",
                type = "VIDEO",
                durationMs = 6000L,
                filterType = "CYBERPUNK",
                colorHex = "#0D0C1D"
            ),
            VideoClip(
                id = UUID.randomUUID().toString(),
                title = "Dramatic Sunset Peak",
                type = "VIDEO",
                durationMs = 5000L,
                filterType = "DRAMATIC",
                colorHex = "#2B1B17"
            ),
            VideoClip(
                id = UUID.randomUUID().toString(),
                title = "Grayscale Old-School VHS",
                type = "VIDEO",
                durationMs = 4000L,
                filterType = "GRAYSCALE",
                colorHex = "#1F1F1F"
            ),
            VideoClip(
                id = UUID.randomUUID().toString(),
                title = "Tech Future Outro Panel",
                type = "IMAGE",
                durationMs = 5000L,
                filterType = "COOL",
                colorHex = "#0A1128"
            )
        )

        val demoAudio = listOf(
            AudioClip(
                id = UUID.randomUUID().toString(),
                title = "Retro Future Beats",
                musicTrack = "FUTURE",
                durationMs = 25000L,
                startOffsetMs = 0L,
                volume = 0.8f
            ),
            AudioClip(
                id = UUID.randomUUID().toString(),
                title = "Cinematic Ambient Soundscape",
                musicTrack = "CINEMATIC",
                durationMs = 15000L,
                startOffsetMs = 10000L,
                volume = 0.5f
            )
        )

        val demoTexts = listOf(
            TextOverlay(
                id = UUID.randomUUID().toString(),
                text = "Welcome to the Cyberpunk City Grid.",
                startMs = 0L,
                endMs = 4500L,
                colorHex = "#00FFFF",
                fontSizeSp = 20f,
                positionY = 0.82f
            ),
            TextOverlay(
                id = UUID.randomUUID().toString(),
                text = "This dramatic sunset marks our new beginning...",
                startMs = 6000L,
                endMs = 10500L,
                colorHex = "#FF4500",
                fontSizeSp = 18f,
                positionY = 0.78f
            ),
            TextOverlay(
                id = UUID.randomUUID().toString(),
                text = "Filmed on custom retro VHS lens.",
                startMs = 11000L,
                endMs = 15000L,
                colorHex = "#CCCCCC",
                fontSizeSp = 18f,
                positionY = 0.85f
            ),
            TextOverlay(
                id = UUID.randomUUID().toString(),
                text = "Subscribe for more edits and tutorials!",
                startMs = 15500L,
                endMs = 20000L,
                colorHex = "#FF007F",
                fontSizeSp = 22f,
                positionY = 0.5f,
                animationType = "ZOOM"
            )
        )

        val project = VideoProject(
            name = name,
            aspectRatio = aspectRatio,
            clips = demoClips,
            audioClips = demoAudio,
            textOverlays = demoTexts
        )

        return dao.insertProject(project)
    }

    suspend fun createBlankProject(name: String, aspectRatio: String): Long {
        val blank = VideoProject(
            name = name,
            aspectRatio = aspectRatio,
            clips = listOf(
                VideoClip(
                    id = UUID.randomUUID().toString(),
                    title = "Scene 1",
                    type = "VIDEO",
                    durationMs = 5000L,
                    colorHex = "#212130"
                )
            )
        )
        return dao.insertProject(blank)
    }
}
