import 'dart:async';
import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/physics.dart';
import 'package:sensors_plus/sensors_plus.dart';

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
  late Animation<double> _revealAnimation;
  
  final Map<String, AnimationController> _badgeFocusControllers = {};

  @override
  void initState() {
    super.initState();
    _loadShader();
    
    _gyroSub = gyroscopeEventStream().listen((GyroscopeEvent event) {
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
    _revealAnimation = CurvedAnimation(
      parent: _revealController,
      curve: Curves.elasticOut,
    );
    _revealController.forward();
    
    for (int i = 0; i < widget.badgeDictionary.length; i++) {
        var category = widget.badgeDictionary.keys.elementAt(i);
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
        title: const Text("荣耀圣殿", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, letterSpacing: 2)),
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
              category.toUpperCase(),
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
              mainAxisSpacing: 24,
              crossAxisSpacing: 24,
              childAspectRatio: 0.75,
            ),
            delegate: SliverChildBuilderDelegate(
              (context, index) {
                final badge = items[index];
                bool isUnlocked = widget.unlockedIds.contains(badge['badge_id']);
                return _buildBadgeItem(badge, isUnlocked, index);
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
    // Staggered reveal
    return AnimatedBuilder(
      animation: _revealAnimation,
      builder: (context, child) {
        double t = (_revealAnimation.value - index * 0.1).clamp(0.0, 1.0);
        double scale = Curves.easeOutBack.transform(t);
        double opacity = Curves.easeIn.transform(t);
        return Transform.scale(
          scale: scale,
          child: Opacity(opacity: opacity, child: child),
        );
      },
      child: GestureDetector(
        onTapDown: (_) {
          if (isUnlocked) {
              _badgeFocusControllers[badge['badge_id']]?.forward();
              HapticFeedback.lightImpact();
          }
        },
        onTapUp: (_) => _badgeFocusControllers[badge['badge_id']]?.reverse(),
        onTapCancel: () => _badgeFocusControllers[badge['badge_id']]?.reverse(),
        onTap: () {
          if (isUnlocked) {
            _showBadgeDetailOverlay(badge);
          } else {
            HapticFeedback.vibrate();
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text("未解锁：${badge['condition_key']}达到${badge['target_value']}")),
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
                child: BadgeShaderWidget(
                  program: _program!,
                  isUnlocked: isUnlocked,
                  materialType: badge['visual_meta']['material'] == 'Cyber' ? 1.0 :
                                badge['visual_meta']['material'] == 'Liquid' ? 2.0 : 0.0,
                  baseColor: _parseColor(badge['visual_meta']['base_color'], isUnlocked),
                  lightOffset: _gyroOffset + _pointerOffset,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                badge['title'],
                style: TextStyle(
                  color: isUnlocked ? Colors.white : Colors.white24,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
                textAlign: TextAlign.center,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Color _parseColor(String? hexString, bool isUnlocked) {
    if (!isUnlocked) return Colors.grey.shade900;
    if (hexString == null) return Colors.amber;
    final buffer = StringBuffer();
    if (hexString.length == 6 || hexString.length == 7) buffer.write('ff');
    buffer.write(hexString.replaceFirst('#', ''));
    return Color(int.parse(buffer.toString(), radix: 16));
  }

  void _showBadgeDetailOverlay(dynamic badge) {
    HapticFeedback.heavyImpact();
    // 激光蚀刻效果展示 - Laser Engraving Scene
    showGeneralDialog(
      context: context,
      barrierDismissible: true,
      barrierLabel: "Dismiss",
      barrierColor: Colors.black87,
      transitionDuration: const Duration(milliseconds: 500),
      pageBuilder: (context, animation, secondaryAnimation) {
        return Center(
          child: Material(
            color: Colors.transparent,
            child: ScaleTransition(
              scale: CurvedAnimation(parent: animation, curve: Curves.elasticOut),
              child: FadeTransition(
                opacity: animation,
                child: Container(
                  width: MediaQuery.of(context).size.width * 0.8,
                  padding: const EdgeInsets.all(32),
                  decoration: BoxDecoration(
                    color: Colors.grey.shade900.withValues(alpha: 0.8),
                    borderRadius: BorderRadius.circular(32),
                    border: Border.all(color: Colors.white24),
                    boxShadow: [
                      BoxShadow(
                        color: _parseColor(badge['visual_meta']['base_color'], true).withValues(alpha: 0.3),
                        blurRadius: 30,
                        spreadRadius: 10,
                      )
                    ]
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      SizedBox(
                        height: 200,
                        child: BadgeShaderWidget(
                          program: _program!,
                          isUnlocked: true,
                          materialType: badge['visual_meta']['material'] == 'Cyber' ? 1.0 :
                                        badge['visual_meta']['material'] == 'Liquid' ? 2.0 : 0.0,
                          baseColor: _parseColor(badge['visual_meta']['base_color'], true),
                          lightOffset: const Offset(0, 0),
                        ),
                      ),
                      const SizedBox(height: 32),
                      Text(
                        badge['title'],
                        style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.bold, letterSpacing: 2),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 16),
                      Text(
                        badge['description'],
                        style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 16, height: 1.5),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 24),
                      // 模拟时间静止的蚀刻感
                      ShaderMask(
                        shaderCallback: (bounds) => LinearGradient(
                          colors: [Colors.white, Colors.white24],
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ).createShader(bounds),
                        child: Text(
                          "解锁条件: ${badge['condition_key']} >= ${badge['target_value']}",
                          style: const TextStyle(color: Colors.white, fontSize: 12, fontFamily: 'monospace'),
                        ),
                      ),
                    ],
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

  const BadgeShaderWidget({
    super.key,
    required this.program,
    required this.isUnlocked,
    required this.materialType,
    required this.baseColor,
    required this.lightOffset,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return CustomPaint(
          size: Size(constraints.maxWidth, constraints.maxHeight),
          painter: BadgeShaderPainter(
            program: program,
            materialType: materialType,
            baseColor: baseColor,
            lightOffset: lightOffset,
          ),
        );
      },
    );
  }
}

class BadgeShaderPainter extends CustomPainter {
  final ui.FragmentProgram program;
  final double materialType;
  final Color baseColor;
  final Offset lightOffset;

  BadgeShaderPainter({
    required this.program,
    required this.materialType,
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

    final paint = Paint()..shader = shader;
    
    // Draw the shader rect
    canvas.drawRect(Offset.zero & size, paint);
  }

  @override
  bool shouldRepaint(covariant BadgeShaderPainter oldDelegate) {
    return oldDelegate.lightOffset != lightOffset ||
           oldDelegate.baseColor != baseColor ||
           oldDelegate.materialType != materialType;
  }
}
