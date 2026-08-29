# Gamilo User Guide

Gamilo is an **air-gapped** business assistant for solo handyman operators. Everything below runs entirely on your device — no accounts, no network calls, no cloud sync. See [PRIVACY.md](PRIVACY.md) for what that means in detail.

## Contents

- [Getting started](#getting-started)
- [Navigation](#navigation)
- [Home](#home)
- [Jobs](#jobs)
- [Tasks](#tasks)
- [Hours](#hours)
- [Expenses](#expenses)
- [Mileage](#mileage)
- [Shipping & Logistics](#shipping--logistics)
- [Calendar & Appointments](#calendar--appointments)
- [Voice Log accelerator](#voice-log-accelerator)
- [The sticky filter bar](#the-sticky-filter-bar)
- [Themes](#themes)
- [Settings — rates, region & data export](#settings--rates-region--data-export)
- [Backups & factory reset](#backups--factory-reset)
- [Permissions](#permissions)
- [Troubleshooting](#troubleshooting)

## Getting started

1. **Install** — see the sideloading steps in [README.md](README.md).
2. **First launch** — Gamilo locks behind your device's biometric/PIN (fingerprint, face, or device credential) every time it cold-starts, and again if you background it for more than two minutes. If your device has no lock method configured at all, the app skips the gate rather than becoming unusable.
3. **Set your region** — open **Settings** (top-right of any tab) and pick **Canada** or **United States**. This seeds sensible currency and mileage-rate defaults for *new* entries — it never rewrites currency, tax rate, or FX figures already saved on past records.
4. You're ready — every tab has its own "Add" form right at the top of the list.

## Navigation

The bottom bar has six tabs: **Home, Tasks, Hours, Expenses, Mileage, Shipping.** Three more screens — **Jobs, Calendar, Settings** — live behind the header links at the top of any tab.

Tasks, Hours, Expenses, Mileage, and Shipping each have a **sticky filter bar** (date range + job) right below the header — see [The sticky filter bar](#the-sticky-filter-bar).

## Home

Your at-a-glance status:
- **Start Shift / End Shift** — punches an Hours session in or out
- **Voice Log** button (only shown on hardware-eligible devices — see [Voice Log accelerator](#voice-log-accelerator))
- **Active Jobs** / **Open Tasks** counts
- A list of your currently active jobs

## Jobs

The central record most other screens link back to: client name, title, notes, and a status you advance by tapping one of four chips — **Active, On Hold, Completed, Cancelled.** Delete a job with the DELETE link on its row (soft-deleted — see [Backups & factory reset](#backups--factory-reset)).

Every job you create here (or via [Voice Log](#voice-log-accelerator)) becomes available to link from Tasks, Hours, Expenses, Mileage, Shipping, and Calendar.

## Tasks

A simple to-do list, optionally linked to a job. Tap a task to mark it done (it shows a strikethrough); tap DELETE to remove it.

## Hours

Manual time entries: how many hours you worked, at what rate, optionally against a job. The rate you enter is **frozen onto that entry permanently** — a later change to your default hourly rate in Settings never reprices hours already logged. The Home tab's Start/End Shift button writes into this same list automatically, with a live notification while a shift is running.

## Expenses

Log a cost, attach a receipt photo, and optionally tag the job it belongs to. Cost is converted and frozen to CAD at the exchange rate in effect when you save it — future FX-rate changes never reshuffle a past expense.

## Mileage

Log a trip's origin, destination, and distance. The per-km rate applied is frozen at entry time, same as Hours.

## Shipping & Logistics

Tracks outbound packages.

**Scanning a label:**
1. Tap **Scan Label**
2. On-device ML Kit text recognition reads the label, then Gamilo tries to auto-detect the **carrier** (Canada Post, FedEx, UPS, DHL) and **tracking number** from the recognized text
3. Review and correct every field before saving — nothing is ever auto-committed from OCR

**What's tracked per shipment:** carrier, tracking number, shipping cost (with currency/FX snapshot), whether you or the client absorbed the cost, insurance cost, declared value, box dimensions, and an optional job link.

**From a shipment's row:**
- **TRACK** — opens the carrier's real tracking page in your browser with the tracking number pre-filled
- **COPY SNIPPET** — copies a clean, plain-text tracking summary to your clipboard, ready to paste into a text or email to the client

## Calendar & Appointments

Book an appointment — title, date, time, an optional duration, location, notes, and an optional job link — from the **Calendar** header link. Date and time use your device's standard picker dialogs. Appointments are listed chronologically; delete one with its DELETE link.

## Voice Log accelerator

A hardware-gated shortcut (needs 6GB+ RAM — hidden entirely on devices that don't qualify) for creating a new job by speaking instead of typing.

1. Tap **Voice Log** on Home
2. Speak naturally, e.g. *"New job for Jane Smith to replace the kitchen faucet"* — the mic stops automatically shortly after you stop talking
3. Transcription happens **entirely on-device** via a bundled Whisper model (TensorFlow Lite) — your voice never leaves the phone
4. A simple pattern extractor guesses the client name and job title from the transcript and shows you a **Review** screen — both fields are editable, and nothing saves until you tap **Confirm & Save**

The exact words you said are always kept in the new job's notes, so you can double-check what Gamilo actually heard. Any part of the sentence about scheduling an appointment (Gamilo doesn't yet parse dates/times from speech) is preserved in that same note — add the appointment separately from [Calendar](#calendar--appointments) if you mentioned one.

## The sticky filter bar

Tasks, Hours, Expenses, Mileage, and Shipping each show a bar with two chips right below the header:
- **RANGE** — Today, Week, Month, or All
- **JOB** — All Jobs, Unassigned, or a specific job

Your filter choice carries over as you switch between these five tabs, so narrowing down to "this week, Job X" stays consistent no matter which of them you're looking at.

## Themes

Settings lets you pick from nine color palettes, applied instantly across the whole app — six dark (Obsidian & Amber, Blueprint & Cyan, Terminal Green, High-Vis Construction, Crimson & Steel, Arctic Slate) and three light (Drafting Table, Blueprint Reverse, Safety Light). Your choice is saved and restored on every launch.

## Settings — rates, region & data export

- **Region** (Canada/United States) — seeds currency and mileage-rate defaults for new entries only
- **Default Rates** — hourly rate, mileage rate, manual FX rate to CAD, GST rate, PST rate; tap **Save Rates** to apply. Like everywhere else in Gamilo, changing these never touches figures already frozen onto past entries
- **Export CSV** — one combined CSV covering every module (jobs, tasks, hours, expenses, mileage, shipping, appointments, attachments), including soft-deleted rows, so it always reconciles with past tax filings
- **Export Backup / Import Backup** — see [Backups & factory reset](#backups--factory-reset)
- **Factory Reset** — see [Backups & factory reset](#backups--factory-reset)

## Backups & factory reset

- **Automatic rolling backups** — a background worker silently copies the database to a hidden internal folder daily, guarding against corruption without any action from you
- **Export Backup** — saves a full database snapshot to a location you choose via Android's file picker, useful for manual backups or moving data to a new device
- **Import Backup** — restores from a backup file, **replacing all current data**, then restarts the app to load it. This can't be undone
- **Factory Reset (Danger Zone)** — permanently wipes everything on the device: guarded by a fresh biometric prompt plus typing the word `DELETE` exactly, so it can't happen by accident

## Permissions

Gamilo requests permissions lazily — only the moment you actually use the feature that needs one, never up front.

| Permission | Requested when | What happens if you deny it |
|---|---|---|
| Camera | Tapping Scan Label on Shipping, or Attach Receipt Photo on Expenses | The camera option just fails to open |
| Microphone | First tap of Voice Log | Voice Log shows an error; manual job entry still works |
| Notifications | The first time a shift timer would show one | The shift-timer notification won't show, but the timer still runs correctly |
| Biometric | Not a runtime permission — uses whatever lock method is already set up on your device | If nothing is enrolled, the lock screen is skipped entirely |

Gamilo never requests broad storage permissions — every export/import goes through Android's Storage Access Framework, where *you* pick the exact file/location each time.

## Troubleshooting

**The app won't unlock / biometric prompt doesn't appear.**
Tap the "Tap to retry" text on the lock screen. If it still doesn't appear, check that your device actually has a fingerprint/face/PIN enrolled in system settings — Gamilo can't prompt for a lock method that isn't set up.

**Voice Log isn't showing up on Home.**
It's hardware-gated (6GB+ RAM). Devices below that threshold don't show the button at all — there's currently no in-app screen showing exactly why, so check your device's specs directly.

**Voice Log mishears me / gets the wrong client or job title.**
The on-device Whisper model is a small, fast variant — it's good but not perfect, especially with uncommon names or heavy background noise. Everything it produces lands on an editable Review screen before saving, so just correct it there. For best results, phrase it as "new job for **[client name]** to **[what you're doing]**."

**Camera won't open from Shipping or Expenses.**
Make sure you granted the camera permission when prompted (see [Permissions](#permissions)). If you denied it once, Android may require you to re-enable it from the app's system settings page rather than re-prompting.

**A shipping label's OCR text looks wrong or came back empty.**
That's expected sometimes — lighting, glare, or an unusual label layout can defeat text recognition. Nothing is auto-committed from OCR; just fill the fields in by hand and save.

**CSV export or backup export button does nothing.**
Check that you're not dismissing the system file picker that appears — Gamilo needs you to actually choose a destination.

**I imported a backup and now see old/wrong data, or the app looks stuck.**
A backup import replaces the live database file and immediately restarts the app to load the fresh state — if that restart didn't happen for some reason, fully close and reopen Gamilo yourself.

**Numbers look off after changing region or default rates.**
That's by design — region and rate changes only affect *new* entries going forward. Every past record keeps the exact currency code and rate that was in effect when it was created, which is what makes the CSV export tax-filing-safe.

**I want to move Gamilo to a new phone.**
Settings → **Export Backup**, transfer that file to the new device however you like (USB, cloud drive, email to yourself — Gamilo itself never touches the network, but you're free to move the exported file however you want), install Gamilo on the new phone, then Settings → **Import Backup**.

---

Found a bug or have a feature request? Open an [issue](https://github.com/zedhand/Gamilo/issues) on GitHub.
