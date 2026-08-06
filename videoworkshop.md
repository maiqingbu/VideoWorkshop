# VideoWorkshop 完整项目架构与第一阶段开发执行令

## 一、架构结论

VideoWorkshop 采用：

> **Android 本地优先 + 模块化单体 + 项目制数据模型 + 持久化任务队列 + 可插拔外部服务适配器。**

第一阶段的完整用户链路为：

```text
创建创作项目
  ↓
选择或手动录入商品
  ↓
导入、下载、整理素材
  ↓
生成并编辑脚本
  ↓
生成配音和时间轴字幕
  ↓
视频成片 / 图文成片
  ↓
导出前质量检查
  ↓
调起平台发布
  ↓
确认发布结果并保存记录
```

当前项目已有的 AB 音画重组、原创加工、AI 文案、配音、字幕、图文、发布等能力不推倒重写，而是逐步接入上述项目闭环。

---

# 二、系统级完整架构

## 2.1 总体架构

```text
┌────────────────────────────────────────────────────────────┐
│                    Android Presentation                     │
│ Compose Screen / Route / ViewModel / UiState / UiEvent     │
├────────────────────────────────────────────────────────────┤
│                       Domain Layer                         │
│ Project / Goods / Asset / Script / Render / Publish       │
│ UseCase / Repository Port / Policy / State Machine        │
├────────────────────────────────────────────────────────────┤
│                        Data Layer                          │
│ Local Repository / Remote Adapter / Mapper / Cache        │
├──────────────────┬──────────────────┬──────────────────────┤
│ Room Database    │ Project Files    │ Secure Credentials   │
│ DataStore        │ MediaStore       │ Android Keystore     │
├──────────────────┴──────────────────┴──────────────────────┤
│                  Processing Infrastructure                 │
│ WorkManager / FFmpeg / Media3 / Canvas Renderer           │
├────────────────────────────────────────────────────────────┤
│                    External Providers                      │
│ LLM / TTS / ASR / 淘宝 / 京东 / 拼多多 / Android Share     │
└────────────────────────────────────────────────────────────┘
```

未来需要账号 OAuth、平台直发、云同步、数字人、团队协作时，再增加：

```text
VideoWorkshop Managed Backend
├── 用户与设备认证
├── 平台 OAuth
├── 云端密钥托管
├── 数字人和云渲染代理
├── 云同步
├── 发布和数据同步
├── 套餐、额度与成本控制
└── 团队协作
```

第一阶段不得为了未来后台而提前引入微服务。

---

# 三、目标模块结构

## 3.1 App 组装层

```text
:app
```

职责：

* Application 初始化。
* Hilt 依赖组装。
* 根导航。
* Deep Link 和 Android Share 接收。
* WorkManager 配置。
* 通知渠道。
* 全局错误入口。

禁止：

* 编写业务规则。
* 直接访问 DAO。
* 直接调用 Retrofit。
* 直接拼接 FFmpeg 命令。
* 在路由参数中传递完整文件路径或大段 JSON。

---

## 3.2 Core 基础模块

保留现有模块并逐步补充：

```text
:core:core-common
:core:core-designsystem
:core:core-ui
:core:core-database
:core:core-datastore
:core:core-network
:core:core-media
:core:core-ffmpeg
:core:core-security       新增
:core:core-testing        新增
```

### core-common

负责：

* Result 和错误类型。
* Dispatcher。
* 时间、UUID、校验等通用工具。
* 日志脱敏规则。
* 不含任何具体业务模型。

### core-database

负责：

* Room 数据库。
* Entity、DAO、Migration。
* 数据库事务。
* 不承担领域业务判断。

### core-datastore

只保存：

* 非敏感用户偏好。
* 默认服务商。
* 默认导出质量。
* 默认音色、语言、平台。
* 功能开关。

API Secret 不再明文保存在普通 Preferences DataStore。

### core-security

负责：

* Android Keystore 封装。
* API 密钥加密保存、读取、删除。
* 凭证别名管理。
* 敏感数据禁止写入日志。
* 凭证可用性检测。

### core-media

升级为统一素材文件基础设施：

* URI 导入。
* 临时文件。
* 项目目录。
* 缩略图。
* 媒体元数据。
* SHA-256 校验。
* 文件原子移动。
* MediaStore 导出。
* 垃圾文件清理。

### core-ffmpeg

只负责通用媒体处理能力：

* 探测。
* 裁剪。
* 合流。
* 混音。
* 字幕烧录。
* 贴纸叠加。
* 转码。
* 质量检测。
* 进度和任务级取消。

最终目标是 `core-ffmpeg` 不依赖业务领域模型。业务配置由 data/domain 层转换为 FFmpeg 请求对象。

---

## 3.3 Domain 领域层

```text
:domain
```

必须保持：

* 不依赖 Feature。
* 不依赖 Room Entity。
* 不依赖 Retrofit DTO。
* 尽量不依赖 Android Context。
* 所有业务 ID 统一使用 `String UUID`。
* 不再出现数据库为 String、领域层为 Long 的双重 ID 体系。

主要领域：

