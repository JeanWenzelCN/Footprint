import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'utils/typography_duet.dart';

// Import math if needed
import 'dart:math' as math;

class SecretAstrolabeSequence extends StatefulWidget {
  final VoidCallback onSuccess;
  final String username;
  const SecretAstrolabeSequence({Key? key, required this.onSuccess, this.username = ""}) : super(key: key);

  @override
  State<SecretAstrolabeSequence> createState() => _SecretAstrolabeSequenceState();
}

class _SecretAstrolabeSequenceState extends State<SecretAstrolabeSequence> with TickerProviderStateMixin {
  int selYear = DateTime.now().year;
  int selMonth = DateTime.now().month;
  int selDay = DateTime.now().day;
  
  bool isUnlocking = false;
  late AnimationController _rippleController;
  ui.FragmentShader? _shader;

  late FixedExtentScrollController _yearController;
  late FixedExtentScrollController _monthController;
  late FixedExtentScrollController _dayController;

  @override
  void initState() {
    super.initState();
    _rippleController = AnimationController(vsync: this, duration: const Duration(milliseconds: 1500));
    
    _yearController = FixedExtentScrollController(initialItem: selYear - 1900);
    _monthController = FixedExtentScrollController(initialItem: selMonth - 1);
    _dayController = FixedExtentScrollController(initialItem: selDay - 1);

    _loadShader();
  }

  void _loadShader() async {
    try {
      final program = await ui.FragmentProgram.fromAsset('shaders/ripple_reveal.frag');
      if (mounted) {
        setState(() {
          _shader = program.fragmentShader();
        });
      }
    } catch (e) {
      debugPrint("Shader load error: $e");
    }
  }

  @override
  void dispose() {
    _rippleController.dispose();
    _yearController.dispose();
    _monthController.dispose();
    _dayController.dispose();
    super.dispose();
  }

