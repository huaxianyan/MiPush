# Modern Xposed API 102 migration

This branch (`modern-api-102-general`) is the only branch used for the Modern Xposed API migration.

## Frozen Legacy releases

Do not merge migration work into either frozen Legacy branch:

- `master`: general Legacy release, frozen at `07579ce`
- `pixel-android-16-notification-badge`: Pixel Legacy release, frozen at `110f396`

The migration build temporarily uses the package name `com.neko7ina.mipush.modern` so it cannot overwrite the frozen general release during incomplete testing. It will return to `com.neko7ina.mipush` only after feature parity and end-to-end validation.

## Current stage: API 102 bootstrap

The APK is now declared as a Modern Xposed module with:

- `io.github.libxposed:api:102.0.0`
- `META-INF/xposed/java_init.list`
- `META-INF/xposed/module.prop`
- `META-INF/xposed/scope.list`
- `minApiVersion=102`
- `targetApiVersion=102`
- `staticScope=false`

The Legacy `assets/xposed_init` entry and Xposed metadata in `AndroidManifest.xml` have been removed. `ModernXposedMod` currently verifies API 102 lifecycle loading only. It deliberately does not call the old `XposedMod`, because modules targeting API 102 are forbidden from calling `de.robv.android.xposed` APIs.

Legacy API remains a compile-only dependency temporarily so unmigrated source files can stay in the tree while they are ported. It must be removed before the migration is considered complete.

**Migration builds are not functionally equivalent to the frozen Legacy release yet.**

## Migration gates

1. Replace `XLog` and the central `XPosedX.kt` hook DSL with API 102 interceptors.
2. Replace direct `XposedBridge`, `XposedHelpers`, `XC_MethodHook`, and `AndroidAppHelper` usage.
3. Port general package dispatch for `system_server`, SystemUI, XMSF, and selected client apps.
4. Restore Android 16 `SystemProperties` hooks and `Build.*` Unsafe fallback.
5. Restore XMSF notification bridge, permissions, icon handling, and notification delivery.
6. Remove `de.robv.android.xposed:api:82`, Legacy entry classes, and Legacy keep rules.
7. Validate independently in `system_server`, SystemUI, XMSF, QQ, and QQ:MSF.
8. Verify fresh QQ MiPush registration and genuine server-side vendor-push delivery.
9. Only after parity, restore package name `com.neko7ina.mipush` and publish a separate Modern release.

Pixel-specific SystemUI behavior is intentionally out of scope for this general migration branch.
