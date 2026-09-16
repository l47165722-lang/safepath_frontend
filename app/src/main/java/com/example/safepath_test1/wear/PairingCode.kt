package com.example.safepath_test1.wear

import android.content.Context
import kotlin.random.Random

/**
 * A short numeric code that scopes Firebase Realtime Database status updates
 * to a single phone+watch pair, so two different installs of the app don't
 * see each other's route status.
 *
 * This is intentionally lightweight (no Firebase Auth) — good enough to stop
 * accidental cross-talk between demo devices, not a substitute for real
 * per-user authentication in a production build.
 */
object PairingCode {
    private const val PREFS = "safepath_wear_pairing"
    private const val KEY_CODE = "pairing_code"

    /** Returns the phone's pairing code, generating and persisting one on first use. */
    fun getOrCreate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CODE, null) ?: generate().also {
            prefs.edit().putString(KEY_CODE, it).apply()
        }
    }

    /** Generates a brand new code and persists it, replacing any previous one. */
    fun regenerate(context: Context): String {
        val code = generate()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CODE, code).apply()
        return code
    }

    private fun generate(): String = Random.nextInt(0, 10000).toString().padStart(4, '0')
}
