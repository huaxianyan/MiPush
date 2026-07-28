package one.yufz.hmspush.hook.modern

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
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

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        ModernRuntime.attach(this)
        processName = param.processName
        log(
            Log.INFO,
            TAG,
            "Modern module loaded: process=${param.processName}, " +
                "systemServer=${param.isSystemServer}, framework=$frameworkName " +
                "$frameworkVersion, api=$apiVersion"
        )
    }

    override fun onPackageReady(param: PackageReadyParam) {
        log(
            Log.DEBUG,
            TAG,
            "Package ready for API 102 migration: package=${param.packageName}, " +
                "process=$processName"
        )
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        log(
            Log.DEBUG,
            TAG,
            "System server ready for API 102 migration: classLoader=${param.classLoader}"
        )
    }
}
