# VideoWorkshop v2.0.0 产品需求文档（PRD）

| 项目 | 内容 |
|---|---|
| 版本号 | v2.0.0 |
| 文档状态 | 待评审 |
| 编写日期 | 2026-07-25 |
| 上一版本 | v1.x（单视频去重 + AI 包装） |
| 本次定位 | **AB 搬运 MVP + 现有问题全面修复 + 素材库升级** |

---

## 一、项目背景

### 1.1 当前现状

VideoWorkshop v1.x 已实现「选品 → 视频去重 → AI 包装 → 发布」的基础链路，但存在以下严重问题：

**用户反馈的 P0/P1 问题**：
- 点商品「选择」按钮立即闪退
- 输入链接解析直接闪退
- 素材库导入视频无法播放
- 导入素材后没有操作空间（无去重/二创入口）
- 主页 5 个按钮中「发布记录」无交互
- 商品库 / 视频带货 / 图文带货 3 个界面 UI 一模一样

**根因**：
- 路由 `import/video` 已定义但从未注册 NavGraph
- 素材卡 `onClick` 是空占位，且未引入 media3 播放器
- 主页部分 `QuickAction` 是 `Unit` 占位
- 商品库与视频带货复用同一 `goods/video` 路由

### 1.2 市场对标

调研同类竞品（算力刺客、闪搬）后发现：

| 能力 | 算力刺客 | 闪搬 | VideoWorkshop 机会 |
|---|---|---|---|
| AB 搬运 | ❌ | ✅ | **必做核心卖点** |
| 多通道去重 | ✅ | ✅ | 已有 8 算法，需组合化 |
| AI 文案/配音/字幕 | ❌ | ❌ | ✅ 已实现，是最大蓝海 |
| 素材库管理 | ❌ | ❌ | ✅ 差异化机会 |
| 多平台联盟对接 | ❌ | 仅拼多多 | ✅ 已搭框架 |
| 模板系统 | ❌ | ✅ | v2.1 规划 |

**结论**：v2.0.0 的核心目标是在修复现有问题的前提下，上线 **AB 搬运** 这一竞品验证过的核心能力，并补齐素材库工作流，形成「素材库 → AB 搬运 → 去重 → AI 包装 → 发布」的完整二创链路。

### 1.3 版本目标

**总目标**：用户能完整走通「导入 A/B 视频 → AB 搬运合成 → 去重 → AI 包装 → 发布」全链路，且现有所有 P0/P1 问题彻底修复。

**量化指标**：
- ✅ 6 个已知 bug 100% 修复
- ✅ AB 搬运功能可用（纯音轨替换 + 截断对齐）
- ✅ 素材库支持视频播放与操作菜单
- ✅ 主页所有按钮均有实际交互
- ✅ 端到端链路无闪退（关键路径冒烟测试通过率 100%）

---

## 二、用户痛点与场景

### 2.1 目标用户

- 抖音/快手好物分享博主
- 短视频带货达人
- 视频搬运二次创作者
- 小型短视频工作室运营者

### 2.2 核心使用场景

**场景一：AB 搬运二创**
> 用户小明做抖音好物分享，发现 A 视频的讲解音频很好，但画面不够吸引人；同时有 B 视频画面拍摄精良但讲解一般。小明希望用 A 的音频 + B 的画面合成新视频，再通过去重和 AI 包装后发布。

**场景二：素材库工作流**
> 用户小红每天会收集大量带货视频素材，希望导入 App 后能预览、分类、选择素材进行二次创作，而不是导入完就找不到入口。

**场景三：商品带货全链路**
> 用户小李从商品库选了商品，希望能流畅地进入素材选择 → AB 搬运 → 去重 → AI 包装 → 发布，而不是点「选择」就闪退。

### 2.3 用户痛点优先级

