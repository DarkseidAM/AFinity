package com.makd.afinity.player.exoplayer

import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AfinityRenderersFactoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun auto_usesExtensionRendererModeOn() {
        val factory = AfinityRenderersFactory(context, DecoderPriority.AUTO)
        assertEquals(
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON,
            factory.extensionRendererModeForTest(),
        )
    }

    @Test
    fun preferSoftware_usesExtensionRendererModePrefer() {
        val factory = AfinityRenderersFactory(context, DecoderPriority.PREFER_SOFTWARE)
        assertEquals(
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER,
            factory.extensionRendererModeForTest(),
        )
    }

    @Test
    fun preferHardware_disablesExtensions() {
        val factory = AfinityRenderersFactory(context, DecoderPriority.PREFER_HARDWARE)
        assertEquals(
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF,
            factory.extensionRendererModeForTest(),
        )
    }
}
