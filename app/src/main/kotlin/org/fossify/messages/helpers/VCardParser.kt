package org.fossify.messages.helpers

import android.content.Context
import android.net.Uri
import ezvcard.Ezvcard
import ezvcard.VCard
import org.fossify.commons.helpers.ensureBackgroundThread

fun parseVCardFromUri(context: Context, uri: Uri, callback: (vCards: List<VCard>) -> Unit) {
    ensureBackgroundThread {
        val inputStream = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            callback(emptyList())
            return@ensureBackgroundThread
        }
        val vCards = Ezvcard.parse(inputStream).all()
        callback(vCards)
    }
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
