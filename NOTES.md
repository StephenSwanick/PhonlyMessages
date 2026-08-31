# Phonly Messages — continuation notes

Public repo: https://github.com/StephenSwanick/PhonlyMessages (GPL-3).
`origin` = PhonlyMessages. `upstream` = FossifyOrg/Messages.
`main` = working branch. `clone` = frozen Fossify at `25971576` — do not commit on `clone`.

This app stays its own public repo. Not inside PhonlyV1 / Phonly Code / Phonly Phone.
**This chat is Messages-only.** Do not edit PhonlyV1 / Phonly Code or Phone unless the operator explicitly asks in this session.
Kotlin namespace stays `org.fossify.messages`. Install id is `co.phonly.messages` (debug and release; no `.debug` suffix).
Voice/dialer lives in Phonly Phone (`co.phonly.phone`). Do not put calling here.

Sibling checkout: `C:\Users\steph\ERC Today Dropbox\SwanickCPA\Phonly\Phonly Phone`
Phone notes: that folder’s `NOTES.md`. Copy patterns from there; do not edit Phone unless asked.

## Status (2026-08-30, session close)

Unread overlay **built, signed, and lab-proven on Esper Home** with the signed APK + fleet KSP grant (not adb).

**APK:** foss release **1.1.0 / `VERSION_CODE` 26**. Esper enterprise library **Messages**, `co.phonly.messages` (app id `e57df4aa-4589-414e-95cd-324c3afb6251`, version row `89b5b0fb-80b7-4722-a447-fac1e547df7b`). Esper UI: version `1.1.0`, release tag `0`. 1.0.0 / 25 remains in the library unused. Signed APK: `%USERPROFILE%\AppData\Local\phonly-messages-build\app\outputs\apk\foss\release\messages-26-foss-release.apk`. Upload: `node scripts/esper-upload-library.mjs`.

**Esper (this session, operator asked):** PhonlyV2 - DEV published **v24.0** — SHOW 1.1.0 **4th** (Phone, VVM, Clock, Messages). **CONVERGE AAAAY only** (`Tester SPA-NNZ-AAAAY`, `c3784e4e-d90c-4a6f-a098-f66a236c8bb8`). No fleet CONVERGE. Installer on device: `io.shoonya.shoonyadpc`. `firstInstallTime` 2026-08-30 14:57.

**KSP (operator, Play MCM iframe, fleet-wide):** Device-wide + application management already on. **Enable permission controls** on. Under **Permission Controls** (Premium): **Group 1 Appear on top → `co.phonly.phone`; Group 2 Appear on top → `co.phonly.messages` (Messages must be last).** Do not overwrite this iframe (RCS/battery/LPM live there). Debug mode stays **off**. Comma-separated packages in one group do not work. USB / Settings taps are not the fleet grant.

**KSP grant restore (AAAAY, 2026-08-30 evening):** Phone missed-call overlay worked; Messages bubbles stopped. Device: Phone Appear-on-top **on**, Messages **off**. Iframe already had both groups Configured (Messages Group 1 / Phone Group 2) — not an overwrite. One-line `co.phonly.messages,co.phonly.phone` did not turn Messages on (may not have pushed). Reverted to two groups. Swapped order so **Messages is last**, outer Play Save, AAAAY reboot → **both toggles on**. Count as working; **monitor**. Not luck: Knox re-grants the last group on a real push; earlier grants stick. Not a kid Settings setting. After a Phone reinstall/CONVERGE, if Messages’ toggle drops: keep this order, Save, reboot. Prove the bubble with a test SMS from Home, not only the toggle.

**Lab prove (AAAAY, signed 1.1.0, Esper Home):** 1:1 SMS bubble; two distinct group threads → two bubbles; no ghost orange “9”. Tap opens thread. Count badge. Group two-heads icon. Overlay AppOps `SYSTEM_ALERT_WINDOW` **allow** after KSP save (new uid from Esper install; debug adb grant died at uninstall). USB does not grant overlay.

**Code:** `SYSTEM_ALERT_WINDOW` + `UnreadOverlayService`. Bubbles = simple-threads inbox unread only (not raw SMS/MMS thread_ids). Distinct groups stay distinct (no subset merge). Nameless/ghost thread ids not drawn. No Settings snackbar if grant missing.

