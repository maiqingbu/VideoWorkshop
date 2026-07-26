# Tasks

## 阶段一：基础设施层（core-ffmpeg 扩展）

- [x] Task 1: 新增 MultiInputCommandBuilder 多输入命令构建器
  - [x] SubTask 1.1: 创建 `core/core-ffmpeg/src/main/kotlin/com/videoworkshop/core/ffmpeg/pipeline/MultiInputCommandBuilder.kt`
  - [x] SubTask 1.2: 支持多 `-i` 输入 + 多 `-map` 流选择
  - [x] SubTask 1.3: 编写单元测试 `MultiInputCommandBuilderTest.kt`

- [x] Task 2: 新增 AVSwapCommandBuilder AB 音轨替换命令构建器
  - [x] SubTask 2.1: 创建 `pipeline/AVSwapCommandBuilder.kt`
  - [x] SubTask 2.2: 支持纯替换模式 `-map 0:v:0 -map 1:a:0`
  - [x] SubTask 2.3: 支持混合模式 `amix` 滤镜
  - [x] SubTask 2.4: 编写单元测试 `AVSwapCommandBuilderTest.kt`

- [x] Task 3: 新增 AVStreamSwapper 音视频流分离重组器
  - [x] SubTask 3.1: 创建 `operators/AVStreamSwapper.kt`
  - [x] SubTask 3.2: 调用 FfmpegEngine 执行命令
  - [x] SubTask 3.3: 支持进度回调与取消
  - [x] SubTask 3.4: 编写单元测试 `AVStreamSwapperTest.kt`

- [x] Task 4: 新增 TimelineAligner 时间轴对齐器
  - [x] SubTask 4.1: 创建 `operators/TimelineAligner.kt`
  - [x] SubTask 4.2: 实现截断策略（min(A, B)）
  - [x] SubTask 4.3: 实现循环策略（max(A, B)，使用 `loop`/`stream_loop`）
  - [x] SubTask 4.4: 实现自定义起止点（`atrim`/`trim` + `setpts`）
  - [x] SubTask 4.5: 编写单元测试 `TimelineAlignerTest.kt`

- [x] Task 5: 新增 CrossVideoAudioMixer 跨视频音频混合器
  - [x] SubTask 5.1: 创建 `operators/CrossVideoAudioMixer.kt`（扩展现 AudioMixer）
  - [x] SubTask 5.2: 从 B 视频抽取原声 + 与 A 音频按比例混合
  - [x] SubTask 5.3: 编写单元测试 `CrossVideoAudioMixerTest.kt`

## 阶段二：领域层扩展（domain）

- [x] Task 6: 新增 AB 搬运相关 Model
  - [x] SubTask 6.1: 创建 `domain/model/ABTransportConfig.kt`（A/B 路径、模式、时长策略、音量比例、起止点）
  - [x] SubTask 6.2: 创建 `domain/model/ABTransportResult.kt`（产物路径、时长、缩略图）
  - [x] SubTask 6.3: 创建 `domain/model/AudioTrack.kt`（音轨元信息）
  - [x] SubTask 6.4: 创建 `domain/model/TimelineSegment.kt`（起止时间片段）

- [x] Task 7: 扩展 DedupRepository 接口与实现
  - [x] SubTask 7.1: 在 `domain/repository/DedupRepository.kt` 新增 `abTransport(config: ABTransportConfig): Flow<TaskProgress>`
  - [x] SubTask 7.2: 在 `data/data-repository/DedupRepositoryImpl.kt` 实现 abTransport
  - [x] SubTask 7.3: 调用 AVStreamSwapper/CrossVideoAudioMixer/TimelineAligner

- [x] Task 8: 新增 ABTransportUseCase
  - [x] SubTask 8.1: 创建 `domain/usecase/ABTransportUseCase.kt`
  - [x] SubTask 8.2: 编写单元测试 `ABTransportUseCaseTest.kt`

## 阶段三：Bug 修复（P0 优先）

- [x] Task 9: 修复路由 import/video 闪退（FIX-01）
  - [x] SubTask 9.1: 修改 `feature/feature-goods/GoodsRoute.kt`，`import/video` → `material`
  - [x] SubTask 9.2: 修改 `import/image` → `material`（统一进素材库选素材）

