# Home Screen

An immersive Android homescreen replica built with Kotlin and Jetpack Compose, featuring a custom status bar, 3-button navigation bar, swipe-up app drawer, Play Store replica, and **Infinity AI** powered by Firebase AI Logic (`gemini-3.8-flash`).

## Pre-built APK

The debug APK is packaged in [`test/app-debug.apk`](test/app-debug.apk).

## Features

- **Immersive Home Screen**: Hides system bars and replicates the top status bar (live device time, battery level & charging indicator, Wi-Fi/cellular/internet status) and bottom 3-button navigation bar.
- **Long-Press Home Settings**: Long-press the home screen to open the settings menu with an **Exit App** action.
- **Swipe-Up App Drawer**: Swipe up to open the app drawer.
- **Play Store & Instant Install**: Open Play Store to install **Infinity AI** onto both the home screen and app drawer.
- **Infinity AI Chat**: Powered by Firebase AI Logic (`gemini-3.8-flash`), uninstalls Play Store on first launch using action blocks, pauses and resumes user chat, and supports multi-turn conversation.

## Building

Open the project in Android Studio or build from the command line:

```bash
gradle :app:assembleDebug
```
