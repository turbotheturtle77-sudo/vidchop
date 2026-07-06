package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.EditorViewModel
import com.example.ui.screens.AiScriptScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.ProjectListScreen
import com.example.ui.screens.TimelineEditorScreen
import com.example.ui.theme.MyApplicationTheme

enum class EditorScreen {
    PROJECT_LIST,
    TIMELINE_EDITOR,
    AI_SCRIPT,
    EXPORT
}

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(EditorScreen.PROJECT_LIST) }

                // Safe system back-press routing
                BackHandler(enabled = currentScreen != EditorScreen.PROJECT_LIST) {
                    currentScreen = when (currentScreen) {
                        EditorScreen.TIMELINE_EDITOR -> EditorScreen.PROJECT_LIST
                        EditorScreen.AI_SCRIPT -> EditorScreen.PROJECT_LIST
                        EditorScreen.EXPORT -> EditorScreen.TIMELINE_EDITOR
                        else -> EditorScreen.PROJECT_LIST
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Crossfade(
                        targetState = currentScreen,
                        label = "screen_routing",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) { screen ->
                        when (screen) {
                            EditorScreen.PROJECT_LIST -> {
                                ProjectListScreen(
                                    viewModel = viewModel,
                                    onProjectSelected = {
                                        currentScreen = EditorScreen.TIMELINE_EDITOR
                                    },
                                    onOpenAiGenerate = {
                                        currentScreen = EditorScreen.AI_SCRIPT
                                    }
                                )
                            }
                            EditorScreen.TIMELINE_EDITOR -> {
                                TimelineEditorScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        currentScreen = EditorScreen.PROJECT_LIST
                                    },
                                    onOpenExport = {
                                        currentScreen = EditorScreen.EXPORT
                                    }
                                )
                            }
                            EditorScreen.AI_SCRIPT -> {
                                AiScriptScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        currentScreen = EditorScreen.PROJECT_LIST
                                    },
                                    onSuccess = {
                                        currentScreen = EditorScreen.TIMELINE_EDITOR
                                    }
                                )
                            }
                            EditorScreen.EXPORT -> {
                                ExportScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        currentScreen = EditorScreen.TIMELINE_EDITOR
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
