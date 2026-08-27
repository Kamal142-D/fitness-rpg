package com.fitnessrpg.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The app palette — the single source of truth for color (ported from the
 * original design tokens, PLAN.txt §7). Dark-first: rank colors are accents,
 * never full-screen floods.
 */
object Palette {
    val Background = Color(0xFF080B10)
    val Surface1 = Color(0xFF10161F)
    val Surface2 = Color(0xFF151D28)

    /** A hair above Surface2, for pressed/selected states. */
    val Surface3 = Color(0xFF1C2734)

    val Primary = Color(0xFF58D6FF) // system cyan; the sole action accent
    val PrimaryContainer = Color(0xFF123748)
    val Accent = Color(0xFF8B7CFF) // semantic rank accent only

    val TextPrimary = Color(0xFFF2F6FA)
    val TextSecondary = Color(0xFFA7B3C1)

    /** Even quieter than secondary, for de-emphasized meta text. */
    val TextTertiary = Color(0xFF697687)

    val Success = Color(0xFF4ADE80)
    val Danger = Color(0xFFFF4D5A)
    val Warning = Color(0xFFF5BD4F)

    /** Self-colored hairline: a light stroke at low opacity for tonal edges. */
    val Hairline = Color(0x12F4F7FA)
    val HairlineStrong = Color(0x26F4F7FA)
    val Scrim = Color(0xB3000000)
}
