# Phonly Messages — continuation notes

Public repo: https://github.com/StephenSwanick/PhonlyMessages (GPL-3).
`origin` = PhonlyMessages. `upstream` = FossifyOrg/Messages.
**Work on `main`.** `clone` = frozen Fossify at `25971576` — do not commit on `clone`.

This app stays its own public repo. Not inside PhonlyV1 / Phonly Code / Phonly Phone.
This chat is Messages-only. Do not edit PhonlyV1 / Phonly Code or Phone unless the operator explicitly asks in this session.
Kotlin namespace stays `org.fossify.messages`. Install id is `co.phonly.messages` (debug and release; no `.debug` suffix).
Voice/dialer lives in Phonly Phone (`co.phonly.phone`). Do not put calling here.

Sibling checkout: `C:\Users\steph\ERC Today Dropbox\SwanickCPA\Phonly\Phonly Phone`
Phone notes: that folder’s `NOTES.md`. Copy patterns from there; do not edit Phone unless asked.

## Status (2026-09-01, evening)

Overlay bubbles are **dead**. CONVERGE clears `SYSTEM_ALERT_WINDOW`. Live cue is a delayed Esper `NOTIFY_DEVICE`. PhonlyAPI owns the timer. This APK only reports facts.

**Git:** live work is **`main`**. Cue source (31) + these notes land here. Overlay 1.1.0 / `VERSION_CODE` 26 is **history only** — do not ship `UnreadOverlayService`, bubbles, or `SYSTEM_ALERT_WINDOW` in the live APK. `cue/notify-device` was the lab branch from 1.0.0 allowlist (`a93793bc`) + brandmark (`a8be98bc`); do not keep using it as the working branch.

**This APK:** foss **`1.2.0` / `VERSION_CODE` 31**. AAAAY is on **31** (operator CONVERGEd; matches Phone 31). Package `co.phonly.messages`. Esper-signed with `%LOCALAPPDATA%\phonly-messages-signing\`. Does **not** `<permission>`-declare `co.phonly.permission.NOTIFICATION` — Phone already owns that name (27 hit `INSTALL_FAILED_DUPLICATE_PERMISSION`). Messages only `uses-permission`.

**Shared Mongo:** Phone and Messages share one `devices.notification { alreadySent, waitingSince, sentAt }`. One wait, one card. Copy: title **Phonly**, body **You have unread messages or a missed call.** Close on the Esper card is **not** looked. Extra events while `alreadySent` stay quiet. Lab delay `NOTIFICATION_DELAY_MS=60000`. Cron `PhonlyScheduler` every minute → `POST /api/cron/schedule-sync`. API `https://phonlyv1.onrender.com`.

**31 behavior:** first pile POST **8s** after unread (not a 2-minute wait). No `NET_CAPABILITY_VALIDATED` skip. If POST fails (`UnknownHostException`, 5xx, 429): retry every **15s** until looked or success — no 2-minute give-up. Also retry immediately on `ConnectivityManager` network-up (INTERNET capability, not VALIDATED). Home is “left”; Recents swipe-kill is not required. Esper library `e57df4aa-4589-414e-95cd-324c3afb6251`, version row 31 `4948732f-0d8b-4c60-b4e2-eb555856e3e8`. SHOW on PhonlyV2 - DEV published blueprint **v37.0**. APK: `%LOCALAPPDATA%\phonly-messages-build\app\outputs\apk\foss\release\messages-31-foss-release.apk`.

**SMS cue proven (AAAAY, daytime):**
- 14:32 SMS, Messages closed → card **14:34**. Close is not looked.
- Extra texts after Close stay quiet (`alreadySent`).
- **29 Home path:** 15:53 SMS → card **15:55**; Close; 15:58a extra (quiet); opened Messages (LOOKED); Home; 15:58b → card **16:00**. Swipe-close-all not required.
- USB debug cannot POST (Knox `UnknownHostException` / `installer=null`). No fleet CONVERGE. No KSP Save.

**10:55 PM text — no card (AAAAY, 31). Not a 6-minute outage.**