```text
project/
goods/
asset/
script/
task/
render/
publish/
settings/
quality/
```

领域层包含：

* 领域模型。
* Repository 接口。
* UseCase。
* 状态机。
* 业务校验规则。
* 任务配方和处理计划。

---

## 3.4 Data 数据模块

目标结构：

```text
:data:data-project       新增
:data:data-material      从 data-repository 拆出
:data:data-task          新增
:data:data-ai
:data:data-alliance
:data:data-publish
:data:data-render        从 data-repository 拆出
:data:data-template      第二阶段
:data:data-analytics     第二阶段
```

现有 `data:data-repository` 不立即删除，但禁止继续塞入新的无关仓库。迁移完成一个领域后，就从该模块移出对应代码。

---

## 3.5 Feature 功能模块

第一阶段目标：

```text
:feature:feature-home
:feature:feature-project       新增
:feature:feature-goods
:feature:feature-material
:feature:feature-taskqueue     新增
:feature:feature-videoenhance
:feature:feature-imageeditor
:feature:feature-abtransport
:feature:feature-dedup
:feature:feature-publish
:feature:feature-history
:feature:feature-settings
```

第二阶段再增加：

```text
:feature:feature-script
:feature:feature-videoeditor
:feature:feature-template
:feature:feature-calendar
:feature:feature-analytics
:feature:feature-longclip
```

---

# 四、强制模块依赖规则

```text
feature  ─────→ domain
feature  ─────→ core-ui / core-designsystem / core-common

data     ─────→ domain
data     ─────→ core

app      ─────→ feature
app      ─────→ data
app      ─────→ core

domain   ─────→ 不依赖 data、feature、Room、Retrofit
core     ─────→ 不依赖 feature
feature  ─────→ 不直接依赖 data
```

禁止产生：

```text
feature → data
domain → Room Entity
domain → Retrofit DTO
core → feature
data-A → data-B 的随意横向依赖
ViewModel → DAO
Composable → Repository
```

---

# 五、核心领域模型

## 5.1 Project：创作项目聚合根

```kotlin
Project(
    id: String,
    title: String,
    type: ProjectType,
    status: ProjectStatus,
    goodsSnapshotId: String?,
    targetPlatforms: Set<PublishPlatform>,
    coverAssetId: String?,
    createdAt: Long,
    updatedAt: Long,
    lastOpenedAt: Long
)
```

类型：

```text
VIDEO_COMMERCE
IMAGE_COMMERCE
AB_RECOMPOSE
VIDEO_REWORK
LONG_VIDEO_CLIP
```

状态：

```text
DRAFT
PREPARING
PROCESSING
READY_TO_PUBLISH
PUBLISHED
FAILED
ARCHIVED
```

一个项目负责关联：

* 商品快照。
* 原始素材。
* 脚本。
* 配音。
* 字幕。
* 处理任务。
* 成片。
* 发布记录。

---

## 5.2 Goods 与 GoodsSnapshot

`Goods` 表示联盟实时搜索结果。

`GoodsSnapshot` 表示项目创建时锁定的商品信息：

```kotlin
GoodsSnapshot(
    id: String,
    projectId: String,
    provider: AllianceProvider,
    externalGoodsId: String?,
    name: String,
    price: Money?,
    originalPrice: Money?,
    commissionRate: Double?,
    promoUrl: String?,
    imageUrls: List<String>,
    videoUrls: List<String>,
    sellingPoints: List<String>,
    capturedAt: Long
)
```

必须使用快照，避免商品价格、标题和链接变化后破坏旧项目。

同时支持：

* 联盟商品。
* 链接解析商品。
* 用户手动创建的自有商品。

---

## 5.3 Asset：统一素材模型

替代当前含义有限的 `MaterialEntity`：

```kotlin
Asset(
    id: String,
    mediaType: AssetMediaType,
    origin: AssetOrigin,
    storageType: AssetStorageType,
    localPath: String,
    displayName: String,
    mimeType: String?,
    sizeBytes: Long,
    durationMs: Long?,
    width: Int?,
    height: Int?,
    frameRate: Double?,
    checksum: String?,
    thumbnailPath: String?,
    parentAssetId: String?,
    lifecycle: AssetLifecycle,
    createdAt: Long
)
```

素材类型：

```text
VIDEO
IMAGE
AUDIO
SUBTITLE
DOCUMENT
```

来源：

```text
USER_IMPORT
CAMERA
ALLIANCE_DOWNLOAD
AI_GENERATED
RENDER_OUTPUT
BUILT_IN_RESOURCE
```

项目与素材使用关系表关联：

```text
PROJECT_SOURCE_VIDEO
PROJECT_SOURCE_IMAGE
PROJECT_AUDIO
PROJECT_BGM
PROJECT_STICKER
PROJECT_SUBTITLE
PROJECT_COVER
PROJECT_OUTPUT
```

同一素材可以被多个项目复用。

---

## 5.4 ScriptDocument

```kotlin
ScriptDocument(
    id: String,
    projectId: String,
    type: ScriptType,
    version: Int,
    title: String,
    segments: List<ScriptSegment>,
    tags: List<String>,
    providerInfo: AiGenerationInfo?,
    status: ScriptStatus,
    createdAt: Long,
    updatedAt: Long
)
```

