import 'dart:async';
import 'dart:convert';
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
             
             // 精准速度临界点拦截 (下压最低点准备回弹时，速度变号)
             double velocity = springSimulation.dx(simTime);
             // 我们在这个微观维度很难完美截获0，所以我们设定一个下落到底部且速度接近0的极窄窗口
             // 由于这是动画构建，最好使用一个标志位。为了简单起见，这里假设框架足够快能够捕捉到。
             // 更稳妥的方式是在控制器里用 Listener 触发。但这暂时足够产生效果，我们只需用一个简单的判断。
        }

        double scale = Curves.easeOutBack.transform((t * 2 - delay).clamp(0.0, 1.0));
        double opacity = t > delay ? Curves.easeIn.transform(((t - delay) * 5).clamp(0.0, 1.0)) : 0.0;
        
        // 我们利用进度差值制造下落感 (-50 代表距离终点上方 50 像素)
        double yOffset = (1.0 - progress) * -150.0;

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
                  materialType: materialType == 'Cyber' ? 1.0 :
                                materialType == 'Liquid' ? 2.0 : 0.0,
                  baseColor: _parseColor(badge['visual_meta']?['base_color'], isUnlocked),
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

  Color _parseColor(String? hexString, bool isUnlocked) {
    if (!isUnlocked) return Colors.grey.shade900;
    if (hexString == null) return Colors.amber;
    final buffer = StringBuffer();
    if (hexString.length == 6 || hexString.length == 7) buffer.write('ff');
    buffer.write(hexString.replaceFirst('#', ''));
    return Color(int.parse(buffer.toString(), radix: 16));
  }

  void _showBadgeDetailOverlay(dynamic badge, String materialType) {
    _triggerHaptic(materialType);
    
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
                        color: _parseColor(badge['visual_meta']?['base_color'], true).withValues(alpha: 0.3),
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
                          materialType: materialType == 'Cyber' ? 1.0 :
                                        materialType == 'Liquid' ? 2.0 : 0.0,
                          baseColor: _parseColor(badge['visual_meta']?['base_color'], true),
                          lightOffset: const Offset(0, 0), // 中心锁定光源
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
                      const SizedBox(height: 24),
                      // ── 分享海报按钮 ──
                      SizedBox(
                        width: double.infinity,
                        child: OutlinedButton.icon(
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
                          icon: const Icon(Icons.share_rounded, size: 18),
                          label: const Text('分享纪念封'),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: Colors.white70,
                            side: const BorderSide(color: Colors.white24),
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                            ),
                          ),
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
    // 构建 3D 透视矩阵
    final Matrix4 depthMatrix = Matrix4.identity()
      ..setEntry(3, 2, 0.001) // 焦距深度 (Z轴透视收缩)
      ..rotateX(-lightOffset.dy * 0.5) // 绑定陀螺仪俯仰 (Pitch)
      ..rotateY(lightOffset.dx * 0.5); // 绑定陀螺仪横滚 (Roll)

    return LayoutBuilder(
      builder: (context, constraints) {
        return Transform(
          transform: depthMatrix,
          alignment: FractionalOffset.center,
          child: CustomPaint(
            size: Size(constraints.maxWidth, constraints.maxHeight),
            painter: BadgeShaderPainter(
              program: program,
              materialType: materialType,
              baseColor: baseColor,
              // 光斑逆向偏移
              lightOffset: Offset(-lightOffset.dx, -lightOffset.dy),
            ),
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
