# Cerberus Signal Lab v0.2 🛡️🐈‍⬛

A terminal-style Android telemetry baseline for the Cerberus local AI-agent defense concept.

## What is real in v0.2

The app now measures actual local Android signals in real time:

- current Cerberus app CPU utilization,
- Java heap usage,
- per-UID RX/TX throughput,
- battery percentage,
- battery temperature,
- Android thermal state,
- app render FPS,
- most recent foreground package when Usage Access is granted,
- known AI-app packages seen in the previous 10 minutes.

## AI-app activity

Android requires explicit **Usage Access** permission. Tap **GRANT / CHECK USAGE ACCESS**, enable Cerberus Signal Lab, then return to the app.

The baseline uses a transparent package-name heuristic for known AI apps such as OpenAI/ChatGPT, Anthropic/Claude, Gemini/Bard, Copilot, Perplexity, DeepSeek, Grok, Poe, and Character.AI. This is a heuristic signal, not proof that an AI agent performed a malicious action.

## GPU signal

Cerberus deliberately reports GPU utilization as **UNAVAILABLE**. A normal stock Android application does not have a portable API for trustworthy system-wide/per-app GPU utilization percentage. The UI preserves that missing signal instead of fabricating a value.

## Design principle

**Measure first. Infer later. Never overwrite raw telemetry with a desired security conclusion.**

The terminal panel keeps a raw local sample stream separate from future derived risk scores.

## APK build

GitHub Actions builds the checked-in Android source directly.

Artifact: **Cerberus-Signal-Lab-v0.2-APK**

APK: `Cerberus-Signal-Lab-v0.2-debug.apk`

Android requirement: **Android 8.0+ (API 26+)**.

No root, backend, cloud account, malware, exploit code, credential theft, or kernel enforcement is required.
