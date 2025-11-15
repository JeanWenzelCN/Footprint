# 足迹 (Footprint)

<div align="center">

**一款通过GPS记录用户活动轨迹、生成可视化报告并解锁地域特色勋章的安卓应用**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)

</div>

---

## 📖 项目简介

**足迹 (Footprint)** 是一款专为旅行爱好者和运动达人设计的轨迹记录应用。通过智能GPS定位技术，自动记录您的每一步行程，生成精美的数据可视化报告，并通过游戏化的勋章系统激励您探索更多未知的角落。

### ✨ 核心特色

- 🔋 **省电优先**：智能双模式定位，平衡精度与续航
- 🎨 **精美可视化**：多维度数据报告，让数据会说话
- 🎮 **趣味成就**：地域特色勋章系统，记录每一次探索
- 🔒 **隐私保护**：本地加密存储，您的数据您做主
- 📱 **原生体验**：基于 Jetpack Compose 构建的现代化 UI

---

## 🚀 功能概览

> **注意**：以下功能将按开发路线图逐步实现

### 📍 智能位置记录

#### 双模式定位
- **手动记录模式**
  - 适合日常通勤、省电场景
  - 用户主动触发记录关键位置
  - 支持批量导入历史轨迹

- **后台实时记录模式**
  - 持续追踪运动轨迹
  - 智能识别停留/移动状态
  - 自动过滤无效漂移点

#### 灵活配置
- ⏱️ **可自定义记录频率**
  - 快速模式：1-5 分钟（适合跑步/骑行）
  - 标准模式：5-15 分钟（适合徒步）
  - 省电模式：15-60 分钟（适合长途旅行）

- 🏃 **运动类型识别**
  - 步行：自动识别步频和步数
  - 跑步：记录配速和心率区间
  - 骑行：计算平均速度和爬升
  - 驾驶：区分高速/城市道路
  - 静止：智能暂停记录

#### 数据安全
- 🔐 **本地加密存储**
  - 采用 AES-256 加密算法
  - 支持指纹/面部识别保护
  - 可设置数据自动清理策略

- ☁️ **可选云同步功能**
  - 多设备数据同步
  - 增量备份节省流量
  - 支持恢复到任意历史版本

---

### 📊 多维数据分析报告

| 报告类型 | 包含指标 | 可视化形式 | 分享功能 |
|---------|---------|-----------|---------|
| **周报** | • 移动距离<br>• 热点区域<br>• 运动时长<br>• 卡路里消耗 | • 热力图<br>• 折线图<br>• 轨迹回放 | ✅ 生成海报 |
| **月报** | • 新地点数量<br>• 停留时长分布<br>• 访问城市数<br>• 运动类型占比 | • 3D 柱状图<br>• 轨迹动画<br>• 饼图分析 | ✅ 视频导出 |
| **年报** | • 城市覆盖率<br>• 勋章进度<br>• 里程碑时刻<br>• 年度之最 | • 交互式地图<br>• 数据仪表盘<br>• 时间轴 | ✅ 社交媒体分享 |

#### 报告亮点功能
- 📈 **智能数据洞察**：AI 分析您的移动模式，提供个性化建议
- 🎬 **动态轨迹回放**：以动画形式重现您的旅程
- 🏆 **成就时刻高光**：自动标注重要里程碑
- 📤 **一键分享**：生成精美海报分享至社交平台

---

### 🏆 地域勋章系统

#### 分级解锁机制

```
青铜 → 白银 → 黄金 → 铂金 → 钻石
 5次   10次   25次   50次   100次
```

#### 特色勋章分类

##### 🍜 美食探索家系列
- **解锁条件**：访问特色餐饮区域
- **勋章示例**：
  - 🍲 成都火锅达人（访问 10 家火锅店）
  - 🥟 西安小吃收集家（品尝 5 种传统小吃）
  - 🍵 杭州茶文化使者（参观 3 个茶园）

##### 🏺 文化守护者系列
- **解锁条件**：访问历史文化遗迹
- **勋章示例**：
  - 🏯 故宫深度游（参观 5 个宫殿）
  - 🗿 丝绸之路行者（打卡 10 个古迹）
  - 📚 博物馆爱好者（访问 20 个博物馆）

##### 🐼 生态观察员系列
- **解锁条件**：探索自然保护区
- **勋章示例**：
  - 🌲 国家公园探险家（访问 5 个国家公园）
  - 🦌 野生动物守护者（记录 10 次生态观察）
  - ⛰️ 登山爱好者（征服 5 座名山）