| 优先级 | 痛点 | 对应功能模块 |
|---|---|---|
| P0 | 点商品选择/链接解析闪退 | 路由修复 |
| P0 | 无法进行 AB 搬运 | AB 搬运新功能 |
| P1 | 素材导入后无法预览 | 素材库升级 |
| P1 | 素材导入后无操作入口 | 素材库升级 |
| P1 | 主页部分按钮无反应 | 主页重构 |
| P2 | 商品库/视频带货 UI 重复 | 主页重构 |

---

## 三、功能需求详细说明

### 模块 1：AB 搬运（核心新功能）

#### 1.1 功能描述

支持用户选择 A 视频和 B 视频，将 A 视频的音频与 B 视频的画面合成新视频，作为二创素材进入后续去重与包装流程。

#### 1.2 用户流程

```
主页 → 点击「AB 搬运」入口
  → 选择 A 视频（音频源）
    - 来源：素材库 / 商品视频 / 系统文件
  → 选择 B 视频（画面源）
    - 来源：素材库 / 商品视频 / 系统文件
  → 配置合成参数
    - 合成模式：纯音轨替换 / 音轨混合
    - 时长对齐：截断到短者 / 循环到长者（v2.1）
    - 自定义起止点（v2.1）
  → 预览合成效果（关键帧）
  → 确认生成
    - 进度条显示
    - 可取消
  → 生成完成
    - 预览产物
    - 选择下一步：去重 / 直接进入 AI 包装 / 保存到素材库
```

#### 1.3 功能清单

| 编号 | 功能项 | 优先级 | 说明 |
|---|---|---|---|
| AB-01 | A/B 视频选择器 | P0 | 双入口选择，支持从素材库与系统文件导入 |
| AB-02 | 纯音轨替换合成 | P0 | B 画面 + A 音频，简单 `-map 0:v -map 1:a` |
| AB-03 | 截断对齐策略 | P0 | 输出时长 = min(A 时长, B 时长) |
| AB-04 | 关键帧预览 | P0 | 生成前预览 3-5 帧合成效果，不渲染全片 |
| AB-05 | 合成进度与取消 | P0 | 进度条 + 取消按钮，取消后立即停止 FFmpeg |
| AB-06 | 产物下一步入口 | P0 | 生成完成页提供「去重」「AI 包装」「保存到素材库」三入口 |
| AB-07 | 音轨混合模式 | P1 | B 画面 + A 音频 + B 原声混音，可调音量比例 |
| AB-08 | 循环对齐策略 | P1 | 短视频循环到长者时长 |
| AB-09 | 自定义起止点 | P1 | 时间轴拖动选择 A/B 的起止时间 |

#### 1.4 输入输出规范

**输入**：
- A 视频路径（必填，本地文件路径）
- B 视频路径（必填，本地文件路径）
- 合成模式（默认：纯音轨替换）
- 时长策略（默认：截断到短者）

**输出**：
- 合成视频文件（MP4，h264 + AAC，保存到 App 私有目录）
- 输出文件命名：`ab_transport_{timestamp}.mp4`
- 返回输出路径供下一步使用

#### 1.5 边界条件与异常处理

| 场景 | 处理方式 |
|---|---|
| A/B 视频任一未选择 | 「开始合成」按钮置灰 |
| A/B 视频格式不支持 | 提示「不支持的格式，请使用 MP4/MOV」 |
| A 视频无音轨 | 提示「A 视频无音轨，无法作为音频源」 |
| 合成过程中存储不足 | 提示「存储空间不足，请清理后重试」 |
| 用户取消合成 | 立即停止 FFmpeg 进程，删除部分产物 |
| 合成失败 | 显示错误信息 + 「重试」按钮 |

#### 1.6 验收标准

- [ ] 用户能从主页进入 AB 搬运页面
- [ ] 能成功选择 A 视频和 B 视频
- [ ] 纯音轨替换模式合成成功，产物音轨来自 A、画面来自 B
- [ ] 截断对齐策略下，产物时长 = min(A, B)
- [ ] 合成过程显示进度条，可取消
- [ ] 取消后立即停止，无残留进程
- [ ] 合成完成能进入「去重」「AI 包装」「保存到素材库」三入口
- [ ] AB 搬运全流程无闪退

