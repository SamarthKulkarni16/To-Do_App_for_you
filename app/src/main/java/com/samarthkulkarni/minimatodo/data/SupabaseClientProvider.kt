package com.samarthkulkarni.minimatodo.data

import com.samarthkulkarni.minimatodo.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

/**
 * Single shared Supabase client for the app.
 *
 * URL and anon key are injected at build time via BuildConfig (see app/build.gradle.kts),
 * sourced from the SUPABASE_URL / SUPABASE_ANON_KEY environment variables (GitHub Actions
 * secrets in CI). The anon key is safe to ship in the client -- it is public by design and
 * relies on Postgres Row Level Security (RLS) policies on the `tasks` table to restrict access
 * to each signed-in user's own rows.
 */
object SupabaseClientProvider {

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                // Deep link that Google OAuth redirects back to after the browser step.
                // Pattern: minimatodo://login-callback (registered as an intent-filter in the manifest).
                host = "login-callback"
                scheme = "minimatodo"
                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
            }
            install(Postgrest)
        }
    }

    val auth get() = client.auth
    val postgrest get() = client.postgrest
}
