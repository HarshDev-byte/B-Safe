# B-Safe - Emergency Safety Application

<p align="center">
  <img src="docs/logo.png" alt="B-Safe Logo" width="200"/>
</p>

<p align="center">
  <strong>Your Personal Safety Companion</strong><br>
  A last-resort lifeline for real-world emergency situations
</p>

<p align="center">
  <a href="#problem-statement">Problem</a> •
  <a href="#solution">Solution</a> •
  <a href="#features">Features</a> •
  <a href="#google-technologies">Google Tech</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#installation">Installation</a>
</p>

---

## 📋 Table of Contents

1. [Problem Statement](#problem-statement)
2. [Solution Overview](#solution)
3. [How We're Different](#differentiation)
4. [Features](#features)
5. [Google Technologies Used](#google-technologies)
6. [Process Flow](#process-flow)
7. [Architecture](#architecture)
8. [Screenshots](#screenshots)
9. [Future Roadmap](#roadmap)
10. [Links](#links)

---

## 🎯 Problem Statement

Personal safety remains a critical concern globally, especially in emergency situations where victims **cannot interact normally with their phones** (assault, kidnapping, medical emergencies, accidents).

**Existing solutions fail because they:**
- ❌ Require internet connectivity to function
- ❌ Need complex interactions during emergencies
- ❌ Lack discretion (attackers can see the app)
- ❌ Don't work offline or in low-connectivity areas
- ❌ Have limited trigger mechanisms (usually just a button)
- ❌ Send location only once, not continuously
- ❌ Store data on external servers (privacy concerns)

---

## 💡 Solution

**B-Safe** is a comprehensive emergency safety application designed as a **last-resort lifeline** that works even when users cannot interact normally with their phone.

### Core Principles
- 🔌 **Offline First** - Full functionality without internet
- 🎭 **Discreet** - Stealth mode disguises as calculator
- ⚡ **Fast** - Multiple trigger methods for instant activation
- 🔒 **Private** - 100% local processing, no external servers

---

## 🆚 Differentiation

| Aspect | Existing Apps | B-Safe |
|--------|---------------|--------|
| **Offline Capability** | Most require internet | ✅ Full offline SMS alerts, siren, flashlight |
| **Trigger Methods** | Single button | ✅ 6+ methods: Volume, shake, voice, widget, wearable |
| **Discretion** | Visible as safety app | ✅ Stealth mode (calculator disguise) |
| **Privacy** | Cloud-dependent | ✅ 100% local, no external servers |
| **Multi-channel Alerts** | SMS or internet only | ✅ SMS + Email + Push simultaneously |
| **Location Updates** | Single share | ✅ Periodic live updates during SOS |
| **Accessibility** | Limited | ✅ WCAG 2.1 AA, 10+ languages |
| **Evidence Collection** | None | ✅ Audio recording during SOS |

---

## ✨ Features

### 🆘 Multi-Modal SOS Triggers

| Trigger Method | Description | Works Offline |
|----------------|-------------|:-------------:|
| **Volume Button Sequence** | Customizable pattern (e.g., UP→UP→DOWN→DOWN) | ✅ |
| **Shake Detection** | Vigorous shaking with configurable sensitivity | ✅ |
| **Power Button Pattern** | Multiple rapid presses | ✅ |
| **Voice Activation** | Say "Help me" or "Emergency" in 10+ languages | ✅ |
| **Widget/Lock Screen** | One-tap SOS from home or lock screen | ✅ |
| **Wearable Integration** | Trigger from smartwatch or panic button | ✅ |

### 📱 Emergency Protocol

When SOS is activated, B-Safe executes:

1. **📨 Instant SMS Alerts** - Sends to all contacts with:
   - 📍 GPS coordinates + Google Maps link
   - 🕐 Timestamp
   - 🔋 Battery level
   - 🏥 Medical info (if enabled)

2. **📞 Auto-Call** - Dials emergency contacts or 911/112/999

3. **🔊 Siren & Flashlight** - Loud alarm + SOS morse code flash

4. **📍 Periodic Location Updates** - Continues sending until cancelled

5. **🎤 Audio Evidence** - Records audio during active SOS

6. **🌐 Internet Alerts** - Email + push notifications when online

### 🛡️ Safety Features

| Feature | Description |
|---------|-------------|
| **Fake Call Mode** | Simulate incoming calls to exit uncomfortable situations |
| **Stealth Mode** | Disguise app as calculator with secret PIN access |
| **Danger Zone Alerts** | Geofencing notifications for marked areas |
| **Scheduled Check-ins** | Auto-alert contacts if you miss a check-in |
| **Journey Monitoring** | Track trips with auto-alert if you don't arrive on time |
| **Safety Dashboard** | AI-powered insights and recommendations |
| **Safety Score** | Gamified protection score (0-100) with improvement tips |
| **Audio Evidence** | Automatic audio recording during active SOS |
| **Quick Escape** | One-tap navigation to nearest police/hospital |

### 🗺️ Google Maps Integration

| Feature | Description |
|---------|-------------|
| **Nearby Safe Places** | Find police stations, hospitals, fire stations, pharmacies |
| **Live Location Sharing** | Real-time location with emergency contacts |
| **Safe Route Navigation** | Directions to nearest safe place with ETA |
| **Danger Zone Visualization** | Interactive map for danger zones |
| **Place Details** | Ratings, hours, phone numbers, directions |

### 🌍 Global Support

- **10+ Languages** - English, Spanish, Hindi, French, German, Chinese, Arabic, Portuguese, Japanese, Korean
- **Regional Emergency Numbers** - Pre-configured for 50+ countries
- **Advanced Mobile Location (AML)** - Enhanced accuracy for emergency services

---

## 🔧 Google Technologies Used

| Technology | Purpose |
|------------|---------|
| **Google Maps SDK** | Interactive maps, location visualization, danger zones |
| **Google Maps Compose** | Jetpack Compose integration for maps |
| **Google Places API** | Nearby safe places search (police, hospitals, etc.) |
| **Google Play Services Location** | High-accuracy GPS location services |
| **Firebase Authentication** | Google Sign-In for user authentication |
| **Firebase Firestore** | Cloud database for user profiles |
| **Firebase Cloud Messaging** | Push notifications to emergency contacts |
| **Firebase Analytics** | Usage analytics |
| **Material Design 3** | Modern UI components and theming |

---

## � Procesos Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     B-SAFE EMERGENCY FLOW                        │
└─────────────────────────────────────────────────────────────────┘

     ┌──────────┐         ┌──────────┐         ┌──────────┐
     │  Setup   │         │  Ready   │         │ Trigger  │
     │ Contacts │────────▶│  State   │────────▶│ Detected │
     │& Settings│         │          │         │          │
     └──────────┘         └──────────┘         └────┬─────┘
                                                    │
                          ┌─────────────────────────┼─────────────────────────┐
                          │                         │                         │
                          ▼                         ▼                         ▼
                   ┌────────────┐           ┌────────────┐           ┌────────────┐
                   │  Volume    │           │   Shake/   │           │  Widget/   │
                   │  Buttons   │           │   Voice    │           │  Wearable  │
                   └─────┬──────┘           └─────┬──────┘           └─────┬──────┘
                         │                        │                        │
                         └────────────────────────┼────────────────────────┘
                                                  │
                                                  ▼
                                        ┌─────────────────┐
                                        │   COUNTDOWN     │
                                        │   (5 seconds)   │
                                        └────────┬────────┘
                                                 │
                               ┌─────────────────┴─────────────────┐
                               │                                   │
                               ▼                                   ▼
                     ┌─────────────────┐                 ┌─────────────────┐
                     │  User Cancels   │                 │  SOS ACTIVATED  │
                     │  (False Alarm)  │                 │                 │
                     └─────────────────┘                 └────────┬────────┘
                                                                  │
              ┌───────────────┬───────────────┬───────────────┬───┴───────────┐
              │               │               │               │               │
              ▼               ▼               ▼               ▼               ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
        │   SMS    │   │  Email/  │   │  Auto    │   │  Siren   │   │  Audio   │
        │  Alerts  │   │   Push   │   │  Call    │   │ +Flash   │   │ Record   │
        │ +GPS Link│   │  Notifs  │   │          │   │          │   │          │
        └──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘
                                                                          │
                                                                          ▼
                                                              ┌─────────────────────┐
                                                              │  Periodic Location  │
                                                              │  Updates Continue   │
                                                              │  Until Cancelled    │
                                                              └─────────────────────┘
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            PRESENTATION LAYER                                │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    Jetpack Compose UI (Material 3)                     │  │
│  │  HomeScreen │ ContactsScreen │ MapsScreen │ SettingsScreen │ History  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                         ViewModels (MVVM)                              │  │
│  │  MainViewModel │ ContactsViewModel │ PlacesViewModel │ SettingsVM     │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              DOMAIN LAYER                                    │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                           Core Managers                                │  │
│  │  SOSManager │ LocationManager │ AlertManager │ TriggerDetector        │  │
│  │  PlacesManager │ VoiceActivation │ WearableManager │ JourneyMonitor   │  │
│  │  AudioEvidenceManager │ SafetyAnalytics │ InternetAlertManager        │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               DATA LAYER                                     │
│  ┌─────────────────────────────┐  ┌─────────────────────────────────────┐   │
│  │       Local Storage         │  │        Remote Services              │   │
│  │  ┌───────────────────────┐  │  │  ┌───────────────────────────────┐  │   │
│  │  │ Room Database         │  │  │  │ Firebase Auth (Google Sign-In)│  │   │
│  │  │ (Contacts, Events,    │  │  │  │ Firebase Firestore            │  │   │
│  │  │  Settings, History)   │  │  │  │ Google Places API             │  │   │
│  │  └───────────────────────┘  │  │  │ Google Maps SDK               │  │   │
│  │  ┌───────────────────────┐  │  │  └───────────────────────────────┘  │   │
│  │  │ DataStore (Encrypted) │  │  │                                     │   │
│  │  └───────────────────────┘  │  │                                     │   │
│  └─────────────────────────────┘  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          BACKGROUND SERVICES                                 │
│  SOSForegroundService │ TriggerDetectionService │ LocationTrackingService   │
│  BootReceiver │ VolumeButtonReceiver │ SOSWidgetProvider                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Project Structure

```
SafeGuard/
├── app/src/main/java/com/safeguard/app/
│   ├── auth/                    # Authentication
│   │   └── AuthManager.kt       # Firebase/Google Sign-In
│   ├── core/                    # Core business logic
│   │   ├── SOSManager.kt        # SOS orchestration
│   │   ├── LocationManager.kt   # GPS services
│   │   ├── AlertManager.kt      # Siren/flashlight
│   │   ├── TriggerDetector.kt   # Hardware triggers
│   │   ├── PlacesManager.kt     # Google Places API
│   │   ├── JourneyMonitor.kt    # Trip monitoring
│   │   ├── AudioEvidenceManager.kt # Audio recording
│   │   ├── SafetyScoreManager.kt # Gamified safety score
│   │   ├── QuickEscapeManager.kt # Quick escape features
│   │   └── VoiceActivationManager.kt
│   ├── data/
│   │   ├── models/              # Data classes
│   │   ├── local/               # Room + DataStore
│   │   └── repository/          # Repository pattern
│   ├── services/                # Foreground services
│   ├── receivers/               # Broadcast receivers
│   ├── widgets/                 # Home screen widgets
│   └── ui/
│       ├── screens/             # Compose screens
│       ├── components/          # Reusable components
│       ├── viewmodels/          # MVVM ViewModels
│       └── theme/               # Material 3 theming
└── app/src/test/                # Unit tests
```

---

## 📱 Screenshots

| Home Screen | SOS Active | Nearby Places |
|:-----------:|:----------:|:-------------:|
| Large SOS button with quick actions | Real-time status during emergency | Google Maps with safe locations |

| Contacts | Stealth Mode | Settings |
|:--------:|:------------:|:--------:|
| Emergency contacts management | Calculator disguise | SOS & trigger configuration |

---

## 🛠️ Technology Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin 100% |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Clean Architecture |
| **Database** | Room with encrypted storage |
| **Preferences** | DataStore (encrypted) |
| **Async** | Kotlin Coroutines + Flow |
| **Background** | Foreground Services + WorkManager |
| **Location** | Google Play Services Location |
| **Maps** | Google Maps SDK + Maps Compose |
| **Places** | Google Places SDK |
| **Auth** | Firebase Auth + Google Sign-In |
| **Testing** | JUnit + Espresso + Compose Testing |

---

## 🚀 Installation

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Kotlin 1.9+
- Physical device recommended (emulator has limited sensor support)

### Build from Source

```bash
# Clone repository
git clone https://github.com/yourusername/safeguard-android.git
cd safeguard-android

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### API Keys Setup

#### Google Maps & Places API
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Enable: Maps SDK for Android, Places API, Directions API
3. Create API Key and restrict to your package name
4. Add to `local.properties`:
   ```properties
   MAPS_API_KEY=your_api_key_here
   ```

#### Firebase Setup
1. Create project at [Firebase Console](https://console.firebase.google.com/)
2. Add Android app with package `com.safeguard.app`
3. Download `google-services.json` to `app/` folder
4. Enable Authentication → Google Sign-In

---

## 🗺️ Roadmap

### Version 2.0
- [ ] Wear OS companion app
- [ ] Bluetooth panic button support
- [ ] Campus security integration

### Version 2.5
- [ ] Government emergency API integration
- [ ] Satellite SOS (Android 14+)
- [ ] AI-powered threat detection
- [ ] Crowd-sourced safety maps

### Version 3.0
- [ ] Cross-platform iOS (Kotlin Multiplatform)
- [ ] Family safety network
- [ ] Smart home integration
- [ ] Emergency responder dashboard

---

## 🔒 Privacy

| Feature | Implementation |
|---------|----------------|
| **Local Processing** | All critical operations on-device |
| **No Tracking** | Location only accessed during active SOS |
| **Encrypted Storage** | AES-256 encryption for sensitive data |
| **No External Servers** | Zero data transmission to third parties |
| **Auto-Delete** | Configurable automatic log cleanup |

---

## 📋 Permissions

| Permission | Purpose | Required |
|------------|---------|:--------:|
| `SEND_SMS` | Emergency SMS alerts | ✅ |
| `CALL_PHONE` | Auto-dial emergency contacts | Optional |
| `ACCESS_FINE_LOCATION` | Share precise location | ✅ |
| `ACCESS_BACKGROUND_LOCATION` | Track during SOS | ✅ |
| `RECORD_AUDIO` | Voice activation & evidence | Optional |
| `CAMERA` | Flashlight control | Optional |
| `POST_NOTIFICATIONS` | Status notifications | ✅ |

---

## 🔗 Links

| Resource | Link |
|----------|------|
| **Repository** | [GitHub](https://github.com/yourusername/safeguard-android) |
| **Demo Video** | [YouTube](#) |
| **APK Download** | [Releases](#) |

---

## 🤝 Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <strong>B-Safe - Because everyone deserves to feel safe.</strong>
</p>

<p align="center">
  <sub>Built with ❤️ for a safer world</sub>
</p>
