# GatePass: Precision Access Management System

GatePass is a high-performance, privacy-first Android application designed to replace traditional manual logbooks at institutional entry points. It leverages computer vision and mathematical validation to ensure security without compromising data privacy.

## 🛡️ Operational Philosophy: "The Surgical Strike"
The core principle of GatePass is **Data Volatility**. We believe that what is not stored cannot be stolen.
- **Immediate Purge:** Sensitive identifiers (like Aadhaar numbers) are used only for mathematical verification and are never committed to any database.
- **Validated Entry:** Mathematical checksums (Verhoeff) ensure data integrity at the edge.
- **Legal Compliance:** By discarding PII (Personally Identifiable Information) post-validation, the system minimizes legal liability and maximizes user trust.

## 🚀 Technical Stack
- **Language:** Java (Enterprise-grade stability)
- **Vision Engine:** Google ML Kit (OCR & Barcode Scanning)
- **Backend:** Firebase Firestore (Real-time synchronization & persistence)
- **Validation:** Verhoeff Algorithm (Dihedral group D5 permutations)
- **QR Generation:** ZXing (Zebra Crossing) Library
- **UI Architecture:** Material 3 (Standardized, accessible design)
- **Printing:** ESC/POS Thermal Printing API support

## 🔄 Data Flow
1. **Acquisition:** OCR extracts text from a physical ID card via Google ML Kit.
2. **Validation:** The ID number is validated via the `Verhoeff` algorithm to ensure authenticity.
3. **Student Verification:** The system performs a real-time lookup against the Firestore `students` collection using the Registration Number. Submission is strictly blocked unless a matching student record is found.
4. **Hashing:** If required for unique visitor tracking, the ID is hashed (SHA-256) via `HashUtils`. The raw ID is deleted from memory.
5. **Persistence:** A visitor record is created in Firestore with a "pending" status and a unique timestamp.
6. **Issuance:** A digital pass with a unique QR code (generated via **ZXing**) is displayed.
7. **Printing:** The pass can be physically issued via a Bluetooth/USB thermal printer using the **ESC/POS API**.
8. **Checkout:** The visitor scans their QR code at the exit, updating the Firestore record with an `exitTime` and marking them as "Left".

## 🛠️ Security Features
- **Security Restricted List:** Real-time checking against a "blacklist" collection.
- **Automated Verification:** Instant validation of host students prevents fraudulent entry.
- **Offline Resilience:** Firestore's local cache allows processing even during network interruptions.
- **Audit Trail:** Every entry and exit is timestamped and indexed for administrative review.

## 📦 Key Components
- `MainActivity`: High-speed OCR scanning with background analysis.
- `Verhoeff`: Implementation of the Dihedral Group D5 checksum.
- `PrintManager`: Logic for formatting and sending data to thermal printers.
- `GatePassFormActivity`: Real-time student lookup and visitor registration.
- `CheckoutScanner`: QR-based exit management.