`ScriptSegment` 至少包含：

* 段落 ID。
* 文本。
* 段落类型。
* 是否锁定。
* 推荐时长。
* 关联素材 ID。
* 排序位置。

禁止继续把文案和媒体路径编码进同一个字符串字段。

---

## 5.5 ProcessingTask

```kotlin
ProcessingTask(
    id: String,
    projectId: String?,
    type: ProcessingTaskType,
    status: ProcessingTaskStatus,
    stage: String?,
    progress: Int,
    configVersion: Int,
    configJson: String,
    outputAssetId: String?,
    errorCode: String?,
    errorMessage: String?,
    retryCount: Int,
    createdAt: Long,
    startedAt: Long?,
    finishedAt: Long?
)
```

任务类型：

```text
ASSET_IMPORT
GOODS_VIDEO_DOWNLOAD
TTS
ASR
AB_RECOMPOSE
VIDEO_DEDUP
VIDEO_RENDER
IMAGE_RENDER
EXPORT
QUALITY_CHECK
```

任务状态：

```text
QUEUED
PREPARING
RUNNING
RETRY_WAIT
SUCCEEDED
FAILED
CANCELLED
```

Worker 参数只允许传递 `taskId`，不得传递完整处理配置和大型文件列表。

---

## 5.6 Publication

```kotlin
Publication(
    id: String,
    projectId: String,
    outputAssetId: String,
    platform: PublishPlatform,
    status: PublicationStatus,
    accountLabel: String?,
    title: String,
    body: String?,
    tags: List<String>,
    goodsLink: String?,
    launchedAt: Long?,
    confirmedAt: Long?,
    externalUrl: String?,
    failureReason: String?
)
```

状态：

```text
DRAFT
READY
SHARE_LAUNCHED
USER_CONFIRMED_PUBLISHED
USER_CONFIRMED_FAILED
CANCELLED
```

Android Share Intent 只能证明“已调起平台”，不能证明“已经发布成功”。因此不得在调起平台后自动写成 `PUBLISHED`。

---

# 六、数据库目标

第一阶段结束后至少具有：

```text
projects
goods_cache
goods_snapshots
assets
project_assets
script_documents
processing_tasks
publications
```

后续增加：

```text
templates
brand_profiles
publish_metrics
content_experiments
team_members
```

数据库要求：

1. 启用 Schema 导出。
2. 删除正式版本中的 `fallbackToDestructiveMigration()`。
3. 每次改表必须提供明确 Migration。
4. 所有主键统一为 UUID 字符串。
5. Repository 内完成 Entity 与 Domain 映射。
6. 跨表保存使用 Room Transaction。
7. 删除项优先软删除或进入回收站。
8. 不允许把复杂列表继续用自定义分隔符拼进正文。
9. JSON 配置必须带 `configVersion`。
10. 数据迁移必须有自动化测试。

---

# 七、文件存储架构

建议项目目录：

```text
files/
└── projects/
    └── {projectId}/
        ├── source/
        ├── audio/
        ├── subtitle/
        ├── image/
        ├── generated/
        └── metadata/

cache/
└── jobs/
    └── {taskId}/
        ├── input/
        ├── temp/
        └── output.tmp

MediaStore/
└── VideoWorkshop/
    ├── Videos/
    └── Images/
```

写入流程：

```text
写入任务临时目录
  ↓
FFprobe 或图片解码验证
  ↓
计算文件元数据和校验值
  ↓
原子移动到项目 generated 目录
  ↓
数据库事务写入 Asset 和项目关系
  ↓
按需导出到 MediaStore
```

失败或取消时：

* 删除 `.tmp`。
* 删除未入库的中间文件。
* 已存在的源素材不得误删。
* 数据库任务保留失败原因。
* App 重启后执行孤儿文件清理。

---

# 八、任务执行架构

所有超过数秒、涉及网络或媒体处理的操作必须进入统一任务队列。

```text
UI 创建任务
  ↓
数据库插入 QUEUED
  ↓
WorkManager 入队，仅传 taskId
  ↓
Worker 从数据库读取任务配置
  ↓
状态更新为 PREPARING / RUNNING
  ↓
执行下载、AI 或 FFmpeg
  ↓
持续写入任务进度
  ↓
验证输出
  ↓
登记输出 Asset
  ↓
任务 SUCCEEDED
```

要求：

* FFmpeg 必须支持任务级取消，不能继续使用只能全局取消的单例语义。
* 长时间媒体处理使用前台 Worker 通知。
* 相同项目、相同配置应支持唯一任务约束。
* App 退出后任务继续。
* App 重启后队列可恢复。
* Worker 不直接更新 UI。
* UI 只观察数据库任务状态。
* 网络任务设置网络约束。
* 存储不足在任务执行前检查。
* 失败任务可重试。
* 不允许 ViewModel 自己持有一个无法恢复的长任务。

现有 `VideoProcessService` 不应与 WorkManager 并行形成两套任务系统。第一阶段统一到 WorkManager 前台任务体系。

---

