# Privacy Policy for Air Hockey

**Last updated:** November 2, 2026

Air Hockey ("we", "our", or "the application") is committed to protecting your privacy. This Privacy Policy explains how information is handled when you use the Air Hockey mobile application (`dev.vmellos.airhockey`).

---

### 1. Summary of Principles

* **Zero Personal Data Collection:** We do not collect, store, transmit, or share any personal information, identifiers, device fingerprints, or telemetry.
* **100% Offline & Ad-Free:** The application contains no advertising SDKs, no analytic trackers, and no third-party telemetry libraries.
* **Local-Only Storage:** All preferences (such as player display name, sound volume, haptics, AI difficulty, puck speed, and bezel calibration) remain strictly stored on your device in local private storage (Android Jetpack DataStore).
* **Direct Peer-to-Peer Communication:** Multiplayer matches operate entirely over direct local peer-to-peer Wi-Fi or local mobile hotspots. Game packets (paddle positions, puck physics, and score telemetry) never leave your local physical environment.

---

### 2. Information Handled by the Application

#### A. Player Display Name and Preferences
When you choose a display name or customize settings, this information is stored locally on your device. During a local multiplayer match, your display name is shared directly with the connected peer device over your local Wi-Fi or hotspot connection solely to display on the opponent's scoreboard. It is never transmitted to any external server.

#### B. On-Demand QR Code Scanning (Camera)
When you choose to join a match by scanning a Pairing Code, the application opens the device camera strictly to decode the local table address (IP and 4-digit code).
- No photos or video frames are saved to disk, cached, or transmitted across the internet.
- The camera stream is discarded immediately once the code is recognized or the scanner is closed.

#### C. Local Network Telemetry
During a synchronized multiplayer match, the Host device and Guest device exchange physical coordinates, impact velocities, and scores via local UDP/TCP packets. This exchange occurs strictly within your local Wi-Fi or mobile hotspot network.

---

### 3. Application Permissions

Air Hockey requests only the minimum Android permissions required for real-time local peer-to-peer gameplay:

| Permission | Purpose |
| :--- | :--- |
| `android.permission.INTERNET` | Required for local socket communication (TCP and UDP) between the two phones on the local network or hotspot. |
| `android.permission.ACCESS_NETWORK_STATE` | Used to inspect local network state (Wi-Fi / Hotspot active). |
| `android.permission.ACCESS_WIFI_STATE` | Required to determine the local Wi-Fi gateway IP for direct hotspot table discovery. |
| `android.permission.CHANGE_WIFI_MULTICAST_STATE` | Enables local Network Service Discovery (mDNS/NSD) to locate the Host device without manual IP configuration. |
| `android.permission.NEARBY_WIFI_DEVICES` | Declared with `android:usesPermissionFlags="neverForLocation"` on Android 12+ (API 31+) to discover nearby peer devices over Wi-Fi without accessing geographic location. |
| `android.permission.ACCESS_FINE_LOCATION` | Scoped to Android 12 and below (`maxSdkVersion="32"`) as mandated by legacy Android APIs for local Wi-Fi/mDNS discovery. Not used for location tracking. |
| `android.permission.VIBRATE` | Drives tactile haptic feedback on paddle impacts and goals. |
| `android.permission.CAMERA` | Optional permission utilized exclusively when scanning a Pairing Code QR on another player's screen. |

---

### 4. Children's Privacy

Air Hockey is designed for players of all ages (Family and Everyone rating). We do not collect any personal information from any user, including children under the age of 13.

---

### 5. Contact & Questions

If you have any questions or feedback regarding this Privacy Policy, you may contact the developer:
- **Developer:** Vinicius Mellos
- **Email:** dev@vmellos.dev
- **Repository:** https://github.com/vmellos/AirHockey
