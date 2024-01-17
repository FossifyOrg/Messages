package org.fossify.messages.extensions.gson

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

fun JsonObject.optGet(key: String): JsonElement? = get(key)

fun JsonObject.optGetJsonArray(key: String): JsonArray? = getAsJsonArray(key)

fun JsonObject.optGetJsonObject(key: String): JsonObject? = getAsJsonObject(key)

fun JsonObject.optGetJsonPrimitive(key: String): JsonPrimitive? = getAsJsonPrimitive(key)

fun JsonObject.optString(key: String) = optGet(key)?.asString

fun JsonObject.optLong(key: String) = optGet(key)?.asLong

fun JsonObject.optBoolean(key: String) = optGet(key)?.asBoolean

fun JsonObject.optFloat(key: String) = optGet(key)?.asFloat

fun JsonObject.optDouble(key: String) = optGet(key)?.asDouble

fun JsonObject.optJsonObject(key: String) = optGet(key)?.asJsonObject

fun JsonObject.optJsonArray(key: String) = optGet(key)?.asJsonArray

fun JsonObject.optJsonPrimitive(key: String) = optGet(key)?.asJsonPrimitive

fun JsonObject.optInt(key: String) = optGet(key)?.asInt

fun JsonObject.optBigDecimal(key: String) = optGet(key)?.asBigDecimal

fun JsonObject.optBigInteger(key: String) = optGet(key)?.asBigInteger

fun JsonObject.optByte(key: String) = optGet(key)?.asByte

fun JsonObject.optShort(key: String) = optGet(key)?.asShort

fun JsonObject.optJsonNull(key: String) = optGet(key)?.asJsonNull

fun JsonObject.optCharacter(key: String) = optGet(key)?.asCharacter

