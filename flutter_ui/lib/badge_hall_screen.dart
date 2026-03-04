import 'dart:async';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/physics.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'badge_share_poster.dart' as poster;

class BadgeHallScreen extends StatefulWidget {
  final Map<String, dynamic> badgeDictionary;
  final List<String> unlockedIds;

  const BadgeHallScreen({
    super.key,
    required this.badgeDictionary,
    required this.unlockedIds,
  });

  @override
  State<BadgeHallScreen> createState() => _BadgeHallScreenState();
}

class _BadgeHallScreenState extends State<BadgeHallScreen> with TickerProviderStateMixin {
  ui.FragmentProgram? _program;
  StreamSubscription? _gyroSub;
  Offset _pointerOffset = Offset.zero;
  Offset _gyroOffset = Offset.zero;
  
  late AnimationController _revealController;
  
  final Map<String, AnimationController> _badgeFocusControllers = {};

  @override
  void initState() {
    super.initState();
    _loadShader();
    
    _gyroSub = gyroscopeEvents.listen((GyroscopeEvent event) {
      if (!mounted) return;
      setState(() {
        // Integrate gyro data into a fake tilt offset. 
        // Dampen it so it slowly returns to zero
        _gyroOffset += Offset(event.y, event.x) * 0.1;
        // Dampen
        _gyroOffset = Offset(
          _gyroOffset.dx * 0.95,
          _gyroOffset.dy * 0.95,
        );
      });
    });

    _revealController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    );
    _revealController.forward();
    
