package com.example.safepath_test1.auth

import android.app.Activity
import androidx.annotation.MainThread
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PhoneCredentialSource { MANUAL_CODE, AUTOMATIC_VERIFICATION }

enum class PhoneAuthError {
    INVALID_PHONE, INVALID_CODE, GOOGLE_LOGIN_REQUIRED, ACCOUNT_CHANGED,
    NO_ACTIVE_REQUEST, NETWORK, TOO_MANY_REQUESTS,
    INVALID_REQUEST, SDK_FAILURE,
}

/** No phone number, SMS code, verification ID, token, credential or raw exception in UI state. */
sealed interface PhoneAuthState {
    data object Idle : PhoneAuthState
    data object Sending : PhoneAuthState
    data class CodeSent(val autoRetrievalTimedOut: Boolean = false) : PhoneAuthState
    /** Ready for linking, NOT a successfully linked/registered Firebase account. */
    data class CredentialReady(val source: PhoneCredentialSource) : PhoneAuthState
    data class Failed(val error: PhoneAuthError) : PhoneAuthState
}

/** Sensitive, in-memory handoff. Never log or persist this object or its fields. */
class PendingPhoneCredential internal constructor(
    val userUid: String,
    val credential: PhoneAuthCredential,
    val source: PhoneCredentialSource,
) {
    override fun toString(): String = "PendingPhoneCredential([REDACTED])"
}

/**
 * Own from a ViewModel and call all operations on the main thread (Firebase callbacks use it too).
 * Call reset() when leaving onboarding/signing out, and when the entered phone number changes.
 * Activity is passed per request, never retained here. Activity-bound callbacks stop on onStop;
 * after returning to the foreground, resumeVerification() reattaches them to the same request.
 * State is intentionally memory-only; after process death request a new verification session.
 * This class NEVER signs in, links an account, changes a UID, or marks onboarding complete.
 */
@MainThread
class PhoneAuthManager internal constructor(private val gateway: PhoneAuthGateway) {
    constructor(auth: FirebaseAuth = FirebaseAuth.getInstance()) : this(FirebasePhoneAuthGateway(auth))

