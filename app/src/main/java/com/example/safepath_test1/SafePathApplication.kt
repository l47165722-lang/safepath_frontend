package com.example.safepath_test1

import android.app.Application
import android.util.Log
import com.kakao.vectormap.KakaoMapSdk

class SafePathApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val nativeAppKey = getString(R.string.kakao_native_app_key).trim().replace("\"", "")
        if (nativeAppKey.isBlank()) {
            Log.e("SafePathApplication", "KAKAO_NATIVE_APP_KEY is missing")
        } else {
            KakaoMapSdk.init(this, nativeAppKey)
        }
    }
}
