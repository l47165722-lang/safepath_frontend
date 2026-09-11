package com.example.safepath_test1.ui.safetymap

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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safepath_test1.location.RadiusAnalysisResult
import com.example.safepath_test1.location.SafetyLevel
import com.example.safepath_test1.location.SafetyRepository
import com.example.safepath_test1.model.GeoPoint
import com.example.safepath_test1.ui.components.PageHeader
import com.example.safepath_test1.ui.theme.AppBorder
import com.example.safepath_test1.ui.theme.FieldBg
import com.example.safepath_test1.ui.theme.SafeBlue
import com.example.safepath_test1.ui.theme.TextMain
import com.example.safepath_test1.ui.theme.TextMuted
import kotlin.math.roundToInt

private const val DefaultDalseoLat = 35.8572
private const val DefaultDalseoLng = 128.5712

@Composable
fun SafetyMapScreen(
    currentLocation: GeoPoint?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var radiusMeters by remember { mutableFloatStateOf(300f) }
    var analysisResult by remember {
        mutableStateOf(
            RadiusAnalysisResult(
                cctvCount = 0,
                streetlightCount = 0,
                level = SafetyLevel.GOOD,
                totalScore = 80,
            )
        )
    }

    val lat = currentLocation?.latitude ?: DefaultDalseoLat
    val lng = currentLocation?.longitude ?: DefaultDalseoLng

    LaunchedEffect(radiusMeters, currentLocation) {
        val result = SafetyRepository.analyzeRadius(
            context = context,
            centerLat = lat,
            centerLng = lng,
            radiusMeters = radiusMeters.toDouble(),
        )
        analysisResult = result
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PageHeader("안전지수 분석", "반경을 조절하여 내 주변 안전도 및 시설을 확인하세요")
        }

        // 1. Radius Control Slider Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "탐색 반경 설정",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SafeBlue.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = "반경 ${radiusMeters.roundToInt()}m",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = SafeBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Horizontal Slider
                Slider(
                    value = radiusMeters,
                    onValueChange = { radiusMeters = it },
                    valueRange = 100f..1000f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = SafeBlue,
                        activeTrackColor = SafeBlue,
                        inactiveTrackColor = FieldBg,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Quick Radius Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(100f, 300f, 500f, 1000f).forEach { preset ->
                        val isSelected = (radiusMeters.roundToInt() == preset.toInt())
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { radiusMeters = preset },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) SafeBlue else FieldBg,
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${preset.toInt()}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextMuted,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Safety Grade Badge Banner Card
        val level = analysisResult.level
        val levelColor = Color(level.hexColor)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "내 주변 안전 등급",
                    fontSize = 14.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium,
                )

                // Large Grade Badge
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = levelColor.copy(alpha = 0.15f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = level.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = levelColor,
                        )
                    }
                }

                Text(
                    text = level.description,
                    fontSize = 13.sp,
                    color = TextMain,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                HorizontalDivider(color = AppBorder)

                // 4-Level Color Gauge Indicator Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SafetyLevelLegend("나쁨", Color(0xFFEF4444), level == SafetyLevel.BAD)
                        SafetyLevelLegend("보통", Color(0xFFEAB308), level == SafetyLevel.MODERATE)
                        SafetyLevelLegend("좋음", Color(0xFF84CC16), level == SafetyLevel.GOOD)
                        SafetyLevelLegend("매우 좋음", Color(0xFF22C55E), level == SafetyLevel.VERY_GOOD)
                    }

                    // 4 Colored Blocks Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(Color(0xFFEF4444).copy(alpha = if (level == SafetyLevel.BAD) 1f else 0.25f)),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(Color(0xFFEAB308).copy(alpha = if (level == SafetyLevel.MODERATE) 1f else 0.25f)),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(Color(0xFF84CC16).copy(alpha = if (level == SafetyLevel.GOOD) 1f else 0.3f)),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(Color(0xFF22C55E).copy(alpha = if (level == SafetyLevel.VERY_GOOD) 1f else 0.25f)),
                        )
                    }
                }
            }
        }

        // 3. Facility Count Breakdown Cards (2x2 Grid)
        Text(
            text = "반경 내 안전시설 현황 (${radiusMeters.roundToInt()}m)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FacilityStatCard(
                icon = Icons.Filled.Lock,
                label = "CCTV",
                count = "${analysisResult.cctvCount}개",
                modifier = Modifier.weight(1f),
            )
            FacilityStatCard(
                icon = Icons.Filled.Star,
                label = "가로등",
                count = "${analysisResult.streetlightCount}개",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FacilityStatCard(
                icon = Icons.Filled.LocationOn,
                label = "경찰시설",
                count = "2개",
                modifier = Modifier.weight(1f),
            )
            FacilityStatCard(
                icon = Icons.Filled.Notifications,
                label = "비상벨",
                count = "4개",
                modifier = Modifier.weight(1f),
            )
        }

        // 4. Safety Tip Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FieldBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = SafeBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("안심 이동 팁", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextMain)
                    Text(
                        "야간 이동 시 CCTV와 가로등 밀집도가 높은 밝은 주요 도로를 중심으로 이동하는 것을 권장합니다.",
                        fontSize = 12.sp,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyLevelLegend(
    label: String,
    color: Color,
    isSelected: Boolean,
) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
        color = if (isSelected) color else TextMuted,
    )
}

@Composable
private fun FacilityStatCard(
    icon: ImageVector,
    label: String,
    count: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, contentDescription = label, tint = SafeBlue, modifier = Modifier.size(18.dp))
                Text(label, fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            }
            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain,
            )
        }
    }
}
