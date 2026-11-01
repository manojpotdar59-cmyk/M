package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.JarvisPreferences
import com.example.data.model.DirectiveCategory
import com.example.data.model.JarvisDirective
import com.example.data.model.SystemDiagnostic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var preferences: JarvisPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferences = JarvisPreferences(context)
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Jarvis", appName)
    }

    @Test
    fun `jarvis preferences manages callsign and voice settings`() {
        preferences.callsign = "Tony"
        assertEquals("Tony", preferences.callsign)

        preferences.autoSpeak = false
        assertFalse(preferences.autoSpeak)
        preferences.autoSpeak = true
        assertTrue(preferences.autoSpeak)

        preferences.voicePitch = 1.1f
        assertEquals(1.1f, preferences.voicePitch, 0.01f)

        preferences.voiceRate = 0.95f
        assertEquals(0.95f, preferences.voiceRate, 0.01f)
    }

    @Test
    fun `jarvis preferences persists mission directives`() {
        val testDirective = JarvisDirective(
            title = "Quantum Field Containment",
            detail = "Verify Stark arc reactor secondary harmonic wave.",
            category = DirectiveCategory.TACTICAL,
            priority = "CRITICAL"
        )

        preferences.saveDirectives(listOf(testDirective))
        val loaded = preferences.getDirectives()

        assertNotNull(loaded)
        assertTrue(loaded.isNotEmpty())
        assertEquals("Quantum Field Containment", loaded[0].title)
        assertEquals(DirectiveCategory.TACTICAL, loaded[0].category)
        assertEquals("CRITICAL", loaded[0].priority)
    }

    @Test
    fun `system diagnostic defaults are nominal`() {
        val diagnostic = SystemDiagnostic()
        assertTrue(diagnostic.batteryPct in 0..100)
        assertTrue(diagnostic.arcPowerGw > 0f)
        assertTrue(diagnostic.arcTempCelsius > 0f)
        assertEquals(99, diagnostic.coreIntegrityPct)
    }
}
