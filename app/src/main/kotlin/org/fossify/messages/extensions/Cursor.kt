package org.fossify.messages.extensions

import android.database.Cursor
import com.google.gson.JsonNull
import com.google.gson.JsonObject

fun Cursor.rowsToJson(): JsonObject {
    val obj = JsonObject()
    for (i in 0 until columnCount) {
        val key = getColumnName(i)

        when (getType(i)) {
            Cursor.FIELD_TYPE_INTEGER -> obj.addProperty(key, getLong(i))
            Cursor.FIELD_TYPE_FLOAT -> obj.addProperty(key, getFloat(i))
            Cursor.FIELD_TYPE_STRING -> obj.addProperty(key, getString(i))
            Cursor.FIELD_TYPE_NULL -> obj.add(key, JsonNull.INSTANCE)
        }
    }
    return obj
}