# 九、AI 服务架构

统一定义三个独立端口：

```kotlin
interface LlmProvider
interface TtsProvider
interface AsrProvider
```

不要继续让一个巨大 `AiRepository` 隐藏所有差异。

## LLM 返回

必须返回经过校验的领域对象：

* ScriptDocument。
* CopyCandidate。
* TitleAndTags。
* SellingPointResult。
* ComplianceResult。

禁止由 UI 自己解析模型 JSON。

## TTS 返回

```kotlin
VoiceSynthesisResult(
    audioAsset: Asset,
    durationMs: Long,
    provider: String,
    voiceId: String
)
```

## ASR 返回

不再只返回 SRT 字符串：

```kotlin
Transcript(
    language: String?,
    text: String,
    segments: List<TranscriptSegment>
)
```

每个 Segment 包含：

* 开始时间。
* 结束时间。
* 文本。
* 置信度，可空。

SRT 是 Transcript 的一种导出格式，不是核心领域模型。

## 凭证

第一阶段采用 BYOK：

* DeepSeek 或兼容 LLM。
* Azure Speech Key 和 Region。
* Groq Key。
* 联盟平台凭证。

所有 Secret 使用 Keystore 加密保存。

未来托管模式中：

* 用户只授权。
* 平台 App Secret 只存在服务端。
* Android 客户端不得内置后台 Secret。

---

# 十、渲染架构

## 10.1 VideoRecipe

视频页面不得直接拼接 FFmpeg 参数，而是生成：

```kotlin
VideoRecipe(
    projectId: String,
    sourceVideoAssetId: String,
    voiceAssetId: String?,
    subtitleAssetId: String?,
    bgmAssetId: String?,
    stickerLayers: List<StickerLayer>,
    audioMode: AudioMode,
    outputProfile: OutputProfile
)
```

然后：

```text
VideoRecipe
  ↓
RenderPlanBuilder
  ↓
RenderStep 列表
  ↓
FFmpeg 执行
  ↓
输出校验
  ↓
Asset 入库
```

## 10.2 ImageComposition

图文成片采用独立、可序列化的画布模型：

```kotlin
ImageComposition(
    canvasSize: CanvasSize,
    background: Background,
    layers: List<ImageLayer>,
    templateId: String?,
    outputFormat: ImageOutputFormat
)
```

图文导出必须通过确定性 Canvas/Bitmap Renderer 完成，不能只截取 Compose 页面截图。

---

# 十一、导航架构

根导航只传业务 ID：

```text
project/{projectId}
goods/select/{projectId}
material/select/{projectId}/{role}
video/edit/{projectId}
image/edit/{projectId}
tasks/{projectId}
publish/{projectId}/{assetId}
```

禁止：

```text
dedup/{完整文件路径}
publish/{完整文件路径}/{商品ID}
enhance/{完整文件路径}/{商品ID}
```

页面通过 `projectId`、`assetId` 从 Repository 恢复状态。

这样可避免：

* 路径编码错误。
* 路由过长。
* 进程重建后状态丢失。
* 文件移动后路由失效。
* 页面间重复传输大量数据。

迁移期间可以保留旧路由兼容层，但新增功能不得继续使用文件路径路由。

---

# 十二、完整产品模块规划

## 第一阶段：本地创作闭环

* 项目中心。
* 素材中心。
* 统一任务队列。
* AI 服务中心。
* 商品与联盟中心。
* 视频生产中心。
* 图文生产中心。
* 发布与发布记录中心。

## 第二阶段：生产效率

* 商品链接一键成片。
* 基础时间线编辑。
* 长视频与直播切片。
* 模板与品牌中心。
* 内容日历。
* 多平台适配。
* 发布质量报告。
* 数据录入与复盘。
* A/B 内容实验。

## 第三阶段：商业化和云能力

* 用户系统。
* 云同步。
* 团队协作。
* 平台 OAuth 和官方直发。
* 数据自动同步。
* 数字人。
* 声音克隆。
* 多语言翻译。
* 云端渲染。
* 套餐和额度系统。

---

# 十三、第一阶段边界

第一阶段完成后，用户必须能够：

1. 创建一个创作项目。
2. 关联联盟商品或手动商品。
3. 导入、下载和管理素材。
4. 生成并编辑带货脚本。
5. 生成配音。
6. 生成真实时间轴字幕。
7. 输出一条真正使用配音和字幕的视频。
8. 输出一组真实图文图片。
9. 所有长任务进入统一队列。
10. App 重启后项目和任务仍能恢复。
11. 调起目标平台发布。
12. 用户确认后形成真实发布记录。

第一阶段不包含：

* 数字人。
* 声音克隆。
* 直播切片。
* 完整时间线。
* 云同步。
* 团队协作。
* 平台官方直发。
* 播放和成交数据自动同步。
* PC、Web、iOS 客户端。

---

# 十四、第一阶段批次规划

第一阶段划分为 **8 个开发批次**。

