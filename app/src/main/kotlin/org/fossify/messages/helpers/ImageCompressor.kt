package org.fossify.messages.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import org.fossify.commons.extensions.getCompressionFormat
import org.fossify.commons.extensions.getMyFileUri
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.extension
import org.fossify.messages.extensions.getExtensionFromMimeType
import org.fossify.messages.extensions.getFileSizeFromUri
import org.fossify.messages.extensions.isImageMimeType
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Compress image to a given size based on
 * [Compressor](https://github.com/zetbaitsu/Compressor/)
 * */
class ImageCompressor(private val context: Context) {
    private val contentResolver = context.contentResolver
    private val outputDirectory = File(context.cacheDir, "compressed").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val minQuality = 30
    private val minResolution = 56
    private val scaleStepFactor = 0.6f // increase for more accurate file size at the cost increased computation

    fun compressImage(uri: Uri, compressSize: Long, lossy: Boolean = compressSize < FILE_SIZE_1_MB, callback: (compressedFileUri: Uri?) -> Unit) {
        ensureBackgroundThread {
            try {
                val fileSize = context.getFileSizeFromUri(uri)
                if (fileSize > compressSize) {
                    val mimeType = contentResolver.getType(uri)!!
                    if (mimeType.isImageMimeType()) {
                        val byteArray = contentResolver.openInputStream(uri)?.readBytes()!!
                        var imageFile = File(outputDirectory, System.currentTimeMillis().toString().plus(mimeType.getExtensionFromMimeType()))
                        imageFile.writeBytes(byteArray)
                        val bitmap = loadBitmap(imageFile)
                        val format = if (lossy) {
                            Bitmap.CompressFormat.JPEG
                        } else {
                            imageFile.path.getCompressionFormat()
                        }

                        // This quality approximation mostly works for smaller images but will fail with larger images.
                        val compressionRatio = compressSize / fileSize.toDouble()
                        val quality = maxOf((compressionRatio * 100).roundToInt(), minQuality)
                        imageFile = overWrite(imageFile, bitmap, format = format, quality = quality)

                        // Even the highest quality images start to look ugly if we use 10 as the minimum quality,
                        // so we better save some image quality and change resolution instead. This is time consuming
                        // and mostly needed for very large images. Since there's no reliable way to predict the
                        // required resolution, we'll just iterate and find the best result.
                        if (imageFile.length() > compressSize) {
                            var scaledWidth = bitmap.width
                            var scaledHeight = bitmap.height

                            while (imageFile.length() > compressSize) {
                                scaledWidth = (scaledWidth * scaleStepFactor).roundToInt()
                                scaledHeight = (scaledHeight * scaleStepFactor).roundToInt()
                                if (scaledHeight < minResolution && scaledWidth < minResolution) {
                                    break
                                }

                                imageFile = decodeSampledBitmapFromFile(imageFile, scaledWidth, scaledHeight).run {
                                    determineImageRotation(imageFile, bitmap = this).run {
                                        overWrite(imageFile, bitmap = this, format = format, quality = quality)
                                    }
                                }
                            }
                        }

                        callback.invoke(context.getMyFileUri(imageFile))
                    } else {
                        callback.invoke(null)
                    }
                } else {
                    // no need to compress since the file is less than the compress size
                    callback.invoke(uri)
                }
            } catch (e: Exception) {
                callback.invoke(null)
            }
        }
    }

    private fun overWrite(imageFile: File, bitmap: Bitmap, format: Bitmap.CompressFormat = imageFile.path.getCompressionFormat(), quality: Int = 100): File {
        val result = if (format == imageFile.path.getCompressionFormat()) {
            imageFile
        } else {
            File("${imageFile.absolutePath.substringBeforeLast(".")}.${format.extension()}")
        }
        imageFile.delete()
        saveBitmap(bitmap, result, format, quality)
        return result
    }

    private fun saveBitmap(bitmap: Bitmap, destination: File, format: Bitmap.CompressFormat = destination.path.getCompressionFormat(), quality: Int = 100) {
        destination.parentFile?.mkdirs()
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(destination.absolutePath)
            bitmap.compress(format, quality, fileOutputStream)
        } finally {
            fileOutputStream?.run {
                flush()
                close()
            }
        }
    }

    private fun loadBitmap(imageFile: File) = BitmapFactory.decodeFile(imageFile.absolutePath).run {
        determineImageRotation(imageFile, this)
    }

    private fun determineImageRotation(imageFile: File, bitmap: Bitmap): Bitmap {
        val exif = ExifInterface(imageFile.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
        val matrix = Matrix()
        when (orientation) {
            6 -> matrix.postRotate(90f)
            3 -> matrix.postRotate(180f)
            8 -> matrix.postRotate(270f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun decodeSampledBitmapFromFile(imageFile: File, reqWidth: Int, reqHeight: Int): Bitmap {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(imageFile.absolutePath, this)

            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            inJustDecodeBounds = false
            BitmapFactory.decodeFile(imageFile.absolutePath, this)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
