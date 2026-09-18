package com.example.safepath_test1.ui.login

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safepath_test1.ui.theme.AppBorder
import com.example.safepath_test1.ui.theme.SafeBlue
import com.example.safepath_test1.ui.theme.SafeGreen
import com.example.safepath_test1.ui.theme.TextMain
import com.example.safepath_test1.ui.theme.TextMuted

/** Development-only sign-in UI. Connect the providers to backend auth later. */
@Composable
fun LoginPage(onDemoLogin: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.White).statusBarsPadding().imePadding()
            .padding(horizontal = 30.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
                Surface(Modifier.size(54.dp), RoundedCornerShape(17.dp), color = SafeBlue) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text("SafePath에 로그인", color = TextMain, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text("대성이 일해라", color = TextMuted, fontSize = 14.sp)

                Spacer(Modifier.height(36.dp))
                ProviderButton("Google", { Text("G", color = SafeBlue, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) }, onDemoLogin)
                Spacer(Modifier.height(12.dp))
                ProviderButton("Kakao", { Icon(Icons.Default.ChatBubble, null, tint = SafeGreen, modifier = Modifier.size(20.dp)) }, onDemoLogin)

                Spacer(Modifier.height(28.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = AppBorder)
                    Text("  or  ", color = TextMuted, fontSize = 12.sp)
                    HorizontalDivider(Modifier.weight(1f), color = AppBorder)
                }
                Spacer(Modifier.height(22.dp))
                //Text("", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onDemoLogin),
                    shape = RoundedCornerShape(14.dp),
                    color = SafeBlue.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, SafeBlue.copy(alpha = 0.22f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("일단 시작", color = SafeBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
        }
    }
}

@Composable
private fun ProviderButton(provider: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(10.dp))
            Text("${provider}로 계속하기", color = TextMain, fontWeight = FontWeight.SemiBold)
        }
    }
}