| 批次    | 大模块       | 核心产物                        |
| ----- | --------- | --------------------------- |
| P1-01 | 项目中心与数据基础 | Project 聚合、项目页、数据库迁移        |
| P1-02 | 素材中心      | Asset 模型、稳定导入、项目素材关系        |
| P1-03 | 统一任务队列    | ProcessingTask、Worker、恢复与取消 |
| P1-04 | AI 服务中心   | 安全凭证、LLM/TTS/ASR 统一适配       |
| P1-05 | 商品与联盟中心   | 真实适配器、商品快照、手动商品             |
| P1-06 | 视频生产中心    | 脚本、配音、字幕、包装、成片              |
| P1-07 | 图文生产中心    | 真实画布、多页渲染、图片导出              |
| P1-08 | 发布与历史中心   | 发布工作台、状态确认、真实历史             |

必须按顺序执行。前一批总验收未通过，不得开始下一批。

---

# 十五、全局开发执行规则

以下规则适用于全部八批：

1. 开始前阅读相关现有源码，不根据文件名猜实现。
2. 先列出现有资产、复用点和需要废弃的占位代码。
3. 不允许一次跨两个大模块开发。
4. 不因当前接口不完整而使用假成功结果。
5. Mock 只能存在于 Debug 或测试环境。
6. Release 不得静默回退 Mock 商品或占位文案。
7. 不得提交任何真实 API Key。
8. 不得将 Secret 输出到日志。
9. 新数据库结构必须有 Migration 和迁移测试。
10. 新增异步流程必须可取消、可恢复、可观察。
11. 页面只依赖 UseCase 或领域 Repository。
12. 不允许 Composable 直接执行文件、数据库或网络操作。
13. 路由只传 ID，不新增文件路径路由。
14. 失败必须提供用户可理解的错误和可诊断错误码。
15. 每批必须补齐单元测试和关键 UI 冒烟测试。
16. 每批验收运行：

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

涉及数据库时额外运行 Migration 测试；涉及关键流程时运行对应 Android 冒烟测试。

17. 每批交付一份：

```text
docs/phase1/P1-XX-implementation-report.md
```

内容必须包含：

* 完成项。
* 修改文件。
* 数据迁移。
* 新增测试。
* 验收结果。
* 未完成项。
* 已知风险。
* 下一批允许依赖的稳定接口。

---

# 十六、第 1 批开发指令：项目中心与数据基础

## 批次编号

```text
P1-01
```

## 唯一目标

建立项目制数据模型，使商品、素材、脚本、任务、成片和发布记录后续都能归属到一个 `Project`。

## 允许改动

* `domain`
* `core-database`
* `data-project` 新模块
* `feature-project` 新模块
* `feature-home`
* `app` 导航和 DI
* 与数据库迁移直接相关的测试

## 主要任务

1. 新增 `Project`、`ProjectType`、`ProjectStatus`。
2. 新增 `GoodsSnapshot` 基础模型。
3. 新增 `ProjectEntity` 和 `GoodsSnapshotEntity`。
4. 新增 Project DAO。
5. 新增 `ProjectRepository`。
6. 新增以下 UseCase：

   * `CreateProjectUseCase`
   * `ObserveRecentProjectsUseCase`
   * `GetProjectUseCase`
   * `UpdateProjectUseCase`
   * `ArchiveProjectUseCase`
7. 新建 `data:data-project`。
8. 新建 `feature:feature-project`。
9. 实现：

   * 新建项目。
   * 项目列表。
   * 项目详情壳。
   * 重命名。
   * 归档。
   * 删除确认。
10. 首页“最近草稿”改为“最近项目”。
11. 新增标准路由：

```text
project/{projectId}
project/create/{projectType}
```

12. 数据库升级并提供明确 Migration。
13. 开始统一新模型 ID 为 UUID String。
14. 保留旧功能兼容，不要求本批迁移全部编辑页面。

## 禁止事项

* 不开发素材导入。
* 不开发任务队列。
* 不接联盟 API。
* 不开发视频渲染。
* 不开发图文渲染。
* 不重写现有 AB 和去重算法。
* 不删除旧 Draft 数据，除非有明确迁移方案。

## 验收标准

* 能创建五种类型中的第一阶段项目类型。
* App 重启后项目仍存在。
* 首页显示最近项目。
* 项目可以重命名和归档。
* 同一项目 ID 可通过路由恢复详情。
* 数据库升级不依赖 destructive migration。
* Migration 测试通过。
* 不再新增 Long/String 双 ID 转换代码。
* 旧首页和现有功能无新增闪退。

---

# 十七、第 2 批开发指令：素材中心

## 批次编号

```text
P1-02
```

## 唯一目标

完成统一 Asset 模型和可靠素材导入，使素材能够被项目稳定引用。

## 允许改动

* `domain/asset`
* `core-media`
* `core-database`
* `data-material`
* `feature-material`
* `feature-project`
* 素材相关导航和测试

## 主要任务

1. 新增 `Asset` 领域模型。
2. 新增 `AssetEntity`。
3. 新增 `ProjectAssetCrossRef`。
4. 将旧 materials 数据迁移到 assets。
5. 统一素材 ID 为 UUID String。
6. 导入流程改为：

   * 复制到临时文件。
   * 获取媒体信息。
   * 生成缩略图。
   * 计算文件校验值。
   * 原子移动到正式目录。
   * 数据库事务入库。