- [x] Task 10: 修复链接解析闪退（FIX-02）
  - [x] SubTask 10.1: 在 `GoodsViewModel` 新增 `parseManualLink(link: String): String?` 正则提取商品 ID
  - [x] SubTask 10.2: 支持淘宝 `id=(\d+)`、京东 `goods_id=(\d+)`、拼多多 `goods_id=(\d+)` 三种 URL
  - [x] SubTask 10.3: 所有路由参数使用 `Uri.encode`（GoodsRoute、EnhanceRoute 等）
  - [x] SubTask 10.4: 非法链接显示 Snackbar 提示「链接格式无法识别」

- [x] Task 11: 修复 GoodsScreen UI 差异化（FIX-04）
  - [x] SubTask 11.1: 标题根据 mode 显示「选择视频带货商品」/「选择图文带货商品」
  - [x] SubTask 11.2: 默认平台 Tab 按 mode 差异化（VIDEO 默认淘宝，IMAGE 默认拼多多）
  - [x] SubTask 11.3: 商品库入口跳独立 `goods-library` 路由（暂复用 GoodsScreen，标题「商品库」）

- [x] Task 12: Mock 数据扩量（FIX-05）
  - [x] SubTask 12.1: 修改 `data/data-alliance/mock/MockGoodsData.kt`
  - [x] SubTask 12.2: 每平台至少 10 条，覆盖服饰/数码/家居/美妆/食品等品类

## 阶段四：素材库升级

- [x] Task 13: 新增 media3 依赖
  - [x] SubTask 13.1: 在 `gradle/libs.versions.toml` 新增 media3 版本与库声明
  - [x] SubTask 13.2: 在 `feature/feature-material/build.gradle.kts` 引入 media3-exoplayer、media3-ui

- [x] Task 14: 新增 VideoPreviewDialog 视频播放器
  - [x] SubTask 14.1: 创建 `feature/feature-material/.../VideoPreviewDialog.kt`
  - [x] SubTask 14.2: 使用 ExoPlayer + PlayerView，支持播放/暂停/进度拖动
  - [x] SubTask 14.3: 释放资源避免内存泄漏

- [x] Task 15: 新增 MaterialActionSheet 操作菜单
  - [x] SubTask 15.1: 创建 `MaterialActionSheet.kt` BottomSheet
  - [x] SubTask 15.2: 菜单项：预览/去重/AB 搬运/制作带货视频/编辑/重命名/删除
  - [x] SubTask 15.3: 各菜单项跳转目标路由

- [x] Task 16: 新增 MaterialEditDialog 素材编辑对话框
  - [x] SubTask 16.1: 创建 `MaterialEditDialog.kt`
  - [x] SubTask 16.2: 支持修改素材名称、标签、备注字段
  - [x] SubTask 16.3: 在 MaterialEntity 新增 `tags`、`note` 字段（Room migration）

- [x] Task 17: 修复素材库 URI 持久化与文件大小（MAT-05/06/07）
  - [x] SubTask 17.1: `MaterialScreen.kt` 导入回调调用 `takePersistableUriPermission`
  - [x] SubTask 17.2: content:// URI 复制到 App 私有目录，存储本地路径
  - [x] SubTask 17.3: 修复 `sizeText()` 对 content:// 返回 0 的问题

- [x] Task 18: 真实缩略图实现（MAT-03）
  - [x] SubTask 18.1: `MaterialThumb` 对视频类型使用 MediaMetadataRetriever 提取首帧
  - [x] SubTask 18.2: 或引入 Coil 的 VideoFrameDecoder

- [x] Task 19: 改造 MaterialCard 交互
  - [x] SubTask 19.1: `onClick` 弹出 MaterialActionSheet（替代空占位）
  - [x] SubTask 19.2: `onLongClick` 进入多选模式
  - [x] SubTask 19.3: MaterialRoute 暴露 `onDedup(path)`、`onABTransport(path)`、`onEnhance(path)` 回调

- [x] Task 20: 多选模式与标签分类（MAT-08/09）
  - [x] SubTask 20.1: 实现多选状态管理，支持批量删除
  - [x] SubTask 20.2: 顶部标签筛选栏（全部/视频/图片/已处理/未处理）