**Not proven:** cap-3 + “more”; delete-hides; hide while thread on screen (inbox open); reboot re-show; Home↔School; deny toast / inbound silent drop; iPhone RCS after Google Messages DISABLE.

**Do not:** fleet CONVERGE; live Home copy until reboot + Home↔School look boring; re-enable KSP Debug; put kid numbers in KSP; put Messages back as Group 1 (that order lost the Messages toggle after the Phone uid swap).

## Status (2026-08-28)

Unread-text overlay **not built**. Research + UX agreed. Build in the **next Messages session**. Coordinate Knox “Appear on top” with the **V1 agent** (not this chat). Do not live-Home this until a lab prove.

Operator: Messages is already the default SMS app on V2; current 1.0.0 is deploying. This session did not change the APK.

Agreed unread cue (kid unit: no Settings, no notification shade):

- Small **bubble** (app-icon size), corner of the screen, over Esper Home. Contact photo or initial. Count badge if that person has more than one unread. Draggable. Tap opens that thread. Stays until that thread is read / deleted / on screen. Not a banner, not a big card (those cover the app grid).
- **One bubble per conversation.** Same sender: update that bubble. Two senders: two bubbles. Cap at three; further unread collapses to a “more” bubble that opens the inbox.
- Fossify has receive/read hooks; no floating window. Moderate new overlay code (`SYSTEM_ALERT_WINDOW`). Do not copy Phone’s Settings snackbar. Do not use Samsung Floating / Smart pop-up / chat bubbles.
- **V1:** KSP Permission Controls → Appear on top for `co.phonly.phone`, then `co.phonly.messages` (Play iframe, not a blueprint). Lab-prove over Home, reboot, Home↔School. If Home hides the window, the bubble cannot ship. Status-bar notify is a weak backup (volume 0, no shade).

## Status (2026-08-23, session close)

This Messages session’s work is done. V2-DEV SHOW / launcher order / CONVERGE belong to the **V1 agent** — do not redo them here.

Signed **1.0.0 / `VERSION_CODE` 25** foss release is in the Esper enterprise library as **Messages**, `co.phonly.messages` (app id `e57df4aa-4589-414e-95cd-324c3afb6251`, version row `7c7059ca-beed-40b5-8b04-430c5e161e97`). Esper listed `version_code` oddly as `1.0.0` / `release_name` `0`. **Not SHOWn** on PhonlyV2 - DEV by this agent.

AAAAY: operator uninstalled Messages. Debug sideload is gone. Google Messages (`com.google.android.apps.messaging`) is gone. **`ROLE_SMS` is empty.** `co.phonly.phone` and T-Mobile VVM (`com.tmobile.vvm.application`) remain. Samsung stock `com.samsung.android.messaging` was never installed on this unit. Signed library APK can be installed via blueprint CONVERGE **after** V1 SHOWs it (debug cert conflict is cleared).

Identity, branding, and SMS allowlist are in the tree. Launcher icon confirmed on AAAAY (foss debug 25, before uninstall). Allowlist landed both packages; **allowed send and group MMS work**. Deny / inbound silent drop **not proven**. iPhone RCS replies did not arrive while Google Messages was still installed (KSP RCS is off; iPhones cache RCS). Should improve after V1 HIDE/DISABLE Google Messages and the number unregisters.

Contacts dual-push (Phone + Messages AppConfig) was patched in Phonly Code this session; dashboard still needs a **PhonlyV1 deploy** if not already live. **V2-DEV layout is V1 only:** SHOW `co.phonly.messages` **4th**, VVM 2nd, HIDE/DISABLE Google Messages. No fleet CONVERGE. AAAAY-only CONVERGE after SHOW. Then set default SMS again (role is empty).

A local `scripts/esper-v2-dev-show-messages.mjs` was started then **deleted** — that job stays in Phonly Code (`scripts/esper/esper-blueprint-v2-dev-vvm-messages.mjs`).

