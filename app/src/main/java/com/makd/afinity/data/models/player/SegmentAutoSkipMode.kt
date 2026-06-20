package com.makd.afinity.data.models.player

/**
 * Controls automatic skipping of media segments (intro/outro/recap/etc.).
 *
 * - [OFF]: never auto-skip; the user taps the skip button.
 * - [PIP_ONLY]: auto-skip only while in picture-in-picture (no controls visible).
 * - [ALWAYS]: always auto-skip when a skippable segment is entered.
 */
enum class SegmentAutoSkipMode(val value: String) {
    OFF("off"),
    PIP_ONLY("pip_only"),
    ALWAYS("always");

    companion object {
        val default = OFF

        fun fromValue(value: String): SegmentAutoSkipMode =
            entries.firstOrNull { it.value == value } ?: default
    }

    fun getDisplayName(): String =
        when (this) {
            OFF -> "Off"
            PIP_ONLY -> "Picture-in-picture only"
            ALWAYS -> "Always"
        }
}
