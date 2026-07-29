# Proguard for Modern Xposed API 102.
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

-keep class com.huawei.android.app.NotificationManagerEx{
    *;
}