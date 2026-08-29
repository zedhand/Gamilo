# Gamilo

**The hardest-working hire you'll ever make — and the only one who doesn't need a paycheck.**

## We'd like to introduce your newest employee

You've got a handyman business. You're the owner, the estimator, the bookkeeper,
the dispatcher, the driver, and — somewhere in there — the guy who actually
fixes things. You're doing the job of five people, and only one of you is
getting paid.

**Gamilo is the other four.**

Think of Gamilo as the assistant you'd hire if you could afford one: sharp,
always on time, never calls in sick, never asks for a raise, never loses a
receipt, and never, ever gossips about your clients — because Gamilo doesn't
talk to anyone but you. Everything it knows stays locked on your phone.

Here's what you're actually paying for when you "hire" the competition:

| Role you're currently filling yourself | Annual cost of hiring it out | What Gamilo charges |
|---|---|---|
| Bookkeeper / Office Admin | $35,000–$45,000/yr | $0 |
| Dispatcher / Scheduler | $30,000–$40,000/yr | $0 |
| Data Entry Clerk | $28,000–$35,000/yr | $0 |
| Someone to chase down that shoebox of receipts every April | Your sanity | $0 |

Gamilo doesn't cost a salary. It doesn't cost a subscription. It doesn't even
cost you an internet connection — it works from a job site with zero bars,
because it was built to.

## What Gamilo actually does for you

**📋 Jobs & Clients** — Every job lives in one place: who it's for, what
you're doing, and where it stands. No more digging through text threads to
remember what you quoted Mrs. Kowalski for the deck repair.

**🎙️ Voice Log — just talk to it** — Standing in a client's driveway with
grease on your hands? Tap one button and say *"New job for Jane Smith to
replace the kitchen faucet."* Gamilo transcribes it, figures out the client
and the job, and hands it back for a one-tap confirm. No typing, no app
fumbling, no lost jobs because you meant to write it down later and forgot.
This runs entirely on your phone — the audio never leaves the device.

**⏱️ Hours** — Punch in, punch out, right from the home screen. A live
notification tracks your shift so you're never guessing how long you were
actually on-site when it's time to invoice.

**💰 Expenses** — Snap a photo of the receipt, log the cost, tag the job.
Done. Every dollar is tied to the job it belongs to, automatically converted
and frozen at the exchange rate on the day you spent it — so your books never
shift under you months later.

**🚗 Mileage** — Log every trip between the shop and the job site. It adds up
faster than you think, and it's deductible.

**📦 Shipping & Logistics** — Scan a shipping label with your camera and
Gamilo reads the carrier and tracking number off it automatically — no
retyping 22 digits with cold fingers. One tap to copy a tracking link
straight to a client text.

**📅 Calendar & Appointments** — Book the appointment the moment you make it,
link it to the job, and never double-book a Tuesday morning again.

**🔍 One filter bar, every tab** — Filter everything — hours, expenses,
mileage, shipments — by job or by date range, sitting right at the top of
every screen. Want to know what last week cost you on the Kowalski job? Two
taps.

**📊 Export everything, anytime** — One button generates a complete CSV of
every job, hour, expense, mile, and shipment — ready to hand to your
accountant or drop into a spreadsheet. Your data, your format, no lock-in.

## The part every other app gets wrong: your data is actually yours

- **100% offline.** No account to create, no server to go down, no internet
  required to log a job from a basement with no signal.
- **Encrypted at rest.** Your entire database is locked behind SQLCipher
  encryption and unlocked with your fingerprint, face, or PIN — whatever your
  phone already uses.
- **No subscription. No ads. No "upgrade to Pro."** You own the app. That's
  the whole business model.
- **You can wipe it all, instantly, on purpose.** A Danger Zone in Settings
  lets you factory-reset the entire app with a biometric check and a typed
  confirmation — because it's your data, and that includes the right to
  delete it.

## Built to survive a real workday

Nine visual themes — from a construction-site high-vis look to a clean
drafting-table light mode — so it's readable whether you're in a dim
basement or on a job site in full sun. Sharp corners, hard borders, no fussy
animations: information you can read at a glance with one dirty glove still
on.

---

## For developers

Gamilo is a native Android app written in Kotlin with Jetpack Compose.

- **UI:** Jetpack Compose, Material 3, a custom "Precision Utility" design
  system (sharp corners, hard borders, monospaced numerics)
- **Data:** Room (SQLite) encrypted at rest with SQLCipher, Paging 3 for
  large lists, DataStore for settings
- **Security:** AndroidKeyStore-backed passphrase encryption gated behind
  `BiometricPrompt` (accepts any device-default unlock method)
- **Voice:** On-device speech-to-text via a bundled Whisper (tiny.en) model
  run through TensorFlow Lite — fully offline, no network call ever made
- **Architecture:** Manual dependency injection (no Hilt/Dagger), MVVM,
  Kotlin Flow-based reactive state
- **Min SDK:** 26 · **Target/Compile SDK:** 37

### Building

```bash
./gradlew assembleDebug
```

### Testing

```bash
./gradlew testDebugUnitTest          # JVM unit tests
./gradlew connectedDebugAndroidTest  # instrumented tests (requires a connected device/emulator)
```

## License

All rights reserved.
