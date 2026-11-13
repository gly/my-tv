package com.lizongying.mytv

import android.util.Log
import com.lizongying.mytv.models.ProgramType
import java.net.HttpURLConnection
import java.net.URL

/**
 * M3U 直播源解析与下载
 */
object M3U {

    private const val TAG = "M3U"

    // 在线直播源，依次尝试，任一成功即使用
    private val sources = listOf(
        "https://raw.githubusercontent.com/vbskycn/iptv/master/tv/iptv4.m3u",
        "https://fastly.jsdelivr.net/gh/vbskycn/iptv@master/tv/iptv4.m3u",
        "https://testingcf.jsdelivr.net/gh/vbskycn/iptv@master/tv/iptv4.m3u",
        "https://gcore.jsdelivr.net/gh/vbskycn/iptv@master/tv/iptv4.m3u",
    )

    fun fetchText(url: String, timeout: Int = 8000): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = timeout
            conn.readTimeout = timeout
            conn.setRequestProperty("User-Agent", "my-tv/1.0")
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.e(TAG, "fetch $url code ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetch $url failed", e)
            null
        }
    }

    fun fetchSource(): String? {
        for (url in sources) {
            val text = fetchText(url)
            if (text != null && text.contains("#EXTINF")) {
                Log.i(TAG, "source ok: $url")
                return text
            }
        }
        return null
    }

    /**
     * 解析 M3U 文本为 分组 -> 频道列表
     * 同名频道的多条地址会合并为同一频道的多个源（播放失败时自动切换）
     */
    fun parse(text: String, maxSources: Int = 5): Map<String, List<TV>> {
        val groups = LinkedHashMap<String, LinkedHashMap<String, MutableList<String>>>()
        val logos = HashMap<String, String>()
        var name = ""
        var group = "其他"
        var logo = ""

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) {
                continue
            }
            if (line.startsWith("#EXTINF")) {
                group =
                    Regex("group-title=\"([^\"]*)\"").find(line)?.groupValues?.get(1)?.trim()
                        ?: "其他"
                if (group.isEmpty()) {
                    group = "其他"
                }
                logo = Regex("tvg-logo=\"([^\"]*)\"").find(line)?.groupValues?.get(1) ?: ""
                name = line.substringAfterLast(',').trim()
                if (name.isEmpty()) {
                    name = Regex("tvg-name=\"([^\"]*)\"").find(line)?.groupValues?.get(1)?.trim()
                        ?: "未知频道"
                }
            } else if (!line.startsWith("#")) {
                // 仅保留可被 ExoPlayer 播放的 http/https 流
                if (!line.startsWith("http://") && !line.startsWith("https://")) {
                    continue
                }
                if (name.isEmpty()) {
                    continue
                }
                val g = groups.getOrPut(group) { LinkedHashMap() }
                val urls = g.getOrPut(name) { mutableListOf() }
                if (urls.size < maxSources && !urls.contains(line)) {
                    urls.add(line)
                }
                if (logo.isNotEmpty() && !logos.containsKey(name)) {
                    logos[name] = logo
                }
                name = ""
                logo = ""
            }
        }

        // 分组排序：央视、卫视优先
        val priority = listOf("央视频道", "卫视频道", "体育频道", "纪录频道", "电影频道", "儿童频道", "音乐频道")
        val sortedKeys = groups.keys.sortedBy { k ->
            val p = priority.indexOf(k)
            if (p >= 0) p else priority.size
        }

        val result = LinkedHashMap<String, List<TV>>()
        var id = 0
        for (k in sortedKeys) {
            val list = mutableListOf<TV>()
            for ((title, urls) in groups[k]!!) {
                list.add(
                    TV(
                        id = id,
                        title = title,
                        alias = "",
                        videoUrl = urls.toList(),
                        channel = "",
                        logo = logos[title] ?: "",
                        pid = "",
                        sid = "",
                        programType = ProgramType.NONE,
                        needToken = false,
                        mustToken = false,
                    )
                )
                id++
            }
            if (list.isNotEmpty()) {
                result[k] = list
            }
        }
        return result
    }
}
