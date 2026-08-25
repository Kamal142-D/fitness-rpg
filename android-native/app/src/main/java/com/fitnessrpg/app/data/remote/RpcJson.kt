package com.fitnessrpg.app.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/** JSON used to turn @Serializable RPC param bodies into the JsonObject the
 *  supabase-kt `rpc(function, parameters)` overload expects. */
val supabaseJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Encode a serializable value to a [JsonObject] for use as RPC parameters. */
inline fun <reified T> T.toJsonObject(): JsonObject =
    supabaseJson.encodeToJsonElement(this).jsonObject

/** Empty RPC parameters (for no-argument functions). */
val emptyRpcParams: JsonObject = JsonObject(emptyMap())