    for (var category in widget.badgeDictionary.keys) {
        List badges = widget.badgeDictionary[category] as List;
        for (var b in badges) {
            _badgeFocusControllers[b['badge_id']] = AnimationController(
              vsync: this,
              duration: const Duration(milliseconds: 300),
            );
        }
    }
  }

  Future<void> _loadShader() async {
    try {
      final program = await ui.FragmentProgram.fromAsset('shaders/badge_material.frag');
      if (mounted) {
        setState(() {
          _program = program;
        });
      }
    } catch (e) {
      debugPrint("Shader load failed: $e");
    }
  }

  @override
  void dispose() {
    _gyroSub?.cancel();
    _revealController.dispose();
    for (var controller in _badgeFocusControllers.values) {
        controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_program == null) {
      return const Scaffold(
        backgroundColor: Colors.black,
        body: Center(child: CircularProgressIndicator()),
      );
    }
    
    // Convert dictionary to structured lists
    List<Widget> slivers = [];
    slivers.add(
      SliverAppBar(
        backgroundColor: Colors.transparent,
        title: const Text("荣誉勋章", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, letterSpacing: 2)),
        iconTheme: const IconThemeData(color: Colors.white),
        pinned: true,
        expandedHeight: 120,
        flexibleSpace: FlexibleSpaceBar(
          background: Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [Colors.black.withValues(alpha: 0.8), Colors.transparent],
              ),
            ),
          ),
        ),
      )
    );

    widget.badgeDictionary.forEach((category, items) {
      slivers.add(
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 32, 24, 16),
            child: Text(
              _translateCategory(category),
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.6),
                fontSize: 14,
                letterSpacing: 4,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        )
      );

      slivers.add(
        SliverPadding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          sliver: SliverGrid(
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              crossAxisSpacing: 16,
              mainAxisSpacing: 20,
              childAspectRatio: 0.68,
            ),
            delegate: SliverChildBuilderDelegate(
                (context, index) {
                final badge = items[index];
                final isUnlocked = widget.unlockedIds.contains(badge['badge_id']);
                return Padding(
                  padding: EdgeInsets.only(bottom: index == items.length - 1 ? 40 : 0),
                  child: _buildBadgeItem(badge, isUnlocked, index),
                );
              },
              childCount: items.length,
            ),
          ),
        )
      );
    });

    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          // The Void Backdrop Spotlight
          Positioned(
            top: -200,
            left: MediaQuery.of(context).size.width / 2 - 300,
            child: Container(
              width: 600,
              height: 600,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    Colors.white.withValues(alpha: 0.1),
                    Colors.transparent
                  ],
                  stops: const [0.0, 1.0],
                ),
              ),
            ),
          ),
          
          MouseRegion(
            onHover: (e) {
              setState(() {
                _pointerOffset = Offset(
                  (e.localPosition.dx / MediaQuery.of(context).size.width - 0.5) * 2.0,
                  (e.localPosition.dy / MediaQuery.of(context).size.height - 0.5) * 2.0,
                );
              });
            },
            child: CustomScrollView(
              physics: const BouncingScrollPhysics(),
              slivers: slivers,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBadgeItem(dynamic badge, bool isUnlocked, int index) {
    // 根据材质解析质量
    final String materialType = badge['visual_meta']?['material'] ?? 'Base';
    double mass = 1.0;
    double stiffness = 150.0;
    double damping = 15.0;

    if (materialType == 'Cyber') {
      mass = 0.5;
      stiffness = 200.0;
      damping = 10.0;
    } else if (materialType == 'Liquid') {
      mass = 0.8;
      stiffness = 100.0;
      damping = 8.0;
    } else if (materialType == 'Heavy') {
      mass = 2.0;
      stiffness = 300.0;
      damping = 20.0;
    }

    final springSimulation = SpringSimulation(
      SpringDescription(mass: mass, stiffness: stiffness, damping: damping),
      0.0, // Initial position (starts off-screen top)
      1.0,  // Target position (lands at 1.0)
      0.0,   // Initial velocity
    );

    return AnimatedBuilder(
      animation: _revealController,
      builder: (context, child) {
        // 使用弹簧模拟器和交错时间，计算每个勋章当前的Y轴进度
        double t = _revealController.value;
        double delay = index * 0.05;
        double progress = 0.0;
        
        if (t > delay) {
             // 缩放时间到弹簧模拟所需的时间范围，比如整个动画 1.5 秒
             double simTime = (t - delay) * 1.5; 
             progress = springSimulation.x(simTime);
        }

        double scale = Curves.easeOutBack.transform((t * 2 - delay).clamp(0.0, 1.0));
        double opacity = t > delay ? Curves.easeIn.transform(((t - delay) * 5).clamp(0.0, 1.0)) : 0.0;
        
        // 我们利用进度差值制造下落感 (-50 代表距离终点上方 50 像素)
        double yOffset = (1.0 - progress) * -80.0;

        return Transform.translate(
          offset: Offset(0, yOffset),
          child: Transform.scale(
            scale: scale,
            child: Opacity(opacity: opacity, child: child),
          ),
        );
      },
      child: GestureDetector(
        onTapDown: (_) {
          if (isUnlocked) {
              _badgeFocusControllers[badge['badge_id']]?.forward();
              _triggerHaptic(materialType);
          }
        },
        onTapUp: (_) => _badgeFocusControllers[badge['badge_id']]?.reverse(),
        onTapCancel: () => _badgeFocusControllers[badge['badge_id']]?.reverse(),
        onTap: () {
          if (isUnlocked) {
            _showBadgeDetailOverlay(badge, materialType);
          } else {
            HapticFeedback.vibrate();
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text("未解锁：${_getConditionDescription(badge)}")),
            );
          }
        },
        child: AnimatedBuilder(
          animation: _badgeFocusControllers[badge['badge_id']]!,
          builder: (context, child) {
             double focusScale = 1.0 - _badgeFocusControllers[badge['badge_id']]!.value * 0.05;
             return Transform.scale(scale: focusScale, child: child);
          },
          child: Column(
            children: [
              Expanded(
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    // 底部的环境阴影，增加悬浮感
                    Positioned(
                      bottom: 10,
                      child: Container(
                        width: 40,
                        height: 10,
                        decoration: BoxDecoration(
                          boxShadow: [
                            BoxShadow(
                              color: isUnlocked 
                                  ? _parseColor(badge['visual_meta']?['base_color']).withAlpha(40)
                                  : Colors.black.withAlpha(100),
                              blurRadius: 20,
                              spreadRadius: 8,
                            ),
                          ],
                        ),
                      ),
                    ),
                    BadgeShaderWidget(
                      program: _program!,
                      isUnlocked: isUnlocked,
                      materialType: (materialType == 'Cyber' || materialType == 'cyber_neon') ? 1.0 :
                                    (materialType == 'Liquid' || materialType == 'liquid_glass') ? 2.0 :
                                    (materialType == 'Gold' || materialType == 'gold') ? 3.0 : 0.0,
                      baseColor: _parseColor(badge['visual_meta']?['base_color']),
                      lightOffset: _gyroOffset + _pointerOffset,
                      iconData: _getBadgeIcon(badge['visual_meta']?['icon']),
                      category: badge['category'] ?? 'General',
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                height: 36,
                child: Center(
                  child: Text(
                    badge['title'],
                    style: TextStyle(
                      color: isUnlocked ? Colors.white : Colors.white.withAlpha(120),
                      fontSize: 11,
                      height: 1.1,
                      fontWeight: isUnlocked ? FontWeight.bold : FontWeight.normal,
                    ),
                    textAlign: TextAlign.center,
                    maxLines: 2,
                    overflow: TextOverflow.visible,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _triggerHaptic(String materialType) {
      if (materialType == 'Heavy') {
          HapticFeedback.heavyImpact();
      } else if (materialType == 'Liquid' || materialType == 'Cyber') {
          HapticFeedback.mediumImpact();
          Future.delayed(const Duration(milliseconds: 50), () => HapticFeedback.lightImpact());
          Future.delayed(const Duration(milliseconds: 100), () => HapticFeedback.lightImpact());
      } else {
          HapticFeedback.lightImpact();
      }
  }

  Color _parseColor(String? hex) {
    try {
      if (hex == null || hex.isEmpty) return Colors.grey;
      
      // Specially boost target colors for Hunan (Red) and Guangdong (Teal)
      // Using deeper, richer colors to avoid the "shallow" or "washed out" appearance
      if (hex.toUpperCase() == "#E53935") return const Color(0xFFC62828); // Deep Crimson
      if (hex.toUpperCase() == "#009688") return const Color(0xFF00695C); // Deep Jade Teal
      
      final String buffer = hex.replaceFirst('#', '');
      return Color(int.parse("FF$buffer", radix: 16));
    } catch (_) {
      return Colors.blueAccent;
    }
  }

  IconData _getBadgeIcon(String? iconName) {
    if (iconName == null) return Icons.stars;
    
    // 映射 badges_config.json 中的原生 icon 名称到 Flutter Icons
    switch (iconName) {
      case 'baseline_directions_run_24': return Icons.directions_run;
      case 'baseline_explore_24': return Icons.explore;
      case 'baseline_flight_takeoff_24': return Icons.flight_takeoff;
      case 'baseline_diamond_24': return Icons.diamond;
      case 'baseline_account_balance_24': return Icons.account_balance;
      case 'baseline_business_24': return Icons.business;
      case 'baseline_local_florist_24': return Icons.local_florist;
      case 'baseline_memory_24': return Icons.memory;
      case 'baseline_umbrella_24': return Icons.umbrella;
      case 'baseline_wb_sunny_24': return Icons.wb_sunny;
      
      // 地域特色图标映射
      case 'beijing_icon': return Icons.temple_hindu; // 北京：故宫/神庙意象
      case 'shanghai_icon': return Icons.apartment; // 上海：东方明珠/摩天大楼
      case 'tianjin_icon': return Icons.fort; // 天津：望海楼/之眼
      case 'chongqing_icon': return Icons.terrain; // 重庆：山城
      case 'hebei_icon': return Icons.landscape; // 河北：长城/关隘
      case 'shanxi_icon': return Icons.museum; // 山西：古建/大院
      case 'liaoning_icon': return Icons.precision_manufacturing; // 辽宁：重工业
      case 'jilin_icon': return Icons.ac_unit; // 吉林：雾凇/长白山
      case 'heilongjiang_icon': return Icons.architecture; // 黑龙江：圣索菲亚
      case 'jiangsu_icon': return Icons.waves; // 江苏：水乡/园林
      case 'zhejiang_icon': return Icons.water; // 浙江：西湖/诗画
      case 'anhui_icon': return Icons.home_work; // 安徽：徽派建筑
      case 'fujian_icon': return Icons.nature; // 福建：土楼/武夷山
      case 'jiangxi_icon': return Icons.terrain; // 江西：庐山/井冈山
      case 'shandong_icon': return Icons.hiking; // 山东：泰山/礼仪
      case 'henan_icon': return Icons.history_edu; // 河南：中原/甲骨文
      case 'hubei_icon': return Icons.alt_route; // 湖北：九省通衢/黄鹤楼
      case 'hunan_icon': return Icons.local_fire_department; // 湖南：热辣/湘江
      case 'guangdong_icon': return Icons.business_center; // 广东：岭南/商贸
      case 'hainan_icon': return Icons.beach_access; // 海南：椰风海韵
      case 'sichuan_icon': return Icons.pets; // 四川：熊猫/锦绣
      case 'guizhou_icon': return Icons.filter_hdr; // 贵州：黄果树/苗岭
      case 'yunnan_icon': return Icons.cloud_done; // 云南：彩云之南
      case 'shaanxi_icon': return Icons.castle; // 陕西：兵马俑/古城
      case 'gansu_icon': return Icons.grain; // 甘肃：丝绸之路/沙漠
      case 'qinghai_icon': return Icons.water_drop; // 青海：三江源
      case 'taiwan_icon': return Icons.landscape; // 台湾：阿里山
      case 'neimenggu_icon': return Icons.grass; // 内蒙古：草原
      case 'guangxi_icon': return Icons.kayaking; // 广西：桂林山水
      case 'xizang_icon': return Icons.landscape; // 西藏：珠峰/布宫
      case 'ningxia_icon': return Icons.agriculture; // 宁夏：塞上江南/枸杞
      case 'xinjiang_icon': return Icons.park; // 新疆：天山/胡杨
      case 'hongkong_icon': return Icons.nightlife; // 香港：维港/霓虹
      case 'macau_icon': return Icons.casino; // 澳门：博彩/大三巴
      
      default: return Icons.stars;
    }
  }

  String _translateCategory(String category) {
    switch (category) {
      case 'Milestone':
        return '里程碑成就';
      case 'Geographic':
        return '地域足迹';
      case 'Emotion':
        return '情感共鸣';
      default:
        return '探索奖章';
    }
  }

  String _getConditionDescription(dynamic badge) {
    String key = badge['condition_key'] ?? '';
    String target = badge['target_value']?.toString() ?? '0';
    
    switch (key) {
      case 'total_mileage':
        return "累计探索距离达到 $target 公里";
      case 'adcode':
        final adcodeMap = {
          '110000': '北京', '120000': '天津', '130000': '河北', '140000': '山西',
          '150000': '内蒙古', '210000': '辽宁', '220000': '吉林', '230000': '黑龙江',
          '310000': '上海', '320000': '江苏', '330000': '浙江', '340000': '安徽',
          '350000': '福建', '360000': '江西', '370000': '山东', '410000': '河南',
          '420000': '湖北', '430000': '湖南', '440000': '广东', '450000': '广西',
          '460000': '海南', '500000': '重庆', '510000': '四川', '520000': '贵州',
          '530000': '云南', '540000': '西藏', '610000': '陕西', '620000': '甘肃',
          '630000': '青海', '640000': '宁夏', '650000': '新疆', '710000': '台湾',
          '810000': '香港', '820000': '澳门'
        };
        String region = adcodeMap[target] ?? "特定地区";
        return "在 $region 留下过探索足迹";
      case 'weather_rainy_count':
        return "在雨天记录了 $target 次足迹";
      case 'weather_sunny_count':
        return "在晴天记录了 $target 次足迹";
      default:
        return "勋章解锁进度：$key 到达 $target";
    }
  }

  void _showBadgeDetailOverlay(dynamic badge, String materialType) {
    _triggerHaptic(materialType);
    
    // 激光蚀刻效果展示 - Laser Engraving Scene
    showGeneralDialog(
      context: context,
      barrierDismissible: true,
      barrierLabel: "Dismiss",
      barrierColor: Colors.black54,
      transitionDuration: const Duration(milliseconds: 400),
      pageBuilder: (context, animation, secondaryAnimation) {
        final catCN = _translateCategory((badge['category'] ?? 'Other').toString());
        final baseColor = _parseColor(badge['visual_meta']?['base_color']);

        return Center(
          child: Container(
            margin: const EdgeInsets.symmetric(horizontal: 24),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(40),
              child: BackdropFilter(
                filter: ui.ImageFilter.blur(sigmaX: 20, sigmaY: 20),
                child: Material(
                  color: Colors.white.withValues(alpha: 0.1),
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(40),
                      border: Border.all(color: Colors.white.withValues(alpha: 0.2), width: 1.5),
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [
                          Colors.white.withValues(alpha: 0.15),
                          Colors.white.withValues(alpha: 0.05),
                        ],
                      ),
                    ),
                    padding: const EdgeInsets.fromLTRB(24, 40, 24, 24),
                    child: Stack(
                      children: [
                        Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            // 勋章旋转展示容器
                            Container(
                              height: 240,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                boxShadow: [
                                  BoxShadow(
                                    color: baseColor.withValues(alpha: 0.4),
                                    blurRadius: 60,
                                    spreadRadius: 10,
                                  ),
                                ],
                              ),
                              child: BadgeShaderWidget(
                                program: _program!,
                                isUnlocked: true,
                                materialType: (materialType == 'Cyber' || materialType == 'cyber_neon') ? 1.0 :
                                              (materialType == 'Liquid' || materialType == 'liquid_glass') ? 2.0 :
                                              (materialType == 'Gold' || materialType == 'gold') ? 3.0 : 0.0,
                                baseColor: baseColor,
                                lightOffset: const Offset(0, 0),
                                iconData: _getBadgeIcon(badge['visual_meta']?['icon']),
                                category: badge['category'] ?? 'General',
                              ),
                            ),
                            const SizedBox(height: 32),
                            // 标题
                            Text(
                              badge['title'],
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 34,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 2,
                              ),
                              textAlign: TextAlign.center,
                            ),
                            const SizedBox(height: 8),
                            // 分类标签
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                              decoration: BoxDecoration(
                                color: baseColor.withValues(alpha: 0.2),
                                borderRadius: BorderRadius.circular(30),
                                border: Border.all(color: baseColor.withValues(alpha: 0.4)),
                              ),
                              child: Text(
                                catCN,
                                style: TextStyle(color: baseColor, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 2),
                              ),
                            ),
                            const SizedBox(height: 24),
                            // 描述
                            Text(
                              badge['description'],
                              style: TextStyle(
                                color: Colors.white.withValues(alpha: 0.9),
                                fontSize: 17,
                                height: 1.6,
                                letterSpacing: 0.5,
                              ),
                              textAlign: TextAlign.center,
                            ),
                            const SizedBox(height: 32),
                            // 解锁目标卡片
                            Container(
                              width: double.infinity,
                              padding: const EdgeInsets.all(20),
                              decoration: BoxDecoration(
                                color: Colors.black.withValues(alpha: 0.3),
                                borderRadius: BorderRadius.circular(24),
                                border: Border.all(color: Colors.white12),
                              ),
                              child: Column(
                                children: [
                                  const Text(
                                    "✨ 达成要求",
                                    style: TextStyle(color: Colors.white54, fontSize: 13, fontWeight: FontWeight.bold),
                                  ),
                                  const SizedBox(height: 12),
                                  Text(
                                    _getConditionDescription(badge),
                                    style: const TextStyle(
                                      color: Colors.white,
                                      fontSize: 15,
                                      height: 1.4,
                                    ),
                                    textAlign: TextAlign.center,
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(height: 32),
                            // 底部操作区
                            Row(
                              children: [
                                Expanded(
                                  child: TextButton(
                                    onPressed: () => Navigator.pop(context),
                                    style: TextButton.styleFrom(
                                      padding: const EdgeInsets.symmetric(vertical: 16),
                                      foregroundColor: Colors.white60,
                                    ),
                                    child: const Text("返回"),
                                  ),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  flex: 2,
                                  child: ElevatedButton.icon(
                                    onPressed: () {
                                      Navigator.of(context).pop();
                                      if (_program != null) {
                                        poster.shareBadgePoster(
                                          context: this.context,
                                          program: _program!,
                                          badge: badge as Map<String, dynamic>,
                                        );
                                      }
                                    },
                                    icon: const Icon(Icons.share_rounded, size: 20),
                                    label: const Text('生成分享海报'),
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: baseColor,
                                      foregroundColor: Colors.white,
                                      elevation: 8,
                                      shadowColor: baseColor.withValues(alpha: 0.5),
                                      padding: const EdgeInsets.symmetric(vertical: 16),
                                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

class BadgeShaderWidget extends StatelessWidget {
  final ui.FragmentProgram program;
  final bool isUnlocked;
  final double materialType;
  final Color baseColor;
  final Offset lightOffset;
  final IconData iconData;
  final String category;

  const BadgeShaderWidget({
    super.key,
    required this.program,
    required this.isUnlocked,
    required this.materialType,
    required this.baseColor,
    required this.lightOffset,
    required this.iconData,
    required this.category,
  });

  @override
  Widget build(BuildContext context) {
    // 构建 3D 透视矩阵
    final Matrix4 depthMatrix = Matrix4.identity()
      ..setEntry(3, 2, 0.001) // 焦距深度 (Z轴透视收缩)
      ..rotateX(-lightOffset.dy * 0.5) // 绑定陀螺仪俯仰 (Pitch)
      ..rotateY(lightOffset.dx * 0.5); // 绑定陀螺仪横滚 (Roll)

    // 装饰色：未解锁则为暗灰色
    final Color displayColor = isUnlocked ? baseColor : Colors.white.withAlpha(20);

    return LayoutBuilder(
      builder: (context, constraints) {
        return Transform(
          transform: depthMatrix,
          alignment: FractionalOffset.center,
          child: Stack(
            fit: StackFit.expand,
            alignment: Alignment.center,
            children: [
              // 背景 3D 材质
              CustomPaint(
                size: Size(constraints.maxWidth, constraints.maxHeight),
                painter: BadgeShaderPainter(
                  program: program,
                  materialType: materialType,
                  isUnlocked: isUnlocked,
                  baseColor: baseColor,
                  lightOffset: Offset(-lightOffset.dx, -lightOffset.dy),
                ),
              ),
              
              // 对应的外部装饰框/边饰
              _buildCategoryFrame(constraints.maxWidth, displayColor),

              // 中心勋章 Icon
              Center(
                child: Padding(
                  padding: const EdgeInsets.all(24.0),
                  child: ShaderMask(
                    shaderCallback: (Rect bounds) {
                      // 恢复较高的曝光度以解决“显示太浅”的问题
                      final Color iconTopColor = isUnlocked 
                          ? Colors.white.withAlpha(255) 
                          : Colors.white.withAlpha(80);
                      final Color iconBottomColor = isUnlocked 
                          ? displayColor.withAlpha(100) 
                          : Colors.transparent;
                      
                      return ui.Gradient.linear(
                        Offset(bounds.width * 0.5 + lightOffset.dx * 10, 0),
                        Offset(bounds.width * 0.5 - lightOffset.dx * 10, bounds.height),
                        [iconTopColor, iconBottomColor],
                      );
                    },
                    child: Stack(
                      children: [
                        // Subtle drop shadow for icon visibility
                        Positioned(
                          top: 1.5,
                          left: 1.5,
                          child: Icon(
                            iconData,
                            size: constraints.maxWidth * 0.4,
                            color: Colors.black.withAlpha(120),
                          ),
                        ),
                        Icon(
                          iconData,
                          size: constraints.maxWidth * 0.4,
                          color: Colors.white,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildCategoryFrame(double size, Color displayColor) {
    if (category == 'Milestone') {
      // 里程碑：外圈加一圈星形装饰或光环
      return Center(
        child: Container(
          width: size * 0.9,
          height: size * 0.9,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(
              color: displayColor.withAlpha(isUnlocked ? 120 : 30),
              width: 1.5,
            ),
            boxShadow: isUnlocked ? [
              BoxShadow(
                color: displayColor.withAlpha(40),
                blurRadius: 10,
                spreadRadius: 1,
              )
            ] : null,
          ),
        ),
      );
    } else if (category == 'Geographic') {
      // 地理：移除刻度点，改为纯净显示
      return const SizedBox.shrink();
    } else if (category == 'Emotion') {
      // 情感/天气：柔和的圆角光晕
      return Center(
        child: Container(
          width: size * 0.85,
          height: size * 0.85,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: displayColor.withAlpha(80),
                blurRadius: 15,
                spreadRadius: 2,
              )
            ],
          ),
        ),
      );
    }
    return const SizedBox.shrink();
  }
}

class BadgeShaderPainter extends CustomPainter {
  final ui.FragmentProgram program;
  final double materialType;
  final bool isUnlocked;
  final Color baseColor;
  final Offset lightOffset;

  BadgeShaderPainter({
    required this.program,
    required this.materialType,
    required this.isUnlocked,
    required this.baseColor,
    required this.lightOffset,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final shader = program.fragmentShader();

    // uniforms
    // uniform vec2 resolution;
    // uniform vec3 lightPos;
    // uniform vec4 baseColor;
    // uniform float materialType;
    
    shader.setFloat(0, size.width);
    shader.setFloat(1, size.height);
    
    // light pos
    shader.setFloat(2, lightOffset.dx * 2.0);
    shader.setFloat(3, lightOffset.dy * 2.0);
    shader.setFloat(4, 1.5); // z height
    
    // color
    shader.setFloat(5, baseColor.r);
    shader.setFloat(6, baseColor.g);
    shader.setFloat(7, baseColor.b);
    shader.setFloat(8, baseColor.a);
    
    // type
    shader.setFloat(9, materialType);
    
    // unlocked
    shader.setFloat(10, isUnlocked ? 1.0 : 0.0);

    final paint = Paint()..shader = shader;
    
    // Draw the shader rect
    canvas.drawRect(Offset.zero & size, paint);
  }

  @override
  bool shouldRepaint(covariant BadgeShaderPainter oldDelegate) {
    return oldDelegate.lightOffset != lightOffset ||
           oldDelegate.baseColor != baseColor ||
           oldDelegate.materialType != materialType ||
           oldDelegate.isUnlocked != isUnlocked;
  }
}
