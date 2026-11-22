package com.focusgain.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusgain.app.data.FocusPreferences
import com.focusgain.app.data.TimerMode
import com.focusgain.app.service.StudyTimerService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = FocusPreferences(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                FocusGainApp(prefs = prefs)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusGainApp(prefs: FocusPreferences) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var todaySeconds by remember { mutableStateOf(prefs.todayStudySeconds) }
    var timerSeconds by remember { mutableStateOf(StudyTimerService.currentSeconds) }
    var isRunning by remember { mutableStateOf(StudyTimerService.isRunning) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == StudyTimerService.BROADCAST_TICK) {
                    timerSeconds = intent.getLongExtra("seconds", 0L)
                    isRunning = intent.getBooleanExtra("running", false)
                    todaySeconds = intent.getLongExtra("today_seconds", prefs.todayStudySeconds)
                }
            }
        }
        val filter = IntentFilter(StudyTimerService.BROADCAST_TICK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    Scaffold(
        containerColor = Color(0xFF0B0F19),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FocusGain", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Studied today: ${todaySeconds / 3600}h ${(todaySeconds % 3600) / 60}m",
                            fontSize = 12.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF131B2E))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF131B2E)) {
                listOf("Timer", "Apps", "YouTube", "Edit").forEachIndexed { idx, label ->
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        label = { Text(label) },
                        icon = { Text(listOf("⏱", "🛡", "▶", "✎")[idx]) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> TimerTab(prefs, timerSeconds, isRunning)
                1 -> AppsBlockerTab(prefs)
                2 -> YouTubeStudyTab(prefs)
                3 -> EditAllTab(prefs)
            }
        }
    }
}

@Composable
fun TimerTab(prefs: FocusPreferences, timerSeconds: Long, isRunning: Boolean) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(prefs.selectedTimerMode) }
    var strictMode by remember { mutableStateOf(prefs.isStrictModeEnabled) }
    val strictActive = prefs.isStrictLockActive()

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Picker (Pomodoro / Countdown / Stopwatch)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimerMode.values().forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = {
                        if (!strictActive) {
                            mode = m
                            prefs.selectedTimerMode = m
                        }
                    },
                    label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        val mins = timerSeconds / 60
        val secs = timerSeconds % 60
        Text(
            text = String.format("%02d:%02d", mins, secs),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131B2E), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Text("Strict Study Mode (Full Lock)", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Locks settings & blocked apps until timer ends", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            Switch(
                checked = strictMode,
                onCheckedChange = {
                    if (!strictActive) {
                        strictMode = it
                        prefs.isStrictModeEnabled = it
                    }
                }
            )
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (strictMode && !strictActive) {
                        val lockMins = when (mode) {
                            TimerMode.POMODORO -> prefs.pomodoroFocusMinutes
                            TimerMode.COUNTDOWN -> prefs.countdownMinutes
                            TimerMode.STOPWATCH -> 30
                        }
                        prefs.strictLockUntilMillis = System.currentTimeMillis() + lockMins * 60_000L
                    }
                    val intent = Intent(context, StudyTimerService::class.java).apply {
                        action = if (isRunning) StudyTimerService.ACTION_PAUSE else StudyTimerService.ACTION_START
                    }
                    context.startForegroundService(intent)
                },
                enabled = !(isRunning && strictActive),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
            ) {
                Text(if (isRunning) "Pause" else "Start Focus", color = Color.White)
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(context, StudyTimerService::class.java).apply {
                        action = StudyTimerService.ACTION_RESET
                    }
                    context.startService(intent)
                },
                enabled = !strictActive
            ) {
                Text("Reset")
            }
        }
    }
}

