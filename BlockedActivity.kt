package com.focusgain.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusgain.app.data.FocusPreferences
import kotlinx.coroutines.delay

class BlockedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = FocusPreferences(this)
        val title = intent.getStringExtra("title") ?: "Blocked by FocusGain"
        val subtitle = intent.getStringExtra("subtitle") ?: "Stay focused on your study goals."
        val target = intent.getStringExtra("target") ?: ""

        setContent {
            BlockedScreen(
                title = title,
                subtitle = subtitle,
                target = target,
                isStrictLocked = prefs.isStrictLockActive(),
                cooldownSeconds = prefs.emergencyCooldownSeconds,
                pledgePhrase = prefs.emergencyPledgePhrase,
                onBackHome = {
                    val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                        addCategory(android.content.Intent.CATEGORY_HOME)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                    finish()
                },
                onEmergencyUnlock = {
                    prefs.strictLockUntilMillis = 0L
                    prefs.isProtectionEnabled = false
                    finish()
                }
            )
        }
    }
}

@Composable
fun BlockedScreen(
    title: String,
    subtitle: String,
    target: String,
    isStrictLocked: Boolean,
    cooldownSeconds: Int,
    pledgePhrase: String,
    onBackHome: () -> Unit,
    onEmergencyUnlock: () -> Unit
) {
    var showEmergency by remember { mutableStateOf(false) }
    var remainingCooldown by remember { mutableStateOf(cooldownSeconds) }
    var typedPhrase by remember { mutableStateOf("") }

    LaunchedEffect(showEmergency) {
        if (showEmergency) {
            remainingCooldown = cooldownSeconds
            while (remainingCooldown > 0) {
                delay(1000L)
                remainingCooldown -= 1
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isStrictLocked) "STRICT STUDY LOCK ACTIVE" else "FOCUSGAIN GUARD",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (target.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = target,
                        color = Color(0xFF38BDF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onBackHome,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Return to Study", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { showEmergency = !showEmergency }) {
                    Text("Emergency Exit", color = Color(0xFFF87171), fontSize = 13.sp)
                }

                if (showEmergency) {
                    Spacer(Modifier.height(10.dp))
                    if (remainingCooldown > 0) {
                        Text(
                            text = "Emergency cooldown: ${remainingCooldown}s remaining...",
                            color = Color(0xFFFBBF24),
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "Type exactly: \"$pledgePhrase\"",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = typedPhrase,
                            onValueChange = { typedPhrase = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onEmergencyUnlock,
                            enabled = typedPhrase.trim().equals(pledgePhrase.trim(), ignoreCase = true),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm Emergency Unlock", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
