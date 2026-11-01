package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.JarvisTab
import com.example.ui.screens.ArcCoreScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.DirectivesScreen
import com.example.ui.screens.ProtocolsSettingsScreen
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.util.ViewModelFactory
import com.example.ui.viewmodel.JarvisViewModel
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MANDATORY STEP 1: App Check wiring before setContent or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            JarvisTheme {
                val context = LocalContext.current
                val factory = remember { ViewModelFactory(context.applicationContext) }
                val viewModel: JarvisViewModel = viewModel(factory = factory)

                JarvisApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun JarvisApp(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val audioRms by viewModel.audioRms.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val directives by viewModel.directives.collectAsStateWithLifecycle()
    val callsign by viewModel.callsign.collectAsStateWithLifecycle()
    val autoSpeak by viewModel.autoSpeak.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val voicePitch by viewModel.voicePitch.collectAsStateWithLifecycle()
    val voiceRate by viewModel.voiceRate.collectAsStateWithLifecycle()
    val partialSpeech by viewModel.partialSpeechInput.collectAsStateWithLifecycle()
    val speechStatusMessage by viewModel.speechStatusMessage.collectAsStateWithLifecycle()

    // Audio recording permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening()
        }
    }

    val onVoiceClick = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.onVoiceAction(hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Back handling: If on secondary tab, return to Arc Core
    BackHandler(enabled = currentTab != JarvisTab.ARC_CORE) {
        viewModel.selectTab(JarvisTab.ARC_CORE)
    }

    Scaffold(
        containerColor = CyberBlack,
        bottomBar = {
            JarvisBottomNav(
                currentTab = currentTab,
                onSelectTab = { tab -> viewModel.selectTab(tab) }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CyberBlack,
                            CyberDark,
                            Color(0xFF040B16)
                        )
                    )
                )
        ) {
            when (currentTab) {
                JarvisTab.ARC_CORE -> ArcCoreScreen(
                    viewModel = viewModel,
                    messages = messages,
                    voiceState = voiceState,
                    audioRms = audioRms,
                    callsign = callsign,
                    isMuted = isMuted,
                    partialSpeech = partialSpeech,
                    speechStatusMessage = speechStatusMessage,
                    onVoiceClick = onVoiceClick,
                    onDismissSpeechStatus = { viewModel.clearSpeechStatus() }
                )
                JarvisTab.DIAGNOSTICS -> DiagnosticsScreen(
                    viewModel = viewModel,
                    diagnostics = diagnostics
                )
                JarvisTab.DIRECTIVES -> DirectivesScreen(
                    viewModel = viewModel,
                    directives = directives
                )
                JarvisTab.PROTOCOLS -> ProtocolsSettingsScreen(
                    viewModel = viewModel,
                    callsign = callsign,
                    autoSpeak = autoSpeak,
                    voicePitch = voicePitch,
                    voiceRate = voiceRate,
                    onTestVoiceRecognition = onVoiceClick
                )
            }
        }
    }
}

@Composable
fun JarvisBottomNav(
    currentTab: JarvisTab,
    onSelectTab: (JarvisTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurface.copy(alpha = 0.95f))
            .border(1.dp, CyberBorder.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            JarvisTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val icon = when (tab) {
                    JarvisTab.ARC_CORE -> Icons.Default.Bolt
                    JarvisTab.DIAGNOSTICS -> Icons.Default.Speed
                    JarvisTab.DIRECTIVES -> Icons.Default.FormatListBulleted
                    JarvisTab.PROTOCOLS -> Icons.Default.Tune
                }

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelectTab(tab) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) ArcCyan else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = tab.title.uppercase(),
                            color = if (isSelected) ArcCyan else TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = ArcCyan.copy(alpha = 0.15f),
                        selectedIconColor = ArcCyan,
                        unselectedIconColor = TextMuted,
                        selectedTextColor = ArcCyan,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                )
            }
        }
    }
}
