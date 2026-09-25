package org.fossify.messages.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri

fun Context.canResolveImagePath(path: String?): Boolean {
    if (path.isNullOrBlank()) {
        return false
    }

    val uri = Uri.parse(path)
    if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
        return true
    }

    val authority = uri.authority ?: return false
    return packageManager.resolveContentProvider(authority, 0) != null
}
