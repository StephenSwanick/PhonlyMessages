# Phonly Messages — continuation notes

Public repo: https://github.com/StephenSwanick/PhonlyMessages (GPL-3).
`origin` = PhonlyMessages. `upstream` = FossifyOrg/Messages.
**Work on `main`.** `clone` = frozen Fossify at `25971576` — do not commit on `clone`.

This app stays its own public repo. Not inside PhonlyV1 / Phonly Code / Phonly Phone.
This chat is Messages-only. Do not edit PhonlyV1 / Phonly Code or Phone unless the operator explicitly asks in this session.
Kotlin namespace stays `org.fossify.messages`. Install id is `co.phonly.messages` (debug and release; no `.debug` suffix).
Voice/dialer lives in Phonly Phone (`co.phonly.phone`). Do not put calling here.
**`co.phonly.contacts` is retired.** Do not install it, query it, push Esper AppConfig to it, or wait on a contacts helper APK. Names in threads are Android Contacts Provider. Messages does not write contacts.

Sibling checkout: `C:\Users\steph\ERC Today Dropbox\SwanickCPA\Phonly\Phonly Phone`
Phone notes: that folder’s `NOTES.md`. Copy patterns from there; do not edit Phone unless asked.

## Status (2026-09-11) — Messages 36 SHOW on PhonlyV2 - DEV v52.0

**1.4.0 / 36** is in the Esper library and SHOW on **PhonlyV2 - DEV v52.0**. Live Home/School/TS stay on **34**. Do not fleet CONVERGE. Do not Save KSP. AAAAY still needs a CONVERGE to install 36.

