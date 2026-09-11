package com.example.safepath_test1.location

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.safepath_test1.model.GeoPoint

fun shareLocation(context: Context, location: GeoPoint?, isEmergency: Boolean) {
    if (location == null) {
        Toast.makeText(context, "위치를 확인한 뒤 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
        return
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, LocationShareFormatter.format(location, isEmergency))
    }
    context.startActivity(Intent.createChooser(intent, "위치 공유"))
}
