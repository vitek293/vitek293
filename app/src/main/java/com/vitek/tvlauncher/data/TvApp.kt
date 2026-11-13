package com.vitek.tvlauncher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

/** A launchable app installed on the TV. */
data class TvApp(
    val packageName: String,
    val label: String,
    val icon: Bitmap,
)

/**
 * Renders any [Drawable] (including adaptive icons) into a [Bitmap].
 * Named differently from core-ktx's `toBitmap` to avoid overload ambiguity.
 * Falls back to 96x96 when the drawable has no intrinsic size.
 */
fun Drawable.toIconBitmap(): Bitmap {
    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap
}