---

### 模块 2：素材库升级

#### 2.1 功能描述

修复素材库当前「导入即死胡同」的问题，支持视频播放预览、操作菜单、URI 持久化，让素材真正可用。

#### 2.2 功能清单

| 编号 | 功能项 | 优先级 | 说明 |
|---|---|---|---|
| MAT-01 | 视频播放预览 | P0 | 点击素材卡弹出视频播放器，支持播放/暂停/进度拖动 |
| MAT-02 | 图片预览 | P0 | 点击图片素材弹出大图预览 |
| MAT-03 | 真实缩略图 | P0 | 视频卡显示首帧缩略图，替代当前占位渐变 |
| MAT-04 | 操作菜单 | P0 | 点击素材卡弹出菜单：预览/去重/AB 搬运/制作带货视频/删除 |
| MAT-05 | URI 持久化权限 | P0 | 导入时调用 `takePersistableUriPermission`，避免重启后失效 |
| MAT-06 | URI 复制到私有目录 | P0 | content:// URI 复制为本地文件路径后再存储 |
| MAT-07 | 文件大小正确显示 | P0 | 修复 `File(path).length()` 对 content:// 返回 0 的问题 |
| MAT-08 | 多选模式 | P1 | 长按进入多选，支持批量删除 |
| MAT-09 | 标签分类 | P1 | 自动标签（视频/图片/已处理/未处理）+ 自定义标签 |
| MAT-10 | 搜索与筛选 | P2 | 按标签、文件名搜索 |

#### 2.3 操作菜单交互规范

点击素材卡弹出 BottomSheet，菜单项：

```
┌──────────────────────────┐
│  素材名称.mp4             │
│  视频 · 12.5MB · 00:45    │
├──────────────────────────┤
│  ▶ 预览                  │
│  🔄 去重                  │
│  🔀 AB 搬运（作为 A/B 源）│
│  🎬 制作带货视频          │
│  📝 重命名                │
│  🗑 删除                  │
└──────────────────────────┘
```

- 「去重」→ 跳转 DedupRoute，携带素材路径
- 「AB 搬运」→ 跳转 ABTransportRoute，预填该素材为 A 或 B
- 「制作带货视频」→ 跳转 EnhanceRoute，携带素材路径
- 「删除」→ 二次确认后删除

#### 2.4 验收标准

- [ ] 导入视频后点击能弹出播放器正常播放
- [ ] 导入图片后点击能查看大图
- [ ] 视频卡显示真实首帧，非占位渐变
- [ ] 点击素材卡弹出操作菜单
- [ ] 操作菜单各入口能正确跳转
- [ ] App 重启后导入的素材仍可访问
- [ ] 素材卡显示正确的文件大小

---

### 模块 3：主页与导航重构

#### 3.1 功能描述

修复主页无交互按钮、入口重复问题，启用底部导航栏，新增发布记录页。

#### 3.2 主页布局重构

**主入口卡片**（顶部 2×2 网格）：

| 卡片 | 跳转目标 |
|---|---|
| 视频带货 | `goods/video` |
| 图文带货 | `goods/image` |
| AB 搬运 | `abtransport` |
| 二创工厂 | `secondarycreation`（v2.1，v2.0 显示「敬请期待」灰态） |

**快捷入口**（中部网格）：

| 入口 | 跳转目标 |
|---|---|
| 素材库 | `material` |
| 任务队列 | `taskqueue`（v2.1，v2.0 隐藏） |
| 发布记录 | `history` |
| 设置 | `settings`（v2.1，v2.0 隐藏） |

**最近草稿**（底部列表）：
- 接 DraftRepository 真实数据，不再占位
- 显示最近 5 条草稿，点击可继续编辑

