import 'dart:convert';
import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'utils/typography_duet.dart';

const MethodChannel _realmDataChannel = MethodChannel('com.footprint/data');
const String _eternalAssetPath = 'assets/eternal_realm_yunnan.json';

const List<Map<String, dynamic>> _eternalSpecialDays = [
  {
    'month': 9,
    'day': 16,
    'title': '星图纪念日',
    'message': '今天的永恒之境会比平日更亮一点。',
  },
  {
    'month': 4,
    'day': 8,
    'title': '再遇日',
    'message': '有些相逢像春雷，一响就是很多年。',
  },
];

const List<Map<String, dynamic>> _cityPresets = [
  {'label': '尚未设置', 'lat': null, 'lng': null},
  {'label': '上海', 'lat': 31.2304, 'lng': 121.4737},
  {'label': '北京', 'lat': 39.9042, 'lng': 116.4074},
  {'label': '成都', 'lat': 30.5728, 'lng': 104.0668},
  {'label': '重庆', 'lat': 29.5630, 'lng': 106.5516},
  {'label': '杭州', 'lat': 30.2741, 'lng': 120.1551},
  {'label': '广州', 'lat': 23.1291, 'lng': 113.2644},
  {'label': '深圳', 'lat': 22.5431, 'lng': 114.0579},
  {'label': '西安', 'lat': 34.3416, 'lng': 108.9398},
  {'label': '武汉', 'lat': 30.5928, 'lng': 114.3055},
];

bool isEternalSpecialDay([DateTime? date]) {
  final now = date ?? DateTime.now();
  return _eternalSpecialDays
      .any((day) => day['month'] == now.month && day['day'] == now.day);
}

String? getEternalTodayBannerText([DateTime? date]) {
  final now = date ?? DateTime.now();
  for (final day in _eternalSpecialDays) {
    if (day['month'] == now.month && day['day'] == now.day) {
      return day['message'] as String?;
    }
  }
  return null;
}

String _formatDate(DateTime date) {
  return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
}

Color _hexColor(String value, {double opacity = 1}) {
  final sanitized = value.replaceAll('#', '');
  final buffer = StringBuffer();
  if (sanitized.length == 6) {
    buffer.write('ff');
  }
  buffer.write(sanitized);
  return Color(int.parse(buffer.toString(), radix: 16)).withValues(alpha: opacity);
}

String _seasonForMonth(int month) {
  if (month >= 3 && month <= 5) return 'spring';
  if (month >= 6 && month <= 8) return 'summer';
  if (month >= 9 && month <= 11) return 'autumn';
  return 'winter';
}

double _distanceKm(double lat1, double lng1, double lat2, double lng2) {
  const double earthRadiusKm = 6371.0;
  final dLat = (lat2 - lat1) * math.pi / 180.0;
  final dLng = (lng2 - lng1) * math.pi / 180.0;
  final a = math.sin(dLat / 2) * math.sin(dLat / 2) +
      math.cos(lat1 * math.pi / 180.0) *
          math.cos(lat2 * math.pi / 180.0) *
          math.sin(dLng / 2) *
          math.sin(dLng / 2);
  final c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a));
  return earthRadiusKm * c;
}

class EternalPlace {
  final String tag;
  final String title;
  final String subtitle;
  final double lat;
  final double lng;
  final String themeHex;
  final String accentHex;
  final String ambient;
  final String poem;
  final String msg;
  final String date;
  final String enQuote;
  final String cnTrans;

  const EternalPlace({
    required this.tag,
    required this.title,
    required this.subtitle,
    required this.lat,
    required this.lng,
    required this.themeHex,
    required this.accentHex,
    required this.ambient,
    required this.poem,
    required this.msg,
    required this.date,
    required this.enQuote,
    required this.cnTrans,
  });

  factory EternalPlace.fromJson(Map<String, dynamic> json) {
    return EternalPlace(
      tag: json['tag'] as String? ?? '',
      title: json['title'] as String? ?? '',
      subtitle: json['subtitle'] as String? ?? '',
      lat: (json['lat'] as num?)?.toDouble() ?? 0,
      lng: (json['lng'] as num?)?.toDouble() ?? 0,
      themeHex: json['themeHex'] as String? ?? '#6A8CAF',
      accentHex: json['accentHex'] as String? ?? '#F5E0A7',
      ambient: json['ambient'] as String? ?? '',
      poem: json['poem'] as String? ?? '',
      msg: json['msg'] as String? ?? '',
      date: json['date'] as String? ?? '',
      enQuote: json['enQuote'] as String? ?? '',
      cnTrans: json['cnTrans'] as String? ?? '',
    );
  }

  Color get themeColor => _hexColor(themeHex);
  Color get accentColor => _hexColor(accentHex);
}

class EternalMemory {
  final String id;
  final String year;
  final String title;
  final String date;
  final String placeTag;
  final String summary;
  final String message;

  const EternalMemory({
    required this.id,
    required this.year,
    required this.title,
    required this.date,
    required this.placeTag,
    required this.summary,
    required this.message,
  });

  factory EternalMemory.fromJson(Map<String, dynamic> json) {
    return EternalMemory(
      id: json['id'] as String? ?? '',
      year: json['year'] as String? ?? '',
      title: json['title'] as String? ?? '',
      date: json['date'] as String? ?? '',
      placeTag: json['placeTag'] as String? ?? '',
      summary: json['summary'] as String? ?? '',
      message: json['message'] as String? ?? '',
    );
  }
}

class EternalSeasonalNote {
  final String season;
  final String title;
  final String message;

  const EternalSeasonalNote({
    required this.season,
    required this.title,
    required this.message,
  });

  factory EternalSeasonalNote.fromJson(Map<String, dynamic> json) {
    return EternalSeasonalNote(
      season: json['season'] as String? ?? 'spring',
      title: json['title'] as String? ?? '',
      message: json['message'] as String? ?? '',
    );
  }
}

class EternalFutureTemplate {
  final String title;
  final String message;

  const EternalFutureTemplate({
    required this.title,
    required this.message,
  });

  factory EternalFutureTemplate.fromJson(Map<String, dynamic> json) {
    return EternalFutureTemplate(
      title: json['title'] as String? ?? '',
      message: json['message'] as String? ?? '',
    );
  }
}

class EternalRealmConfig {
  final String realmTitle;
  final String introQuote;
  final String introSubquote;
  final List<Map<String, dynamic>> anniversaries;
  final Map<String, dynamic> secretQuestion;
  final List<EternalSeasonalNote> seasonalNotes;
  final List<EternalFutureTemplate> futureTemplates;
  final List<String> privatePromises;
  final List<EternalPlace> places;
  final List<EternalMemory> memories;

  const EternalRealmConfig({
    required this.realmTitle,
    required this.introQuote,
    required this.introSubquote,
    required this.anniversaries,
    required this.secretQuestion,
    required this.seasonalNotes,
    required this.futureTemplates,
    required this.privatePromises,
    required this.places,
    required this.memories,
  });

