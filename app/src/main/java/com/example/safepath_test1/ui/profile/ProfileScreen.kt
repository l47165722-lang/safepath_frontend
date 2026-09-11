package com.example.safepath_test1.ui.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safepath_test1.ui.settings.SettingsScreen
import com.example.safepath_test1.ui.theme.AppBorder
import com.example.safepath_test1.ui.theme.FieldBg
import com.example.safepath_test1.ui.theme.SafeBlue
import com.example.safepath_test1.ui.theme.TextMain
import com.example.safepath_test1.ui.theme.TextMuted
import org.json.JSONArray

@Composable
fun ProfileScreen(
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            hasLocationPermission = hasLocationPermission,
            onBack = { showSettings = false },
            modifier = modifier,
        )
    } else {
        ProfileContent(
            onOpenSettings = { showSettings = true },
            modifier = modifier,
        )
    }
}

@Composable
private fun ProfileContent(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("safe_path_profile", Context.MODE_PRIVATE)
    }

    var guardianCount by remember { mutableIntStateOf(0) }
    var guardianNames by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val gPrefs = context.getSharedPreferences("safe_path_guardians", Context.MODE_PRIVATE)
        val jsonString = gPrefs.getString("guardian_list", "[]") ?: "[]"
        val array = JSONArray(jsonString)
        guardianCount = array.length()
        if (guardianCount > 0) {
            val first = array.optJSONObject(0)?.optString("name", "") ?: ""
            guardianNames = if (guardianCount > 1) "${first} 외 ${guardianCount - 1}명" else first
        } else {
            guardianNames = "없음"
        }
    }

    var userName by remember { mutableStateOf(prefs.getString("user_name", "김안전") ?: "김안전") }
    var userPhone by remember { mutableStateOf(prefs.getString("user_phone", "010-1234-5678") ?: "010-1234-5678") }
    var emergencyContact by remember { mutableStateOf(prefs.getString("emergency_contact", "010-9876-5432 (엄마)") ?: "010-9876-5432 (엄마)") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header with Settings Gear Button in Top-Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "내 정보",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextMain,
                )
                Text(
                    text = "내 상태 및 정보를 확인하세요",
                    color = TextMuted,
                    fontSize = 14.sp,
                )
            }

            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clickable(onClick = onOpenSettings),
                shape = CircleShape,
                color = FieldBg,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, contentDescription = "설정", tint = TextMain, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Profile Main Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SafeBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = SafeBlue, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain,
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SafeBlue.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    text = "안심 회원",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = SafeBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = userPhone,
                            fontSize = 13.sp,
                            color = TextMuted,
                        )
                    }
                }

                HorizontalDivider(color = AppBorder)

                InfoRow(label = "비상 연락처", value = emergencyContact)
                InfoRow(label = "주 사용 위치", value = "서울특별시 중구")
            }
        }

        // Quick Safety Status Card
        Text(
            text = "안심 연결 상태",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column {
                StatusItemRow(
                    icon = Icons.Filled.Person,
                    title = "연결된 보호자",
                    status = if (guardianCount > 0) "${guardianCount}명 연결됨 ($guardianNames)" else "보호자 없음",
                    isPositive = guardianCount > 0,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = AppBorder)
                StatusItemRow(
                    icon = Icons.Filled.LocationOn,
                    title = "실시간 위치 공유",
                    status = "활성화 됨",
                    isPositive = true,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = AppBorder)
                StatusItemRow(
                    icon = Icons.Filled.Info,
                    title = "스마트워치 기기 연동",
                    status = "미연동",
                    isPositive = false,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = AppBorder)
                StatusItemRow(
                    icon = Icons.Filled.Lock,
                    title = "마이크 상태",
                    status = if (hasMicPermission(context)) "권한 허용" else "허용 필요",
                    isPositive = hasMicPermission(context),
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = AppBorder)
                StatusItemRow(
                    icon = Icons.Filled.Lock,
                    title = "카메라 상태",
                    status = if (hasCameraPermission(context)) "권한 허용" else "허용 필요",
                    isPositive = hasCameraPermission(context),
                )
            }
        }
    }
}

private fun hasMicPermission(context: Context): Boolean {
    return androidx.core.content.ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun hasCameraPermission(context: Context): Boolean {
    return androidx.core.content.ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.CAMERA
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp)
        Text(text = value, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusItemRow(
    icon: ImageVector,
    title: String,
    status: String,
    isPositive: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = title, tint = SafeBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextMain,
        )
        Text(
            text = status,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPositive) SafeBlue else TextMuted,
        )
    }
}
