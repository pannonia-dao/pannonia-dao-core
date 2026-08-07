# Cerberus Android Demo v0.1 🛡️

English, offline Android proof-of-concept for a local AI-agent defense dashboard.

## Demo

Tap **RUN ATTACK DEMO** to execute a deterministic local simulation:

1. agent starts,
2. suspicious file access,
3. honeytoken access,
4. forbidden outbound connection,
5. fake exfiltration attempt,
6. quarantine and audit logging.

The simulated final risk score is **87/100**.

## Safety and scope

This is a UI/security-decision-flow PoC. It does **not** run malware, exploits, real credential theft, cloud exfiltration, or kernel-level enforcement.

## APK build

The GitHub Actions workflow `.github/workflows/cerberus-android-apk.yml` generates the Android project and builds a debug APK.

Artifact name: **Cerberus-Demo-v0.1-APK**

File: `Cerberus-Demo-v0.1-debug.apk`

Android requirement: **Android 8.0+ (API 26+)**.
