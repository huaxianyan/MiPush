package one.yufz.hmspush.hook.hms

import one.yufz.xposed.*

class HookHMS {
    companion object {
        private const val TAG = "HookHMS"
    }

    fun hook(classLoader: ClassLoader) {
        if (HookPushNC.canHook(classLoader)) {
            HookPushNC.hook(classLoader)
        }
    }

}
