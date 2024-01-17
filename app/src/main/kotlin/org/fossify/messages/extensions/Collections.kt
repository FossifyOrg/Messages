package org.fossify.messages.extensions

import android.content.ContentValues

inline fun <T> List<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    var index = 0
    for (item in this) {
        if (predicate(item))
            return index
        index++
    }
    return null
}

fun Map<String, Any>.toContentValues(): ContentValues {
    val contentValues = ContentValues()
    for (item in entries) {
        when (val value = item.value) {
            is String -> contentValues.put(item.key, value)
            is Byte -> contentValues.put(item.key, value)
            is Short -> contentValues.put(item.key, value)
            is Int -> contentValues.put(item.key, value)
            is Long -> contentValues.put(item.key, value)
            is Float -> contentValues.put(item.key, value)
            is Double -> contentValues.put(item.key, value)
            is Boolean -> contentValues.put(item.key, value)
            is ByteArray -> contentValues.put(item.key, value)
        }
    }

    return contentValues
}

fun <T> Collection<T>.toArrayList() = ArrayList(this)
