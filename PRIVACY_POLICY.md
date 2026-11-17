# Privacy Policy for Air Hockey

**Last updated:** November 17, 2026

Air Hockey ("we", "our", or "the application", package `dev.vmellos.airhockey`) is committed to protecting your privacy. This Privacy Policy explains how information is handled when you use the Air Hockey mobile application.

---

### 1. Summary of Principles

* **Local-First Gameplay & Peer-to-Peer Multiplayer:** Matches run locally on your device or directly between two devices over local Wi-Fi or Mobile Hotspot using TCP/UDP sockets and mDNS discovery. We do not operate proprietary game servers or store match traffic on remote servers.
* **Local Storage:** Preferences, match history records, competitive Elo statistics, and 100-stage progression are stored locally on your device using Android Jetpack DataStore and Room SQLite.
* **No Mandatory Account Creation:** You can play all offline and local multiplayer modes without creating an account or signing in.
* **Third-Party Google Services:** The application integrates **Google AdMob** (with Google User Messaging Platform — UMP — consent management), **Google Play Billing** (optional one-time `remove_ads` purchase), and optional **Google Play Games Services v2** (cloud saves and global leaderboards).

---

### 2. Information Handled by the Application & Third-Party SDKs

#### A. Player Display Name, Preferences & Local Telemetry
When you set a player display name or customize game preferences, that data is stored locally on your device. During a local Wi-Fi or Hotspot multiplayer match, your display name and real-time match coordinates/scores are exchanged directly with the connected peer device on the same local network and are never transmitted to external servers.

#### B. On-Demand QR Code Scanning (Camera)
When you choose to join a local table by scanning a Pairing QR Code, the application opens the device camera strictly on demand to decode the local table address (`airhockey://connect`).
- No photos or video frames are saved to disk, cached, or transmitted over the internet.
- The camera stream is discarded immediately once the code is recognized or the scanner is closed.

#### C. Advertising & Consent (Google AdMob & UMP)
To offer the game free of charge, the application displays non-intrusive advertisements served by **Google AdMob** (banners on navigation menus and frequency-capped post-match interstitials; never during active gameplay).
- Google AdMob and its mediation/ad partners may process device identifiers (such as Android Advertising ID), IP address, app interaction data, and diagnostic information to serve, measure, and prevent fraud on advertisements, in accordance with your regional consent choices managed through **Google UMP** (GDPR, LGPD, and ePrivacy compliance).
- You may manage or revoke your ad consent preferences and Android Advertising ID settings on your device at any time.

#### D. In-App Purchases (Google Play Billing)
The application offers an optional, non-consumable one-time purchase (`remove_ads`) to permanently remove banner and interstitial ads. All payment transactions and purchase restorations are processed securely by **Google Play Billing**. We do not collect, access, or store credit card numbers or billing credentials.

#### E. Google OAuth & Google Play Games Services User Data (Optional)
If you choose to sign in with **Google Play Games Services v2**, the application accesses limited Google user data through Google OAuth solely to enable optional cloud and competitive gaming features:
- **Google User Data Accessed:** Google Play Games Player ID, public gamertag/display name, Play Games profile avatar image (displayed only locally in the app UI), submitted leaderboard scores (`Elo Ranking` and `Stage Stars`), and encrypted app-specific cloud save snapshots (`SnapshotsClient` / `drive.appdata` scope).
- **How We Use Google User Data:** Used strictly to authenticate your Play Games session, display your player badge and connection status in the Stats & History screen, synchronize your 100-stage progress, Elo rating, and cosmetic preferences across your own Android devices, and post your scores to official Google Play Games Leaderboards.
- **Data Sharing & Limited Use Disclosure:** We do **not** sell, rent, transfer, or share Google OAuth user data with third parties, data brokers, or advertising networks (such as AdMob). Air Hockey's use and transfer to any other app of information received from Google APIs adhere to the **Google API Services User Data Policy**, including the **Limited Use** requirements.

---

### 3. Data Storage, Security & Encryption

