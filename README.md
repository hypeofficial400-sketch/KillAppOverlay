# KillApp Overlay

Floating **⛔ Kill App** button that appears when you open Recents on XOS 13.

---

## How to Build (Phone only — GitHub Actions)

### Step 1 — Create GitHub repo
1. Open [github.com](https://github.com) on your phone browser
2. Tap **+** → **New repository**
3. Name it: `KillAppOverlay`
4. Set to **Public**
5. Tap **Create repository**

### Step 2 — Upload the files
In your new repo, upload files **in this order** (maintain folder structure):

```
.github/workflows/build.yml
app/build.gradle
app/src/main/AndroidManifest.xml
app/src/main/java/com/carl/killappassist/KillAppService.java
app/src/main/java/com/carl/killappassist/PermissionActivity.java
app/src/main/res/xml/accessibility_service_config.xml
app/src/main/res/values/strings.xml
build.gradle
gradle/wrapper/gradle-wrapper.properties
gradle.properties
settings.gradle
```

> Tip: On GitHub mobile, tap **Add file → Upload files**, then navigate into folders before uploading.

### Step 3 — Wait for build
1. Tap **Actions** tab in your repo
2. You'll see **Build KillApp Overlay APK** running
3. Wait ~2-3 minutes for it to finish ✅

### Step 4 — Download APK
1. Tap the completed workflow run
2. Scroll down to **Artifacts**
3. Tap **KillAppOverlay-debug** to download
4. Install the APK on your Infinix Note 12 G96

---

## First-time Setup After Install
1. Open **KillApp Overlay** app → it opens Accessibility Settings automatically
2. Enable **KillApp Overlay Service**
3. Grant **Display over other apps** permission when prompted
4. Done!

---

## Usage
1. Open any app (e.g. Chrome, Facebook, MLBB)
2. Press the **Recents** button
3. **⛔ Kill App** button appears at the bottom
4. Tap it → app force-stopped instantly
5. Long press the button → dismiss without killing

---

## Notes
- Works best with Magisk root (uses `su -c am force-stop`)
- Falls back to `killBackgroundProcesses` without root
- Protected: SystemUI, Launcher, Magisk, itself
- Target device: Infinix Note 12 G96 (X670), XOS 13
