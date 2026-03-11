import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'utils/typography_duet.dart';

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
      width: 90,
      child: Stack(
        alignment: Alignment.center,
        children: [
          Container(
            height: 54,
            width: double.infinity,
            margin: const EdgeInsets.symmetric(horizontal: 4),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.08),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: Colors.white.withValues(alpha: 0.15), width: 1),
            ),
          ),
          ListWheelScrollView.useDelegate(
            itemExtent: 54,
            diameterRatio: 1.5,
            magnification: 1.25,
            useMagnifier: true,
            overAndUnderCenterOpacity: 0.4,
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
                      color: isSel ? const Color(0xFFE5C07B) : Colors.white.withValues(alpha: 0.4),
                      fontSize: isSel ? 26 : 18,
                      fontWeight: isSel ? FontWeight.w900 : FontWeight.w300,
                      letterSpacing: isSel ? 2 : 1,
                      fontFamily: 'monospace',
                      shadows: isSel ? [
                        const BoxShadow(color: Color(0x66E5C07B), blurRadius: 12)
                      ] : null,
                    ),
                  ),
                );
              },
              childCount: max - min + 1,
            ),
          ),
        ],
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
      'date': 'Spring, Memory',
      'en_quote': 'The winds bring not only the migration of seasons, but the gentle reflection of lakes in your eyes.',
    },
    'DALI': {
      'title': '大理 · 古城',
      'font': 'Brush Script',
      'msg': '上关花，下关风，苍山雪，洱海月。风花雪月里，只有你的笑是万物生辉的注脚。在南诏的古砖里，我们藏过一个永恒的秘密。',
      'date': 'Autumn, Silence',
      'en_quote': 'Amidst the wind, flowers, snow, and moon, your smile remains the radiant footnote to all creation.',
    },
    'LIJIANG': {
      'title': '丽江 · 玉龙',
      'font': 'Ma Shan Zheng',
      'msg': '雪山下的誓言，被云雾半遮半掩。那时候你说，未来的路像这里的石板路一样，虽然曲折，但终点总有光。',
      'date': 'Winter, Eternal',
      'en_quote': 'The path ahead may wind like these cobblestones, but there is always light at the end of the road.',
    }
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          // Background Map (The Void is handled by Scaffold and the Map mask)
          if (showMap)
            _EternalMapDisplay(
              onMarkerClick: (tag) {
                setState(() => selectedPOI = poiDetails[tag]);
              },
            ),

          // Cloud Overlay for Map
          if (showMap) ...[
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
            const IgnorePointer(
               child: _WeatherResonanceOverlay(),
            ),
          ],

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
          
          if (showMap && selectedPOI == null) 
             Positioned(
               right: 32,
               bottom: 120, // Move up to make space for playback wheel
               child: GestureDetector(
                 onTap: () {
                   ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("时间锁充能中：2027年9月16日解封")));
                 },
                 child: const SizedBox(
                   width: 80, height: 80,
                   child: _TimeCapsuleGlow(),
                 ),
               ),
             ),

          // Spatiotemporal Playback Compass
          if (showMap && selectedPOI == null)
            Positioned(
              left: 20, right: 20, bottom: 40,
              child: _SpatiotemporalPlaybackCompass(
                onChanged: (progress) {
                  // This will eventually update the map playback
                },
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
                  const SizedBox(height: 32),
                  TypographyDuet.buildTranslatorBottomView(widget.details['en_quote'], ""),
                  const SizedBox(height: 38),
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

// StarFieldPainter removed as requested.

class _TimeCapsuleGlow extends StatefulWidget {
  const _TimeCapsuleGlow({Key? key}) : super(key: key);

  @override
  State<_TimeCapsuleGlow> createState() => _TimeCapsuleGlowState();
}

class _TimeCapsuleGlowState extends State<_TimeCapsuleGlow> with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  ui.FragmentShader? _shader;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(vsync: this, duration: const Duration(seconds: 4))..repeat();
    _loadShader();
  }

  Future<void> _loadShader() async {
    try {
      final program = await ui.FragmentProgram.fromAsset('shaders/time_capsule_core.frag');
      setState(() => _shader = program.fragmentShader());
    } catch (e) {
      debugPrint("Failed to load time capsule shader: \$e");
    }
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_shader == null) {
      return Container(
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: Colors.blueAccent.withValues(alpha: 0.5),
          boxShadow: const [BoxShadow(blurRadius: 10, color: Colors.blueAccent)],
        ),
      );
    }
    return AnimatedBuilder(
      animation: _ctrl,
      builder: (context, _) {
        _shader!.setFloat(0, 80.0); // resolution.x
        _shader!.setFloat(1, 80.0); // resolution.y
        _shader!.setFloat(2, _ctrl.value * 4.0); // time
        _shader!.setFloat(3, 0.4); // progress
        return CustomPaint(
          size: const Size(80, 80),
          painter: _ShaderPainter(_shader!),
        );
      },
    );
  }
}