**This APK:** foss **`1.4.0` / `VERSION_CODE` 36**. Package `co.phonly.messages`. Esper-signed with `%LOCALAPPDATA%\phonly-messages-signing\`. Library `e57df4aa-4589-414e-95cd-324c3afb6251`, version `e8a38a64-342b-44fb-ac66-e14f28440d84`. APK: `%LOCALAPPDATA%\phonly-messages-build\app\outputs\apk\foss\release\messages-36-foss-release.apk`.

**This cut vs 35:** Archive is locked with the same Esper key as Delete (`disable_delete`). Already-archived threads restore to the inbox on launch. **`co.phonly.contacts` is retired** — do not wait on it; names stay Android Contacts Provider; Messages does not write contacts.

**Allowlist:** Esper boolean **`allowlist_enabled`** next to **`allowlist_json`**. Only explicit `false` allows all SMS. Missing key or `true` keeps today’s empty-list / missing `sms` = emergency only. This APK does **not** parse Mongo `devices.allowList` (`{ enabled, updatedAt, entries }`). Do not read `label` from `allowlist_json` for display.

**Delete / archive:** off by default (`disable_delete`). Kids cannot delete threads, single messages, recycle-bin contents, or use notification Delete. They also cannot archive threads to hide them. Only explicit `false` allows delete and archive. Copy / forward / share stay.

34 cue behavior on AAAAY is still the locked product (pile / looked / hold). This cut does not change that.

## Status (2026-09-09) — Messages 35 on AAAAY (V2-DEV); history

**1.3.0 / 35** was SHOW on PhonlyV2 - DEV **v46.0** and on **AAAAY** (operator: Phone and Messages enterprise apps installed). Superseded on DEV by **36** / **v52.0**. Live Home/School/TS stay on **34** until asked. Do not fleet CONVERGE. Do not Save KSP.

**This APK:** foss **`1.3.0` / `VERSION_CODE` 35**. Package `co.phonly.messages`. Esper-signed with `%LOCALAPPDATA%\phonly-messages-signing\`. Library `e57df4aa-4589-414e-95cd-324c3afb6251`, version `737a9101-5238-4e86-ac16-600f25b2e8bf`. APK: `%LOCALAPPDATA%\phonly-messages-build\app\outputs\apk\foss\release\messages-35-foss-release.apk`.

**Allowlist:** Esper boolean **`allowlist_enabled`** next to **`allowlist_json`**. Only explicit `false` allows all SMS. Missing key or `true` keeps today’s empty-list / missing `sms` = emergency only. This APK does **not** parse Mongo `devices.allowList` (`{ enabled, updatedAt, entries }`). Names in threads stay Android Contacts Provider. Do not write contacts from Messages. Do not read `label` from `allowlist_json` for display. Do not wait on **`co.phonly.contacts`** (retired).

**Delete:** off by default (`disable_delete`). Kids cannot delete threads, single messages, recycle-bin contents, or use notification Delete. **Archive is locked with the same key** (kids cannot hide threads from the inbox). Only explicit `false` allows delete and archive. Copy / forward / share stay.

**Send to phone (2026-09-09):** dashboard push updated Esper AppConfig on **Phone** and **Messages** (`allowlist_json` + `allowlist_enabled: true`). Ignore leftover “wait for the contacts helper” notes. **`co.phonly.contacts` is retired** — do not push AppConfig to it, do not install it, do not wait on another agent to wire it. Testing of 35 (allowlist flag, delete lock) follows elsewhere.

34 cue behavior on AAAAY is still the locked product (pile / looked / hold). This cut does not change that.

## Status (2026-09-08) — Messages 35 uploaded (history)

Library upload + SHOW on PhonlyV2 - DEV **v46.0** that day. AAAAY was still on **34** until the operator CONVERGEd V2-DEV (see 2026-09-09). Do not pin over **34** on live Home/School/TS. Do not fleet CONVERGE.

## Status (2026-09-02) — Messages 34 proven on AAAAY

This chat is Messages-only. Do not edit Phonly Code or Phone. Do not hold the Esper key. No overlay. No second local timer. Close on the Esper dialog is not looked. Recents is not read texts. Two HTTP clients stay two clients.

PhonlyAPI slot (messages only): `{ cardStatus: idle|waiting|shown, reminderStartedAt, cardShownAt }`. Do not parse or write `alreadySent` / `waitingSince` / `sentAt`. HTTP: GET `/api/device/notification?imei=&source=messages`. POST `/pile|looked|clear` body `{ imei, source: "messages" }`. Copy is API: title **Phonly**, body **You have a missed text.**

**This APK:** foss **`1.2.0` / `VERSION_CODE` 34**. Package `co.phonly.messages`. Esper-signed with `%LOCALAPPDATA%\phonly-messages-signing\`. Library `e57df4aa-4589-414e-95cd-324c3afb6251`, version `4bde4024-de94-4bb7-9c64-5f3c622a721a`. SHOW on PhonlyV2 - DEV **v42.0**. APK: `%LOCALAPPDATA%\phonly-messages-build\app\outputs\apk\foss\release\messages-34-foss-release.apk`. Operator CONVERGEd **AAAAY only** (2026-09-02). Do not fleet CONVERGE. Does **not** `<permission>`-declare `co.phonly.permission.NOTIFICATION`. Overlay 1.1.0 / 26 is history only. Phone on this device may still be **33** until that agent CONVERGEs.

**34 proven (AAAAY, operator):** unread while closed → pile → text card (`waiting` then `shown`); Close stays `shown`; extra SMS does not restart `reminderStartedAt`; open Messages → `looked` → `idle`; Home with same unread → no pile; SMS while Messages is open, then Home → no pile; new SMS after Home → pile → card. Call history must not change `notification.messages`. logcat tag `PhonlyCue`. USB is not Messages DNS.

**34 behavior (locked):**
- Messages closed + unread → `POST pile` (~8s). Cron → `shown` (~1 min). Extra texts do not restart `reminderStartedAt`.
- Open Messages → `POST looked` → `idle`. Home with those same unreads → **no** `pile` (`leave; hold, no pile`).
- New SMS/MMS while Messages is in front → no card; Home still **no** `pile` (`incoming; Messages in front, hold`).
- New SMS/MMS after Home → `incoming; hold released` then `POST pile` → card.
- Empty inbox → `POST clear` `source=messages` only.
- Phone LOOKED/LEFT ignored.

**33 (history, Esper v41.0, do not ship):** looked + hold for same unreads on Home. Hole: incoming always released the hold, so a text while Messages was open piled on leave. Library version `ca393b9d-45d5-4eab-8832-bc8ff5bb1577`.

**32 (history):** debounce kept; ignore Phone LOOKED; ranked poster + shortService FGS. Hole: Home after look re-piled the same unreads (1:14 looked → 1:14:53 leave → 1:15:01 pile). 1:13 text → 1:14 card; 1:15 miss → 1:16 call card (fork works).

**Wire:** GET `/api/device/notification?imei=&source=messages`. POST `/pile|looked|clear` body `{ imei, source: "messages" }`. Missing source → 400. Copy is API: title **Phonly**, body **You have a missed text.**

**Mongo:** `devices.notification.messages` is created on first pile, never deleted. Looked/clear set `cardStatus` idle (`reminderStartedAt` null, `cardShownAt` kept). They must not touch `notification.phone`.

**31 proof (history, APK unchanged):** 9:33 SMS → card 9:35; Close; extra 9:41 no restart; 9:38 miss never reached Phone Mongo (Phone DNS, later fixed on Phone 33); Call history did not clear texts; opening Messages is looked; 9:48 SMS → card 9:50.

**AAAAY proof (APK 31 unchanged) — 2026-09-02 morning, fork holds for Messages.**

- 9:33 SMS. Messages `UnknownHostException` then `network up` + `POST pile` 9:34:03. Mongo `messages.waitingSince` `2026-09-02T13:34:03.542Z`.
- Text card 9:35:05 (`sentAt` `13:35:05.258Z`, `alreadySent: true`). Copy is the messages card.
- Close on Esper: Mongo unchanged (Close is not looked).
- Extra SMS 9:41: Messages `POST pile` 9:42:09; API did not restart `waitingSince`.
- 9:38 miss (caller hung up, `disconnect=5`, Recents `type=3`). Phone **29180** `UnknownHostException` for `phonlyv1.onrender.com` from 9:38:43 through 9:43+ — no `phone` slot, no miss card. USB connected is not Phone DNS.
- Opening **Phone** lands on **Call history** (that is Recents; no separate Recents button). After that, `messages.waitingSince` / `sentAt` **unchanged**. Recents does not clear texts.

Last night’s shared lock would have wiped the 9:33 wait when Phone opened. Forking isolates Messages from Phone’s post-call resolver failures.

**Looked (9:47):** opening the **Messages** launcher (conversation list; no Home first, no thread tap) → `waitingSince: null`, `alreadySent: false`, `sentAt` kept `13:35:05.258Z`. Slot was not deleted. Inbox in front is looked. Home with unread still there can start a **new** pile/wait. Do not CONVERGE.

**New wait after looked:** 9:48 SMS → text card **9:50** (8s pile + ~60s delay + cron). Looked does not lock them quiet forever; leaving Messages with unread/new text starts a fresh messages wait. Phone Call history still must not touch it.

---

## Status (2026-09-01, evening) — shared lock (history)

Overlay bubbles are **dead**. CONVERGE clears `SYSTEM_ALERT_WINDOW`. Live cue is a delayed Esper `NOTIFY_DEVICE`. PhonlyAPI owns the timer. This APK only reports facts.

**Git:** live work is **`main`**. Cue source (31) + these notes land here. Overlay 1.1.0 / `VERSION_CODE` 26 is **history only** — do not ship `UnreadOverlayService`, bubbles, or `SYSTEM_ALERT_WINDOW` in the live APK. `cue/notify-device` was the lab branch from 1.0.0 allowlist (`a93793bc`) + brandmark (`a8be98bc`); do not keep using it as the working branch.

**This APK:** foss **`1.2.0` / `VERSION_CODE` 31**. AAAAY is on **31** (operator CONVERGEd; matches Phone 31). Package `co.phonly.messages`. Esper-signed with `%LOCALAPPDATA%\phonly-messages-signing\`. Does **not** `<permission>`-declare `co.phonly.permission.NOTIFICATION` — Phone already owns that name (27 hit `INSTALL_FAILED_DUPLICATE_PERMISSION`). Messages only `uses-permission`.

**Shared Mongo (superseded 2026-09-02):** Phone and Messages shared one `devices.notification { alreadySent, waitingSince, sentAt }`. One wait, one card. Copy was **You have unread messages or a missed call.** Lab delay `NOTIFICATION_DELAY_MS=60000`. Cron `PhonlyScheduler` every minute → `POST /api/cron/schedule-sync`. API `https://phonlyv1.onrender.com`.

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

