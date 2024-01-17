package org.fossify.messages.extensions.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

private val gsonBuilder = GsonBuilder().registerTypeAdapter(object : TypeToken<Map<String, Any>>() {}.type, MapDeserializerDoubleAsIntFix())
val gson: Gson = gsonBuilder.create()

