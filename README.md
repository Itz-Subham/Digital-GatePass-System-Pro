# Digital GatePass System — Pro

An extended version of my original [Digital-GatePass-System](https://github.com/itz-subham/Digital-GatePass-System) project, rebuilt on my own Firebase backend. This is an Android-based visitor management application that streamlines visitor entry and exit using Aadhaar OCR, QR-based digital gate passes, and Firebase — replacing manual gate registers with a secure, paperless solution while protecting sensitive information through SHA-256 hashing.

---

## What's different from the original

- Runs on a fully self-owned Firebase project (Firestore + Hosting), not a shared/borrowed backend
- [Add your new features here as you build them — e.g. admin analytics, push notifications, role-based auth]

---

## Features

- Aadhaar scanning using Google ML Kit OCR
- SHA-256 hashing for privacy protection
- Verhoeff checksum validation for Aadhaar numbers
- Optional student verification
- Blacklist verification before pass generation
- QR code generation for digital gate passes
- QR-based visitor checkout
- Firebase Firestore cloud database
- Visitor history and search functionality
- Firebase Hosting for online gate pass viewing

---

## Tech Stack

| Category | Technologies |
|----------|--------------|
| Language | Java |
| IDE | Android Studio |
| Database | Firebase Firestore |
| Hosting | Firebase Hosting |
| OCR | Google ML Kit Text Recognition |
| Camera | CameraX |
| QR Code | ZXing |
| Security | SHA-256 Hashing, Verhoeff Validation |
| UI | Material Design 3 |

---

## How It Works

1. Scan the visitor's Aadhaar card using the device camera.
2. Extract details using Google ML Kit OCR.
3. Validate the Aadhaar number using the Verhoeff algorithm.
4. Check whether the visitor is blacklisted.
5. (Optional) Verify the student being visited.
6. Hash the Aadhaar number using SHA-256 before storage.
7. Store visitor details securely in Firebase Firestore.
8. Generate a unique QR-based digital gate pass.
9. Scan the QR code during checkout to update the visitor's exit status.

---

## Project Structure

```
app/src/main/java/com/example/gatepass/
 ├── activities/     # All screens (Main, Login, Dashboard, GatePassForm, Checkout, etc.)
 ├── adapters/        # RecyclerView adapters (Blacklist, Guard, LiveSearch, Visitor)
 ├── firebase/        # Firestore repository and constants
 ├── models/          # Data models (Guard, Visitor, SearchResult)
 ├── qr/               # QR code generation utilities
 ├── scanner/          # Aadhaar OCR scanning and blur detection
 ├── utils/            # Shared utilities (hashing, date/time, session, constants)
 └── validation/       # Verhoeff checksum and blacklist validation logic
```

---

## Firestore Data Structure

Firestore has no schema file, so if you're cloning this repo, recreate the following collections in your own Firebase project exactly as described below (collection names are case-sensitive and referenced directly in code via `FirestoreConstants.java`).

### `users`
Stores both admin and security-guard accounts, distinguished by `role`.

| Field | Type | Example | Notes |
|---|---|---|---|
| username | string | `admin1` | |
| password | string | — | ⚠️ currently plaintext, see Security Notes below |
| role | string | `"admin"` or `"security"` | |
| active | boolean | `true` | disabling a guard sets this to `false` |

### `students`
Used to verify who a visitor is visiting.

| Field | Type | Example |
|---|---|---|
| regNo | string | `2341041062` |
| name | string | `Priya Mohanty` |

### `visitors`
One document per visitor entry. Written entirely by the app (`GatePassFormActivity`) — you don't need to seed this manually, it populates the first time you generate a gate pass.

| Field | Type | Example |
|---|---|---|
| visitorName | string | `Subham Singh` |
| visitorNameLower | string | `subham singh` (lowercase copy, used for search queries) |
| visitorIdHash | string | SHA-256 hash of Aadhaar number |
| studentReg | string | `2341041062`, or `"N/A"` if not visiting a student |
| studentName | string | `Priya Mohanty`, or `"N/A"` if not visiting a student |
| hasStudentVisit | boolean | `true` |
| entryTime | Firestore Timestamp | set via `Timestamp.now()` |
| timestamp | number | Unix epoch (millis), used for `orderBy`/range queries |
| status | string | `"pending"` initially, updated on checkout |

### `blacklist`
Restricted individuals, hashed rather than stored in plain text.

| Field | Type | Example |
|---|---|---|
| name | string | `hahah` |
| nameLower | string | `hahah` (lowercase, for search) |
| id | string | hashed ID |
| timestamp | number | Unix epoch |

---

## Security

- Aadhaar numbers are never stored in plain text — only their SHA-256 hash.
- Verhoeff checksum validation prevents invalid Aadhaar entries.
- Blacklist verification is performed before pass generation.
- **Self-migrating password security**: `LoginActivity` verifies passwords via `HashUtils.verifyPassword()`, which accepts either a hashed or legacy plaintext value. The first time an account with a plaintext password logs in successfully, it's automatically re-hashed (SHA-256) and persisted back to Firestore — so accounts transparently upgrade to secure storage without requiring a manual password reset.

---

## Requirements

- Android 7.0 (API 24) or later
- Android Studio
- A Firebase project of your own (Firestore + Hosting enabled)
- Internet connection

---

## Setup

1. Clone the repository.
   ```bash
   git clone https://github.com/itz-subham/Digital-GatePass-System-Pro.git
   ```
2. Open the project in Android Studio.
3. Create your own Firebase project at [console.firebase.google.com](https://console.firebase.google.com), register an Android app with package name `com.example.gatepass`, and download your own `google-services.json`.
4. Place `google-services.json` in the `app/` directory. This file is gitignored and **not included in the repo** — the app will not build without your own copy.
5. In Firestore, manually create just two collections to get started: `users` (one document with `username`, `password`, `role`, `active` fields, so you have something to log in with) and `students` (one document with `name` and `regNo`, so the "visiting a student" lookup has something to find). `visitors` and `blacklist` populate automatically the first time you use the app.
6. Sync Gradle.
7. Build and run the application.

---

## Future Improvements

- Admin dashboard analytics
- Push notifications
- Multi-campus support
- Role-based authentication refinements

---

## Author

**Subham Singh**
BCA Student | Android Developer
GitHub: [github.com/itz-subham](https://github.com/itz-subham)
