package one.yufz.hmspush.hook.fakedevice

import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.hookMethod

object FakeDevice {
    private const val TAG = "FakeDevice"

    private val Default = arrayOf(Common::class.java)
    private val FakeDeviceConfig: Map<String, Array<Class<out IFakeDevice>>> = mapOf(
        "com.coolapk.market" to arrayOf(CoolApk::class.java),
        "com.tencent.mobileqq" to arrayOf(QQ::class.java),
        "com.tencent.tim" to arrayOf(QQ::class.java),
        "com.sankuai.meituan" to arrayOf(FakeEmuiOnly::class.java),
        "com.sankuai.meituan.takeoutnew" to arrayOf(FakeEmuiOnly::class.java),
        "com.dianping.v1" to arrayOf(FakeEmuiOnly::class.java),
        "com.eg.android.AlipayGphone" to arrayOf(Alipay::class.java),
        "com.xunmeng.pinduoduo" to arrayOf(PinDuoDuo::class.java),
        "com.ss.android.ugc.aweme" to arrayOf(DouYin::class.java),
    )

    fun fake(loadedPackage: LoadedPackage) {
        XLog.d(
            TAG,
            "fake() called with: packageName = ${loadedPackage.packageName}, " +
                "processName = ${loadedPackage.processName}"
        )
        if (loadedPackage.packageName == "com.google.android.webview") {
            XLog.d(TAG, "fake() called, ignore ${loadedPackage.packageName}")
            return
        }

        val fakes = FakeDeviceConfig[loadedPackage.packageName] ?: Default
        fakes.forEach { it.newInstance().fake(loadedPackage) }
    }
}