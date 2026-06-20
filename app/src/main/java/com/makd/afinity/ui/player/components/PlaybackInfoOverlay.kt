package com.makd.afinity.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makd.afinity.data.models.player.VideoMetadata

/**
 * Compact "stats for nerds" panel showing the active backend, video/audio codecs,
 * resolution, HDR/Dolby Vision profile, channels and decoder. Rendered as an overlay
 * while [VideoMetadata] is available and the toggle is on.
 */
@Composable
fun PlaybackInfoOverlay(
    info: VideoMetadata,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .widthIn(min = 220.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Playback info",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        InfoRow("Backend", info.backend)
        InfoRow("Video", "${info.videoCodec} · ${info.resolution.label}")
        InfoRow("HDR", info.displayProfile.label)
        InfoRow("Audio", buildString {
            append(info.audioCodec)
            append(" · ")
            append(info.audioChannels)
            if (info.isAtmos) append(" · Atmos")
        })
        InfoRow("Decoder", info.decoder)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}
