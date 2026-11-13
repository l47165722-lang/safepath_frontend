package com.example.safepath_test1.ui.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safepath_test1.auth.GoogleAuthManager
import com.example.safepath_test1.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginPage(onLoginSuccess: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { GoogleAuthManager(context) }
    var busy by remember { mutableStateOf(false) }
    Column(
        modifier.fillMaxSize().background(Color.White).systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = FieldBg,
            modifier = Modifier.size(112.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Shield, null, tint = SafeBlue, modifier = Modifier.size(64.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Safe Path", style = MaterialTheme.typography.headlineLarge, color = SafeBlue)
        Spacer(Modifier.height(8.dp))
        Text("밤길, 혼자 걷지 않도록", color = TextMuted)
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            shape = MaterialTheme.shapes.small,
            onClick = {
                scope.launch {
                    busy = true
                    val result = auth.signInWithGoogle()
                    busy = false
                    if (result.isSuccess) onLoginSuccess()
                    else
                        Toast.makeText(
                                context,
                                "Google 로그인에 실패했습니다. 다시 시도해 주세요.",
                                Toast.LENGTH_LONG,
                            )
                            .show()
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text("G", color = SkyBlue, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text(if (busy) "로그인 중…" else "Google로 계속하기", color = TextMain)
        }
        Text(
            "계속하면 Safe Path의 이용약관 · 개인정보처리방침에\n동의하는 것으로 간주돼요.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
        )
    }
}

@Composable
fun SignupScreen(onComplete: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val signup: SignupViewModel = viewModel(key = "signup-phone:${user?.uid}")
    val state by signup.state.collectAsStateWithLifecycle()
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember {
        context.getSharedPreferences("safe_path_profile", android.content.Context.MODE_PRIVATE)
    }
    val leaveSignup = {
        if (state.phase != SignupPhase.LINKING) {
            signup.abandon()
            onBack()
        }
    }
    BackHandler(onBack = leaveSignup)
    DisposableEffect(lifecycleOwner, activity, signup) {
        if (activity != null) signup.onForeground(activity, reattach = true)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> signup.onBackground()
                Lifecycle.Event.ON_RESUME -> if (activity != null) signup.onForeground(activity)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Column(
        Modifier.fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = leaveSignup, enabled = state.phase != SignupPhase.LINKING) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
            }
            Text("처음 오셨네요", style = MaterialTheme.typography.headlineLarge)
            Text("보호자가 알아볼 수 있도록 이름과 전화번호를 알려 주세요.", color = TextMuted)
            OutlinedTextField(
                state.name,
                signup::setName,
                label = { Text("이름") },
                singleLine = true,
                supportingText = { if (state.name.isBlank()) Text("이름을 입력해 주세요.") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                state.phone,
                signup::setPhone,
                label = { Text("전화번호") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = state.phase != SignupPhase.LINKING,
                supportingText = { Text("010 또는 +82로 시작하는 휴대전화 번호") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { activity?.let(signup::sendCode) },
                    enabled = activity != null && state.phase in listOf(SignupPhase.IDLE, SignupPhase.FAILED),
                    modifier = Modifier.weight(1f),
                ) { Text("인증번호 발송") }
                OutlinedButton(
                    onClick = { activity?.let(signup::resendCode) },
                    enabled = activity != null && state.canResend && !state.busy,
                    modifier = Modifier.weight(1f),
                ) { Text("재발송") }
            }
            if (state.canEnterCode || state.phase == SignupPhase.LINKING) {
                OutlinedTextField(
                    value = state.code,
                    onValueChange = signup::setCode,
                    label = { Text("6자리 인증번호") },
                    singleLine = true,
                    enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = signup::verifyCode,
                    enabled = state.canEnterCode && !state.busy && state.code.length == 6,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("인증 확인") }
            }
            if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            val progressMessage = when (state.phase) {
                SignupPhase.SENDING -> "인증번호를 요청하고 있어요."
                SignupPhase.CODE_SENT -> if (state.autoRetrievalTimedOut)
                    "자동 확인 시간이 끝났어요. 문자로 받은 인증번호를 입력해 주세요."
                else "문자로 받은 6자리 인증번호를 입력해 주세요."
                SignupPhase.LINKING -> if (state.automaticVerification)
                    "자동으로 확인한 전화번호를 Google 계정에 연결하고 있어요."
                else "인증번호를 확인하고 전화번호를 연결하고 있어요."
                SignupPhase.VERIFIED -> "전화번호 인증과 Google 계정 연결이 완료됐어요."
                else -> null
            }
            progressMessage?.let { Text(it, color = SafeBlue) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("이런 권한이 필요해요", fontWeight = FontWeight.Bold)
            listOf(
                    "위치" to "귀가 중 보호자와 실시간 위치를 공유해요",
                    "마이크" to "위험 음성과 소리를 감지할 때 사용해요",
                    "알림" to "내가 지키는 사람의 위험 알림을 받아요",
                )
                .forEach { (title, body) ->
                    Surface(color = FieldBg, shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(title, fontWeight = FontWeight.Bold)
                            Text(
                                body,
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
        }
        Button(
            shape = MaterialTheme.shapes.small,
            onClick = {
                val completed = signup.completeSignup { uid, name, phone ->
                    prefs
                        .edit()
                        .putString("user_name", name.trim())
                        .putString("user_phone", phone.trim())
                        .putString("onboarded_uid", uid)
                        .apply()
                }
                if (completed) onComplete()
            },
            enabled = state.canComplete,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text("시작하기")
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
