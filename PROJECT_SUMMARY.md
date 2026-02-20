# 足迹 (Footprint) 项目深度技术总结

## 1. 项目概述
本项目是一个原生 Android 应用，采用 **Kotlin** 语言和 **Jetpack Compose** 声明式 UI 框架构建。它不仅是一个高精度的 GPS 轨迹记录器，更是一个集成了生成艺术、情感计算和地理社交元素（时空胶囊）的个人数字化身 (Digital Avatar) 系统。

## 2. 核心功能实现详解

### 🌍 1. 高精度地理追踪系统 (Geo-Tracking System)
*代码核心: `service/LocationTrackingService.kt`, `ui/screens/MapScreen.kt`*
- **后台保活服务**: 实现了一个前台服务 (`LocationTrackingService`)，绑定通知栏，确保应用切后台或锁屏时仍能持续记录 GPS 轨迹。
- **智能定位算法**:
  - **防漂移 (Anti-Drift)**: 过滤距离 < 5m 或速度异常（瞬间移动）的噪点。
  - **自适应频率 (Adaptive Interval)**: 根据移动速度动态调整定位频率（静止时 30s/次，开车时 2s/次，步行时 10s/次），省电与精度平衡。
  - **WakeLock**: 管理电源锁，防止 CPU 休眠导致断触。
- **实时状态通知**: 通知栏实时更新当前的移动距离 (km)、速度 (km/h) 和持续时间。
- **多模式地图引擎**: 基于 **AMap (高德) 3D SDK** 开发了四种地图渲染模式：
  - **标准模式 (Standard)**: 传统轨迹线 + 标记点。
  - **迷雾模式 (Fog of War)**: 实现了类似游戏中“战争迷雾”的视觉效果。利用 `Haze` 库和 Canvas 绘图，将未探索区域覆盖动态噪点云雾，已探索路径通过混合模式 (BlendMode.DstOut) “擦除”迷雾，并带有边缘柔化光晕。
  - **热力图模式 (Heatmap)**: 基于历史所有轨迹点渲染热力分布，通过半透明圆叠加实现。
  - **胶囊模式 (Capsule)**: 专注显示时空胶囊分布。

### ⏳ 2. 时空胶囊 (Time Capsules)
*代码核心: `data/local/TimeCapsuleEntity.kt`, `FootprintViewModel.kt`*
- **LBS 地理围栏**: 允许用户在当前经纬度“埋藏”一条消息或图片。
- **时间锁**: 设定解锁时间（1分钟至1年）。只有当用户**再次物理进入**该坐标半径 50 米范围内，且**时间已到达**解锁时刻时，胶囊才会自动开启。
- **状态管理**: 数据库区分存储已解锁和未解锁的胶囊，UI 层分别渲染黄色（已解锁）和红色（未解锁）标记。

### 🎨 3. 生成艺术工坊 (Generative Art Studio)
*代码核心: `ui/screens/art/FootprintArtStudioScreen.kt`*
- **轨迹可视化**: 将无聊的 GPS 数据转化为艺术海报。使用 `TextureMapView` 剥离地图标签，仅保留路网或纯黑背景。
- **参数化设计**: 用户可实时调节线条粗细、光晕半径 (Glow Radius)、地图配色（赛博粉、霓虹绿等）。
- **布局引擎**: 实现了三种 Canvas 绘图模板：
  - `FULLSCREEN`: 全屏海报风格，支持自定义谷歌字体 (Ma Shan Zheng, Long Cang 等)。
  - `POLAROID`: 拍立得相纸风格，自动计算边框比例。
  - `GEEK_STATS`: 极客数据面板风格，带有装饰性数据线框。
- **高清导出**: 利用 `View` 绘图缓存机制，将生成的艺术作品导出为高分辨率图片保存至相册。

