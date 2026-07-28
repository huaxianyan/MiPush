/*
 * Device-property spoofing for MiPush.
 *
 * The native SystemProperties hooks and the corresponding MIUI property set are
 * adapted from yin-ol/MiPushFaker, licensed under GNU AGPL v3.0. See NOTICE and
 * LICENSE at the repository root.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package one.yufz.hmspush.hook.fakedevice

import android.os.Build
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.HookContext
import one.yufz.xposed.findClass
import one.yufz.xposed.hookMethod
import one.yufz.xposed.set
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "FakeProperties"

enum class Property(val entry: Pair<String, String>) {
    EMUI_API("ro.build.hw_emui_api_level" to ""),
    EMUI_VERSION("ro.build.version.emui" to ""),
    BRAND("ro.product.brand" to "Xiaomi"),
    VENDOR_BRAND("ro.product.vendor.brand" to "Xiaomi"),
    MANUFACTURER("ro.product.manufacturer" to "Xiaomi"),
    PRODUCT_MANUFACTURER("product.manufacturer" to "Xiaomi"),
    VENDOR_MANUFACTURER("ro.product.vendor.manufacturer" to "Xiaomi"),
    MIUI_VERSION_NAME("ro.miui.ui.version.name" to "V12"),
    MIUI_VERSION_CODE("ro.miui.ui.version.code" to "10"),
    MIUI_VERSION_CODE_TIME("ro.miui.version.code_time" to "1592409600"),
    FLYME_VERSION_NAME("ro.build.flyme.version" to ""),
    FLYME_VERSION_CODE("ro.flyme.version.id" to ""),
    COLOROS_BUILD_VERSION_OLD("ro.build.version.opporom" to ""),
    COLOROS_BUILD_VERSION("ro.build.version.oplusrom" to ""),

    REGION_MIUI("ro.miui.region" to "CN"),
    REGION_PRODUCT_LOCALE("ro.product.locale.region" to "CN"),
    REGION_PRODUCT_COUNTRY("ro.product.country.region" to "CN"),
    REGION_PERSIST_COUNTRY("persist.sys.country" to "CN"),
    ;

    val key: String
        get() = entry.first

    val value: String
        get() = entry.second
}

fun fakeProperty(property: Property, overrideValue: String) =
    fakeProperty(Pair(property.key, overrideValue))

fun fakeAllBuildInProperties() =
    fakeProperty(*Property.values().map { it.entry }.toTypedArray())

fun fakeProperty(vararg properties: Property) {
    fakeProperty(*properties.map { it.entry }.toTypedArray())
}

private val propertyMap: MutableMap<String, String> = HashMap()
private val hooked = AtomicBoolean(false)
private val loggedSpoofs = ConcurrentHashMap.newKeySet<String>()

fun fakeProperty(vararg properties: Pair<String, String>) {
    propertyMap.putAll(properties)

    // Install the hooks first. Android 16 rejects reflective writes to Build's
    // static final fields; those best-effort writes must not prevent the actual
    // SystemProperties spoofing from being installed.
    installPropertyHooksIfNeeded()
    fakeBuildFieldsBestEffort()
}

private fun installPropertyHooksIfNeeded() {
    if (!hooked.compareAndSet(false, true)) return

    val systemProperties = Build::class.java.classLoader.findClass("android.os.SystemProperties")

    val stringPropertyCallback: HookContext.() -> Unit = {
        doBefore {
            val key = args[0] as String
            propertyMap[key]?.let {
                logSpoofOnce("get", key, it)
                result = it
            }
        }
    }

    systemProperties.hookMethod("get", String::class.java, callback = stringPropertyCallback)
    systemProperties.hookMethod(
        "get",
        String::class.java,
        String::class.java,
        callback = stringPropertyCallback
    )

    // Some MiPush SDK versions call hidden native accessors directly. Hook
    // those accessors as MiPushFaker does, while handling signature differences
    // between Android releases without aborting the remaining spoofing setup.
    hookOptional("native_get", String::class.java) {
        val key = args[0] as String
        propertyMap[key]?.let {
            logSpoofOnce("native_get", key, it)
            result = it
        }
    }
    hookOptional("native_get", String::class.java, String::class.java) {
        val key = args[0] as String
        propertyMap[key]?.let {
            logSpoofOnce("native_get_with_default", key, it)
            result = it
        }
    }
    hookOptional("native_get_int", String::class.java, Int::class.javaPrimitiveType!!) {
        val key = args[0] as String
        propertyMap[key]?.toIntOrNull()?.let {
            logSpoofOnce("native_get_int", key, it.toString())
            result = it
        }
    }
    hookOptional("native_get_long", String::class.java, Long::class.javaPrimitiveType!!) {
        val key = args[0] as String
        propertyMap[key]?.toLongOrNull()?.let {
            logSpoofOnce("native_get_long", key, it.toString())
            result = it
        }
    }

    Runtime::class.java.hookMethod("exec", String::class.java) {
        doBefore {
            val cmd = args[0] as String
            if (cmd.startsWith("getprop")) {
                val key = cmd.removePrefix("getprop").trim()
                propertyMap[key]?.let {
                    XLog.d(TAG, "hook getprop $key")
                    args[0] = "echo $it"
                }
            }
        }
    }
}

private fun hookOptional(
    methodName: String,
    vararg parameterTypes: Class<*>,
    afterHook: de.robv.android.xposed.XC_MethodHook.MethodHookParam.() -> Unit
) {
    try {
        val systemProperties = Build::class.java.classLoader.findClass("android.os.SystemProperties")
        systemProperties.hookMethod(methodName, *parameterTypes) {
            doAfter(afterHook)
        }
    } catch (t: Throwable) {
        XLog.d(
            TAG,
            "SystemProperties.$methodName${parameterTypes.contentToString()} is unavailable: ${t.message}"
        )
    }
}

private fun logSpoofOnce(accessor: String, key: String, value: String) {
    if (loggedSpoofs.add("$accessor:$key")) {
        XLog.d(TAG, "Spoofed SystemProperties.$accessor($key)=$value")
    }
}

private fun fakeBuildFieldsBestEffort() {
    setBuildFieldBestEffort("BRAND", propertyMap[Property.BRAND.key])
    setBuildFieldBestEffort("MANUFACTURER", propertyMap[Property.MANUFACTURER.key])
    setBuildFieldBestEffort("MODEL", propertyMap["ro.product.model"])
    setBuildFieldBestEffort("DISPLAY", propertyMap["ro.build.display.id"])
    setBuildFieldBestEffort("USER", propertyMap["ro.build.user"])
}

private fun setBuildFieldBestEffort(fieldName: String, value: String?) {
    if (value == null) return

    try {
        Build::class.java[fieldName] = value
        return
    } catch (reflectionFailure: Throwable) {
        // Android 16 blocks Field.set() for Build's public static final fields.
        // These fields have no ConstantValue and are read with sget-object by
        // applications, so writing their ART static-field storage before app
        // startup restores the behavior older Android releases allowed.
        if (setStaticObjectFieldWithUnsafe(Build::class.java, fieldName, value)) {
            XLog.d(TAG, "Set Build.$fieldName with Unsafe after reflection was blocked")
            return
        }

        XLog.e(
            TAG,
            "Unable to set Build.$fieldName; SystemProperties hooks remain active",
            reflectionFailure
        )
    }
}

private fun setStaticObjectFieldWithUnsafe(
    declaringClass: Class<*>,
    fieldName: String,
    value: Any
): Boolean {
    val field = try {
        declaringClass.getDeclaredField(fieldName).apply { isAccessible = true }
    } catch (t: Throwable) {
        XLog.d(TAG, "Unable to resolve $declaringClass.$fieldName for Unsafe: ${t.message}")
        return false
    }

    for (unsafeClassName in arrayOf("sun.misc.Unsafe", "jdk.internal.misc.Unsafe")) {
        try {
            val unsafeClass = Class.forName(unsafeClassName)
            val unsafe = unsafeClass.getDeclaredField("theUnsafe").run {
                isAccessible = true
                get(null)
            }
            val base = unsafeClass.getMethod("staticFieldBase", Field::class.java)
                .invoke(unsafe, field)
            val offset = (unsafeClass.getMethod("staticFieldOffset", Field::class.java)
                .invoke(unsafe, field) as Number).toLong()

            val writer = arrayOf(
                "putObjectVolatile",
                "putObject",
                "putReferenceVolatile",
                "putReference"
            ).firstNotNullOfOrNull { methodName ->
                try {
                    unsafeClass.getMethod(
                        methodName,
                        Any::class.java,
                        Long::class.javaPrimitiveType!!,
                        Any::class.java
                    )
                } catch (_: Throwable) {
                    null
                }
            } ?: continue

            writer.invoke(unsafe, base, offset, value)
            if (field.get(null) == value) return true
        } catch (t: Throwable) {
            XLog.d(TAG, "$unsafeClassName cannot set Build.$fieldName: ${t.message}")
        }
    }

    return false
}
