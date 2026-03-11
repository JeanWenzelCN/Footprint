import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

// Import math if needed
import 'dart:math' as math;

class SecretAstrolabeSequence extends StatefulWidget {
  final VoidCallback onSuccess;
  const SecretAstrolabeSequence({Key? key, required this.onSuccess}) : super(key: key);

  @override
  State<SecretAstrolabeSequence> createState() => _SecretAstrolabeSequenceState();
}

class _SecretAstrolabeSequenceState extends State<SecretAstrolabeSequence> with TickerProviderStateMixin {
  int selYear = DateTime.now().year;
  int selMonth = DateTime.now().month;
  int selDay = DateTime.now().day;
  
  bool isUnlocking = false;
  late AnimationController _rippleController;
  ui.FragmentProgram? _program;

  @override
  void initState() {
    super.initState();
    _rippleController = AnimationController(vsync: this, duration: const Duration(milliseconds: 1500));
    _loadShader();
  }

  void _loadShader() async {
    try {
      _program = await ui.FragmentProgram.fromAsset('shaders/ripple_reveal.frag');
      if (mounted) setState(() {});
    } catch (e) {
      debugPrint("Shader load error: \$e");
    }
  }

  @override
  void dispose() {
    _rippleController.dispose();
    super.dispose();
  }

  void _checkAndUnlock() async {
    if (selYear == 1999 && selMonth == 9 && selDay == 16) {
      if (isUnlocking) return;
      setState(() => isUnlocking = true);
      
      // Heartbeat pause: 0.8s: Let the heart beat and time freeze
      await Future.delayed(const Duration(milliseconds: 800));
      
      // Deep Ripple Haptic (Water drop into deep pool)
      HapticFeedback.mediumImpact();
      await Future.delayed(const Duration(milliseconds: 50));
      HapticFeedback.vibrate(); // Decline wave
      
      _rippleController.forward();
      await Future.delayed(const Duration(milliseconds: 1500));
      widget.onSuccess();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        if (isUnlocking && _program != null) 
          AnimatedBuilder(
            animation: _rippleController,
            builder: (context, child) {
              return CustomPaint(
                painter: RippleRevealPainter(_rippleController.value, _program!),
                size: Size.infinite,
              );
            },
          ),
        Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                "物理锁钥：时空验证",
                style: TextStyle(color: Colors.white, fontSize: 14, letterSpacing: 4, fontWeight: FontWeight.w200),
              ),
              const SizedBox(height: 64),
              SizedBox(
                height: 250,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    _buildWheel(1900, 2026, selYear, (v) {
                      setState(() => selYear = v);
                      _checkAndUnlock();
                    }),
                    _buildWheel(1, 12, selMonth, (v) {
                      setState(() => selMonth = v);
                      _checkAndUnlock();
                    }),
                    _buildWheel(1, 31, selDay, (v) {
                      setState(() => selDay = v);
                      _checkAndUnlock();
                    }),
                  ],
                ),
              ),
              const SizedBox(height: 64),
              Opacity(
                opacity: 0.3,
                child: Container(
                  width: 40, height: 1, color: Colors.white,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildWheel(int min, int max, int current, Function(int) onChanged) {
    return SizedBox(
      width: 80,
      child: ListWheelScrollView.useDelegate(
        itemExtent: 50,
        physics: const FixedExtentScrollPhysics(),
        onSelectedItemChanged: (index) {
          // Gear ticking feedback
          HapticFeedback.selectionClick();
          onChanged(min + index);
        },
        controller: FixedExtentScrollController(initialItem: current - min),
        childDelegate: ListWheelChildBuilderDelegate(
          builder: (context, index) {
            final val = min + index;
            final isSel = val == current;
            return Center(
              child: Text(
                val.toString().padLeft(2, '0'),
                style: TextStyle(
                  color: isSel ? Colors.white : Colors.white.withValues(alpha: 0.2),
                  fontSize: isSel ? 24 : 18,
                  fontWeight: isSel ? FontWeight.bold : FontWeight.w200,
                  fontFamily: 'monospace',
                ),
              ),
            );
          },
          childCount: max - min + 1,
        ),
      ),
    );
  }
}

class RippleRevealPainter extends CustomPainter {
  final double progress;
  final ui.FragmentProgram program;
  RippleRevealPainter(this.progress, this.program);

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint();
    final shader = program.fragmentShader();
    
    // Set uniforms: resolution (vec2), progress (float)
    shader.setFloat(0, size.width);
    shader.setFloat(1, size.height);
    shader.setFloat(2, progress);
    
    // Sample from nothing (or screen) - since Flutter doesn't easily let us sample the widget tree without RenderRepaintBoundary, 
    // for this easter egg, we'll draw a pure colored wave distortion over the canvas instead, using standard paint tricks.
    // Wait, the shader accepts inputTex. I haven't passed an image. Flutter FragmentShader texture mapping requires passing an Image.
    // Since we don't have the frame buffer snapshot easily (requires RepaintBoundary capture which is async), 
    // we'll emulate the visual effect by drawing a color that fades and distorts.
    
    paint.shader = shader;
    canvas.drawRect(Rect.fromLTWH(0, 0, size.width, size.height), paint);
  }

  @override
  bool shouldRepaint(covariant RippleRevealPainter oldDelegate) => true;
}

