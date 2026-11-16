# Home Screen

An immersive Android homescreen replica built with Kotlin and Jetpack Compose, featuring a custom status bar, 3-button navigation bar, swipe-up app drawer, Play Store replica, and standalone on-device **Infinity AI**.

## Pre-built APK

The debug APK is packaged in [`test/app-debug.apk`](test/app-debug.apk).

## Features

- **Immersive Home Screen**: Suppresses the host phone's system status bar and navigation bars, replicating the top status bar (live device time, battery level & charging indicator, Wi-Fi/cellular/internet status), pull-down shade, and bottom 3-button navigation bar.
- **Long-Press Controls**: Long-press any app icon (on Home Screen, Dock, or App Drawer) to **Open App**, **Move App** (reorder or move between Home Grid and Dock), **Remove from Home** / **Add to Home** (without removing from the App Drawer), or **Uninstall** completely; long-press empty Home Screen space to open Home Settings and Exit App.
- **Smooth Halfway-Pull App Drawer**: Pull halfway up with damped spring physics to smoothly reveal the App Drawer with optional live search, category tabs, and app counter; launching any app automatically closes the drawer.
- **Play Store & Instant Install**: Open Play Store to install **Infinity AI** onto both the home screen and app drawer.
- **Standalone Infinity AI, Local LLM Evolution & App Library**: Runs completely on-device without cloud dependencies. Early on, Infinity AI creates **Local LLM** on your home screen alongside **App Library** (12 working tools & games). Infinity AI guides your Local LLM through a 3-stage evolution (**Full-Fledged LLM** with 32K context & system prompt → **Multimodal AI** with Vision photo picker, Audio & Document modes → **Autonomous AGI Core** with reasoning traces & phone tool inspection), plus 100 total tiered upgrades while respecting disabled/uninstalled preferences, custom app forging, and ordered **AI Changes** (`system_changes_app.json`) rollback.

## Building

Open the project in Android Studio or build from the command line:

```bash
gradle :app:assembleDebug
```
