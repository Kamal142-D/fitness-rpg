package com.fitnessrpg.app.data.remote

import com.fitnessrpg.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

/**
 * The single Supabase client for the app. Configuration comes from PUBLIC keys
 * only (BuildConfig, sourced from local.properties) — never a service-role key
 * in the client. Postgrest + Auth are installed; the client is created lazily.
 */
object SupabaseProvider {

    /** True only when both public config values are present. */
    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotEmpty() && BuildConfig.SUPABASE_ANON_KEY.isNotEmpty()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL.ifEmpty { "http://localhost:54321" },
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY.ifEmpty { "public-anon-key-placeholder" },
        ) {
            defaultSerializer = KotlinXSerializer(json)
            install(Auth) {
                autoLoadFromStorage = true
                alwaysAutoRefresh = true
            }
            install(Postgrest)
        }
    }
}
