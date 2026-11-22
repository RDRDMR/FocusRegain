package com.focusgain.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.focusgain.app.data.FocusPreferences
import com.focusgain.app.ui.BlockedActivity

/**
 * Core Regain-style blocker service.
 * 1. Blocks any distracting app in [FocusPreferences.blockedAppPackages].
 * 2. When YouTube (com.google.android.youtube) is open and YouTube Study Mode is active:
 *    - Blocks YouTube Shorts immediately.
 *    - Reads visible channel name / handle from the player UI and checks against the user's
 *      allowed channels list. Non-educational / non-allowlisted videos trigger BlockedActivity.
 */
class FocusAccessibilityService : AccessibilityService() {

    private lateinit var prefs: FocusPreferences
    private var lastBlockedTimestamp = 0L

    companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val BLOCK_COOLDOWN_MS = 1200L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = FocusPreferences(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::prefs.isInitialized) return
        if (!prefs.isProtectionEnabled) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore our own app
        if (packageName == this.packageName) return

        // 1. Check general distracting app blocklist
        if (packageName != YOUTUBE_PACKAGE && prefs.blockedAppPackages.contains(packageName)) {
            launchBlockScreen(
                title = "App Blocked",
                subtitle = "Stay focused on your study session.",
                blockedTarget = packageName
            )
            return
        }

        // 2. Check YouTube Study Mode
        if (packageName == YOUTUBE_PACKAGE && prefs.isYouTubeStudyModeEnabled) {
            val root = rootInActiveWindow ?: return
            inspectYouTubeWindow(root)
        }
    }

    private fun inspectYouTubeWindow(root: AccessibilityNodeInfo) {
        // A. Block YouTube Shorts
        if (prefs.isBlockShortsEnabled && isShortsVisible(root)) {
            launchBlockScreen(
                title = "YouTube Shorts Blocked",
                subtitle = "Short-form feeds are locked in YouTube Study Mode.",
                blockedTarget = "YouTube Shorts"
            )
            return
        }

        // B. Detect if user is on a Video Watch Page and inspect the channel name
        val channelName = extractVisibleChannelName(root) ?: return
        val allowed = prefs.allowedYouTubeChannels
        val normalizedChannel = channelName.trim().lowercase()

        val isAllowed = allowed.any { entry ->
            val cleanEntry = entry.trim().lowercase().removePrefix("@")
            cleanEntry.isNotEmpty() && (
                normalizedChannel == cleanEntry ||
                normalizedChannel.contains(cleanEntry) ||
                cleanEntry.contains(normalizedChannel)
            )
        }

        if (!isAllowed) {
            launchBlockScreen(
                title = "Channel Not in Study List",
                subtitle = "Channel \"$channelName\" is blocked in YouTube Study Mode.",
                blockedTarget = channelName
            )
        }
    }

    private fun isShortsVisible(root: AccessibilityNodeInfo): Boolean {
        val shortsIds = listOf(
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/reel_watch_player",
            "com.google.android.youtube:id/shorts_player_container"
        )
        return shortsIds.any { id ->
            root.findAccessibilityNodeInfosByViewId(id).isNotEmpty()
        }
    }

    private fun extractVisibleChannelName(root: AccessibilityNodeInfo): String? {
        // Common YouTube Android view IDs for channel title on watch screen
        val candidateIds = listOf(
            "com.google.android.youtube:id/channel_name",
            "com.google.android.youtube:id/owner_text",
            "com.google.android.youtube:id/channel_title"
        )
        for (id in candidateIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                val text = node.text?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    return text
                }
            }
        }
        return null
    }

    private fun launchBlockScreen(title: String, subtitle: String, blockedTarget: String) {
        val now = System.currentTimeMillis()
        if (now - lastBlockedTimestamp < BLOCK_COOLDOWN_MS) return
        lastBlockedTimestamp = now

        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("title", title)
            putExtra("subtitle", subtitle)
            putExtra("target", blockedTarget)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Required override
    }
}
