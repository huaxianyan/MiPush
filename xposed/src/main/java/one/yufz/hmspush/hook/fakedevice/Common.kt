package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.hookMethod
import miui.external.SdkHelper

open class Common : IFakeDevice {
    companion object {
        private const val TAG = "Common"
    }

    override fun fake(loadedPackage: LoadedPackage): Boolean {
        XLog.d(TAG, "fake() called with: packageName = ${loadedPackage.packageName}")
        fakeAllBuildInProperties()
        fakeClass(loadedPackage)
        return true
    }

    private fun fakeClass(loadedPackage: LoadedPackage) {
        var isMIUI = false
        try {
            // check MIUI environment
            Class.forName("miui.os.Build", false, loadedPackage.classLoader)
            isMIUI = true
        } catch (_: Throwable) {
        }
        if (isMIUI) {
            return
        }

        val classMap: Map<String, Class<out Any>> = mapOf(
            "miui.os.Build" to Object::class.java,
            SdkHelper::class.java.name to SdkHelper::class.java,
        )
        Class::class.java.hookMethod(
            "forName",
            String::class.java,
            Boolean::class.java,
            ClassLoader::class.java
        ) {
            doBefore {
                var requestClass = args[0]
                val returnClass = classMap[requestClass]
                if (returnClass != null) {
                    XLog.d(TAG, "forHook $requestClass")
                    result = returnClass
                } else {
                    XLog.t(TAG, "forName $requestClass")
                }
            }
        }
    }
}