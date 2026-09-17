package com.example.safepath_test1.wear.presentation

import android.content.Context

/**
 * Stores the 4-digit pairing code the user typed in on the watch, matching
 * the code shown on the phone's Settings screen (see PairingCode.kt on the
 * phone side). Kept separate from the phone's copy — these are two
 * independent devices/processes.
 */
object WearPairing {
    private const val PREFS = "safepath_wear_pairing"
    private const val KEY_CODE = "pairing_code"

    fun getCode(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CODE, null)

    fun setCode(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CODE, code).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_CODE).apply()
    }
}
