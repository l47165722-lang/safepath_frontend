package com.example.safepath_test1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safepath_test1.ui.SafePathTab
import com.example.safepath_test1.ui.theme.DestRed
import com.example.safepath_test1.ui.theme.SafeBlue
import com.example.safepath_test1.ui.theme.TextMuted

@Composable
fun SafePathBottomBar(
    selectedTab: SafePathTab,
    onTabSelected: (SafePathTab) -> Unit,
    onSosClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = SafePathTab.entries

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom Bar Background Card (Unified 20.dp Radius, subtle 4.dp shadow)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left Tabs (0: Home, 1: SafetyMap)
                Row(modifier = Modifier.weight(1f)) {
                    TabItem(
                        tab = tabs[0],
                        isSelected = selectedTab == tabs[0],
                        onClick = { onTabSelected(tabs[0]) },
                        modifier = Modifier.weight(1f)
                    )
                    TabItem(
                        tab = tabs[1],
                        isSelected = selectedTab == tabs[1],
                        onClick = { onTabSelected(tabs[1]) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Center Spacer for SOS Floating Button
                Spacer(modifier = Modifier.width(68.dp))

                // Right Tabs (2: Guardian, 3: Profile)
                Row(modifier = Modifier.weight(1f)) {
                    TabItem(
                        tab = tabs[2],
                        isSelected = selectedTab == tabs[2],
                        onClick = { onTabSelected(tabs[2]) },
                        modifier = Modifier.weight(1f)
                    )
                    TabItem(
                        tab = tabs[3],
                        isSelected = selectedTab == tabs[3],
                        onClick = { onTabSelected(tabs[3]) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Center Floating SOS Button
        Surface(
            modifier = Modifier
                .padding(bottom = 32.dp)
                .size(72.dp)
                .shadow(6.dp, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSosClick
                ),
            shape = CircleShape,
            color = DestRed,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "SOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: SafePathTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .background(
                color = if (isSelected) SafeBlue else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = if (isSelected) Color.White else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = tab.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else TextMuted,
            )
        }
    }
}
