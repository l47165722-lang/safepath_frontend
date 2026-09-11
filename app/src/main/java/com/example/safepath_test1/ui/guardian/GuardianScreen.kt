package com.example.safepath_test1.ui.guardian

import android.widget.Toast
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.safepath_test1.model.GeoPoint
import com.example.safepath_test1.ui.components.PageHeader
import com.example.safepath_test1.ui.theme.AppBorder
import com.example.safepath_test1.ui.theme.DangerRed
import com.example.safepath_test1.ui.theme.FieldBg
import com.example.safepath_test1.ui.theme.SafeBlue
import com.example.safepath_test1.ui.theme.TextMain
import com.example.safepath_test1.ui.theme.TextMuted
import org.json.JSONArray
import org.json.JSONObject

data class GuardianItem(
    val id: String,
    val name: String,
    val relationship: String,
    val phone: String,
)

@Composable
fun GuardianScreen(
    currentLocation: GeoPoint?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("safe_path_guardians", android.content.Context.MODE_PRIVATE)
    }
    var showAddDialog by remember { mutableStateOf(false) }

    val guardians = remember {
        val jsonString = prefs.getString("guardian_list", "[]") ?: "[]"
        val array = JSONArray(jsonString)
        val list = mutableStateListOf<GuardianItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                GuardianItem(
                    id = obj.optString("id", ""),
                    name = obj.optString("name", ""),
                    relationship = obj.optString("relationship", ""),
                    phone = obj.optString("phone", ""),
                )
            )
        }
        list
    }

    fun saveGuardians() {
        val array = JSONArray()
        guardians.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("relationship", g.relationship)
            obj.put("phone", g.phone)
            array.put(obj)
        }
        prefs.edit().putString("guardian_list", array.toString()).apply()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader("보호자", "안심 보호자를 등록하고 관리하세요 (최대 9명)")

        // Top Row: Status & Add Guardian Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "등록된 보호자 (${guardians.size}/9)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
            )

            Button(
                onClick = {
                    if (guardians.size >= 9) {
                        Toast.makeText(context, "보호자는 최대 9명까지 등록할 수 있습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        showAddDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafeBlue),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("보호자 추가", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3-Column Grid for Guardians
        if (guardians.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = SafeBlue,
                            modifier = Modifier.size(44.dp),
                        )
                        Text(
                            text = "등록된 보호자가 없습니다.",
                            fontWeight = FontWeight.Bold,
                            color = TextMain,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "상단 '보호자 추가' 버튼을 눌러 보호자를 등록해 주세요.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            val rows = guardians.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (guardian in rowItems) {
                            GuardianCard(
                                guardian = guardian,
                                onDelete = {
                                    guardians.remove(guardian)
                                    saveGuardians()
                                    Toast.makeText(context, "${guardian.name} 보호자가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGuardianDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, relationship, phone ->
                val newGuardian = GuardianItem(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    relationship = relationship,
                    phone = phone,
                )
                guardians.add(newGuardian)
                saveGuardians()
                showAddDialog = false
                Toast.makeText(context, "${name} 보호자가 추가되었습니다.", Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun GuardianCard(
    guardian: GuardianItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            // Delete Button (Top-Right "✕")
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clickable(onClick = onDelete),
                shape = CircleShape,
                color = DangerRed.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, contentDescription = "삭제", tint = DangerRed, modifier = Modifier.size(12.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SafeBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = SafeBlue, modifier = Modifier.size(22.dp))
                }

                Text(
                    text = guardian.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = FieldBg,
                ) {
                    Text(
                        text = guardian.relationship,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = guardian.phone,
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AddGuardianDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, relationship: String, phone: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var rawPhone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val presetRelationships = listOf("부모님", "배우자", "자녀", "친구")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header with Icon & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = SafeBlue.copy(alpha = 0.12f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = SafeBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "새 보호자 추가",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain,
                            )
                            Text(
                                text = "비상 시 소식을 받을 보호자 정보",
                                fontSize = 11.sp,
                                color = TextMuted,
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onDismiss),
                        shape = CircleShape,
                        color = FieldBg,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Close, contentDescription = "닫기", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                HorizontalDivider(color = AppBorder)

                // Input Card 1: 이름
                InputCard(
                    icon = Icons.Filled.Person,
                    label = "이름",
                    value = name,
                    placeholder = "보호자 이름",
                    onValueChange = { name = it },
                )

                // Input Card 2: 관계 + Quick Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InputCard(
                        icon = Icons.Filled.Info,
                        label = "관계",
                        value = relationship,
                        placeholder = "부모님, 친구, 배우자 등",
                        onValueChange = { relationship = it },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        presetRelationships.forEach { preset ->
                            val isSelected = relationship == preset
                            Surface(
                                modifier = Modifier.clickable { relationship = preset },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) SafeBlue.copy(alpha = 0.15f) else FieldBg,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, SafeBlue) else null,
                            ) {
                                Text(
                                    text = preset,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) SafeBlue else TextMuted,
                                )
                            }
                        }
                    }
                }

                // Input Card 3: 전화번호
                InputCard(
                    icon = Icons.Filled.Call,
                    label = "전화번호",
                    value = rawPhone,
                    placeholder = "010-0000-0000",
                    isPhoneField = true,
                    onValueChange = { rawPhone = it },
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = DangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Primary Action Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clickable {
                            if (name.isBlank() || relationship.isBlank() || rawPhone.isBlank()) {
                                errorMessage = "모든 정보를 올바르게 입력해 주세요."
                            } else {
                                onAdd(name.trim(), relationship.trim(), rawPhone.trim())
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = SafeBlue,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "보호자 등록하기",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputCard(
    icon: ImageVector,
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isPhoneField: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = FieldBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = label, tint = SafeBlue, modifier = Modifier.size(18.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                )

                if (isPhoneField) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { context ->
                            android.widget.EditText(context).apply {
                                setBackgroundResource(android.R.color.transparent)
                                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                                setTextColor(android.graphics.Color.parseColor("#111827"))
                                setHintTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                                hint = placeholder
                                inputType = android.text.InputType.TYPE_CLASS_PHONE
                                maxLines = 1
                                setSingleLine()
                                setPadding(0, 0, 0, 0)

                                @Suppress("DEPRECATION")
                                addTextChangedListener(android.telephony.PhoneNumberFormattingTextWatcher())

                                addTextChangedListener(object : android.text.TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                    override fun afterTextChanged(s: android.text.Editable?) {
                                        onValueChange(s?.toString() ?: "")
                                    }
                                })
                            }
                        },
                        update = { view ->
                            if (view.text.toString() != value) {
                                val cursor = view.selectionStart
                                view.setText(value)
                                try {
                                    view.setSelection(cursor)
                                } catch (exception: Exception) {
                                    Log.w("GuardianScreen", "Restoring text cursor failed; moving it to the end", exception)
                                    view.setSelection(view.text.length)
                                }
                            }
                        },
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 13.sp,
                                color = TextMuted.copy(alpha = 0.6f),
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                color = TextMain,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            cursorBrush = SolidColor(SafeBlue),
                            keyboardOptions = keyboardOptions,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
