package one.yufz.hmspush.hook.hms.nm.handler

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import one.yufz.hmspush.hook.hms.Prefs
import one.yufz.hmspush.hook.hms.icon.IconManager
import one.yufz.hmspush.hook.hms.nm.INotificationManager
import one.yufz.hmspush.hook.util.newBuilder

class IconHandler : NotificationHandler {
    companion object {
        private const val QQ_PACKAGE_NAME = "com.tencent.mobileqq"
        private const val EXTRA_PREFER_SMALL_ICON = "android.app.preferSmallIcon"
    }

    override fun careAbout(manager: INotificationManager, context: Context, packageName: String, id: Int, notification: Notification): Boolean {
        // QQ does not consistently provide an Android-compliant monochrome
        // status-bar icon. Apply the bundled fallback automatically while
        // retaining the existing opt-in behavior for every other app.
        return Prefs.prefModel.useCustomIcon ||
            IconManager.hasBuiltInNotificationIcon(packageName)
    }

    override fun handle(chain: NotificationHandler.Chain, manager: INotificationManager, context: Context, packageName: String, id: Int, notification: Notification) {
        var newNotification = notification
        val iconData = IconManager.getNotificationIconData(
            context,
            packageName,
            Prefs.prefModel.useCustomIcon
        )
        if (iconData != null) {
            val builder = notification.newBuilder(context)
                .setSmallIcon(Icon.createWithBitmap(iconData.iconBitmap))

            if (Prefs.prefModel.tintIconColor) {
                iconData.iconColor?.let {
                    builder.setColor(it)
                }
            }

            newNotification = builder.build().apply {
                if (packageName == QQ_PACKAGE_NAME) {
                    // Android 16's notification redesign otherwise replaces the small icon in the
                    // conversation-avatar badge with QQ's full-color launcher icon. This framework
                    // extra is the platform-supported opt-out consumed by SystemUI's
                    // NotificationIconStyleProviderImpl.
                    extras.putBoolean(EXTRA_PREFER_SMALL_ICON, true)
                }
            }
        }
        super.handle(chain, manager, context, packageName, id, newNotification)
    }
}