## 阶段五：AB 搬运 Feature 模块

- [x] Task 21: 新建 feature-abtransport 模块骨架
  - [x] SubTask 21.1: 创建模块目录结构 + `build.gradle.kts`
  - [x] SubTask 21.2: 在 `settings.gradle.kts` 注册 `:feature:feature-abtransport`
  - [x] SubTask 21.3: 创建 ABTransportRoute/Screen/ViewModel 三件套

- [x] Task 22: ABTransportScreen UI 实现
  - [x] SubTask 22.1: A/B 视频选择卡片（支持从素材库/系统文件）
  - [x] SubTask 22.2: 合成模式选择（纯替换/混合）+ 音量比例 Slider
  - [x] SubTask 22.3: 时长策略选择（截断/循环/自定义）+ 时间轴拖动
  - [x] SubTask 22.4: 关键帧预览区域
  - [x] SubTask 22.5: 进度条 + 取消按钮

- [x] Task 23: ABTransportViewModel 实现
  - [x] SubTask 23.1: 状态管理：A/B 路径、模式、策略、进度、错误
  - [x] SubTask 23.2: 调用 ABTransportUseCase 执行合成
  - [x] SubTask 23.3: 关键帧预览：调用 ThumbnailExtractor 提取 3-5 帧
  - [x] SubTask 23.4: 取消合成：取消 UseCase Flow

- [x] Task 24: ABTransportResultScreen 结果页
  - [x] SubTask 24.1: 预览产物视频
  - [x] SubTask 24.2: 三个入口：去重 / AI 包装 / 保存到素材库
  - [x] SubTask 24.3: 「保存到素材库」调用 MaterialRepository 保存

## 阶段六：发布记录 Feature 模块

- [x] Task 25: 新建 feature-history 模块
  - [x] SubTask 25.1: 创建模块目录 + `build.gradle.kts` + 注册到 settings.gradle.kts
  - [x] SubTask 25.2: 创建 HistoryRoute/Screen/ViewModel
  - [x] SubTask 25.3: 从 DraftRepository 读取已发布草稿列表
  - [x] SubTask 25.4: 列表项：标题/平台/时间/状态
  - [x] SubTask 25.5: 详情页 + 「重新发布」按钮

## 阶段七：主页与导航重构

- [x] Task 26: 重构 AppNavHost
  - [x] SubTask 26.1: 包裹 Scaffold + VWBottomBar
  - [x] SubTask 26.2: 注册新路由：abtransport / history / goods-library / settings
  - [x] SubTask 26.3: 删除 import/video 漏注册的常量
  - [x] SubTask 26.4: 路由参数统一 Uri.encode

- [x] Task 27: 重构 HomeScreen
  - [x] SubTask 27.1: 4 主入口卡片：视频带货/图文带货/AB 搬运/二创工厂（灰态）
  - [x] SubTask 27.2: 快捷入口：素材库/发布记录可用，任务队列/设置灰态或隐藏
  - [x] SubTask 27.3: 最近草稿接 DraftRepository，显示最近 5 条
  - [x] SubTask 27.4: 修复 QuickAction.PUBLISH_RECORDS -> Unit 改为跳转 history

- [x] Task 28: 启用 VWBottomBar
  - [x] SubTask 28.1: 4 Tab：首页/素材库/发布记录/我的
  - [x] SubTask 28.2: Tab 切换路由跳转（navigate + popUpTo + saveState + restoreState）
  - [x] SubTask 28.3: 当前 Tab 高亮状态（基于 currentBackStackEntryAsState 路由匹配）

- [x] Task 28B: 新建 feature-settings 模块（我的 Tab）
  - [x] SubTask 28B.1: 创建模块目录结构 + `build.gradle.kts` + 在 `settings.gradle.kts` 注册 `:feature:feature-settings`
  - [x] SubTask 28B.2: 创建 SettingsRoute/Screen/ViewModel 三件套
  - [x] SubTask 28B.3: AI API Key 配置项（DataStore 存储，密文显示，运行时注入 data-ai）
  - [x] SubTask 28B.4: 联盟凭证配置项（淘宝/京东/拼多多 AppKey/AppSecret，未配置时显示「使用 Mock 数据」提示）
  - [x] SubTask 28B.5: 关于页（版本号、构建时间、GitHub 仓库链接）
  - [x] SubTask 28B.6: 清理缓存功能（显示缓存大小，确认后清理 cacheDir 与临时产物目录，不影响素材库已导入文件）
  - [x] SubTask 28B.7: 在 AppNavHost 注册 `settings` 路由，VWBottomBar「我的」Tab 跳转

