package one.yufz.hmspush.hook.fakedevice

class QQ : Common() {

    override fun fake(loadedPackage: LoadedPackage): Boolean {
        if (loadedPackage.packageName == loadedPackage.processName ||
            loadedPackage.processName.endsWith(":MSF")
        ) {
            return super.fake(loadedPackage)
        }
        return false
    }
}