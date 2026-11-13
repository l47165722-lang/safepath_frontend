package com.example.safepath_test1.auth

import android.app.Activity
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneAuthManagerTest {
    @Test
    fun normalizesDomesticAndInternationalMobileNumbers() {
        listOf("01012345678", "010-1234-5678", " 010 1234 5678 ",
            "+821012345678", "+82 10-1234-5678", "+82 (10) 1234-5678")
            .forEach { assertEquals("+821012345678", normalizeKoreanPhoneNumber(it)) }
    }

    @Test
    fun rejectsLandlinesMalformedCountryCodesAndNonAsciiDigits() {
        listOf("", "02-1234-5678", "01112345678", "0101234567", "010123456789",
            "+8201012345678", "+811012345678", "82 10 1234 5678", "++821012345678",
            "010+12345678", "abc01012345678", "010/1234/5678", "010１２３４５６７８",
            "01012345678 ext 1")
            .forEach { assertNull(normalizeKoreanPhoneNumber(it)) }
    }

    @Test
    fun codeWithoutVerificationSessionCannotProduceCredential() {
        val manager = PhoneAuthManager(UnusedGateway())
        manager.createCredential("123456")
        assertEquals(PhoneAuthState.Failed(PhoneAuthError.NO_ACTIVE_REQUEST), manager.state.value)
        assertNull(manager.consumePendingCredential())
    }

    @Test
    fun resetClearsFailureAndReturnsToIdle() {
        val manager = PhoneAuthManager(UnusedGateway())
        manager.createCredential("123456")
        manager.reset()
        assertEquals(PhoneAuthState.Idle, manager.state.value)
    }

    private class UnusedGateway : PhoneAuthGateway {
        override fun currentGoogleUid(): String? = "google-user"
        override fun verify(
            activity: Activity,
            phone: String,
            token: PhoneAuthProvider.ForceResendingToken?,
            callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        ): Unit = error("A unit test must not request SMS")

        override fun createCredential(verificationId: String, code: String): PhoneAuthCredential =
            error("No active session must not construct a credential")
    }
}
