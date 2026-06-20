package com.makd.afinity.data.models.player

import com.makd.afinity.data.models.media.AfinityMediaStream
import org.jellyfin.sdk.model.api.MediaStreamType

/** Resolution bucket derived from the video stream's pixel dimensions. */
enum class Resolution(val label: String) {
    SD("SD"),
    HD("HD"),
    FULL_HD("1080p"),
    UHD_4K("4K"),
    UHD_8K("8K"),
    UNKNOWN("—");

    companion object {
        fun from(width: Int?, height: Int?): Resolution {
            val w = width ?: 0
            val h = height ?: 0
            val longEdge = maxOf(w, h)
            return when {
                longEdge <= 0 -> UNKNOWN
                longEdge >= 7680 -> UHD_8K
                longEdge >= 3840 -> UHD_4K
                longEdge >= 1920 -> FULL_HD
                longEdge >= 1280 -> HD
                else -> SD
            }
        }
    }
}

/** HDR / dynamic-range profile of the video stream. */
enum class DisplayProfile(val label: String) {
    SDR("SDR"),
    HDR10("HDR10"),
    HDR10_PLUS("HDR10+"),
    DOLBY_VISION("Dolby Vision"),
    HLG("HLG"),
    UNKNOWN("—");

    companion object {
        /** Maps from the SDK [videoRangeType] name + a Dolby Vision title hint. */
        fun from(videoRangeTypeName: String?, doViTitle: String?): DisplayProfile {
            if (!doViTitle.isNullOrBlank()) return DOLBY_VISION
            val name = videoRangeTypeName?.uppercase().orEmpty()
            return when {
                name.contains("DOVI") || name.contains("DOLBY") -> DOLBY_VISION
                name.contains("HDR10PLUS") || name.contains("HDR10_PLUS") -> HDR10_PLUS
                name.contains("HDR10") -> HDR10
                name.contains("HLG") -> HLG
                name.contains("SDR") -> SDR
                name.isEmpty() || name == "UNKNOWN" -> UNKNOWN
                else -> SDR
            }
        }
    }
}

/**
 * Human-readable diagnostics for the current playback, shown in the player's
 * stats overlay. Built from the active source's media streams plus runtime
 * backend/decoder info.
 */
data class VideoMetadata(
    val backend: String,
    val videoCodec: String,
    val resolution: Resolution,
    val displayProfile: DisplayProfile,
    val audioCodec: String,
    val audioChannels: String,
    val isAtmos: Boolean,
    val decoder: String,
) {
    companion object {
        fun from(
            streams: List<AfinityMediaStream>,
            backend: String,
            decoder: String,
        ): VideoMetadata {
            val video = streams.firstOrNull { it.type == MediaStreamType.VIDEO }
            val audio =
                streams.firstOrNull { it.type == MediaStreamType.AUDIO && it.isDefault }
                    ?: streams.firstOrNull { it.type == MediaStreamType.AUDIO }

            val channels =
                audio?.channelLayout
                    ?: audio?.channels?.let { ch ->
                        when (ch) {
                            1 -> "Mono"
                            2 -> "Stereo"
                            6 -> "5.1"
                            8 -> "7.1"
                            else -> "$ch ch"
                        }
                    }
                    ?: "—"

            val atmos =
                (audio?.displayTitle.orEmpty() + " " + audio?.codec.orEmpty())
                    .contains("atmos", ignoreCase = true)

            return VideoMetadata(
                backend = backend,
                videoCodec = video?.codec?.uppercase().orEmpty().ifEmpty { "—" },
                resolution = Resolution.from(video?.width, video?.height),
                displayProfile = DisplayProfile.from(
                    video?.videoRangeType?.toString(),
                    video?.videoDoViTitle,
                ),
                audioCodec = audio?.codec?.uppercase().orEmpty().ifEmpty { "—" },
                audioChannels = channels,
                isAtmos = atmos,
                decoder = decoder,
            )
        }
    }
}