##### 🌆 城市漫步系列
- **解锁条件**：深度游览城市地标
- **勋章示例**：
  - 🌃 夜上海（夜晚打卡 10 个地标）
  - 🚇 地铁通勤达人（乘坐 50 个不同站点）
  - 🏙️ 摩天大楼收集者（登顶 10 座高楼）

#### 勋章特权

- 💰 **获取当地商家优惠**
  - 合作商户专属折扣（8-9 折）
  - 限定商品兑换资格
  - 会员优先预订权

- 🏅 **参与成就排行榜**
  - 全国/省份/城市多级榜单
  - 好友圈排名对比
  - 周/月/年度排行奖励

- 🎴 **分享专属纪念卡片**
  - 动态勋章展示卡片
  - AR 虚拟勋章墙
  - 个性化电子证书

---

## 🛠️ 技术架构

### 核心技术栈

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│  ┌─────────────────────────────────┐   │
│  │   Jetpack Compose (Material 3)  │   │
│  │   ViewModel + StateFlow         │   │
│  └─────────────────────────────────┘   │
├─────────────────────────────────────────┤
│            Domain Layer                 │
│  ┌─────────────────────────────────┐   │
│  │   Use Cases + Business Logic    │   │
│  │   Clean Architecture            │   │
│  └─────────────────────────────────┘   │
├─────────────────────────────────────────┤
│             Data Layer                  │
│  ┌──────────────┬──────────────────┐   │
│  │ Room Database│  GPS Service     │   │
│  │ (Encrypted)  │  (Location API)  │   │
│  └──────────────┴──────────────────┘   │
└─────────────────────────────────────────┘
```

### 主要依赖库

- **UI 框架**
  - [Jetpack Compose](https://developer.android.com/jetpack/compose) - 声明式 UI
  - [Material 3](https://m3.material.io/) - Material Design 3
  - [Accompanist](https://github.com/google/accompanist) - Compose 扩展库

- **架构组件**
  - [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) - 生命周期感知
  - [Room](https://developer.android.com/training/data-storage/room) - 数据库 ORM
  - [Hilt](https://dagger.dev/hilt/) - 依赖注入
  - [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) + [Flow](https://kotlinlang.org/docs/flow.html) - 异步处理

- **位置服务**
  - [Google Location Services](https://developers.google.com/location-context/fused-location-provider) - 融合定位
  - [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) - 后台任务

- **数据可视化**
  - [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) - 图表库
  - [Mapbox](https://www.mapbox.com/) / [高德地图](https://lbs.amap.com/) - 地图服务

- **网络与存储**
  - [Retrofit](https://square.github.io/retrofit/) - HTTP 客户端
  - [OkHttp](https://square.github.io/okhttp/) - 网络请求
  - [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) - 偏好存储
  - [SQLCipher](https://www.zetetic.net/sqlcipher/) - 数据库加密

---

## 📦 项目结构

```
Footprint/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/footprint/
│   │   │   │   ├── data/              # 数据层
│   │   │   │   │   ├── local/         # 本地数据源（Room）
│   │   │   │   │   ├── remote/        # 远程数据源（API）
│   │   │   │   │   ├── repository/    # 仓储实现
│   │   │   │   │   └── model/         # 数据模型
│   │   │   │   ├── domain/            # 业务逻辑层
│   │   │   │   │   ├── usecase/       # 用例
│   │   │   │   │   ├── repository/    # 仓储接口
│   │   │   │   │   └── model/         # 领域模型
│   │   │   │   ├── presentation/      # 表示层
│   │   │   │   │   ├── ui/            # UI 组件
│   │   │   │   │   │   ├── home/      # 主页面
│   │   │   │   │   │   ├── map/       # 地图页面
│   │   │   │   │   │   ├── report/    # 报告页面
│   │   │   │   │   │   └── badge/     # 勋章页面
│   │   │   │   │   ├── viewmodel/     # ViewModel
│   │   │   │   │   └── navigation/    # 导航
│   │   │   │   ├── service/           # 后台服务
│   │   │   │   │   └── location/      # 定位服务
│   │   │   │   ├── di/                # 依赖注入
│   │   │   │   └── utils/             # 工具类
│   │   │   └── res/                   # 资源文件
│   │   └── test/                      # 测试代码
│   └── build.gradle.kts
├── gradle/                            # Gradle 配置
├── .gitignore
├── README.md
└── LICENSE
```

---

## 🔧 开发环境搭建

### 前置要求

- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17 或更高版本
- Android SDK API 26+ (Android 8.0 Oreo)
- Gradle 8.6+

### 克隆项目

```bash
git clone https://github.com/yourusername/Footprint.git
cd Footprint
```

### 配置

1. **本地配置文件**

   创建 `local.properties` 文件（已在 .gitignore 中）：
   ```properties
   sdk.dir=/path/to/your/android-sdk
   ```

2. **API 密钥配置**（未来需要）

   在 `app/build.gradle.kts` 或环境变量中配置：
   ```kotlin
   // 地图服务密钥
   buildConfigField("String", "AMAP_API_KEY", "\"your_api_key\"")

   // 云服务密钥（可选）
   buildConfigField("String", "SERVER_URL", "\"https://your-api.com\"")
   ```

### 编译运行

```bash
# 编译项目
./gradlew build

