package com.example.safepath_test1.friends

/** Korean mobile numbers are stored consistently for the requester's profile. */
fun normalizeFriendPhone(value: String): String? {
    val text = value.trim()
    if (text.any { !it.isDigit() && it !in " +()-" }) return null
    val digits = text.filter(Char::isDigit)
    val local =
        when {
            text.startsWith("+82") -> "0" + digits.removePrefix("82")
            text.startsWith("0082") -> "0" + digits.removePrefix("0082")
            else -> digits
        }
    return if (Regex("010[0-9]{8}").matches(local)) "+82" + local.drop(1) else null
}