### 📊 4. 仪表盘与数据洞察 (Dashboard & Insights)
*代码核心: `ui/screens/DashboardScreen.kt`, `FootprintViewModel.kt`*
- **那年今日 (Memory Lane)**: 算法自动检索历史年份同月同日的记录，唤醒记忆；若无记录则显示每日格言。
- **用户画像**: 基于总里程自动计算用户等级（新手 -> 传奇旅行家），并在 UI 上显示对应的 Material 图标。
- **动态统计**: 实时计算年度总里程、活跃天数、活力指数（基于心情和能量等级加权计算）。
- **心情热力图**: 在首页底部展示基于 Github Contribution 风格的心情日历热力图。

### 📝 5. 情感化记录与 AI
*代码核心: `utils/AIStoryGenerator.kt`, `data/model/FootprintEntry.kt`*
- **AI 故事引擎**: 内置一个基于模板和随机因子的轻量级生成器。根据地点、天气、心情、时间段（如“晨光熹微”、“霓虹闪烁”）自动生成带有赛博朋克叙事风格的日记短文。
- **多维数据录入**: 支持心情 (Mood)、能量值 (Energy)、交通方式、标签、图片附件的结构化存储。

### 💾 6. 数据架构与持久化
*代码核心: `data/local/*`, `utils/FileUtils.kt`*
- **Room 数据库**: 设计了 6 张关联表：`footprints` (主记录), `track_points` (高频轨迹点), `travel_goals` (目标), `time_capsules` (胶囊), `badges`, `privacy_fences`。
- **完整备份/恢复**:
  - **导出**: 将数据库序列化为 JSON，并将所有引用的图片资源打包，生成 ZIP 压缩包。
  - **导入**: 解析 ZIP，智能恢复图片路径，合并或覆盖现有数据。
- **API Key 动态注入**: 支持在应用内运行时输入高德 API Key，避免硬编码 Key 导致的安全风险或开源泄露。

## 3. 技术栈清单 (Tech Stack)

| 类别 | 技术/库 | 用途 |
| :--- | :--- | :--- |
| **语言** | Kotlin | 100% 纯 Kotlin 开发，深度使用 Coroutines 和 Flow |
| **UI 框架** | Jetpack Compose | Material 3 设计规范，全声明式 UI |
| **地图 SDK** | AMap 3DMap SDK | 地图渲染、轨迹绘制、纹理地图 |
| **定位 SDK** | AMap Location SDK | 高精度持续定位 |
| **架构** | MVVM | ViewModel, Repository Pattern, StateFlow |
| **数据库** | Room (SQLite) | 本地数据持久化，自定义 TypeConverters |
| **UI 特效** | Haze (Chris Banes) | 高性能实时毛玻璃/模糊效果 (Glassmorphism) |
| **图片加载** | Coil | 异步加载本地图片和头像 |
| **字体** | Google Fonts | 艺术工坊中的特殊字体支持 |
| **序列化** | Gson | 数据的 JSON 序列化与备份 |
| **构建** | Gradle Kotlin DSL | 现代化构建配置 |

## 4. 项目文件结构索引

```
app/src/main/java/com/footprint/
├── data/
│   ├── local/          # Room Entity 定义 (FootprintEntity, TimeCapsuleEntity...)
│   ├── model/          # 领域模型 (Mood, TransportType...)
│   └── repository/     # 数据仓库，负责协调 DB 和 ViewModel
├── service/
│   ├── LocationTrackingService.kt # 核心：后台定位前台服务
│   └── LocationManager.kt         # 辅助定位工具
├── ui/
│   ├── screens/
│   │   ├── DashboardScreen.kt     # 仪表盘：统计、那年今日、列表
│   │   ├── MapScreen.kt           # 地图：迷雾模式、胶囊交互、轨迹显示
│   │   └── art/                   # 艺术工坊相关屏幕
│   ├── components/     # 通用组件 (LiquidGlassCard, Heatmap...)
│   └── theme/          # 主题定义
├── utils/
│   ├── AIStoryGenerator.kt # 赛博朋克文案生成器
│   └── FileUtils.kt        # ZIP 打包/解包逻辑
└── FootprintViewModel.kt   # 全局状态管理
```