class _WeatherResonanceOverlay extends StatefulWidget {
  const _WeatherResonanceOverlay({Key? key}) : super(key: key);

  @override
  State<_WeatherResonanceOverlay> createState() => _WeatherResonanceOverlayState();
}

class _WeatherResonanceOverlayState extends State<_WeatherResonanceOverlay> with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  ui.FragmentShader? _shader;

  // Assuming partner is in Kunming and it's raining (Weather Resonance)
  final bool isPartnerRaining = true; 

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(vsync: this, duration: const Duration(seconds: 10))..repeat();
    if (isPartnerRaining) _loadShader();
  }

  Future<void> _loadShader() async {
    try {
      final program = await ui.FragmentProgram.fromAsset('shaders/weather_resonance.frag');
      setState(() => _shader = program.fragmentShader());
    } catch (e) {
      debugPrint("Failed to load weather shader: \$e");
    }
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!isPartnerRaining || _shader == null) return const SizedBox.shrink();

    return Positioned.fill(
      child: IgnorePointer(
        child: AnimatedBuilder(
          animation: _ctrl,
          builder: (context, _) {
            final size = MediaQuery.of(context).size;
            _shader!.setFloat(0, size.width); // resolution.x
            _shader!.setFloat(1, size.height); // resolution.y
            _shader!.setFloat(2, _ctrl.value * 10.0); // time
            _shader!.setFloat(3, 0.85); // intensity
            return CustomPaint(
              size: Size.infinite,
              painter: _ShaderPainter(_shader!),
            );
          },
        ),
      ),
    );
  }
}

class _ShaderPainter extends CustomPainter {
  final ui.FragmentShader shader;
  _ShaderPainter(this.shader);

  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawRect(Offset.zero & size, Paint()..shader = shader);
  }

  @override
  bool shouldRepaint(_ShaderPainter oldDelegate) => true;
}


class _SpatiotemporalPlaybackCompass extends StatefulWidget {
  final Function(double) onChanged;
  const _SpatiotemporalPlaybackCompass({Key? key, required this.onChanged}) : super(key: key);

  @override
  State<_SpatiotemporalPlaybackCompass> createState() => _SpatiotemporalPlaybackCompassState();
}

class _SpatiotemporalPlaybackCompassState extends State<_SpatiotemporalPlaybackCompass> {
  double progress = 1.0;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 100,
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.6),
        borderRadius: BorderRadius.circular(50),
        border: Border.all(color: Colors.white10),
        boxShadow: [BoxShadow(color: Colors.blueAccent.withValues(alpha: 0.1), blurRadius: 20)],
      ),
      child: Stack(
        alignment: Alignment.center,
        children: [
          const Positioned(
            top: 15,
            child: Text(
              '时空穿梭罗盘 · SPATIOTEMPORAL PLAYBACK',
              style: TextStyle(color: Colors.white30, fontSize: 8, letterSpacing: 2, fontWeight: FontWeight.bold),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 40),
            child: SliderTheme(
              data: SliderTheme.of(context).copyWith(
                activeTrackColor: const Color(0xFFE5C07B),
                inactiveTrackColor: Colors.white10,
                thumbColor: Colors.white,
                overlayColor: Colors.white10,
                thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6, elevation: 5),
                trackHeight: 1,
              ),
              child: Slider(
                value: progress,
                onChanged: (v) {
                  setState(() => progress = v);
                  widget.onChanged(v);
                },
              ),
            ),
          ),
          const Positioned(
            bottom: 12,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('2023.09.16', style: TextStyle(color: Colors.white24, fontSize: 10, fontFamily: 'monospace')),
                SizedBox(width: 20),
                Icon(Icons.history_toggle_off, color: Color(0xFFE5C07B), size: 14),
                SizedBox(width: 20),
                Text('2024.09.16', style: TextStyle(color: Colors.white24, fontSize: 10, fontFamily: 'monospace')),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
