package com.lizongying.mytv

import android.content.Context
import android.util.Log
import java.io.File

/**
 * 频道列表：从在线 M3U 直播源加载，本地缓存实现秒开
 */
object TVList {

    private const val TAG = "TVList"
    private const val CACHE_FILE = "sources.m3u"

    var list: Map<String, List<TV>> = mapOf()
        private set

    var loaded = false
        private set

    /**
     * 加载频道：缓存优先（无网络也可用），缓存不可用时从网络拉取
     */
    fun load(context: Context): Boolean {
        val file = File(context.filesDir, CACHE_FILE)
        if (file.exists() && file.length() > 0) {
            try {
                val parsed = M3U.parse(file.readText())
                if (parsed.isNotEmpty()) {
                    list = parsed
                    loaded = true
                    Log.i(
                        TAG,
                        "loaded from cache: ${parsed.size} groups, ${parsed.values.sumOf { it.size }} channels"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "parse cache failed", e)
            }
        }
        if (!loaded) {
            refresh(context)
        }
        return loaded
    }

    /**
     * 从网络重新拉取直播源并写入缓存（下次启动生效）
     */
    fun refresh(context: Context): Boolean {
        val text = M3U.fetchSource()
        if (text == null) {
            Log.e(TAG, "no source available")
            return loaded
        }
        try {
            File(context.filesDir, CACHE_FILE).writeText(text)
        } catch (e: Exception) {
            Log.e(TAG, "write cache failed", e)
        }
        val parsed = M3U.parse(text)
        if (parsed.isNotEmpty()) {
            list = parsed
            loaded = true
            Log.i(
                TAG,
                "refreshed: ${parsed.size} groups, ${parsed.values.sumOf { it.size }} channels"
            )
        }
        return loaded
    }
}
