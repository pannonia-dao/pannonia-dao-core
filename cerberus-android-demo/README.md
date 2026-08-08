# Cerberus Sentinel v0.3 🛡️🐈‍⬛

Cerberus Sentinel is a local-first Android security telemetry product concept for observing AI-app and device activity without root or a cloud account.

## Product direction

v0.3 upgrades the previous terminal-style baseline into a product-grade security dashboard with clearer posture, branding, install integrity visibility, and a dedicated launcher mark.

## Real signals

Cerberus measures and displays:

- current Cerberus app CPU utilization,
- Java heap usage,
- per-UID RX/TX throughput,
- battery percentage and temperature,
- Android thermal state,
- app render FPS,
- APK signing-certificate SHA-256 fingerprint,
- install source when Android exposes it,
- most recent foreground package with explicit Usage Access,
- known AI-app packages observed in the previous 10 minutes.

## Install integrity

The dashboard reports whether an APK signing certificate is present and displays its actual SHA-256 fingerprint. This is evidence about the installed package identity; it does **not** claim that an arbitrary certificate is trusted merely because it exists.

The install source is shown as raw Android metadata. Sideloaded or unavailable installer metadata remains explicitly labelled rather than being silently upgraded to a trusted status.

## Readiness indicator

The hero readiness indicator is operational state, not a malware verdict:

1. local telemetry pipeline available,
2. APK signing certificate readable,
3. Usage Access enabled for foreground-app visibility.

## AI-app activity

Android requires explicit **Usage Access** permission. Tap **ENABLE ACTIVITY VISIBILITY**, enable Cerberus Sentinel, then return to the app.

The AI-app list uses a transparent package-name heuristic for applications such as ChatGPT/OpenAI, Claude/Anthropic, Gemini, Copilot, Perplexity, DeepSeek, Grok/xAI, Poe, and Character.AI. This is an observation signal, not proof of malicious agent behavior.

## GPU signal

Cerberus deliberately reports GPU utilization as **N/A**. A normal stock Android application does not have a portable API for trustworthy system-wide/per-app GPU utilization percentage.

## Signal integrity

**Measure first. Infer later. Never fabricate unavailable security evidence.**

Raw telemetry and derived product state remain conceptually separate. v0.3 also fixes the terminal RX/TX sample logging order so displayed network rates use the actual prior sample as their reference.

## APK build

GitHub Actions builds the checked-in Android source directly.

Artifact: **Cerberus-Sentinel-v0.3-APK**

APK: `Cerberus-Sentinel-v0.3-debug.apk`

Android requirement: **Android 8.0+ (API 26+)**.

No root, backend, cloud account, malware execution, exploit code, credential theft, or kernel enforcement is required.
