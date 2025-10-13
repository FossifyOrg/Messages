package org.fossify.messages.helpers

import android.util.LruCache
import org.fossify.messages.models.NamePhoto

object MessagingCache {
    val namePhoto = LruCache<String, NamePhoto>(512)
}
