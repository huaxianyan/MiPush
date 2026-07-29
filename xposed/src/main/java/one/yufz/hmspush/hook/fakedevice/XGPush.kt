package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClass
import one.yufz.xposed.hook
import java.lang.reflect.Method

open class XGPush : IFakeDevice {
    companion object {
        private const val TAG = "FakeForXGPush"
    }

    override fun fake(loadedPackage: LoadedPackage): Boolean {
        val classLoader = loadedPackage.classLoader
        XLog.d(TAG, "fake() called with: classLoader = $classLoader")

        return try {
            val classChannelUtils =
                classLoader.findClass("com.tencent.tpns.baseapi.base.util.ChannelUtils")
            fakeChannels(classChannelUtils)
            true
        } catch (e: ClassNotFoundException) {
            XLog.e(TAG, "fake ClassNotFoundException", e)
            false
        } catch (e: Throwable) {
            XLog.e(TAG, "fake error: ", e)
            false
        }
    }

    private fun fakeChannels(classChannelUtils: Class<*>): Boolean {
        XLog.d(TAG, "fakeChannels() called")
        classChannelUtils.declaredMethods.forEach { hookedMethod ->
            hookedMethod.hook {
                doBefore {
                    val method = executable as Method
                    result = when {
                        method.name == "getMiuiVersionCode" -> "13"
                        method.name == "getMiuiVersionName" -> "V130"
                        method.name == "isBrandXiaoMi" -> true
                        method.returnType == Boolean::class.java -> false
                        method.returnType == String::class.java -> ""
                        else -> return@doBefore
                    }
                }
            }
        }
        return true
    }
}
