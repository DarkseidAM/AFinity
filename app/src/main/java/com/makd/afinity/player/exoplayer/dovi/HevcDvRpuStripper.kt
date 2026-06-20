package com.makd.afinity.player.exoplayer.dovi

import androidx.media3.common.util.UnstableApi
import java.io.ByteArrayOutputStream

/**
 * Strips Dolby Vision RPU NAL units (HEVC NAL type 62) from an HEVC bitstream
 * on-the-fly, leaving the HDR10/HDR10+ base layer intact.
 */
@UnstableApi
internal object HevcDvRpuStripper {

    private const val NAL_TYPE_DV_RPU = 62
    private const val NAL_TYPE_DV_EL = 63

    /**
     * Rewrites a length-delimited (MP4/fMP4) sample, removing any NAL unit
     * whose type is 62 (DV RPU). Returns the rewritten bytes, or null if
     * nothing was stripped (caller should use the original).
     */
    fun stripRpuLengthDelimited(
        sample: ByteArray,
        sampleLen: Int,
        nalLengthFieldLength: Int
    ): ByteArray? {
        if (sampleLen < nalLengthFieldLength) return null
        // Lazy allocation: only allocate/copy once we actually hit a NAL to drop. If the
        // sample has no RPU/EL NALs (the common case) we return null with zero allocation.
        var out: ByteArrayOutputStream? = null
        var pos = 0
        while (pos + nalLengthFieldLength <= sampleLen) {
            var nalSize = 0
            for (i in 0 until nalLengthFieldLength) {
                nalSize = (nalSize shl 8) or (sample[pos + i].toInt() and 0xFF)
            }
            val nalStart = pos + nalLengthFieldLength
            if (nalSize <= 0 || nalSize > sampleLen - nalStart) return null
            val nalType = (sample[nalStart].toInt() ushr 1) and 0x3F
            if (nalType == NAL_TYPE_DV_RPU || nalType == NAL_TYPE_DV_EL) {
                if (out == null) {
                    // First NAL to drop: backfill everything kept so far verbatim.
                    out = ByteArrayOutputStream(sampleLen)
                    out.write(sample, 0, pos)
                }
                // Drop this NAL entirely — don't write length prefix or payload.
            } else {
                out?.let {
                    for (i in nalLengthFieldLength - 1 downTo 0) {
                        it.write((nalSize ushr (i * 8)) and 0xFF)
                    }
                    it.write(sample, nalStart, nalSize)
                }
            }
            pos = nalStart + nalSize
        }
        return out?.toByteArray()
    }

    /**
     * Rewrites an Annex-B (TS/raw HEVC) sample, removing DV RPU NAL units.
     * Returns the rewritten bytes, or null if nothing was stripped.
     */
    fun stripRpuAnnexB(sample: ByteArray, sampleLen: Int): ByteArray? {
        var out: ByteArrayOutputStream? = null
        var scan = 0
        while (scan < sampleLen) {
            val startCode = findStartCode(sample, scan, sampleLen)
            if (startCode < 0) {
                out?.write(sample, scan, sampleLen - scan)
                break
            }
            val scLen = startCodeLength(sample, startCode, sampleLen)
            val nalBegin = startCode + scLen
            val nextStartCode = findStartCode(sample, nalBegin + 2, sampleLen)
            val nalEnd = if (nextStartCode < 0) sampleLen else nextStartCode

            val drop = nalBegin < nalEnd &&
                (((sample[nalBegin].toInt() ushr 1) and 0x3F).let {
                    it == NAL_TYPE_DV_RPU || it == NAL_TYPE_DV_EL
                })

            if (drop) {
                if (out == null) {
                    // First NAL to drop: backfill everything kept so far verbatim.
                    out = ByteArrayOutputStream(sampleLen)
                    out.write(sample, 0, startCode)
                } else if (startCode > scan) {
                    out.write(sample, scan, startCode - scan)
                }
                // Drop start code + NAL payload entirely.
            } else {
                out?.let {
                    if (startCode > scan) it.write(sample, scan, startCode - scan)
                    if (nalBegin < nalEnd) it.write(sample, startCode, nalEnd - startCode)
                }
            }
            scan = nalEnd
        }
        return out?.toByteArray()
    }

    private fun findStartCode(data: ByteArray, from: Int, limit: Int): Int {
        var i = from
        while (i + 2 < limit) {
            if (data[i].toInt() == 0 && data[i + 1].toInt() == 0) {
                if (data[i + 2].toInt() == 1) return i
                if (i + 3 < limit && data[i + 2].toInt() == 0 && data[i + 3].toInt() == 1) return i
            }
            i++
        }
        return -1
    }

    private fun startCodeLength(data: ByteArray, offset: Int, limit: Int): Int {
        return if (offset + 3 < limit &&
            data[offset].toInt() == 0 &&
            data[offset + 1].toInt() == 0 &&
            data[offset + 2].toInt() == 0 &&
            data[offset + 3].toInt() == 1
        ) 4 else 3
    }
}
