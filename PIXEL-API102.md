# Pixel Android 16 · Modern Xposed API 102

This branch (`modern-api-102-pixel-android16`) is the Pixel-specific Modern API 102 edition.

## Identity and update path

- Package: `com.neko7ina.mipush.pixel`
- Modern API dependency: `io.github.libxposed:api:102.0.0`
- Frozen predecessor: `pixel-android-16-notification-badge` at `110f396`

A release signed with the repository's long-term certificate can update the corrected Pixel Legacy package in place.

## Recommended scope

- `system`
- `com.android.systemui`
- `com.xiaomi.xmsf`
- Selected target applications that need Xiaomi property spoofing

Do not enable another MiPush module edition in the same LSPosed scopes.

## Pixel-only SystemUI behavior

The hook activates only on Android 16 or newer and targets Pixel's concrete notification icon provider:

```text
com.android.systemui.statusbar.notification.row.icon.
NotificationRowIconViewInflaterFactory$createIconProvider$2.getIconType()
```

It is further restricted to:

```text
packageName=com.tencent.mobileqq
tag=mipush_com.tencent.mobileqq
```

For matching QQ MiPush conversation notifications it:

1. Selects `ICON_TYPE_SMALL_ICON` for the lower-right badge without rebuilding the notification.
2. Preserves `android.largeIcon`, `Person.icon`, contact avatars, and group avatars.
3. Changes only Android 16's 40dp conversation template to the tested Pixel 5-like geometry:
   - avatar: 48dp
   - badge: 20dp
   - badge position: 30dp
   - badge icon padding: 4dp
4. Fails safely when Pixel's implementation classes or fields do not match.

No fallback QQ icon is bundled or enabled. MiPushFramework remains the sole source of the notification `smallIcon`.

## Validation gates

- [x] Port the Pixel provider and geometry hooks from Legacy callbacks to API 102 interceptors.
- [x] Remove Legacy Xposed API references and entry declarations.
- [x] Keep the Pixel package identity and recommended SystemUI scope.
- [x] Build and inspect the signed Pixel API 102 APK.
- [x] Install with all other MiPush editions disabled in LSPosed.
- [x] Reboot and confirm the Pixel provider and geometry hooks install without errors.
- [x] Verify a QQ MiPush conversation notification retains its avatar and uses the custom monochrome `smallIcon` badge.
- [x] Verify the 48dp avatar and 4dp badge padding visually.
- [x] Verify genuine server-side delivery while QQ:MSF remains absent.

## Device validation

Validated on Pixel 10 Pro, Android 16 / API 36, with LSPosed 2.1.0 / API 102. The historical-notification test exercised both Pixel paths and logged:

```text
Adjusted QQ conversation avatar to 48dp and badge icon padding to 4dp
Using QQ small icon via Pixel NotificationIconProvider.getIconType
```

A separate live test received `Api102 pixel 测试` from the Xiaomi server while Thanox kept `com.tencent.mobileqq:MSF` absent. XMSF created the QQ conversation channel and published the notification as `com.tencent.mobileqq` with `opPkg=android`; the 68x68 small icon, 100x100 large icon, valid conversation shortcut, and `MessagingStyle` payload remained intact.
