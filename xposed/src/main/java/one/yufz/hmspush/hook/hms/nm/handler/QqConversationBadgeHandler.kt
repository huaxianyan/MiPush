package one.yufz.hmspush.hook.hms.nm.handler

import android.app.Notification
import android.content.Context
import one.yufz.hmspush.hook.hms.nm.INotificationManager

/**
 * Makes Android 16 use the notification's existing small icon for the badge over a QQ
 * conversation avatar. The small icon remains owned by the notification producer (including
 * MiPushFramework's custom icon configuration); this handler neither supplies nor replaces it.
 */
class QqConversationBadgeHandler : NotificationHandler {
    companion object {
        private const val QQ_PACKAGE_NAME = "com.tencent.mobileqq"
        private const val EXTRA_PREFER_SMALL_ICON = "android.app.preferSmallIcon"
    }

    override fun careAbout(
        manager: INotificationManager,
        context: Context,
        packageName: String,
        id: Int,
        notification: Notification
    ): Boolean {
        return packageName == QQ_PACKAGE_NAME &&
            notification.extras.getString(Notification.EXTRA_TEMPLATE) ==
            Notification.MessagingStyle::class.java.name
    }

    override fun handle(
        chain: NotificationHandler.Chain,
        manager: INotificationManager,
        context: Context,
        packageName: String,
        id: Int,
        notification: Notification
    ) {
        // Mutate only the extras bundle. Rebuilding the notification can discard Person/large-icon
        // data used for the contact or group avatar.
        notification.extras.putBoolean(EXTRA_PREFER_SMALL_ICON, true)
        super.handle(chain, manager, context, packageName, id, notification)
    }
}
