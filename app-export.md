# How to Export Your Android App (APK)

This guide will walk you through creating an APK file from your Android Studio project. There are two ways to do this:
1. **Debug APK**: Fast, for testing on your own device.
2. **Signed Release APK**: Secure, for sharing with others or publishing.

---

## Option 1: Quick Export (Debug APK)
*Best for: Quick testing on your own phone.*

1. Open your project in **Android Studio**.
2. In the top menu, go to **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
3. Wait for the build process to finish (progress bar at the bottom right).
4. A notification will appear: "Build APK(s): APK(s) generated successfully."
5. Click **locate** in that notification.
   - *If you missed the notification:* Navigate manually to `click-to-call-supabase\android-app\app\build\outputs\apk\debug\`.
6. You will see `app-debug.apk`. You can copy this file to your phone and install it.

---

## Option 2: Signed Export (Release APK)
*Best for: Sharing the app with others or publishing to the Play Store.*

This process involves "signing" the app with a digital key so Android knows it's safe and authentic.

### Step 1: Start the Wizard
1. In the top menu, go to **Build** > **Generate Signed Bundle / APK...**.
2. Select **APK** and click **Next**.

### Step 2: Create a Key Store (First Time Only)
*If you have never created a key before, follow these steps. If you already have one, skip to Step 3.*

1. Under "Key store path", click **Create new...**.
2. **Key store path**: Click the folder icon. Save it in your project folder (e.g., inside `android-app`) and name it `keystore.jks`. Click **OK**.
3. **Password**: Create a password (e.g., `mysecurepassword`) and confirm it. **Remember this password!**
4. **Key > Alias**: You can leave this as `key0` or name it `c2c_key`.
5. **Key > Password**: Enter a password for the key itself (can be the same as the store password).
6. **Certificate**: Fill in at least one field, like "First and Last Name" (e.g., "Admin").
7. Click **OK**.

### Step 3: Enter Key Details
1. Back in the "Generate Signed Bundle or APK" window, the fields should now be filled.
2. If not, click **Choose existing...** and select the `keystore.jks` file you just created.
3. Enter the **Key store password** and **Key password** you set in Step 2.
4. Check the boxes for **Remember passwords** to save time later.
5. Click **Next**.

### Step 4: Build
1. Select **release**.
2. Click **Create** (or **Finish**).
3. Wait for the build to complete.
4. Click **locate** in the success notification.
   - *Manual location:* `click-to-call-supabase\android-app\app\release\`.
5. You will see `app-release.apk`. This is your final file!

---

## How to Install on Your Phone
1. Transfer the `.apk` file to your phone (via USB, Google Drive, WhatsApp, etc.).
2. On your phone, tap the file to open it.
3. If prompted, allow installation from "Unknown Sources" (this is normal for apps not from the Play Store).
4. Tap **Install**.

### Troubleshooting
*   **"App not installed" error**: If you have the "Debug" version installed and try to install the "Release" version, it might fail. **Uninstall the old version from your phone first**, then install the new one.
*   **Build Errors**: If the build fails, go to **Build** > **Clean Project**, wait a moment, then try again.
