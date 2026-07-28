package one.yufz.hmspush.hook.systemui

import android.app.AndroidAppHelper
import android.app.Notification
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.StatusBarNotification
import android.view.View
import android.widget.RemoteViews
import de.robv.android.xposed.XposedHelpers
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.callMethod
import one.yufz.xposed.findClass
import one.yufz.xposed.get
import one.yufz.xposed.hook
import one.yufz.xposed.hookAllMethods
import one.yufz.xposed.hookMethod
import java.util.concurrent.atomic.AtomicBoolean

class HookSystemUI {
    companion object {
        private const val TAG = "HookSystemUI"
    }

    private val loggedQqConversationBadgeOverride = AtomicBoolean(false)

    private val ID_ICON_IS_PRE_L: Int by lazy {
        val app = AndroidAppHelper.currentApplication()
        app.resources.getIdentifier("icon_is_pre_L", "id", app.packageName)
    }

    fun hook(classLoader: ClassLoader) {
        hookNotificationRowAppIcon(classLoader)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            classLoader.findClass("com.android.systemui.statusbar.notification.icon.IconManager")
                .hookAllMethods("setIcon") {
                    doAfter {
                        val iconView = args[2] as View
                        iconView.setTag(ID_ICON_IS_PRE_L, true)
                    }
                }
        } else {
            classLoader.findClass("com.android.systemui.statusbar.notification.collection.NotificationEntry")
                .hookMethod("setIconTag", Int::class.java, Any::class.java) {
                    doBefore {
                        if (args[0] == ID_ICON_IS_PRE_L) {
                            args[1] = true
                        }
                    }
                }
        }

        Notification.Builder::class.java.hookAllMethods("processSmallIconColor") {
            doBefore {
                val context: Context = thisObject["mContext"]
                val smallIcon = args[0] as Icon
                val contentView = args[1] as RemoteViews
                val p = args[2]

                val isGrayscaleIcon = thisObject.callMethod("getColorUtil")!!
                    .callMethod("isGrayscaleIcon", context, smallIcon) as Boolean

                if (!isGrayscaleIcon) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        contentView.setInt(android.R.id.icon, "setBackgroundColor", thisObject.callMethod("getBackgroundColor", p) as Int)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        contentView.setInt(android.R.id.icon, "setOriginalIconColor", 1)
                    }
                    result = true
                }
            }
        }
    }

    /**
     * Android 16's notification redesign normally replaces the small icon in a notification row
     * with the application's launcher icon. In MessagingStyle notifications that launcher icon is
     * also drawn as the badge over the conversation avatar. Keep MiPush-proxied QQ notifications
     * on the small-icon path so the bundled (or user-selected) monochrome icon is used for that
     * badge, matching the pre-redesign Android behavior.
     */
    private fun hookNotificationRowAppIcon(classLoader: ClassLoader) {
        if (Build.VERSION.SDK_INT < 36) return

        // Hook the policy method first. This is the normal Android 16 AOSP path.
        try {
            classLoader.findClass(
                "com.android.systemui.statusbar.notification.row.icon.NotificationIconStyleProviderImpl"
            ).hookMethod(
                "shouldShowAppIcon",
                StatusBarNotification::class.java,
                Context::class.java
            ) {
                doBefore {
                    val sbn = args[0] as StatusBarNotification
                    if (isMiPushQqNotification(sbn)) {
                        logQqConversationBadgeOverride("NotificationIconStyleProviderImpl")
                        result = false
                    }
                }
            }
            XLog.d(TAG, "Installed Android 16 QQ notification icon policy hook")
        } catch (t: Throwable) {
            XLog.e(TAG, "Unable to install Android 16 QQ notification icon policy hook", t)
        }

        // Pixel Android 16 creates a per-row provider object and NotificationRowIconView calls it
        // directly. Hook that final decision as well, rather than relying only on the policy method
        // (which can be cached or bypassed by vendor SystemUI changes).
        try {
            val providerClass = classLoader.findClass(
                "com.android.systemui.statusbar.notification.row.icon." +
                    "NotificationRowIconViewInflaterFactory\$createIconProvider\$2"
            )
            providerClass.hookMethod("shouldShowAppIcon") {
                doBefore {
                    val sbn = findStatusBarNotification(thisObject)
                    if (sbn != null && isMiPushQqNotification(sbn)) {
                        logQqConversationBadgeOverride("NotificationRowIconView provider")
                        result = false
                    }
                }
            }
            providerClass.hookMethod("getAppIcon") {
                doBefore {
                    val sbn = findStatusBarNotification(thisObject)
                    if (sbn != null && isMiPushQqNotification(sbn)) {
                        // Defensive fallback: a null app icon also makes the row use smallIcon.
                        logQqConversationBadgeOverride("NotificationRowIconView app icon loader")
                        result = null
                    }
                }
            }
            XLog.d(TAG, "Installed Android 16 QQ per-row app icon hooks")
        } catch (t: Throwable) {
            // Keep SystemUI usable on builds whose Kotlin-generated provider class differs.
            XLog.e(TAG, "Unable to install Android 16 QQ per-row app icon hooks", t)
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

    private fun isMiPushQqNotification(sbn: StatusBarNotification): Boolean {
        return sbn.packageName == "com.tencent.mobileqq" &&
            sbn.tag == "mipush_com.tencent.mobileqq"
    }

    private fun logQqConversationBadgeOverride(path: String) {
        if (loggedQqConversationBadgeOverride.compareAndSet(false, true)) {
            XLog.d(TAG, "Using QQ notification small icon for conversation badge via $path")
        }
    }

}
