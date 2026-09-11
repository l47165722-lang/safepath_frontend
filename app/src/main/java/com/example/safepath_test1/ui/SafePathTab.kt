package com.example.safepath_test1.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class SafePathTab(val label: String, val icon: ImageVector) {
    Home("홈", Icons.Filled.Home),
    SafetyMap("안전지수", Icons.Filled.Lock),
    Guardian("보호자", Icons.Filled.Person),
    Profile("내 정보", Icons.Filled.Person),
}