  void _checkAndUnlock() async {
    bool match = false;
    if (widget.username == "Lucas" || widget.username == "L\u0075\u0063\u0061\u0073") {
      if (selYear == 1999 && selMonth == 9 && selDay == 16) match = true;
    } else if (widget.username == "Ace") {
      if (selYear == 2024 && selMonth == 4 && selDay == 8) match = true;
    }

    if (match) {
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
      if (mounted) widget.onSuccess();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        if (isUnlocking && _shader != null) 
          AnimatedBuilder(
            animation: _rippleController,
            builder: (context, child) {
              return CustomPaint(
                painter: RippleRevealPainter(_rippleController.value, _shader!),
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
                    _buildWheel(1900, 2026, selYear, _yearController, (v) {
                      setState(() => selYear = v);
                      _checkAndUnlock();
                    }),
                    _buildWheel(1, 12, selMonth, _monthController, (v) {
                      setState(() => selMonth = v);
                      _checkAndUnlock();
                    }),
                    _buildWheel(1, 31, selDay, _dayController, (v) {
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

  Widget _buildWheel(int min, int max, int current, FixedExtentScrollController controller, Function(int) onChanged) {
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
            controller: controller,
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
  final ui.FragmentShader shader;
  RippleRevealPainter(this.progress, this.shader);

  @override
  void paint(Canvas canvas, Size size) {
    shader.setFloat(0, size.width);
    shader.setFloat(1, size.height);
    shader.setFloat(2, progress);
    final paint = Paint();
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
  bool showMap = false;
  Map<String, dynamic>? selectedPOI;

  final poiDetails = {
    'KUNMING': {
      'title': '昆明 · 翠湖',
      'font': 'Ma Shan Zheng',
      'msg': '在这里，你曾陪我看过红嘴鸥飞过海埂大坝。那些风里带来的不仅是季节的迁徙，还有你眼底温柔的湖光。',
      'date': 'Spring, Memory',
      'en_quote': 'The winds bring not only the migration of seasons, but the gentle reflection of lakes in your eyes.',
      'cn_trans': '“风中不仅有季节的变换，还有你眼底那抹温柔。”'
    },
    'DALI': {
      'title': '大理 · 古城',
      'font': 'Brush Script',
      'msg': '上关花，下关风，苍山雪，洱海月。风花雪月里，只有你的笑是万物生辉的注脚。在南诏的古砖里，我们藏过一个永恒的秘密。',
      'date': 'Autumn, Silence',
      'en_quote': 'Amidst the wind, flowers, snow, and moon, your smile remains the radiant footnote to all creation.',
      'cn_trans': '“风花雪月，都不如你的一抹浅笑。”'
    },
    'LIJIANG': {
      'title': '丽江 · 玉龙',
      'font': 'Ma Shan Zheng',
      'msg': '雪山下的誓言，被云雾半遮半掩。那时候你说，未来的路像这里的石板路一样，虽然曲折，但终点总有光。',
      'date': 'Winter, Eternal',
      'en_quote': 'The path ahead may wind like these cobblestones, but there is always light at the end of the road.',
      'cn_trans': '“前方的路或许如石阶般崎岖，但终点必定有光。”'
    }
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          if (showMap)
            _EternalMapDisplay(
              onMarkerClick: (tag) {
                setState(() => selectedPOI = poiDetails[tag]);
              },
            ),
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
               bottom: 120,
               child: GestureDetector(
                 onTap: () {
                   showGeneralDialog(
                     context: context,
                     barrierDismissible: true,
                     barrierLabel: '',
                     barrierColor: Colors.black26,
                     transitionDuration: const Duration(milliseconds: 600),
                     pageBuilder: (ctx, anim1, anim2) => const _QuantumTimeCapsuleDialog(),
                     transitionBuilder: (ctx, anim1, anim2, child) {
                       return FadeTransition(
                         opacity: anim1,
                         child: ScaleTransition(
                           scale: Tween<double>(begin: 0.8, end: 1.0).animate(
                             CurvedAnimation(parent: anim1, curve: Curves.easeOutBack),
                           ),
                           child: child,
                         ),
                       );
                     },
                   );
                 },
                 child: const SizedBox(
                   width: 80, height: 80,
                   child: _TimeCapsuleGlow(isUnlocked: true),
                 ),
               ),
             ),
          if (showMap && selectedPOI == null)
            Positioned(
              left: 20, right: 20, bottom: 40,
              child: _SpatiotemporalPlaybackCompass(
                onChanged: (progress) {},
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
        final channel = MethodChannel('com.footprint/amap_$id');
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
                  TypographyDuet.buildTranslatorBottomView(
                    widget.details['en_quote'] ?? "", 
                    widget.details['cn_trans'] ?? ""
                  ),
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

class _TimeCapsuleGlow extends StatefulWidget {
  final bool isUnlocked;
  const _TimeCapsuleGlow({Key? key, this.isUnlocked = false}) : super(key: key);

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
      debugPrint("Failed to load time capsule shader: $e");
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
          color: widget.isUnlocked ? Colors.cyanAccent.withValues(alpha: 0.5) : Colors.blueAccent.withValues(alpha: 0.5),
          boxShadow: [BoxShadow(blurRadius: 10, color: widget.isUnlocked ? Colors.cyanAccent : Colors.blueAccent)],
        ),
      );
    }
    return AnimatedBuilder(
      animation: _ctrl,
      builder: (context, _) {
        _shader!.setFloat(0, 80.0);
        _shader!.setFloat(1, 80.0);
        _shader!.setFloat(2, _ctrl.value * 4.0);
        _shader!.setFloat(3, widget.isUnlocked ? 1.0 : 0.4);
        return CustomPaint(
          size: const Size(80, 80),
          painter: _ShaderPainter(_shader!),
        );
      },
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
            top: 15, child: Text('时空穿梭罗盘 · SPATIOTEMPORAL PLAYBACK', style: TextStyle(color: Colors.white30, fontSize: 8, letterSpacing: 2, fontWeight: FontWeight.bold)),
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

// --- 量子时空胶囊 核心交互弹窗 ---

class _QuantumTimeCapsuleDialog extends StatefulWidget {
  const _QuantumTimeCapsuleDialog({Key? key}) : super(key: key);
  @override
  State<_QuantumTimeCapsuleDialog> createState() => _QuantumTimeCapsuleDialogState();
}

class _QuantumTimeCapsuleDialogState extends State<_QuantumTimeCapsuleDialog> {
  int _currentIndex = 0;
  final List<Map<String, dynamic>> _tabs = [
    {'icon': Icons.auto_awesome_motion, 'label': '记忆回溯', 'view': const _MemoryPlaybackView()},
    {'icon': Icons.inventory_2_outlined, 'label': '永恒档案', 'view': const _EternalArchivesView()},
    {'icon': Icons.history_edu, 'label': '时空信笺', 'view': const _FutureLetterView()},
  ];

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: MediaQuery.of(context).size.width * 0.9,
        height: MediaQuery.of(context).size.height * 0.7,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(32),
          border: Border.all(color: Colors.white.withValues(alpha: 0.15)),
        ),
        clipBehavior: Clip.antiAlias,
        child: Stack(
          children: [
            Positioned.fill(
              child: BackdropFilter(
                filter: ui.ImageFilter.blur(sigmaX: 16, sigmaY: 16),
                child: Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topLeft, end: Alignment.bottomRight,
                      colors: [const Color(0xFF1A1A2E).withValues(alpha: 0.8), const Color(0xFF16213E).withValues(alpha: 0.9)],
                    ),
                  ),
                ),
              ),
            ),
            Positioned(
              top: 0, left: 0, right: 0,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
                child: Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(color: Colors.cyanAccent.withValues(alpha: 0.1), shape: BoxShape.circle),
                      child: const Icon(Icons.blur_on, color: Colors.cyanAccent, size: 24),
                    ),
                    const SizedBox(width: 16),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Text("量子时空胶囊", style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold, letterSpacing: 1)),
                        const Text("QUANTUM TIME CAPSULE · VER: LUCAS.01", style: TextStyle(color: Colors.white38, fontSize: 9, letterSpacing: 1)),
                      ],
                    ),
                    const Spacer(),
                    IconButton(icon: const Icon(Icons.close, color: Colors.white38), onPressed: () => Navigator.pop(context)),
                  ],
                ),
              ),
            ),
            Positioned.fill(
              top: 90,
              bottom: 100,
              child: RepaintBoundary(
                child: IndexedStack(
                  index: _currentIndex,
                  children: _tabs.map<Widget>((tab) => tab['view'] as Widget).toList(growable: false),
                ),
              ),
            ),
            Positioned(
              bottom: 0, left: 0, right: 0,
              child: Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(border: Border(top: BorderSide(color: Colors.white.withValues(alpha: 0.05)))),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: List.generate(_tabs.length, (index) {
                    final isSel = _currentIndex == index;
                    return GestureDetector(
                      onTap: () => setState(() => _currentIndex = index),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                        decoration: BoxDecoration(color: isSel ? Colors.cyanAccent.withValues(alpha: 0.1) : Colors.transparent, borderRadius: BorderRadius.circular(20)),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(_tabs[index]['icon'], color: isSel ? Colors.cyanAccent : Colors.white24, size: 24),
                            const SizedBox(height: 6),
                            Text(_tabs[index]['label'], style: TextStyle(color: isSel ? Colors.cyanAccent : Colors.white24, fontSize: 10, fontWeight: isSel ? FontWeight.bold : FontWeight.normal)),
                          ],
                        ),
                      ),
                    );
                  }),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MemoryPlaybackView extends StatelessWidget {
  const _MemoryPlaybackView({Key? key}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    final memories = [
      {'year': '2023', 'title': '初遇 · 翠湖', 'desc': '海埂大坝的鸥群见证了故事的开始。'},
      {'year': '2024', 'title': '重逢 · 大理', 'desc': '洱海的风吹动了尘封的往事。'},
      {'year': '2025', 'title': '约定 · 玉龙', 'desc': '雪山之下，星河永灿。'},
    ];
    return ListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 10),
      itemCount: memories.length,
      itemBuilder: (context, index) {
        final m = memories[index];
        return Container(
          margin: const EdgeInsets.only(bottom: 20),
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.03), borderRadius: BorderRadius.circular(20), border: Border.all(color: Colors.white.withValues(alpha: 0.05))),
          child: Row(
            children: [
              Container(
                width: 50, height: 50,
                decoration: BoxDecoration(gradient: const LinearGradient(colors: [Colors.cyanAccent, Colors.blueAccent]), borderRadius: BorderRadius.circular(15)),
                child: Center(child: Text(m['year']!, style: const TextStyle(color: Colors.black, fontSize: 12, fontWeight: FontWeight.bold))),
              ),
              const SizedBox(width: 20),
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(m['title']!, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w600)), const SizedBox(height: 4), Text(m['desc']!, style: const TextStyle(color: Colors.white54, fontSize: 12))])),
              const Icon(Icons.arrow_forward_ios, color: Colors.white12, size: 14),
            ],
          ),
        );
      },
    );
  }
}

