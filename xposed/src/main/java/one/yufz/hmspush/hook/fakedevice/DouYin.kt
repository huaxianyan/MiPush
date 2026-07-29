package one.yufz.hmspush.hook.fakedevice

import android.os.Build
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClass
import one.yufz.xposed.hookMethod
import org.json.JSONArray
import org.json.JSONObject


class DouYin : Common() {
    companion object {
        private const val TAG = "DouYin"
    }

    override fun fake(loadedPackage: LoadedPackage): Boolean {
        super.fake(loadedPackage)
        if (Build.DISPLAY.contains("flyme", true) || Build.USER.contains("flyme", true)) {
            fakeProperty("ro.build.display.id" to "")
            fakeProperty("ro.build.user" to "")
            fakeProperty("ro.build.flyme.version" to "")
            fakeProperty("ro.flyme.version.id" to "")
        }

        //public java.lang.String com.bytedance.common.network.DefaultNetWorkClient.post(java.lang.String,java.util.List,java.util.Map,com.bytedance.common.utility.NetworkClient$ReqContext)
        val classAppLogNetworkClient = loadedPackage.classLoader.findClass("com.ss.android.ugc.aweme.statistic.AppLogNetworkClient")
        val classReqContext = loadedPackage.classLoader.findClass("com.bytedance.common.utility.NetworkClient\$ReqContext")
        classAppLogNetworkClient.hookMethod("post", String::class.java, List::class.java, Map::class.java, classReqContext) {
            doAfter {
                val url = args[0] as String
                if (!url.contains("/cloudpush/update_sender/")) return@doAfter

                XLog.d(TAG, result.toString())

                val json = result as String
                val obj = JSONObject(json)
                val allowPushList = obj.getJSONArray("allow_push_list")
                val newArray = tryInsertMiPushChannel(allowPushList)
                obj.put("allow_push_list", newArray)

                result = obj.toString()

                XLog.d(TAG, result.toString())
            }
        }
        return true
    }

    private fun tryInsertMiPushChannel(originArray: JSONArray): JSONArray {
        val array = ArrayList<Int>()
        for (i in 0 until originArray.length()) {
            array.add(originArray.getInt(i))
        }
        array.remove(1)
        array.add(0, 1)
        return JSONArray(array)
    }
}