- Package: `co.phonly.messages`. Namespace pinned to `org.fossify.messages`. Launcher **Messages**. About name Phonly Messages. No `.debug` suffix.
- First cut: not minified. Signing key: `%LOCALAPPDATA%\phonly-messages-signing\` (not git, not Dropbox, not the Phone keystore).
- Debug APK: `%USERPROFILE%\AppData\Local\phonly-messages-build\app\outputs\apk\foss\debug\messages-25-foss-debug.apk`.
- Esper-signed release: `%USERPROFILE%\AppData\Local\phonly-messages-build\app\outputs\apk\foss\release\messages-25-foss-release.apk`. Upload: `node scripts/esper-upload-library.mjs`.
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
- `app_restrictions.xml` + manifest `APP_RESTRICTIONS` meta-data. Today: `allowlist_json`. Later: `disable_delete` (or agreed Code key) when DISABLE-DELETE ships.
- Dropbox build dir: Phone uses `%USERPROFILE%\AppData\Local\phonly-phone-build`. Use `%USERPROFILE%\AppData\Local\phonly-messages-build` here (see Phone root `build.gradle.kts`). Do not sync `app/build`, `build`, `.gradle` in Dropbox.

## Practical build notes

- `JAVA_HOME` = Android Studio `jbr`, `ANDROID_HOME` = `%LOCALAPPDATA%\Android\Sdk`.
- Debug: `.\gradlew.bat assembleFossDebug` → `%USERPROFILE%\AppData\Local\phonly-messages-build\app\outputs\apk\foss\debug\`.
- Release (Esper): `.\gradlew.bat assembleFossRelease`.
- Test device: AAAAY (Galaxy A16, T-Mobile, V2-DEV). Phone already holds `ROLE_DIALER` there.

## Suggested next

**V1 / Phonly Code agent:**

1. Do **not** re-SHOW Messages or re-CONVERGE AAAAY unless DEV drifted — already **v24.0** + AAAAY CONVERGED to 1.1.0.
2. Do **not** edit the fleet KSP iframe except to preserve Permission Controls Groups (**Phone first, Messages last**). Debug mode stays off. No kid numbers in KSP. After any Phone reinstall, force a KSP Save + device reboot if Messages’ Appear-on-top toggle drops.
3. Optional lab: reboot AAAAY with unread still there; Home↔School; cap-3 “more”. Then copy V2 layout to live Homes **only if asked**.
4. Contacts dual-push / DISABLE-DELETE: unchanged. Do not start DISABLE-DELETE until this APK reads the flag.

**Next Messages session:**

1. Overlay is in 1.1.0. **Monitor** Appear-on-top: both toggles still on after Phone install / reboot; bubble still draws from Home. Remaining lab: reboot re-show, delete-hides, overflow “more”, thread-on-screen hide.
2. Prove **deny** if not already: non-allowed outgoing → toast **Message not allowed**; inbound not-allowed → silent drop.
3. Recheck iPhone group replies after Google Messages is disabled (RCS unregister is not instant).
4. DISABLE-DELETE: not unless asked.
5. Do not upload or CONVERGE unless asked.

Do not mix Esper V1=V2 cutover, PhonlyV1 codebase, or Knox Manage into this folder unless asked.

## Unread bubble (build spec, 2026-08-28)

Kid-visible cue until the text is read. No Settings, no shade, no Samsung bubbles.

**Look:** circle ~app-icon size, parked in a corner over Esper Home. Photo or initial. Badge if that thread has multiple unread. Draggable so it does not cover launcher icons. Tap → that `ThreadActivity`. Not a top banner, not a full-width card.

**Stacking:** one bubble per conversation. Same thread updates in place. Cap three on screen; overflow is one “more” bubble → conversation list.

**Code (1.1.0):** `SYSTEM_ALERT_WINDOW` + `WindowManager` overlay (`UnreadOverlayService`). Hook show next to `showReceivedMessageNotification` (SMS + MMS). Hide on existing mark-read / cancel paths (`ThreadActivity.onResume`, `MarkAsReadReceiver`, delete, conversation CAB). No snackbar to overlay Settings. Inbox unread threads only; do not subset-merge distinct groups.

**Grant (done 2026-08-30):** Knox KSP Permission Controls (Play iframe, Premium) → Appear on top, **Phone Group 1 / Messages Group 2**. Blueprint cannot grant this. Keep it on the fleet KSP profile. Signed overlay proven over Esper Home on AAAAY. After Phone 1.1.0 uid swap, Messages toggle dropped until this order was pushed and the unit rebooted. Reboot / Home↔School still to prove.

**If Home eats the overlay:** stop. There is no kid-safe fallback. A status-bar notification is not the product (fleet notification volume is 0).