@Composable
fun AppsBlockerTab(prefs: FocusPreferences) {
    val context = LocalContext.current
    val pm = context.packageManager
    val strictActive = prefs.isStrictLockActive()
    var blockedSet by remember { mutableStateOf(prefs.blockedAppPackages) }
    var search by remember { mutableStateOf("") }

    val installedApps = remember {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != context.packageName }
            .map { it.packageName to pm.getApplicationLabel(it).toString() }
            .sortedBy { it.second.lowercase() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Accessibility Guard (Required)")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search installed apps...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(installedApps.filter { it.second.contains(search, ignoreCase = true) }) { (pkg, name) ->
                val isBlocked = blockedSet.contains(pkg)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131B2E), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(name, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(pkg, color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    Switch(
                        checked = isBlocked,
                        onCheckedChange = { checked ->
                            if (strictActive && isBlocked && !checked) return@Switch
                            val updated = blockedSet.toMutableSet()
                            if (checked) updated.add(pkg) else updated.remove(pkg)
                            blockedSet = updated
                            prefs.blockedAppPackages = updated
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun YouTubeStudyTab(prefs: FocusPreferences) {
    val strictActive = prefs.isStrictLockActive()
    var ytMode by remember { mutableStateOf(prefs.isYouTubeStudyModeEnabled) }
    var blockShorts by remember { mutableStateOf(prefs.isBlockShortsEnabled) }
    var channels by remember { mutableStateOf(prefs.allowedYouTubeChannels) }
    var newChannel by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF131B2E), RoundedCornerShape(10.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("YouTube Study Mode (Allowlist Only)", color = Color.White)
            Switch(checked = ytMode, onCheckedChange = {
                if (!strictActive) {
                    ytMode = it
                    prefs.isYouTubeStudyModeEnabled = it
                }
            })
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF131B2E), RoundedCornerShape(10.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Block YouTube Shorts", color = Color.White)
            Switch(checked = blockShorts, onCheckedChange = {
                if (!strictActive) {
                    blockShorts = it
                    prefs.isBlockShortsEnabled = it
                }
            })
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newChannel,
                onValueChange = { newChannel = it },
                label = { Text("Add channel name or @handle") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (newChannel.isNotBlank()) {
                    val updated = channels + newChannel.trim()
                    channels = updated
                    prefs.allowedYouTubeChannels = updated
                    newChannel = ""
                }
            }) { Text("Add") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(channels) { ch ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF131B2E), RoundedCornerShape(10.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ch, color = Color.White)
                    TextButton(onClick = {
                        if (!strictActive) {
                            val updated = channels.filterNot { it == ch }
                            channels = updated
                            prefs.allowedYouTubeChannels = updated
                        }
                    }) { Text("Remove", color = Color(0xFFF87171)) }
                }
            }
        }
    }
}

@Composable
fun EditAllTab(prefs: FocusPreferences) {
    val strictActive = prefs.isStrictLockActive()
    var pomoFocus by remember { mutableStateOf(prefs.pomodoroFocusMinutes.toString()) }
    var pomoBreak by remember { mutableStateOf(prefs.pomodoroBreakMinutes.toString()) }
    var countdown by remember { mutableStateOf(prefs.countdownMinutes.toString()) }
    var cooldown by remember { mutableStateOf(prefs.emergencyCooldownSeconds.toString()) }
    var phrase by remember { mutableStateOf(prefs.emergencyPledgePhrase) }
    var savedMsg by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Edit Timer & Strict Mode Rules", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = pomoFocus, onValueChange = { pomoFocus = it }, label = { Text("Pomodoro Focus (minutes)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pomoBreak, onValueChange = { pomoBreak = it }, label = { Text("Pomodoro Break (minutes)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = countdown, onValueChange = { countdown = it }, label = { Text("Countdown Default (minutes)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = cooldown, onValueChange = { cooldown = it }, label = { Text("Emergency Exit Cooldown (seconds)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phrase, onValueChange = { phrase = it }, label = { Text("Emergency Exit Unlock Phrase") }, modifier = Modifier.fillMaxWidth())
        Button(
            enabled = !strictActive,
            onClick = {
                prefs.pomodoroFocusMinutes = pomoFocus.toIntOrNull()?.coerceIn(1, 180) ?: 25
                prefs.pomodoroBreakMinutes = pomoBreak.toIntOrNull()?.coerceIn(1, 60) ?: 5
                prefs.countdownMinutes = countdown.toIntOrNull()?.coerceIn(1, 300) ?: 45
                prefs.emergencyCooldownSeconds = cooldown.toIntOrNull()?.coerceIn(5, 300) ?: 30
                if (phrase.isNotBlank()) prefs.emergencyPledgePhrase = phrase.trim()
                savedMsg = "Saved all custom settings!"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save All Edits")
        }
        if (savedMsg.isNotEmpty()) {
            Text(savedMsg, color = Color(0xFF38BDF8))
        }
    }
}
