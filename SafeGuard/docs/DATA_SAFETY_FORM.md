# B-Safe Data Safety Form - Play Store

Use this guide when filling out the Data Safety form in Google Play Console.

## Overview

| Question | Answer |
|----------|--------|
| Does your app collect or share any of the required user data types? | Yes |
| Is all of the user data collected by your app encrypted in transit? | Yes |
| Do you provide a way for users to request that their data is deleted? | Yes |

---

## Data Types Collected

### 1. Location
| Field | Value |
|-------|-------|
| Collected | Yes |
| Shared | No (only with user-designated emergency contacts) |
| Ephemeral | No |
| Required | Yes |
| Purpose | App functionality (emergency location sharing) |

**Explanation**: Location is collected only during active SOS emergencies to share with the user's designated emergency contacts.

### 2. Personal Info - Name
| Field | Value |
|-------|-------|
| Collected | Yes |
| Shared | No |
| Ephemeral | No |
| Required | No |
| Purpose | App functionality |

**Explanation**: User's name is optionally collected for personalization and inclusion in emergency messages.

### 3. Personal Info - Phone Number
| Field | Value |
|-------|-------|
| Collected | Yes |
| Shared | No |
| Ephemeral | No |
| Required | Yes |
| Purpose | App functionality |

**Explanation**: Emergency contact phone numbers are collected to send SMS alerts during emergencies.

### 4. Personal Info - Email Address
| Field | Value |
|-------|-------|
| Collected | Yes |
| Shared | No |
| Ephemeral | No |
| Required | No |
| Purpose | App functionality, Account management |

**Explanation**: Email is collected for Google Sign-In (optional) and email-based emergency alerts.

### 5. Health Info - Medical Info
| Field | Value |
|-------|-------|
| Collected | Yes |
| Shared | No (only with user-designated emergency contacts) |
| Ephemeral | No |
| Required | No |
| Purpose | App functionality |

**Explanation**: Optional medical information (blood type, allergies, conditions) can be included in emergency messages.

### 6. Contacts
| Field | Value |
|-------|-------|
| Collected | Yes |
| Shared | No |
| Ephemeral | No |
| Required | Yes |
| Purpose | App functionality |

**Explanation**: Emergency contact information is stored locally to send alerts during emergencies.

### 7. App Activity - App Interactions
| Field | Value |
|-------|-------|
| Collected | Yes |
| Shared | No |
| Ephemeral | No |
| Required | No |
| Purpose | Analytics |

**Explanation**: Anonymous app usage analytics via Firebase Analytics to improve the app.

### 8. App Info and Performance - Crash Logs
| Field | Value |
|-------|-------|
| Collected | Yes |
| Shared | Yes (with Firebase Crashlytics) |
| Ephemeral | No |
| Required | No |
| Purpose | Analytics |

**Explanation**: Crash reports are collected via Firebase Crashlytics to fix bugs and improve stability.

### 9. Device or Other IDs
| Field | Value |
|-------|-------|
| Collected | Yes |
| Shared | Yes (with Firebase) |
| Ephemeral | No |
| Required | No |
| Purpose | Analytics |

**Explanation**: Anonymous device identifiers for Firebase Analytics.

---

## Data NOT Collected

- Financial info (payment info, purchase history)
- Messages (emails, SMS, other messages)
- Photos or videos
- Audio files (recordings are stored locally only)
- Files and docs
- Calendar
- Web browsing history
- Search history

---

## Security Practices

| Practice | Implemented |
|----------|-------------|
| Data encrypted in transit | ✅ Yes (HTTPS) |
| Data encrypted at rest | ✅ Yes (optional encrypted storage) |
| Users can request data deletion | ✅ Yes (uninstall or in-app delete) |
| Committed to Play Families Policy | ❌ No (not a kids app) |
| Independent security review | ❌ No |

---

## Data Deletion

Users can delete their data by:
1. **Uninstalling the app** - All local data is deleted
2. **In-app deletion** - Settings > Privacy > Delete All History
3. **Account deletion** - Sign out and delete account in Profile settings

---

## Third-Party Services

| Service | Data Shared | Purpose |
|---------|-------------|---------|
| Firebase Authentication | Email, User ID | Account management |
| Firebase Firestore | Encrypted contacts, settings | Cloud backup (optional) |
| Firebase Analytics | Anonymous usage data | Analytics |
| Firebase Crashlytics | Crash logs, device info | Bug fixing |
| Google Maps | Location queries | Map display |
| Google Places | Location queries | Nearby places |

---

## Notes for Review

1. **SMS Permission**: Required to send emergency SMS alerts to user-designated contacts. This is the core functionality of the app.

2. **Background Location**: Required to share location during active SOS emergencies. Location is NOT tracked when SOS is inactive.

3. **Call Permission**: Required to auto-dial emergency contacts during SOS events.

4. **Microphone Permission**: Optional, used only for voice-activated SOS commands.

5. **Camera Permission**: Used only for flashlight SOS signals, not for capturing photos/videos.

---

## Contact

For data privacy inquiries:
- Email: privacy@bsafe-app.com
- Privacy Policy: https://bsafe-app.com/privacy
