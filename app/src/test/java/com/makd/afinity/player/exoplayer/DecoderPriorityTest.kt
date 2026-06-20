package com.makd.afinity.player.exoplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class DecoderPriorityTest {

    @Test
    fun roundTrips_byValue() {
        DecoderPriority.entries.forEach { priority ->
            assertEquals(priority, DecoderPriority.fromValue(priority.value))
        }
    }

    @Test
    fun unknown_fallsBackToDefault() {
        assertEquals(DecoderPriority.AUTO, DecoderPriority.fromValue("nonsense"))
        assertEquals(DecoderPriority.AUTO, DecoderPriority.default)
    }

    @Test
    fun values_areStableStrings() {
        // These strings are persisted in DataStore — they must not change casually.
        assertEquals("auto", DecoderPriority.AUTO.value)
        assertEquals("prefer_software", DecoderPriority.PREFER_SOFTWARE.value)
        assertEquals("prefer_hardware", DecoderPriority.PREFER_HARDWARE.value)
    }
}