# 安装到设备
./gradlew installDebug

# 运行测试
./gradlew test
```

---

## 📱 使用指南

### 快速开始

1. **首次启动**
   - 授予位置权限（必需）
   - 选择定位模式（手动/自动）
   - 完成引导教程

2. **开始记录**
   - 手动模式：点击主页"记录"按钮
   - 自动模式：应用将在后台持续记录

3. **查看数据**
   - 主页查看今日统计
   - 地图页查看轨迹
   - 报告页生成周/月/年报
   - 勋章页查看成就进度

### 权限说明

| 权限 | 用途 | 必需性 |
|------|------|--------|
| 位置权限 | 记录GPS轨迹 | ✅ 必需 |
| 后台位置 | 后台持续记录 | ⚠️ 可选（自动模式需要） |
| 存储权限 | 导出报告/导入数据 | ⚠️ 可选 |
| 网络权限 | 云同步/地图加载 | ⚠️ 可选 |

---

## 🗺️ 开发路线图

### Phase 1: MVP 核心功能 (v0.1.0) - 进行中

- [x] 项目初始化
- [x] 基础架构搭建
- [ ] GPS 定位服务实现
- [ ] 本地数据库设计
- [ ] 手动记录模式
- [ ] 基础轨迹展示

### Phase 2: 可视化与报告 (v0.2.0)

- [ ] 地图轨迹渲染
- [ ] 周报生成功能
- [ ] 数据图表展示
- [ ] 后台实时记录模式
- [ ] 运动类型识别

### Phase 3: 勋章系统 (v0.3.0)

- [ ] 勋章数据库设计
- [ ] POI 数据集成
- [ ] 地域匹配算法
- [ ] 勋章解锁逻辑
- [ ] 勋章展示页面

### Phase 4: 数据安全与同步 (v0.4.0)

- [ ] 数据加密存储
- [ ] 云端备份服务
- [ ] 多设备同步
- [ ] 数据导入/导出

### Phase 5: 社交与分享 (v0.5.0)

- [ ] 报告海报生成
- [ ] 社交平台分享
- [ ] 好友系统
- [ ] 排行榜功能

### Phase 6: 优化与完善 (v1.0.0)

- [ ] 性能优化
- [ ] 电池续航优化
- [ ] UI/UX 打磨
- [ ] 多语言支持
- [ ] 无障碍功能

---

## 🤝 贡献指南

我们欢迎任何形式的贡献！

### 如何贡献

1. **Fork 本仓库**
2. **创建特性分支** (`git checkout -b feature/AmazingFeature`)
3. **提交更改** (`git commit -m 'Add some AmazingFeature'`)
4. **推送到分支** (`git push origin feature/AmazingFeature`)
5. **开启 Pull Request**

### 代码规范

- 遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 使用有意义的变量/函数命名
- 添加必要的注释
- 编写单元测试

### 提交信息规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型：**
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具链更新

---

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

```
Copyright 2024 Footprint

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 📞 联系方式

- **问题反馈**：[GitHub Issues](https://github.com/yourusername/Footprint/issues)
- **功能建议**：[GitHub Discussions](https://github.com/yourusername/Footprint/discussions)
- **邮件联系**：footprint@example.com

---

## 🙏 致谢

感谢以下开源项目：

- [Android Jetpack](https://developer.android.com/jetpack)
- [Kotlin](https://kotlinlang.org/)
- [Material Design](https://material.io/)
- 以及所有依赖库的开发者们

---

<div align="center">

**⭐ 如果这个项目对您有帮助，请给我们一个 Star！⭐**

Made with ❤️ by Footprint Team

</div>
