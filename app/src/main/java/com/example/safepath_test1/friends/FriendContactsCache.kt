package com.example.safepath_test1.friends

import android.content.Context
import com.example.safepath_test1.ui.guardian.GuardianItem
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

internal fun cacheServerFriends(context: Context, uid: String, friends: List<GuardianItem>) {
    val array = JSONArray()
    friends.forEach { friend ->
        array.put(
            JSONObject().put("id", friend.id).put("name", friend.name).put("phone", friend.phone)
        )
    }
    context
        .getSharedPreferences("safe_path_server_friends", Context.MODE_PRIVATE)
        .edit()
        .putString(uid, array.toString())
        .apply()
}

fun readServerFriends(context: Context): List<GuardianItem> {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
    return runCatching {
            val array =
                JSONArray(
                    context
                        .getSharedPreferences("safe_path_server_friends", Context.MODE_PRIVATE)
                        .getString(uid, "[]")
                )
            (0 until array.length()).map { index ->
                array.getJSONObject(index).let {
                    GuardianItem(
                        it.getString("id"),
                        it.getString("name"),
                        "친구",
                        it.optString("phone"),
                    )
                }
            }
        }
        .getOrDefault(emptyList())
}
