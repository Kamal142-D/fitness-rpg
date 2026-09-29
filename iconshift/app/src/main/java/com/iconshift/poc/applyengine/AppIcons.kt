package com.iconshift.poc.applyengine

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.util.DisplayMetrics
import androidx.core.graphics.drawable.toBitmap
import com.iconshift.core.applyengine.AppTarget
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Loads a target's launcher icon straight from its resources, bypassing PackageManager's icon
 * cache, so enabled/disabled overlays are reflected immediately.
 */
object AppIcons {

    fun iconResId(context: Context, target: AppTarget): Int {
        val pm = context.packageManager
        val activityIcon = target.activityName?.let {
            runCatching { pm.getActivityInfo(ComponentName(target.packageName, it), 0).icon }.getOrDefault(0)
        } ?: 0
        if (activityIcon != 0) return activityIcon
        return runCatching { pm.getApplicationInfo(target.packageName, 0).icon }.getOrDefault(0)
    }

    /** "pkg:type/name" for the launcher icon and, when present, its `_round` sibling. */
    fun iconResourceNames(context: Context, target: AppTarget): List<String> {
        val pm = context.packageManager
        val res = pm.getResourcesForApplication(pm.getApplicationInfo(target.packageName, 0))
        val ids = linkedSetOf<Int>()
        iconResId(context, target).takeIf { it != 0 }?.let(ids::add)
        runCatching { pm.getApplicationInfo(target.packageName, 0).icon }.getOrNull()?.takeIf { it != 0 }?.let(ids::add)
        val names = ids.mapNotNull { runCatching { res.getResourceName(it) }.getOrNull() }.toMutableList()
        for (name in names.toList()) {
            val type = name.substringAfter(':').substringBefore('/')
            val entry = name.substringAfter('/')
            @Suppress("DiscouragedApi")
            val roundId = res.getIdentifier("${entry}_round", type, target.packageName)
            if (roundId != 0) runCatching { res.getResourceName(roundId) }.getOrNull()?.let { if (it !in names) names += it }
        }
        return names
    }

    fun render(context: Context, target: AppTarget, sizePx: Int = 192): Bitmap? = runCatching {
        val pm = context.packageManager
        val res = pm.getResourcesForApplication(pm.getApplicationInfo(target.packageName, 0))
        val id = iconResId(context, target).takeIf { it != 0 } ?: return null
        val drawable = res.getDrawableForDensity(id, DisplayMetrics.DENSITY_XXXHIGH, null) ?: return null
        drawable.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }.getOrNull()

    fun hash(bitmap: Bitmap): String {
        val buf = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buf)
        return MessageDigest.getInstance("SHA-256").digest(buf.array()).joinToString("") { "%02x".format(it) }
    }

    fun renderHash(context: Context, target: AppTarget): String? = render(context, target, 96)?.let(::hash)
}