    private val mutableState = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Idle)
    val state: StateFlow<PhoneAuthState> = mutableState.asStateFlow()

    private var generation = 0L
    private var ownerUid: String? = null
    private var phoneNumber: String? = null
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var pending: PendingPhoneCredential? = null
    private var terminal = false

    fun sendVerificationCode(activity: Activity, phone: String) {
        val normalized = normalizeKoreanPhoneNumber(phone)
        if (normalized == null) {
            reset()
            fail(PhoneAuthError.INVALID_PHONE)
            return
        }
        val uid = gateway.currentGoogleUid()
        if (uid == null) {
            reset()
            fail(PhoneAuthError.GOOGLE_LOGIN_REQUIRED)
            return
        }
        if (mutableState.value == PhoneAuthState.Sending && ownerUid == uid && phoneNumber == normalized) {
            return
        }
        reset()
        ownerUid = uid
        phoneNumber = normalized
        start(activity, forceResend = false)
    }

    fun resendVerificationCode(activity: Activity) {
        if (!checkOwner()) return
        if (mutableState.value == PhoneAuthState.Sending) return
        if (resendToken == null || terminal) {
            fail(PhoneAuthError.NO_ACTIVE_REQUEST)
            return
        }
        start(activity, forceResend = true)
    }

    /** Reentrant SDK request without forcing another SMS, e.g. after Activity recreation. */
    fun resumeVerification(activity: Activity) {
        if (!checkOwner()) return
        if (phoneNumber == null || terminal) {
            fail(PhoneAuthError.NO_ACTIVE_REQUEST)
            return
        }
        start(activity, forceResend = false)
    }

    /** Only constructs a credential. A wrong/expired six-digit code can still reach CredentialReady. */
    fun createCredential(code: String) {
        if (!checkOwner()) return
        if (mutableState.value == PhoneAuthState.Sending) return
        val id = verificationId
        if (id == null || terminal) {
            fail(PhoneAuthError.NO_ACTIVE_REQUEST)
            return
        }
        if (!Regex("[0-9]{6}").matches(code)) {
            fail(PhoneAuthError.INVALID_CODE)
            return
        }
        try {
            ready(gateway.createCredential(id, code), PhoneCredentialSource.MANUAL_CODE)
        } catch (_: Exception) {
            fail(PhoneAuthError.INVALID_CODE)
        }
    }

    /** One-time credential handoff. Session metadata remains in memory for a failed-link retry. */
    fun consumePendingCredential(): PendingPhoneCredential? {
        if (!checkOwner()) return null
        val result = pending ?: return null
        pending = null
        generation++
        mutableState.value = PhoneAuthState.Idle
        return result
    }

    /** Only call after a recoverable linking error; this does not mark the code as verified. */
    fun retryAfterLinkFailure(): Boolean {
        if (!checkOwner()) return false
        terminal = false
        if (verificationId != null) {
            mutableState.value = PhoneAuthState.CodeSent()
            return true
        }
        return false
    }

    val canResend: Boolean get() = resendToken != null

    /** Invalidates late callbacks too; cannot cancel an SMS already accepted by Firebase. */
    fun reset() {
        generation++
        ownerUid = null
        phoneNumber = null
        verificationId = null
        resendToken = null
        pending = null
        terminal = false
        mutableState.value = PhoneAuthState.Idle
    }

    private fun start(activity: Activity, forceResend: Boolean) {
        val phone = phoneNumber ?: return
        val requestGeneration = ++generation
        verificationId = null
        pending = null
        terminal = false
        mutableState.value = PhoneAuthState.Sending
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                if (!accept(requestGeneration)) return
                verificationId = id
                resendToken = token
                mutableState.value = PhoneAuthState.CodeSent()
            }

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                if (!accept(requestGeneration)) return
                ready(credential, PhoneCredentialSource.AUTOMATIC_VERIFICATION)
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                if (!accept(requestGeneration)) return
                terminal = true
                verificationId = null
                resendToken = null
                fail(classifyPhoneAuthError(exception))
            }

            override fun onCodeAutoRetrievalTimeOut(id: String) {
                if (!accept(requestGeneration)) return
                // Auto-retrieval timeout does NOT expire the code; manual entry remains possible.
                verificationId = id
                mutableState.value = PhoneAuthState.CodeSent(autoRetrievalTimedOut = true)
            }
        }
        try {
            gateway.verify(activity, phone, if (forceResend) resendToken else null, callbacks)
        } catch (exception: Exception) {
            if (accept(requestGeneration)) {
                terminal = true
                resendToken = null
                fail(classifyPhoneAuthError(exception))
            }
        }
    }

    private fun accept(requestGeneration: Long): Boolean =
        requestGeneration == generation && !terminal && checkOwner()

    private fun checkOwner(): Boolean {
        val uid = ownerUid
        if (uid == null) {
            fail(PhoneAuthError.NO_ACTIVE_REQUEST)
            return false
        }
        if (gateway.currentGoogleUid() != uid) {
            reset()
            fail(PhoneAuthError.ACCOUNT_CHANGED)
            return false
        }
        return true
    }

    private fun ready(credential: PhoneAuthCredential, source: PhoneCredentialSource) {
        pending = PendingPhoneCredential(checkNotNull(ownerUid), credential, source)
        terminal = true
        mutableState.value = PhoneAuthState.CredentialReady(source)
    }

    private fun fail(error: PhoneAuthError) {
        mutableState.value = PhoneAuthState.Failed(error)
    }
}

/** Accepts current Korean 010 mobile numbers only; ASCII digits avoid Unicode lookalikes. */
fun normalizeKoreanPhoneNumber(value: String): String? {
    val text = value.trim()
    if (text.isEmpty() || text.any { it !in '0'..'9' && it !in " +-()" }) return null
    val compact = text.filter { it !in " -()" }
    return when {
        Regex("010[0-9]{8}").matches(compact) -> "+82${compact.drop(1)}"
        Regex("\\+8210[0-9]{8}").matches(compact) -> compact
        else -> null
    }
}

internal interface PhoneAuthGateway {
    fun currentGoogleUid(): String?
    fun verify(
        activity: Activity,
        phone: String,
        token: PhoneAuthProvider.ForceResendingToken?,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
    )
    fun createCredential(verificationId: String, code: String): PhoneAuthCredential
}

private class FirebasePhoneAuthGateway(private val auth: FirebaseAuth) : PhoneAuthGateway {
    override fun currentGoogleUid(): String? = auth.currentUser?.takeIf { user ->
        user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }?.uid

    override fun verify(
        activity: Activity,
        phone: String,
        token: PhoneAuthProvider.ForceResendingToken?,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
    ) {
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setActivity(activity)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setCallbacks(callbacks)
        if (token != null) builder.setForceResendingToken(token)
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    override fun createCredential(verificationId: String, code: String): PhoneAuthCredential =
        PhoneAuthProvider.getCredential(verificationId, code)
}

private fun classifyPhoneAuthError(exception: Exception): PhoneAuthError = when (exception) {
    is FirebaseNetworkException -> PhoneAuthError.NETWORK
    is FirebaseTooManyRequestsException -> PhoneAuthError.TOO_MANY_REQUESTS
    is FirebaseAuthException -> PhoneAuthError.INVALID_REQUEST
    else -> PhoneAuthError.SDK_FAILURE
}