## 阶段八：测试覆盖

- [x] Task 29: core-ffmpeg 单元测试
  - [x] SubTask 29.1: MultiInputCommandBuilderTest
  - [x] SubTask 29.2: AVSwapCommandBuilderTest
  - [x] SubTask 29.3: AVStreamSwapperTest
  - [x] SubTask 29.4: TimelineAlignerTest
  - [x] SubTask 29.5: CrossVideoAudioMixerTest

- [x] Task 30: domain 单元测试
  - [x] SubTask 30.1: ABTransportUseCaseTest
  - [x] SubTask 30.2: parseManualLink 测试（GoodsViewModel）

- [x] Task 31: UI 冒烟测试
  - [x] SubTask 31.1: AB 搬运全链路场景（代码已编写，待 CI 运行）
  - [x] SubTask 31.2: 素材库工作流场景（代码已编写，待 CI 运行）
  - [x] SubTask 31.3: 商品带货全链路场景（代码已编写，待 CI 运行）
  - [x] SubTask 31.4: 发布记录场景（代码已编写，待 CI 运行）

## 阶段九：集成与发布

- [~] Task 32: 端到端集成测试（待用户真机测试）
  - [~] SubTask 32.1: 场景 A：AB 搬运全链路手动测试（待用户真机测试）
  - [~] SubTask 32.2: 场景 B：素材库工作流手动测试（待用户真机测试）
  - [~] SubTask 32.3: 场景 C：商品带货全链路手动测试（待用户真机测试）
  - [~] SubTask 32.4: 场景 D：发布记录手动测试（待用户真机测试）

- [~] Task 33: 回归测试（待用户真机测试）
  - [~] SubTask 33.1: v1.x 视频去重功能正常（待用户真机测试）
  - [~] SubTask 33.2: v1.x AI 包装功能正常（待用户真机测试）
  - [~] SubTask 33.3: v1.x 图文编辑功能正常（待用户真机测试）
  - [~] SubTask 33.4: v1.x 发布功能正常（待用户真机测试）

- [~] Task 34: 构建与打包
  - [x] SubTask 34.1: 本地 Gradle 构建通过（已修复 SettingsViewModel.kt 的 combine 误用错误，构建成功）
  - [~] SubTask 34.2: GitHub Actions CI 构建通过（待用户推送后 CI 验证）
  - [x] SubTask 34.3: 生成 debug APK（app/build/outputs/apk/debug/app-debug.apk, 92MB）

# Task Dependencies

- Task 2 依赖 Task 1（AVSwapCommandBuilder 依赖 MultiInputCommandBuilder）
- Task 3/4/5 依赖 Task 1/2（operators 依赖 pipeline）
- Task 7 依赖 Task 3/4/5/6（Repository 依赖 operators 和 Model）
- Task 8 依赖 Task 7（UseCase 依赖 Repository）
- Task 21-24 依赖 Task 8（feature 依赖 UseCase）
- Task 26 依赖 Task 21/25（导航依赖新 feature 模块）
- Task 27 依赖 Task 26（主页依赖导航）
- Task 29 依赖 Task 1-5（单测依赖实现）
- Task 31 依赖 Task 21-28（UI 测试依赖所有 feature 完成）
- Task 32 依赖 Task 29/30/31（集成测试依赖单测与 UI 测试）

# 可并行任务

- 阶段一 Task 1-5 内部部分可并行（Task 1 完成后 Task 2/3/4/5 可并行）
- 阶段三 Task 9-12 互相独立，可全部并行
- 阶段四 Task 13/14/15/16/17/18 互相独立，可并行
- 阶段五 Task 21-24 串行
- 阶段六 Task 25 独立于阶段五，可并行
