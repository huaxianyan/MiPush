package one.yufz.hmspush.hook.fakedevice

import android.app.Application
import one.yufz.xposed.hookMethod

class CoolApk : XGPush() {
    override fun fake(loadedPackage: LoadedPackage): Boolean {
        Application::class.java.hookMethod("onCreate") {
            doAfter { super.fake(loadedPackage) }
        }
        return true
    }
}