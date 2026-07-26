# VideoWorkshop v2.0 AB 搬运与素材库升级 Spec

## Why

v1.x 存在 6 个 P0/P1 bug（点商品选择闪退、链接解析闪退、素材无法播放、无操作入口、按钮无交互、UI 重复），且缺失竞品验证过的核心能力「AB 搬运」。本次升级目标是在彻底修复现有问题的前提下，上线 AB 搬运完整能力（纯替换+混合+循环对齐），并补齐素材库工作流，让用户能完整走通「素材库 → AB 搬运 → 去重 → AI 包装 → 发布」全链路。

## What Changes

### 新增功能
- **AB 搬运全能力**：纯音轨替换 + 音轨混合 + 循环对齐 + 自定义起止点 + 关键帧预览 + 可取消
- **素材库升级**：视频播放预览（media3）+ 真实缩略图 + 操作菜单 + **素材编辑修改** + URI 持久化 + 多选模式 + 标签分类
- **主页与导航重构**：底部导航栏 4 Tab（首页/素材库/发布记录/我的）+ 主页入口卡片重构 + 最近草稿接真实数据
- **发布记录页**：新建 feature-history 模块
- **Bug 修复**：5 个已知 bug 全部修复
- **Mock 数据扩量**：每平台至少 10 条

### Bug 修复
- FIX-01：点商品「选择」闪退 → `import/video` 改为 `material`
- FIX-02：输入链接解析闪退 → GoodsViewModel 加 `parseManualLink` + 所有路由参数 `Uri.encode`
- FIX-03：主页「发布记录」无交互 → 新建 feature-history
- FIX-04：商品库与视频带货 UI 重复 → 商品库独立路由 + GoodsScreen 按 mode 差异化
- FIX-05：Mock 数据仅 2 条 → 扩量到 10 条/平台

### 测试覆盖
- 所有新增 core-ffmpeg 模块单测覆盖
- domain 层 UseCase 单测覆盖
- 关键路径 UI 冒烟测试（AB 搬运全链路、素材库工作流、商品带货全链路、发布记录）

## Impact

### Affected Code
- `core/core-ffmpeg/` - 新增 pipeline/MultiInputCommandBuilder、AVSwapCommandBuilder、operators/AVStreamSwapper、TimelineAligner、CrossVideoAudioMixer
- `domain/model/` - 新增 ABTransportConfig、ABTransportResult、AudioTrack、TimelineSegment
- `domain/repository/DedupRepository.kt` - 扩展 abTransport 方法
- `domain/usecase/` - 新增 ABTransportUseCase
- `data/data-repository/` - 扩展 DedupRepositoryImpl
- `feature/feature-abtransport/` - 新建模块
- `feature/feature-history/` - 新建模块
- `feature/feature-material/` - 重构：新增 VideoPreviewDialog、MaterialActionSheet、MaterialEditDialog
- `feature/feature-home/` - 重构主页布局
- `feature/feature-goods/` - 修复路由 + GoodsScreen 差异化 + parseManualLink
- `app/src/main/java/com/videoworkshop/app/nav/AppNavHost.kt` - 重构导航 + 启用 VWBottomBar
- `data/data-alliance/mock/MockGoodsData.kt` - 扩量
- `gradle/libs.versions.toml` - 新增 media3 依赖

### Affected Specs
- 无（首次建立 spec）

## ADDED Requirements

### Requirement: AB 搬运功能
系统 SHALL 提供独立的 AB 搬运功能模块，支持用户选择 A 视频（音频源）和 B 视频（画面源），通过三种合成模式生成新视频。

**素材来源约束**：A 视频与 B 视频**仅从素材库选择**，不支持直接调用系统文件选择器。理由：(1) 与素材库升级形成闭环，避免 content:// URI 权限问题复发；(2) 强制用户先导入素材库再使用，便于产物追溯与统一管理；(3) 减少开发量约 30%，缩短上线周期。

#### Scenario: 素材库选择 A/B 视频
- **WHEN** 用户在 AB 搬运页点击「选择 A 视频」或「选择 B 视频」
- **THEN** 弹出素材库选择器（仅显示视频类型素材），用户选中后返回路径与缩略图
- **WHEN** 用户未从素材库选择
- **THEN** 「开始合成」按钮置灰，提示「请先选择 A/B 视频」

#### Scenario: 纯音轨替换合成
- **WHEN** 用户选择 A 视频 + B 视频 + 合成模式「纯音轨替换」+ 时长策略「截断对齐」
- **THEN** 系统使用 FFmpeg `-map 0:v:0 -map 1:a:0` 生成新视频，画面来自 B，音频来自 A，时长 = min(A, B)

