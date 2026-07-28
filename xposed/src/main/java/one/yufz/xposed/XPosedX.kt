package one.yufz.xposed

import io.github.libxposed.api.XposedInterface
import one.yufz.hmspush.hook.modern.ModernRuntime
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** Mutable compatibility view used by the project's hook DSL on Modern API 102. */
class HookParam internal constructor(
    val executable: Executable,
    var thisObject: Any?,
    val args: Array<Any?>
) {
    val method: Executable
        get() = executable

    var result: Any? = null
        set(value) {
            field = value
            throwable = null
            returnEarly = true
        }

    var throwable: Throwable? = null
        set(value) {
            field = value
            if (value != null) returnEarly = true
        }

    internal var returnEarly: Boolean = false

    internal fun setProceededResult(value: Any?) {
        result = value
        returnEarly = false
    }

    internal fun setProceededThrowable(value: Throwable) {
        throwable = value
        returnEarly = false
    }
}

typealias HookAction = HookParam.() -> Unit
typealias ReplaceAction = HookParam.() -> Any?
typealias HookCallback = HookContext.() -> Unit

fun Class<*>.hookMethod(
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: HookCallback
): XposedInterface.HookHandle =
    findMethodExact(this, methodName, parameterTypes).installHook(callback)

fun Class<*>.hookConstructor(
    vararg parameterTypes: Class<*>,
    callback: HookCallback
): XposedInterface.HookHandle =
    getDeclaredConstructor(*parameterTypes).apply { isAccessible = true }.installHook(callback)

fun Class<*>.hookAllConstructor(callback: HookCallback): Set<XposedInterface.HookHandle> =
    declaredConstructors.mapTo(linkedSetOf()) { constructor ->
        constructor.apply { isAccessible = true }.installHook(callback)
    }

fun hookMethod(
    className: String,
    classLoader: ClassLoader,
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: HookCallback
): XposedInterface.HookHandle =
    classLoader.findClass(className).hookMethod(methodName, *parameterTypes, callback = callback)

fun hookConstructor(
    className: String,
    classLoader: ClassLoader,
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: HookCallback
): XposedInterface.HookHandle {
    // methodName is retained for source compatibility with the old helper signature.
    @Suppress("UNUSED_VARIABLE") val ignored = methodName
    return classLoader.findClass(className).hookConstructor(*parameterTypes, callback = callback)
}

fun Method.hook(callback: HookCallback): XposedInterface.HookHandle =
    apply { isAccessible = true }.installHook(callback)

fun Class<*>.hookAllMethods(
    methodName: String,
    callback: HookCallback
): Set<XposedInterface.HookHandle> =
    allMethods().filter { it.name == methodName }.mapTo(linkedSetOf()) { method ->
        method.apply { isAccessible = true }.installHook(callback)
    }

private fun Executable.installHook(callback: HookCallback): XposedInterface.HookHandle {
    val context = HookContext().apply(callback)
    return ModernRuntime.hook(this) { chain -> context.intercept(chain) }
}

class HookContext {
    internal var beforeAction: HookAction? = null
        private set
    internal var afterAction: HookAction? = null
        private set
    internal var replaceAction: ReplaceAction? = null
        private set
    internal var needHook: (() -> Boolean)? = null
        private set

    fun doBefore(action: HookAction) {
        beforeAction = action
    }

    fun doAfter(action: HookAction) {
        afterAction = action
    }

    fun replace(action: ReplaceAction) {
        replaceAction = action
    }

    fun replace(hookCheck: () -> Boolean, action: ReplaceAction) {
        needHook = hookCheck
        replaceAction = action
    }

    internal fun intercept(chain: XposedInterface.Chain): Any? {
        val param = HookParam(
            chain.executable,
            chain.thisObject,
            chain.args.toTypedArray()
        )

        val replacement = replaceAction
        if (replacement != null && needHook?.invoke() != false) {
            return replacement.invoke(param)
        }

        beforeAction?.invoke(param)
        if (!param.returnEarly) {
            try {
                val proceeded = if (param.thisObject === chain.thisObject) {
                    chain.proceed(param.args)
                } else {
                    val receiver = checkNotNull(param.thisObject) {
                        "A non-static hook receiver cannot be changed to null"
                    }
                    chain.proceedWith(receiver, param.args)
                }
                param.setProceededResult(proceeded)
            } catch (t: Throwable) {
                param.setProceededThrowable(t)
            }
        }

        afterAction?.invoke(param)
        param.throwable?.let { throw it }
        return param.result
    }
}

fun HookParam.unhook(handle: XposedInterface.HookHandle) {
    handle.unhook()
}

fun Any.callMethod(methodName: String, vararg args: Any?): Any? =
    findCompatibleMethod(javaClass, methodName, args).invokeUnwrapped(this, args)

fun Any.callMethod(
    methodName: String,
    parameterTypes: Array<Class<*>>,
    vararg args: Any?
): Any? = findMethodExact(javaClass, methodName, parameterTypes).invokeUnwrapped(this, args)

fun Class<*>.callStaticMethod(methodName: String, vararg args: Any?): Any? =
    findCompatibleMethod(this, methodName, args, requireStatic = true).invokeUnwrapped(null, args)

fun Class<*>.callStaticMethod(
    methodName: String,
    parameterTypes: Array<Class<*>>,
    vararg args: Any?
): Any? = findMethodExact(this, methodName, parameterTypes).invokeUnwrapped(null, args)

fun Class<*>.newInstance(vararg args: Any?): Any =
    findCompatibleConstructor(this, args).newInstanceUnwrapped(args)

