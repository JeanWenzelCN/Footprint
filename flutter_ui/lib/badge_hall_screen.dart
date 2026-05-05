import 'dart:math' as math;
import 'dart:async';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/physics.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'badge_share_poster.dart' as poster;

class _BadgeVisualSpec {
  final List<Color> ribbonColors;
  final List<Color> outerRingColors;
  final List<Color> innerRingColors;
  final List<Color> connectorColors;
  final List<Color> plinthColors;
  final List<Color> standColors;
  final List<Color> nameplateColors;
  final Color glowColor;
  final Color edgeStroke;
  final Color titleColor;
  final String materialLabel;
  final String seriesLabel;

  const _BadgeVisualSpec({
    required this.ribbonColors,
    required this.outerRingColors,
    required this.innerRingColors,
    required this.connectorColors,
    required this.plinthColors,
    required this.standColors,
    required this.nameplateColors,
    required this.glowColor,
    required this.edgeStroke,
    required this.titleColor,
    required this.materialLabel,
    required this.seriesLabel,
  });
}

class _CabinetStyleSpec {
  final String label;
  final IconData icon;
  final Color backgroundTop;
  final Color backgroundMid;
  final Color backgroundBottom;
  final Color panelColor;
  final Color panelBorder;
  final Color shelfTop;
  final Color shelfBottom;
  final Color shelfEdge;
  final Color titleColor;
  final Color subtitleColor;
  final Color accentColor;

