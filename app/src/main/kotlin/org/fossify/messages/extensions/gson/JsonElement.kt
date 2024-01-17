package org.fossify.messages.extensions.gson

import com.google.gson.*
import java.math.BigDecimal
import java.math.BigInteger

val JsonElement.optString: String?
    get() = safeConversion { asString }

val JsonElement.optLong: Long?
    get() = safeConversion { asLong }

val JsonElement.optBoolean: Boolean?
    get() = safeConversion { asBoolean }

val JsonElement.optFloat: Float?
    get() = safeConversion { asFloat }

val JsonElement.optDouble: Double?
    get() = safeConversion { asDouble }

val JsonElement.optJsonObject: JsonObject?
    get() = safeConversion { asJsonObject }

val JsonElement.optJsonArray: JsonArray?
    get() = safeConversion { asJsonArray }

val JsonElement.optJsonPrimitive: JsonPrimitive?
    get() = safeConversion { asJsonPrimitive }

val JsonElement.optInt: Int?
    get() = safeConversion { asInt }

val JsonElement.optBigDecimal: BigDecimal?
    get() = safeConversion { asBigDecimal }

val JsonElement.optBigInteger: BigInteger?
    get() = safeConversion { asBigInteger }

val JsonElement.optByte: Byte?
    get() = safeConversion { asByte }

val JsonElement.optShort: Short?
    get() = safeConversion { asShort }

val JsonElement.optJsonNull: JsonNull?
    get() = safeConversion { asJsonNull }

val JsonElement.optCharacter: Char?
    get() = safeConversion { asCharacter }

private fun <T> JsonElement.safeConversion(converter: () -> T?): T? {

    return try {
        converter()
    } catch (e: Exception) {
        null
    }
}
