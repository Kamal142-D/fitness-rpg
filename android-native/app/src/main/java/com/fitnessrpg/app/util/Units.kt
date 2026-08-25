package com.fitnessrpg.app.util

import kotlin.math.roundToLong

/**
 * Unit conversion. Canonical storage is KILOGRAMS; these helpers support a
 * future imperial display option. Pure.
 */
private const val LB_PER_KG = 2.2046226218

fun kgToLb(kg: Double): Double = kg * LB_PER_KG

fun lbToKg(lb: Double): Double = lb / LB_PER_KG

/** Round to [decimals] places (default 1). */
fun roundTo(value: Double, decimals: Int = 1): Double {
    var f = 1.0
    repeat(decimals) { f *= 10 }
    return (value * f).roundToLong() / f
}

/** Round to 2 decimal places (matches the JS `Math.round(n*100)/100`). */
fun round2(n: Double): Double = (n * 100).roundToLong() / 100.0
