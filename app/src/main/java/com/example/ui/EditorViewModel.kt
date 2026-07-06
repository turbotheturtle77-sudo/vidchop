package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.api.GeminiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AiState {
    object Idle : AiState
    object Loading : AiState
    data class Success(val projectName: String) : AiState
    data class Error(val message: String) : AiState
}

sealed interface ExportState {
    object Idle : ExportState
    data class Exporting(
        val progress: Float,
        val statusText: String,
        val currentFrameColorHex: String,
        val currentFrameFilter: String
    ) : ExportState
    data class Success(val savedFileName: String, val durationMs: Long, val resolution: String) : ExportState
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = VideoRepository(db.videoProjectDao())

    // All available projects
    val projects: StateFlow<List<VideoProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active project under editing
    private val _activeProject = MutableStateFlow<VideoProject?>(null)
    val activeProject: StateFlow<VideoProject?> = _activeProject.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    // Editing selections
    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _selectedOverlayId = MutableStateFlow<String?>(null)
    val selectedOverlayId: StateFlow<String?> = _selectedOverlayId.asStateFlow()

    private val _selectedAudioId = MutableStateFlow<String?>(null)
    val selectedAudioId: StateFlow<String?> = _selectedAudioId.asStateFlow()

    // AI Assist state
    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    // Export simulation state
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private var playbackJob: Job? = null

    init {
        // Automatically pre-load sample project if no projects exist
        viewModelScope.launch {
            projects.first { true } // Wait for flow initialization
            delay(500)
            if (projects.value.isEmpty()) {
                repository.createDemoProject("Neon Beats Horizon", "16:9")
            }
        }
    }

    fun selectProject(project: VideoProject) {
        _activeProject.value = project
        _playheadMs.value = 0L
        _isPlaying.value = false
        _selectedClipId.value = project.clips.firstOrNull()?.id
        _selectedOverlayId.value = null
        _selectedAudioId.value = null
        playbackJob?.cancel()
    }

    fun createProject(name: String, aspectRatio: String) {
        viewModelScope.launch {
            val id = repository.createBlankProject(name, aspectRatio)
            val created = repository.getProjectById(id.toInt())
            if (created != null) {
                selectProject(created)
            }
        }
    }

