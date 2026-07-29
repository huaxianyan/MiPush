package one.yufz.hmspush.hook.modern

import android.content.Context

/** Context captured from the target process without relying on Legacy AndroidAppHelper. */
object ProcessContext {
    @Volatile
    private var applicationContext: Context? = null

    fun attach(context: Context) {
        applicationContext = context.applicationContext ?: context
    }

    fun require(): Context = checkNotNull(applicationContext) {
        "Target process application context is not ready"
    }
}