7. 支持视频、图片、音频和字幕类型。
8. 支持素材与项目建立角色关系。
9. 素材详情显示：

   * 名称。
   * 类型。
   * 来源。
   * 大小。
   * 时长。
   * 分辨率。
   * 创建时间。
10. 实现预览、标签、备注、批量删除。
11. 删除内部素材时同步清理受控文件。
12. 删除前检查项目引用。
13. 素材缺失时标记 `MISSING`，不得直接崩溃。
14. 现有 AB、去重、包装入口通过 Asset ID 获取路径。

## 禁止事项

* 不开发任务队列 UI。
* 不处理商品真实 API。
* 不开发 AI。
* 不开发图文画布。
* 不增加云存储。

## 验收标准

* 视频、图片和音频可以稳定导入。
* 重启后素材可访问。
* 素材可以关联到指定项目。
* 同一素材可以关联多个项目。
* 100 条素材列表可正常加载和筛选。
* content URI 不直接作为长期处理输入。
* 删除被引用素材时有明确提示。
* 不产生数据库记录存在但文件不存在的假成功结果。

---

# 十八、第 3 批开发指令：统一任务队列

## 批次编号

```text
P1-03
```

## 唯一目标

将下载、AB、去重、增强和后续 AI/渲染统一到可恢复任务基础设施。

## 允许改动

* `domain/task`
* `core-database`
* `core-ffmpeg`
* `data-task`
* `feature-taskqueue`
* WorkManager 配置
* 现有长任务调用入口

## 主要任务

1. 新增统一 `ProcessingTask` 模型。
2. 新增 ProcessingTask DAO 和 Repository。
3. 新增 Worker 注册与 Task Handler Registry。
4. Worker 只接收 taskId。
5. 任务配置从数据库读取。
6. 首批接入：

   * 商品视频下载。
   * AB 音画重组。
   * 视频原创加工。
   * 视频增强。
7. 新增任务队列页面：

   * 等待。
   * 运行中。
   * 成功。
   * 失败。
   * 已取消。
8. 支持取消。
9. 支持失败重试。
10. 支持 App 重启后恢复状态。
11. 支持前台通知。
12. 为 FFmpeg 增加任务级 Session 或 CancelHandle。
13. 禁止继续使用会误取消其他任务的全局取消。
14. 输出采用临时文件、校验、原子转正。
15. 任务成功后输出自动登记为 Asset。
16. 处理旧 `VideoProcessService`：

* 迁移到 WorkManager 前台执行。
* 或删除。
* 不保留两套并行机制。

## 禁止事项

* 不开发 AI 业务。
* 不接真实联盟搜索。
* 不开发新视频编辑功能。
* 不允许一个任务失败后自动标记成功。

## 验收标准

* 四类现有长任务均进入队列。
* App 退到后台后任务继续。
* App 重启后能看到任务历史。
* 取消只影响指定任务。
* 失败任务能显示原因并重试。
* 任务成功后有可播放输出 Asset。
* 失败和取消不保留残缺正式文件。
* 相同任务不会被重复提交。

---

# 十九、第 4 批开发指令：AI 服务中心

## 批次编号

```text
P1-04
```

## 唯一目标

建立安全、可测试、可替换的 LLM、TTS 和 ASR 服务体系。

## 允许改动

* `core-security`
* `core-datastore`
* `data-ai`
* `domain/ai`
* `feature-settings`
* AI 测试

## 主要任务

1. 新增 `core-security`。
2. 将 API Secret 从普通 DataStore 迁移到 Keystore 加密存储。
3. DataStore 只保留服务商和非敏感配置。
4. 配置字段至少包含：

   * LLM Provider。
   * LLM API Key。
   * Base URL，可选。
   * Model，可选。
   * Azure Speech Key。
   * Azure Region。
   * Groq Key。
5. 定义：

   * `LlmProvider`
   * `TtsProvider`
   * `AsrProvider`
6. AI 返回结构化领域对象。
7. ASR 返回真实时间段 Segment。
8. TTS 输出登记为 Audio Asset。
9. 增加独立“测试连接”。
10. 每项服务显示：

    * 未配置。
    * 测试中。
    * 可用。
    * 认证失败。
    * 网络失败。
11. 错误日志必须脱敏。
12. 不允许生产环境因 AI 失败自动生成虚假占位文案并继续发布。
13. JSON 解析失败可执行有限次数的格式修复重试。
14. 为 AI 调用增加超时、取消和错误映射。
15. 为相同输入增加结果缓存键设计。

## 禁止事项

* 不开发完整视频成片。
* 不接数字人。
* 不接声音克隆。
* 不把 API Key 写入 BuildConfig。
* 不在日志中输出完整请求授权头。

## 验收标准

* 三项服务都能独立配置和测试。
* Azure Region 不再硬编码。
* Secret 不以明文存在普通 Preferences 中。
* ASR 能返回带时间轴的 Transcript。
* TTS 输出能作为 Asset 读取。
* LLM 返回结构化脚本候选。
* 无配置时给出明确引导，不闪退、不假成功。

