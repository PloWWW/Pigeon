# 🕊️ Pigeon

An open-source local file sharing app for Android. Transfer files over local Wi-Fi directly to and from any device with a web browser—no client app required, no internet, and no third-party cloud services.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Ktor](https://img.shields.io/badge/Engine-Ktor%20CIO-087CFA?style=flat-square&logo=ktor&logoColor=white)](https://ktor.io)
[![Open Source](https://img.shields.io/badge/Open%20Source-%E2%99%A5-brightgreen?style=flat-square)](https://github.com/PloWWW/Pigeon)

[Download APK](https://github.com/PloWWW/Pigeon/releases/latest) • [Features](#features) • [Installation](#installation) • [Building](#building)

---

## Features

- **Local Wi-Fi transfer:** Moves files directly over the local network via TCP sockets without uploading to the internet.
- **Two-way sharing:** Pick files on the phone to share, or drop files in the web interface to save them to the device.
- **Stream-based transfer:** Pipes data in 8 KB chunks directly through Ktor's `respondOutputStream` without loading entire files into RAM.
- **ZIP on the fly:** Generates and streams a single `.zip` archive on request without saving temporary files to disk.
- **QR Code connect:** Displays a dynamic QR code on the phone screen for quick pairing with a desktop browser.

---

## Installation

1. Grab the latest `.apk` from the [Releases](https://github.com/PloWWW/Pigeon/releases/latest) page.
2. Install it on your Android device.
3. Connect both the phone and PC to the same Wi-Fi network.
4. Launch the app and open the shown IP address (or scan the QR code) in the browser.

---

## Building

```bash
git clone https://github.com/PloWWW/Pigeon.git
cd Pigeon
./gradlew assembleDebug
