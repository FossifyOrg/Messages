package org.fossify.messages.extensions

import android.graphics.Bitmap

fun Bitmap.CompressFormat.extension() = when (this) {
    Bitmap.CompressFormat.PNG -> "png"
    Bitmap.CompressFormat.WEBP -> "webp"
    else -> "jpg"
}
