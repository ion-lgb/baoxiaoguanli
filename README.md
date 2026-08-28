# 报销管理（Android）

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84.svg)](https://developer.android.com/)

一款本地优先的 Android 个人出差费用与报销管理应用。使用 Kotlin、Jetpack Compose 与 Room 构建，支持行程、费用、图片/PDF 凭证、PDF 报销单、Excel+凭证 ZIP，以及手动 WebDAV 备份与恢复。

> 项目仍处于早期阶段。使用真实报销数据前，请自行验证导出内容与 WebDAV 恢复流程，并保留独立备份。

## 功能

- 创建、编辑、完成和删除出差行程
- 按行程记录费用，金额统一使用人民币“分”（`Long`）存储
- 内置分类与自定义分类
- 从相册、相机或文件选择器添加图片/PDF 凭证
- 生成包含费用明细、分类汇总与凭证附页的 PDF 报销单
- 导出 Excel 明细与凭证 ZIP 包
- 配置 WebDAV，手动备份、查看备份和恢复数据
- 数据默认保存在本机，不依赖后端服务

## 技术栈

- Kotlin 2.3.21
- Android Gradle Plugin 9.3.0（Built-in Kotlin）
- Gradle 9.5.0
- Jetpack Compose / Material 3
- Navigation-Compose 类型安全路由
- Room 3.0.1（`androidx.room3`）+ Bundled SQLite
- Coil 3
- iText 7.2.6（PDF，AGPLv3）
- Apache POI 5.5.1（Excel）
- sardine-android 0.9（WebDAV）
- kotlinx.serialization / Coroutines

## 架构

单 Activity、单 `:app` 模块，无 Hilt/Koin。`AppContainer` 手动持有数据库、Repository 和同步组件。

```text
Room Flow → Repository → ViewModel StateFlow → Compose UI
```

主要目录：

```text
app/src/main/java/cn/loxx/expense/
├── data/local/       Room Entity / DAO / Database
├── data/repository/  Repository 与凭证文件存储
├── data/model/       跨层模型、金额格式化
├── data/export/      PDF 与 Excel+ZIP 导出
├── data/webdav/      WebDAV 客户端、备份与恢复
└── ui/               Compose 页面、组件、导航、主题
```

## 环境要求

- Android Studio（使用内置 JDK）
- Android SDK Platform 37
- Android 8.0 / API 26 或更高版本

命令行构建需要设置：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

## 构建与测试

```bash
./gradlew assembleDebug
./gradlew test
```

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

当前测试是纯 JVM JUnit4 测试，覆盖金额换算、PDF 生成和 Excel+ZIP 生成。尚无 `androidTest`/仪器测试。

## 运行

### Android Studio

1. 使用 Android Studio 打开项目根目录。
2. 等待 Gradle Sync 完成。
3. 在 Device Manager 创建模拟器，或连接已开启 USB 调试的 Android 设备。
4. 选择 `app` 配置并点击 Run。

### 命令行

```bash
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
~/Library/Android/sdk/platform-tools/adb shell monkey \
  -p cn.loxx.expense \
  -c android.intent.category.LAUNCHER 1
```

## 数据与隐私

- Room 数据库：应用私有目录中的 `expense.db`
- 凭证：`filesDir/receipts/{expenseId}/...`
- 临时导出：`cacheDir/exports/`
- WebDAV 备份：数据库实体 JSON + 凭证文件 ZIP
- 应用不包含广告、统计 SDK 或自建服务器

目前 WebDAV 密码保存在应用私有的 `SharedPreferences` 中，**尚未使用 Android Keystore 加密**。不要在不可信或共享设备上保存敏感凭证。

恢复 WebDAV 备份会替换本机的行程、费用、分类和凭证。恢复前建议先执行一次备份。

## 非标准构建注意事项

- AGP 9 已内置 Kotlin，**不要**添加 `org.jetbrains.kotlin.android` 插件。
- Room 3 使用 `androidx.room3.*`，`@Relation` 参数是数组形式的 `parentColumns` / `entityColumns`。
- Apache POI 在 Android 上通过 Aalto XML 提供 StAX；相关 `System.setProperty` 必须在任何 POI 调用之前设置。
- Android 构建仅使用 iText 的 `kernel` / `io` / `layout` 模块，避免 `itext7-core` 聚合包引入不适用于 Android 的 AWT 模块。

更多维护说明见 [AGENTS.md](AGENTS.md)。

## 开源许可证

Copyright (C) 2026 ion-lgb

本项目以 **GNU Affero General Public License v3.0 only（AGPL-3.0-only）** 发布，完整文本见 [LICENSE](LICENSE)。

本项目使用 iText 7 Community 生成 PDF。iText Community 采用 AGPLv3/商业双重许可。按照 iText 的 AGPL 条款使用本项目时：

- 本应用及基于本应用的衍生作品必须以兼容的 AGPL 方式提供完整对应源码；
- 分发 APK/其他二进制时，必须向接收者提供对应源码与许可证；
- 不得移除 iText 生成 PDF 中的 Producer/版权信息；
- 对 iText 的修改也必须公开；
- 如果无法遵守 AGPLv3，请购买 iText 商业许可，或替换 `PdfExporter` 中的 iText 实现。

这不是法律意见。发布或商业使用前，请自行核对 AGPLv3 与各第三方依赖许可证。

第三方组件与字体许可见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。Noto Sans SC 的 OFL 文本保存在 `app/src/main/assets/NotoSansSC-OFL.txt`。

## 贡献

欢迎提交 Issue 或 Pull Request。提交代码即表示你有权贡献该代码，并同意按本项目的 AGPL-3.0-only 许可证发布。
