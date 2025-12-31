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

### 2025-12-31: 液态玻璃 UI 重构与动态配置
- **UI 重构**: 全面升级为液态玻璃 (Glassmorphism) 风格，新增 `GlassMorphicCard` 和 `AppBackground` 组件。
- **页面优化**: 概览、地图、时间轴、目标计划页及弹窗均已适配新风格。
- **功能增强**: 支持应用内动态配置高德地图 API Key，移除对 `AndroidManifest.xml` 硬编码的依赖，提升安全性与易用性。
- **体验提升**: 优化了高分屏下的显示效果，新增动态渐变背景。

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