Cron through 11:00: `sent: 0, skipped: 0` (nobody waiting on the server). Mongo GET ~11:01: `alreadySent: false`, `waitingSince: 2026-09-02T03:01:21.872Z` (11:01:21 PM EDT), `sentAt: null`.

`adb logcat` tag `PhonlyCue` (two PIDs: **29435** Messages, **29180** Phone), device `SM-A166U` / `R5GL55PXBNW`:

- **22:50:28** Phone `POST pile` succeeded; Esper card around **22:52** (`sentAt` 22:52:04). That set `alreadySent`.
- **22:55:08** Messages `pile debounced 8000ms` (the 10:55 SMS). **22:55:17** Messages **`POST pile` succeeded.** API no-op because Phone already owned the one-card lock. Cron stayed 0/0. This is why the text looked like it never queued.
- Phone **29180** kept `UnknownHostException: Unable to resolve host "phonlyv1.onrender.com"` every 15s from ~22:46 through **23:00+**, including after `network up; pile now`. That is **this app uid cannot resolve DNS**, not the whole phone offline. Browser can work. Messages sometimes can POST while Phone cannot.
- **23:00:57** Recents opened → Phone LOOKED. **23:01:00** Phone LEFT + **CLEAR** (`waitingSince=null`). That **wiped** the 10:55 Messages wait even though the inbox was still unread. Recents is “I looked at calls,” not “I read texts.”
- **23:01:01** Messages `POST looked`; **23:01:22** Messages `POST pile` → matches Mongo `waitingSince` 11:01:21. The 11:01 wait is a **new** pile after Phone CLEAR, not a late first POST.

Inbox observer also resets the 8s debounce in bursts (`pile debounced` many times in one second).

**Why it feels erratic (three clocks):**
1. One shared “already told you” flag — a miss card blocks a later text (and the other way around) until they open Recents *or* Messages.
2. Looking resets the wait — Recents LOOKED/CLEAR can wipe an SMS wait; opening Messages is LOOKED for the shared object.
3. Knox per-uid DNS — Phone and Messages do not share “is the network up?” Retries do not invent a resolver. 31 is not the old VALIDATED skip.

**What would make it workable (do all three; ~3 in 4 for lab-useful, not 99%):**
1. **API: source-scoped `looked` / `clear`.** Phone may only clear the miss side. Messages may only clear the SMS side. A card should still fire if the other side is waiting. This is the only change that would have kept the 10:55 wait through the 11:01 Recents open. PhonlyAPI / Phonly Code, not this APK alone.
2. **Product call on “one card.”** Current rule: after a card, extra texts *and* extra misses stay quiet until they open the relevant app; Close is not looked. That is consistent, but a text after a miss card feels broken. Pick one and code it: keep one card until they look; **or** new event after Close starts a new wait; **or** two Mongo locks (miss vs unread). Without this call, miss-then-text will still look flaky even after (1).
3. **One HTTP poster, not a hope that Knox treats both uids the same.** Esper “allow the hostname” may not fix Phone’s `UnknownHostException`. Tonight Messages resolved at 22:55:17 while Phone did not. Funnel Phone facts locally and let Messages POST (or the reverse, whichever uid actually resolves). If both uids fail DNS, there is still no card until Knox lets someone through.

**Not (1)–(3), still leftover:** do not reset the 8s debounce on every inbox observer tick; Phone `signature|knownSigner` + both cert SHA-256s so LOOKED/LEFT actually cross apps (today best-effort; different release certs); 1-minute cron jitter; Esper actually delivering `NOTIFY_DEVICE`.

**Chances:** (1)+(3) without (2) ≈ half — Recents will not steal a live SMS wait and POSTs land more often, but miss-then-text stays silent. (2) without (3) — rules get clearer, Phone still goes quiet for minutes after a call. All three, with (3) as a single HTTP path: **workable as a delayed “you haven’t looked” nudge**, not a guaranteed SMS notification.

