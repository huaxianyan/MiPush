package one.yufz.hmspush.hook.modern

import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Executable

/** Process-local access to the framework interface attached to the Modern module entry. */
object ModernRuntime {
    @Volatile
    private var module: XposedModule? = null

    fun attach(module: XposedModule) {
        this.module = module
    }

    fun hook(executable: Executable, interceptor: (XposedInterface.Chain) -> Any?): XposedInterface.HookHandle {
        val api = requireModule()
        return api.hook(executable)
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept { chain -> interceptor(chain) }
    }

    fun log(priority: Int, tag: String, message: String?, throwable: Throwable? = null) {
        val text = message ?: "null"
        val api = module
        if (api != null) {
            api.log(priority, tag, text, throwable)
        } else {
            // This fallback is useful for migration diagnostics before the framework attaches.
            Log.println(priority, tag, if (throwable == null) text else "$text\n${Log.getStackTraceString(throwable)}")
        }
    }

    private fun requireModule(): XposedModule =
        checkNotNull(module) { "Modern Xposed framework is not attached in this process" }
}
