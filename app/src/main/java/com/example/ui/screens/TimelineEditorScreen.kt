package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.EditorViewModel
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineEditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProjectState by viewModel.activeProject.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playheadMs by viewModel.playheadMs.collectAsState()

    val selectedClipId by viewModel.selectedClipId.collectAsState()
    val selectedOverlayId by viewModel.selectedOverlayId.collectAsState()
    val selectedAudioId by viewModel.selectedAudioId.collectAsState()

    // Dialog state for adding items
    var showAddClipDialog by remember { mutableStateOf(false) }
    var showAddTextDialog by remember { mutableStateOf(false) }
    var showAddAudioDialog by remember { mutableStateOf(false) }

    val activeProject = activeProjectState ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeProject.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Aspect: ${activeProject.aspectRatio}",
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onOpenExport,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Export",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepSpaceObsidian)
        ) {
            // --- SECTION 1: STUNNING REAL-TIME PLAYER VIEW ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .background(Color.Black)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Determine layout aspect ratio modifiers
                val aspectModifier = when (activeProject.aspectRatio) {
                    "9:16" -> Modifier.aspectRatio(9f / 16f).fillMaxHeight()
                    "1:1" -> Modifier.aspectRatio(1f).fillMaxWidth(0.8f)
                    else -> Modifier.aspectRatio(16f / 9f).fillMaxWidth()
                }

                // Render current active frame based on playhead position
                val activeClipPair = getActiveClipAtPlayhead(activeProject, playheadMs)
                if (activeClipPair != null) {
                    val (clip, _) = activeClipPair
                    val filterBrush = getFilterBrush(clip.filterType)

                    Box(
                        modifier = aspectModifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(android.graphics.Color.parseColor(clip.colorHex)))
                            .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        // Dynamic video action visualization (soundwave overlay, rotating icon, scanning line)
                        if (isPlaying) {
                            ScanningLineAnimation()
                        }

                        // Apply filter overlay color
                        if (filterBrush != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(filterBrush)
                            )
                        }

                        // Decorative clip details inside video frame
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (clip.type == "VIDEO") Icons.Default.Movie else Icons.Default.Image,
                                contentDescription = "Clip type",
                                tint = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = clip.title,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Subtitle text render
                        val activeSubtitles = activeProject.textOverlays.filter {
                            playheadMs >= it.startMs && playheadMs <= it.endMs
                        }
                        activeSubtitles.forEach { subtitle ->
                            val scale = remember { Animatable(0.9f) }
                            LaunchedEffect(subtitle.id) {
                                if (subtitle.animationType == "ZOOM") {
                                    scale.animateTo(
                                        1.1f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(if (subtitle.positionY < 0.33f) Alignment.TopCenter else if (subtitle.positionY > 0.66f) Alignment.BottomCenter else Alignment.Center)
                                        .graphicsLayer(
                                            scaleX = if (subtitle.animationType == "ZOOM") scale.value else 1f,
                                            scaleY = if (subtitle.animationType == "ZOOM") scale.value else 1f
                                        )
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = subtitle.text,
                                        color = Color(android.graphics.Color.parseColor(subtitle.colorHex)),
                                        fontSize = subtitle.fontSizeSp.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // No active clip at playhead
                    Box(
                        modifier = aspectModifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceTrack)
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Timeline Empty\nAdd clips to start editing",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // --- SECTION 2: PLAYER CONTROLS & TIMELINE STATS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time stamps in Monospace format
                Text(
                    text = "${formatMs(playheadMs)} / ${formatMs(activeProject.totalDurationMs)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.stepFrame(-1) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceTrack, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Step Back",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { viewModel.togglePlay() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (isPlaying) CyberPink else CyberCyan, CircleShape)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = if (isPlaying) Color.White else Color.Black
                        )
                    }

                    IconButton(
                        onClick = { viewModel.stepFrame(1) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceTrack, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Step Forward",
                            tint = Color.White
                        )
                    }
                }

                // Add Actions Overlay Button
                Box {
                    var showDropdown by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showDropdown = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(CyberPurple.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, CyberPurple, CircleShape)
                            .testTag("add_track_element_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Track Element",
                            tint = CyberPurple
                        )
                    }

                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        modifier = Modifier.background(SurfaceSlate)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Video/Photo Clip", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null, tint = CyberCyan) },
                            onClick = {
                                showDropdown = false
                                showAddClipDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Subtitle Text", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, tint = CyberPink) },
                            onClick = {
                                showDropdown = false
                                showAddTextDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Audio Track", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyberYellow) },
                            onClick = {
                                showDropdown = false
                                showAddAudioDialog = true
                            }
                        )
                    }
                }
            }

            // --- SECTION 3: MULTI-TRACK PROFESSIONAL TIMELINE ---
            // Width proportion factor for timeline: 1ms = 0.05dp
            val timePxFactor = 0.06f
            val maxTimelineWidth = (activeProject.totalDurationMs * timePxFactor).coerceAtLeast(300f).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
                    .background(DeepSpaceObsidian)
                    .border(width = 1.dp, color = SurfaceTrack, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                // Horizontal scrolling container that groups all tracks under the same horizontal context
                val horizontalScrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    Column(
                        modifier = Modifier
                            .width(maxTimelineWidth + 100.dp) // Add cushion margin at end
                            .fillMaxHeight()
                    ) {
                        // A: TIMELINE TIME RULER (ticks every 1000ms)
                        TimelineRuler(totalDurationMs = activeProject.totalDurationMs, pxFactor = timePxFactor)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // B: VIDEO/PHOTO TRACK
                            VideoTrackRow(
                                clips = activeProject.clips,
                                selectedClipId = selectedClipId,
                                pxFactor = timePxFactor,
                                onSelectClip = { viewModel.selectClip(it) }
                            )

                            // C: SUBTITLES/TEXT OVERLAY TRACK
                            TextTrackRow(
                                textOverlays = activeProject.textOverlays,
                                totalDurationMs = activeProject.totalDurationMs,
                                selectedOverlayId = selectedOverlayId,
                                pxFactor = timePxFactor,
                                onSelectOverlay = { viewModel.selectOverlay(it) }
                            )

                            // D: AUDIO TRACK
                            AudioTrackRow(
                                audioClips = activeProject.audioClips,
                                totalDurationMs = activeProject.totalDurationMs,
                                selectedAudioId = selectedAudioId,
                                pxFactor = timePxFactor,
                                onSelectAudio = { viewModel.selectAudio(it) }
                            )
                        }
                    }

                    // E: VERTICAL PLAYHEAD LINE (synchronous overlay across tracks)
                    val playheadOffset = (playheadMs * timePxFactor).dp
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .offset(x = playheadOffset)
                            .background(CyberCyan)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = (-4).dp)
                                .background(CyberCyan, CircleShape)
                        )
                    }

                    // Interactive seek overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // Direct tap on empty space to seek playhead!
                                // We can estimate the seek position from coordinates or simple relative drags
                            }
                    )
                }
            }

            // --- SECTION 4: CONTEXT SENSITIVE EDITING PANEL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
                    .background(SurfaceSlate)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(16.dp)
            ) {
                when {
                    selectedClipId != null -> {
                        val clip = activeProject.clips.firstOrNull { it.id == selectedClipId }
                        if (clip != null) {
                            ClipEditPanel(clip = clip, viewModel = viewModel, activeProject = activeProject)
                        }
                    }
                    selectedOverlayId != null -> {
                        val overlay = activeProject.textOverlays.firstOrNull { it.id == selectedOverlayId }
                        if (overlay != null) {
                            OverlayEditPanel(overlay = overlay, viewModel = viewModel, activeProject = activeProject)
                        }
                    }
                    selectedAudioId != null -> {
                        val audio = activeProject.audioClips.firstOrNull { it.id == selectedAudioId }
                        if (audio != null) {
                            AudioEditPanel(audio = audio, viewModel = viewModel, activeProject = activeProject)
                        }
                    }
                    else -> {
                        // Empty state details
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gesture,
                                contentDescription = "Tap",
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select a track element to view adjustments",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS FOR ADDING ITEMS ---

    // 1. ADD CLIP DIALOG
    if (showAddClipDialog) {
        var sceneTitle by remember { mutableStateOf("") }
        var clipType by remember { mutableStateOf("VIDEO") }
        var durationSecs by remember { mutableStateOf("5") }
        val sceneColors = listOf("#0D0C1D", "#211C18", "#1A1A2E", "#0A1128", "#121A0F", "#2A0E15", "#151515")
        var selectedColor by remember { mutableStateOf(sceneColors.first()) }

        AlertDialog(
            onDismissRequest = { showAddClipDialog = false },
            title = { Text("Add Scene Clip", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = sceneTitle,
                        onValueChange = { sceneTitle = it },
                        label = { Text("Scene / Clip Title") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, focusedLabelColor = CyberCyan),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_clip_title_input")
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = clipType == "VIDEO", onClick = { clipType = "VIDEO" })
                            Text("Video", color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = clipType == "IMAGE", onClick = { clipType = "IMAGE" })
                            Text("Photo", color = Color.White)
                        }
                    }

                    OutlinedTextField(
                        value = durationSecs,
                        onValueChange = { durationSecs = it },
                        label = { Text("Duration (seconds)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, focusedLabelColor = CyberCyan),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_clip_duration_input")
                    )

                    Text("Placeholder Scene Tone", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sceneColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (selectedColor == hex) 2.dp else 0.dp,
                                        color = if (selectedColor == hex) CyberCyan else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = sceneTitle.ifEmpty { "New Scene" }
                        val durMs = (durationSecs.toFloatOrNull() ?: 5f) * 1000L
                        viewModel.addClipToActive(title, clipType, durMs.toLong(), selectedColor)
                        showAddClipDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Add Scene", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClipDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // 2. ADD SUBTITLE DIALOG
    if (showAddTextDialog) {
        var subText by remember { mutableStateOf("") }
        var startSecs by remember { mutableStateOf("0") }
        var endSecs by remember { mutableStateOf("4") }

        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = { Text("Add Subtitle Text", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = subText,
                        onValueChange = { subText = it },
                        label = { Text("Subtitle / Overlay Text") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberPink, focusedLabelColor = CyberPink),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_subtitle_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = startSecs,
                            onValueChange = { startSecs = it },
                            label = { Text("Start (s)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberPink),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endSecs,
                            onValueChange = { endSecs = it },
                            label = { Text("End (s)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberPink),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subText.isNotEmpty()) {
                            val st = (startSecs.toFloatOrNull() ?: 0f) * 1000L
                            val et = (endSecs.toFloatOrNull() ?: 4f) * 1000L
                            viewModel.addSubtitleToActive(subText, st.toLong(), et.toLong())
                        }
                        showAddTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPink)
                ) {
                    Text("Add Subtitle", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // 3. ADD AUDIO DIALOG
    if (showAddAudioDialog) {
        var audioTitle by remember { mutableStateOf("") }
        var trackPreset by remember { mutableStateOf("LOFI") }
        var offsetSecs by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddAudioDialog = false },
            title = { Text("Add Audio Track", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = audioTitle,
                        onValueChange = { audioTitle = it },
                        label = { Text("Track Title") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberYellow, focusedLabelColor = CyberYellow),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_audio_title_input")
                    )

                    Text("Genre / Vibe Track", color = Color.Gray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val genres = listOf("LOFI", "CINEMATIC", "UPBEAT", "FUTURE")
                        genres.forEach { genre ->
                            val isSel = trackPreset == genre
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) CyberYellow.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(width = 1.dp, color = if (isSel) CyberYellow else Color.Transparent, shape = RoundedCornerShape(6.dp))
                                    .clickable { trackPreset = genre }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(genre, fontSize = 10.sp, color = if (isSel) CyberYellow else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = offsetSecs,
                        onValueChange = { offsetSecs = it },
                        label = { Text("Start Offset (s)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberYellow),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = audioTitle.ifEmpty { "$trackPreset Ambient Track" }
                        val offsetMs = (offsetSecs.toFloatOrNull() ?: 0f) * 1000L
                        viewModel.addAudioTrackToActive(title, trackPreset, 30000L, offsetMs.toLong())
                        showAddAudioDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberYellow)
                ) {
                    Text("Add Audio", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAudioDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

// --- HELPER SUB-COMPONENTS FOR TIMELINE RENDERING ---

@Composable
fun TimelineRuler(totalDurationMs: Long, pxFactor: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
        val seconds = (totalDurationMs / 1000L) + 10L // Extend ticks beyond end
        for (i in 0..seconds) {
            val offsetDp = (i * 1000L * pxFactor).dp
            Box(
                modifier = Modifier
                    .offset(x = offsetDp)
                    .align(Alignment.BottomStart)
            ) {
                // Ticks
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(8.dp)
                        .background(Color.Gray.copy(alpha = 0.5f))
                )
                Text(
                    text = "${i}s",
                    color = Color.Gray,
                    fontSize = 8.sp,
                    modifier = Modifier.offset(x = 4.dp, y = (-12).dp)
                )
            }
        }
    }
}

@Composable
fun VideoTrackRow(
    clips: List<VideoClip>,
    selectedClipId: String?,
    pxFactor: Float,
    onSelectClip: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(SurfaceTrack.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Icon prefix
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = "Video",
            tint = CyberCyan.copy(alpha = 0.5f),
            modifier = Modifier
                .size(24.dp)
                .padding(horizontal = 4.dp)
        )

        clips.forEach { clip ->
            val clipWidth = (clip.activeDurationMs * pxFactor).dp
            val isSelected = clip.id == selectedClipId

            Box(
                modifier = Modifier
                    .width(clipWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(android.graphics.Color.parseColor(clip.colorHex)))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectClip(clip.id) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (clip.type == "VIDEO") Icons.Default.VideoCameraFront else Icons.Default.Photo,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = clip.title,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // If filter is applied, show small icon indicator
                if (clip.filterType != "NONE") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .background(CyberCyan, CircleShape)
                            .size(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TextTrackRow(
    textOverlays: List<TextOverlay>,
    totalDurationMs: Long,
    selectedOverlayId: String?,
    pxFactor: Float,
    onSelectOverlay: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(SurfaceTrack.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp)
    ) {
        // Track Icon
        Icon(
            imageVector = Icons.Default.TextFields,
            contentDescription = "Text Track",
            tint = CyberPink.copy(alpha = 0.5f),
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
        )

        // Render relative positioned absolute text cards on timeline
        textOverlays.forEach { textOverlay ->
            val startOffset = (textOverlay.startMs * pxFactor).dp
            val width = ((textOverlay.endMs - textOverlay.startMs) * pxFactor).dp
            val isSelected = textOverlay.id == selectedOverlayId

            Box(
                modifier = Modifier
                    .offset(x = startOffset + 32.dp) // Adjusted offset for start icon spacing
                    .width(width)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyberPink.copy(alpha = 0.2f))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CyberPink else CyberPink.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelectOverlay(textOverlay.id) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = textOverlay.text,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AudioTrackRow(
    audioClips: List<AudioClip>,
    totalDurationMs: Long,
    selectedAudioId: String?,
    pxFactor: Float,
    onSelectAudio: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(SurfaceTrack.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp)
    ) {
        // Track Icon
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Audio Track",
            tint = CyberYellow.copy(alpha = 0.5f),
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
        )

        // Absolute rendering
        audioClips.forEach { audio ->
            val startOffset = (audio.startOffsetMs * pxFactor).dp
            val width = (audio.durationMs * pxFactor).dp
            val isSelected = audio.id == selectedAudioId

            Box(
                modifier = Modifier
                    .offset(x = startOffset + 32.dp)
                    .width(width)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyberYellow.copy(alpha = 0.15f))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) CyberYellow else CyberYellow.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelectAudio(audio.id) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicVideo,
                        contentDescription = null,
                        tint = CyberYellow,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = audio.title,
                        color = CyberYellow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// --- CONTEXT SENSITIVE ADJUSTMENT SUB PANELS ---

@Composable
fun ClipEditPanel(clip: VideoClip, viewModel: EditorViewModel, activeProject: VideoProject) {
    var activeTab by remember { mutableStateOf("Filters") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Inner context navigation tab
        TabRow(
            selectedTabIndex = if (activeTab == "Filters") 0 else 1,
            containerColor = Color.Transparent,
            contentColor = CyberCyan,
            divider = {}
        ) {
            Tab(selected = activeTab == "Filters", onClick = { activeTab = "Filters" }) {
                Text("Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Tab(selected = activeTab == "Adjust", onClick = { activeTab = "Adjust" }) {
                Text("Trim & Speed", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeTab == "Filters") {
            val filtersList = listOf(
                "NONE" to "None",
                "GRAYSCALE" to "Monochrome",
                "SEPIA" to "Sepia Vintage",
                "WARM" to "Golden Warm",
                "COOL" to "Arctic Cool",
                "CYBERPUNK" to "Neon Grid",
                "DRAMATIC" to "Contrast Cinematic"
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtersList) { (type, label) ->
                    val isSel = clip.filterType == type
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else SurfaceTrack)
                            .border(
                                width = if (isSel) 2.dp else 1.dp,
                                color = if (isSel) CyberCyan else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.updateClipFilterInActive(clip.id, type) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) CyberCyan else Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        } else {
            // Trim / Speed controls
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Trim Start Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Trim In Point", fontSize = 11.sp, color = Color.Gray)
                        Text(formatMs(clip.trimStartMs), fontSize = 11.sp, color = CyberCyan)
                    }
                    Slider(
                        value = clip.trimStartMs.toFloat(),
                        onValueChange = { viewModel.updateClipTrimInActive(clip.id, it.toLong(), clip.trimEndMs) },
                        valueRange = 0f..clip.durationMs.toFloat(),
                        colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan)
                    )
                }

                // Trim End Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Trim Out Point", fontSize = 11.sp, color = Color.Gray)
                        Text(formatMs(clip.trimEndMs), fontSize = 11.sp, color = CyberCyan)
                    }
                    Slider(
                        value = clip.trimEndMs.toFloat(),
                        onValueChange = { viewModel.updateClipTrimInActive(clip.id, clip.trimStartMs, it.toLong()) },
                        valueRange = 0f..clip.durationMs.toFloat(),
                        colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = CyberCyan)
                    )
                }

                // Playback speed rate selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Motion Speed", fontSize = 12.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val speeds = listOf(0.5f, 1.0f, 2.0f)
                        speeds.forEach { speed ->
                            val isSel = clip.speed == speed
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSel) CyberCyan else SurfaceTrack)
                                    .clickable {
                                        viewModel.updateClipAdjustmentsInActive(
                                            clip.id,
                                            clip.brightness,
                                            clip.contrast,
                                            clip.saturation,
                                            speed
                                        )
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Delete Clip Action
        Button(
            onClick = { viewModel.removeClipFromActive(clip.id) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D1414)),
            modifier = Modifier.fillMaxWidth().testTag("delete_clip_button")
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete Scene Clip", color = Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OverlayEditPanel(overlay: TextOverlay, viewModel: EditorViewModel, activeProject: VideoProject) {
    var text by remember(overlay.id) { mutableStateOf(overlay.text) }
    var fontScale by remember(overlay.id) { mutableStateOf(overlay.fontSizeSp) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                viewModel.updateSubtitleInActive(
                    overlay.id,
                    it,
                    overlay.startMs,
                    overlay.endMs,
                    overlay.colorHex,
                    overlay.fontSizeSp,
                    overlay.positionY
                )
            },
            label = { Text("Edit Overlay Text") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberPink, focusedLabelColor = CyberPink),
            modifier = Modifier.fillMaxWidth().testTag("edit_subtitle_input_field")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Start Time (s)", fontSize = 11.sp, color = Color.Gray)
                Slider(
                    value = overlay.startMs.toFloat(),
                    onValueChange = {
                        viewModel.updateSubtitleInActive(
                            overlay.id,
                            text,
                            it.toLong(),
                            overlay.endMs,
                            overlay.colorHex,
                            overlay.fontSizeSp,
                            overlay.positionY
                        )
                    },
                    valueRange = 0f..activeProject.totalDurationMs.toFloat()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("End Time (s)", fontSize = 11.sp, color = Color.Gray)
                Slider(
                    value = overlay.endMs.toFloat(),
                    onValueChange = {
                        viewModel.updateSubtitleInActive(
                            overlay.id,
                            text,
                            overlay.startMs,
                            it.toLong(),
                            overlay.colorHex,
                            overlay.fontSizeSp,
                            overlay.positionY
                        )
                    },
                    valueRange = 0f..activeProject.totalDurationMs.toFloat()
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Font Size: ${fontScale.toInt()}sp", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(90.dp))
            Slider(
                value = fontScale,
                onValueChange = {
                    fontScale = it
                    viewModel.updateSubtitleInActive(
                        overlay.id,
                        text,
                        overlay.startMs,
                        overlay.endMs,
                        overlay.colorHex,
                        it,
                        overlay.positionY
                    )
                },
                valueRange = 12f..36f,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.removeSubtitleFromActive(overlay.id) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D1414)),
            modifier = Modifier.fillMaxWidth().testTag("delete_subtitle_button")
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete Subtitle", color = Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AudioEditPanel(audio: AudioClip, viewModel: EditorViewModel, activeProject: VideoProject) {
    var volume by remember(audio.id) { mutableStateOf(audio.volume) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Track Source: ${audio.title}", fontWeight = FontWeight.Bold, color = CyberYellow)

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Volume Level", fontSize = 11.sp, color = Color.Gray)
                Text("${(volume * 100).toInt()}%", fontSize = 11.sp, color = CyberYellow)
            }
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    // Simple updates could trigger local volume modifiers
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(activeTrackColor = CyberYellow, thumbColor = CyberYellow)
            )
        }

        Column {
            Text("Timeline Start Sync Offset", fontSize = 11.sp, color = Color.Gray)
            Slider(
                value = audio.startOffsetMs.toFloat(),
                onValueChange = {
                    // Offset updates can reposition audio tracks
                },
                valueRange = 0f..activeProject.totalDurationMs.toFloat()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.removeAudioFromActive(audio.id) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D1414)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete Background Track", color = Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}

// --- CORE PLAYBACK VIEW LOGIC ---

fun getActiveClipAtPlayhead(project: VideoProject, playheadMs: Long): Pair<VideoClip, Long>? {
    var currentSum = 0L
    for (clip in project.clips) {
        val nextSum = currentSum + clip.activeDurationMs
        if (playheadMs in currentSum until nextSum) {
            val offsetInClip = (playheadMs - currentSum)
            return Pair(clip, offsetInClip)
        }
        currentSum = nextSum
    }
    // Safe fallback if playhead matches absolute end
    if (project.clips.isNotEmpty() && playheadMs >= project.totalDurationMs) {
        return Pair(project.clips.last(), project.clips.last().activeDurationMs)
    }
    return null
}

fun getFilterBrush(filterType: String): Brush? {
    return when (filterType) {
        "GRAYSCALE" -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.3f)))
        "SEPIA" -> Brush.verticalGradient(listOf(Color(0xFF8B4513).copy(alpha = 0.15f), Color(0xFFCD853F).copy(alpha = 0.1f)))
        "WARM" -> Brush.verticalGradient(listOf(Color(0xFFFF8C00).copy(alpha = 0.12f), Color(0xFFD2691E).copy(alpha = 0.08f)))
        "COOL" -> Brush.verticalGradient(listOf(Color(0xFF00BFFF).copy(alpha = 0.15f), Color(0xFF1E90FF).copy(alpha = 0.1f)))
        "CYBERPUNK" -> Brush.verticalGradient(listOf(Color(0xFFFF007F).copy(alpha = 0.15f), Color(0xFF00F0FF).copy(alpha = 0.12f)))
        "DRAMATIC" -> Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.45f)))
        else -> null
    }
}

@Composable
fun ScanningLineAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val lineOffset = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val y = size.height * lineOffset.value
        drawLine(
            color = CyberCyan.copy(alpha = 0.6f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx()
        )
    }
}

fun formatMs(ms: Long): String {
    val secs = ms / 1000
    val remMs = (ms % 1000) / 10
    return String.format("%02d.%02d", secs, remMs)
}