**Do not:** treat “browser works” as “Phone can POST”; add another local timer or overlay; fleet CONVERGE; Save KSP; Appear-on-top; put kid numbers in KSP. Do not race Phone git unless asked.

LOOKED/LEFT between Phone and Messages is one-way until Phone adds `signature|knownSigner` with both certs. Phone Recents LOOKED currently holds Messages pile until Phone LEFT.

**Token / IMEI (not in git):** rebuild after the operator sets Render env. File `%LOCALAPPDATA%\phonly-messages-signing\notification.properties` (or the Phone signing folder):

```
token=<NOTIFICATION_SECRET or DEVICE_NOTIFICATION_LAB_TOKEN>
imei=<14–16 digit IMEI in Mongo>
```

Env `NOTIFICATION_SECRET` / `DEVICE_NOTIFICATION_LAB_TOKEN` also work. **Signed 1.2.0 does not bake an IMEI** — it reads this phone’s IMEI at runtime (telephony, then Esper AppConfig `device_imei`). Debug may still use a local `imei=` override. Empty token → log `WOULD_PILE` / `WOULD_LOOKED` / `WOULD_CLEAR` and keep going. SMS must not crash if the API is down (503).

**Facts only:** `GET /api/device/notification`, `POST .../pile|looked|clear` at `https://phonlyv1.onrender.com`. JSON `{ "imei", "source": "messages" }`. No SMS body, no thread id. No local 15-minute timer. No AlarmManager for this. No Esper key.

**When:** unread SMS/MMS while Messages is not in front → POST pile (**8s** debounce). If that POST fails, retry every **15s** until looked or success (no 2-minute give-up). API does not restart an active wait or a sent card. Open inbox/thread → POST looked + broadcast `NOTIFICATION_LOOKED` to Phone. Home / Recents (not swipe-kill) with unread → POST pile (unless Phone Recents is in front). Inbox empty → POST clear. Boot GET must not overwrite a LOOKED that landed while the GET was in flight. Phone LOOKED holds pile until Phone LEFT.

**Do not:** fleet CONVERGE; Save KSP; Appear-on-top; put kid numbers in KSP.

## Status (2026-08-21)

Identity, branding, and SMS allowlist are in the tree. Default launcher icon is the messenger speech-bubble brandmark (rounded square + bottom-right tail, black shield + white check). Debug APK built as `co.phonly.messages` (`VERSION_NAME` 1.0.0, `VERSION_CODE` 24, foss debug). Device sideload / default-SMS / AppConfig proof is paused until asked — do not sideload, grant `ROLE_SMS`, or upload this APK to Esper.

