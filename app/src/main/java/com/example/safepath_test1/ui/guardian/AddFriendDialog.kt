package com.example.safepath_test1.ui.guardian

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.safepath_test1.ui.theme.*

@Composable
internal fun AddFriendDialog(
    isSubmitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties =
            DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.White) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "친구 목록으로 돌아가기")
                    }
                    Text("친구 추가", style = MaterialTheme.typography.titleLarge)
                }
                HorizontalDivider(color = AppBorder)
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        null,
                        tint = SafeBlue,
                        modifier = Modifier.size(40.dp),
                    )
                    Text("친구에게 연결 요청을 보내세요", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "카카오톡에서 친구나 채팅방을 선택해 친구 요청 링크를 보내세요. 친구가 링크에서 내 정보를 확인하고 수락하면 연결돼요.",
                        color = TextMuted,
                    )
                    Surface(
                        color = SafeBlue.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("1. 카카오톡에서 받을 친구 선택")
                            Text("2. 친구 요청 메시지 전송")
                            Text("3. 친구가 링크에서 요청 수락")
                        }
                    }
                    error?.let {
                        Text(it, color = DangerRed, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(
                    onClick = onSubmit,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(20.dp).heightIn(min = 52.dp),
                ) {
                    if (isSubmitting)
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    else Text("카카오톡으로 요청 보내기")
                }
            }
        }
    }
}