#### 3.3 底部导航栏

启用已写好的 `VWBottomBar`，4 个 Tab：

| Tab | 图标 | 跳转 |
|---|---|---|
| 首页 | home | `home` |
| 素材库 | folder | `material` |
| 发布记录 | history | `history` |
| 我的 | person | `profile`（v2.1，v2.0 显示设置入口） |

#### 3.4 功能清单

| 编号 | 功能项 | 优先级 | 说明 |
|---|---|---|---|
| HOME-01 | 主页入口卡片重构 | P0 | 4 个主入口：视频带货/图文带货/AB 搬运/二创工厂(灰态) |
| HOME-02 | 快捷入口重构 | P0 | 素材库/发布记录可用，任务队列灰态 |
| HOME-03 | 最近草稿接真实数据 | P0 | 接 DraftRepository，显示最近 5 条 |
| HOME-04 | 底部导航栏启用 | P0 | 4 Tab：首页/素材库/发布记录/我的 |
| HOME-05 | 发布记录页新建 | P0 | 列表展示已发布记录，支持查看详情/重新发布 |
| HOME-06 | 商品库入口独立 | P1 | 商品库跳独立商品库页，不再复用 `goods/video` |

#### 3.5 验收标准

- [ ] 主页所有按钮均有实际交互（无 Unit 占位）
- [ ] 主页入口卡片不重复
- [ ] 底部导航栏 4 Tab 可正常切换
- [ ] 最近草稿显示真实数据
- [ ] 发布记录页能查看历史发布
- [ ] 「二创工厂」入口显示「敬请期待」灰态，不闪退

---

### 模块 4：Bug 修复（P0 必须）

#### 4.1 路由修复

| 编号 | Bug | 修复方案 |
|---|---|---|
| FIX-01 | 点商品「选择」闪退 | 把 `import/video` 路由改为 `material`，串通「选商品 → 选素材 → 去重 → 增强」链路 |
| FIX-02 | 输入链接解析闪退 | 在 GoodsViewModel 加 `parseManualLink` 正则提取商品 ID；所有外部字符串入路由必须 `Uri.encode` |
| FIX-03 | 主页「发布记录」无交互 | 新建 `feature-history` 模块承接，路由表新增 `history` 目的地 |

#### 4.2 商品库 UI 差异化

| 编号 | Bug | 修复方案 |
|---|---|---|
| FIX-04 | 商品库与视频带货 UI 一模一样 | 商品库跳独立 `goods-library` 路由；GoodsScreen 标题与默认平台 Tab 按 mode 差异化 |
| FIX-05 | Mock 数据仅 2 条占位 | 每平台扩量到至少 10 条，覆盖不同品类 |

#### 4.3 验收标准

- [ ] 点商品「选择」能正常跳转，不闪退
- [ ] 输入合法商品链接能解析出商品 ID 并跳转
- [ ] 输入非法链接显示「链接格式无法识别」提示，不闪退
- [ ] 主页所有按钮点击有响应
- [ ] 商品库与视频带货是两个不同界面
- [ ] Mock 数据至少 10 条/平台

---

## 四、非功能需求

### 4.1 性能要求

| 指标 | 要求 |
|---|---|
| AB 搬运合成 5 分钟视频 | ≤ 3 分钟（中端机型） |
| 素材库列表加载 | ≤ 500ms（100 条素材） |
| 视频预览启动 | ≤ 1s |
| 主页冷启动 | ≤ 1.5s |

### 4.2 稳定性要求

- 关键路径冒烟测试通过率 100%（无闪退）
- 内存泄漏检测：关键页面退出后内存可回收
- FFmpeg 进程必须可取消，取消后无残留

### 4.3 兼容性要求

- 最低支持 Android 8.0（API 26）
- 目标 API 34
- 支持架构：arm64-v8a, armeabi-v7a

