# Click-to-Call Android App

An Android app that receives phone number notifications from the Chrome extension and opens the dialer.

## Features

- **FCM Integration**: Receives push notifications with phone numbers
- **One-tap Dial**: Tap notification to open phone dialer with number pre-filled
- **Device Pairing**: 6-digit code to pair with Chrome extension
- **Settings**: Toggle auto-dial and notification preferences

## Setup

### Prerequisites

1. Android Studio (2024.1+)
2. Firebase project with FCM enabled
3. `google-services.json` from Firebase Console

### Firebase Configuration

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project or select existing
3. Add Android app with package name `com.example.c2c`
4. Download `google-services.json`
5. Place it in `app/` directory (next to `build.gradle.kts`)

### Build & Run

1. Open project in Android Studio
2. Add `google-services.json` to `app/` folder
3. Sync Gradle files
4. Run on device (FCM requires physical device or emulator with Play Services)

## Project Structure

```
app/src/main/java/com/example/c2c/
├── MainActivity.kt                 # Entry point with Compose
├── C2CApplication.kt               # Application class
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   └── Navigation.kt
│   └── screens/
│       ├── HomeScreen.kt           # Main dashboard with pairing
│       └── SettingsScreen.kt       # App settings
├── service/
│   └── CallNotificationService.kt  # FCM message handler
├── receiver/
│   └── CallActionReceiver.kt       # Notification actions
├── data/
│   └── PreferencesManager.kt       # DataStore preferences
└── util/
    └── DialerHelper.kt             # Phone dialer utilities
```

## How It Works

1. User pairs app with Chrome extension using 6-digit code
2. FCM token is registered with backend
3. When user clicks "Call from Device" in Chrome:
   - Extension sends request to Firebase
   - Firebase sends FCM push notification
   - App shows notification with phone number
   - User taps notification to open dialer

## Permissions

- `INTERNET` - Required for FCM
- `POST_NOTIFICATIONS` - Required for Android 13+ notifications
- `CALL_PHONE` - Optional, for future auto-call feature

## License

MIT
