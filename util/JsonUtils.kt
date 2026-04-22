package dev.wizard.meta.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

val JsonElement.asJsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

val JsonElement.asJsonPrimitiveOrNull: JsonPrimitive?
    get() = this as? JsonPrimitive

val JsonElement.asBooleanOrNull: Boolean?
    get() = runCatching { asBoolean }.getOrNull()

val JsonElement.asIntOrNull: Int?
    get() = runCatching { asInt }.getOrNull()

val JsonElement.asFloatOrNull: Float?
    get() = runCatching { asFloat }.getOrNull()

val JsonElement.asDoubleOrNull: Double?
    get() = runCatching { asDouble }.getOrNull()

val JsonElement.asStringOrNull: String?
    get() = runCatching { asString }.getOrNull()
