package com.makd.afinity.player.exoplayer

/**
 * Controls how the ExoPlayer backend chooses between hardware (MediaCodec) and
 * software (FFmpeg extension) decoders.
 *
 * - [AUTO]: hardware first, fall back to software when the hardware decoder
 *   cannot handle the stream (`EXTENSION_RENDERER_MODE_ON`).
 * - [PREFER_SOFTWARE]: software decoders take priority over hardware
 *   (`EXTENSION_RENDERER_MODE_PREFER`).
 * - [PREFER_HARDWARE]: hardware only, software extensions disabled
 *   (`EXTENSION_RENDERER_MODE_OFF`).
 */
enum class DecoderPriority(val value: String) {
    AUTO("auto"),
    PREFER_SOFTWARE("prefer_software"),
    PREFER_HARDWARE("prefer_hardware");

    companion object {
        val default = AUTO

        fun fromValue(value: String): DecoderPriority =
            entries.firstOrNull { it.value == value } ?: default
    }

    fun getDisplayName(): String =
        when (this) {
            AUTO -> "Auto (hardware, then software)"
            PREFER_SOFTWARE -> "Prefer software"
            PREFER_HARDWARE -> "Force hardware"
        }
}
