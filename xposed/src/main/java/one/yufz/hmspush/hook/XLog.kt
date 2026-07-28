package one.yufz.hmspush.hook

import android.util.Log
import one.yufz.hmspush.hook.modern.ModernRuntime
import one.yufz.hmspush.xposed.BuildConfig
import one.yufz.xposed.HookParam
import java.lang.reflect.Method

object XLog {
    fun t(tag: String, message: String?) {
        if (BuildConfig.DEBUG) {
            ModernRuntime.log(Log.VERBOSE, tag, "[MiPush][T][$tag] $message")
        }
    }

    fun d(tag: String, message: String?) {
        ModernRuntime.log(Log.DEBUG, tag, "[MiPush][D][$tag] $message")
    }

    fun i(tag: String, message: String?) {
        ModernRuntime.log(Log.INFO, tag, "[MiPush][I][$tag] $message")
    }

    fun e(tag: String, message: String?, throwable: Throwable?) {
        ModernRuntime.log(Log.ERROR, tag, "[MiPush][E][$tag] $message", throwable)
    }

    fun HookParam.logMethod(tag: String, stackTrace: Boolean = false) {
        d(tag, "╔═══════════════════════════════════════════════════════")
        d(tag, executable.toString())
        d(tag, "${executable.name} called with ${args.contentDeepToString()}")
        if (stackTrace) {
            d(tag, Log.getStackTraceString(Throwable()))
        }
        if (throwable != null) {
            e(tag, "${executable.name} thrown", throwable)
        } else if (executable is Method && executable.returnType != Void.TYPE) {
            d(tag, "${executable.name} return $result")
        }
        d(tag, "╚═══════════════════════════════════════════════════════")
    }
}