fun Class<*>.newInstance(parameterTypes: Array<Class<*>>, vararg args: Any?): Any =
    getDeclaredConstructor(*parameterTypes).apply { isAccessible = true }.newInstanceUnwrapped(args)

fun ClassLoader.findClass(className: String): Class<*> =
    Class.forName(className, false, this)

inline fun <reified T> Any.getOrNull(name: String): T? = getField(name, T::class.java)

inline operator fun <reified T> Any.get(name: String): T = getField(name, T::class.java)!!

inline operator fun <reified T> Any.set(name: String, value: T?) = setField(name, value, T::class.java)

fun <T> Any.getField(name: String, fieldClazz: Class<T>): T? {
    val target = if (this is Class<*>) null else this
    val owner = if (this is Class<*>) this else javaClass
    val field = findField(owner, name)
    val value: Any? = when (fieldClazz) {
        Boolean::class.java -> field.getBoolean(target)
        Byte::class.java -> field.getByte(target)
        Char::class.java -> field.getChar(target)
        Double::class.java -> field.getDouble(target)
        Float::class.java -> field.getFloat(target)
        Int::class.java -> field.getInt(target)
        Long::class.java -> field.getLong(target)
        Short::class.java -> field.getShort(target)
        else -> field.get(target)
    }
    @Suppress("UNCHECKED_CAST")
    return value as? T
}

fun <T> Any.setField(name: String, value: T?, fieldClass: Class<T>) {
    val target = if (this is Class<*>) null else this
    val owner = if (this is Class<*>) this else javaClass
    val field = findField(owner, name)
    when (fieldClass) {
        Boolean::class.java -> field.setBoolean(target, value as Boolean)
        Byte::class.java -> field.setByte(target, value as Byte)
        Char::class.java -> field.setChar(target, value as Char)
        Double::class.java -> field.setDouble(target, value as Double)
        Float::class.java -> field.setFloat(target, value as Float)
        Int::class.java -> field.setInt(target, value as Int)
        Long::class.java -> field.setLong(target, value as Long)
        Short::class.java -> field.setShort(target, value as Short)
        else -> field.set(target, value)
    }
}

private fun findField(clazz: Class<*>, fieldName: String): Field {
    var current: Class<*>? = clazz
    while (current != null) {
        try {
            return current.getDeclaredField(fieldName).apply { isAccessible = true }
        } catch (_: NoSuchFieldException) {
            current = current.superclass
        }
    }
    throw NoSuchFieldException("$clazz#$fieldName")
}

private fun findMethodExact(clazz: Class<*>, name: String, types: Array<out Class<*>>): Method {
    var current: Class<*>? = clazz
    while (current != null) {
        try {
            return current.getDeclaredMethod(name, *types).apply { isAccessible = true }
        } catch (_: NoSuchMethodException) {
            current = current.superclass
        }
    }
    throw NoSuchMethodException("$clazz#$name(${types.joinToString { it.name }})")
}

private fun findCompatibleMethod(
    clazz: Class<*>,
    name: String,
    args: Array<out Any?>,
    requireStatic: Boolean = false
): Method = clazz.allMethods().firstOrNull { method ->
    method.name == name &&
        (!requireStatic || Modifier.isStatic(method.modifiers)) &&
        parametersMatch(method.parameterTypes, args)
}?.apply { isAccessible = true }
    ?: throw NoSuchMethodException("$clazz#$name with ${args.size} compatible arguments")

private fun findCompatibleConstructor(clazz: Class<*>, args: Array<out Any?>): Constructor<*> =
    clazz.declaredConstructors.firstOrNull { parametersMatch(it.parameterTypes, args) }
        ?.apply { isAccessible = true }
        ?: throw NoSuchMethodException("$clazz constructor with ${args.size} compatible arguments")

private fun Class<*>.allMethods(): Sequence<Method> = sequence {
    val signatures = hashSetOf<String>()
    var current: Class<*>? = this@allMethods
    while (current != null) {
        current.declaredMethods.forEach { method ->
            val signature = method.name + method.parameterTypes.joinToString(prefix = "(") { it.name }
            if (signatures.add(signature)) yield(method)
        }
        current = current.superclass
    }
}

private fun parametersMatch(types: Array<Class<*>>, args: Array<out Any?>): Boolean =
    types.size == args.size && types.indices.all { index ->
        val arg = args[index]
        arg == null && !types[index].isPrimitive ||
            arg != null && boxed(types[index]).isAssignableFrom(arg.javaClass)
    }

private fun boxed(type: Class<*>): Class<*> = when (type) {
    java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
    java.lang.Byte.TYPE -> java.lang.Byte::class.java
    java.lang.Character.TYPE -> java.lang.Character::class.java
    java.lang.Double.TYPE -> java.lang.Double::class.java
    java.lang.Float.TYPE -> java.lang.Float::class.java
    java.lang.Integer.TYPE -> java.lang.Integer::class.java
    java.lang.Long.TYPE -> java.lang.Long::class.java
    java.lang.Short.TYPE -> java.lang.Short::class.java
    java.lang.Void.TYPE -> java.lang.Void::class.java
    else -> type
}

private fun Method.invokeUnwrapped(target: Any?, args: Array<out Any?>): Any? = try {
    invoke(target, *args)
} catch (e: InvocationTargetException) {
    throw e.targetException
}

private fun Constructor<*>.newInstanceUnwrapped(args: Array<out Any?>): Any = try {
    newInstance(*args)
} catch (e: InvocationTargetException) {
    throw e.targetException
}
