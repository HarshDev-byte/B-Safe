# B-Safe Play Store Release Checklist

## 🔧 Prerequisites

### Development Environment
- [ ] **Java JDK 17+** installed and JAVA_HOME set
- [ ] **Android Studio** (recommended) or Android SDK
- [ ] **Gradle** (included via wrapper)

### Quick Setup (Windows)
```powershell
# Check Java installation
java -version

# If not installed, download from:
# https://adoptium.net/temurin/releases/?version=17

# Set JAVA_HOME (replace with your path)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot"
```

### Quick Setup (macOS/Linux)
```bash
# Check Java installation
java -version

# If not installed (macOS with Homebrew)
brew install openjdk@17

# Set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

---

## ✅ Completed

### Build Configuration
- [x] Release signing config in build.gradle.kts
- [x] ProGuard rules for code obfuscation
- [x] Minification and resource shrinking enabled
- [x] Version code: 1, Version name: 1.0.0
- [x] Target SDK 34 (Android 14)
- [x] Min SDK 26 (Android 8.0)

### App Resources
- [x] Adaptive launcher icons (ic_launcher, ic_launcher_round)
- [x] Launcher icon background color
- [x] Launcher foreground drawable
- [x] Mipmap folders for all densities
- [x] Widget preview image
- [x] App theme and colors
- [x] Strings localized (en, es, hi)

### Manifest & Permissions
- [x] All required permissions declared
- [x] Hardware features declared (required/optional)
- [x] Data extraction rules for backup
- [x] Services and receivers properly configured
- [x] Quick Settings tile configured

### Privacy & Security
- [x] Privacy Policy document
- [x] Data extraction rules (backup disabled for security)
- [x] Encrypted storage support
- [x] No PII in logs (release build)

### Play Store Metadata
- [x] App title (30 chars max)
- [x] Short description (80 chars max)
- [x] Full description (4000 chars max)
- [x] Changelog for v1.0.0

## ⏳ Manual Steps Required

### Before Upload
1. **Create Release Keystore**
   ```bash
   keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bsafe
   ```

2. **Configure keystore.properties**
   - Copy `keystore.properties.example` to `keystore.properties`
   - Fill in your keystore credentials
   - Never commit this file!

3. **Set up google-services.json**
   - Copy `google-services.json.example` to `google-services.json`
   - Replace with your Firebase project config

4. **Set MAPS_API_KEY in local.properties**
   ```properties
   MAPS_API_KEY=your_google_maps_api_key
   ```

### Build Release APK/AAB
```bash
./gradlew bundleRelease
# or
./gradlew assembleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### Play Store Console Requirements

#### Graphics Assets (Create these)
- [ ] App icon: 512x512 PNG
- [ ] Feature graphic: 1024x500 PNG
- [ ] Screenshots (phone): 2-8 images, min 320px, max 3840px
- [ ] Screenshots (tablet 7"): Optional but recommended
- [ ] Screenshots (tablet 10"): Optional but recommended

#### Store Listing
- [ ] Select app category: Tools or Lifestyle
- [ ] Content rating questionnaire
- [ ] Target audience and content
- [ ] Contact email and privacy policy URL

#### App Content
- [ ] Data safety form (declare data collection)
- [ ] Ads declaration (no ads)
- [ ] App access (if login required, provide test account)

### Testing Before Release
- [ ] Test all SOS trigger methods
- [ ] Verify SMS sending works
- [ ] Test location sharing accuracy
- [ ] Verify Firebase integration
- [ ] Test on multiple device sizes
- [ ] Test offline functionality
- [ ] Verify stealth mode works
- [ ] Test widget functionality

## 📝 Notes

- The app uses telephony features, so it will only be available on devices with phone capability
- Background location permission requires additional Play Store justification
- SMS permission requires Play Store policy declaration
- Consider staged rollout (10% → 50% → 100%)

## 📄 Documentation Created

- [x] RELEASE_CHECKLIST.md - This file
- [x] DATA_SAFETY_FORM.md - Play Store data safety guide
- [x] TERMS_OF_SERVICE.md - Legal terms
- [x] PRIVACY_POLICY.md - Privacy policy
- [x] LICENSE - MIT License
- [x] README.md - Comprehensive documentation

## 🔗 Useful Links

- [Play Console](https://play.google.com/console)
- [App Content Policy](https://play.google.com/about/developer-content-policy/)
- [Data Safety Form Guide](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Release Checklist](https://developer.android.com/distribute/best-practices/launch/launch-checklist)
