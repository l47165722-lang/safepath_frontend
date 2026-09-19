package com.example.safepath_test1.location

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.widget.Toast
import com.example.safepath_test1.model.GeoPoint

fun shareLocation(context: Context, location: GeoPoint?, isEmergency: Boolean): Boolean {
    if (location == null) {
        Toast.makeText(context, "위치를 확인한 뒤 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
        return false
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, LocationShareFormatter.format(location, isEmergency))
    }
    return try {
        context.startActivity(Intent.createChooser(intent, if (isEmergency) "긴급 위치 공유" else "위치 공유"))
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "공유할 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
        false
    }
}
