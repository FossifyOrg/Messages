package org.fossify.messages.extensions

fun String.getExtensionFromMimeType(): String {
    return when (lowercase()) {
        "image/png" -> ".png"
        "image/apng" -> ".apng"
        "image/webp" -> ".webp"
        "image/svg+xml" -> ".svg"
        "image/gif" -> ".gif"
        else -> ".jpg"
    }
}

fun String.isImageMimeType(): Boolean {
    return lowercase().startsWith("image")
}

fun String.isGifMimeType(): Boolean {
    return lowercase().endsWith("gif")
}

fun String.isVideoMimeType(): Boolean {
    return lowercase().startsWith("video")
}

fun String.isVCardMimeType(): Boolean {
    val lowercase = lowercase()
    return lowercase.endsWith("x-vcard") || lowercase.endsWith("vcard")
}

fun String.isAudioMimeType(): Boolean {
    return lowercase().startsWith("audio")
}

private val playableAudioMimeTypes = setOf(
    "audio/aac",
    "audio/mp4",
    "audio/mp4a-latm",
    "audio/mpeg",
    "audio/3gpp",
    "audio/3gpp2",
    "audio/amr",
    "audio/amr-wb",
    "audio/flac",
    "audio/ogg",
    "audio/opus",
    "audio/wav",
    "audio/x-wav",
    "audio/midi",
    "audio/x-midi",
    "audio/vorbis",
    "audio/raw",
    "audio/x-m4a",
    "audio/m4a",
    "audio/x-aac",
    "audio/aac-adts",
)

fun String.isPlayableAudioMimeType(): Boolean {
    return lowercase() in playableAudioMimeTypes
}

fun String.isCalendarMimeType(): Boolean {
    return lowercase().endsWith("calendar")
}

fun String.isPdfMimeType(): Boolean {
    return lowercase().endsWith("pdf")
}

fun String.isPlainTextMimeType(): Boolean {
    return lowercase() == "text/plain"
}
