package one.yufz.hmspush.hook.systemui

import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XposedHelpers
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClass
import one.yufz.xposed.hookAllMethods
import one.yufz.xposed.hookMethod
import java.util.concurrent.atomic.AtomicBoolean

/** Pixel Android 16-specific notification row icon adaptation. */
class HookPixelSystemUI {
    companion object {
        private const val TAG = "HookPixelSystemUI"
        private const val QQ_PACKAGE_NAME = "com.tencent.mobileqq"
        private const val QQ_MIPUSH_NOTIFICATION_TAG = "mipush_com.tencent.mobileqq"
        private const val ICON_TYPE_SMALL_ICON = 0
        private const val REDESIGN_AVATAR_SIZE_DP = 40
        private const val PIXEL_5_AVATAR_SIZE_DP = 48
        private const val BADGE_SIZE_DP = 20
        private const val BADGE_PROTRUSION_DP = 2
        private const val BADGE_ICON_PADDING_DP = 4
    }

    private val loggedQqBadgeOverride = AtomicBoolean(false)
    private val loggedQqGeometryAdjustment = AtomicBoolean(false)

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

            val rowIconClass = classLoader.findClass(
                "com.android.internal.widget.NotificationRowIconView"
            )
            listOf("setImageIcon", "setImageIconAsync").forEach { methodName ->
                rowIconClass.hookAllMethods(methodName) {
                    doBefore {
                        val iconView = thisObject as? View ?: return@doBefore
                        val provider = try {
                            XposedHelpers.getObjectField(thisObject, "mIconProvider")
                        } catch (_: Throwable) {
                            null
                        } ?: return@doBefore
                        val sbn = findStatusBarNotification(provider)
                        if (sbn?.packageName == QQ_PACKAGE_NAME &&
                            sbn.tag == QQ_MIPUSH_NOTIFICATION_TAG
                        ) {
                            adjustConversationIconGeometry(iconView)
                        }
                    }
                }
            }
            XLog.d(TAG, "Installed Pixel Android 16 QQ conversation icon geometry hooks")
        } catch (t: Throwable) {
            // This class is Pixel/SystemUI-build-specific. Never destabilize SystemUI on mismatch.
            XLog.e(TAG, "Unable to install Pixel Android 16 notification icon hook", t)
        }
    }

    private fun adjustConversationIconGeometry(iconView: View) {
        val badge = iconView.parent as? View ?: return
        val iconGroup = badge.parent as? ViewGroup ?: return
        val resources = iconView.resources
        val avatarId = resources.getIdentifier("conversation_icon", "id", "android")
        val facePileId = resources.getIdentifier("conversation_face_pile", "id", "android")
        val avatarSize = dp(iconView, PIXEL_5_AVATAR_SIZE_DP)
        val redesignAvatarSize = dp(iconView, REDESIGN_AVATAR_SIZE_DP)
        val badgeSize = dp(iconView, BADGE_SIZE_DP)
        val badgePosition = avatarSize - badgeSize + dp(iconView, BADGE_PROTRUSION_DP)
        val badgeIconPadding = dp(iconView, BADGE_ICON_PADDING_DP)

        // Apply this only to Android 16's 2025 conversation layout. Its avatar is 40dp; the
        // legacy Pixel 5 layout already uses 48dp and must not be changed again.
        val avatars = (0 until iconGroup.childCount)
            .map(iconGroup::getChildAt)
            .filter { it.id == avatarId || it.id == facePileId }
        if (avatars.none { it.layoutParams?.width == redesignAvatarSize }) return

        avatars.forEach { avatar ->
            avatar.layoutParams = avatar.layoutParams.apply {
                width = avatarSize
                height = avatarSize
            }
        }

        (badge.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.width = badgeSize
            params.height = badgeSize
            params.topMargin = badgePosition
            params.marginStart = badgePosition
            badge.layoutParams = params
        }
        (iconView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.setMargins(
                badgeIconPadding,
                badgeIconPadding,
                badgeIconPadding,
                badgeIconPadding
            )
            iconView.layoutParams = params
        }

        if (loggedQqGeometryAdjustment.compareAndSet(false, true)) {
            XLog.d(
                TAG,
                "Adjusted QQ conversation avatar to 48dp and badge icon padding to 4dp"
            )
        }
    }

    private fun dp(view: View, value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            view.resources.displayMetrics
        ).toInt()

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
