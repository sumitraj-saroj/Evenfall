# Evenfall 🌙

> A calm, offline-first native Android audiobook player for local files.

Evenfall is an elegant, privacy-first audiobook player built specifically for listening to local audio files. It scans your chosen folder, organizes multi-file and single-file audiobooks, extracts chapter metadata, remembers exact playback positions continuously, and provides a tranquil listening experience with Material 3 Expressive design, dynamic book-matching themes, and zero tracking.

---

## 🌟 Key Features

- **100% Offline & Private**: Zero network permission (`android.permission.INTERNET` is omitted). No telemetry, no ads, no cloud logins.
- **Smart Folder Scanning**: Storage Access Framework (SAF) folder picker with persisted permissions. Supports both single-file M4B/M4A audiobooks with chapter markers and multi-file books ordered naturally.
- **Continuous Position Memory**: Automatically records playback positions every few seconds, on pause, and on process death so you never lose your place.
- **Material 3 Expressive UI**: Features cookie/scalloped shapes, morphing play/pause buttons, and smooth wavy progress indicators.
- **Dynamic Book Theming**: Dynamically extracts colors from book cover artwork to theme the player in light and dark modes (e.g. amber for sunset covers, deep blue for space covers).
- **Audiobook-Tuned Playback (Media3 & ExoPlayer)**:
  - Speech-optimized audio focus (pause on incoming calls, ducking, logging interruption length)
  - Variable speed playback (0.5x – 3.0x with preserved pitch) and per-book speed memory
  - Silence skipping powered by Media3 audio processor
  - Volume boost via `LoudnessEnhancer`
  - Auto-rewind after pause based on pause duration
  - Sleep timer with smooth volume fade-out and accelerometer shake-to-extend
- **Rich Bookmarks & Listening History**:
  - Chapter-grouped bookmarks with custom tags (Notes, Favorites, Quotes, Moments)
  - Visual timeline with bookmark markers and "You are here" indicator
  - Comprehensive history tracking (Played, Paused, Interrupted, Sleep Timer, Shake to Extend) with one-tap position restoration
- **Home Screen Widgets (Jetpack Glance)**:
  - Large Now Playing widget with cookie cover, wavy progress bar, and media controls
  - Continue Listening shelf widget with recent book covers and instant play
- **Android Auto**:
  - Driver-safe MediaLibraryService with browsable library (Continue Listening, All Books, Chapters)

---

## 🏗️ Architecture & Tech Stack

Evenfall follows Android modern development best practices: Single Activity architecture, MVVM with Unidirectional Data Flow (UDF), and clean modular separation.

- **Language & Runtime**: Kotlin 2.3+, Coroutines, StateFlow / SharedFlow
- **UI**: Jetpack Compose, Material 3, Material 3 Expressive shapes & animations
- **Media Playback**: AndroidX Media3 (`media3-exoplayer`, `media3-session`, `media3-ui`) with `MediaLibraryService`
- **Database & Storage**:
  - Room 2.7+ (Books, Chapters, Bookmarks, History events)
  - Jetpack DataStore (User preferences and playback settings)
  - Storage Access Framework (SAF) for persistent tree URIs
- **Dependency Injection**: Hilt (Dagger)
- **Image Loading**: Coil Compose
- **Home Screen Widgets**: Jetpack Glance
- **Testing**: JUnit 4, Kotlinx Coroutines Test, Compose UI Testing

```
app/src/main/java/com/evenfall/app/
├── data/
│   ├── db/              # Room database, entities (Book, Chapter, Bookmark, History), DAOs
│   ├── datastore/       # DataStore preferences & settings repository
│   ├── parser/          # Audio metadata & chapter extractor (M4B, MP3, Vorbis, AAC)
│   ├── repository/      # Library, bookmark, history, and playback repositories
│   └── scanner/         # SAF DocumentFile audiobook library scanner
├── di/                  # Hilt dependency injection modules
├── domain/              # Domain models, state definitions, business logic
├── playback/            # Media3 MediaLibraryService, ExoPlayer wrapper, sleep timer, auto-rewind
├── ui/
│   ├── components/      # CookieShape, WavyProgress, MorphButton, SeekUndoChip
│   ├── navigation/      # Compose Navigation graph and destinations
│   ├── screens/
│   │   ├── onboarding/  # Add folder & scan preview (Image 8)
│   │   ├── home/        # Library, greeting, continue listening hero card (Image 1)
│   │   ├── player/      # Now playing screen, dynamic theme (Images 2 & 3)
│   │   ├── bookmarks/   # Bookmarks and History timeline screens (Images 4 & 5)
│   │   └── settings/    # Appearance, listening settings, about (Image 7)
│   └── theme/           # Evenfall color schemes, serif typography, expressive shapes
└── widget/              # Jetpack Glance widgets (Image 6)
```

---

## 🛠️ Build & Run

### Prerequisites
- JDK 17 or JDK 21
- Android SDK with platform `android-35` or `android-36` and Build Tools 35.0.0+

### Setup `local.properties`
Create `local.properties` in the root directory:
```properties
sdk.dir=/path/to/your/android/sdk
```

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Install on connected device/emulator
./gradlew installDebug
```

---

## 📄 License
Open source under the Apache License 2.0.
