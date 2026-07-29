package one.yufz.hmspush.hook.fakedevice

data class LoadedPackage(
    val packageName: String,
    val processName: String,
    val classLoader: ClassLoader
)

interface IFakeDevice {
    fun fake(loadedPackage: LoadedPackage): Boolean
}