---

# 二十、第 5 批开发指令：商品与联盟中心

## 批次编号

```text
P1-05
```

## 唯一目标

形成真实商品获取、手动商品和项目商品快照闭环。

## 允许改动

* `domain/goods`
* `data-alliance`
* `core-network`
* `core-database`
* `feature-goods`
* `feature-project`
* 联盟 API 测试

## 主要任务

1. 区分：

   * 联盟搜索缓存 Goods。
   * 项目锁定 GoodsSnapshot。
2. 支持手动创建商品。
3. 完成链接解析器。
4. 淘宝、京东、拼多多使用独立 Provider Adapter。
5. 每个 Provider 统一输出：

   * 搜索结果。
   * 商品详情。
   * 商品图片。
   * 商品视频。
   * 推广链接。
   * 佣金信息，可获得时。
6. 未配置凭证时显示“未连接”，不得返回 Mock 冒充真实结果。
7. Mock 只能在 Debug 和测试中启用。
8. 选择商品进入项目时创建不可变快照。
9. 支持修改项目内卖点，但不篡改原搜索缓存。
10. 支持商品视频下载进入任务队列。
11. 外部 API 响应必须经过 DTO、Mapper、Domain 三层转换。
12. 每个 Provider 使用 MockWebServer 或契约样本测试。
13. 实际接口字段不确定时，必须查阅官方文档，不得猜接口和签名算法。
14. 未提供真实测试凭证时，只能验收请求签名、响应解析和模拟契约，不得声称线上调用已通过。

## 禁止事项

* 不把任何真实联盟 Secret 提交到仓库。
* 不让 ViewModel 直接签名请求。
* 不用一个 Provider 的字段模型强行套三个平台。
* 不开发订单和成交数据同步。

## 验收标准

* 可手动创建商品并绑定项目。
* 合法商品链接可解析。
* 无效链接不闪退。
* 已配置平台可执行真实请求。
* 未配置平台不显示假商品。
* 商品进入项目后形成快照。
* 商品视频可下载并登记为 Asset。
* Release 构建中 Mock 默认关闭。

---

# 二十一、第 6 批开发指令：视频生产中心

## 批次编号

```text
P1-06
```

## 唯一目标

真正完成“素材 + 脚本 + 配音 + 字幕 + 包装 → 视频成片”。

## 允许改动

* `domain/script`
* `domain/render`
* `data-render`
* `data-ai`
* `core-ffmpeg`
* `feature-videoenhance`
* `feature-abtransport`
* `feature-dedup`
* 项目详情中的视频工作流

## 主要任务

1. 新增 ScriptDocument 和 ScriptSegment。
2. 支持生成多版带货脚本。
3. 支持标题、正文、卖点和标签编辑。
4. 支持锁定段落。
5. 生成配音并将 `voiceAssetId` 写入项目。
6. 对配音或原始语音执行 ASR，获得时间轴字幕。
7. 生成 SRT Asset。
8. 建立 `VideoRecipe`。
9. 建立 `RenderPlanBuilder`。
10. 渲染步骤至少支持：

    * 原视频。
    * 配音替换或混音。
    * 背景音乐。
    * 人声出现时降低 BGM。
    * 时间轴字幕。
    * 真实贴纸资源。
11. 当前已生成但未进入成片的配音必须真正使用。
12. 当前整段文案伪字幕必须替换为真实时间轴字幕。
13. AB 音画重组和原创加工结果统一登记为项目 Asset。
14. 渲染任务必须进入统一任务队列。
15. 输出后执行：

    * 文件存在检查。
    * 可探测检查。
    * 视频流检查。
    * 音频流检查。
    * 时长检查。
16. 生成成功后项目状态变为 `READY_TO_PUBLISH`。
17. 失败不得覆盖上一个有效输出。
18. 内置 BGM 和贴纸必须具备真实文件映射和来源信息。

## 禁止事项

* 不开发完整多轨时间线。
* 不开发数字人。
* 不开发复杂运动跟踪。
* 不重新实现 FFmpeg 底层已有能力。
* 不把 ViewModel 状态当作项目唯一保存位置。

## 验收标准

* 选择视频素材后能生成脚本。
* 能生成并试听配音。
* 最终视频确实使用生成配音。
* 字幕与语音具有实际时间轴。
* BGM 和贴纸确实进入最终视频。
* 退出页面后可从项目恢复。
* App 重启后可继续查看任务和输出。
* 输出视频可播放、包含正确音视频轨。
* 能直接进入发布工作台。

---

# 二十二、第 7 批开发指令：图文生产中心

## 批次编号

```text
P1-07
```

## 唯一目标

把当前图文页面从界面和文案外壳补成可保存、可恢复、可导出的真实多页图文工具。

## 允许改动

* `domain/image`
* `data-render`
* `core-media`
* `feature-imageeditor`
* 项目与 Asset 关联
* 图片渲染测试

## 主要任务

1. 新增可序列化 `ImageComposition`。
2. 支持多页面。
3. 支持模板：

   * 商品推荐。
   * 测评打分。
   * 清单合集。
   * 促销秒杀。
