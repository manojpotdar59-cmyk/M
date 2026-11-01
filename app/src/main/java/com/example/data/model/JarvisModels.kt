package com.example.data.model

import java.util.UUID

enum class MessageRole {
    USER,
    JARVIS,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val isDirective: Boolean = false
)

enum class DirectiveCategory(val label: String) {
    TACTICAL("Tactical"),
    ROUTINE("Routine"),
    RESEARCH("Research"),
    DEFENSE("Security")
}

data class JarvisDirective(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val detail: String,
    val category: DirectiveCategory = DirectiveCategory.TACTICAL,
    val isCompleted: Boolean = false,
    val priority: String = "HIGH",
    val createdAt: Long = System.currentTimeMillis()
)

data class SystemDiagnostic(
    val batteryPct: Int = 85,
    val isCharging: Boolean = false,
    val memoryFreeMb: Long = 1840,
    val memoryTotalMb: Long = 4096,
    val uptimeFormatted: String = "04:12:30",
    val networkStatus: String = "Online (5G / StarkNet)",
    val arcPowerGw: Float = 3.42f,
    val arcTempCelsius: Float = 98.4f,
    val coreIntegrityPct: Int = 99
)

enum class JarvisVoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

enum class JarvisTab(val title: String) {
    ARC_CORE("Arc Core"),
    DIAGNOSTICS("Diagnostics"),
    DIRECTIVES("Directives"),
    PROTOCOLS("Protocols")
}

data class QuickCommand(
    val id: String,
    val label: String,
    val prompt: String,
    val tag: String
)