#### Scenario: 音轨混合合成
- **WHEN** 用户选择合成模式「音轨混合」+ 设置 A/B 音量比例
- **THEN** 系统使用 `amix` 滤镜混合 A 音频与 B 原声，按用户设置的比例输出

#### Scenario: 循环对齐
- **WHEN** 用户选择时长策略「循环对齐」
- **THEN** 短视频循环到长者时长，输出时长 = max(A, B)

#### Scenario: 自定义起止点
- **WHEN** 用户通过时间轴拖动设置 A/B 的起止时间
- **THEN** 系统使用 `atrim`/`trim` 滤镜按自定义时间窗口合成

#### Scenario: 关键帧预览
- **WHEN** 用户点击「预览」按钮
- **THEN** 系统提取 3-5 帧合成效果（不渲染全片），1 秒内显示

#### Scenario: 取消合成
- **WHEN** 用户在合成过程中点击「取消」
- **THEN** 系统立即停止 FFmpeg 进程，删除部分产物，无残留进程

#### Scenario: 产物自动入库与下一步入口
- **WHEN** 合成完成
- **THEN** 系统**自动将产物保存到素材库**（type=VIDEO，标签含「AB搬运」），无需用户手动保存，避免产物丢失
- **AND** 在结果页提供「去重」「AI 包装」两个跳转入口，用户可选择继续加工或返回
- **AND** 用户可在素材库中找到该产物，标签「AB搬运」便于筛选

#### Scenario: 异常处理
- **WHEN** A 视频无音轨
- **THEN** 提示「A 视频无音轨，无法作为音频源」，禁止开始合成
- **WHEN** A/B 任一未选择
- **THEN** 「开始合成」按钮置灰

### Requirement: 素材库升级
系统 SHALL 提供完整的素材库管理能力，支持视频播放预览、操作菜单、编辑修改、URI 持久化、多选与标签分类。

#### Scenario: 视频播放预览
- **WHEN** 用户点击视频素材卡
- **THEN** 弹出 media3 视频播放器，支持播放/暂停/进度拖动

#### Scenario: 真实缩略图
- **WHEN** 视频素材导入完成
- **THEN** 素材卡显示 MediaMetadataRetriever 提取的首帧，而非占位渐变

#### Scenario: 操作菜单
- **WHEN** 用户点击素材卡
- **THEN** 弹出 BottomSheet 菜单：预览/去重/AB 搬运/制作带货视频/编辑/重命名/删除

#### Scenario: 素材编辑修改
- **WHEN** 用户选择「编辑」
- **THEN** 弹出编辑对话框，可修改素材名称、标签、备注

#### Scenario: URI 持久化
- **WHEN** 用户从系统选择器导入 content:// URI
- **THEN** 系统调用 takePersistableUriPermission 并复制到 App 私有目录，存储本地路径

#### Scenario: 多选模式
- **WHEN** 用户长按素材卡
- **THEN** 进入多选模式，支持批量删除/批量去重

#### Scenario: 标签分类
- **WHEN** 用户在素材库顶部切换标签
- **THEN** 列表按标签筛选显示

#### Scenario: 文件大小正确显示
- **WHEN** 素材卡渲染
- **THEN** 显示正确文件大小（修复 content:// 返回 0 的问题）

### Requirement: 主页与导航重构
系统 SHALL 启用底部导航栏，重构主页入口卡片，修复无交互按钮与 UI 重复问题。

#### Scenario: 底部导航栏
- **WHEN** App 启动
- **THEN** 底部显示 4 Tab：首页/素材库/发布记录/我的，可正常切换

#### Scenario: 主页入口卡片
- **WHEN** 用户在主页
- **THEN** 看到 4 个主入口卡片：视频带货/图文带货/AB 搬运/二创工厂（灰态「敬请期待」）

#### Scenario: 快捷入口
- **WHEN** 用户点击快捷入口
- **THEN** 素材库/发布记录可用，任务队列与设置灰态

#### Scenario: 最近草稿
- **WHEN** 用户在主页
- **THEN** 底部显示最近 5 条草稿（接 DraftRepository 真实数据），点击可继续编辑

#### Scenario: 商品库独立
- **WHEN** 用户从商品库入口进入
- **THEN** 跳转独立 `goods-library` 路由，不再复用 `goods/video`

### Requirement: 发布记录页
系统 SHALL 提供发布记录列表，展示历史发布内容。

#### Scenario: 查看发布记录
- **WHEN** 用户点击底部「发布记录」Tab
- **THEN** 显示历史发布记录列表，含标题/平台/时间/状态

#### Scenario: 重新发布
- **WHEN** 用户点击某条记录
- **THEN** 显示详情，提供「重新发布」按钮

