package one.yufz.hmspush.hook.hms.icon

import android.app.AndroidAppHelper
import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.yufz.hmspush.common.IconData
import one.yufz.hmspush.common.IconData.Companion.scaleForNotification
import one.yufz.hmspush.common.model.IconModel
import java.io.File

object IconManager {
    private const val QQ_PACKAGE_NAME = "com.tencent.mobileqq"
    private const val BUILT_IN_ICON_ASSET_DIR = "mipush_builtin_icons"

    private val context = AndroidAppHelper.currentApplication()

    private val iconDir = File(context.filesDir, "hms_push/icons")

    private val cache = LruCache<String, IconData>(20)

    fun hasBuiltInNotificationIcon(packageName: String): Boolean =
        packageName == QQ_PACKAGE_NAME

    suspend fun saveToLocal(packageName: String, jsonString: String) {
        withContext(Dispatchers.IO) {
            if (!iconDir.exists()) {
                iconDir.mkdirs()
            }

            File(iconDir, packageName).apply {
                if (exists()) {
                    delete()
                }
                createNewFile()

                writeText(jsonString)
            }
            cache.remove(cacheKey(packageName, true))
        }
    }

    fun getNotificationIconData(
        context: Context,
        packageName: String,
        useCustomIcon: Boolean = true
    ): IconData? {
        val cacheEntryKey = cacheKey(packageName, useCustomIcon)
        val cacheIconData = cache.get(cacheEntryKey)
        if (cacheIconData != null) return cacheIconData

        val iconDataFile = File(iconDir, packageName)
        val rawIconData = if (useCustomIcon && iconDataFile.exists()) {
            IconData.fromJson(iconDataFile.readText())
        } else {
            getBuiltInNotificationIconData(packageName) ?: return null
        }
        val iconData = rawIconData.scaleForNotification(context)

        cache.put(cacheEntryKey, iconData)

        return iconData
    }

    private fun cacheKey(packageName: String, useCustomIcon: Boolean) =
        "$packageName:${if (useCustomIcon) "custom" else "built-in"}"

    private fun getBuiltInNotificationIconData(packageName: String): IconData? {
        if (!hasBuiltInNotificationIcon(packageName)) return null

        val assetPath = "$BUILT_IN_ICON_ASSET_DIR/$packageName.json"
        return context.assets.open(assetPath).bufferedReader().use {
            IconData.fromJson(it.readText())
        }
    }

    suspend fun getAllIconModel(): List<IconModel> {
        return withContext(Dispatchers.IO) {
            iconDir.listFiles()
                ?.map { IconModel(it.name, dataFD = ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY)) }
                ?: emptyList()
        }
    }

    suspend fun deleteIcon(packages: Array<String>?) {
        return withContext(Dispatchers.IO) {
            val size = iconDir.listFiles()?.size ?: 0
            if (packages.isNullOrEmpty()) {
                if (size != 0) {
                    iconDir.deleteRecursively()
                    cache.evictAll()
                }
            } else {
                packages.onEach {
                    File(iconDir, it).delete()
                    cache.remove(cacheKey(it, true))
                    cache.remove(cacheKey(it, false))
                }
            }
        }
    }
}