package com.example.safepath_test1.friends

import org.junit.Assert.*
import org.junit.Test

class FriendInviteTest {
    @Test
    fun domesticAndInternationalNumbersMatch() {
        listOf("010-1234-5678", "010 1234 5678", "+82 10-1234-5678", "0082 10 1234 5678").forEach {
            assertEquals("+821012345678", normalizeFriendPhone(it))
        }
    }

    @Test
    fun invalidAndNonMobileNumbersAreRejected() {
        listOf("", "02-123-4567", "0101234567", "010123456789", "abc01012345678", "010/1234/5678")
            .forEach { assertNull(normalizeFriendPhone(it)) }
    }
}
