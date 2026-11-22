# FocusGain — Custom Regain-Style Study App for Samsung A34 5G

This folder contains the complete native Android app (**FocusGain**) designed for your Samsung Galaxy A34 5G, with:
- **App Blocker** (blocks any installed Android app you choose)
- **YouTube Study Mode** (blocks YouTube Shorts & all channels except the ones you add)
- **3-Mode Focus Timer** (Pomodoro, Countdown, and Stopwatch) + **Today's Total Study Time**
- **Strict Study Mode (Full Lock + Emergency Exit)** (locks settings/apps until timer ends, with a cooldown + typed pledge emergency exit)
- **Full Edit Tab** (edit timer durations, cooldowns, unlock phrase, app list, and YouTube channels anytime)

## How to Get the Installable `.apk` on Your Samsung A34 5G (Zero Coding Needed!)

### Method 1: Free Cloud Build via GitHub (Easiest — No PC Software Needed)
1. Create a free account at [github.com](https://github.com) and create a new repository (e.g., `focusgain-app`).
2. Unzip `regain-custom-android.zip` and upload all files (including the `.github` folder) to your repository.
3. Click the **Actions** tab in your GitHub repository -> select **Build FocusGain APK** -> click **Run workflow**.
4. After ~2 minutes, download **FocusGain-APK** (`app-debug.apk`) directly onto your Samsung A34 5G and tap to install (enable *Install unknown apps* when prompted).

### Method 2: Android Studio (If Using a PC/Laptop)
1. Open [Android Studio](https://developer.android.com/studio) -> **Open** -> select the `regain-custom-android` folder.
2. Click **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
3. Transfer `app-debug.apk` to your Samsung A34 5G and install it.

## First-Time Setup on Your Samsung A34 5G
1. Open **FocusGain** -> **Apps** tab -> tap **Enable Accessibility Guard** -> find **FocusGain App & YouTube Study Guard** in Installed Apps and turn it **ON**.
2. Grant **Appear on top** permission when prompted so the block screen can cover distracting apps.
3. Add the apps you want to block in the **Apps** tab and your study YouTube channels in the **YouTube** tab.
