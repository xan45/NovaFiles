# NovaFiles 🚀

[![Android](https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Samsung One UI](https://img.shields.io/badge/Design-Samsung_One_UI-1259C3?style=for-the-badge&logo=samsung&logoColor=white)](https://www.samsung.com/one-ui/)
[![Release](https://img.shields.io/badge/Release-v1.0.0--beta1-FF6F00?style=for-the-badge&logo=github)](releases/NovaFiles-v1.0.0-beta1.apk)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=for-the-badge)](LICENSE)

**NovaFiles** is an advanced, ultra-responsive next-generation Android file manager designed and developed by **Abhinav Kishore (XanDron45)**, ultra-responsive next-generation Android file manager built with **Jetpack Compose**, **Material Design 3**, and dedicated **Samsung One UI** design system elements. Powered by reactive Kotlin coroutines, modern storage engines, and enterprise-grade networking, NovaFiles delivers a desktop-class file management experience directly on Android.

---

## ✨ Key Features

### 🗂️ Advanced File Management & Dual-Pane Navigation
- **Dual-Pane File Browser**: Multitask across dual panels or tabbed panes for rapid cross-directory file copying, moving, and comparison.
- **Deep File Operations**: Batch copy, move, rename, delete, recursive search, hidden file toggles, and metadata inspection.
- **APK Manager**: Inspect installed applications, extract base APKs, and analyze package signatures and permissions.

### 🎨 Samsung One UI Adaptive Icon Pack & Theming
- **Adaptive Vector Icon Set**: Pixel-crafted squircle icons designed to blend seamlessly into Samsung One UI 6 & 7 and modern AOSP launchers.
- **Monochrome & Cobalt Variants**: Includes high-contrast Cobalt squircle backgrounds, dynamic material accenting, and themed monochrome icons for Android 13+.
- **One UI Design Mode**: Native One UI squircle card corners (26.dp), tactile feedback, and oversized header viewing areas for effortless one-handed ergonomics.

### 🔒 AES-256 Encrypted Private Vault
- **Zero-Knowledge Architecture**: Secure local private vault protected with salted PBKDF2/SHA-256 PIN authentication.
- **Cryptographic Obfuscation**: Files moved to the vault are encrypted on-the-fly using `AES/CBC/PKCS5Padding` with uniquely generated Initialization Vectors (IVs) per file.
- **Stealth Protection**: Encrypted vaults remain invisible to standard Android media scanners and third-party applications.

### 🗜️ Comprehensive Universal Archive Suite
- **Complete Format Support**: Read, browse, inspect, and extract:
  - **ZIP** archives (standard Deflate, Store)
  - **7-Zip (`.7z`)** archives powered by Apache Commons Compress
  - **WinRAR (`.rar`)** archives powered by Junrar (RAR4/RAR5 format headers)
  - **TAR & GZ (`.tar`, `.tar.gz`, `.tgz`)** UNIX archives
- **In-Archive Browsing**: Explore nested compressed folder hierarchies and inspect file sizes, CRC checksums, and compression ratios prior to decompression.

### 🌐 SMBv2 & SMBv3 True Windows Share Client
- **Native Protocol Stack**: Direct SMBv2/SMBv3 implementation via `smbj` for modern Windows 10/11 and Linux Samba network shares.
- **LAN Discovery & Bookmarks**: Effortlessly browse remote shared directories, stream remote media, and sync files over local Wi-Fi networks.
- **Secure Authentication**: Supports NTLMv2 and Kerberos network credentials.

### ⚡ Shizuku & Root Privileged File Explorer
- **Privileged Shell Operations**: Elevated shell command execution (`su`) for rooted devices to inspect restricted partitions (`/`, `/system`, `/data`, `/vendor`).
- **Shizuku Integration**: Binder status detection and privileged API access for non-root ADB-level system file exploration.
- **File System Permissions**: Inspect UNIX file modes, owner UIDs, and SELinux contexts.

### 🧠 Intelligent Storage Analyzer & Safe Duplicate Detection
- **Visual Storage Breakdown**: Interactive disk usage breakdown categorizing Images, Videos, Audio, Documents, Archives, and Apps.
- **3-Point SHA-256 Duplicate Scanner**: Eliminates false positives by computing multi-point cryptographic samples (64KB Head + 64KB Mid + 64KB Tail) rather than simple file sizes or truncated prefixes.
- **Large & Redundant File Cleanup**: Instantly locate dormant APKs, abandoned cache folders, and bloated media.

---

## 🏗️ Architecture & Technology Stack

```mermaid
graph TD
    UI[Jetpack Compose UI & Material 3] --> VM[MainViewModel]
    VM --> Repo[FileRepository]
    VM --> Analyzer[StorageAnalyzerEngine]
    VM --> Vault[AES-256 Secure Vault]

    Repo --> Local[Local File System / SAF]
    Repo --> SMB[SmbClientManager - SMBv2/v3]
    Repo --> Root[RootStorageManager - Root / Shizuku]
    Repo --> Archive[ArchiveManager - 7z, RAR, TAR, ZIP]

    Analyzer --> SafeHash[3-Point SHA-256 Sampler]
```

| Layer | Component | Technologies |
|---|---|---|
| **UI & UX** | Screens, Components, Dialogs | Jetpack Compose 1.7+, Material Design 3, Samsung One UI theme system |
| **Language** | Modern Kotlin | Kotlin 2.0.0, Coroutines, StateFlow, Flow |
| **Networking** | SMB Client | `com.hierynomus:smbj:0.13.0` (SMB2/SMB3) |
| **Privilege** | Root / Shizuku | Rikka Shizuku API 13.1.5, Superuser Shell (`su`) |
| **Compression**| Universal Archives | Apache Commons Compress 1.26.1, Junrar 7.5.5, `java.util.zip` |
| **Storage & DB** | Persistence & Settings | Android Room 2.7.0, DataStore Preferences |
| **Security** | Vault Encryption | `javax.crypto.Cipher` (AES/CBC/PKCS5Padding), SHA-256 |

---

## 📦 First Beta Release (v1.0.0-beta1)

The first compiled beta APK release is available in this repository:

- **File**: [`releases/NovaFiles-v1.0.0-beta1.apk`](releases/NovaFiles-v1.0.0-beta1.apk)
- **Version**: `1.0.0-beta1` (versionCode `1`)
- **Package ID**: `com.aistudio.novafiles.kxmfle`
- **File Size**: `31,311,854 bytes (~29.8 MB)`
- **SHA-256 Checksum**:
  ```text
  EB235C0F897632F80567795F1878A32781963EF01540D59033FB97BD15A7050B
  ```

### Verification
You can verify the release integrity on Windows PowerShell:
```powershell
Get-FileHash releases/NovaFiles-v1.0.0-beta1.apk -Algorithm SHA256
```
Or on Linux / macOS:
```bash
sha256sum releases/NovaFiles-v1.0.0-beta1.apk
```

---

## 🛠️ Building from Source

### Prerequisites
1. **Java Development Kit**: JDK 17, 21, or 25 (e.g. Android Studio JBR).
2. **Android SDK**: Target SDK 36 (Android 16 preview) or SDK 34/35.
3. **Gradle**: 8.11+ / 9.x (wrapper included).

### Build Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/xan45/NovaFiles.git
   cd NovaFiles
   ```

2. **Configure Local SDK**:
   Create a `local.properties` file in the project root:
   ```properties
   sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
   ```

3. **Compile and Assemble Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The compiled APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Install to a connected Android device or Emulator**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📄 License

NovaFiles is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for more information.
