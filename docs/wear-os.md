# Wear OS Remote Controls Design Note

## Overview

Evenfall is an offline-first, privacy-respecting native Android audiobook player. This document outlines the architectural blueprint and design specifications for extending playback controls to **Wear OS smartwatches**.

In accordance with Evenfall's core tenets:
- **Zero Network / Cloud Dependency**: The Wear OS remote communicates exclusively via local on-device APIs (Media3 `MediaController` and Bluetooth MediaSession routing). No network permission, cloud account, or external server is involved.
- **Battery & Screen Efficiency**: Designed specifically for circular AMOLED displays with pure black backgrounds (`#000000`), minimal CPU wakeups, and native Ambient Mode support.

---

## 1. System Architecture

```
┌────────────────────────────────────────────────────────┐
│                   Wear OS Companion                    │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │     Wear OS Compose UI (NowPlayingScreen)        │  │
│  │   - Title, Chapter, Progress arc                 │  │
│  │   - Play/Pause, Skip Back/Forward, Chapters      │  │
│  │   - Rotary crown input for seeking/volume        │  │
│  └────────────────────────▲─────────────────────────┘  │
│                           │                            │
│  ┌────────────────────────┴─────────────────────────┐  │
│  │             Media3 MediaController               │  │
│  │   - Connects via SessionToken                    │  │
│  │   - Listens to Player.Listener events            │  │
│  └────────────────────────▲─────────────────────────┘  │
└───────────────────────────┼────────────────────────────┘
                            │ (Local IPC / Bluetooth MediaSession)
┌───────────────────────────▼────────────────────────────┐
│                    Phone Application                   │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │        EvenfallMediaService (Media3)             │  │
│  │   - MediaSession & MediaLibraryService           │  │
│  │   - PlaybackManager & ExoPlayer Engine           │  │
│  │   - Room Database (Positions, History, Stats)    │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### Media3 Session Controller Integration
Instead of creating custom Bluetooth protocols or proprietary sync services:
1. The Wear OS application utilizes Media3's standard `androidx.media3.session.MediaController`.
2. When the phone is playing or has a session registered, `EvenfallMediaService` exposes a standard `MediaLibrarySession`.
3. The Wear OS app discovers the session via Android's `MediaSessionManager` or binds directly via `SessionToken(context, ComponentName("com.evenfall.app", "com.evenfall.app.playback.EvenfallMediaService"))`.
4. Command execution is handled entirely through standard `Player` APIs:
   - `controller.play()` / `controller.pause()`
   - `controller.seekBack()` / `controller.seekForward()`
   - `controller.seekToNextMediaItem()` / `controller.seekToPreviousMediaItem()`

---

## 2. Wear OS UI Specifications

### Screen Layout (Circular Display)
- **Top Arc**: Curved chapter progress indicator (`CurvedLayout` / Canvas sweep gradient).
- **Upper Center**: Audiobook title (single line, marquee scrolling if overflowing) and current chapter name in secondary font.
- **Center Hero**: 48dp+ Play/Pause button with morphing iconography.
- **Left / Right Flanks**: Circular skip backward (e.g. 20s) and skip forward (e.g. 20s) buttons with large touch targets.
- **Bottom**: Current playback speed badge (e.g. `1.2x`) and elapsed time / remaining time.

### Rotary Crown Support
- Rotating the physical crown scrolls seeking or skip increments smoothly.
- Crown gestures mapped through `Modifier.rotaryScrollable`.

---

## 3. Ambient Mode & Battery Conservation

- **Ambient Mode**: When the user's wrist drops or ambient mode triggers, the watch face simplifies:
  - Disables animated progress updates.
  - Switches to a static outline display with zero background render passes.
  - Updates position only once per minute to preserve battery.
- **Connection Lifecycle**:
  - The `MediaController` connects in `onStart()` / `onResume()` and cleanly releases in `onStop()`, avoiding holding wake locks on either the watch or phone.

---

## 4. Standalone Offline Playback Considerations (Future Milestone)

If future releases introduce direct audio playback on the watch (for running without a phone):
- SAF folder sync via Android Nearby Connections or local Wi-Fi Direct file push.
- Embedded SQLite/Room mirror for storing playback positions.
- Direct Bluetooth headphone pairing via Wear OS `AudioManager`.