### 4.4 存储要求

- AB 搬运产物保存到 App 私有目录
- 提供清理功能（设置页 → 清理缓存）
- 单次合成临时文件在合成完成后自动清理

---

## 五、技术方案概要

### 5.1 core-ffmpeg 新增能力

```
core-ffmpeg/src/main/kotlin/com/videoworkshop/core/ffmpeg/
├── pipeline/
│   ├── MultiInputCommandBuilder.kt     # 多输入命令基础
│   └── AVSwapCommandBuilder.kt         # AB 音轨替换命令
└── operators/
    ├── AVStreamSwapper.kt              # 音视频流分离重组
    └── TimelineAligner.kt              # 时间轴对齐（截断策略）
```

### 5.2 domain 层新增

```
domain/model/
├── ABTransportConfig.kt                # AB 搬运配置
└── ABTransportResult.kt                # AB 搬运产物

domain/repository/DedupRepository.kt（扩展）
    + fun abTransport(config: ABTransportConfig): Flow<TaskProgress>

domain/usecase/
└── ABTransportUseCase.kt
```

### 5.3 feature 层新增

```
feature/
├── feature-abtransport/                # AB 搬运新模块
│   ├── ABTransportRoute.kt
│   ├── ABTransportScreen.kt
│   ├── ABTransportViewModel.kt
│   └── ABTransportResultScreen.kt
│
└── feature-history/                    # 发布记录新模块
    ├── HistoryRoute.kt
    ├── HistoryScreen.kt
    └── HistoryViewModel.kt
```

### 5.4 素材库改造

```
feature-material/
├── VideoPreviewDialog.kt               # 新增：media3 视频播放器
├── MaterialActionSheet.kt              # 新增：操作菜单
└── MaterialScreen.kt（改造）
    + 接入 media3 exoplayer
    + onClick 弹出操作菜单
    + URI 持久化处理
```

### 5.5 导航重构

```
app/nav/AppNavHost.kt（重构）
├── 包裹 Scaffold + VWBottomBar
├── 注册新路由：abtransport / history
├── 修复 import/video 漏注册
└── 路由参数统一 Uri.encode
```

### 5.6 依赖新增

```toml
# gradle/libs.versions.toml
[versions]
androidx-media3 = "1.3.1"

[libraries]
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "androidx-media3" }
androidx-media3-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "androidx-media3" }
```

```kotlin
// feature-material/build.gradle.kts
implementation(libs.androidx.media3.exoplayer)
implementation(libs.androidx.media3.ui)
```

---

## 六、验收标准（端到端）

### 6.1 主流程验收

**场景 A：AB 搬运全链路**
1. 用户打开 App，主页底部导航栏可见
2. 点击「AB 搬运」入口，进入 AB 搬运页
3. 选择 A 视频（从素材库）
4. 选择 B 视频（从系统文件）
5. 选择「纯音轨替换」+「截断对齐」
6. 点击预览，看到 3-5 帧合成效果
7. 点击「开始合成」，进度条显示，可取消
8. 合成完成，预览产物
9. 选择「进入去重」，跳转 DedupRoute
10. 去重完成，进入 AI 包装
11. 包装完成，进入发布
12. 全程无闪退

**场景 B：素材库工作流**
1. 进入素材库，点击导入视频
2. 视频卡显示真实首帧缩略图
3. 点击视频卡，弹出播放器预览
4. 点击操作菜单「去重」，跳转 DedupRoute
5. App 重启后素材仍可访问
6. 素材卡显示正确文件大小

**场景 C：商品带货全链路**
1. 点击「视频带货」
2. 选商品，点「选择」按钮
3. 跳转到素材库（不闪退）
4. 选素材 → 去重 → AI 包装 → 发布
5. 全程无闪退

**场景 D：发布记录**
1. 点击底部「发布记录」Tab
2. 看到历史发布记录列表
3. 点击记录查看详情
4. 可重新发布

