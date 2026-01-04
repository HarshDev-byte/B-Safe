# B-Safe Setup Guide

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Google Cloud Console account
- Firebase account

## Step 1: Clone & Open Project

```bash
git clone https://github.com/yourusername/B-Safe.git
cd B-Safe
```

Open in Android Studio.

## Step 2: Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing
3. Add Android app:
   - Package name: `com.safeguard.app`
   - Debug package: `com.safeguard.app.debug`
4. Download `google-services.json` → place in `app/` folder
5. Enable Authentication:
   - Go to Authentication → Sign-in method
   - Enable Google Sign-In
   - Copy the **Web Client ID**

## Step 3: Google Cloud Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your Firebase project
3. Enable APIs:
   - Maps SDK for Android
   - Places API
   - Directions API
4. Create API Key:
   - Go to Credentials → Create Credentials → API Key
   - Restrict to Android apps
   - Add your package name and SHA-1

## Step 4: Configure API Keys

Create `local.properties` in project root:

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=your_google_maps_api_key_here
```

Update `SignInScreen.kt` with your Web Client ID:

```kotlin
private const val WEB_CLIENT_ID = "your_web_client_id.apps.googleusercontent.com"
```

## Step 5: Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

## Step 6: Release Build Setup

### Generate Keystore

```bash
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bsafe
```

### Create keystore.properties

```properties
storeFile=../release-keystore.jks
storePassword=your_store_password
keyAlias=bsafe
keyPassword=your_key_password
```

### Build Release APK

```bash
./gradlew assembleRelease
```

APK location: `app/build/outputs/apk/release/app-release.apk`

## Step 7: Firebase Crashlytics (Optional)

1. In Firebase Console, enable Crashlytics
2. Build and run the app
3. Force a test crash to verify:

```kotlin
// Add temporarily to test
throw RuntimeException("Test Crash")
```

## Troubleshooting

### Google Sign-In Not Working
- Verify Web Client ID is correct
- Check SHA-1 fingerprint in Firebase
- Ensure Google Sign-In is enabled in Firebase Auth

### Maps Not Loading
- Verify MAPS_API_KEY in local.properties
- Check API is enabled in Google Cloud Console
- Verify API key restrictions

### Build Errors
- Sync Gradle files
- Invalidate caches: File → Invalidate Caches
- Check JDK version is 17

## Support

- GitHub Issues: [Report Bug](https://github.com/yourusername/B-Safe/issues)
- Email: support@bsafe-app.com