**What would make it workable (superseded 2026-09-02):** API source-scoped looked/clear + two locks shipped in PhonlyAPI `63e43f0`. One HTTP poster was **rejected** — two clients are correct. Leftover APK hygiene after AAAAY proof: debounce; ignore Phone LOOKED for the wait.

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

- Source of truth: Mongo `phonly.devices.allowList` (Phonly Code). No Mongo in this APK. Do not parse the Mongo object (`{ enabled, updatedAt, entries }`).
- Delivery: Esper managed config on package **`co.phonly.messages`**: string **`allowlist_json`** (entries array only), boolean **`allowlist_enabled`**.
- **`allowlist_enabled`:** only an explicit `false` (boolean, or strings `false` / `0`) means allow all SMS. Missing key or `true` keeps today’s empty-list / missing `sms` = emergency only. Do not treat a missing key as open — live kids stay locked until PhonlyAPI pushes `false`.
- Esper AppConfig is **per package**. The same JSON must be pushed to Phone and to Messages. Do not assume Messages can read Phone’s restrictions.
- Shape of `allowlist_json`: JSON **string** whose contents are an array:

```json
[{"e164":"+17046180435","label":"Test","voice":true,"sms":true}]
```

Esper’s device AppConfig box for this package must be:

```json
{
  "allowlist_json": "[{\"e164\":\"+17046180435\",\"label\":\"Test\",\"voice\":true,\"sms\":true}]",
  "allowlist_enabled": true
}
```

