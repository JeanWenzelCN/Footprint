# 足迹 (Footprint) - 赛博朋克探索记录器

<div align="center">

**一款基于 Jetpack Compose 构建，拥有极致液态玻璃 (Glassmorphism) 视觉效果的足迹追踪应用**

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Map](https://img.shields.io/badge/Map-AMap-blue.svg)](https://lbs.amap.com/)

</div>

---

## ✨ 核心特性

- 🧪 **液态玻璃 UI**：深度定制的 Material 3 界面，模拟 iOS/macOS 的背景模糊与毛玻璃质感。
- 🌃 **赛博朋克配色**：内置高饱和度霓虹配色方案，让旅行记录极具科技感。
- 📍 **高德定位集成**：针对国内环境优化的 AMap 定位引擎，支持实时轨迹绘制。
- 🛡️ **智能隐私围栏**：独创“幽灵模式”，自动隐藏敏感区域（如家、公司）的精准坐标。
- 🏆 **勋章博物馆**：根据探索深度自动解锁地域特色成就。
- 📝 **AI 故事引擎**：基于地理位置和心情，一键生成旅行文学草稿。

---

## 🎨 最新视觉更新 (New!)

- **全域液态玻璃**：所有页面（概览、足迹海、目标页、弹窗）均已升级为高通透的液态玻璃风格。
- **动态渐变背景**：新增 `AppBackground`，提供淡雅且富有层次的全局背景，增强沉浸感。
- **高分屏优化**：摒弃传统模糊滤镜，采用高精度矢量渐变与光影模拟，确保在高分辨率屏幕上字体与图标锐利清晰，无锯齿。

---

## 🚀 快速上手 (配置 API Key)

为了保护隐私，本项目仓库不包含高德地图 API Key。请按照以下步骤配置以运行项目：

### 1. 申请高德 Key
1. 前往 [高德开放平台控制台](https://console.amap.com/)。
2. 创建一个 **Android 平台** 的应用。
3. **获取 SHA1**：在项目根目录下运行 `./gradlew signingReport`，复制 `Variant: debug` 下的 SHA1 值。
4. 将你的 **包名** (`com.footprint`) 和 **SHA1** 填入高德后台，生成 API Key。

### 2. 应用内配置 Key (New!)
无需修改代码或配置文件！
1. 编译并安装应用。
2. 打开地图界面，点击右上角的 **设置 (⚙️)** 按钮。
3. 在弹出的玻璃风格对话框中输入你的 Key 并保存。
4. 重启应用即可生效。

---

## 🛠️ 技术架构

- **UI**: Jetpack Compose (Declarative UI)
- **Navigation**: Compose Navigation with Custom Animations
- **Database**: Room Persistence Library
- **Architecture**: MVVM + Repository Pattern
- **Async**: Kotlin Coroutines & Flow
- **Maps**: AMap 3D SDK & Location SDK

---

## 📂 项目结构

- `app/src/main/java/com/footprint/ui/theme`：定制的液态玻璃主题与赛博朋克调色板。
- `app/src/main/java/com/footprint/ui/components`：核心 UI 组件库，包含 `GlassMorphicCard` 和 `AppBackground`。
- `app/src/main/java/com/footprint/service`：高性能后台定位追踪服务。
- `app/src/main/java/com/footprint/utils`：API Key 安全管理工具。
- `app/src/main/java/com/footprint/ui/screens/MapScreen`：核心地图交互逻辑。

---

## 🕒 更新日志 (Changelog)

### 2026-01-04: 稳定性与布局健壮性优化 (v1.9.0)

#### 🐛 修复日志 (Bug Fixes)
- **图片处理稳定性**: 修复了添加足迹时因主线程进行 IO 操作导致的闪退，现已全异步化。
- **列表布局防崩溃**: 为 Timeline 和 Dashboard 的图片列表增加了显式高度约束，并增加了脏数据（空路径）过滤机制，彻底解决嵌套列表布局崩溃问题。

### 2026-01-03: iOS 26 液态玻璃与智能交互深度进化 (v1.5.0 - v1.8.0)

#### ✨ 新增特性 (Features)
- **视觉重构 (Visual Overhaul)**:
    - **液态玻璃 UI (v1.8.0)**: 全站升级为 iOS 26 风格液态玻璃质感，引入物理折射模拟、动态流动背景及触控物理反馈。
    - **自适应视觉 (v1.5.0)**: 主题色随心情自动变换，新增自适应 App 图标。
- **智能交互 (Smart Interaction)**:
    - **年度聚合看板 (v1.7.0)**: 数据概览升级为年度视角，支持点击下钻查看明细及足迹回放联动。
    - **折叠列表 (v1.7.0)**: 引入月份折叠功能，优化长列表体验。
- **个性化系统 (Personalization)**:
    - **旅行者等级 (v1.6.0)**: 基于里程解锁“传奇旅行家”等荣誉等级与勋章。
    - **自定义图标 (v1.6.0)**: 支持为足迹选择特定图标（如飞行、餐饮等）。
    - **回忆引擎 (v1.5.0)**: 首页“那年今日”自动重连历史记忆，无记录时展示诗意寄语。
    - **活力指数 (v1.5.0)**: 实时计算生活活跃度。

#### 🐛 修复日志 (Bug Fixes)
- **稳定性**: 修复多流合并导致的类型推断错误、语法嵌套错误及双精度类型不匹配问题。
- **体验优化**: 优化底部栏启动逻辑及动画流畅度。

### 2026-01-02: Telegram 风格视觉与数据安全 (v1.2.0 - v1.4.0)

#### ✨ 新增特性 (Features)
- **数据安全 (v1.4.0)**: 上线全量数据备份与恢复功能 (JSON 导入/导出)，保障数据资产安全。
- **Telegram 视觉语言 (v1.3.0)**: 引入沉浸式毛玻璃顶部栏、圆形头像系统、悬浮胶囊导航及 Squircle 连续圆角设计。
- **交互进化 (v1.2.0)**:
    - **物理弹性动画**: 列表滑动引入物理阻尼效果。
    - **主题持久化**: 支持黑夜/白天模式手动切换与记忆。
    - **数据下钻**: 支持点击统计卡片查看深度分布。

#### 🐛 修复日志 (Bug Fixes)
- **地图定位**: 彻底修复“点击两次才生效”的定位焦点问题，实现启动即对焦。
- **UI 适配**: 修复黑夜模式下部分文本颜色显示异常及概览页按钮跳转错误。

### 2026-01-01: 历史足迹回放与无限列表 (v1.1.0)

#### ✨ 新增特性 (Features)
- **历史足迹回放**: 支持按时间段动态回放移动轨迹。
- **列表体验**: 解除概览页展示数量限制，支持全域内容编辑。
- **沉浸式弹窗**: 优化对话框背景的高斯模糊遮罩。

#### 🐛 修复日志 (Bug Fixes)
- **错误屏蔽**: 优化定位服务，屏蔽非关键的 GPS 信号波动错误提示。

### 2025-12-31: 初始液态玻璃重构与架构升级

#### ✨ 新增特性 (Features)
- **UI 重构**: 全面升级为液态玻璃 (Glassmorphism) 风格组件。
- **动态配置**: 支持应用内动态配置高德地图 API Key。

#### 🐛 核心修复日志 (Bug Fixes)
- **地图黑屏**: 修复 Compose 与 MapView 生命周期不同步导致的黑屏问题。
- **安全拦截**: 修正 debug 包名后缀导致的 API Key 鉴权失败 (Error 7)。
- **配置生效**: 解决 Key 设置后需重启才能生效的问题。

---

## 🤝 贡献与反馈

欢迎提交 PR 或 Issue 来完善这个项目！

1. Fork 本项目。
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)。
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)。
4. 推送到分支 (`git push origin feature/AmazingFeature`)。
5. 开启 Pull Request。

---

<div align="center">
Made with ❤️ by StarsUnsurpass
</div>