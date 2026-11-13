package com.example.safepath_test1.ui.login

import android.app.Activity
import com.example.safepath_test1.auth.PhoneAuthManager
import com.example.safepath_test1.auth.PhoneAuthState
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

internal class SignupAccount(val uid: String, val phone: String?, val name: String = "") {
    override fun toString() = "SignupAccount([REDACTED])"
}

internal enum class SignupLinkError {
    ACCOUNT_CHANGED, INVALID_CODE, EXPIRED_CODE, PHONE_IN_USE, NETWORK, TOO_MANY_REQUESTS,
    RECENT_LOGIN_REQUIRED, UNKNOWN,
}

internal class SignupLinkException(val reason: SignupLinkError) : Exception(reason.name)

/** Isolates Firebase and Android from the signup state machine for offline unit tests. */
internal interface SignupPhoneBackend {
    val state: StateFlow<PhoneAuthState>
    val canResend: Boolean
    fun currentAccount(): SignupAccount?
    fun observeAccount(listener: () -> Unit): () -> Unit
    fun send(activity: Activity, phone: String)
    fun resend(activity: Activity)
    fun resume(activity: Activity)
    fun createCredential(code: String)
    suspend fun linkCredential(expectedUid: String, expectedPhone: String): SignupAccount
    fun retryCode(): Boolean
    fun reset()
}

internal class FirebaseSignupPhoneBackend(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val manager: PhoneAuthManager = PhoneAuthManager(auth),
) : SignupPhoneBackend {
    override val state = manager.state
    override val canResend get() = manager.canResend

    override fun currentAccount(): SignupAccount? = auth.currentUser?.takeIf { user ->
        user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }?.let { SignupAccount(it.uid, it.phoneNumber, it.displayName.orEmpty()) }

    override fun observeAccount(listener: () -> Unit): () -> Unit {
        val observer = FirebaseAuth.AuthStateListener { listener() }
        auth.addAuthStateListener(observer)
        return { auth.removeAuthStateListener(observer) }
    }

    override fun send(activity: Activity, phone: String) = manager.sendVerificationCode(activity, phone)
    override fun resend(activity: Activity) = manager.resendVerificationCode(activity)
    override fun resume(activity: Activity) = manager.resumeVerification(activity)
    override fun createCredential(code: String) = manager.createCredential(code)
    override fun retryCode() = manager.retryAfterLinkFailure()
    override fun reset() = manager.reset()

    override suspend fun linkCredential(expectedUid: String, expectedPhone: String): SignupAccount {
        val pending = manager.consumePendingCredential()
            ?: throw SignupLinkException(SignupLinkError.ACCOUNT_CHANGED)
        val user = auth.currentUser
        if (user == null || user.uid != expectedUid || pending.userUid != expectedUid ||
            currentAccount()?.uid != expectedUid) {
            throw SignupLinkException(SignupLinkError.ACCOUNT_CHANGED)
        }
        try {
            val linked = try {
                user.linkWithCredential(pending.credential).await().user
            } catch (error: FirebaseAuthException) {
                if (error.errorCode != "ERROR_PROVIDER_ALREADY_LINKED") throw error
                if (currentAccount()?.uid != expectedUid) {
                    throw SignupLinkException(SignupLinkError.ACCOUNT_CHANGED)
                }
                // A previous attempt may have linked before process death or a lost response.
                // Still validate the NEW code on the server; never trust the cached phone alone.
                if (user.phoneNumber == expectedPhone) {
                    user.reauthenticate(pending.credential).await()
                    user
                } else {
                    // Replacing an already linked phone also preserves this Google user's UID.
                    user.updatePhoneNumber(pending.credential).await()
                    user
                }
            }
            val current = currentAccount()
            if (linked?.uid != expectedUid || current?.uid != expectedUid ||
                linked.phoneNumber != expectedPhone || current.phone != expectedPhone) {
                throw SignupLinkException(SignupLinkError.ACCOUNT_CHANGED)
            }
            return current
        } catch (error: CancellationException) {
            throw error
        } catch (error: SignupLinkException) {
            throw error
        } catch (error: Exception) {
            throw SignupLinkException(classifySignupLinkError(error))
        }
    }
}

internal fun classifySignupLinkError(error: Exception): SignupLinkError = when (error) {
    is FirebaseNetworkException -> SignupLinkError.NETWORK
    is FirebaseTooManyRequestsException -> SignupLinkError.TOO_MANY_REQUESTS
    is FirebaseAuthException -> signupLinkErrorCode(error.errorCode)
    else -> SignupLinkError.UNKNOWN
}

internal fun signupLinkErrorCode(code: String): SignupLinkError = when (code) {
    "ERROR_INVALID_VERIFICATION_CODE", "ERROR_INVALID_CREDENTIAL" -> SignupLinkError.INVALID_CODE
    "ERROR_SESSION_EXPIRED", "ERROR_CODE_EXPIRED", "ERROR_INVALID_VERIFICATION_ID" -> SignupLinkError.EXPIRED_CODE
    "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> SignupLinkError.PHONE_IN_USE
    "ERROR_REQUIRES_RECENT_LOGIN" -> SignupLinkError.RECENT_LOGIN_REQUIRED
    "ERROR_USER_MISMATCH", "ERROR_USER_TOKEN_EXPIRED", "ERROR_USER_DISABLED" -> SignupLinkError.ACCOUNT_CHANGED
    "ERROR_TOO_MANY_REQUESTS", "ERROR_QUOTA_EXCEEDED" -> SignupLinkError.TOO_MANY_REQUESTS
    "ERROR_NETWORK_REQUEST_FAILED" -> SignupLinkError.NETWORK
    else -> SignupLinkError.UNKNOWN
}
