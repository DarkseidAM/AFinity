package com.makd.afinity.player.exoplayer

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory

/**
 * RenderersFactory for the ExoPlayer backend that orders hardware (MediaCodec) vs.
 * software (FFmpeg/AV1/IAMF/MPEG-H extension) renderers according to the user's
 * [DecoderPriority].
 *
 * The extension renderers are auto-discovered by [DefaultRenderersFactory] (via the
 * bundled `lib-decoder-*` AARs) whenever the extension renderer mode is not OFF.
 */
@UnstableApi
class AfinityRenderersFactory(
    context: Context,
    priority: DecoderPriority,
) : DefaultRenderersFactory(context) {

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
