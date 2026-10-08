package org.fossify.messages.extensions

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
