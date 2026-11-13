# 我的电视 · my-tv

安卓电视直播软件，安装即用。基于 [lizongying/my-tv](https://github.com/lizongying/my-tv) 改造：弃用已失效的央视频私有接口，改为加载在线 M3U 直播源，稳定可用。

## 特性

- **在线直播源** — 内置多镜像 M3U 源（自动依次尝试），包含 CCTV 全套、卫视、地方频道等 400+ 条流
- **本地缓存秒开** — 直播源缓存到本地，无网络也能启动，第二次打开约 1 秒开播
- **多源自动切换** — 同一频道聚合多条地址，播放失败自动切换下一个源
- **记住频道** — 退出时保存当前频道，下次启动直接恢复
- **电视遥控友好** — 频道上下切换、数字键直达、频道列表浏览、设置页

## 下载安装

| 渠道 | 地址 |
|---|---|
| GitHub Releases | [github.com/gly/my-tv/releases](https://github.com/gly/my-tv/releases) |
| Gitee 仓库 | [gitee.com/leoder/mg-tv](https://gitee.com/leoder/mg-tv) |

安装方式：

- U 盘安装：APK 拷贝到 U 盘，电视上用文件管理器打开
- 小米电视：使用「小米电视助手」安装
- ADB 安装：

```shell
adb install my-tv-3.0.0.apk
```

系统要求：Android 5.0+（minSdk 21），arm64-v8a / armeabi-v7a。

## 使用说明

- 打开 APP 自动播放上次观看的频道（首次播放央视频道第一个台）
- 遥控上下键切换频道，数字键直达频道号
- 单击画面打开频道列表，双击进入设置
- 某个频道卡顿或报错时会自动在同频道其他源之间切换

## 更换直播源

编辑 `app/src/main/java/com/lizongying/mytv/M3U.kt` 顶部的 `sources` 列表，填入你自己的 M3U 地址即可：

```kotlin
private val sources = listOf(
    "https://raw.githubusercontent.com/vbskycn/iptv/master/tv/iptv4.m3u",
    "https://fastly.jsdelivr.net/gh/vbskycn/iptv@master/tv/iptv4.m3u",
    // 换成你自己的源...
)
```

按顺序尝试，任一地址成功即使用；解析时自动过滤 udp/rtp 等播放器不支持的地址，同名频道多条地址会合并为多源。

## 本地构建

要求：JDK 17、Android SDK（含 NDK、CMake）。

```shell
./gradlew assembleRelease          # 正式签名包，输出到 app/build/outputs/apk/release/
./gradlew assembleDebug            # 调试包
```

仓库自带签名文件 `my-tv-release.jks`（仅供学习使用，请勿用于正式发布）。

## 自动发布

推送 `v*` 标签即可触发 GitHub Actions 自动构建并发布 Release：

```shell
git tag v3.0.1
git push origin v3.0.1
```

流程见 [.github/workflows/release.yml](.github/workflows/release.yml)。

## 更新日志

[更新日志](./HISTORY.md)

## 已知问题

- 免费直播源稳定性有限，个别频道可能失效（会自动换源）
- Gitee 镜像仅同步代码，发行版页面需手动上传 APK

## 版权说明

[LICENSE](./LICENSE)

本项目基于 [lizongying/my-tv](https://github.com/lizongying/my-tv) 修改，仅供学习研究，禁止用于商业用途，请于下载二十四小时内删除。

免费直播源来自网络公开收集，版权归各频道所有，如有侵权请联系删除。建议使用有线电视、运营商 IPTV 或官方 App 观看。
