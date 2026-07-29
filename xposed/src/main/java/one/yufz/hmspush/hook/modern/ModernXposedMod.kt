package one.yufz.hmspush.hook.modern

import android.app.Application
import android.content.Context
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import one.yufz.hmspush.common.HMS_CORE_PROCESS
import one.yufz.hmspush.common.HMS_PACKAGE_NAME
import one.yufz.hmspush.hook.fakedevice.FakeDevice
import one.yufz.hmspush.hook.fakedevice.LoadedPackage
import one.yufz.hmspush.hook.hms.HookHMS
import one.yufz.hmspush.hook.system.HookSystemService
import one.yufz.hmspush.hook.systemui.HookPixelSystemUI
import one.yufz.xposed.hookMethod
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

/**
 * Modern Xposed API 102 entry point.
 *
 * Hook dispatch is intentionally not delegated to the legacy entry: API 102 modules are forbidden
 * from calling de.robv.android.xposed APIs. Features are enabled here only after their hook paths
 * have been migrated to the modern interceptor API and validated independently.
 */
class ModernXposedMod : XposedModule() {
    companion object {
        private const val TAG = "MiPushModern"
    }

    private var processName: String = "unknown"
    private var systemServer: Boolean = false

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        ModernRuntime.attach(this)
        processName = param.processName
        systemServer = param.isSystemServer
        log(
            Log.INFO,
            TAG,
            "Modern module loaded: process=${param.processName}, " +
                "systemServer=${param.isSystemServer}, framework=$frameworkName " +
                "$frameworkVersion, api=$apiVersion"
        )
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (shouldInstallGeneralPropertySpoof(param.packageName)) {
            try {
                FakeDevice.fake(
                    LoadedPackage(param.packageName, processName, param.defaultClassLoader)
                )
                log(
                    Log.INFO,
                    TAG,
                    "Installed Modern API 102 property spoof: package=${param.packageName}, " +
                        "process=$processName"
                )
            } catch (t: Throwable) {
                log(
                    Log.ERROR,
                    TAG,
                    "Unable to install Modern API 102 property spoof for ${param.packageName}",
                    t
                )
            }
        }
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName == "com.android.systemui") {
            try {
                HookPixelSystemUI().hook(param.classLoader)
                log(Log.INFO, TAG, "Installed Modern API 102 Pixel SystemUI hooks")
            } catch (t: Throwable) {
                log(Log.ERROR, TAG, "Unable to install Modern API 102 Pixel SystemUI hooks", t)
            }
        }
        if (param.packageName == HMS_PACKAGE_NAME && processName == HMS_CORE_PROCESS) {
            installApplicationContextCapture()
            try {
                HookHMS().hook(param.classLoader)
                log(Log.INFO, TAG, "Installed Modern API 102 XMSF notification bridge hooks")
            } catch (t: Throwable) {
                log(Log.ERROR, TAG, "Unable to install Modern API 102 XMSF hooks", t)
            }
        }
        log(
            Log.DEBUG,
            TAG,
            "Package ready for API 102 migration: package=${param.packageName}, " +
                "process=$processName"
        )
    }

    private fun shouldInstallGeneralPropertySpoof(packageName: String): Boolean {
        // Modern package callbacks can report additional APKs loaded inside system_server. Never
        // treat those package names as ordinary client processes or spoof the whole system process.
        if (systemServer) return false
        if (packageName == "android" ||
            packageName == "com.android.systemui" ||
            packageName == "com.google.android.webview" ||
            packageName == "com.xiaomi.xmsf"
        ) {
            return false
        }
        if (packageName == "com.tencent.mobileqq" || packageName == "com.tencent.tim") {
            return processName == packageName || processName.endsWith(":MSF")
        }
        return true
    }

    private fun installApplicationContextCapture() {
        Application::class.java.hookMethod("attach", Context::class.java) {
            doAfter {
                ProcessContext.attach(args[0] as Context)
                log(Log.DEBUG, TAG, "Captured XMSF application context")
            }
        }
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        try {
            HookSystemService().hook(param.classLoader)
            log(Log.INFO, TAG, "Installed Modern API 102 system_server hooks")
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "Unable to install Modern API 102 system_server hooks", t)
        }
    }
}
