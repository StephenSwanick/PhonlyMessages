# Phonly Messages — continuation notes

Public repo: https://github.com/StephenSwanick/PhonlyMessages (GPL-3).
`origin` = PhonlyMessages. `upstream` = FossifyOrg/Messages.
`main` = working branch. `clone` = frozen Fossify at `25971576` — do not commit on `clone`.

This app stays its own public repo. Not inside PhonlyV1 / Phonly Code / Phonly Phone.
Kotlin namespace stays `org.fossify.messages`. Install id is `co.phonly.messages` (debug and release; no `.debug` suffix).
Voice/dialer lives in Phonly Phone (`co.phonly.phone`). Do not put calling here.

Sibling checkout: `C:\Users\steph\ERC Today Dropbox\SwanickCPA\Phonly\Phonly Phone`
Phone notes: that folder’s `NOTES.md`. Copy patterns from there; do not edit Phone unless asked.

## Status (2026-09-01)

Overlay bubbles are **dead**. CONVERGE clears `SYSTEM_ALERT_WINDOW`. Live cue is a delayed Esper `NOTIFY_DEVICE`. PhonlyAPI owns the timer. This APK only reports facts. Phone and Messages share one Mongo `notification` object (one wait, one card).

**Git:** branch `cue/notify-device` from 1.0.0 allowlist (`a93793bc`) + brandmark (`a8be98bc`). Overlay 1.1.0 / `VERSION_CODE` 26 is **not** on this branch. Do not merge `UnreadOverlayService`, bubbles, or `SYSTEM_ALERT_WINDOW`.

**This APK:** foss **`1.2.0` / `VERSION_CODE` 29** on AAAAY (CONVERGE in place over 28; no uninstall). Package `co.phonly.messages`. Esper library `e57df4aa-4589-414e-95cd-324c3afb6251`, version row **29** `d1da19fa-e086-45a1-910a-0deda3920471`. **PhonlyV2 - DEV** blueprint **v29.0** SHOWs it. Esper-signed with `%LOCALAPPDATA%\phonly-messages-signing\`. Does **not** `<permission>`-declare `co.phonly.permission.NOTIFICATION` — Phone already owns that name (27 hit `INSTALL_FAILED_DUPLICATE_PERMISSION`). Messages only `uses-permission`.

**Home is “left.”** Recents swipe-kill is not required. 28 skipped pile on stale local `alreadySent` / `piledThisCycle` after LOOKED. 29 POSTs pile whenever Messages is not on screen; the API ignores extra piles.

**SMS cue proven (AAAAY, 2026-09-01):**
- 14:32 SMS, Messages closed → card **14:34** (**Phonly** / **You have unread messages or a missed call.**). Close is not looked.
- Extra texts after Close stay quiet (`alreadySent`).
- **29 Home path:** 15:53 SMS → card **15:55**; Close; 15:58a extra (quiet); **opened Messages** (LOOKED); Home; 15:58b → wait 15:59 → card **16:00**. Swipe-close-all not required.
- USB debug cannot POST (Knox). No fleet CONVERGE. No KSP Save.

**Next lab: missed calls on Phone** (same card, same Mongo). Recents open = looked; Home = left; Close on the Esper dialog is still not looked. LOOKED/LEFT between Phone and Messages is one-way until Phone adds `signature|knownSigner` with both certs. Do not edit Phone from this folder unless asked. Phone notes: sibling `Phonly Phone/NOTES.md`.

**Token / IMEI (not in git):** rebuild after the operator sets Render env. File `%LOCALAPPDATA%\phonly-messages-signing\notification.properties` (or the Phone signing folder):

```
token=<NOTIFICATION_SECRET or DEVICE_NOTIFICATION_LAB_TOKEN>
imei=<14–16 digit IMEI in Mongo>
```

Env `NOTIFICATION_SECRET` / `DEVICE_NOTIFICATION_LAB_TOKEN` also work. **Signed 1.2.0 does not bake an IMEI** — it reads this phone’s IMEI at runtime (telephony, then Esper AppConfig `device_imei`). Debug may still use a local `imei=` override. Empty token → log `WOULD_PILE` / `WOULD_LOOKED` / `WOULD_CLEAR` and keep going. SMS must not crash if the API is down (503).

**Facts only:** `GET /api/device/notification`, `POST .../pile|looked|clear` at `https://phonlyv1.onrender.com`. JSON `{ "imei", "source": "messages" }`. No SMS body, no thread id. No local 15-minute timer. No AlarmManager for this. No Esper key.

**When:** unread SMS/MMS while Messages is not in front → POST pile (8s debounce). API does not restart an active wait or a sent card. Open inbox/thread → POST looked + broadcast `NOTIFICATION_LOOKED` to Phone. Home / Recents (not swipe-kill) with unread → POST pile (unless Phone Recents is in front). Inbox empty → POST clear. Boot GET must not overwrite a LOOKED that landed while the GET was in flight. Phone LOOKED holds pile until Phone LEFT.

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
- `app_restrictions.xml` + manifest `APP_RESTRICTIONS` meta-data, same key `allowlist_json`.
- Dropbox build dir: Phone uses `%USERPROFILE%\AppData\Local\phonly-phone-build`. Use `%USERPROFILE%\AppData\Local\phonly-messages-build` here (see Phone root `build.gradle.kts`). Do not sync `app/build`, `build`, `.gradle` in Dropbox.

## Practical build notes

- `JAVA_HOME` = Android Studio `jbr`, `ANDROID_HOME` = `%LOCALAPPDATA%\Android\Sdk`.
- Debug: `.\gradlew.bat assembleFossDebug` → `%USERPROFILE%\AppData\Local\phonly-messages-build\app\outputs\apk\foss\debug\`.
- Release (Esper): `.\gradlew.bat assembleFossRelease`.
- Test device: AAAAY (Galaxy A16, T-Mobile, V2-DEV). Phone already holds `ROLE_DIALER` there.

## Suggested next

1. **Phone miss cue on AAAAY** (operator + Phone agent). Allowlisted miss, Recents not in front → one wait → one card. Close → extra misses stay quiet. Open Recents (looked) → Home → later miss → new card. Do not swipe-kill Phone. Same 60s lab delay / 1-minute cron.
2. Do not fleet CONVERGE. Do not Save KSP. Do not merge overlay here.

Do not mix Esper V1=V2 cutover, PhonlyV1 codebase, or Knox Manage into this folder unless asked.
