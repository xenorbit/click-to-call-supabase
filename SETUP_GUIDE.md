# Click-to-Call Supabase - Setup Guide

Complete guide for setting up the Supabase backend and deploying the app.

---

## Prerequisites

- Node.js 18+ installed
- Supabase CLI installed: `npm install -g supabase`
- Android Studio (for Android app)
- Chrome browser (for extension testing)

---

## 1. Supabase Project Setup

### 1.1 Create Project

1. Go to [supabase.com](https://supabase.com) → Sign in/Sign up
2. Click **"New Project"**
3. Enter:
   - Project name: `click-to-call`
   - Database Password: Generate a strong password (save it!)
   - Region: Choose closest to you
4. Click **"Create new project"** (wait ~2 min)

### 1.2 Get API Credentials

Go to **Project Settings → API** and copy:

| Key | Where to use |
|-----|--------------|
| **Project URL** | `https://xxxxx.supabase.co` → Extension & Android app |
| **anon public** | For extension & app |
| **service_role** | ⚠️ Keep secret! Only for Edge Functions |

### 1.3 Run Database Schema

1. Go to **SQL Editor** in Supabase dashboard
2. Click **"New Query"**
3. Paste contents of `supabase/migrations/001_initial_schema.sql`
4. Click **"Run"**

---

## 2. Edge Functions Deployment

### 2.1 Login to Supabase CLI

```bash
supabase login
```

### 2.2 Link Project

```bash
cd click-to-call-supabase/supabase
supabase link --project-ref YOUR_PROJECT_REF
```

(Get project ref from Project Settings → General)

### 2.3 Set FCM Server Key Secret

Get your FCM Server Key from Firebase Console:
- Go to Firebase Console → Project Settings → Cloud Messaging
- Copy the **Server Key** (Legacy)

```bash
supabase secrets set FCM_SERVER_KEY=your_server_key_here
```

### 2.4 Deploy Functions

```bash
supabase functions deploy register-device
supabase functions deploy pair-device
supabase functions deploy send-call-request
```

### 2.5 Verify Deployment

Test with curl:
```bash
curl -X POST https://YOUR_PROJECT_REF.supabase.co/functions/v1/pair-device \
  -H "Content-Type: application/json" \
  -d '{"pairingCode": "123456"}'
```

Should return: `{"error": "Invalid pairing code. No device found."}`

---

## 3. Chrome Extension Setup

### 3.1 Update Supabase Config

Edit `extension/utils/supabase-client.js`:

```javascript
export const SUPABASE_URL = 'https://YOUR_PROJECT_REF.supabase.co';
export const SUPABASE_ANON_KEY = 'YOUR_ANON_KEY_HERE';
```

### 3.2 Load Extension in Chrome

1. Open Chrome → `chrome://extensions`
2. Enable **"Developer mode"** (top right toggle)
3. Click **"Load unpacked"**
4. Select `click-to-call-supabase/extension` folder
5. Extension should appear with Click-to-Call icon

### 3.3 Verify Extension

1. Click extension icon
2. You should see "Not Connected" status
3. Pairing section should be visible

---

## 4. Android App Setup

### 4.1 Update Supabase URL

Edit `android-app/app/src/main/java/com/example/c2c/data/SupabaseRepository.kt`:

```kotlin
private const val SUPABASE_URL = "https://YOUR_PROJECT_REF.supabase.co"
```

### 4.2 Keep Firebase for FCM

The app still uses Firebase for push notifications only. You need:

1. Create/use existing Firebase project
2. Download `google-services.json`
3. Place in `android-app/app/google-services.json`

### 4.3 Build and Install

Open in Android Studio:

```bash
# Option 1: Android Studio
Open android-app folder in Android Studio
Click Run (green play button)

# Option 2: Command line
cd android-app
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. End-to-End Test

### Step 1: Generate Pairing Code
1. Open Android app
2. Tap **"Generate Code"**
3. Note the 6-digit code displayed

### Step 2: Pair Extension
1. Click Chrome extension icon
2. Enter the 6-digit code
3. Click **"Pair"**
4. Should show **"Connected"**

### Step 3: Test Call Request
1. Go to any webpage with a phone number
2. Select the phone number text
3. Right-click → **"Call from Device"**
4. Check Android for notification
5. Tap notification → Dialer opens with number

---

## Troubleshooting

### Extension: "Failed to pair device"
- Make sure you ran SQL schema in Supabase
- Verify Edge Functions are deployed
- Check pairing code matches exactly

### Android: "Failed to get device token"
- Ensure `google-services.json` is in `app/` folder
- Check internet connection
- Try on physical device (emulator may have issues)

### Push notification not received
- Verify FCM_SERVER_KEY is set in Supabase secrets
- Check FCM token is being registered (see Supabase logs)
- Test on physical device with good connection

### Edge Function errors
Check logs:
```bash
supabase functions logs register-device
supabase functions logs pair-device
supabase functions logs send-call-request
```

---

## Quick Reference

| Item | Location |
|------|----------|
| Supabase Project URL | `https://YOUR_REF.supabase.co` |
| Edge Functions | `supabase/functions/` |
| Chrome Extension | `extension/` |
| Android App | `android-app/` |
| Database Schema | `supabase/migrations/001_initial_schema.sql` |
