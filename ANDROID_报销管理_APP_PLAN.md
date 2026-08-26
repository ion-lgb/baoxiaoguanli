# Android 个人报销管理 APP 实施计划

## Context

构建一个 Android 原生 APP（Kotlin + Jetpack Compose），用于出差费用记录和报销管理。核心流程：创建出差行程 → 逐条记录费用（含上传凭证截图/PDF）→ 将行程费用打包生成报销单（PDF 或 Excel+ZIP）。数据本地存储（Room），支持通过用户配置的 WebDAV 服务器进行云端备份/同步。金额以整数分存储，仅支持人民币。费用分类预设常用项 + 用户可自定义。

## Approach

### Step 1: 项目脚手架 — Gradle + 依赖 + 包结构

创建 Android 项目，单模块 `:app`，包名 `cn.loxx.expense`。

**build.gradle.kts (project-level)** 插件：
```kotlin
plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
}
```

**build.gradle.kts (app-level)** 关键配置：
- `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37`
- `kotlinOptions.jvmTarget = "17"`
- 依赖（全部已验证版本可用性）：

| 用途 | 依赖 | 版本 |
|---|---|---|
| Compose BOM | `platform("androidx.compose:compose-bom")` | `2026.08.00` |
| Compose UI/Material3 | `compose-bom` 管理 | — |
| Activity Compose | `androidx.activity:activity-compose` | `1.13.0` |
| Navigation | `androidx.navigation:navigation-compose` | `2.9.8` |
| Lifecycle ViewModel | `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.11.0` |
| Lifecycle Runtime | `androidx.lifecycle:lifecycle-runtime-compose` | `2.11.0` |
| Room Runtime | `androidx.room3:room3-runtime` | `3.0.1` |
| Room Compiler (KSP) | `androidx.room3:room3-compiler` | `3.0.1` |
| SQLite Bundled Driver | `androidx.sqlite:sqlite-bundled` | (随 Room 3 传递) |
| Coil Compose | `io.coil-kt.coil3:coil-compose` | `3.5.0` |
| Kotlin Serialization JSON | `org.jetbrains.kotlinx:kotlinx-serialization-json` | `1.11.0` |
| Coroutines Android | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.11.0` |
| iText 7 Core | `com.itextpdf:itext7-core` | `7.2.6` |
| Apache POI | `org.apache.poi:poi` + `poi-ooxml` | `5.5.1` |
| Aalto XML (POI StAX) | `com.fasterxml:aalto-xml` | `1.4.0` |
| WebDAV | `com.github.thegrizzlylabs:sardine-android` | `0.9` |

JitPack repository 需要在 settings.gradle.kts 的 dependencyResolutionManagement 中添加 `maven("https://jitpack.io")`。

**包结构**（所有在 `cn.loxx.expense` 下）：
```
ui/
  home/          — 首页：行程列表
  trip/          — 行程详情 + 费用列表
  expense/       — 添加/编辑费用
  report/        — 报销单预览 + 导出
  settings/      — WebDAV 配置 + 分类管理
  component/     — 共用 Compose 组件
  theme/         — Material 3 主题 (primary=#6d5ef2)
data/
  local/         — Room entities, DAOs, Database, TypeConverters
  repository/    — TripRepository, ExpenseRepository, CategoryRepository, SettingsRepository
  export/        — PdfExporter, ExcelExporter (生成文件到 cacheDir)
  webdav/        — WebDavClient (sardine 封装), SyncManager
  model/         — 跨层共享的数据类（如 TripWithExpenses）
```

**Application 类** `ExpenseApp`：初始化 Room DB + Coil ImageLoader + POI StAX 系统属性（3 行 `System.setProperty`）。手动 DI 容器 `AppContainer`，持有 Database 实例 + 各 Repository 单例。不引入 Hilt/Koin，保持简单。

### Step 2: 数据层 — Room 数据库 + Entities + DAOs

**Entities**（包 `data.local`）：

```kotlin
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,           // "8月北京出差"
    val destination: String,     // "北京"
    val startDate: Long,         // epoch millis
    val endDate: Long,           // epoch millis, 0 = 进行中
    val status: String,          // "ongoing" | "completed" | "reported"
    val note: String,            // 备注
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tripId")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val categoryId: Long,        // 关联 categories 表
    val amountCents: Long,       // 金额（分）
    val description: String,     // "打车去机场"
    val date: Long,              // 消费日期 epoch millis
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,         // 关联 expenses
    val filePath: String,        // 相对于 filesDir 的路径
    val fileType: String,        // "image" | "pdf"
    val originalName: String,    // 原始文件名
    val createdAt: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,            // "交通"
    val icon: String,            // Material icon name: "directions_car"
    val isBuiltin: Boolean,      // 预设分类不可删除
    val sortOrder: Int,
    val createdAt: Long
)
```

**预设分类**（通过 `RoomDatabase.Callback.onCreate` 插入）：
1. 交通 (directions_car)
2. 住宿 (hotel)
3. 餐饮 (restaurant)
4. 通讯 (phone)
5. 办公用品 (inventory_2)
6. 其他 (more_horiz)

**DAOs**：

- `TripDao`：`getAll(): Flow<List<TripEntity>>`（按 createdAt DESC），`getById(id): Flow<TripEntity?>`，`insert(trip): Long`，`update(trip)`，`delete(trip)`，`getTripWithExpenses(tripId): Flow<TripWithExpenses>`（`@Transaction` + `@Relation`）。
- `ExpenseDao`：`getByTripId(tripId): Flow<List<ExpenseEntity>>`，`insert(expense): Long`，`update(expense)`，`delete(expense)`，`getTotalCentsByTrip(tripId): Flow<Long>`。
- `ReceiptDao`：`getByExpenseId(expenseId): Flow<List<ReceiptEntity>>`，`insert(receipt): Long`，`delete(receipt)`，`getByTripId(tripId): Flow<List<ReceiptEntity>>`（JOIN expenses）。
- `CategoryDao`：`getAll(): Flow<List<CategoryEntity>>`（按 sortOrder），`insert(cat): Long`，`update(cat)`，`delete(cat)`，`getById(id): Flow<CategoryEntity?>`。

**TypeConverters**：不需要。所有日期已存为 `Long`（epoch millis），枚举存为 `String`。

**Database**：
```kotlin
@Database(entities = [TripEntity::class, ExpenseEntity::class, ReceiptEntity::class, CategoryEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun categoryDao(): CategoryDao
}
```

构建方式（Room 3.0.1）：
```kotlin
Room.databaseBuilder<AppDatabase>(context, "expense.db")
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .addCallback(SeedCategoriesCallback())
    .build()
```

**Repositories**：每个 Repository 是一个类，接收对应 DAO + Context（用于文件操作）。方法签名与 DAO 一一对应，Repository 层不做额外转换（数据模型足够简单，直接用 Entity）。`ExpenseRepository.addReceipt(expenseId, sourceUri, fileType)` 负责从 content URI 复制文件到 `filesDir/receipts/{expenseId}/{uuid}.{ext}` 并插入 ReceiptEntity。

### Step 3: 导航 + 主题 + 应用骨架

**导航路由**（type-safe，`@Serializable`）：
```kotlin
@Serializable object HomeRoute
@Serializable data class TripDetailRoute(val tripId: Long)
@Serializable data class AddEditExpenseRoute(val tripId: Long, val expenseId: Long = 0) // 0 = 新建
@Serializable data class ReceiptViewRoute(val receiptId: Long)
@Serializable object SettingsRoute
@Serializable data class ReportPreviewRoute(val tripId: Long)
```

**单 Activity**（`MainActivity`）：设置 `NavHost`，startDestination = `HomeRoute`。

**主题**：Material 3 动态色方案 + 固定 seed color `#6d5ef2`（与微信小程序保持一致）。定义在 `ui/theme/Theme.kt`。

### Step 4: 首页 — 行程列表

**HomeScreen** + **HomeViewModel**：

UI：
- TopAppBar 标题 "我的行程"，右侧齿轮图标 → 导航到 SettingsRoute
- FAB "+" → 弹出 BottomSheet 创建新行程（填写：标题、目的地、开始日期、可选结束日期、备注）
- 行程卡片列表（LazyColumn）：每张卡片显示标题、目的地、日期范围、费用总额（DAO 查询汇总分→格式化为元）、状态标签（进行中/已完成/已报销）
- 左滑删除行程（SwipeToDismiss）
- 点击行程 → 导航到 TripDetailRoute(tripId)

ViewModel：
- `uiState: StateFlow<HomeUiState>` 包含 `trips: List<TripWithTotal>`（TripEntity + totalCents: Long）
- 从 TripRepository 观察 Flow
- 提供 `createTrip(title, destination, startDate, endDate, note)` / `deleteTrip(trip)` 方法

### Step 5: 行程详情 — 费用记录列表

**TripDetailScreen** + **TripDetailViewModel**：

UI：
- TopAppBar 显示行程标题，菜单：编辑行程 / 标记完成 / 生成报销单
- 费用总额卡片（大字显示 ¥xxx.xx）
- 按日期分组的费用列表（LazyColumn + stickyHeader）：每条显示分类图标、描述、金额、凭证数量角标
- 点击费用 → 展开显示凭证缩略图列表（Coil AsyncImage）+ 编辑/删除按钮
- FAB "记一笔" → 导航到 AddEditExpenseRoute(tripId, 0)
- "生成报销单" → 导航到 ReportPreviewRoute(tripId)

ViewModel：
- `uiState: StateFlow<TripDetailUiState>` 包含 trip、expenses（含凭证列表）、totalCents
- 从 ExpenseRepository + ReceiptRepository 组合 Flow（`combine`）

### Step 6: 添加/编辑费用 + 上传凭证

**AddEditExpenseScreen** + **AddEditExpenseViewModel**：

UI：
- 如果 expenseId > 0 加载已有数据，否则空表单
- 表单字段：
  - 金额输入：数字键盘，显示 ¥ 前缀，输入元（如 "123.45"），保存时 ×100 转为分
  - 分类选择：水平滚动 Chip 列表（从 CategoryDao 查询）
  - 描述：单行文本
  - 日期：DatePicker，默认今天
- 凭证区域：
  - 已添加的凭证网格（2 列 LazyVerticalGrid）：图片显示缩略图（Coil），PDF 显示 PDF 图标 + 文件名。长按删除。
  - "添加凭证" 按钮弹出选择：拍照（相机 Intent）/ 从相册选择（PickVisualMedia，支持多选 maxItems=9）/ 选择 PDF（OpenDocument，filter `application/pdf`）
- 保存按钮（校验：金额>0，分类已选）

文件存储流程：
1. 用户通过 picker 选择文件 → 得到 content URI
2. Repository 将文件复制到 `filesDir/receipts/{expenseId}/{UUID}.{jpg|pdf}`
3. ReceiptEntity 存储相对路径（如 `receipts/5/abc123.jpg`）
4. 显示时通过 `File(filesDir, relativePath)` 加载

拍照流程：
- 使用 `ActivityResultContracts.TakePicture()`，先创建临时文件 URI（FileProvider），拍照成功后复制到 receipts 目录

### Step 7: 报销单导出 — PDF + Excel+ZIP

**ReportPreviewScreen** + **ReportViewModel**：

UI：
- 报销单预览信息：行程标题、日期范围、费用按分类汇总表、总金额
- 两个导出按钮："导出 PDF 报销单" / "导出 Excel+凭证包"
- 导出完成后弹出分享 Sheet（Android Sharesheet）或保存到位置

**PdfExporter**（`data.export.PdfExporter`）：

使用 iText 7.2.6 生成 PDF，输出到 `cacheDir/exports/report_{tripId}.pdf`。

PDF 结构：
1. **封面**：标题 "费用报销单"，行程名称，报销人（从设置读取，如未设置显示"—"），日期范围，制表日期
2. **费用明细表**：Table 列 = 序号 | 日期 | 分类 | 描述 | 金额(¥)，最后一行合计
3. **按分类汇总**：Table 列 = 分类 | 笔数 | 小计(¥)
4. **凭证附页**：每个凭证一页或半页，图片居中缩放铺满（保持比例），PDF 凭证用 iText 的 PdfFormXObject 嵌入

中文字体：在 `assets/` 中放入 `NotoSansSC-Regular.otf`（~8MB），通过 `PdfFontFactory.createFont(assetPath, PdfEncodings.IDENTITY_H)` 加载。

关键 API：
```kotlin
val writer = PdfWriter(outputStream)
val pdf = PdfDocument(writer)
val document = Document(pdf, PageSize.A4)
val font = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H)
document.setFont(font)
// Table: Table(floatArrayOf(1f, 2f, 2f, 3f, 2f)) + Cell(...) + Paragraph(...)
// Image: Image(ImageDataFactory.create(receiptBytes)).setAutoScale(true)
document.close()
```

**ExcelExporter**（`data.export.ExcelExporter`）：

使用 Apache POI 5.5.1 生成 xlsx + 将凭证文件打包为 ZIP。

Application.onCreate 中需设置 3 个系统属性：
```kotlin
System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.evt.EventFactoryImpl")
```

Excel 结构（单 sheet "费用明细"）：
- 表头行：序号 | 日期 | 分类 | 描述 | 金额(¥) | 凭证文件名
- 数据行：每条费用一行，金额 = amountCents / 100.0（setCellValue(double)）
- 末尾合计行，金额列用 SUM 公式
- 样式：表头加粗 + 灰色背景，金额列格式 `#,##0.00`

ZIP 包结构（`cacheDir/exports/report_{tripId}.zip`）：
```
报销单_{行程标题}/
  费用明细.xlsx
  凭证/
    001_交通_打车去机场.jpg
    002_住宿_酒店发票.pdf
    ...
```

凭证文件名格式：`{序号}_{分类}_{描述}.{ext}`，用 `java.util.zip.ZipOutputStream` 打包。

**分享/保存**：导出完成后使用 `ActivityResultContracts.CreateDocument` 让用户选择保存位置（PDF 用 `application/pdf`，ZIP 用 `application/zip`），同时提供 Android Sharesheet 分享选项（通过 FileProvider + Intent.ACTION_SEND）。

### Step 8: 设置页 — 分类管理 + WebDAV 配置

**SettingsScreen** + **SettingsViewModel**：

UI 分两个区域：

**个人信息**：
- 报销人姓名：编辑文本，保存到 SharedPreferences（`SettingsRepository` 封装）
- 所属部门：编辑文本

**分类管理**：
- 当前分类列表（可拖动排序，ReorderableList）
- 预设分类显示锁图标，不可删除但可改名
- 自定义分类可编辑名称、删除
- 底部 "添加分类" 按钮

**WebDAV 配置**：
- 服务器地址：`https://your-server.com/dav/expense/`
- 用户名 + 密码（密码用 EncryptedSharedPreferences 存储）
- "测试连接" 按钮：尝试 PROPFIND，成功显示绿色提示
- "立即同步" 按钮

**SettingsRepository**：封装 `EncryptedSharedPreferences`，存储 userName、department、webdavUrl、webdavUser、webdavPass。

### Step 9: WebDAV 同步

**WebDavClient**（`data.webdav.WebDavClient`）：

封装 sardine-android 的同步操作：
```kotlin
class WebDavClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private val sardine = OkHttpSardine().apply { setCredentials(username, password) }

    suspend fun testConnection(): Boolean  // PROPFIND baseUrl
    suspend fun upload(remotePath: String, data: ByteArray)
    suspend fun download(remotePath: String): ByteArray
    suspend fun list(remotePath: String): List<DavResource>
    suspend fun mkdir(remotePath: String)
    suspend fun exists(remotePath: String): Boolean
}
```

**SyncManager**（`data.webdav.SyncManager`）：

同步策略 — **手动触发的全量备份**（非实时同步），简单可靠：

1. 导出整个数据库 + 凭证文件为一个 ZIP 备份包
2. 上传到 WebDAV 服务器的 `/expense-backups/backup_{timestamp}.zip`
3. 下载 = 从服务器拉取最新备份 ZIP，替换本地数据库 + 凭证文件

备份 ZIP 结构：
```
backup_20260826_143000/
  expense.db          — SQLite 数据库文件副本
  receipts/           — 整个 receipts 目录
```

SyncManager 方法：
- `backup()`: 关闭 DB checkpoint → 复制 db 文件 + receipts → ZIP → upload
- `restore()`: download 最新 → 解压 → 替换 db + receipts → 重启 DB 连接
- `listBackups()`: 列出服务器上的备份列表（带日期）

在 Settings 页面提供：备份 / 恢复 / 查看备份列表 三个操作入口。

### Step 10: 文件管理 + FileProvider

**AndroidManifest.xml** 中声明：
- `FileProvider`：authority = `cn.loxx.expense.fileprovider`，paths 配置 `files-path` (receipts) + `cache-path` (exports + camera temp)
- `uses-permission INTERNET`（WebDAV）
- `uses-permission CAMERA`（拍照，可选）
- `android:usesCleartextTraffic="true"`（支持 http:// WebDAV 服务器）

**file_paths.xml**：
```xml
<paths>
    <files-path name="receipts" path="receipts/" />
    <cache-path name="exports" path="exports/" />
    <cache-path name="camera" path="camera/" />
</paths>
```

### Step 11: CJK 字体资源

在 `app/src/main/assets/` 中放入 `NotoSansSC-Regular.otf`（约 8MB）。

获取方式：从 Google Fonts 下载 Noto Sans SC Regular（https://fonts.google.com/noto/specimen/Noto+Sans+SC），取 Regular 权重的 OTF 文件。

此字体用于 PDF 导出中的中文渲染。Excel 不需要内嵌字体（由查看端设备字体渲染）。

## Critical files & anchors

| 文件 | 关注点 | 原因 |
|---|---|---|
| `app/build.gradle.kts` | 依赖声明 + KSP 配置 | iText/POI/sardine 的精确坐标和 JitPack 仓库配置，错一个都编译不过 |
| `data/local/AppDatabase.kt` | Room 3.0.1 Builder API | 新版 `Room.databaseBuilder<T>(context, name)` + `BundledSQLiteDriver` + `setQueryCoroutineContext`，与 Room 2.x 差异大 |
| `data/export/PdfExporter.kt` | iText 7 字体加载 + 表格布局 | `PdfFontFactory.createFont(bytes, IDENTITY_H)` 必须用 IDENTITY_H 编码才能正确渲染中文 |
| `ExpenseApp.kt` (Application) | POI StAX 属性设置 | 3 个 `System.setProperty` 必须在任何 POI 调用前执行，否则运行时崩溃 |
| `data/webdav/WebDavClient.kt` | sardine 凭证 + 线程切换 | sardine 是同步阻塞 API，所有调用必须在 `withContext(Dispatchers.IO)` 中 |

## Verification

### 1. 项目构建验证
```bash
cd app && ./gradlew assembleDebug
```
预期：APK 生成成功，无编译错误。在 Android Studio 中打开项目也可直接 Build。

### 2. 核心流程端到端测试（在模拟器/真机上手动验证）

**行程 + 费用录入流程**：
1. 启动 APP → 首页显示空状态提示 "暂无行程"
2. 点击 FAB 创建行程 "8月测试出差"，目的地 "上海"，开始日期选今天 → 保存 → 列表显示新行程卡片，金额 ¥0.00
3. 点击行程进入详情 → 空费用列表
4. 点击 "记一笔"：金额 123.45，分类选 "交通"，描述 "打车"，日期今天 → 保存 → 列表显示该费用
5. 再添加一笔：金额 500，分类 "住宿"，描述 "酒店"，附加一张相册图片凭证 → 保存 → 列表显示凭证角标 "1"
6. 返回首页 → 行程卡片金额显示 ¥623.45

**PDF 导出验证**：
1. 在行程详情点击 "生成报销单" → 预览页面显示汇总信息
2. 点击 "导出 PDF" → 系统文件选择器弹出 → 选择保存位置
3. 用 PDF 阅读器打开：封面有行程标题，明细表有 2 行数据，合计 ¥623.45，凭证附页有图片

**Excel+ZIP 导出验证**：
1. 点击 "导出 Excel+凭证包" → 选择保存 → 得到 ZIP 文件
2. 解压：包含 `费用明细.xlsx` 和 `凭证/` 目录下有对应图片文件
3. 打开 xlsx：数据正确，合计行有公式

**WebDAV 验证**：
1. 设置页填入一个可用的 WebDAV 地址（如 Nextcloud/坚果云）
2. 点击 "测试连接" → 显示连接成功
3. 点击 "备份" → 进度指示 → 完成提示
4. 清空 APP 数据 → 重新配置 WebDAV → 恢复 → 之前的行程和费用数据恢复

### 3. 自动化测试（最低限度）

```bash
./gradlew test  # 运行 JVM 单元测试
```

- `PdfExporterTest`：给定一组 TripEntity + ExpenseEntity + 模拟凭证字节 → PdfExporter 输出 PDF 字节 → 验证 PDF 字节非空且 > 1KB
- `ExcelExporterTest`：同上 → 验证 xlsx 字节可被 POI 读回且行数正确
- `AmountFormatterTest`：`formatCents(12345L)` → `"123.45"`，`parseToCents("123.45")` → `12345L`，`parseToCents("0.01")` → `1L`

## Assumptions & contingencies

- **iText AGPL 许可证**：用户已确认接受。如果后续需要闭源发布，将 PdfExporter 替换为 `android.graphics.pdf.PdfDocument`（手写 Canvas 绘制表格，代码量增加约 300 行，功能等价但布局代码更繁琐）。
- **NotoSansSC-Regular.otf 体积**：约 8MB 会增加 APK 大小。如果不可接受，可改用 Android 系统内置 PdfDocument API（使用系统 CJK 字体，零额外体积）。
- **Room 3.0.1 API 稳定性**：Room 3 是 2026 新发布的重大版本更新。如果遇到 KSP/编译问题，回退到 Room 2.8.4（`androidx.room:room-runtime:2.8.4`，API 基本一致，Builder 方式不同：`Room.databaseBuilder(context, AppDatabase::class.java, "expense.db").build()`，不需要 BundledSQLiteDriver）。
- **sardine-android 维护状态**：最后更新 2024-02，如果遇到兼容性问题，替换为直接用 OkHttp 发送 WebDAV 请求（PUT/GET/MKCOL/PROPFIND ~200 行代码）。
