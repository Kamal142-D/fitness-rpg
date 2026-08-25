package com.fitnessrpg.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The app palette — the single source of truth for color (ported from the
 * original design tokens, PLAN.txt §7). Dark-first: rank colors are accents,
 * never full-screen floods.
 */
object Palette {
    val Background = Color(0xFF070A0F)
    val Surface1 = Color(0xFF0E141D)
    val Surface2 = Color(0xFF111923)

    /** A hair above Surface2, for pressed/selected states. */
    val Surface3 = Color(0xFF18202C)

    val Primary = Color(0xFF3EA6FF) // electric blue
    val Accent = Color(0xFF7B61FF) // blue-violet

    val TextPrimary = Color(0xFFF4F7FA)
    val TextSecondary = Color(0xFF8B99AA)

    /** Even quieter than secondary, for de-emphasized meta text. */
    val TextTertiary = Color(0xFF5B6675)

    val Success = Color(0xFF4ADE80)
    val Danger = Color(0xFFFF4D5A)

    /** Self-colored hairline: a light stroke at low opacity for tonal edges. */
    val Hairline = Color(0x14F4F7FA) // ~0.08 alpha
    val HairlineStrong = Color(0x24F4F7FA) // ~0.14 alpha
}
