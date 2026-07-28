package one.yufz.hmspush.hook.systemui

import android.os.Build
import android.service.notification.StatusBarNotification
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClass
import one.yufz.xposed.hookMethod
import java.util.concurrent.atomic.AtomicBoolean

/** Pixel Android 16-specific notification row icon adaptation. */
class HookPixelSystemUI {
    companion object {
        private const val TAG = "HookPixelSystemUI"
        private const val QQ_PACKAGE_NAME = "com.tencent.mobileqq"
        private const val QQ_MIPUSH_NOTIFICATION_TAG = "mipush_com.tencent.mobileqq"
        private const val ICON_TYPE_SMALL_ICON = 0
    }

    private val loggedQqBadgeOverride = AtomicBoolean(false)

    fun hook(classLoader: ClassLoader) {
        if (Build.VERSION.SDK_INT < 36) return

        try {
            val providerClass = classLoader.findClass(
                "com.android.systemui.statusbar.notification.row.icon." +
                    "NotificationRowIconViewInflaterFactory\$createIconProvider\$2"
            )
            providerClass.hookMethod("getIconType") {
                doBefore {
                    val sbn = findStatusBarNotification(thisObject)
                    if (sbn?.packageName == QQ_PACKAGE_NAME &&
                        sbn.tag == QQ_MIPUSH_NOTIFICATION_TAG
                    ) {
                        result = ICON_TYPE_SMALL_ICON
                        if (loggedQqBadgeOverride.compareAndSet(false, true)) {
                            XLog.d(
                                TAG,
                                "Using QQ small icon via Pixel NotificationIconProvider.getIconType"
                            )
                        }
                    }
                }
            }
            XLog.d(TAG, "Installed Pixel Android 16 NotificationIconProvider.getIconType hook")
        } catch (t: Throwable) {
            // This class is Pixel/SystemUI-build-specific. Never destabilize SystemUI on mismatch.
            XLog.e(TAG, "Unable to install Pixel Android 16 notification icon hook", t)
        }
    }

    private fun findStatusBarNotification(provider: Any): StatusBarNotification? {
        return provider.javaClass.declaredFields.firstNotNullOfOrNull { field ->
            if (!StatusBarNotification::class.java.isAssignableFrom(field.type)) {
                null
            } else {
                try {
                    field.isAccessible = true
                    field.get(provider) as? StatusBarNotification
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }
}
