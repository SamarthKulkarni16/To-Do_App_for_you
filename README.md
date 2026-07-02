<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/c9a05e91-9f3a-49b6-9da8-d2b08ef4e231

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

## Supabase Auth + Sync setup

This app requires two environment variables at build time, read via `System.getenv()` in `app/build.gradle.kts` (same pattern as the release signing secrets):

- `SUPABASE_URL` — your Supabase project URL (e.g. `https://xxxxx.supabase.co`)
- `SUPABASE_ANON_KEY` — your project's anon/public key (Settings → API)

For local builds, set these as OS environment variables before launching Android Studio / running Gradle. For CI, add them as GitHub Actions repository secrets and export them as env vars in the workflow step that runs the build.

### Google OAuth (sign-in)

Google sign-in uses the standard browser-redirect OAuth flow (Custom Tabs), **not** native Credential Manager — so no Android-specific OAuth client or SHA-1 fingerprint registration is needed for this particular flow. Instead:

1. In Google Cloud Console, create an OAuth 2.0 Client ID of type **Web application**.
2. Add this Authorized redirect URI: `https://<your-project-ref>.supabase.co/auth/v1/callback`
3. In the Supabase dashboard, go to Authentication → Providers → Google, enable it, and paste in the Client ID and Client Secret from step 1.
4. The app's deep link (`minimatodo://login-callback`) is already registered in `AndroidManifest.xml` and configured in `SupabaseClientProvider.kt` — no further app-side changes needed.

### Database

The `tasks` table schema and Row Level Security policy this app expects live in `docs/supabase-schema.sql` (run once in the Supabase SQL editor).