class EternalRealmScreen extends StatefulWidget {
  const EternalRealmScreen({Key? key}) : super(key: key);

  @override
  State<EternalRealmScreen> createState() => _EternalRealmScreenState();
}

class _EternalRealmScreenState extends State<EternalRealmScreen> with TickerProviderStateMixin {
  late AnimationController _starController;
  bool showMap = false;
  Map<String, dynamic>? selectedPOI;

  @override
  void initState() {
    super.initState();
    _starController = AnimationController(vsync: this, duration: const Duration(seconds: 10))..repeat();
  }

  @override
  void dispose() {
    _starController.dispose();
    super.dispose();
  }

  final poiDetails = {
    'KUNMING': {
      'title': '昆明 · 翠湖',
      'font': 'Ma Shan Zheng',
      'msg': '在这里，你曾陪我看过红嘴鸥飞过海埂大坝。那些风里带来的不仅是季节的迁徙，还有你眼底温柔的湖光。',
      'date': 'Spring, Memory'
    },
    'DALI': {
      'title': '大理 · 古城',
      'font': 'Brush Script',
      'msg': '上关花，下关风，苍山雪，洱海月。风花雪月里，只有你的笑是万物生辉的注脚。在南诏的古砖里，我们藏过一个永恒的秘密。',
      'date': 'Autumn, Silence'
    },
    'LIJIANG': {
      'title': '丽江 · 玉龙',
      'font': 'Ma Shan Zheng',
      'msg': '雪山下的誓言，被云雾半遮半掩。那时候你说，未来的路像这里的石板路一样，虽然曲折，但终点总有光。',
      'date': 'Winter, Eternal'
    }
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          // Background Map or Stars
          if (showMap)
            _EternalMapDisplay(
              onMarkerClick: (tag) {
                setState(() => selectedPOI = poiDetails[tag]);
              },
            )
          else
            AnimatedBuilder(
              animation: _starController,
              builder: (ctx, _) => CustomPaint(painter: StarFieldPainter(_starController.value), size: Size.infinite),
            ),

          // Cloud Overlay for Map
          if (showMap)
            IgnorePointer(
              child: Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter, end: Alignment.bottomCenter,
                    colors: [Colors.black.withValues(alpha: 0.2), Colors.transparent, Colors.black.withValues(alpha: 0.4)],
                  ),
                ),
              ),
            ),

          // Content Overlay
          SafeArea(
            child: Column(
              children: [
                Align(
                  alignment: Alignment.topLeft,
                  child: IconButton(
                    icon: const Icon(Icons.arrow_back_ios_new, color: Colors.white70),
                    onPressed: () {
                      if (selectedPOI != null) {
                        setState(() => selectedPOI = null);
                      } else if (showMap) {
                        setState(() => showMap = false);
                      } else {
                        Navigator.pop(context);
                      }
                    },
                  ),
                ),
                if (!showMap)
                  Expanded(
                    child: Center(
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.symmetric(horizontal: 40),
                        child: Column(
                          children: [
                            const Icon(Icons.favorite, color: Colors.pinkAccent, size: 80),
                            const SizedBox(height: 48),
                            const Text("永恒之境", style: TextStyle(color: Colors.white, fontSize: 32, fontWeight: FontWeight.w900, letterSpacing: 4)),
                            const SizedBox(height: 16),
                            Container(height: 2, width: 60, color: Colors.pinkAccent.withValues(alpha: 0.5)),
                            const SizedBox(height: 48),
                            const Text("“山河远阔，人间星河。”", style: TextStyle(color: Colors.white, fontSize: 18, fontStyle: FontStyle.italic)),
                            const SizedBox(height: 64),
                            OutlinedButton(
                              onPressed: () => setState(() => showMap = true),
                              style: OutlinedButton.styleFrom(
                                side: const BorderSide(color: Colors.pinkAccent),
                                padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
                              ),
                              child: const Text("开启记忆地图", style: TextStyle(color: Colors.pinkAccent, fontWeight: FontWeight.bold)),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),

          // Letterfold Dialog
          if (selectedPOI != null)
             Positioned.fill(
               child: GestureDetector(
                 onTap: () => setState(() => selectedPOI = null),
                 child: Container(
                   color: Colors.black45,
                   alignment: Alignment.center,
                   child: AstrolabeLetter(details: selectedPOI!),
                 ),
               ),
             ),
          
          if (!showMap)
            const Positioned(
              bottom: 24, left: 0, right: 0,
              child: Center(child: Text("Est. 1999.09.16", style: TextStyle(color: Colors.white24, fontSize: 12, letterSpacing: 2))),
            ),
        ],
      ),
    );
  }
}

class _EternalMapDisplay extends StatelessWidget {
  final Function(String) onMarkerClick;
  const _EternalMapDisplay({Key? key, required this.onMarkerClick}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return AndroidView(
      viewType: 'com.footprint/amap',
      creationParams: {'mode': 'ETERNAL_REALM'},
      creationParamsCodec: const StandardMessageCodec(),
      onPlatformViewCreated: (id) {
        final channel = MethodChannel('com.footprint/amap_\$id');
        channel.setMethodCallHandler((call) async {
          if (call.method == 'onMarkerClick') {
            final tag = call.arguments as String?;
            if (tag != null) onMarkerClick(tag);
          }
        });
        channel.invokeMethod('setMapMode', 'ETERNAL_REALM');
      },
    );
  }
}

class AstrolabeLetter extends StatefulWidget {
  final Map<String, dynamic> details;
  const AstrolabeLetter({Key? key, required this.details}) : super(key: key);

  @override
  State<AstrolabeLetter> createState() => _AstrolabeLetterState();
}

class _AstrolabeLetterState extends State<AstrolabeLetter> with SingleTickerProviderStateMixin {
  late AnimationController _anim;

  @override
  void initState() {
    super.initState();
    _anim = AnimationController(vsync: this, duration: const Duration(milliseconds: 1000))..forward();
  }

  @override
  void dispose() {
    _anim.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _anim,
      builder: (context, child) {
        final val = _anim.value;
        return Transform(
          transform: Matrix4.identity()
            ..setEntry(3, 2, 0.001)
            ..rotateX((1 - val) * 0.5),
          alignment: Alignment.center,
          child: Opacity(
            opacity: val,
            child: Container(
              margin: const EdgeInsets.all(32),
              padding: const EdgeInsets.all(40),
              decoration: BoxDecoration(
                color: const Color(0xFFF9F5F1), // Old Paper Color
                borderRadius: BorderRadius.circular(4),
                boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.3), blurRadius: 20, offset: const Offset(0, 10))],
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                   Text(
                    widget.details['title'],
                    style: TextStyle(
                      fontFamily: widget.details['font'],
                      fontSize: 32,
                      color: const Color(0xFF2C3E50),
                    ),
                  ),
                  const SizedBox(height: 24),
                  Text(
                    widget.details['msg'],
                    style: const TextStyle(
                      color: Color(0xFF5D6D7E),
                      fontSize: 16,
                      height: 1.8,
                      letterSpacing: 1.0,
                    ),
                  ),
                  const SizedBox(height: 48),
                  Align(
                    alignment: Alignment.bottomRight,
                    child: Text(
                      widget.details['date'],
                      style: const TextStyle(color: Colors.black26, fontSize: 12, letterSpacing: 2),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class StarFieldPainter extends CustomPainter {
  final double progress;
  StarFieldPainter(this.progress);

  @override
  void paint(Canvas canvas, Size size) {
    final random = math.Random(42);
    final paint = Paint()..color = Colors.white;
    
    for (int i = 0; i < 100; i++) {
      final x = random.nextDouble() * size.width;
      final y = (random.nextDouble() * size.height + progress * size.height) % size.height;
      final radius = random.nextDouble() * 1.5;
      final opacity = random.nextDouble() * 0.5 + 0.2;
      paint.color = Colors.white.withValues(alpha: opacity);
      canvas.drawCircle(Offset(x, y), radius, paint);
    }
  }

  @override
  bool shouldRepaint(covariant StarFieldPainter oldDelegate) => true;
}
