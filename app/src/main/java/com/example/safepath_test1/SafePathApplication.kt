package com.example.safepath_test1

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import com.kakao.vectormap.KakaoMapSdk
import java.security.MessageDigest

class SafePathApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val nativeAppKey = getString(R.string.kakao_native_app_key).trim().replace("\"", "")
        if (nativeAppKey.isBlank()) {
            Log.e("SafePathApplication", "KAKAO_NATIVE_APP_KEY is missing")
        } else {
            KakaoMapSdk.init(this, nativeAppKey)
        }

        val keyHash = getKeyHash()
        Log.i("SafePathApplication", "==================================================")
        Log.i("SafePathApplication", "KAKAO_KEY_HASH: $keyHash")
        Log.i("SafePathApplication", "==================================================")
    }

    private fun getKeyHash(): String? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.firstOrNull()?.let { signature ->
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Base64.encodeToString(md.digest(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.e("SafePathApplication", "Failed to calculate key hash", e)
            null
        }
    }
}