class _EternalArchivesView extends StatelessWidget {
  const _EternalArchivesView({Key? key}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          _buildStatRow("共处时光", "2,341 天", Icons.timer_outlined),
          _buildStatRow("足迹总里程", "12,480 KM", Icons.map_outlined),
          _buildStatRow("探索城市", "32 座", Icons.location_city_outlined),
          _buildStatRow("珍藏瞬间", "1,024 张", Icons.photo_library_outlined),
          const SizedBox(height: 32),
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(gradient: LinearGradient(colors: [Colors.cyanAccent.withValues(alpha: 0.1), Colors.blueAccent.withValues(alpha: 0.1)]), borderRadius: BorderRadius.circular(24), border: Border.all(color: Colors.cyanAccent.withValues(alpha: 0.1))),
            child: const Column(
              children: [
                Icon(Icons.verified_user_outlined, color: Colors.cyanAccent, size: 32),
                SizedBox(height: 12),
                Text("永恒之境 · 终极协议已生效", style: TextStyle(color: Colors.cyanAccent, fontSize: 12, fontWeight: FontWeight.bold)),
                SizedBox(height: 8),
                Text("本档案由量子纠缠技术加密，跨越所有平行时空，永不毁灭。", textAlign: TextAlign.center, style: TextStyle(color: Colors.white38, fontSize: 10, height: 1.5)),
              ],
            ),
          ),
        ],
      ),
    );
  }
  Widget _buildStatRow(String label, String value, IconData icon) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: [
          Icon(icon, color: Colors.white24, size: 20),
          const SizedBox(width: 16),
          Text(label, style: const TextStyle(color: Colors.white70, fontSize: 14)),
          const Spacer(),
          Text(value, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold, fontFamily: 'monospace')),
        ],
      ),
    );
  }
}

class _FutureLetterView extends StatelessWidget {
  const _FutureLetterView({Key? key}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.auto_fix_high, color: Colors.cyanAccent, size: 48),
          const SizedBox(height: 24),
          const Text("写给平行时空的信", style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          const Text("在这里留下的文字，将通过量子波函数发送至任意时间支流。无论过去还是未来，这份思念都将被接收。", textAlign: TextAlign.center, style: TextStyle(color: Colors.white54, fontSize: 12, height: 1.6)),
          const SizedBox(height: 48),
          ElevatedButton(
            onPressed: () { ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("量子信道繁忙中，思念已预存至云端..."))); },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.cyanAccent, foregroundColor: Colors.black, minimumSize: const Size(double.infinity, 54), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)), elevation: 0),
            child: const Text("开启量子书写模式", style: TextStyle(fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}
