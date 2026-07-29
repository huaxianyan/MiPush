package one.yufz.hmspush.hook.fakedevice

class FakeEmuiOnly : IFakeDevice {
    override fun fake(loadedPackage: LoadedPackage): Boolean {
        fakeProperty(Property.EMUI_VERSION)
        return true
    }
}