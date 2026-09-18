package com.example.safepath_test1.ui.login

import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safepath_test1.auth.GoogleAuthManager
import com.example.safepath_test1.ui.theme.AppBorder
import com.example.safepath_test1.ui.theme.SafeBlue
import com.example.safepath_test1.ui.theme.TextMain
import com.example.safepath_test1.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun LoginPage(
    onLoginSuccess: () -> Unit,
    onDemoLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleAuthManager = remember {
        GoogleAuthManager(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 30.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(17.dp),
                color = SafeBlue
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "SafePath에 로그인",
                color = TextMain,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "대성이 일해라",
                color = TextMuted,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            ProviderButton(
                provider = "Google",
                icon = {
                    Text(
                        text = "G",
                        color = SafeBlue,
                        fontWeight = FontWeight.ExtraBold,

                        fontSize = 20.sp
                    )
                },
                onClick = {
                    scope.launch {
                        val result = googleAuthManager.signInWithGoogle()

                        if (result.isSuccess) {
                            Log.d("GoogleLogin", "Google 로그인 성공")
                            onLoginSuccess()
                        } else {
                            val exception = result.exceptionOrNull()
                            Log.e(
                                "GoogleLogin",
                                "Google 로그인 실패",
                                exception
                            )
                            Toast.makeText(
                                context,
                                "Google 로그인 실패: ${exception?.localizedMessage ?: "알 수 없는 오류"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = AppBorder
                )
                Text(
                    text = "  or  ",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = AppBorder
                )
            }

            Spacer(modifier = Modifier.height(22.dp))
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(onClick = onDemoLogin),
                shape = RoundedCornerShape(14.dp),
                color = SafeBlue.copy(alpha = 0.09f),
                border = BorderStroke(1.dp, SafeBlue.copy(alpha = 0.22f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "일단 시작",
                        color = SafeBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderButton(
    provider: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AppBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${provider}로 계속하기",
                color = TextMain,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
