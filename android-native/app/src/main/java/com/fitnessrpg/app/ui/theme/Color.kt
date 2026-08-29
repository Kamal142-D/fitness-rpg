package com.fitnessrpg.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The app palette — the single source of truth for color (ported from the
 * original design tokens, PLAN.txt §7). Dark-first: rank colors are accents,
 * never full-screen floods.
 */
object Palette {
    // Neutral surface ramp from the supplied design-system reference.
    val Background = Color(0xFF0E0E0E)
    val Surface1 = Color(0xFF161616)
    val Surface2 = Color(0xFF1D1D1D)

    /** Highest neutral surface, for pressed and selected states. */
    val Surface3 = Color(0xFF252525)

    /** Mangaku-inspired glass, kept neutral so accent color stays purposeful. */
    val GlassBase = Color(0xD9161616)
    val GlassTint = Color(0x8A1D1D1D)
    val GlassEdge = Color(0x4D6A6A6A)

    // The reference lime ramp hue-shifted into blue at matching luminance.
    val PrimaryDark = Color(0xFF081114)
    val PrimaryMid = Color(0xFF2792B5)
    val Primary = Color(0xFF34C4F9)
    val PrimaryContainer = PrimaryDark

    // Purple remains reserved for semantic rank/analysis states.
    val AccentContainer = Color(0xFF221C2B)
    val Accent = Color(0xFFB580FF)

    val TextPrimary = Color(0xFFF7F7F7)
    val TextSecondary = Color(0xFFA0A0A0)

    /** Even quieter than secondary, for de-emphasized meta text. */
    val TextTertiary = Color(0xFF666666)

    val Success = Color(0xFF4ADE80)
    val Danger = Color(0xFFFF4D5A)
    val Warning = Color(0xFFF5BD4F)

    /** Opaque neutral strokes stay predictable across the four dark surfaces. */
    val Hairline = Color(0xFF252525)
    val HairlineStrong = Color(0xFF343434)
    val Scrim = Color(0xB3000000)
}