- **Local Storage Isolation:** Offline match records, stage progression, and user preferences are stored exclusively within your device's private Android application sandbox (`Room SQLite` and `Jetpack DataStore`) and are not uploaded to any proprietary developer server.
- **Encryption in Transit:** All network communication between the application and Google services (**Google Play Games Services**, **Google AdMob**, **Google UMP**, and **Google Play Billing**) uses industry-standard **HTTPS/TLS** encryption in transit.
- **Local P2P Multiplayer:** Wi-Fi and Mobile Hotspot matches communicate directly between the two devices on the local network (`TCP/UDP` sockets and `mDNS`) without passing through external cloud servers.

---

### 4. Data Retention, Consent Revocation & Deletion

- **Retention Period:** Local gameplay records remain on your device until you clear application data or uninstall the app. Google Play Games cloud saves and leaderboard records remain associated with your Google Play Games profile until you delete them or revoke access.
- **How to Revoke Access or Delete Your Data:**
  1. **Revoke Google OAuth Access:** Visit [https://myaccount.google.com/permissions](https://myaccount.google.com/permissions), select **Air Hockey**, and revoke access at any time.
  2. **Delete Play Games Cloud Saves & Profile Data:** Open the **Google Play Games** app on your Android device → *Settings* → *Delete Play Games account & data* (or remove individual game data).
  3. **Manage Ad Consent & Advertising ID:** Adjust your regional privacy preferences through the Google UMP consent dialog when presented, or reset/delete your Android Advertising ID in Android *Settings → Privacy → Ads*.
  4. **Delete Local Data or Contact Us:** Clear app storage or uninstall Air Hockey to remove all local data, or contact us directly at **contact@vmellos.dev** (or **contact-vmellos-dev@googlegroups.com**) for assistance with any data deletion inquiry.

---

### 5. Application Permissions

| Permission | Purpose |
| :--- | :--- |
| `android.permission.INTERNET` | Required for local TCP/UDP peer-to-peer multiplayer, Google AdMob, Google Play Billing, and Google Play Games Services. |
| `android.permission.ACCESS_NETWORK_STATE` | Inspects network availability for local table discovery and ad/billing initialization. |
| `android.permission.ACCESS_WIFI_STATE` | Resolves the host IP address in local Wi-Fi or Mobile Hotspot mode without manual setup. |
| `android.permission.CHANGE_WIFI_MULTICAST_STATE` | Enables mDNS/NSD protocol for zero-configuration local table discovery. |
| `android.permission.NEARBY_WIFI_DEVICES` | Declared with `android:usesPermissionFlags="neverForLocation"` on Android 13+ to discover nearby peer devices over Wi-Fi without accessing geographic location. |
| `android.permission.ACCESS_LOCAL_NETWORK` | Requests local network access authorization on modern Android versions for Wi-Fi/Hotspot multiplayer discovery. |
| `android.permission.ACCESS_FINE_LOCATION` | Scoped strictly to Android 12 and below (`maxSdkVersion="32"`) as mandated by legacy Android APIs for local Wi-Fi/mDNS discovery. Not used for location tracking. |
| `android.permission.VIBRATE` | Provides tactile haptic feedback on paddle impacts and goal celebrations. |
| `android.permission.CAMERA` | Optional permission used strictly on demand when scanning another device's local Pairing QR Code. |
| `com.android.vending.BILLING` | Enables the optional permanent ad-removal in-app purchase (`remove_ads`) through Google Play Store. |

---

### 6. Children's Privacy

Air Hockey is designed for general audiences (Everyone / Family rating). We do not knowingly collect personal information directly from children under the age of 13, and AdMob consent flows follow applicable regulations.

---

### 7. Contact & Official Portal

If you have any questions, privacy requests, or data deletion inquiries regarding this Privacy Policy, please contact the developer:
- **Developer:** VMellos Dev
- **Email:** contact@vmellos.dev
- **Support Group Email:** contact-vmellos-dev@googlegroups.com
- **Official Portal:** https://vmellos.dev
- **Repository:** https://github.com/vmellos/AirHockey
