# Proguard for Modern Xposed API 102.
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# Transitional rules for legacy implementation files that have not been ported yet.
-keep class * implements de.robv.android.xposed.IXposedHookZygoteInit
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage
-keep class * implements de.robv.android.xposed.IXposedHookInitPackageResources

-keep class one.yufz.hmspush.hook.XposedMod{
    *;
}
-keep class com.huawei.android.app.NotificationManagerEx{
    *;
}