Messages uses `sms: true` (default **false** if the key is missing — only numbers explicitly allowed to text). Phone uses `voice` (default true). Keep that split.  
When the list is **on**: missing, blank, `[]`, or invalid JSON → emergency only.  
When the list is **off** (`allowlist_enabled: false`): any number may be texted; 911 is always allowed.  
Names: Android Contacts Provider. Do not write contacts from Messages. Do not read `label` from `allowlist_json` for display. **`co.phonly.contacts` is retired** — do not wait on it.  
Plug-in: `SmsAllowlist`.

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

## DISABLE-DELETE (in 35)

Kids must not hide SMS/MMS. Not phone calls / call log — that is Phonly Phone.

Esper boolean **`disable_delete`** on package **`co.phonly.messages`**. Only an explicit `false` (boolean, or strings `false` / `0`) allows delete **and archive**. Missing key or `true` keeps both off. The child cannot turn it on in Settings.

When delete/archive is off:

- Hide every Delete control: single message (thread CAB), whole conversation (thread menu + conversation list), archived threads, Recycle Bin empty/permanent delete, notification Delete (`DeleteSmsReceiver`).
- Hide every Archive control: conversation list CAB, thread Archive, main-menu “show archived”, Settings “keep conversations archived”.
- No-op the delete functions so a hidden UI or leftover intent cannot wipe the telephony store. MMS resend cleanup may still delete the original duplicate (`bypassDeleteGuard`).
- No-op archive writes (`updateConversationArchivedStatus(..., true)`). Unarchive still runs so existing hidden threads come back.
- On launch, restore any already-archived threads to the inbox (telephony + Room). Incoming SMS/MMS also unarchive.
- Lock Recycle Bin Settings (child can otherwise turn it off and delete for real). Recycle Bin auto-expire is also off.
- Canceling an unsent scheduled message stays allowed.

**Requires this app to be the default SMS app.** Android only lets `ROLE_SMS` write/delete the SMS provider. Google Messages / Samsung Messages must stay HIDE/DISABLE (already the fleet plan) or the child can delete there.

Does **not** cover: copy, forward, share (message stays on the device); factory reset / uninstall if Esper still allows those; the other party’s phone.

Dashboard / Mongo / Esper push of the key is **Phonly Code + V1**. Until Code pushes `false`, missing key = delete off.

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
- `app_restrictions.xml` + manifest `APP_RESTRICTIONS` meta-data. Today: `allowlist_json`, `allowlist_enabled`, `disable_delete`, `device_imei`.
- Dropbox build dir: Phone uses `%USERPROFILE%\AppData\Local\phonly-phone-build`. Use `%USERPROFILE%\AppData\Local\phonly-messages-build` here (see Phone root `build.gradle.kts`). Do not sync `app/build`, `build`, `.gradle` in Dropbox.

## Practical build notes

- `JAVA_HOME` = Android Studio `jbr`, `ANDROID_HOME` = `%LOCALAPPDATA%\Android\Sdk`.
- Debug: `.\gradlew.bat assembleFossDebug` → `%USERPROFILE%\AppData\Local\phonly-messages-build\app\outputs\apk\foss\debug\`.
- Release (Esper): `.\gradlew.bat assembleFossRelease`.
- Test device: AAAAY (Galaxy A16, T-Mobile, V2-DEV). Phone already holds `ROLE_DIALER` there.

## Overlay 1.1.0 (dead — history)

Signed **1.1.0 / 26** shipped `SYSTEM_ALERT_WINDOW` + `UnreadOverlayService` and was lab-proven over Esper Home (2026-08-30). KSP Appear-on-top: **Phone Group 1, Messages Group 2 (Messages last)**. CONVERGE still clears that grant. **Do not revive this path.** Live cue is Esper `NOTIFY_DEVICE`. Keep overlay Kotlin/layouts **out of `main`**.

## Suggested next

This Messages chat: **36** is SHOW on **PhonlyV2 - DEV v52.0**. AAAAY will not install it until that device CONVERGEs. Live Home/School/TS stay on **34**. **`co.phonly.contacts` is retired** — ignore leftover “wait for the contacts helper” notes. Do not fleet CONVERGE. Do not Save KSP unless asked.

Do not funnel HTTP through Phone. Do not merge overlay. `knownSigner` is Phone’s job, not a cue blocker.

Do not mix Esper V1=V2 cutover, PhonlyV1 codebase, or Knox Manage into this folder unless asked.