### 6.2 回归测试

- [ ] v1.x 已有功能（视频去重、AI 包装、图文编辑、发布）全部正常
- [ ] 6 个已知 bug 全部修复
- [ ] 端到端 4 个场景全部通过
- [ ] 性能指标达标
- [ ] 无内存泄漏

---

## 七、版本规划

### v2.0.0（本次需求）

- AB 搬运 MVP（纯音轨替换 + 截断对齐）
- 素材库升级（视频播放 + 操作菜单 + URI 持久化）
- 主页与导航重构
- 6 个已知 bug 修复
- 发布记录页

### v2.1.0（下一版本规划）

- AB 搬运进阶（音轨混合 + 循环对齐 + 自定义起止点）
- 二创工厂基础（画中画 + 视频拼接）
- 批量任务队列
- 内置 3 套二创模板
- 设置页

### v2.2.0（远期规划）

- 绿幕抠图 + 虚拟背景
- 分屏合成
- 模板库扩展到 8 套
- 多选批量处理
- 发布数据回流

---

## 八、风险评估

| 风险 | 等级 | 影响 | 对策 |
|---|---|---|---|
| FFmpeg 多输入命令在低端机性能差 | 中 | 合成耗时长 | 限制输入视频 ≤ 5 分钟；提供性能模式降分辨率 |
| AB 搬运音画不同步 | 高 | 用户体验差 | TimelineAligner 强制对齐；预览阶段显示波形对比 |
| 素材库 URI 权限丢失 | 中 | 重启后素材不可访问 | 导入时复制到私有目录 |
| media3 与现有依赖冲突 | 低 | 编译失败 | 锁定版本 1.3.1，与 AndroidX BOM 对齐 |
| 路由重构引入新 bug | 中 | 影响主流程 | 增加路由表静态校验；关键路径冒烟测试 |
| 底部导航与现有栈式导航冲突 | 中 | 导航混乱 | 统一改为底部 Tab + 栈式子导航 |

---

## 九、附录

### 9.1 术语表

| 术语 | 说明 |
|---|---|
| AB 搬运 | A 视频音频 + B 视频画面合成新视频 |
| 二创 | 二次创作，对原视频进行改编 |
| 去重 | 通过算法修改视频指纹，规避平台查重 |
| 素材库 | 用户导入的视频/图片素材管理 |
| 工作台 | 素材库 + 操作入口的统称 |

### 9.2 参考文档

- 竞品调研报告：算力刺客 / 闪搬
- v1.x 现状分析：6 个已知 bug 根因分析
- 技术调研：core-ffmpeg 能力缺口盘点

### 9.3 相关文件

- 项目根目录：`/Users/mac/Desktop/VideoWorkshop`
- 路由表：[app/src/main/java/com/videoworkshop/app/nav/AppNavHost.kt](file:///Users/mac/Desktop/VideoWorkshop/app/src/main/java/com/videoworkshop/app/nav/AppNavHost.kt)
- FFmpeg 引擎：[core/core-ffmpeg/src/main/kotlin/com/videoworkshop/core/ffmpeg/FfmpegEngine.kt](file:///Users/mac/Desktop/VideoWorkshop/core/core-ffmpeg/src/main/kotlin/com/videoworkshop/core/ffmpeg/FfmpegEngine.kt)
- 素材库页面：[feature/feature-material/src/main/kotlin/com/videoworkshop/feature/material/MaterialScreen.kt](file:///Users/mac/Desktop/VideoWorkshop/feature/feature-material/src/main/kotlin/com/videoworkshop/feature/material/MaterialScreen.kt)
- 主页：[feature/feature-home/src/main/kotlin/com/videoworkshop/feature/home/HomeScreen.kt](file:///Users/mac/Desktop/VideoWorkshop/feature/feature-home/src/main/kotlin/com/videoworkshop/feature/home/HomeScreen.kt)

---

**文档结束**