  factory EternalRealmConfig.fromJson(Map<String, dynamic> json) {
    return EternalRealmConfig(
      realmTitle: json['realmTitle'] as String? ?? '永恒之境',
      introQuote: json['introQuote'] as String? ?? '',
      introSubquote: json['introSubquote'] as String? ?? '',
      anniversaries: (json['anniversaries'] as List<dynamic>? ?? [])
          .map((e) => Map<String, dynamic>.from(e as Map))
          .toList(),
      secretQuestion:
          Map<String, dynamic>.from(json['secretQuestion'] as Map? ?? const {}),
      seasonalNotes: (json['seasonalNotes'] as List<dynamic>? ?? [])
          .map((e) => EternalSeasonalNote.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList(),
      futureTemplates: (json['futureTemplates'] as List<dynamic>? ?? [])
          .map((e) =>
              EternalFutureTemplate.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList(),
      privatePromises: (json['privatePromises'] as List<dynamic>? ?? [])
          .map((e) => e.toString())
          .toList(),
      places: (json['places'] as List<dynamic>? ?? [])
          .map((e) => EternalPlace.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList(),
      memories: (json['memories'] as List<dynamic>? ?? [])
          .map((e) => EternalMemory.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList(),
    );
  }

  EternalPlace? placeByTag(String tag) {
    try {
      return places.firstWhere((place) => place.tag == tag);
    } catch (_) {
      return null;
    }
  }

  EternalSeasonalNote seasonalFor(DateTime now) {
    final currentSeason = _seasonForMonth(now.month);
    return seasonalNotes.firstWhere(
      (note) => note.season == currentSeason,
      orElse: () => const EternalSeasonalNote(
        season: 'spring',
        title: '此刻来信',
        message: '所有想念都会慢慢落回你的方向。',
      ),
    );
  }
}

class EternalFutureLetter {
  final String id;
  final String title;
  final String message;
  final String unlockType;
  final int createdAt;
  final int? unlockAt;
  final String? placeTag;
  final bool isUnlocked;
  final bool isOpened;

  const EternalFutureLetter({
    required this.id,
    required this.title,
    required this.message,
    required this.unlockType,
    required this.createdAt,
    this.unlockAt,
    this.placeTag,
    this.isUnlocked = false,
    this.isOpened = false,
  });

  factory EternalFutureLetter.fromJson(Map<String, dynamic> json) {
    return EternalFutureLetter(
      id: json['id'] as String? ?? '',
      title: json['title'] as String? ?? '',
      message: json['message'] as String? ?? '',
      unlockType: json['unlockType'] as String? ?? 'date',
      createdAt: (json['createdAt'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      unlockAt: (json['unlockAt'] as num?)?.toInt(),
      placeTag: json['placeTag'] as String?,
      isUnlocked: json['isUnlocked'] as bool? ?? false,
      isOpened: json['isOpened'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'message': message,
        'unlockType': unlockType,
        'createdAt': createdAt,
        'unlockAt': unlockAt,
        'placeTag': placeTag,
        'isUnlocked': isUnlocked,
        'isOpened': isOpened,
      };

  EternalFutureLetter copyWith({
    String? id,
    String? title,
    String? message,
    String? unlockType,
    int? createdAt,
    int? unlockAt,
    String? placeTag,
    bool? isUnlocked,
    bool? isOpened,
  }) {
    return EternalFutureLetter(
      id: id ?? this.id,
      title: title ?? this.title,
      message: message ?? this.message,
      unlockType: unlockType ?? this.unlockType,
      createdAt: createdAt ?? this.createdAt,
      unlockAt: unlockAt ?? this.unlockAt,
      placeTag: placeTag ?? this.placeTag,
      isUnlocked: isUnlocked ?? this.isUnlocked,
      isOpened: isOpened ?? this.isOpened,
    );
  }
}

class EternalBondProfile {
  final String yourLabel;
  final double? yourLat;
  final double? yourLng;
  final String herLabel;
  final String? herTag;
  final double? herLat;
  final double? herLng;

  const EternalBondProfile({
    required this.yourLabel,
    required this.yourLat,
    required this.yourLng,
    required this.herLabel,
    required this.herTag,
    required this.herLat,
    required this.herLng,
  });

  factory EternalBondProfile.fromJson(Map<String, dynamic> json) {
    return EternalBondProfile(
      yourLabel: json['yourLabel'] as String? ?? '远方的你',
      yourLat: (json['yourLat'] as num?)?.toDouble(),
      yourLng: (json['yourLng'] as num?)?.toDouble(),
      herLabel: json['herLabel'] as String? ?? '云南的她',
      herTag: json['herTag'] as String?,
      herLat: (json['herLat'] as num?)?.toDouble(),
      herLng: (json['herLng'] as num?)?.toDouble(),
    );
  }

  Map<String, dynamic> toJson() => {
        'yourLabel': yourLabel,
        'yourLat': yourLat,
        'yourLng': yourLng,
        'herLabel': herLabel,
        'herTag': herTag,
        'herLat': herLat,
        'herLng': herLng,
      };

  EternalBondProfile copyWith({
    String? yourLabel,
    double? yourLat,
    double? yourLng,
    String? herLabel,
    String? herTag,
    double? herLat,
    double? herLng,
  }) {
    return EternalBondProfile(
      yourLabel: yourLabel ?? this.yourLabel,
      yourLat: yourLat ?? this.yourLat,
      yourLng: yourLng ?? this.yourLng,
      herLabel: herLabel ?? this.herLabel,
      herTag: herTag ?? this.herTag,
      herLat: herLat ?? this.herLat,
      herLng: herLng ?? this.herLng,
    );
  }

  bool get isConfigured =>
      yourLat != null &&
      yourLng != null &&
      herLat != null &&
      herLng != null;
}

class SecretAstrolabeSequence extends StatefulWidget {
  final VoidCallback onSuccess;
  final String username;

  const SecretAstrolabeSequence({
    super.key,
    required this.onSuccess,
    this.username = '',
  });

  @override
  State<SecretAstrolabeSequence> createState() => _SecretAstrolabeSequenceState();
}

class _SecretAstrolabeSequenceState extends State<SecretAstrolabeSequence>
    with TickerProviderStateMixin {
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
    _rippleController =
        AnimationController(vsync: this, duration: const Duration(milliseconds: 1500));
    _yearController = FixedExtentScrollController(initialItem: selYear - 1900);
    _monthController = FixedExtentScrollController(initialItem: selMonth - 1);
    _dayController = FixedExtentScrollController(initialItem: selDay - 1);
    _loadShader();
  }

  Future<void> _loadShader() async {
    try {
      final program = await ui.FragmentProgram.fromAsset('shaders/ripple_reveal.frag');
      if (mounted) {
        setState(() => _shader = program.fragmentShader());
      }
    } catch (e) {
      debugPrint('Shader load error: $e');
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

  Future<void> _checkAndUnlock() async {
    bool match = false;
    if (widget.username == 'Lucas' || widget.username == 'L\u0075\u0063\u0061\u0073') {
      match = selYear == 1999 && selMonth == 9 && selDay == 16;
    } else if (widget.username == 'Ace') {
      match = selYear == 2024 && selMonth == 4 && selDay == 8;
    }

    if (!match || isUnlocking) return;

    setState(() => isUnlocking = true);
    await Future<void>.delayed(const Duration(milliseconds: 800));
    HapticFeedback.mediumImpact();
    await Future<void>.delayed(const Duration(milliseconds: 50));
    HapticFeedback.vibrate();
    _rippleController.forward();
    await Future<void>.delayed(const Duration(milliseconds: 1500));
    if (mounted) widget.onSuccess();
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
                '物理锁钥：时空验证',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 14,
                  letterSpacing: 4,
                  fontWeight: FontWeight.w200,
                ),
              ),
              const SizedBox(height: 16),
              Text(
                getEternalTodayBannerText() ?? '把那个只有她和你知道的日期慢慢拨回正确的位置。',
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.45),
                  fontSize: 11,
                  letterSpacing: 1.2,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 48),
              SizedBox(
                height: 250,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    _buildWheel(1900, 2026, selYear, _yearController, (value) {
                      setState(() => selYear = value);
                      _checkAndUnlock();
                    }),
                    _buildWheel(1, 12, selMonth, _monthController, (value) {
                      setState(() => selMonth = value);
                      _checkAndUnlock();
                    }),
                    _buildWheel(1, 31, selDay, _dayController, (value) {
                      setState(() => selDay = value);
                      _checkAndUnlock();
                    }),
                  ],
                ),
              ),
              const SizedBox(height: 64),
              Opacity(
                opacity: 0.3,
                child: Container(width: 40, height: 1, color: Colors.white),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildWheel(
    int min,
    int max,
    int current,
    FixedExtentScrollController controller,
    ValueChanged<int> onChanged,
  ) {
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
              HapticFeedback.selectionClick();
              onChanged(min + index);
            },
            controller: controller,
            childDelegate: ListWheelChildBuilderDelegate(
              builder: (context, index) {
                final value = min + index;
                final isSelected = value == current;
                return Center(
                  child: Text(
                    value.toString().padLeft(2, '0'),
                    style: TextStyle(
                      color: isSelected
                          ? const Color(0xFFE5C07B)
                          : Colors.white.withValues(alpha: 0.4),
                      fontSize: isSelected ? 26 : 18,
                      fontWeight: isSelected ? FontWeight.w900 : FontWeight.w300,
                      letterSpacing: isSelected ? 2 : 1,
                      fontFamily: 'monospace',
                      shadows: isSelected
                          ? const [
                              BoxShadow(
                                color: Color(0x66E5C07B),
                                blurRadius: 12,
                              ),
                            ]
                          : null,
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
    final paint = Paint()..shader = shader;
    canvas.drawRect(Offset.zero & size, paint);
  }

  @override
  bool shouldRepaint(covariant RippleRevealPainter oldDelegate) => true;
}

class EternalRealmScreen extends StatefulWidget {
  const EternalRealmScreen({super.key});

  @override
  State<EternalRealmScreen> createState() => _EternalRealmScreenState();
}

class _EternalRealmScreenState extends State<EternalRealmScreen>
    with TickerProviderStateMixin {
  EternalRealmConfig? _config;
  bool _loading = true;
  bool _activated = false;
  int _sectionIndex = 0;
  EternalPlace? _selectedPlace;
  List<EternalFutureLetter> _letters = const [];
  EternalBondProfile _bondProfile = const EternalBondProfile(
    yourLabel: '远方的你',
    yourLat: null,
    yourLng: null,
    herLabel: '云南的她',
    herTag: null,
    herLat: null,
    herLng: null,
  );
  bool _innerGateUnlocked = false;
  List<dynamic> _yunnanEntries = const [];
  List<dynamic> _yunnanCapsules = const [];
  MethodChannel? _mapChannel;

  @override
  void initState() {
    super.initState();
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    try {
      final configString = await rootBundle.loadString(_eternalAssetPath);
      final config = EternalRealmConfig.fromJson(
        jsonDecode(configString) as Map<String, dynamic>,
      );

      final nativeStateString =
          await _realmDataChannel.invokeMethod<String>('getEternalRealmState');
      final nativeState = nativeStateString == null
          ? <String, dynamic>{}
          : jsonDecode(nativeStateString) as Map<String, dynamic>;

      final lettersJson = nativeState['lettersJson'] as String? ?? '[]';
      final bondProfileJson = nativeState['bondProfileJson'] as String? ?? '{}';

      final entriesString = await _realmDataChannel.invokeMethod<String>(
        'getAllEntries',
        {'mode': 'ETERNAL_REALM'},
      );
      final capsulesString = await _realmDataChannel.invokeMethod<String>(
        'getAllTimeCapsules',
        {'mode': 'ETERNAL_REALM'},
      );

      final letters = (jsonDecode(lettersJson) as List<dynamic>? ?? [])
          .map((item) =>
              EternalFutureLetter.fromJson(Map<String, dynamic>.from(item as Map)))
          .toList();

      EternalBondProfile bondProfile;
      if (bondProfileJson.trim().isEmpty || bondProfileJson.trim() == '{}') {
        final defaultHer = config.placeByTag('ERHAI') ?? config.places.first;
        bondProfile = EternalBondProfile(
          yourLabel: '远方的你',
          yourLat: null,
          yourLng: null,
          herLabel: '云南的她',
          herTag: defaultHer.tag,
          herLat: defaultHer.lat,
          herLng: defaultHer.lng,
        );
      } else {
        bondProfile = EternalBondProfile.fromJson(
          jsonDecode(bondProfileJson) as Map<String, dynamic>,
        );
      }

      if (mounted) {
        setState(() {
          _config = config;
          _letters = letters;
          _bondProfile = bondProfile;
          _innerGateUnlocked = nativeState['innerGateUnlocked'] as bool? ?? false;
          _yunnanEntries = entriesString == null ? [] : jsonDecode(entriesString) as List<dynamic>;
          _yunnanCapsules =
              capsulesString == null ? [] : jsonDecode(capsulesString) as List<dynamic>;
          _loading = false;
        });
      }
      await _refreshLetterUnlocks();
    } catch (e) {
      if (mounted) {
        setState(() => _loading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('永恒之境加载失败: $e')),
        );
      }
    }
  }

  Future<void> _saveLetters() async {
    await _realmDataChannel.invokeMethod('saveEternalLetters',
        _letters.map((letter) => letter.toJson()).toList());
  }

  Future<void> _saveBondProfile() async {
    await _realmDataChannel.invokeMethod(
      'saveEternalBondProfile',
      _bondProfile.toJson(),
    );
    _syncBondPathToMap();
  }

  Future<void> _refreshLetterUnlocks({String? visitedPlaceTag}) async {
    if (_letters.isEmpty) return;
    final now = DateTime.now().millisecondsSinceEpoch;
    bool changed = false;
    final updated = _letters.map((letter) {
      bool shouldUnlock = letter.isUnlocked;
      if (!shouldUnlock) {
        if (letter.unlockType == 'date' &&
            letter.unlockAt != null &&
            now >= letter.unlockAt!) {
          shouldUnlock = true;
        }
        if (letter.unlockType == 'place' &&
            visitedPlaceTag != null &&
            visitedPlaceTag == letter.placeTag) {
          shouldUnlock = true;
        }
      }
      if (shouldUnlock != letter.isUnlocked) {
        changed = true;
        return letter.copyWith(isUnlocked: shouldUnlock);
      }
      return letter;
    }).toList();

    if (changed && mounted) {
      setState(() => _letters = updated);
      await _saveLetters();
    }
  }

  void _syncBondPathToMap() {
    if (_mapChannel == null || !_bondProfile.isConfigured) return;
    _mapChannel!.invokeMethod('setEternalBondPath', [
      {'lat': _bondProfile.yourLat, 'lng': _bondProfile.yourLng},
      {'lat': _bondProfile.herLat, 'lng': _bondProfile.herLng},
    ]);
  }

  Future<void> _onMapCreated(int id) async {
    _mapChannel = MethodChannel('com.footprint/amap_$id');
    _mapChannel!.setMethodCallHandler((call) async {
      if (call.method == 'onMarkerClick') {
        final tag = call.arguments as String?;
        if (tag != null) {
          final place = _config?.placeByTag(tag);
          if (place != null && mounted) {
            setState(() => _selectedPlace = place);
            await _refreshLetterUnlocks(visitedPlaceTag: tag);
          }
        }
      }
    });
    await _mapChannel!.invokeMethod('setMapMode', 'ETERNAL_REALM');
    _syncBondPathToMap();
  }

  void _focusPlace(EternalPlace place, {bool openOverlay = true}) {
    _mapChannel?.invokeMethod(
      'centerLocation',
      {'latitude': place.lat, 'longitude': place.lng, 'zoom': 10.8},
    );
    setState(() {
      _activated = true;
      _sectionIndex = 0;
      _selectedPlace = openOverlay ? place : null;
    });
    _refreshLetterUnlocks(visitedPlaceTag: place.tag);
  }

  Future<void> _showCreateLetterSheet({EternalFutureTemplate? template}) async {
    if (_config == null) return;
    final titleController = TextEditingController(text: template?.title ?? '');
    final messageController = TextEditingController(text: template?.message ?? '');
    String unlockType = 'date';
    DateTime unlockDate = DateTime.now().add(const Duration(days: 30));
    String selectedPlaceTag = _config!.places.first.tag;

    final createdLetter = await showModalBottomSheet<EternalFutureLetter>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setSheetState) {
            return Padding(
              padding: EdgeInsets.only(
                left: 16,
                right: 16,
                top: 16,
                bottom: MediaQuery.of(context).viewInsets.bottom + 16,
              ),
              child: Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: const Color(0xFF111827),
                  borderRadius: BorderRadius.circular(28),
                  border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '埋一封只给她的未来信',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: titleController,
                      style: const TextStyle(color: Colors.white),
                      decoration: _darkInputDecoration('标题'),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: messageController,
                      minLines: 4,
                      maxLines: 6,
                      style: const TextStyle(color: Colors.white),
                      decoration: _darkInputDecoration('写给她的话'),
                    ),
                    const SizedBox(height: 16),
                    SegmentedButton<String>(
                      segments: const [
                        ButtonSegment<String>(
                          value: 'date',
                          label: Text('到日期解锁'),
                          icon: Icon(Icons.calendar_today_outlined),
                        ),
                        ButtonSegment<String>(
                          value: 'place',
                          label: Text('到地点解锁'),
                          icon: Icon(Icons.place_outlined),
                        ),
                      ],
                      style: ButtonStyle(
                        foregroundColor:
                            WidgetStatePropertyAll(Colors.white.withValues(alpha: 0.9)),
                        backgroundColor: WidgetStateProperty.resolveWith((states) {
                          if (states.contains(WidgetState.selected)) {
                            return Colors.cyanAccent.withValues(alpha: 0.16);
                          }
                          return Colors.white.withValues(alpha: 0.05);
                        }),
                      ),
                      selected: {unlockType},
                      onSelectionChanged: (value) {
                        setSheetState(() => unlockType = value.first);
                      },
                    ),
                    const SizedBox(height: 12),
                    if (unlockType == 'date')
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: const Icon(Icons.event, color: Colors.cyanAccent),
                        title: Text(
                          '${unlockDate.year}-${unlockDate.month.toString().padLeft(2, '0')}-${unlockDate.day.toString().padLeft(2, '0')}',
                          style: const TextStyle(color: Colors.white),
                        ),
                        subtitle: const Text(
                          '到了这一天，她就可以打开',
                          style: TextStyle(color: Colors.white54),
                        ),
                        trailing: const Icon(Icons.chevron_right, color: Colors.white38),
                        onTap: () async {
                          final picked = await showDatePicker(
                            context: context,
                            initialDate: unlockDate,
                            firstDate: DateTime.now(),
                            lastDate: DateTime(2099),
                            locale: const Locale('zh'),
                          );
                          if (picked != null) {
                            setSheetState(() => unlockDate = picked);
                          }
                        },
                      )
                    else
                      DropdownButtonFormField<String>(
                        value: selectedPlaceTag,
                        dropdownColor: const Color(0xFF182235),
                        decoration: _darkInputDecoration('抵达哪里时解锁'),
                        items: _config!.places
                            .map(
                              (place) => DropdownMenuItem(
                                value: place.tag,
                                child: Text(
                                  place.title,
                                  style: const TextStyle(color: Colors.white),
                                ),
                              ),
                            )
                            .toList(),
                        onChanged: (value) {
                          if (value != null) {
                            setSheetState(() => selectedPlaceTag = value);
                          }
                        },
                      ),
                    const SizedBox(height: 20),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: () {
                          if (titleController.text.trim().isEmpty ||
                              messageController.text.trim().isEmpty) {
                            return;
                          }
                          final letter = EternalFutureLetter(
                            id: 'letter_${DateTime.now().millisecondsSinceEpoch}',
                            title: titleController.text.trim(),
                            message: messageController.text.trim(),
                            unlockType: unlockType,
                            createdAt: DateTime.now().millisecondsSinceEpoch,
                            unlockAt: unlockType == 'date'
                                ? unlockDate.millisecondsSinceEpoch
                                : null,
                            placeTag: unlockType == 'place' ? selectedPlaceTag : null,
                          );
                          Navigator.pop(context, letter);
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.cyanAccent,
                          foregroundColor: Colors.black,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                        child: const Text('埋入未来信箱'),
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );

    if (createdLetter != null && mounted) {
      setState(() => _letters = [createdLetter, ..._letters]);
      await _saveLetters();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('新的未来信已经藏好了。')),
        );
      }
    }
  }

  Future<void> _openLetter(EternalFutureLetter letter) async {
    if (!letter.isUnlocked) {
      final hint = letter.unlockType == 'date'
          ? '它还在等时间抵达。'
          : '它还在等你走到那片风景里。';
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(hint)));
      return;
    }

    final updatedLetters = _letters
        .map((item) => item.id == letter.id ? item.copyWith(isOpened: true) : item)
        .toList();
    setState(() => _letters = updatedLetters);
    await _saveLetters();

    if (!mounted) return;
    showDialog<void>(
      context: context,
      builder: (context) => Dialog(
        backgroundColor: Colors.transparent,
        child: Container(
          padding: const EdgeInsets.all(28),
          decoration: BoxDecoration(
            color: const Color(0xFFF8F2E9),
            borderRadius: BorderRadius.circular(26),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.28),
                blurRadius: 30,
                offset: const Offset(0, 16),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                letter.title,
                style: const TextStyle(
                  color: Color(0xFF24324A),
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 18),
              Text(
                letter.message,
                style: const TextStyle(
                  color: Color(0xFF425466),
                  fontSize: 15,
                  height: 1.8,
                ),
              ),
              const SizedBox(height: 24),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('收好这封信'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _showBondEditor() async {
    if (_config == null) return;
    String yourLabel = _bondProfile.yourLabel;
    String selectedYourCity = _cityPresets.firstWhere(
      (city) =>
          city['lat'] == _bondProfile.yourLat && city['lng'] == _bondProfile.yourLng,
      orElse: () => _cityPresets.first,
    )['label'] as String;
    String herTag = _bondProfile.herTag ?? _config!.places.first.tag;
    String herLabel = _bondProfile.herLabel;

    final result = await showDialog<EternalBondProfile>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              backgroundColor: const Color(0xFF111827),
              title: const Text(
                '双地点连线设置',
                style: TextStyle(color: Colors.white),
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    controller: TextEditingController(text: yourLabel),
                    onChanged: (value) => yourLabel = value,
                    style: const TextStyle(color: Colors.white),
                    decoration: _darkInputDecoration('给你的锚点起个名字'),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    value: selectedYourCity,
                    dropdownColor: const Color(0xFF182235),
                    decoration: _darkInputDecoration('你所在的城市'),
                    items: _cityPresets
                        .map(
                          (city) => DropdownMenuItem(
                            value: city['label'] as String,
                            child: Text(
                              city['label'] as String,
                              style: const TextStyle(color: Colors.white),
                            ),
                          ),
                        )
                        .toList(),
                    onChanged: (value) {
                      if (value != null) {
                        setDialogState(() => selectedYourCity = value);
                      }
                    },
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: TextEditingController(text: herLabel),
                    onChanged: (value) => herLabel = value,
                    style: const TextStyle(color: Colors.white),
                    decoration: _darkInputDecoration('她在云南的称呼'),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    value: herTag,
                    dropdownColor: const Color(0xFF182235),
                    decoration: _darkInputDecoration('她在云南的锚点'),
                    items: _config!.places
                        .map(
                          (place) => DropdownMenuItem(
                            value: place.tag,
                            child: Text(
                              place.title,
                              style: const TextStyle(color: Colors.white),
                            ),
                          ),
                        )
                        .toList(),
                    onChanged: (value) {
                      if (value != null) {
                        setDialogState(() => herTag = value);
                      }
                    },
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('取消'),
                ),
                FilledButton(
                  onPressed: () {
                    final yourPreset = _cityPresets.firstWhere(
                      (city) => city['label'] == selectedYourCity,
                      orElse: () => _cityPresets.first,
                    );
                    final herPlace = _config!.placeByTag(herTag) ?? _config!.places.first;
                    Navigator.pop(
                      context,
                      EternalBondProfile(
                        yourLabel: yourLabel.trim().isEmpty ? '远方的你' : yourLabel.trim(),
                        yourLat: (yourPreset['lat'] as num?)?.toDouble(),
                        yourLng: (yourPreset['lng'] as num?)?.toDouble(),
                        herLabel: herLabel.trim().isEmpty ? '云南的她' : herLabel.trim(),
                        herTag: herPlace.tag,
                        herLat: herPlace.lat,
                        herLng: herPlace.lng,
                      ),
                    );
                  },
                  child: const Text('保存'),
                ),
              ],
            );
          },
        );
      },
    );

    if (result != null && mounted) {
      setState(() => _bondProfile = result);
      await _saveBondProfile();
    }
  }

  Future<void> _unlockInnerGate(String answer) async {
    final config = _config;
    if (config == null) return;
    final expected =
        (config.secretQuestion['answer'] as String? ?? '').trim().toLowerCase();
    final normalized = answer.trim().toLowerCase();
    if (normalized == expected && normalized.isNotEmpty) {
      await _realmDataChannel.invokeMethod('setEternalInnerGateUnlocked', true);
      if (mounted) {
        setState(() => _innerGateUnlocked = true);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content:
                Text(config.secretQuestion['successMessage'] as String? ?? '已经为你打开。'),
          ),
        );
      }
    } else if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('答案还不对，再想一想你们之间最柔软的那阵风。')),
      );
    }
  }

  Map<String, dynamic>? _nextAnniversary(EternalRealmConfig config) {
    if (config.anniversaries.isEmpty) return null;
    final now = DateTime.now();
    Map<String, dynamic>? best;
    Duration? bestGap;
    for (final item in config.anniversaries) {
      final month = item['month'] as int? ?? now.month;
      final day = item['day'] as int? ?? now.day;
      var target = DateTime(now.year, month, day);
      if (target.isBefore(DateTime(now.year, now.month, now.day))) {
        target = DateTime(now.year + 1, month, day);
      }
      final gap = target.difference(DateTime(now.year, now.month, now.day));
      if (bestGap == null || gap < bestGap) {
        bestGap = gap;
        best = {...item, 'targetDate': target, 'daysLeft': gap.inDays};
      }
    }
    return best;
  }

  int _lettersUnlockedCount() => _letters.where((item) => item.isUnlocked).length;

  int _lettersUnreadCount() =>
      _letters.where((item) => item.isUnlocked && !item.isOpened).length;

  List<EternalMemory> _memoriesForPlace(EternalRealmConfig config, String tag) {
    return config.memories.where((item) => item.placeTag == tag).toList();
  }

  String _letterUnlockHint(EternalFutureLetter letter, EternalRealmConfig config) {
    if (letter.isUnlocked) {
      return letter.isOpened ? '这封信已经被读过了。' : '已经可以打开了。';
    }
    if (letter.unlockType == 'date' && letter.unlockAt != null) {
      final target = DateTime.fromMillisecondsSinceEpoch(letter.unlockAt!);
      final today = DateTime.now();
      final start = DateTime(today.year, today.month, today.day);
      final targetDay = DateTime(target.year, target.month, target.day);
      final diff = targetDay.difference(start).inDays;
      if (diff <= 0) {
        return '今天就可以打开。';
      }
      return '距离解锁还有 $diff 天';
    }
    final place = config.placeByTag(letter.placeTag ?? '');
    return '等你们走到 ${place?.title ?? '指定地点'}';
  }

  Widget _overviewTile(String label, String value, IconData icon) {
    return Container(
      width: 104,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.05),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: Colors.cyanAccent.withValues(alpha: 0.86), size: 18),
          const SizedBox(height: 10),
          Text(
            value,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.6),
              fontSize: 11,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildIntro(EternalRealmConfig config) {
    final todayBanner = getEternalTodayBannerText();
    final nextAnniversary = _nextAnniversary(config);
    final unreadLetters = _lettersUnreadCount();
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 20),
        child: Column(
          children: [
            const Icon(Icons.favorite, color: Colors.pinkAccent, size: 84),
            const SizedBox(height: 32),
            Text(
              config.realmTitle,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 30,
                fontWeight: FontWeight.w900,
                letterSpacing: 4,
              ),
            ),
            const SizedBox(height: 14),
            Container(
              height: 2,
              width: 60,
              color: Colors.pinkAccent.withValues(alpha: 0.6),
            ),
            const SizedBox(height: 28),
            Text(
              config.introQuote,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 18,
                fontStyle: FontStyle.italic,
                height: 1.55,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              config.introSubquote,
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.58),
                fontSize: 12,
                height: 1.7,
                letterSpacing: 1,
              ),
            ),
            if (todayBanner != null) ...[
              const SizedBox(height: 24),
              _glassCard(
                child: Row(
                  children: [
                    const Icon(Icons.auto_awesome, color: Colors.amberAccent),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        todayBanner,
                        style: const TextStyle(color: Colors.white, height: 1.5),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 40),
            _glassCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '今日总览',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 14),
                  Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    alignment: WrapAlignment.center,
                    children: [
                      _overviewTile('云南地点', '${config.places.length}', Icons.map_outlined),
                      _overviewTile('记忆片段', '${config.memories.length}', Icons.history_edu_outlined),
                      _overviewTile('未来信', '${_letters.length}', Icons.mail_outline),
                      _overviewTile('待开启', '$unreadLetters', Icons.mark_email_unread_outlined),
                    ],
                  ),
                ],
              ),
            ),
            if (nextAnniversary != null) ...[
              const SizedBox(height: 14),
              _glassCard(
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.event_available, color: Colors.cyanAccent),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            nextAnniversary['title'] as String? ?? '下一次纪念日',
                            style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            '${_formatDate(nextAnniversary['targetDate'] as DateTime)} · 还有 ${nextAnniversary['daysLeft']} 天',
                            style: TextStyle(
                              color: Colors.amberAccent.withValues(alpha: 0.9),
                              fontSize: 12,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            nextAnniversary['message'] as String? ?? '',
                            style: TextStyle(
                              color: Colors.white.withValues(alpha: 0.72),
                              height: 1.55,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 18),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              alignment: WrapAlignment.center,
              children: [
                OutlinedButton(
                  onPressed: () => setState(() {
                    _activated = true;
                    _sectionIndex = 0;
                  }),
                  style: OutlinedButton.styleFrom(
                    side: const BorderSide(color: Colors.pinkAccent),
                    padding:
                        const EdgeInsets.symmetric(horizontal: 28, vertical: 16),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(28)),
                  ),
                  child: const Text(
                    '开启云南心地图',
                    style: TextStyle(
                      color: Colors.pinkAccent,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                FilledButton.tonal(
                  onPressed: () => setState(() {
                    _activated = true;
                    _sectionIndex = 1;
                  }),
                  style: FilledButton.styleFrom(
                    backgroundColor: Colors.white.withValues(alpha: 0.08),
                    foregroundColor: Colors.white,
                    padding:
                        const EdgeInsets.symmetric(horizontal: 28, vertical: 16),
                  ),
                  child: const Text('进入时间博物馆'),
                ),
              ],
            ),
            const SizedBox(height: 44),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              alignment: WrapAlignment.center,
              children: config.places
                  .take(6)
                  .map(
                    (place) => Chip(
                      label: Text(place.title),
                      labelStyle: const TextStyle(color: Colors.white),
                      backgroundColor: place.themeColor.withValues(alpha: 0.22),
                      side: BorderSide(color: place.accentColor.withValues(alpha: 0.3)),
                    ),
                  )
                  .toList(),
            ),
            const SizedBox(height: 32),
            Text(
              'Est. 1999.09.16',
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.22),
                fontSize: 12,
                letterSpacing: 2,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMapSection(EternalRealmConfig config) {
    final selected = _selectedPlace;
    final seasonal = config.seasonalFor(DateTime.now());
    final selectedMemories =
        selected == null ? const <EternalMemory>[] : _memoriesForPlace(config, selected.tag);
    final bondDistance = _bondProfile.isConfigured
        ? _distanceKm(
            _bondProfile.yourLat!,
            _bondProfile.yourLng!,
            _bondProfile.herLat!,
            _bondProfile.herLng!,
          )
        : null;
    return Stack(
      children: [
        Positioned.fill(
          child: _EternalMapDisplay(onCreated: _onMapCreated),
        ),
        IgnorePointer(
          child: Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Colors.black.withValues(alpha: 0.38),
                  Colors.transparent,
                  Colors.black.withValues(alpha: 0.48),
                ],
              ),
            ),
          ),
        ),
        Positioned(
          top: 18,
          left: 16,
          right: 16,
          child: Column(
            children: [
              if (selected != null)
                _glassCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Container(
                            width: 12,
                            height: 12,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: selected.accentColor,
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  selected.title,
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  '${selected.subtitle} · ${selected.ambient}',
                                  style: TextStyle(
                                    color: Colors.white.withValues(alpha: 0.72),
                                    fontSize: 12,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          IconButton(
                            onPressed: () => setState(() => _selectedPlace = null),
                            icon: const Icon(Icons.close, color: Colors.white70),
                          ),
                        ],
                      ),
                      if (selectedMemories.isNotEmpty) ...[
                        const SizedBox(height: 12),
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: selectedMemories
                              .take(2)
                              .map(
                                (memory) => GestureDetector(
                                  onTap: () => setState(() => _sectionIndex = 1),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 10,
                                      vertical: 8,
                                    ),
                                    decoration: BoxDecoration(
                                      color: Colors.white.withValues(alpha: 0.06),
                                      borderRadius: BorderRadius.circular(14),
                                      border: Border.all(
                                        color: Colors.white.withValues(alpha: 0.06),
                                      ),
                                    ),
                                    child: Text(
                                      memory.title,
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontSize: 11,
                                      ),
                                    ),
                                  ),
                                ),
                              )
                              .toList(),
                        ),
                      ],
                    ],
                  ),
                )
              else
                _glassCard(
                  child: Row(
                    children: [
                      const Icon(Icons.auto_awesome, color: Colors.cyanAccent),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          seasonal.message,
                          style: const TextStyle(color: Colors.white, height: 1.45),
                        ),
                      ),
                    ],
                  ),
                ),
              const SizedBox(height: 10),
              if (bondDistance != null)
                Align(
                  alignment: Alignment.centerLeft,
                  child: Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                    decoration: BoxDecoration(
                      color: Colors.black.withValues(alpha: 0.42),
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                    ),
                    child: Text(
                      '${_bondProfile.yourLabel} ↔ ${_bondProfile.herLabel} · ${bondDistance.toStringAsFixed(0)} km',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
        Positioned(
          right: 20,
          bottom: 160,
          child: Column(
            children: [
              GestureDetector(
                onTap: () => setState(() => _sectionIndex = 3),
                child: const SizedBox(
                  width: 82,
                  height: 82,
                  child: _TimeCapsuleGlow(isUnlocked: true),
                ),
              ),
              const SizedBox(height: 12),
              GestureDetector(
                onTap: _showBondEditor,
                child: Container(
                  width: 52,
                  height: 52,
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.48),
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                  ),
                  child: const Icon(Icons.favorite_border, color: Colors.white),
                ),
              ),
            ],
          ),
        ),
        Positioned(
          left: 16,
          right: 16,
          bottom: 28,
          child: SizedBox(
            height: 122,
            child: Column(
              children: [
                Expanded(
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: config.places.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 10),
                    itemBuilder: (context, index) {
                      final place = config.places[index];
                      final isSelected = selected?.tag == place.tag;
                      return GestureDetector(
                        onTap: () => _focusPlace(place),
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 220),
                          width: 144,
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(24),
                            color: isSelected
                                ? place.themeColor.withValues(alpha: 0.35)
                                : Colors.black.withValues(alpha: 0.46),
                            border: Border.all(
                              color: isSelected
                                  ? place.accentColor.withValues(alpha: 0.55)
                                  : Colors.white.withValues(alpha: 0.08),
                            ),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisAlignment: MainAxisAlignment.end,
                            children: [
                              Text(
                                place.title,
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                place.poem,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.78),
                                  fontSize: 11,
                                  height: 1.4,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
                const SizedBox(height: 10),
                _SpatiotemporalPlaybackCompass(
                  onChanged: (value) {
                    final index =
                        ((config.places.length - 1) * value).round().clamp(0, config.places.length - 1);
                    _focusPlace(config.places[index], openOverlay: false);
                  },
                ),
              ],
            ),
          ),
        ),
        if (selected != null)
          Positioned.fill(
            child: GestureDetector(
              onTap: () => setState(() => _selectedPlace = null),
              child: Container(
                color: Colors.black45,
                alignment: Alignment.center,
                child: AstrolabeLetter(place: selected),
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildMemoriesSection(EternalRealmConfig config) {
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: config.memories.length,
      separatorBuilder: (_, __) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final memory = config.memories[index];
        final place = config.placeByTag(memory.placeTag);
        final theme = place?.themeColor ?? const Color(0xFF6B7280);
        final accent = place?.accentColor ?? Colors.white;
        return InkWell(
          onTap: () {
            showDialog<void>(
              context: context,
              builder: (context) => Dialog(
                backgroundColor: Colors.transparent,
                child: Container(
                  padding: const EdgeInsets.all(24),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(28),
                    gradient: LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: [
                        theme.withValues(alpha: 0.95),
                        const Color(0xFF111827),
                      ],
                    ),
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        memory.title,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 22,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 10),
                      Text(
                        '${memory.date} · ${place?.title ?? '云南'}',
                        style: TextStyle(
                          color: accent.withValues(alpha: 0.92),
                          fontSize: 12,
                          letterSpacing: 1.1,
                        ),
                      ),
                      const SizedBox(height: 18),
                      Text(
                        memory.message,
                        style: const TextStyle(
                          color: Colors.white,
                          height: 1.7,
                          fontSize: 14,
                        ),
                      ),
                      const SizedBox(height: 24),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          TextButton(
                            onPressed: () => Navigator.pop(context),
                            child: const Text('先收好'),
                          ),
                          const SizedBox(width: 8),
                          FilledButton.tonal(
                            onPressed: () {
                              Navigator.pop(context);
                              if (place != null) {
                                _focusPlace(place);
                              }
                            },
                            child: const Text('在心地图里看'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            );
          },
          child: Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: const Color(0xFF101828),
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
              boxShadow: [
                BoxShadow(
                  color: theme.withValues(alpha: 0.12),
                  blurRadius: 22,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: Row(
              children: [
                Container(
                  width: 58,
                  height: 58,
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [accent, theme.withValues(alpha: 0.9)],
                    ),
                    borderRadius: BorderRadius.circular(18),
                  ),
                  alignment: Alignment.center,
                  child: Text(
                    memory.year,
                    style: const TextStyle(
                      color: Colors.black,
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        memory.title,
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          fontSize: 15,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        memory.summary,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.66),
                          height: 1.45,
                        ),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right, color: Colors.white24),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildArchiveSection(EternalRealmConfig config) {
    final seasonal = config.seasonalFor(DateTime.now());
    final totalDistance = _yunnanEntries.fold<double>(
      0.0,
      (sum, item) => sum + ((item['distanceKm'] as num?)?.toDouble() ?? 0.0),
    );
    final uniquePlaces = _yunnanEntries
        .map((entry) => (entry['location'] ?? '').toString().split(' ').first)
        .where((item) => item.isNotEmpty)
        .toSet()
        .length;
    final bondDistance = _bondProfile.isConfigured
        ? _distanceKm(
            _bondProfile.yourLat!,
            _bondProfile.yourLng!,
            _bondProfile.herLat!,
            _bondProfile.herLng!,
          )
        : null;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _glassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                seasonal.title,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                seasonal.message,
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.72),
                  height: 1.6,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),
        Wrap(
          spacing: 12,
          runSpacing: 12,
          children: [
            _statTile('云南足迹', '${_yunnanEntries.length}', Icons.auto_stories_outlined),
            _statTile(
                '里程总和', '${totalDistance.toStringAsFixed(1)} km', Icons.route_outlined),
            _statTile('探索地点', '$uniquePlaces', Icons.location_city_outlined),
            _statTile('记忆胶囊', '${_yunnanCapsules.length}', Icons.inventory_2_outlined),
          ],
        ),
        const SizedBox(height: 14),
        _glassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Icon(Icons.favorite, color: Colors.pinkAccent),
                  const SizedBox(width: 10),
                  const Expanded(
                    child: Text(
                      '异地心跳连线',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  TextButton(
                    onPressed: _showBondEditor,
                    child: const Text('设置'),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              if (bondDistance == null)
                Text(
                  '还没有配置两端坐标。设置之后，地图会出现一条只属于你们的发光连线。',
                  style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.68),
                    height: 1.6,
                  ),
                )
              else ...[
                Text(
                  '${_bondProfile.yourLabel} 与 ${_bondProfile.herLabel} 之间，今天相隔 ${bondDistance.toStringAsFixed(0)} 公里。',
                  style: const TextStyle(
                    color: Colors.white,
                    height: 1.55,
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  '但它们已经被同一条星图连在一起。',
                  style: TextStyle(
                    color: Colors.amberAccent.withValues(alpha: 0.86),
                    height: 1.5,
                  ),
                ),
                const SizedBox(height: 12),
                FilledButton.tonal(
                  onPressed: () => setState(() => _sectionIndex = 0),
                  child: const Text('去心地图查看连线'),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 14),
        _glassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                '只写给她的四句承诺',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 12),
              ...config.privatePromises.map(
                (promise) => Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Padding(
                        padding: EdgeInsets.only(top: 4),
                        child: Icon(Icons.circle, size: 8, color: Colors.amberAccent),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          promise,
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.75),
                            height: 1.55,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildLettersSection(EternalRealmConfig config) {
    final sortedLetters = [..._letters]
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
    final unlockedCount = _lettersUnlockedCount();
    final unreadCount = _lettersUnreadCount();
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _glassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                '未来信箱',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '可以把一封信埋给未来的她，也可以把它交给某个云南地点保管。',
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.7),
                  height: 1.6,
                ),
              ),
              const SizedBox(height: 16),
              Wrap(
                spacing: 10,
                runSpacing: 10,
                children: [
                  FilledButton(
                    onPressed: _showCreateLetterSheet,
                    child: const Text('写一封新信'),
                  ),
                  ...config.futureTemplates.map(
                    (template) => ActionChip(
                      label: Text(template.title),
                      onPressed: () => _showCreateLetterSheet(template: template),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Wrap(
                spacing: 10,
                runSpacing: 10,
                children: [
                  _overviewTile('已解锁', '$unlockedCount', Icons.lock_open_outlined),
                  _overviewTile('待阅读', '$unreadCount', Icons.drafts_outlined),
                  _overviewTile(
                    '地点解锁',
                    '${_letters.where((item) => item.unlockType == 'place').length}',
                    Icons.place_outlined,
                  ),
                  _overviewTile(
                    '日期解锁',
                    '${_letters.where((item) => item.unlockType == 'date').length}',
                    Icons.event_outlined,
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),
        if (sortedLetters.isEmpty)
          _glassCard(
            child: Text(
              '这里还没有埋下任何信。未来会来，但有些话可以先替未来放在这里。',
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.72),
                height: 1.6,
              ),
            ),
          )
        else
          ...sortedLetters.map((letter) {
            final place = config.placeByTag(letter.placeTag ?? '');
            final unlockLabel = letter.unlockType == 'date'
                ? '到 ${letter.unlockAt == null ? '--' : _formatDate(DateTime.fromMillisecondsSinceEpoch(letter.unlockAt!))} 解锁'
                : '到 ${place?.title ?? '指定地点'} 解锁';
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: InkWell(
                onTap: () => _openLetter(letter),
                child: Container(
                  padding: const EdgeInsets.all(18),
                  decoration: BoxDecoration(
                    color: letter.isUnlocked
                        ? const Color(0xFF142033)
                        : const Color(0xFF0F172A),
                    borderRadius: BorderRadius.circular(22),
                    border: Border.all(
                      color: letter.isUnlocked
                          ? Colors.cyanAccent.withValues(alpha: 0.25)
                          : Colors.white.withValues(alpha: 0.06),
                    ),
                  ),
                  child: Row(
                    children: [
                      Container(
                        width: 46,
                        height: 46,
                        decoration: BoxDecoration(
                          color: letter.isUnlocked
                              ? Colors.cyanAccent.withValues(alpha: 0.16)
                              : Colors.white.withValues(alpha: 0.06),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: Icon(
                          letter.isUnlocked ? Icons.mark_email_read : Icons.mark_email_unread,
                          color: letter.isUnlocked ? Colors.cyanAccent : Colors.white54,
                        ),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              letter.title,
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 15,
                              ),
                            ),
                            const SizedBox(height: 6),
                            Text(
                              unlockLabel,
                              style: TextStyle(
                                color: Colors.white.withValues(alpha: 0.66),
                                fontSize: 12,
                              ),
                            ),
                            const SizedBox(height: 6),
                            Text(
                              _letterUnlockHint(letter, config),
                              style: TextStyle(
                                color: letter.isUnlocked
                                    ? Colors.cyanAccent.withValues(alpha: 0.82)
                                    : Colors.amberAccent.withValues(alpha: 0.82),
                                fontSize: 11,
                              ),
                            ),
                            if (letter.isOpened)
                              Padding(
                                padding: const EdgeInsets.only(top: 6),
                                child: Text(
                                  '这封信她已经看过了。',
                                  style: TextStyle(
                                    color: Colors.amberAccent.withValues(alpha: 0.85),
                                    fontSize: 11,
                                  ),
                                ),
                              ),
                          ],
                        ),
                      ),
                      const Icon(Icons.chevron_right, color: Colors.white24),
                    ],
                  ),
                ),
              ),
            );
          }),
      ],
    );
  }

  Widget _buildPrivateRoom(EternalRealmConfig config) {
    if (!_innerGateUnlocked) {
      final controller = TextEditingController();
      return Padding(
        padding: const EdgeInsets.all(20),
        child: _glassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                '只对她开放',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 10),
              Text(
                config.secretQuestion['question'] as String? ??
                    '输入只有她知道的答案。',
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.72),
                  height: 1.6,
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: controller,
                style: const TextStyle(color: Colors.white),
                decoration: _darkInputDecoration('答案'),
              ),
              const SizedBox(height: 14),
              FilledButton(
                onPressed: () => _unlockInnerGate(controller.text),
                child: const Text('打开这扇门'),
              ),
            ],
          ),
        ),
      );
    }

    final favoritePlace = _selectedPlace ?? config.placeByTag(_bondProfile.herTag ?? '') ?? config.places.first;
    final unreadLetters = _letters.where((item) => item.isUnlocked && !item.isOpened).toList();
    final nextLetter = _letters
        .where((item) => !item.isUnlocked)
        .toList()
      ..sort((a, b) {
        final aTime = a.unlockAt ?? (1 << 62);
        final bTime = b.unlockAt ?? (1 << 62);
        return aTime.compareTo(bTime);
      });

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _glassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                '只对你开放',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 10),
              Text(
                config.secretQuestion['successTitle'] as String? ??
                    '这里收着更私密的句子。',
                style: TextStyle(
                  color: Colors.amberAccent.withValues(alpha: 0.9),
                  fontSize: 13,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                '如果前面的页面像地图和时间，那这个房间更像心脏。它不负责解释，只负责安静地偏向她。',
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.72),
                  height: 1.65,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),
        _glassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                '今日偏爱的云南坐标',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 10),
              Text(
                favoritePlace.title,
                style: TextStyle(
                  color: favoritePlace.accentColor,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                favoritePlace.msg,
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.72),
                  height: 1.6,
                ),
              ),
              const SizedBox(height: 12),
              FilledButton.tonal(
                onPressed: () {
                  _focusPlace(favoritePlace);
                },
                child: const Text('回到这片风景'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),
        _glassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                '未寄出的温柔',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 12),
              if (unreadLetters.isNotEmpty)
                Text(
                  '已经有 ${unreadLetters.length} 封信等着被她看见，最近的一封是《${unreadLetters.first.title}》。',
                  style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.72),
                    height: 1.6,
                  ),
                )
              else if (nextLetter.isNotEmpty)
                Text(
                  '下一封要抵达的信是《${nextLetter.first.title}》，${_letterUnlockHint(nextLetter.first, config)}。',
                  style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.72),
                    height: 1.6,
                  ),
                )
              else
                Text(
                  '这里暂时没有等待中的信。你可以再埋一封，把一句话留给未来的她。',
                  style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.72),
                    height: 1.6,
                  ),
                ),
              const SizedBox(height: 12),
              FilledButton.tonal(
                onPressed: () => setState(() => _sectionIndex = 3),
                child: const Text('去未来信箱看看'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),
        ...config.privatePromises.map(
          (promise) => Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.04),
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
              ),
              child: Text(
                promise,
                style: const TextStyle(
                  color: Colors.white,
                  height: 1.7,
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _statTile(String label, String value, IconData icon) {
    return SizedBox(
      width: 160,
      child: _glassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: Colors.cyanAccent.withValues(alpha: 0.85)),
            const SizedBox(height: 12),
            Text(
              value,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.65),
                fontSize: 12,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _glassCard({required Widget child}) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(24),
      child: BackdropFilter(
        filter: ui.ImageFilter.blur(sigmaX: 14, sigmaY: 14),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: 0.06),
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
          ),
          child: child,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final config = _config;
    if (_loading) {
      return const Scaffold(
        backgroundColor: Colors.black,
        body: Center(child: CircularProgressIndicator()),
      );
    }
    if (config == null) {
      return const Scaffold(
        backgroundColor: Colors.black,
        body: Center(
          child: Text(
            '永恒之境暂时无法开启',
            style: TextStyle(color: Colors.white70),
          ),
        ),
      );
    }

    final sections = [
      '云南心地图',
      '时间博物馆',
      '永恒档案',
      '未来信箱',
      '只对她开放',
    ];

    return Scaffold(
      backgroundColor: Colors.black,
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
              child: Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.arrow_back_ios_new, color: Colors.white70),
                    onPressed: () {
                      if (_selectedPlace != null) {
                        setState(() => _selectedPlace = null);
                      } else if (_activated) {
                        setState(() => _activated = false);
                      } else {
                        Navigator.pop(context);
                      }
                    },
                  ),
                  Expanded(
                    child: Column(
                      children: [
                        Text(
                          config.realmTitle,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          isEternalSpecialDay()
                              ? '今日星图已点亮'
                              : 'YUNNAN MEMORY SPACE',
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.4),
                            fontSize: 10,
                            letterSpacing: 1.8,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    onPressed: () => setState(() => _sectionIndex = 3),
                    icon: const Icon(Icons.mail_outline, color: Colors.white70),
                  ),
                ],
              ),
            ),
            if (!_activated)
              Expanded(child: _buildIntro(config))
            else ...[
              SizedBox(
                height: 44,
                child: ListView.separated(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  scrollDirection: Axis.horizontal,
                  itemBuilder: (context, index) {
                    final selected = _sectionIndex == index;
                    return ChoiceChip(
                      selected: selected,
                      label: Text(sections[index]),
                      labelStyle: TextStyle(
                        color: selected ? Colors.black : Colors.white,
                        fontWeight: selected ? FontWeight.bold : FontWeight.w500,
                      ),
                      selectedColor: Colors.cyanAccent,
                      backgroundColor: Colors.white.withValues(alpha: 0.08),
                      onSelected: (_) => setState(() => _sectionIndex = index),
                    );
                  },
                  separatorBuilder: (_, __) => const SizedBox(width: 8),
                  itemCount: sections.length,
                ),
              ),
              const SizedBox(height: 12),
              Expanded(
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 280),
                  child: switch (_sectionIndex) {
                    0 => _buildMapSection(config),
                    1 => _buildMemoriesSection(config),
                    2 => _buildArchiveSection(config),
                    3 => _buildLettersSection(config),
                    _ => _buildPrivateRoom(config),
                  },
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _EternalMapDisplay extends StatelessWidget {
  final ValueChanged<int> onCreated;

  const _EternalMapDisplay({required this.onCreated});

  @override
  Widget build(BuildContext context) {
    return AndroidView(
      viewType: 'com.footprint/amap',
      creationParams: const {'mode': 'ETERNAL_REALM'},
      creationParamsCodec: const StandardMessageCodec(),
      onPlatformViewCreated: onCreated,
    );
  }
}

class AstrolabeLetter extends StatefulWidget {
  final EternalPlace place;

  const AstrolabeLetter({super.key, required this.place});

  @override
  State<AstrolabeLetter> createState() => _AstrolabeLetterState();
}

class _AstrolabeLetterState extends State<AstrolabeLetter>
    with SingleTickerProviderStateMixin {
  late final AnimationController _animationController;

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    )..forward();
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final place = widget.place;
    return AnimatedBuilder(
      animation: _animationController,
      builder: (context, child) {
        final value = Curves.easeOutCubic.transform(_animationController.value);
        return Transform(
          transform: Matrix4.identity()
            ..setEntry(3, 2, 0.001)
            ..rotateX((1 - value) * 0.32),
          alignment: Alignment.center,
          child: Opacity(
            opacity: value,
            child: Container(
              margin: const EdgeInsets.all(28),
              padding: const EdgeInsets.all(28),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(28),
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    const Color(0xFFF9F5F1),
                    place.accentColor.withValues(alpha: 0.28),
                  ],
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.32),
                    blurRadius: 30,
                    offset: const Offset(0, 16),
                  ),
                ],
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: place.themeColor.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Text(
                      place.ambient,
                      style: TextStyle(
                        color: place.themeColor,
                        fontSize: 11,
                        letterSpacing: 1,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    place.title,
                    style: TextStyle(
                      color: const Color(0xFF24324A),
                      fontSize: 30,
                      fontWeight: FontWeight.bold,
                      fontFamily: place.tag == 'DALI' ? 'Brush Script' : null,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    place.poem,
                    style: TextStyle(
                      color: place.themeColor,
                      fontSize: 14,
                      fontStyle: FontStyle.italic,
                    ),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    place.msg,
                    style: const TextStyle(
                      color: Color(0xFF425466),
                      fontSize: 15,
                      height: 1.8,
                      letterSpacing: 0.4,
                    ),
                  ),
                  const SizedBox(height: 26),
                  TypographyDuet.buildTranslatorBottomView(
                    place.enQuote,
                    place.cnTrans,
                  ),
                  const SizedBox(height: 26),
                  Align(
                    alignment: Alignment.bottomRight,
                    child: Text(
                      place.date,
                      style: const TextStyle(
                        color: Colors.black38,
                        fontSize: 12,
                        letterSpacing: 1.6,
                      ),
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

  const _TimeCapsuleGlow({this.isUnlocked = false});

  @override
  State<_TimeCapsuleGlow> createState() => _TimeCapsuleGlowState();
}

class _TimeCapsuleGlowState extends State<_TimeCapsuleGlow>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  ui.FragmentShader? _shader;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 4),
    )..repeat();
    _loadShader();
  }

  Future<void> _loadShader() async {
    try {
      final program = await ui.FragmentProgram.fromAsset('shaders/time_capsule_core.frag');
      if (mounted) {
        setState(() => _shader = program.fragmentShader());
      }
    } catch (e) {
      debugPrint('Failed to load time capsule shader: $e');
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_shader == null) {
      return Container(
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: widget.isUnlocked
              ? Colors.cyanAccent.withValues(alpha: 0.5)
              : Colors.blueAccent.withValues(alpha: 0.5),
          boxShadow: [
            BoxShadow(
              blurRadius: 12,
              color: widget.isUnlocked ? Colors.cyanAccent : Colors.blueAccent,
            ),
          ],
        ),
      );
    }

    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        _shader!.setFloat(0, 82.0);
        _shader!.setFloat(1, 82.0);
        _shader!.setFloat(2, _controller.value * 4.0);
        _shader!.setFloat(3, widget.isUnlocked ? 1.0 : 0.4);
        return CustomPaint(
          size: const Size(82, 82),
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
  bool shouldRepaint(covariant _ShaderPainter oldDelegate) => true;
}

class _SpatiotemporalPlaybackCompass extends StatefulWidget {
  final ValueChanged<double> onChanged;

  const _SpatiotemporalPlaybackCompass({required this.onChanged});

  @override
  State<_SpatiotemporalPlaybackCompass> createState() =>
      _SpatiotemporalPlaybackCompassState();
}

class _SpatiotemporalPlaybackCompassState
    extends State<_SpatiotemporalPlaybackCompass> {
  double progress = 0.0;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 56,
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.66),
        borderRadius: BorderRadius.circular(32),
        border: Border.all(color: Colors.white10),
      ),
      child: Row(
        children: [
          const SizedBox(width: 16),
          const Icon(Icons.history_toggle_off, color: Color(0xFFE5C07B), size: 16),
          const SizedBox(width: 10),
          const Text(
            '时空穿梭',
            style: TextStyle(
              color: Colors.white54,
              fontSize: 11,
              letterSpacing: 1.6,
            ),
          ),
          Expanded(
            child: SliderTheme(
              data: SliderTheme.of(context).copyWith(
                activeTrackColor: const Color(0xFFE5C07B),
                inactiveTrackColor: Colors.white10,
                thumbColor: Colors.white,
                overlayColor: Colors.white10,
                thumbShape: const RoundSliderThumbShape(
                  enabledThumbRadius: 6,
                  elevation: 4,
                ),
                trackHeight: 1.4,
              ),
              child: Slider(
                value: progress,
                onChanged: (value) {
                  setState(() => progress = value);
                  widget.onChanged(value);
                },
              ),
            ),
          ),
          const SizedBox(width: 8),
        ],
      ),
    );
  }
}

InputDecoration _darkInputDecoration(String label) {
  return InputDecoration(
    labelText: label,
    labelStyle: const TextStyle(color: Colors.white54),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.08)),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: const BorderSide(color: Colors.cyanAccent),
    ),
    filled: true,
    fillColor: Colors.white.withValues(alpha: 0.04),
  );
}