    fun deleteProject(project: VideoProject) {
        viewModelScope.launch {
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = null
                _isPlaying.value = false
                playbackJob?.cancel()
            }
            repository.deleteProject(project)
        }
    }

    // Playback loop
    fun togglePlay() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    private fun play() {
        val project = _activeProject.value ?: return
        val duration = project.totalDurationMs
        if (duration == 0L) return

        if (_playheadMs.value >= duration) {
            _playheadMs.value = 0L
        }

        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (_isPlaying.value) {
                delay(16) // ~60fps refresh
                val now = System.currentTimeMillis()
                val delta = now - lastTime
                lastTime = now

                val nextPlayhead = _playheadMs.value + delta
                if (nextPlayhead >= duration) {
                    _playheadMs.value = duration
                    _isPlaying.value = false
                    break
                } else {
                    _playheadMs.value = nextPlayhead
                }
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun seekTo(ms: Long) {
        val project = _activeProject.value ?: return
        val maxDuration = project.totalDurationMs
        _playheadMs.value = ms.coerceIn(0L, maxDuration)
    }

    fun stepFrame(direction: Int) {
        // direction: -1 (prev frame), +1 (next frame)
        // 1 frame ~ 33ms (30 FPS)
        val project = _activeProject.value ?: return
        val nextPlayhead = _playheadMs.value + (direction * 33)
        _playheadMs.value = nextPlayhead.coerceIn(0L, project.totalDurationMs)
    }

    fun selectClip(clipId: String?) {
        _selectedClipId.value = clipId
        _selectedOverlayId.value = null
        _selectedAudioId.value = null
    }

    fun selectOverlay(overlayId: String?) {
        _selectedOverlayId.value = overlayId
        _selectedClipId.value = null
        _selectedAudioId.value = null
    }

    fun selectAudio(audioId: String?) {
        _selectedAudioId.value = audioId
        _selectedClipId.value = null
        _selectedOverlayId.value = null
    }

    // Timeline editing APIs
    fun addClipToActive(title: String, type: String, durationMs: Long, colorHex: String) {
        val project = _activeProject.value ?: return
        val newClip = VideoClip(
            id = UUID.randomUUID().toString(),
            title = title,
            type = type,
            durationMs = durationMs,
            colorHex = colorHex
        )
        val updatedClips = project.clips + newClip
        val updatedProject = project.copy(clips = updatedClips)
        saveActiveProject(updatedProject)
    }

    fun removeClipFromActive(clipId: String) {
        val project = _activeProject.value ?: return
        val updatedClips = project.clips.filter { it.id != clipId }
        val updatedProject = project.copy(clips = updatedClips)
        saveActiveProject(updatedProject)
        if (_selectedClipId.value == clipId) {
            _selectedClipId.value = updatedClips.firstOrNull()?.id
        }
    }

    fun updateClipFilterInActive(clipId: String, filter: String) {
        val project = _activeProject.value ?: return
        val updatedClips = project.clips.map {
            if (it.id == clipId) it.copy(filterType = filter) else it
        }
        val updatedProject = project.copy(clips = updatedClips)
        saveActiveProject(updatedProject)
    }

    fun updateClipAdjustmentsInActive(
        clipId: String,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        speed: Float
    ) {
        val project = _activeProject.value ?: return
        val updatedClips = project.clips.map {
            if (it.id == clipId) {
                // Adjust trimEndMs dynamically based on speed if clip is modified
                it.copy(
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    speed = speed
                )
            } else {
                it
            }
        }
        val updatedProject = project.copy(clips = updatedClips)
        saveActiveProject(updatedProject)
    }

    fun updateClipTrimInActive(clipId: String, startMs: Long, endMs: Long) {
        val project = _activeProject.value ?: return
        val updatedClips = project.clips.map {
            if (it.id == clipId) {
                it.copy(
                    trimStartMs = startMs.coerceIn(0L, it.durationMs),
                    trimEndMs = endMs.coerceIn(startMs, it.durationMs)
                )
            } else {
                it
            }
        }
        val updatedProject = project.copy(clips = updatedClips)
        saveActiveProject(updatedProject)
    }

    fun addSubtitleToActive(text: String, startMs: Long, endMs: Long) {
        val project = _activeProject.value ?: return
        val newOverlay = TextOverlay(
            id = UUID.randomUUID().toString(),
            text = text,
            startMs = startMs,
            endMs = endMs
        )
        val updatedProject = project.copy(textOverlays = project.textOverlays + newOverlay)
        saveActiveProject(updatedProject)
    }

    fun updateSubtitleInActive(
        id: String,
        text: String,
        startMs: Long,
        endMs: Long,
        colorHex: String,
        fontSizeSp: Float,
        positionY: Float
    ) {
        val project = _activeProject.value ?: return
        val updatedOverlays = project.textOverlays.map {
            if (it.id == id) {
                it.copy(
                    text = text,
                    startMs = startMs,
                    endMs = endMs,
                    colorHex = colorHex,
                    fontSizeSp = fontSizeSp,
                    positionY = positionY
                )
            } else {
                it
            }
        }
        val updatedProject = project.copy(textOverlays = updatedOverlays)
        saveActiveProject(updatedProject)
    }

    fun removeSubtitleFromActive(id: String) {
        val project = _activeProject.value ?: return
        val updatedOverlays = project.textOverlays.filter { it.id != id }
        val updatedProject = project.copy(textOverlays = updatedOverlays)
        saveActiveProject(updatedProject)
        if (_selectedOverlayId.value == id) {
            _selectedOverlayId.value = null
        }
    }

    fun addAudioTrackToActive(title: String, musicTrack: String, durationMs: Long, offsetMs: Long) {
        val project = _activeProject.value ?: return
        val newAudio = AudioClip(
            id = UUID.randomUUID().toString(),
            title = title,
            musicTrack = musicTrack,
            durationMs = durationMs,
            startOffsetMs = offsetMs
        )
        val updatedProject = project.copy(audioClips = project.audioClips + newAudio)
        saveActiveProject(updatedProject)
    }

    fun removeAudioFromActive(id: String) {
        val project = _activeProject.value ?: return
        val updatedAudios = project.audioClips.filter { it.id != id }
        val updatedProject = project.copy(audioClips = updatedAudios)
        saveActiveProject(updatedProject)
        if (_selectedAudioId.value == id) {
            _selectedAudioId.value = null
        }
    }

    private fun saveActiveProject(project: VideoProject) {
        _activeProject.value = project
        viewModelScope.launch {
            repository.updateProject(project)
        }
    }

    // AI Script to Video compilation via Gemini
    fun generateVideoWithAi(prompt: String) {
        _aiState.value = AiState.Loading
        viewModelScope.launch {
            try {
                val composition = GeminiClient.generateVideoComposition(prompt)
                if (composition != null) {
                    val convertedClips = composition.clips.map { aiClip ->
                        VideoClip(
                            id = UUID.randomUUID().toString(),
                            title = aiClip.title,
                            type = aiClip.type,
                            durationMs = aiClip.durationMs,
                            filterType = aiClip.filterType,
                            colorHex = aiClip.colorHex
                        )
                    }

                    val convertedSubtitles = composition.subtitles.map { aiSub ->
                        TextOverlay(
                            id = UUID.randomUUID().toString(),
                            text = aiSub.text,
                            startMs = aiSub.startMs,
                            endMs = aiSub.endMs,
                            colorHex = aiSub.colorHex
                        )
                    }

                    val project = VideoProject(
                        name = "AI: " + composition.projectName,
                        clips = convertedClips,
                        textOverlays = convertedSubtitles,
                        audioClips = listOf(
                            AudioClip(
                                id = UUID.randomUUID().toString(),
                                title = "Ambient AI Synth",
                                musicTrack = "FUTURE",
                                durationMs = convertedClips.sumOf { it.activeDurationMs },
                                startOffsetMs = 0L
                            )
                        )
                    )

                    val newId = repository.insertProject(project)
                    val created = repository.getProjectById(newId.toInt())
                    if (created != null) {
                        selectProject(created)
                        _aiState.value = AiState.Success(created.name)
                    } else {
                        _aiState.value = AiState.Error("Failed to store generated project")
                    }
                } else {
                    _aiState.value = AiState.Error("AI returned invalid structure")
                }
            } catch (e: Exception) {
                _aiState.value = AiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun clearAiState() {
        _aiState.value = AiState.Idle
    }

    // Export pipeline simulation
    fun startExportSimulation(resolution: String, fps: Int, format: String) {
        val project = _activeProject.value ?: return
        _exportState.value = ExportState.Exporting(
            progress = 0f,
            statusText = "Initializing renderer pipeline...",
            currentFrameColorHex = "#000000",
            currentFrameFilter = "NONE"
        )

        viewModelScope.launch {
            val totalClips = project.clips.size
            if (totalClips == 0) {
                _exportState.value = ExportState.Idle
                return@launch
            }

            // Simulate frame processing
            for (step in 1..10) {
                delay(400)
                val progress = step / 10f
                val clipIndex = ((progress * totalClips).toInt() - 1).coerceIn(0, totalClips - 1)
                val activeClip = project.clips[clipIndex]

                val status = when (step) {
                    1 -> "Configuring canvas context at $resolution @ ${fps}fps..."
                    2 -> "Decoding source textures for scene 1: '${project.clips.firstOrNull()?.title}'..."
                    3 -> "Synthesizing transitions and crossfades..."
                    4 -> "Applying shader filters: '${activeClip.filterType}' on Active Frame..."
                    5 -> "Processing track color grading matrix (Brightness: ${activeClip.brightness}x)..."
                    6 -> "Rendering typography overlay: ${project.textOverlays.size} text frames..."
                    7 -> "Mixing audio channels & encoding AAC stream..."
                    8 -> "Combining video & audio stream into $format container..."
                    9 -> "Optimizing fast-start moov atom..."
                    else -> "Writing final stream data to on-disk storage..."
                }

                _exportState.value = ExportState.Exporting(
                    progress = progress,
                    statusText = status,
                    currentFrameColorHex = activeClip.colorHex,
                    currentFrameFilter = activeClip.filterType
                )
            }

            delay(200)
            val fileName = "Export_${project.name.replace(" ", "_")}_${resolution.split(" ")[0]}.$format"
            _exportState.value = ExportState.Success(
                savedFileName = fileName,
                durationMs = project.totalDurationMs,
                resolution = resolution
            )
        }
    }

    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }
}
