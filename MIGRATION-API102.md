# Modern Xposed API 102 migration

This branch (`modern-api-102-general`) is the only branch used for the Modern Xposed API migration.

## Frozen Legacy releases

Do not merge migration work into either frozen Legacy branch:

- `master`: general Legacy release, frozen at `07579ce`
- `pixel-android-16-notification-badge`: Pixel Legacy release, frozen at `110f396`

The migration build temporarily uses the package name `com.neko7ina.mipush.modern` so it cannot overwrite the frozen general release during incomplete testing. It will return to `com.neko7ina.mipush` only after feature parity and end-to-end validation.

## Current stage: API 102 functional parity and end-to-end validation

The APK is now declared as a Modern Xposed module with:

- `io.github.libxposed:api:102.0.0`
- `META-INF/xposed/java_init.list`
- `META-INF/xposed/module.prop`
- `META-INF/xposed/scope.list`
- `minApiVersion=102`
- `targetApiVersion=102`
- `staticScope=false`

The Legacy `assets/xposed_init` entry, Xposed metadata in `AndroidManifest.xml`, old `XposedMod`, API 82 dependency, and Legacy R8 keep rules have been removed. The source tree and release APK contain no `de.robv.android.xposed` references.

The process-local Modern runtime, logging bridge, and central hook DSL use API 102 interceptors. The compatibility DSL preserves the existing `doBefore`, `doAfter`, `replace`, mutable arguments, result/throwable, and unhook call shapes without invoking Legacy APIs. General fake-device dispatch now uses a framework-neutral `LoadedPackage` value.

On the Pixel Android 16 test device, Modern API 102 has been validated in `system_server`, XMSF, QQ, and QQ:MSF. Android 16 `SystemProperties` spoofing works, system_server is explicitly excluded from device spoofing, and a MiPushFramework historical resend successfully exercised notification channel creation, query, publish, and cancel with the QQ package identity.

The fully Modern, no-Legacy-dependency build (`0dffdd7`, versionCode 229) also passed a genuine server-side delivery test on 2026-07-29. QQ:MSF was absent before and after delivery (and Android rejected attempts to start it as a bad process), while XMSF logged `From Server` with `action=SendMessage` for the new `Api102 modern 测试` message. The API 102 bridge then created the QQ channel and published the notification as `com.tencent.mobileqq` with `opPkg=android`, its 68x68 small icon, 100x100 large icon, and `MessagingStyle` payload intact. This was a live vendor push, not a historical resend.

**Migration builds are not functionally equivalent to the frozen Legacy release yet because the general HyperOS/SystemUI path still requires compatible-device validation.**

## Migration gates

1. [x] Replace `XLog` and the central `XPosedX.kt` hook DSL with API 102 interceptors.
2. [x] Replace direct `XposedBridge`, `XposedHelpers`, `XC_MethodHook`, and `AndroidAppHelper` usage.
3. [x] Port general package dispatch for `system_server`, SystemUI, XMSF, and selected client apps.
4. [x] Restore Android 16 `SystemProperties` hooks and `Build.*` Unsafe fallback code paths.
5. [x] Restore and locally validate XMSF notification bridge, permissions, icon handling, and notification delivery.
6. [x] Remove `de.robv.android.xposed:api:82`, Legacy entry classes, and Legacy keep rules.
7. [x] Revalidate the no-Legacy-dependency build in `system_server`, XMSF, QQ, and QQ:MSF.
8. [ ] Validate the general SystemUI/HyperOS path on a compatible device; Pixel-specific behavior remains out of scope.
9. [x] Verify genuine server-side QQ vendor-push delivery while QQ:MSF is absent.
10. [ ] Repeat destructive fresh-registration validation only if release validation requires it; the existing controlled fresh registration remains valid.
11. [ ] Only after parity, restore package name `com.neko7ina.mipush` and publish a separate Modern release.

Pixel-specific SystemUI behavior is intentionally out of scope for this general migration branch.
