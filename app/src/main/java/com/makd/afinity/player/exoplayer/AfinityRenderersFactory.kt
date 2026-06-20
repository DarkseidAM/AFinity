package com.makd.afinity.player.exoplayer

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.media3.common.util.UnstableApi
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

/**
 * RenderersFactory for the ExoPlayer backend that orders hardware (MediaCodec) vs.
 * software (FFmpeg) renderers according to the user's [DecoderPriority].
 *
 * Extends NextLib's [NextRenderersFactory], which adds FFmpeg software **audio** and
 * **video** decoders (H.264/HEVC/VP8/VP9 + AC3/E-AC3/DTS/TrueHD/...) on top of the
 * standard media3 renderers. The extension renderers are selected per the extension
 * renderer mode derived from [priority].
 */
@UnstableApi
class AfinityRenderersFactory(
    context: Context,
    priority: DecoderPriority,
) : NextRenderersFactory(context) {

    private val rendererMode: Int = when (priority) {
        DecoderPriority.AUTO -> EXTENSION_RENDERER_MODE_ON
        DecoderPriority.PREFER_SOFTWARE -> EXTENSION_RENDERER_MODE_PREFER
        DecoderPriority.PREFER_HARDWARE -> EXTENSION_RENDERER_MODE_OFF
    }

    init {
        setExtensionRendererMode(rendererMode)
        // Let ExoPlayer try the next decoder (e.g. the FFmpeg software renderer) when a
        // hardware decoder fails to initialize. Combined with AUTO this gives HW->SW fallback.
        setEnableDecoderFallback(true)
    }

    @VisibleForTesting
    fun extensionRendererModeForTest(): Int = rendererMode
}
