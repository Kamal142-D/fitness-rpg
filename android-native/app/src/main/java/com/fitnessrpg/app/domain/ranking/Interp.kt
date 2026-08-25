package com.fitnessrpg.app.domain.ranking

/**
 * Piecewise-linear interpolation over sorted anchor points, clamped at the ends.
 * Maps a raw metric (e.g. a bodyweight-strength ratio) onto the 0..100 scale.
 */
data class Anchor(val x: Double, val y: Double)

/** Interpolate y for a given x across [anchors] sorted ascending by x. */
fun interpolate(anchors: List<Anchor>, x: Double): Double {
    if (anchors.isEmpty()) return 0.0
    if (x <= anchors.first().x) return anchors.first().y
    val last = anchors.last()
    if (x >= last.x) return last.y
    for (i in 0 until anchors.size - 1) {
        val a = anchors[i]
        val b = anchors[i + 1]
        if (x >= a.x && x <= b.x) {
            val t = (x - a.x) / (b.x - a.x)
            return a.y + t * (b.y - a.y)
        }
    }
    return last.y
}
