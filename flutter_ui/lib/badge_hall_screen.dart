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
  final ValueNotifier<Offset> _lightOffsetNotifier = ValueNotifier(Offset.zero);
  
  late AnimationController _revealController;
  
  final Map<String, AnimationController> _badgeFocusControllers = {};

  @override
  void initState() {
    super.initState();
    _loadShader();
    
    _gyroSub = gyroscopeEvents.listen((GyroscopeEvent event) {
      if (!mounted) return;
      // Integrate gyro data into a fake tilt offset and keep updates local to badge shaders.
      _gyroOffset += Offset(event.y, event.x) * 0.1;
      _gyroOffset = Offset(
        _gyroOffset.dx * 0.95,
        _gyroOffset.dy * 0.95,
      );
      _pushLightOffset();
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

  void _pushLightOffset() {
    final next = _gyroOffset + _pointerOffset;
    final current = _lightOffsetNotifier.value;
    if ((next - current).distanceSquared < 0.0004) return;
    _lightOffsetNotifier.value = next;
  }

  @override
  void dispose() {
    _gyroSub?.cancel();
    _revealController.dispose();
    for (var controller in _badgeFocusControllers.values) {
        controller.dispose();
    }
    _lightOffsetNotifier.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_program == null) {
      return const Scaffold(
        backgroundColor: Color(0xFF1A110C),
        body: Center(child: CircularProgressIndicator(color: Color(0xFFD4AF37))),
      );
    }
    
    // Convert dictionary to structured lists
    List<Widget> slivers = [];
    slivers.add(
      SliverAppBar(
        backgroundColor: Colors.transparent,
        title: const Text("荣誉勋章", style: TextStyle(color: Color(0xFFE8D3A2), fontWeight: FontWeight.bold, letterSpacing: 4, fontFamily: 'serif')),
        iconTheme: const IconThemeData(color: Color(0xFFE8D3A2)),
        pinned: true,
        expandedHeight: 120,
        flexibleSpace: FlexibleSpaceBar(
          background: Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [const Color(0xFF150A05).withValues(alpha: 0.9), Colors.transparent],
              ),
            ),
          ),
        ),
      )
    );

    widget.badgeDictionary.forEach((category, items) {
      slivers.add(
        SliverToBoxAdapter(
          child: Column(
            children: [
              const SizedBox(height: 32),
              // Name Plate for the shelf
              Container(
                margin: const EdgeInsets.symmetric(horizontal: 40),
                padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 24),
                decoration: BoxDecoration(
                  color: const Color(0xFF2A1C14),
                  border: Border.all(color: const Color(0xFF8B6539), width: 1.5),
                  borderRadius: BorderRadius.circular(4),
                  boxShadow: [
                    BoxShadow(color: Colors.black.withValues(alpha: 0.5), blurRadius: 4, offset: const Offset(0, 2))
                  ]
                ),
                child: Text(
                  _translateCategory(category),
                  style: const TextStyle(
                    color: Color(0xFFE8D3A2),
                    fontSize: 16,
                    letterSpacing: 4,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(height: 24),
            ],
          )
        )
      );

      slivers.add(
        SliverPadding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          sliver: SliverGrid(
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              crossAxisSpacing: 12,
              mainAxisSpacing: 32,
              childAspectRatio: 0.65,
            ),
            delegate: SliverChildBuilderDelegate(
                (context, index) {
                final badge = items[index];
                final isUnlocked = widget.unlockedIds.contains(badge['badge_id']);
                return _buildBadgeItem(badge, isUnlocked, index);
              },
              childCount: items.length,
            ),
          ),
        )
      );

      // Shelf bottom edge
      slivers.add(
        SliverToBoxAdapter(
          child: Container(
            height: 20,
            margin: const EdgeInsets.only(top: 8),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Color(0xFF3E2723), // Wood top highlight
                  Color(0xFF1F1209), // Wood shadow
                ],
              ),
              border: const Border(
                top: BorderSide(color: Color(0xFF5D4037), width: 1),
                bottom: BorderSide(color: Colors.black, width: 2),
              ),
              boxShadow: [
                BoxShadow(color: Colors.black.withValues(alpha: 0.6), blurRadius: 10, offset: const Offset(0, 5))
              ]
            ),
          ),
        ),
      );
      
      slivers.add(const SliverToBoxAdapter(child: SizedBox(height: 10)));
    });

    // Handle Safe Area for the bottom
    slivers.add(
      SliverPadding(
        padding: EdgeInsets.only(bottom: MediaQuery.of(context).padding.bottom + 40),
      )
    );

    return Scaffold(
      backgroundColor: const Color(0xFF1E140A),
      body: Stack(
        children: [
          // Background wood texture/gradient for the cabinet
          Positioned.fill(
            child: Container(
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.centerLeft,
                  end: Alignment.centerRight,
                  colors: [
                    Color(0xFF150A05),
                    Color(0xFF2C1E16),
                    Color(0xFF150A05),
                  ],
                  stops: [0.0, 0.5, 1.0],
                ),
              ),
            ),
          ),
          
          MouseRegion(
            onHover: (e) {
              _pointerOffset = Offset(
                (e.localPosition.dx / MediaQuery.of(context).size.width - 0.5) * 2.0,
                (e.localPosition.dy / MediaQuery.of(context).size.height - 0.5) * 2.0,
              );
              _pushLightOffset();
            },
            child: CustomScrollView(
              physics: const BouncingScrollPhysics(),
              slivers: slivers,
            ),
          ),
          
          // Cabinet glass reflection overlay
          IgnorePointer(
            child: Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    Colors.white.withValues(alpha: 0.05),
                    Colors.transparent,
                    Colors.white.withValues(alpha: 0.02),
                    Colors.transparent,
                  ],
                  stops: const [0.0, 0.2, 0.8, 1.0],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBadgeItem(dynamic badge, bool isUnlocked, int index) {
    // 根据材质解析振动和动画质感
    final String materialType = badge['visual_meta']?['material'] ?? 'Base';
    
    // Calculate a staggered delay so they drop nicely for the initial visible items.
    // For items far down the list, TweenAnimationBuilder handles lazy-load insertion beautifully.
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: Duration(milliseconds: 600 + (index % 6) * 100),
      curve: Curves.easeOutBack,
      builder: (context, value, child) {
        // 下落感的位移和透明度
        double yOffset = (1.0 - value) * -40.0;
        double opacity = (value * 2).clamp(0.0, 1.0); // 快速淡入
        
        return Transform.translate(
          offset: Offset(0, yOffset),
          child: Transform.scale(
            scale: value.clamp(0.0, 1.2), // 防止过度反弹放大
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
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              Expanded(
                child: Stack(
                  alignment: Alignment.bottomCenter,
                  children: [
                    // Pedestal back support
                    Positioned(
                      bottom: 5,
                      child: Container(
                        width: 6,
                        height: 40,
                        decoration: const BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.centerLeft,
                            end: Alignment.centerRight,
                            colors: [Colors.black, Color(0xFF4A4A4A), Colors.black],
                          ),
                        ),
                      ),
                    ),
                    // Drop shadow on the pedestal base
                    Positioned(
                      bottom: 0,
                      child: Container(
                        width: 60,
                        height: 15,
                        decoration: BoxDecoration(
                          color: Colors.black.withValues(alpha: 0.6),
                          borderRadius: BorderRadius.circular(30),
                          boxShadow: [
                            BoxShadow(
                              color: isUnlocked 
                                  ? _parseColor(badge['visual_meta']?['color']).withAlpha(60)
                                  : Colors.black.withAlpha(80),
                              blurRadius: 15,
                              spreadRadius: 2,
                            ),
                          ],
                        ),
                      ),
                    ),
                    // Pedestal Base
                    Positioned(
                      bottom: 0,
                      child: Container(
                        width: 70,
                        height: 8,
                        decoration: BoxDecoration(
                          gradient: const LinearGradient(
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                            colors: [Color(0xFFD4AF37), Color(0xFF8B6508)], // Gold
                          ),
                          borderRadius: BorderRadius.circular(4),
                          boxShadow: [
                            BoxShadow(color: Colors.black.withValues(alpha: 0.8), offset: const Offset(0, 4), blurRadius: 4)
                          ]
                        ),
                      ),
                    ),
                    // The Badge itself
                    Align(
                      alignment: Alignment.topCenter,
                      child: Padding(
                        padding: const EdgeInsets.only(bottom: 15.0),
                        child: AspectRatio(
                          aspectRatio: 1.0,  // 强制正方形，避免拉伸成椭圆
                          child: ValueListenableBuilder<Offset>(
                            valueListenable: _lightOffsetNotifier,
                            builder: (context, lightOffset, _) => BadgeShaderWidget(
                              program: _program!,
                              isUnlocked: isUnlocked,
                              materialType: (materialType == 'Cyber' || materialType == 'cyber_neon') ? 1.0 :
                                            (materialType == 'Liquid' || materialType == 'liquid_glass') ? 2.0 :
                                            (materialType == 'Gold' || materialType == 'gold') ? 3.0 : 0.0,
                              baseColor: _parseColor(badge['visual_meta']?['color']),
                              lightOffset: lightOffset,
                              iconData: _getBadgeIcon(badge['visual_meta']?['icon']),
                              category: badge['category'] ?? 'General',
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
              // Name Plaque
              Container(
                height: 32, // 固定高度以保证上面 Expanded 计算对齐
                width: double.infinity,
                padding: const EdgeInsets.symmetric(horizontal: 2),
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: const Color(0xFF2A1C14), // Dark wood plaque
                  border: Border.all(color: const Color(0xFF5D4037), width: 1),
                  borderRadius: BorderRadius.circular(2),
                  boxShadow: [
                    BoxShadow(color: Colors.black.withValues(alpha: 0.5), blurRadius: 2, offset: const Offset(0, 1))
                  ]
                ),
                child: Text(
                  badge['title'],
                  style: TextStyle(
                    color: isUnlocked ? const Color(0xFFE8D3A2) : const Color(0xFF8B6539), // Gold text
                    fontSize: 10,
                    height: 1.1,
                    fontWeight: isUnlocked ? FontWeight.bold : FontWeight.normal,
                  ),
                  textAlign: TextAlign.center,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
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
      
      final String buffer = hex.replaceFirst('#', '');
      final color = Color(int.parse("FF$buffer", radix: 16));
      
      // Ensure color has adequate saturation and brightness for visibility
      // Boost very light/pastel colors by darkening them
      final HSLColor hsl = HSLColor.fromColor(color);
      if (hsl.lightness > 0.75) {
        // Too light — darken to ensure visibility on dark background
        return hsl.withLightness(0.55).toColor();
      }
      return color;
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
      
      // 新增勋章图标映射
      case 'baseline_nightlight_24': return Icons.nightlight_round; // 夜行者
      case 'baseline_schedule_24': return Icons.schedule; // 时间相关
      case 'baseline_alarm_on_24': return Icons.alarm_on; // 早起
      case 'baseline_local_fire_department_24': return Icons.local_fire_department; // 连续打卡
      case 'baseline_whatshot_24': return Icons.whatshot; // 连续记录
      case 'baseline_auto_awesome_24': return Icons.auto_awesome; // 多样探索
      case 'baseline_photo_camera_24': return Icons.photo_camera; // 摄影
      case 'baseline_edit_note_24': return Icons.edit_note; // 写作
      case 'baseline_emoji_events_24': return Icons.emoji_events; // 奖杯
      case 'baseline_rocket_launch_24': return Icons.rocket_launch; // 传奇
      case 'baseline_cake_24': return Icons.cake; // 周年纪念
      case 'baseline_celebration_24': return Icons.celebration; // 里程碑
      case 'baseline_speed_24': return Icons.speed; // 速度
      case 'baseline_timer_24': return Icons.timer; // 时长
      case 'baseline_calendar_month_24': return Icons.calendar_month; // 日历
      case 'baseline_favorite_24': return Icons.favorite; // 最爱
      case 'baseline_elevation_24': return Icons.terrain; // 海拔
      case 'baseline_snowshoeing_24': return Icons.snowshoeing; // 极寒
      case 'baseline_thunderstorm_24': return Icons.thunderstorm; // 风暴
      
      default: return Icons.stars;
    }
  }

  String _translateCategory(String category) {
    switch (category) {
      case 'Milestone':
        return '里 程 碑 成 就';
      case 'Geographic':
        return '地 域 足 迹';
      case 'Emotion':
        return '情 感 共 鸣';
      case 'Time':
        return '时 光 印 记';
      case 'Streak':
        return '毅 力 之 证';
      case 'Explorer':
        return '探 索 精 神';
      case 'Special':
        return '隐 藏 成 就';
      default:
        return '探 索 奖 章';
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
      case 'night_footprint_count':
        return "在夜间（22:00-6:00）记录了 $target 次足迹";
      case 'early_morning_count':
        return "在清晨（5:00-7:00）记录了 $target 次足迹";
      case 'consecutive_days':
        return "连续 $target 天记录足迹";
      case 'total_footprints':
        return "累计记录了 $target 条足迹";
      case 'photo_count':
        return "累计拍摄了 $target 张照片";
      case 'detail_char_count':
        return "累计撰写了 $target 字的足迹记录";
      case 'unique_cities':
        return "探索了 $target 个不同的城市";
      case 'provinces_visited':
        return "足迹覆盖 $target 个省级行政区";
      case 'max_altitude':
        return "到达过海拔 $target 米以上";
      case 'weather_snow_count':
        return "在雪天记录了 $target 次足迹";
      case 'weather_storm_count':
        return "在暴风雨中记录了 $target 次足迹";
      case 'total_duration_hours':
        return "累计记录足迹时长达到 $target 小时";
      case 'max_single_distance':
        return "单次足迹距离超过 $target 公里";
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
        final baseColor = _parseColor(badge['visual_meta']?['color']);

        return Center(
          child: Container(
            margin: const EdgeInsets.symmetric(horizontal: 24),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(40),
              child: BackdropFilter(
                filter: ui.ImageFilter.blur(sigmaX: 14, sigmaY: 14),
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
                                    blurRadius: 36,
                                    spreadRadius: 6,
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
          child: RepaintBoundary(
            child: Stack(
              fit: StackFit.expand,
              alignment: Alignment.center,
              children: [
                // 背景 3D 材质
                RepaintBoundary(
                  child: CustomPaint(
                    size: Size(constraints.maxWidth, constraints.maxHeight),
                    painter: BadgeShaderPainter(
                      program: program,
                      materialType: materialType,
                      isUnlocked: isUnlocked,
                      baseColor: baseColor,
                      lightOffset: Offset(-lightOffset.dx, -lightOffset.dy),
                    ),
                  ),
                ),
                
                // 对应的外部装饰框/边饰
                _buildCategoryFrame(constraints.maxWidth, displayColor),

                // 中心勋章 Icon
                Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: RepaintBoundary(
                      child: ShaderMask(
                        shaderCallback: (Rect bounds) {
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
                ),
              ],
            ),
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