### Requirement: 我的 Tab 设置页
系统 SHALL 在底部导航「我的」Tab 提供轻量化设置入口，包含 AI API Key 配置、联盟凭证配置、版本信息与缓存清理。本期不引入账号体系。

**设计原则**：与现有 `data-ai`（API Key 运行时注入）和 `data-alliance`（联盟 fallback）模块对接，将原本散落在代码中的配置项暴露给用户。不引入登录注册，降低上线复杂度。

#### Scenario: AI API Key 配置
- **WHEN** 用户进入「我的」>「AI 设置」
- **THEN** 显示当前 AI 服务提供商选择（默认 OpenAI 兼容）与 API Key 输入框（密文显示）
- **WHEN** 用户保存 API Key
- **THEN** 通过 DataStore 加密存储，运行时注入到 data-ai 模块

#### Scenario: 联盟凭证配置
- **WHEN** 用户进入「我的」>「联盟设置」
- **THEN** 显示淘宝/京东/拼多多等联盟 AppKey/AppSecret 配置项
- **WHEN** 用户未配置联盟凭证
- **THEN** 显示「未配置，将使用 Mock 数据」提示，data-alliance 模块走 fallback

#### Scenario: 版本信息
- **WHEN** 用户进入「我的」>「关于」
- **THEN** 显示应用版本号、构建时间、GitHub 仓库链接

#### Scenario: 清理缓存
- **WHEN** 用户点击「清理缓存」
- **THEN** 显示当前缓存大小，确认后清理 `cacheDir` 与临时产物目录，不影响素材库已导入文件

### Requirement: 路由与 Bug 修复
系统 SHALL 修复所有已知 P0/P1 bug，确保关键路径无闪退。

#### Scenario: 商品选择不闪退
- **WHEN** 用户在 GoodsScreen 点击「选择」按钮
- **THEN** 跳转到素材库（`material` 路由），不闪退

#### Scenario: 链接解析
- **WHEN** 用户输入合法商品链接
- **THEN** 通过正则提取商品 ID 后跳转
- **WHEN** 用户输入非法链接
- **THEN** 显示「链接格式无法识别」提示，不闪退

#### Scenario: 路由参数编码
- **WHEN** 任何外部字符串作为路由参数
- **THEN** 必须经过 `Uri.encode`，避免 `/`、`?`、`=` 破坏路由解析

#### Scenario: GoodsScreen 差异化
- **WHEN** mode=VIDEO
- **THEN** 标题显示「选择视频带货商品」
- **WHEN** mode=IMAGE
- **THEN** 标题显示「选择图文带货商品」

### Requirement: 测试覆盖
系统 SHALL 对所有新增 core-ffmpeg 模块和 domain UseCase 提供单元测试，对关键路径提供 UI 冒烟测试。

#### Scenario: FFmpeg 模块单测
- **WHEN** 运行 core-ffmpeg 测试
- **THEN** AVStreamSwapper/TimelineAligner/CrossVideoAudioMixer/AVSwapCommandBuilder 全部通过

#### Scenario: UseCase 单测
- **WHEN** 运行 domain 测试
- **THEN** ABTransportUseCase 全部通过

#### Scenario: UI 冒烟测试
- **WHEN** 运行 UI 测试
- **THEN** AB 搬运全链路、素材库工作流、商品带货全链路、发布记录 4 个场景全部通过

## MODIFIED Requirements

### Requirement: 素材库导入流程
[原：仅保存 URI 字符串到 Room]
[新：调用 takePersistableUriPermission + 复制到私有目录 + 提取首帧缩略图 + 存储本地路径]

### Requirement: 素材卡交互
[原：onClick 空占位，onLongClick 删除]
[新：onClick 弹出操作菜单 BottomSheet，onLongClick 进入多选模式]

### Requirement: 主页布局
[原：双模式卡片 + 5 个快捷入口（含 Unit 占位）]
[新：4 主入口卡片（含 AB 搬运）+ 快捷入口（无占位）+ 最近草稿真实数据 + 底部导航栏]

### Requirement: 商品库路由
[原：跳转 `goods/video`，与视频带货重复]
[新：跳转独立 `goods-library` 路由]

### Requirement: Mock 数据
[原：每平台 2 条]
[新：每平台至少 10 条，覆盖不同品类]

## REMOVED Requirements

### Requirement: import/video 路由
**Reason**: 该路由常量已定义但从未注册 NavGraph，是闪退根因
**Migration**: 改为跳转 `material` 路由，串通「选商品 → 选素材」链路

### Requirement: QuickAction.PUBLISH_RECORDS Unit 占位
**Reason**: 无实际交互，用户体验差
**Migration**: 新建 feature-history 模块，路由表新增 `history` 目的地
