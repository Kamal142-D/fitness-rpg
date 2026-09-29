package com.iconshift.poc.poc

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.graphics.scale
import com.iconshift.core.applyengine.IconSource
import java.io.ByteArrayOutputStream

/** Test icons for the POC. The generated one is our own art, so no third-party assets are bundled. */
object TestIcons {
    const val SIZE = 432

    /**
     * A full-bleed square (no transparent margin) in loud colours with an inner ring near the edge,
     * so masking, cropping and any added white frame are easy to spot on the home screen.
     */
    fun generated(): IconSource {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), Color.rgb(233, 30, 140), Color.rgb(255, 145, 0), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), bg)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 14f
            color = Color.rgb(20, 20, 60)
        }
        c.drawCircle(SIZE / 2f, SIZE / 2f, SIZE * 0.40f, ring)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = SIZE * 0.34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val y = SIZE / 2f - (text.descent() + text.ascent()) / 2f
        c.drawText("IS", SIZE / 2f, y, text)
        return IconSource(toPng(bmp), "IconShift test icon", "generated")
    }

    fun fromUri(resolver: ContentResolver, uri: Uri): IconSource {
        val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val square = decoded.scale(SIZE, SIZE)
        return IconSource(toPng(square), uri.lastPathSegment ?: "picked image", "picker")
    }

    private fun toPng(bmp: Bitmap): ByteArray =
        ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
}