4. 支持图层：

   * 图片。
   * 文本。
   * 商品名称。
   * 价格。
   * 卖点。
   * Logo。
   * 贴纸。
5. 支持：

   * 移动。
   * 缩放。
   * 旋转。
   * 裁剪。
   * 图层排序。
   * 字体、字号、颜色和对齐。
6. 编辑状态保存到项目。
7. 使用确定性 Canvas/Bitmap Renderer 导出。
8. 不使用 Compose 页面截图充当正式导出。
9. 支持 JPG/PNG。
10. 支持平台尺寸预设。
11. 每页导出为独立 Asset。
12. 封面可单独标记。
13. 导出任务进入统一任务队列。
14. 导出失败时不登记空文件。
15. 加入基本内存控制，避免一次加载所有原图大 Bitmap。

## 禁止事项

* 不做专业 Photoshop。
* 不做云模板市场。
* 不做 AI 抠图，除非已有稳定本地能力。
* 不开发社交分享社区。

## 验收标准

* 可以创建多页图文。
* 可以编辑真实图层。
* 重启后继续编辑。
* 能导出清晰图片组。
* 导出结果与编辑预览主要布局一致。
* 输出图片登记为项目 Asset。
* 可以选择封面并进入发布工作台。

---

# 二十三、第 8 批开发指令：发布与历史中心

## 批次编号

```text
P1-08
```

## 唯一目标

建立正确的发布状态、发布确认和历史记录，完成第一阶段端到端闭环。

## 允许改动

* `domain/publish`
* `data-publish`
* `core-database`
* `feature-publish`
* `feature-history`
* 项目状态联动
* 发布流程测试

## 主要任务

1. 新增 Publication Entity、DAO、Repository。
2. 发布前创建 DRAFT 记录。
3. 用户选择目标平台。
4. 每个平台保存独立：

   * 标题。
   * 正文。
   * 标签。
   * 商品链接。
   * 封面。
5. 发布前检查：

   * 输出文件存在。
   * 文件可读。
   * 标题非空。
   * 商品链接格式。
   * 平台应用是否安装。
6. 调起 Android Share 后写入 `SHARE_LAUNCHED`。
7. 用户返回应用后询问：

   * 已发布。
   * 发布失败。
   * 稍后确认。
8. 只有用户确认后才写入 `USER_CONFIRMED_PUBLISHED`。
9. 历史页面必须读取 Publication，不得继续把草稿伪装成已发布内容。
10. 历史记录支持：

    * 查看详情。
    * 打开成片。
    * 复制文案。
    * 再次调起发布。
    * 记录平台作品链接。
11. 发布成功后更新项目状态。
12. 重新发布复用项目和 Asset，不使用 Draft ID 冒充文件路径。
13. 未安装平台时提供：

    * 保存到相册。
    * 复制文案。
    * 安装提示。
14. 不接模拟点击和无人值守批量发布。

## 禁止事项

* 不声称 Share Intent 等于平台发布成功。
* 不保存虚假播放量。
* 不接平台 App Secret。
* 不开发平台官方直发。
* 不开发数据自动同步。

## 验收标准

* 视频和图文均可进入发布工作台。
* 能调起已安装的平台。
* 发布状态语义正确。
* 用户可确认成功或失败。
* 发布历史来自真实 Publication 表。
* 重启后记录仍在。
* 可以查看和重新发布历史内容。
* 项目、输出 Asset 和 Publication 关系完整。

---

# 二十四、第一阶段总验收

八批全部通过后，执行完整验收。

## 必测主链路 A：视频带货

```text
创建视频带货项目
→ 选择或手动创建商品
→ 导入商品视频
→ 生成脚本
→ 生成配音
→ 生成时间轴字幕
→ 添加 BGM
→ 视频成片
→ 发布检查
→ 调起平台
→ 用户确认发布
→ 历史记录可查看
```

## 必测主链路 B：AB 音画重组

```text
创建 AB 项目
→ 选择 A/B 素材
→ 后台合成
→ 原创加工
→ 配音或字幕包装
→ 输出
→ 发布
```

## 必测主链路 C：图文带货

```text
创建图文项目
→ 绑定商品
→ 导入图片
→ 生成文案
→ 编辑多页画布
→ 导出图片组
→ 发布
→ 保存记录
```

## 恢复性测试

* 编辑中杀死 App。
* 任务运行中杀死 App。
* 网络中断。
* 存储不足。
* API Key 失效。
* 原始文件被删除。
* FFmpeg 失败。
* 用户取消任务。
* 数据库从旧版本升级。

## 第一阶段发布门槛

必须全部满足：

* 不使用 destructive migration。
* Release 默认无 Mock 商品。
* Release 不含真实密钥。
* 所有长任务可恢复。
* 配音真正进入成片。
* 字幕具备真实时间轴。
* 图文能够真实导出。
* 发布历史不再使用草稿伪装。
* 三条端到端链路通过。
* 单元测试、Lint 和 Debug 构建通过。
* 没有 P0、P1 阻断问题。

第一阶段完成后的版本可以定义为：

```text
VideoWorkshop v3.0 Local Production MVP
```