  const _CabinetStyleSpec({
    required this.label,
    required this.icon,
    required this.backgroundTop,
    required this.backgroundMid,
    required this.backgroundBottom,
    required this.panelColor,
    required this.panelBorder,
    required this.shelfTop,
    required this.shelfBottom,
    required this.shelfEdge,
    required this.titleColor,
    required this.subtitleColor,
    required this.accentColor,
  });
}

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
  int _cabinetStyleIndex = 0;

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
        backgroundColor: Color(0xFFF4EFE7),
        body: Center(child: CircularProgressIndicator(color: Color(0xFF163A59))),
      );
    }
    final cabinet = _currentCabinetStyle;
    
    // Convert dictionary to structured lists
    List<Widget> slivers = [];
    slivers.add(
      SliverAppBar(
        backgroundColor: Colors.transparent,
        title: Text("荣誉勋章", style: TextStyle(color: cabinet.titleColor, fontWeight: FontWeight.w900)),
        iconTheme: IconThemeData(color: cabinet.titleColor),
        pinned: true,
        expandedHeight: 154,
        flexibleSpace: FlexibleSpaceBar(
          background: Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [cabinet.panelColor.withValues(alpha: 0.84), Colors.transparent],
              ),
            ),
          ),
        ),
      )
    );
    slivers.add(
      SliverToBoxAdapter(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
          child: Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: cabinet.panelColor.withOpacity(0.92),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: cabinet.panelBorder),
              boxShadow: [
                BoxShadow(
                  color: cabinet.shelfEdge.withOpacity(0.14),
                  blurRadius: 20,
                  offset: const Offset(0, 12),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      width: 52,
                      height: 52,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: [cabinet.shelfBottom, cabinet.accentColor],
                        ),
                        borderRadius: BorderRadius.circular(16),
                        boxShadow: [
                          BoxShadow(
                            color: cabinet.accentColor.withOpacity(0.28),
                            blurRadius: 14,
                            offset: const Offset(0, 6),
                          ),
                        ],
                      ),
                      child: const Icon(Icons.workspace_premium_outlined, color: Colors.white),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            "勋章陈列馆",
                            style: TextStyle(
                              color: cabinet.titleColor,
                              fontWeight: FontWeight.w900,
                              fontSize: 16,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            "把已经跨过的边界、抵达过的节点和特别时刻，像收藏品一样安放在这里。",
                            style: TextStyle(
                              color: cabinet.subtitleColor,
                              height: 1.45,
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                _buildCabinetSelector(cabinet),
              ],
            ),
          ),
        ),
      ),
    );

    widget.badgeDictionary.forEach((category, items) {
      slivers.add(
        SliverToBoxAdapter(
          child: Column(
            children: [
              const SizedBox(height: 32),
              Container(
                margin: const EdgeInsets.symmetric(horizontal: 40),
                padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 24),
                decoration: BoxDecoration(
                  color: cabinet.panelColor.withOpacity(0.9),
                  border: Border.all(color: cabinet.panelBorder, width: 1.2),
                  borderRadius: BorderRadius.circular(12),
                  boxShadow: [
                    BoxShadow(color: cabinet.shelfEdge.withValues(alpha: 0.14), blurRadius: 12, offset: const Offset(0, 4))
                  ]
                ),
                child: Text(
                  _translateCategory(category),
                  style: TextStyle(
                    color: cabinet.titleColor,
                    fontSize: 15,
                    fontWeight: FontWeight.w800,
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
              mainAxisExtent: 252,
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

      slivers.add(
        SliverToBoxAdapter(
          child: _buildCabinetShelf(cabinet),
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
      backgroundColor: cabinet.backgroundMid,
      body: Stack(
        children: [
          Positioned.fill(child: _buildCabinetBackground(cabinet)),
          
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
          
          IgnorePointer(
            child: Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    Colors.white.withValues(alpha: 0.12),
                    Colors.transparent,
                    cabinet.accentColor.withValues(alpha: 0.08),
                    Colors.transparent,
                  ],
                  stops: const [0.0, 0.22, 0.76, 1.0],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  List<_CabinetStyleSpec> get _cabinetStyles => const [
    _CabinetStyleSpec(
      label: '胡桃木',
      icon: Icons.shelves,
      backgroundTop: Color(0xFFF8F1E6),
      backgroundMid: Color(0xFFEFE1CF),
      backgroundBottom: Color(0xFFDCC5A9),
      panelColor: Color(0xFFFFFAF1),
      panelBorder: Color(0xFFD7B98E),
      shelfTop: Color(0xFFE7C18B),
      shelfBottom: Color(0xFF8B5B35),
      shelfEdge: Color(0xFF5D3720),
      titleColor: Color(0xFF2D2219),
      subtitleColor: Color(0xFF715E4D),
      accentColor: Color(0xFFC38B4B),
    ),
    _CabinetStyleSpec(
      label: '黑曜展柜',
      icon: Icons.diamond_outlined,
      backgroundTop: Color(0xFF20242A),
      backgroundMid: Color(0xFF101419),
      backgroundBottom: Color(0xFF06080B),
      panelColor: Color(0xFF242A31),
      panelBorder: Color(0xFF58616D),
      shelfTop: Color(0xFF7D8797),
      shelfBottom: Color(0xFF222934),
      shelfEdge: Color(0xFF05070A),
      titleColor: Color(0xFFF4F1E8),
      subtitleColor: Color(0xFFB5BEC9),
      accentColor: Color(0xFF81B7C9),
    ),
    _CabinetStyleSpec(
      label: '青玉玻璃',
      icon: Icons.view_in_ar_outlined,
      backgroundTop: Color(0xFFEAF7F1),
      backgroundMid: Color(0xFFD4E7E0),
      backgroundBottom: Color(0xFFAEC9C0),
      panelColor: Color(0xFFF4FFFB),
      panelBorder: Color(0xFF8CB9AA),
      shelfTop: Color(0xFFB9D9CB),
      shelfBottom: Color(0xFF456E62),
      shelfEdge: Color(0xFF294840),
      titleColor: Color(0xFF183C34),
      subtitleColor: Color(0xFF55736A),
      accentColor: Color(0xFF3D9B86),
    ),
  ];

  _CabinetStyleSpec get _currentCabinetStyle => _cabinetStyles[_cabinetStyleIndex % _cabinetStyles.length];

  Widget _buildCabinetSelector(_CabinetStyleSpec active) {
    return Row(
      children: List.generate(_cabinetStyles.length, (index) {
        final style = _cabinetStyles[index];
        final selected = index == _cabinetStyleIndex;
        return Expanded(
          child: Padding(
            padding: EdgeInsets.only(right: index == _cabinetStyles.length - 1 ? 0 : 8),
            child: InkWell(
              borderRadius: BorderRadius.circular(10),
              onTap: () => setState(() => _cabinetStyleIndex = index),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 220),
                height: 38,
                decoration: BoxDecoration(
                  gradient: selected
                      ? LinearGradient(colors: [style.shelfBottom, style.accentColor])
                      : null,
                  color: selected ? null : style.panelColor.withOpacity(0.72),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(
                    color: selected ? style.accentColor.withOpacity(0.8) : active.panelBorder.withOpacity(0.55),
                  ),
                  boxShadow: selected
                      ? [
                          BoxShadow(
                            color: style.accentColor.withOpacity(0.22),
                            blurRadius: 12,
                            offset: const Offset(0, 5),
                          ),
                        ]
                      : null,
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(style.icon, size: 15, color: selected ? Colors.white : active.titleColor.withOpacity(0.72)),
                    const SizedBox(width: 5),
                    Flexible(
                      child: Text(
                        style.label,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: selected ? Colors.white : active.titleColor.withOpacity(0.76),
                          fontSize: 11,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      }),
    );
  }

  Widget _buildCabinetBackground(_CabinetStyleSpec cabinet) {
    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            cabinet.backgroundTop,
            cabinet.backgroundMid,
            cabinet.backgroundBottom,
          ],
        ),
      ),
      child: CustomPaint(
        painter: _CabinetTexturePainter(cabinet),
      ),
    );
  }

  Widget _buildCabinetShelf(_CabinetStyleSpec cabinet) {
    return Container(
      height: 24,
      margin: const EdgeInsets.only(top: 8),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            cabinet.shelfTop,
            cabinet.shelfBottom,
          ],
        ),
        border: Border(
          top: BorderSide(color: cabinet.shelfTop.withOpacity(0.9), width: 1),
          bottom: BorderSide(color: cabinet.shelfEdge, width: 2),
        ),
        boxShadow: [
          BoxShadow(
            color: cabinet.shelfEdge.withValues(alpha: 0.32),
            blurRadius: 12,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Align(
        alignment: Alignment.topCenter,
        child: Container(
          height: 4,
          margin: const EdgeInsets.symmetric(horizontal: 18),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.18),
            borderRadius: BorderRadius.circular(999),
          ),
        ),
      ),
    );
  }

  Widget _buildBadgeItem(dynamic badge, bool isUnlocked, int index) {
    final String materialType = badge['visual_meta']?['material'] ?? 'Base';
    final Color baseColor = _parseColor(badge['visual_meta']?['color']);
    final _BadgeVisualSpec spec = _buildBadgeVisualSpec(
      materialType: materialType,
      baseColor: baseColor,
      category: (badge['category'] ?? 'General').toString(),
      unlocked: isUnlocked,
    );
    
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: Duration(milliseconds: 600 + (index % 6) * 100),
      curve: Curves.easeOutBack,
      builder: (context, value, child) {
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
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                height: 172,
                child: _buildMuseumBadge(
                  badge: badge,
                  spec: spec,
                  baseColor: baseColor,
                  materialType: materialType,
                  isUnlocked: isUnlocked,
                ),
              ),
              const SizedBox(height: 8),
              _buildBadgeNameplate(
                title: (badge['title'] ?? '').toString(),
                spec: spec,
                isUnlocked: isUnlocked,
              ),
              const SizedBox(height: 6),
              _buildBadgeMaterialPill(spec, isUnlocked),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMuseumBadge({
    required dynamic badge,
    required _BadgeVisualSpec spec,
    required Color baseColor,
    required String materialType,
    required bool isUnlocked,
  }) {
    return Stack(
      alignment: Alignment.bottomCenter,
      clipBehavior: Clip.none,
      children: [
        Positioned(
          bottom: 6,
          child: Container(
            width: 96,
            height: 18,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(999),
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  spec.glowColor.withOpacity(isUnlocked ? 0.24 : 0.08),
                  Colors.black.withOpacity(0.04),
                ],
              ),
              boxShadow: [
                BoxShadow(
                  color: spec.glowColor.withOpacity(isUnlocked ? 0.24 : 0.08),
                  blurRadius: 22,
                  spreadRadius: 2,
                ),
              ],
            ),
          ),
        ),
        Positioned(
          bottom: 14,
          child: Container(
            width: 15,
            height: 36,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(999),
              gradient: LinearGradient(
                begin: Alignment.centerLeft,
                end: Alignment.centerRight,
                colors: spec.standColors,
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.24),
                  blurRadius: 6,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
          ),
        ),
        Positioned(
          bottom: 4,
          child: Container(
            width: 104,
            height: 16,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(999),
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: spec.plinthColors,
              ),
              border: Border.all(
                color: Colors.white.withOpacity(isUnlocked ? 0.24 : 0.1),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.28),
                  blurRadius: 8,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
          ),
        ),
        Positioned(
          top: 0,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildBadgeRibbon(spec, isUnlocked),
              _buildSuspensionRing(spec, isUnlocked),
              Container(
                width: 18,
                height: 12,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(999),
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: spec.connectorColors,
                  ),
                  border: Border.all(
                    color: Colors.white.withOpacity(isUnlocked ? 0.18 : 0.06),
                    width: 0.7,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.18),
                      blurRadius: 4,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 1),
              _buildMedalBody(
                badge: badge,
                spec: spec,
                baseColor: baseColor,
                materialType: materialType,
                isUnlocked: isUnlocked,
                size: 108,
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildMedalBody({
    required dynamic badge,
    required _BadgeVisualSpec spec,
    required Color baseColor,
    required String materialType,
    required bool isUnlocked,
    required double size,
  }) {
    return Container(
      width: size,
      height: size,
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: spec.outerRingColors,
        ),
        border: Border.all(
          color: Colors.white.withOpacity(isUnlocked ? 0.48 : 0.14),
          width: 1.4,
        ),
        boxShadow: [
          BoxShadow(
            color: spec.glowColor.withOpacity(isUnlocked ? 0.3 : 0.08),
            blurRadius: 24,
            spreadRadius: 1,
          ),
          BoxShadow(
            color: Colors.black.withOpacity(0.26),
            blurRadius: 12,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Stack(
        alignment: Alignment.center,
        children: [
          Positioned.fill(
            child: DecoratedBox(
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: SweepGradient(
                  colors: [
                    Colors.white.withOpacity(isUnlocked ? 0.44 : 0.08),
                    spec.edgeStroke.withOpacity(isUnlocked ? 0.18 : 0.04),
                    Colors.black.withOpacity(isUnlocked ? 0.16 : 0.08),
                    Colors.white.withOpacity(isUnlocked ? 0.36 : 0.07),
                  ],
                ),
              ),
            ),
          ),
          Positioned.fill(
            child: CustomPaint(
              painter: _MedalHardwarePainter(
                spec: spec,
                materialType: _materialShaderValue(materialType),
                isUnlocked: isUnlocked,
              ),
            ),
          ),
          ...List.generate(12, (index) {
            final angle = index * math.pi / 6;
            final gemColor = Color.lerp(
              spec.edgeStroke,
              spec.glowColor,
              index.isEven ? 0.28 : 0.72,
            )!;
            return Transform.rotate(
              angle: angle,
              child: Align(
                alignment: Alignment.topCenter,
                child: Container(
                  width: size * 0.045,
                  height: size * 0.045,
                  margin: EdgeInsets.only(top: size * 0.025),
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: RadialGradient(
                      colors: [
                        Colors.white.withOpacity(isUnlocked ? 0.86 : 0.22),
                        gemColor.withOpacity(isUnlocked ? 0.9 : 0.28),
                        Colors.black.withOpacity(isUnlocked ? 0.22 : 0.2),
                      ],
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: gemColor.withOpacity(isUnlocked ? 0.28 : 0.06),
                        blurRadius: 5,
                      ),
                    ],
                  ),
                ),
              ),
            );
          }),
          Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: spec.innerRingColors,
              ),
              border: Border.all(
                color: spec.edgeStroke.withOpacity(isUnlocked ? 0.9 : 0.24),
                width: 1.1,
              ),
            ),
            child: ValueListenableBuilder<Offset>(
              valueListenable: _lightOffsetNotifier,
              builder: (context, lightOffset, _) => BadgeShaderWidget(
                program: _program!,
                isUnlocked: isUnlocked,
                materialType: _materialShaderValue(materialType),
                baseColor: baseColor,
                lightOffset: lightOffset,
                iconData: _getBadgeIcon(badge['visual_meta']?['icon']),
                category: badge['category'] ?? 'General',
              ),
            ),
          ),
          if (!isUnlocked)
            Positioned.fill(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [
                      Colors.white.withOpacity(0.12),
                      Colors.black.withOpacity(0.42),
                    ],
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildBadgeNameplate({
    required String title,
    required _BadgeVisualSpec spec,
    required bool isUnlocked,
  }) {
    return SizedBox(
      height: 42,
      width: double.infinity,
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: spec.nameplateColors,
          ),
          border: Border.all(
            color: spec.edgeStroke.withOpacity(isUnlocked ? 0.65 : 0.18),
            width: 1,
          ),
          borderRadius: BorderRadius.circular(8),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.18),
              blurRadius: 6,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Center(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 7),
            child: Text(
              title,
              style: TextStyle(
                color: spec.titleColor,
                fontSize: 10.5,
                height: 1.05,
                fontWeight: FontWeight.w800,
              ),
              textAlign: TextAlign.center,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBadgeMaterialPill(_BadgeVisualSpec spec, bool isUnlocked) {
    return SizedBox(
      height: 22,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(isUnlocked ? 0.72 : 0.3),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
            color: spec.edgeStroke.withOpacity(isUnlocked ? 0.22 : 0.1),
          ),
        ),
        child: FittedBox(
          fit: BoxFit.scaleDown,
          child: Text(
            spec.materialLabel,
            style: TextStyle(
              color: spec.edgeStroke.withOpacity(isUnlocked ? 0.92 : 0.56),
              fontSize: 8.5,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSuspensionRing(_BadgeVisualSpec spec, bool isUnlocked) {
    return SizedBox(
      width: 34,
      height: 18,
      child: Stack(
        alignment: Alignment.center,
        children: [
          Container(
            width: 28,
            height: 18,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: SweepGradient(
                colors: [
                  Colors.white.withOpacity(isUnlocked ? 0.72 : 0.18),
                  spec.edgeStroke.withOpacity(isUnlocked ? 0.78 : 0.22),
                  Colors.black.withOpacity(isUnlocked ? 0.26 : 0.18),
                  spec.glowColor.withOpacity(isUnlocked ? 0.58 : 0.14),
                  Colors.white.withOpacity(isUnlocked ? 0.62 : 0.16),
                ],
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.22),
                  blurRadius: 4,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
          ),
          Container(
            width: 17,
            height: 10,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: Colors.black.withOpacity(isUnlocked ? 0.22 : 0.18),
              border: Border.all(
                color: Colors.white.withOpacity(isUnlocked ? 0.18 : 0.06),
                width: 0.6,
              ),
            ),
          ),
        ],
      ),
    );
  }

  double _materialShaderValue(String materialType) {
    final normalized = materialType.toLowerCase();
    if (normalized.contains('cyber')) return 1.0;
    if (normalized.contains('liquid')) return 2.0;
    if (normalized.contains('gold')) return 3.0;
    return 0.0;
  }

  Widget _buildBadgeRibbon(_BadgeVisualSpec spec, bool isUnlocked) {
    return SizedBox(
      width: 72,
      height: 34,
      child: CustomPaint(
        painter: _RibbonPainter(
          colors: spec.ribbonColors,
          isUnlocked: isUnlocked,
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

  _BadgeVisualSpec _buildBadgeVisualSpec({
    required String materialType,
    required Color baseColor,
    required String category,
    required bool unlocked,
  }) {
    final String normalized = materialType.toLowerCase();
    if (normalized.contains('gold')) {
      return _BadgeVisualSpec(
        ribbonColors: const [Color(0xFF7C1C1C), Color(0xFFD8B36B), Color(0xFF7C1C1C)],
        outerRingColors: const [Color(0xFFFFF2C2), Color(0xFFD8AE49), Color(0xFF8C6324)],
        innerRingColors: const [Color(0xFFF4D684), Color(0xFFB37B23)],
        connectorColors: const [Color(0xFFF3D07B), Color(0xFF9E6F21)],
        plinthColors: const [Color(0xFFE1BD74), Color(0xFF875C1A)],
        standColors: const [Color(0xFF634D36), Color(0xFFC9A56A), Color(0xFF634D36)],
        nameplateColors: const [Color(0xFF4B3522), Color(0xFF2E1E13)],
        glowColor: const Color(0xFFE8BC58),
        edgeStroke: const Color(0xFFF6E1B1),
        titleColor: unlocked ? const Color(0xFFF1DFB2) : const Color(0xFFA48657),
        materialLabel: 'AUREATE METAL',
        seriesLabel: _seriesLabelForCategory(category),
      );
    }
    if (normalized.contains('liquid')) {
      return _BadgeVisualSpec(
        ribbonColors: const [Color(0xFF235C6F), Color(0xFF7AD3DE), Color(0xFF235C6F)],
        outerRingColors: const [Color(0xFFF7FEFF), Color(0xFF9CD2DC), Color(0xFF466D79)],
        innerRingColors: const [Color(0xFFDDF8FB), Color(0xFF6DB7C1)],
        connectorColors: const [Color(0xFFBEE9EF), Color(0xFF5B99A3)],
        plinthColors: const [Color(0xFFD9F1F4), Color(0xFF4A737A)],
        standColors: const [Color(0xFF48646D), Color(0xFFB8E8EF), Color(0xFF48646D)],
        nameplateColors: const [Color(0xFF183A46), Color(0xFF102731)],
        glowColor: const Color(0xFF8AE8F2),
        edgeStroke: const Color(0xFFDDFBFF),
        titleColor: unlocked ? const Color(0xFFD9FAFF) : const Color(0xFF7D9AA0),
        materialLabel: 'LIQUID GLASS',
        seriesLabel: _seriesLabelForCategory(category),
      );
    }
    if (normalized.contains('cyber')) {
      return _BadgeVisualSpec(
        ribbonColors: const [Color(0xFF1B1A57), Color(0xFF8B58FF), Color(0xFF1B1A57)],
        outerRingColors: const [Color(0xFFD5D4FF), Color(0xFF7040F5), Color(0xFF1C145D)],
        innerRingColors: const [Color(0xFFB8C7FF), Color(0xFF304A99)],
        connectorColors: const [Color(0xFFA5B7FF), Color(0xFF384FA0)],
        plinthColors: const [Color(0xFFBFC7FF), Color(0xFF2D2C7B)],
        standColors: const [Color(0xFF1F235C), Color(0xFF9BA9FF), Color(0xFF1F235C)],
        nameplateColors: const [Color(0xFF1D173E), Color(0xFF120F27)],
        glowColor: const Color(0xFF8E83FF),
        edgeStroke: const Color(0xFFD8D6FF),
        titleColor: unlocked ? const Color(0xFFE5E2FF) : const Color(0xFF8A86A6),
        materialLabel: 'CYBER ENAMEL',
        seriesLabel: _seriesLabelForCategory(category),
      );
    }
    return _BadgeVisualSpec(
      ribbonColors: [baseColor.withOpacity(0.88), Color.alphaBlend(Colors.white.withOpacity(0.22), baseColor), baseColor.withOpacity(0.88)],
      outerRingColors: [Colors.white, Color.alphaBlend(Colors.white.withOpacity(0.22), baseColor), Color.alphaBlend(Colors.black.withOpacity(0.35), baseColor)],
      innerRingColors: [Color.alphaBlend(Colors.white.withOpacity(0.2), baseColor), Color.alphaBlend(Colors.black.withOpacity(0.24), baseColor)],
      connectorColors: [Color.alphaBlend(Colors.white.withOpacity(0.26), baseColor), Color.alphaBlend(Colors.black.withOpacity(0.18), baseColor)],
      plinthColors: const [Color(0xFFD7C2A1), Color(0xFF8E7054)],
      standColors: const [Color(0xFF6A5847), Color(0xFFDCC9A5), Color(0xFF6A5847)],
      nameplateColors: const [Color(0xFF423126), Color(0xFF2B2018)],
      glowColor: baseColor,
      edgeStroke: Colors.white,
      titleColor: unlocked ? const Color(0xFFF5EBDD) : const Color(0xFFA4937E),
      materialLabel: 'FORGED ALLOY',
      seriesLabel: _seriesLabelForCategory(category),
    );
  }

  String _seriesLabelForCategory(String category) {
    switch (category) {
      case 'Milestone':
        return 'MILESTONE ORDER';
      case 'Geographic':
        return 'ATLAS DIVISION';
      case 'Emotion':
        return 'AFFECTA INSIGNIA';
      case 'Time':
        return 'CHRONO SEAL';
      case 'Streak':
        return 'PERSISTENCE MARK';
      case 'Explorer':
        return 'EXPLORER GUILD';
      case 'Special':
        return 'PRIVATE RESERVE';
      default:
        return 'FOOTPRINT SOCIETY';
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
        final spec = _buildBadgeVisualSpec(
          materialType: materialType,
          baseColor: baseColor,
          category: (badge['category'] ?? 'General').toString(),
          unlocked: true,
        );

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
                      border: Border.all(color: Colors.white.withValues(alpha: 0.28), width: 1.5),
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [
                          Colors.white.withValues(alpha: 0.18),
                          Colors.white.withValues(alpha: 0.07),
                        ],
                      ),
                    ),
                    padding: const EdgeInsets.fromLTRB(24, 40, 24, 24),
                    child: Stack(
                      children: [
                        Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            SizedBox(
                              height: 300,
                              child: Stack(
                                alignment: Alignment.center,
                                children: [
                                  Positioned(
                                    bottom: 18,
                                    child: Container(
                                      width: 170,
                                      height: 26,
                                      decoration: BoxDecoration(
                                        borderRadius: BorderRadius.circular(999),
                                        boxShadow: [
                                          BoxShadow(
                                            color: spec.glowColor.withOpacity(0.3),
                                            blurRadius: 28,
                                            spreadRadius: 4,
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                  Positioned(
                                    top: 10,
                                    child: _buildBadgeRibbon(spec, true),
                                  ),
                                  Positioned(
                                    top: 36,
                                    child: Container(
                                      width: 18,
                                      height: 28,
                                      decoration: BoxDecoration(
                                        borderRadius: BorderRadius.circular(999),
                                        gradient: LinearGradient(
                                          begin: Alignment.topCenter,
                                          end: Alignment.bottomCenter,
                                          colors: spec.connectorColors,
                                        ),
                                      ),
                                    ),
                                  ),
                                  Container(
                                    width: 220,
                                    height: 220,
                                    padding: const EdgeInsets.all(14),
                                    decoration: BoxDecoration(
                                      shape: BoxShape.circle,
                                      gradient: LinearGradient(
                                        begin: Alignment.topLeft,
                                        end: Alignment.bottomRight,
                                        colors: spec.outerRingColors,
                                      ),
                                      border: Border.all(color: Colors.white.withOpacity(0.48), width: 1.6),
                                      boxShadow: [
                                        BoxShadow(
                                          color: spec.glowColor.withOpacity(0.36),
                                          blurRadius: 36,
                                          spreadRadius: 4,
                                        ),
                                        BoxShadow(
                                          color: Colors.black.withOpacity(0.22),
                                          blurRadius: 16,
                                          offset: const Offset(0, 10),
                                        ),
                                      ],
                                    ),
                                    child: Container(
                                      padding: const EdgeInsets.all(10),
                                      decoration: BoxDecoration(
                                        shape: BoxShape.circle,
                                        gradient: LinearGradient(
                                          begin: Alignment.topLeft,
                                          end: Alignment.bottomRight,
                                          colors: spec.innerRingColors,
                                        ),
                                        border: Border.all(color: spec.edgeStroke.withOpacity(0.92), width: 1.2),
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
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(height: 18),
                            Text(
                              badge['title'],
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 32,
                                fontWeight: FontWeight.w900,
                                letterSpacing: 1.2,
                              ),
                              textAlign: TextAlign.center,
                            ),
                            const SizedBox(height: 8),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 8),
                              decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  colors: spec.nameplateColors,
                                ),
                                borderRadius: BorderRadius.circular(30),
                                border: Border.all(color: spec.edgeStroke.withValues(alpha: 0.36)),
                              ),
                              child: Text(
                                '$catCN · ${spec.materialLabel}',
                                style: TextStyle(color: spec.titleColor, fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 1.5),
                              ),
                            ),
                            const SizedBox(height: 24),
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
                            Container(
                              width: double.infinity,
                              padding: const EdgeInsets.all(20),
                              decoration: BoxDecoration(
                                color: Colors.black.withValues(alpha: 0.26),
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

class _CabinetTexturePainter extends CustomPainter {
  final _CabinetStyleSpec cabinet;

  const _CabinetTexturePainter(this.cabinet);

  @override
  void paint(Canvas canvas, Size size) {
    final glassPaint = Paint()
      ..shader = ui.Gradient.linear(
        Offset.zero,
        Offset(size.width, size.height),
        [
          Colors.white.withOpacity(0.18),
          Colors.white.withOpacity(0.02),
          cabinet.accentColor.withOpacity(0.08),
        ],
      );
    canvas.drawRect(Offset.zero & size, glassPaint);

    final linePaint = Paint()
      ..color = cabinet.shelfEdge.withOpacity(0.045)
      ..strokeWidth = 1;
    for (double y = 96; y < size.height; y += 84) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), linePaint);
    }

    final verticalPaint = Paint()
      ..color = cabinet.panelBorder.withOpacity(0.08)
      ..strokeWidth = 1;
    for (double x = 24; x < size.width; x += 76) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), verticalPaint);
    }

    final highlight = Paint()
      ..shader = ui.Gradient.linear(
        Offset(size.width * 0.18, 0),
        Offset(size.width * 0.72, size.height),
        [
          Colors.white.withOpacity(0.28),
          Colors.white.withOpacity(0.0),
        ],
      )
      ..blendMode = BlendMode.screen;
    final path = Path()
      ..moveTo(size.width * 0.08, 0)
      ..lineTo(size.width * 0.28, 0)
      ..lineTo(size.width * 0.72, size.height)
      ..lineTo(size.width * 0.5, size.height)
      ..close();
    canvas.drawPath(path, highlight);
  }

  @override
  bool shouldRepaint(covariant _CabinetTexturePainter oldDelegate) {
    return oldDelegate.cabinet != cabinet;
  }
}

class _RibbonPainter extends CustomPainter {
  final List<Color> colors;
  final bool isUnlocked;

  const _RibbonPainter({
    required this.colors,
    required this.isUnlocked,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final opacity = isUnlocked ? 1.0 : 0.42;
    final body = Path()
      ..moveTo(size.width * 0.12, 0)
      ..quadraticBezierTo(size.width * 0.5, size.height * 0.08, size.width * 0.88, 0)
      ..lineTo(size.width * 0.78, size.height * 0.94)
      ..quadraticBezierTo(size.width * 0.5, size.height * 0.76, size.width * 0.22, size.height * 0.94)
      ..close();

    final shadowPaint = Paint()
      ..color = Colors.black.withOpacity(0.22 * opacity)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4);
    canvas.drawPath(body.shift(const Offset(0, 3)), shadowPaint);

    final ribbonPaint = Paint()
      ..shader = ui.Gradient.linear(
        Offset.zero,
        Offset(size.width, size.height),
        [
          colors.first.withOpacity(opacity),
          colors.length > 1 ? colors[1].withOpacity(opacity) : colors.first.withOpacity(opacity),
          colors.last.withOpacity(opacity),
        ],
        [0.0, 0.48, 1.0],
      );
    canvas.drawPath(body, ribbonPaint);

    final borderPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.9
      ..color = Colors.white.withOpacity(0.22 * opacity);
    canvas.drawPath(body, borderPaint);

    for (int i = 0; i < 3; i++) {
      final x = size.width * (0.28 + i * 0.22);
      final fold = Path()
        ..moveTo(x, size.height * 0.08)
        ..quadraticBezierTo(x + (i == 1 ? 0 : size.width * 0.04), size.height * 0.48, x, size.height * 0.88);
      final foldPaint = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = i == 1 ? 2.4 : 1.4
        ..strokeCap = StrokeCap.round
        ..shader = ui.Gradient.linear(
          Offset(x, 0),
          Offset(x, size.height),
          [
            Colors.white.withOpacity((i == 1 ? 0.3 : 0.18) * opacity),
            Colors.black.withOpacity((i == 1 ? 0.14 : 0.09) * opacity),
          ],
        );
      canvas.drawPath(fold, foldPaint);
    }

    final topHighlight = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.2
      ..strokeCap = StrokeCap.round
      ..color = Colors.white.withOpacity(0.32 * opacity);
    canvas.drawArc(
      Rect.fromLTWH(size.width * 0.16, -size.height * 0.18, size.width * 0.68, size.height * 0.38),
      0.12,
      math.pi - 0.24,
      false,
      topHighlight,
    );
  }

  @override
  bool shouldRepaint(covariant _RibbonPainter oldDelegate) {
    return oldDelegate.colors != colors || oldDelegate.isUnlocked != isUnlocked;
  }
}

class _BadgeReliefPainter extends CustomPainter {
  final String category;
  final double materialType;
  final Color baseColor;
  final bool isUnlocked;
  final Offset lightOffset;

  const _BadgeReliefPainter({
    required this.category,
    required this.materialType,
    required this.baseColor,
    required this.isUnlocked,
    required this.lightOffset,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.shortestSide / 2;
    final accent = _accentForMaterial();
    final reliefPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeWidth = math.max(0.8, radius * 0.028)
      ..color = accent.withOpacity(isUnlocked ? 0.62 : 0.18);
    final shadowPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeWidth = reliefPaint.strokeWidth
      ..color = Colors.black.withOpacity(isUnlocked ? 0.18 : 0.1);

    void drawReliefPath(Path path) {
      canvas.save();
      canvas.translate(1.1 + lightOffset.dx, 1.2 + lightOffset.dy);
      canvas.drawPath(path, shadowPaint);
      canvas.restore();
      canvas.drawPath(path, reliefPaint);
    }

    final outerPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = radius * 0.045
      ..shader = ui.Gradient.sweep(
        center,
        [
          Colors.white.withOpacity(isUnlocked ? 0.72 : 0.18),
          accent.withOpacity(isUnlocked ? 0.5 : 0.12),
          Colors.black.withOpacity(isUnlocked ? 0.22 : 0.16),
          Colors.white.withOpacity(isUnlocked ? 0.62 : 0.16),
        ],
      );
    canvas.drawCircle(center, radius * 0.82, outerPaint);
    canvas.drawCircle(center, radius * 0.66, reliefPaint..strokeWidth = radius * 0.012);

    if (category == 'Milestone') {
      final path = Path();
      for (int i = 0; i < 5; i++) {
        final outer = -math.pi / 2 + i * math.pi * 2 / 5;
        final inner = outer + math.pi / 5;
        final p1 = center + Offset(math.cos(outer), math.sin(outer)) * radius * 0.58;
        final p2 = center + Offset(math.cos(inner), math.sin(inner)) * radius * 0.28;
        if (i == 0) path.moveTo(p1.dx, p1.dy);
        path.lineTo(p2.dx, p2.dy);
        final next = -math.pi / 2 + (i + 1) * math.pi * 2 / 5;
        final p3 = center + Offset(math.cos(next), math.sin(next)) * radius * 0.58;
        path.lineTo(p3.dx, p3.dy);
      }
      path.close();
      drawReliefPath(path);
    } else if (category == 'Geographic') {
      for (int i = -2; i <= 2; i++) {
        final rect = Rect.fromCenter(center: center, width: radius * 1.15, height: radius * (0.22 + i.abs() * 0.12));
        drawReliefPath(Path()..addOval(rect));
      }
      for (int i = -1; i <= 1; i++) {
        final path = Path()
          ..moveTo(center.dx + i * radius * 0.18, center.dy - radius * 0.58)
          ..quadraticBezierTo(center.dx + i * radius * 0.44, center.dy, center.dx + i * radius * 0.18, center.dy + radius * 0.58);
        drawReliefPath(path);
      }
    } else if (category == 'Emotion') {
      for (int i = 0; i < 8; i++) {
        final angle = i * math.pi / 4;
        canvas.save();
        canvas.translate(center.dx, center.dy);
        canvas.rotate(angle);
        final rect = Rect.fromCenter(center: Offset(0, -radius * 0.34), width: radius * 0.26, height: radius * 0.46);
        canvas.drawOval(rect.shift(const Offset(1, 1)), shadowPaint);
        canvas.drawOval(rect, reliefPaint);
        canvas.restore();
      }
    } else if (category == 'Time') {
      for (int i = 0; i < 12; i++) {
        final angle = -math.pi / 2 + i * math.pi / 6;
        final start = center + Offset(math.cos(angle), math.sin(angle)) * radius * 0.48;
        final end = center + Offset(math.cos(angle), math.sin(angle)) * radius * (i % 3 == 0 ? 0.62 : 0.57);
        canvas.drawLine(start.translate(1, 1), end.translate(1, 1), shadowPaint);
        canvas.drawLine(start, end, reliefPaint);
      }
      final handPaint = Paint()
        ..color = accent.withOpacity(isUnlocked ? 0.72 : 0.2)
        ..strokeWidth = radius * 0.035
        ..strokeCap = StrokeCap.round;
      canvas.drawLine(center, center + Offset(radius * 0.25, -radius * 0.32), handPaint);
      canvas.drawLine(center, center + Offset(radius * 0.36, radius * 0.16), handPaint);
    } else if (category == 'Streak') {
      for (int i = 0; i < 4; i++) {
        final y = center.dy - radius * 0.34 + i * radius * 0.2;
        final path = Path()
          ..moveTo(center.dx - radius * 0.42, y)
          ..lineTo(center.dx - radius * 0.14, y + radius * 0.12)
          ..lineTo(center.dx + radius * 0.42, y - radius * 0.1);
        drawReliefPath(path);
      }
    } else if (category == 'Explorer') {
      final path = Path()
        ..moveTo(center.dx, center.dy - radius * 0.58)
        ..lineTo(center.dx + radius * 0.2, center.dy - radius * 0.16)
        ..lineTo(center.dx + radius * 0.58, center.dy)
        ..lineTo(center.dx + radius * 0.16, center.dy + radius * 0.2)
        ..lineTo(center.dx, center.dy + radius * 0.58)
        ..lineTo(center.dx - radius * 0.2, center.dy + radius * 0.16)
        ..lineTo(center.dx - radius * 0.58, center.dy)
        ..lineTo(center.dx - radius * 0.16, center.dy - radius * 0.2)
        ..close();
      drawReliefPath(path);
    } else if (category == 'Special') {
      for (int i = 0; i < 3; i++) {
        final scale = 0.28 + i * 0.16;
        final path = Path()
          ..moveTo(center.dx, center.dy - radius * scale)
          ..lineTo(center.dx + radius * scale, center.dy)
          ..lineTo(center.dx, center.dy + radius * scale)
          ..lineTo(center.dx - radius * scale, center.dy)
          ..close();
        drawReliefPath(path);
      }
    } else {
      for (int i = 0; i < 16; i++) {
        final angle = i * math.pi / 8;
        final start = center + Offset(math.cos(angle), math.sin(angle)) * radius * 0.42;
        final end = center + Offset(math.cos(angle), math.sin(angle)) * radius * 0.62;
        canvas.drawLine(start.translate(1, 1), end.translate(1, 1), shadowPaint);
        canvas.drawLine(start, end, reliefPaint);
      }
    }
  }

  Color _accentForMaterial() {
    if (materialType == 1.0) return const Color(0xFF88D7FF);
    if (materialType == 2.0) return const Color(0xFFBDF5E9);
    if (materialType == 3.0) return const Color(0xFFFFE2A2);
    return Color.lerp(Colors.white, baseColor, 0.55)!;
  }

  @override
  bool shouldRepaint(covariant _BadgeReliefPainter oldDelegate) {
    return oldDelegate.category != category ||
        oldDelegate.materialType != materialType ||
        oldDelegate.baseColor != baseColor ||
        oldDelegate.isUnlocked != isUnlocked ||
        oldDelegate.lightOffset != lightOffset;
  }
}

class _MedalHardwarePainter extends CustomPainter {
  final _BadgeVisualSpec spec;
  final double materialType;
  final bool isUnlocked;

  const _MedalHardwarePainter({
    required this.spec,
    required this.materialType,
    required this.isUnlocked,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.shortestSide / 2;
    final opacity = isUnlocked ? 1.0 : 0.34;

    final sidePaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = radius * 0.16
      ..shader = ui.Gradient.sweep(
        center,
        [
          Colors.white.withOpacity(0.72 * opacity),
          spec.edgeStroke.withOpacity(0.62 * opacity),
          Colors.black.withOpacity(0.34 * opacity),
          spec.glowColor.withOpacity(0.46 * opacity),
          Colors.white.withOpacity(0.68 * opacity),
        ],
      );
    canvas.drawCircle(center, radius * 0.82, sidePaint);

    final bevelPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = radius * 0.035
      ..shader = ui.Gradient.sweep(
        center,
        [
          Colors.white.withOpacity(0.84 * opacity),
          spec.glowColor.withOpacity(0.36 * opacity),
          Colors.black.withOpacity(0.22 * opacity),
          Colors.white.withOpacity(0.7 * opacity),
        ],
      );
    canvas.drawCircle(center, radius * 0.92, bevelPaint);
    canvas.drawCircle(center, radius * 0.66, bevelPaint..strokeWidth = radius * 0.022);

    final enamelColors = _enamelPalette();
    for (int i = 0; i < 6; i++) {
      final start = -math.pi / 2 + i * math.pi / 3;
      final sweep = math.pi / 3 - 0.035;
      final paint = Paint()
        ..style = PaintingStyle.fill
        ..shader = ui.Gradient.radial(
          center,
          radius * 0.62,
          [
            enamelColors[i % enamelColors.length].withOpacity(0.3 * opacity),
            enamelColors[(i + 1) % enamelColors.length].withOpacity(0.16 * opacity),
            Colors.black.withOpacity(0.04 * opacity),
          ],
        );
      final path = Path()
        ..moveTo(center.dx, center.dy)
        ..arcTo(Rect.fromCircle(center: center, radius: radius * 0.6), start, sweep, false)
        ..close();
      canvas.drawPath(path, paint);
    }

    final separatorPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = radius * 0.012
      ..color = spec.edgeStroke.withOpacity(0.34 * opacity);
    for (int i = 0; i < 6; i++) {
      final angle = -math.pi / 2 + i * math.pi / 3;
      final start = center + Offset(math.cos(angle), math.sin(angle)) * radius * 0.22;
      final end = center + Offset(math.cos(angle), math.sin(angle)) * radius * 0.62;
      canvas.drawLine(start, end, separatorPaint);
    }

    final guillochePaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.55
      ..color = Colors.white.withOpacity(0.2 * opacity);
    for (int i = 0; i < 18; i++) {
      final path = Path();
      final phase = i * math.pi / 9;
      for (int step = 0; step <= 80; step++) {
        final t = step / 80.0;
        final angle = t * math.pi * 2 + phase;
        final waveRadius = radius * (0.32 + 0.08 * math.sin(t * math.pi * 6 + phase));
        final point = center + Offset(math.cos(angle), math.sin(angle)) * waveRadius;
        if (step == 0) {
          path.moveTo(point.dx, point.dy);
        } else {
          path.lineTo(point.dx, point.dy);
        }
      }
      canvas.drawPath(path, guillochePaint);
    }

    final highlight = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = radius * 0.11
      ..strokeCap = StrokeCap.round
      ..color = Colors.white.withOpacity(0.24 * opacity);
    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius * 0.72),
      -math.pi * 0.78,
      math.pi * 0.34,
      false,
      highlight,
    );

    final lowerShadow = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = radius * 0.08
      ..strokeCap = StrokeCap.round
      ..color = Colors.black.withOpacity(0.14 * opacity);
    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius * 0.72),
      math.pi * 0.22,
      math.pi * 0.46,
      false,
      lowerShadow,
    );
  }

  List<Color> _enamelPalette() {
    if (materialType == 1.0) {
      return const [Color(0xFF224C92), Color(0xFF66D7FF), Color(0xFF7B5CFF)];
    }
    if (materialType == 2.0) {
      return const [Color(0xFF2F7B79), Color(0xFFCFF8EA), Color(0xFF78BFD1)];
    }
    if (materialType == 3.0) {
      return const [Color(0xFF9A4F22), Color(0xFFFFD56F), Color(0xFFE76F51)];
    }
    return [
      spec.glowColor,
      Color.lerp(Colors.white, spec.glowColor, 0.42)!,
      spec.edgeStroke,
    ];
  }

  @override
  bool shouldRepaint(covariant _MedalHardwarePainter oldDelegate) {
    return oldDelegate.spec != spec ||
        oldDelegate.materialType != materialType ||
        oldDelegate.isUnlocked != isUnlocked;
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
                
                CustomPaint(
                  size: Size(constraints.maxWidth, constraints.maxHeight),
                  painter: _BadgeReliefPainter(
                    category: category,
                    materialType: materialType,
                    baseColor: displayColor,
                    isUnlocked: isUnlocked,
                    lightOffset: lightOffset,
                  ),
                ),

                _buildCategoryFrame(constraints.maxWidth, displayColor),

                Center(
                  child: Container(
                    width: constraints.maxWidth * 0.5,
                    height: constraints.maxWidth * 0.5,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: RadialGradient(
                        colors: [
                          Colors.white.withAlpha(isUnlocked ? 210 : 72),
                          displayColor.withAlpha(isUnlocked ? 150 : 36),
                          Colors.black.withAlpha(isUnlocked ? 34 : 74),
                        ],
                      ),
                      border: Border.all(
                        color: Colors.white.withAlpha(isUnlocked ? 150 : 44),
                        width: 1,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withAlpha(isUnlocked ? 94 : 80),
                          blurRadius: 8,
                          offset: const Offset(0, 3),
                        ),
                        BoxShadow(
                          color: displayColor.withAlpha(isUnlocked ? 70 : 18),
                          blurRadius: 10,
                        ),
                      ],
                    ),
                    child: Center(
                      child: Stack(
                        alignment: Alignment.center,
                        children: [
                          Transform.translate(
                            offset: const Offset(1.4, 1.6),
                            child: Icon(
                              iconData,
                              size: constraints.maxWidth * 0.28,
                              color: Colors.black.withAlpha(112),
                            ),
                          ),
                          ShaderMask(
                            shaderCallback: (Rect bounds) {
                              return ui.Gradient.linear(
                                Offset(bounds.width * 0.3 + lightOffset.dx * 6, 0),
                                Offset(bounds.width * 0.75 - lightOffset.dx * 5, bounds.height),
                                [
                                  Colors.white.withAlpha(isUnlocked ? 255 : 96),
                                  displayColor.withAlpha(isUnlocked ? 190 : 64),
                                  Colors.black.withAlpha(isUnlocked ? 70 : 96),
                                ],
                              );
                            },
                            child: Icon(
                              iconData,
                              size: constraints.maxWidth * 0.28,
                              color: Colors.white,
                            ),
                          ),
                        ],
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
      return Center(
        child: Stack(
          alignment: Alignment.center,
          children: [
            Container(
              width: size * 0.92,
              height: size * 0.92,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: displayColor.withAlpha(isUnlocked ? 120 : 30),
                  width: 1.5,
                ),
                boxShadow: isUnlocked
                    ? [
                        BoxShadow(
                          color: displayColor.withAlpha(36),
                          blurRadius: 10,
                          spreadRadius: 1,
                        ),
                      ]
                    : null,
              ),
            ),
            Container(
              width: size * 0.76,
              height: size * 0.76,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: displayColor.withAlpha(isUnlocked ? 66 : 18),
                  width: 1,
                ),
              ),
            ),
            ...List.generate(10, (index) {
              final angle = index * 0.6283185307;
              return Transform.rotate(
                angle: angle,
                child: Align(
                  alignment: Alignment.topCenter,
                  child: Container(
                    width: 2,
                    height: size * 0.06,
                    margin: EdgeInsets.only(top: size * 0.06),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(999),
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          displayColor.withAlpha(isUnlocked ? 120 : 24),
                          displayColor.withAlpha(isUnlocked ? 28 : 8),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            }),
          ],
        ),
      );
    } else if (category == 'Geographic') {
      return Center(
        child: Stack(
          alignment: Alignment.center,
          children: [
            Container(
              width: size * 0.88,
              height: size * 0.88,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: displayColor.withAlpha(isUnlocked ? 96 : 24),
                  width: 1.1,
                ),
              ),
            ),
            Container(
              width: size * 0.68,
              height: size * 0.68,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: displayColor.withAlpha(isUnlocked ? 58 : 16),
                  width: 0.8,
                ),
              ),
            ),
            ...List.generate(4, (index) {
              final bool vertical = index % 2 == 0;
              return Transform.rotate(
                angle: vertical ? 0 : math.pi / 2,
                child: Container(
                  width: size * 0.46,
                  height: 1,
                  color: displayColor.withAlpha(isUnlocked ? 72 : 18),
                ),
              );
            }),
            ...List.generate(8, (index) {
              final angle = index * 0.78539816339;
              return Transform.rotate(
                angle: angle,
                child: Align(
                  alignment: Alignment.topCenter,
                  child: Container(
                    width: 2,
                    height: size * 0.045,
                    margin: EdgeInsets.only(top: size * 0.1),
                    decoration: BoxDecoration(
                      color: displayColor.withAlpha(isUnlocked ? 96 : 20),
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                ),
              );
            }),
          ],
        ),
      );
    } else if (category == 'Emotion') {
      return Center(
        child: Stack(
          alignment: Alignment.center,
          children: [
            ...List.generate(8, (index) {
              final angle = index * 0.78539816339;
              return Transform.rotate(
                angle: angle,
                child: Align(
                  alignment: Alignment.topCenter,
                  child: Container(
                    width: size * 0.14,
                    height: size * 0.2,
                    margin: EdgeInsets.only(top: size * 0.12),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(size * 0.08),
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          displayColor.withAlpha(isUnlocked ? 110 : 22),
                          displayColor.withAlpha(isUnlocked ? 28 : 8),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            }),
            Container(
              width: size * 0.84,
              height: size * 0.84,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                boxShadow: [
                  BoxShadow(
                    color: displayColor.withAlpha(isUnlocked ? 70 : 24),
                    blurRadius: 18,
                    spreadRadius: 2,
                  ),
                ],
              ),
            ),
          ],
        ),
      );
    } else if (category == 'Time') {
      return Center(
        child: Stack(
          alignment: Alignment.center,
          children: [
            Container(
              width: size * 0.9,
              height: size * 0.9,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: displayColor.withAlpha(isUnlocked ? 110 : 24),
                  width: 1.3,
                ),
              ),
            ),
            Container(
              width: size * 0.68,
              height: size * 0.68,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: displayColor.withAlpha(isUnlocked ? 68 : 16),
                  width: 0.9,
                ),
              ),
            ),
            ...List.generate(12, (index) {
              final angle = index * 0.52359877559;
              return Transform.rotate(
                angle: angle,
                child: Align(
                  alignment: Alignment.topCenter,
                  child: Container(
                    width: 2,
                    height: index % 3 == 0 ? size * 0.08 : size * 0.055,
                    margin: EdgeInsets.only(top: size * 0.075),
                    decoration: BoxDecoration(
                      color: displayColor.withAlpha(index % 3 == 0 ? 112 : 72),
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                ),
              );
            }),
          ],
        ),
      );
    } else if (category == 'Streak') {
      return Center(
        child: Stack(
          alignment: Alignment.center,
          children: [
            Container(
              width: size * 0.92,
              height: size * 0.92,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: displayColor.withAlpha(isUnlocked ? 102 : 22),
                  width: 1.2,
                ),
              ),
            ),
            ...List.generate(8, (index) {
              final angle = index * 0.39269908169 - 0.18;
              return Transform.rotate(
                angle: angle,
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: Container(
                    width: size * 0.14,
                    height: size * 0.05,
                    margin: EdgeInsets.only(left: size * 0.08),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(999),
                      gradient: LinearGradient(
                        colors: [
                          displayColor.withAlpha(isUnlocked ? 92 : 20),
                          displayColor.withAlpha(isUnlocked ? 22 : 6),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            }),
          ],
        ),
      );
    } else if (category == 'Explorer') {
      return Center(
        child: Stack(
          alignment: Alignment.center,
          children: [
            Container(
              width: size * 0.9,
              height: size * 0.9,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: displayColor.withAlpha(isUnlocked ? 106 : 22),
                  width: 1.2,
                ),
              ),
            ),
            Container(
              width: size * 0.62,
              height: 1,
              color: displayColor.withAlpha(isUnlocked ? 94 : 20),
            ),
            Transform.rotate(
              angle: math.pi / 4,
              child: Container(
                width: size * 0.44,
                height: 1,
                color: displayColor.withAlpha(isUnlocked ? 72 : 18),
              ),
            ),
            Transform.rotate(
              angle: -math.pi / 4,
              child: Container(
                width: size * 0.44,
                height: 1,
                color: displayColor.withAlpha(isUnlocked ? 72 : 18),
              ),
            ),
          ],
        ),
      );
    } else if (category == 'Special') {
      return Center(
        child: Container(
          width: size * 0.88,
          height: size * 0.88,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(
              color: displayColor.withAlpha(isUnlocked ? 120 : 24),
              width: 1.4,
            ),
          ),
          child: Center(
            child: Container(
              width: size * 0.6,
              height: size * 0.6,
              decoration: BoxDecoration(
                border: Border.all(
                  color: displayColor.withAlpha(isUnlocked ? 84 : 18),
                  width: 1,
                ),
                borderRadius: BorderRadius.circular(10),
              ),
            ),
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
