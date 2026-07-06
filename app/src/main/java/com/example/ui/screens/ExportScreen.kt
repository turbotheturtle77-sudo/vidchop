package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoProject
import com.example.ui.ExportState
import com.example.ui.EditorViewModel
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exportState by viewModel.exportState.collectAsState()
    val activeProjectState by viewModel.activeProject.collectAsState()
    val activeProject = activeProjectState ?: return

    var selectedResolution by remember { mutableStateOf("1080p Full HD (1920x1080)") }
    var selectedFps by remember { mutableStateOf(30) }
    var selectedFormat by remember { mutableStateOf("MP4") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Master Render", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepSpaceObsidian)
                .padding(16.dp)
        ) {
            when (val state = exportState) {
                is ExportState.Exporting -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "RENDERING HARDWARE PIPELINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // --- LIVE ENGINE FRAME-PREVIEW ANIMATION ---
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(android.graphics.Color.parseColor(state.currentFrameColorHex)))
                                .border(2.dp, CyberCyan, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Filter overlay
                            val filterBrush = getFilterBrush(state.currentFrameFilter)
                            if (filterBrush != null) {
                                Box(modifier = Modifier.fillMaxSize().background(filterBrush))
                            }

                            // Spinning process gear
                            val infiniteTransition = rememberInfiniteTransition(label = "gear")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotation"
                            )
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Rendering",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer(rotationZ = rotation)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "FX: ${state.currentFrameFilter}",
                                    color = CyberCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Percentage & Slider Progress
                        Text(
                            text = "${(state.progress * 100).toInt()}% Completed",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { state.progress },
                            color = CyberCyan,
                            trackColor = SurfaceTrack,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Log outputs
                        Text(
                            text = state.statusText,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            lineHeight = 18.sp
                        )
                    }
                }

                is ExportState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(CyberCyan.copy(alpha = 0.2f), CircleShape)
                                    .border(2.dp, CyberCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Render Completed successfully!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Your master stream was saved inside the sandbox library gallery.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Playable Review Player Box
                        item {
                            var isReviewPlaying by remember { mutableStateOf(false) }
                            val mockPlayhead = remember { Animatable(0f) }
                            val totalDur = state.durationMs.toFloat()

                            LaunchedEffect(isReviewPlaying) {
                                if (isReviewPlaying) {
                                    mockPlayhead.animateTo(
                                        targetValue = totalDur,
                                        animationSpec = tween(
                                            durationMillis = state.durationMs.toInt(),
                                            easing = LinearEasing
                                        )
                                    )
                                    isReviewPlaying = false
                                    mockPlayhead.snapTo(0f)
                                } else {
                                    mockPlayhead.stop()
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { isReviewPlaying = !isReviewPlaying },
                                contentAlignment = Alignment.Center
                            ) {
                                // Dynamic background colors based on playhead animation
                                val activeClipPair = getActiveClipAtPlayhead(activeProject, mockPlayhead.value.toLong())
                                if (activeClipPair != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(android.graphics.Color.parseColor(activeClipPair.first.colorHex)))
                                    ) {
                                        val filterBrush = getFilterBrush(activeClipPair.first.filterType)
                                        if (filterBrush != null) {
                                            Box(modifier = Modifier.fillMaxSize().background(filterBrush))
                                        }
                                    }
                                }

                                // Interactive Overlay text
                                val subText = activeProject.textOverlays.firstOrNull {
                                    mockPlayhead.value.toLong() in it.startMs..it.endMs
                                }
                                if (subText != null) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    ) {
                                        Text(subText.text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (!isReviewPlaying) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text("Play Back Final Master File", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Tiny playback progress bar
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth(if (totalDur > 0) mockPlayhead.value / totalDur else 0f)
                                        .height(4.dp)
                                        .background(CyberCyan)
                                )
                            }
                        }

                        // Meta details List
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MetaRow(label = "Filename", value = state.savedFileName)
                                    MetaRow(label = "Target Format", value = selectedFormat)
                                    MetaRow(label = "Resolution", value = state.resolution)
                                    MetaRow(label = "Length", value = formatDuration(state.durationMs))
                                    MetaRow(label = "Total File Size", value = String.format(Locale.getDefault(), "%.1f MB", (state.durationMs / 1000f) * 2.3f))
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    viewModel.clearExportState()
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Done & Back to Workspace", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                else -> {
                    // Export Setup panel
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Select your hardware decoding and stream container parameters below.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )

                        // Resolution Select
                        Column {
                            Text("TARGET OUTPUT RESOLUTION", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            val resolutions = listOf(
                                "1080p Full HD (1920x1080)",
                                "4K Ultra HD (3844x2160)",
                                "720p HD Ready (1280x720)"
                            )
                            resolutions.forEach { res ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedResolution = res }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(res, color = if (selectedResolution == res) CyberCyan else Color.White)
                                    RadioButton(
                                        selected = selectedResolution == res,
                                        onClick = { selectedResolution = res },
                                        colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                                    )
                                }
                            }
                        }

                        // FPS Selector
                        Column {
                            Text("TARGET ENCODING RATE (FPS)", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val rates = listOf(24, 30, 60)
                                rates.forEach { fps ->
                                    val isSel = selectedFps == fps
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) CyberCyan.copy(alpha = 0.15f) else SurfaceSlate)
                                            .border(width = 1.dp, color = if (isSel) CyberCyan else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                            .clickable { selectedFps = fps }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${fps} FPS", fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                                    }
                                }
                            }
                        }

                        // Format selection
                        Column {
                            Text("STREAM ENVELOPE CONTAINER", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val formats = listOf("MP4", "MKV", "MOV")
                                formats.forEach { fmt ->
                                    val isSel = selectedFormat == fmt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) CyberCyan.copy(alpha = 0.15f) else SurfaceSlate)
                                            .border(width = 1.dp, color = if (isSel) CyberCyan else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                            .clickable { selectedFormat = fmt }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(fmt, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { viewModel.startExportSimulation(selectedResolution, selectedFps, selectedFormat) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_render_button")
                        ) {
                            Icon(Icons.Default.MovieCreation, contentDescription = "Render", tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Master Render", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
