package org.fossify.messages.helpers

import android.content.Context
import android.net.Uri
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.io.scribe.LogoScribe
import ezvcard.io.scribe.PhotoScribe
import ezvcard.parameter.ImageType
import org.fossify.commons.helpers.ensureBackgroundThread

fun parseVCardFromUri(context: Context, uri: Uri, callback: (vCards: List<VCard>) -> Unit) {
    ensureBackgroundThread {
        val vCards = try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                Ezvcard.parse(inputStream)
                    .register(LenientPhotoScribe())
                    .register(LenientLogoScribe())
                    .all()
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        callback(vCards)
    }
}

private class LenientPhotoScribe : PhotoScribe() {
    override fun _mediaTypeFromTypeParameter(type: String?): ImageType? =
        findImageType(type = type)

    override fun _mediaTypeFromMediaTypeParameter(mediaType: String?): ImageType? =
        findImageType(mediaType = mediaType)

    override fun _mediaTypeFromFileExtension(extension: String?): ImageType? =
        findImageType(extension = extension)
}

private class LenientLogoScribe : LogoScribe() {
    override fun _mediaTypeFromTypeParameter(type: String?): ImageType? =
        findImageType(type = type)

    override fun _mediaTypeFromMediaTypeParameter(mediaType: String?): ImageType? =
        findImageType(mediaType = mediaType)

    override fun _mediaTypeFromFileExtension(extension: String?): ImageType? =
        findImageType(extension = extension)
}

private fun findImageType(
    type: String? = null,
    mediaType: String? = null,
    extension: String? = null,
): ImageType? {
    if (type == null && mediaType == null && extension == null) {
        return null
    }

    return ImageType.find(type, mediaType, extension)
}

fun VCard?.parseNameFromVCard(): String? {
    if (this == null) return null
    var fullName = formattedName?.value
    if (fullName.isNullOrEmpty()) {
        val structured = structuredName ?: return null
        val nameComponents = arrayListOf<String?>().apply {
            addAll(structured.prefixes)
            add(structured.given)
            addAll(structured.additionalNames)
            add(structured.family)
            addAll(structured.suffixes)
        }
        fullName = nameComponents.filter { !it.isNullOrEmpty() }.joinToString(separator = " ")
    }
    return fullName
}
