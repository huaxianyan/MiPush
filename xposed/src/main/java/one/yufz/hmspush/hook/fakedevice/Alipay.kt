package one.yufz.hmspush.hook.fakedevice

import one.yufz.xposed.findClass
import one.yufz.xposed.hook

class Alipay : IFakeDevice {
    override fun fake(loadedPackage: LoadedPackage): Boolean {
        loadedPackage.classLoader.findClass("com.alipay.pushsdk.thirdparty.xiaomi.XiaoMIPushWorker")
            .declaredMethods
            .find { it.returnType == Boolean::class.java }
            ?.hook { replace { true } }

        return true
    }
}