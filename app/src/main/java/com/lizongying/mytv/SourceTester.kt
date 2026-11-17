package com.lizongying.mytv

import android.content.Context
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 直播源测速：后台探测每个源的首包延迟，用于把最快的源排到最前
 */
object SourceTester {

    private const val TAG = "SourceTester"
    private const val CACHE_FILE = "probe_cache.txt"
    private const val CACHE_TTL_MS = 12 * 60 * 60 * 1000L
    private const val THREADS = 24
    private const val TIMEOUT_MS = 3000

    /**
     * 探测所有频道的所有源
     * @return url -> 延迟(ms)，-1 表示不可达
     */
    fun probe(list: Map<String, List<TV>>): Map<String, Int> {
        val urls = list.values.flatten().flatMap { it.videoUrl }.distinct()
        val results = ConcurrentHashMap<String, Int>()
        val pool = Executors.newFixedThreadPool(THREADS)
        val start = System.currentTimeMillis()
        urls.forEach { url ->
            pool.execute { results[url] = test(url) }
        }
        pool.shutdown()
        try {
            pool.awaitTermination(120, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
        }
        val reachable = results.count { it.value >= 0 }
        Log.i(
            TAG,
            "probe done: ${results.size} urls in ${(System.currentTimeMillis() - start) / 1000}s, reachable=$reachable"
        )
        return results
    }

    /** 测量单个源的首包延迟，失败返回 -1 */
    fun test(url: String): Int {
        val start = System.currentTimeMillis()
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "my-tv/1.0")
            conn.inputStream.use { it.read(ByteArray(256)) }
            (System.currentTimeMillis() - start).toInt()
        } catch (e: Exception) {
            -1
        }
    }

    /** 读取测速缓存（12 小时内有效），避免每次启动都全量探测 */
    fun loadCache(context: Context): Map<String, Int>? {
        return try {
            val file = File(context.filesDir, CACHE_FILE)
            if (!file.exists()) return null
            val lines = file.readLines()
            if (lines.isEmpty()) return null
            val ts = lines[0].toLongOrNull() ?: return null
            if (System.currentTimeMillis() - ts > CACHE_TTL_MS) return null
            val map = HashMap<String, Int>()
            lines.drop(1).forEach { line ->
                val parts = line.split(" ")
                if (parts.size == 2) {
                    map[parts[0]] = parts[1].toInt()
                }
            }
            if (map.isNotEmpty()) map else null
        } catch (e: Exception) {
            null
        }
    }

    fun saveCache(context: Context, results: Map<String, Int>) {
        try {
            File(context.filesDir, CACHE_FILE).writeText(buildString {
                append(System.currentTimeMillis()).append('\n')
                results.forEach { (url, latency) ->
                    append(url).append(' ').append(latency).append('\n')
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "save cache failed", e)
        }
    }
}