- Package: `co.phonly.messages`. Namespace pinned to `org.fossify.messages`. Launcher **Messages**. About name Phonly Messages. No `.debug` suffix.
- First cut: not minified. **Release** signing key: `%LOCALAPPDATA%\phonly-messages-signing\` (not git, not Dropbox, not the Phone release keystore). **Debug** APK uses `%USERPROFILE%\.android\debug.keystore` so it matches Phone debug (signature permission).
- Cue debug APK: `%USERPROFILE%\AppData\Local\phonly-messages-build\app\outputs\apk\foss\debug\messages-27-foss-debug.apk`.
- Public repo is live: https://github.com/StephenSwanick/PhonlyMessages (GPL-3). `origin/main` = `a93793bc`. `origin/clone` = `25971576`.
- Signed Phone 1.0.0 is already on Esper / AAAAY. Messages needs its own AppConfig on this package.

## What this fork must become

Mirror Phonly Phone, for SMS/MMS:

- Launcher **Messages**, About name Phonly Messages, NOTICE/GPL-3 kept.
- No hardcoded numbers. Enforcement is AppConfig plus 911 / Android emergency (including text-to-911).
- Outgoing: not-allowed → toast **Message not allowed**. Cover every send path, not only the compose UI.
- Incoming (this app must be the **default SMS app**): not-allowed → **silently drop**. Do not insert, notify, or put in recycle bin. `SMS_DELIVER` / MMS receive consumes the message; dropping it is how it stays off the device.
- Fossify Commons “fake version / Play Store” check rewritten at build time so `co.phonly.messages` is accepted (copy Phone’s ASM visitor).
- Default launcher icon = messenger speech-bubble brandmark. Fossify color-icon variants can stay (they share `ic_launcher_foreground`).

## Allowlist

- Source of truth: Mongo `phonly.devices.allowlist` (Phonly Code). No Mongo in this APK.
- Delivery: Esper managed config string key **`allowlist_json`** on package **`co.phonly.messages`**.
- Esper AppConfig is **per package**. The same JSON must be pushed to Phone and to Messages. Do not assume Messages can read Phone’s restrictions.
- Shape: JSON **string** whose contents are an array:

```json
[{"e164":"+17046180435","label":"Test","voice":true,"sms":true}]
```

Esper’s device AppConfig box for this package must be:

```json
{
  "allowlist_json": "[{\"e164\":\"+17046180435\",\"label\":\"Test\",\"voice\":true,\"sms\":true}]"
}
```

Messages uses `sms: true` (default **false** if the key is missing — only numbers explicitly allowed to text). Phone uses `voice` (default true). Keep that split.
Missing, blank, `[]`, or invalid JSON → emergency only.
Plug-in: `SmsAllowlist` modeled on Phone’s `CallAllowlist.allowedNumbers()` / `isNumberAllowed()` / `PhoneNumberUtils.compare`.

Do **not** put kid numbers in KSP / Backbone.

### Send / receive hooks (Fossify)

Outgoing (gate before send; group: **every** address must be allowed):

- `messaging/Messaging.kt` → `sendMessageCompat` (central)
- `activities/ThreadActivity.kt` → `sendMessage`
- `receivers/DirectReplyReceiver.kt`
- `receivers/ScheduledMessageReceiver.kt`
- `services/HeadlessSmsSendService.kt`

Incoming (drop before insert/notify):

- `receivers/SmsReceiver.kt` (already returns early for blocked/filtered)
- `receivers/MmsReceiver.kt` → `isAddressBlocked`

Do not rely on Fossify’s user block-list UI as the allowlist.

## DISABLE-DELETE (possible, not built)

Parents may want to stop a child from deleting **text messages** (SMS/MMS in this app). Not phone calls / call log — that is Phonly Phone.

**Possible.** Same delivery as the allowlist: parent dashboard toggle → Phonly Code (Mongo) → Esper managed config on package **`co.phonly.messages`**. The child cannot turn it off in Settings. An in-app PIN is weaker; do not use one.

Proposed AppConfig boolean **`disable_delete`** (name TBD with Code). When true:

- Hide every Delete control: single message (thread CAB), whole conversation (thread menu + conversation list), archived threads, Recycle Bin empty/permanent delete, notification Delete (`DeleteSmsReceiver`).
- No-op the delete functions so a hidden UI or leftover intent cannot wipe the telephony store.
- Lock Recycle Bin Settings (child can otherwise turn it off and delete for real). Recycle Bin alone is not the parent control.

Kid-device default should almost certainly be **delete off** (`disable_delete: true`). Confirm with dashboard UX when Code adds the toggle.

**Requires this app to be the default SMS app.** Android only lets `ROLE_SMS` write/delete the SMS provider. Google Messages / Samsung Messages must stay HIDE/DISABLE (already the fleet plan) or the child can delete there.

Does **not** cover: archive, copy, forward, share (message stays on the device); factory reset / uninstall if Esper still allows those; the other party’s phone.

Not in this APK yet. Dashboard / Mongo / Esper push is **Phonly Code + V1**, not this chat. Add the restriction key to `app_restrictions.xml` in the same Messages session that gates the delete paths.

## Default SMS app

The APK cannot silently become the default SMS app. Android 10+ `ROLE_SMS` is a user or device-owner grant. Fossify already prompts in `MainActivity`.

Dev-only on a test unit:

```
adb shell cmd role add-role-holder android.app.role.SMS co.phonly.messages
```

On V2-DEV, Settings is SHOW: **Settings → Apps → Choose default apps → SMS app → Messages**.
Fleet: Esper/MDM default-SMS if they expose it, or a one-time prompt the blueprint allows. Do not fake a silent in-app switch.

Sideload/debug vs Esper-signed: different certificates. Same uninstall dance as Phone if swapping certs on AAAAY.

## Identity / Gradle (copy Phone)

Fossify Messages currently sets `namespace = project.property("APP_ID")`. **Pin `namespace = "org.fossify.messages"`** when `APP_ID` becomes `co.phonly.messages`, or Kotlin/R break.

Also:

- `gradle.properties`: `APP_ID=co.phonly.messages`. First Phonly cut: `VERSION_NAME=1.0.0` and bump `VERSION_CODE` above 23.
- Remove `debug { applicationIdSuffix = ".debug" }` so debug and release share `co.phonly.messages`.
- `donottranslate.xml`: `package_name` + `app_name` = Phonly Messages.
- `app_launcher_name` stays **Messages** (launcher label, like Phone’s **Phone**).
- First Esper cut: **do not minify** (`isMinifyEnabled = false`).
- Signing key: `%LOCALAPPDATA%\phonly-messages-signing\` (not git, not Dropbox, **not** the Phone keystore). Lose it and we cannot update this package in place.
- ASM rewrite: copy `PhonlyFossifyClassVisitorFactory` from Phone `app/build.gradle.kts` (`org.fossify.` → `co.phonly.`, `yfissof` → `ylnohp.`).
- `app_restrictions.xml` + manifest `APP_RESTRICTIONS` meta-data. Today: `allowlist_json`, `device_imei`. Later: `disable_delete` (or agreed Code key) when DISABLE-DELETE ships.
- Dropbox build dir: Phone uses `%USERPROFILE%\AppData\Local\phonly-phone-build`. Use `%USERPROFILE%\AppData\Local\phonly-messages-build` here (see Phone root `build.gradle.kts`). Do not sync `app/build`, `build`, `.gradle` in Dropbox.

## Practical build notes

- `JAVA_HOME` = Android Studio `jbr`, `ANDROID_HOME` = `%LOCALAPPDATA%\Android\Sdk`.
- Debug: `.\gradlew.bat assembleFossDebug` → `%USERPROFILE%\AppData\Local\phonly-messages-build\app\outputs\apk\foss\debug\`.
- Release (Esper): `.\gradlew.bat assembleFossRelease`.
- Test device: AAAAY (Galaxy A16, T-Mobile, V2-DEV). Phone already holds `ROLE_DIALER` there.

## Overlay 1.1.0 (dead — history)

Signed **1.1.0 / 26** shipped `SYSTEM_ALERT_WINDOW` + `UnreadOverlayService` and was lab-proven over Esper Home (2026-08-30). KSP Appear-on-top: **Phone Group 1, Messages Group 2 (Messages last)**. CONVERGE still clears that grant. **Do not revive this path.** Live cue is Esper `NOTIFY_DEVICE`. Keep overlay Kotlin/layouts **out of `main`**.

## Suggested next

1. **PhonlyAPI / Phonly Code:** source-scoped `looked` / `clear` (fix 1). Product call on one-card vs new-wait-after-Close vs two locks (fix 2). Do not start that in this APK until Code owns the object shape.
2. **One HTTP poster** (fix 3): Phone facts → Messages POST, or whichever uid actually resolves `phonlyv1.onrender.com`. Esper hostname allow alone is not enough.
3. Messages APK hygiene after (1): do not reset the 8s debounce on every inbox observer tick.
4. Phone: `signature|knownSigner` + both release cert SHA-256s so LOOKED/LEFT cross apps. Do not edit Phone from this folder unless asked.
5. Do not fleet CONVERGE. Do not Save KSP. Do not merge overlay. Do not start DISABLE-DELETE unless asked.

Do not mix Esper V1=V2 cutover, PhonlyV1 codebase, or Knox Manage into this folder unless asked.
