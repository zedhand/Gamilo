# Privacy Policy — Gamilo

_Last updated: 2026-08-29_

Gamilo is an **air-gapped, offline-first** business assistant for anyone running a self-owned business or side hustle. This policy is short because the app does very little with your data — it doesn't send it anywhere.

## What Gamilo stores

Everything you enter — jobs, tasks, hours, expenses, mileage, shipments, appointments, and settings — is stored **only in a local, encrypted SQLite database on your device**, plus any receipt photos you attach as local files.

## What Gamilo does NOT do

- **No network calls, ever.** Gamilo has no server, no analytics SDK, no crash-reporting service, and no ad network. It does not know you exist.
- **No account, no sign-in.** There is nothing to create an account for.
- **No data leaves your device** unless you explicitly export it yourself (a combined CSV report or a database backup file, both saved via Android's Storage Access Framework to a location *you* choose).
- **On-device AI only.** Voice-log transcription (Whisper) and shipping-label text recognition (ML Kit) run entirely on your device. No audio, photo, or recognized text is ever transmitted anywhere.

## Permissions Gamilo requests, and why

| Permission | Why |
|---|---|
| Camera | To scan shipping labels for on-device text recognition, and to attach receipt photos to expenses. |
| Microphone | To record a voice note for the local Voice Log accelerator. |
| Notifications | To show the live shift-timer notification while an hours session is running. |
| Biometric | To lock the app behind your device's fingerprint/face/PIN, protecting the client and business data stored locally. |

Denying any of the above simply hides the feature that needs it — the rest of the app keeps working.

## Data deletion

Since everything lives on your device, uninstalling the app (or using the "Factory Reset" option in Settings) deletes all of it permanently. There is no server-side copy to separately request deletion of.

## Changes to this policy

If this policy changes, the update will be committed to this same file in the app's repository, and the version noted in Settings.
