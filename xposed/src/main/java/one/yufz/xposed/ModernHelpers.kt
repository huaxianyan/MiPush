package one.yufz.xposed

import java.lang.reflect.Constructor
import java.lang.reflect.Method

/** Small reflection-only replacement for the XposedHelpers calls still used by business code. */
object ModernHelpers {
    fun findClass(className: String, classLoader: ClassLoader?): Class<*> =
        Class.forName(className, false, classLoader)

    fun findMethodExact(clazz: Class<*>, name: String, vararg parameterTypes: Class<*>): Method =
        clazz.findMethodExact(name, *parameterTypes)

    fun findConstructorExact(
        className: String,
        classLoader: ClassLoader?,
        vararg parameterTypes: Class<*>
    ): Constructor<*> = Class.forName(className, false, classLoader)
        .getDeclaredConstructor(*parameterTypes)
        .apply { isAccessible = true }

    fun findMethodsByExactParameters(
        clazz: Class<*>,
        returnType: Class<*>,
        vararg parameterTypes: Class<*>
    ): Array<Method> = clazz.declaredMethods.filter { method ->
        method.returnType == returnType && method.parameterTypes.contentEquals(parameterTypes)
    }.onEach { it.isAccessible = true }.toTypedArray()
}
