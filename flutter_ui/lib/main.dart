import 'dart:convert';
import 'dart:io';
import 'dart:ui';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/rendering.dart';
import 'package:image_picker/image_picker.dart';
import 'footprint_detail_page.dart';
import 'goal_planner_page.dart';

Widget buildAvatar(String avatarId, {double radius = 24, Color? bgColor, Color? fgColor}) {
  if (avatarId.contains('/') || avatarId.contains('\\')) {
    return CircleAvatar(
      radius: radius,
      backgroundImage: FileImage(File(avatarId)),
      backgroundColor: bgColor,
    );
  }
  IconData iconData = Icons.face;
  if (avatarId == 'avatar_2') iconData = Icons.account_circle;
  if (avatarId == 'avatar_3') iconData = Icons.smart_toy;
  if (avatarId == 'avatar_4') iconData = Icons.fingerprint;
  return CircleAvatar(
    radius: radius,
    backgroundColor: bgColor,
    child: Icon(iconData, color: fgColor),
  );
}

void main() {
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.dark,
      systemNavigationBarColor: Colors.transparent,
    ),
  );
  runApp(const MyApp());
}

// --- 顶级定义的录入页面 (1:1 复刻) ---
class AddFootprintPage extends StatefulWidget {
  const AddFootprintPage({super.key});
  @override
  State<AddFootprintPage> createState() => _AddFootprintPageState();
}

class _AddFootprintPageState extends State<AddFootprintPage> {
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _locationController = TextEditingController();
  final TextEditingController _detailController = TextEditingController();
  final TextEditingController _distanceController = TextEditingController(
    text: "5.0",
  );
  String selectedIcon = "LocationOn";
  double energyLevel = 6.0;
  String selectedMood = "愉快";
  final List<String> availableIcons = [
    "LocationOn",
    "Restaurant",
    "LocalCafe",
    "Park",
    "Flight",
    "Train",
    "DirectionsBike",
    "ShoppingBag",
    "CameraAlt",
  ];
  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          '记录新的足迹',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          TextField(
            controller: _titleController,
            decoration: InputDecoration(
              labelText: '标题',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          ),
          const SizedBox(height: 24),
          Text("选择图标", style: tt.labelSmall?.copyWith(color: cs.outline)),
          const SizedBox(height: 12),
          SizedBox(
            height: 50,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: availableIcons.length,
              separatorBuilder: (_, __) => const SizedBox(width: 12),
              itemBuilder: (context, index) {
                bool isSelected = selectedIcon == availableIcons[index];
                return GestureDetector(
                  onTap: () =>
                      setState(() => selectedIcon = availableIcons[index]),
                  child: CircleAvatar(
                    radius: 22,
                    backgroundColor: isSelected
                        ? cs.primary
                        : cs.surfaceContainerHighest.withValues(alpha: 0.5),
                    child: Icon(
                      _getIconData(availableIcons[index]),
                      color: isSelected ? Colors.white : cs.onSurfaceVariant,
                      size: 20,
                    ),
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 24),
          Row(
            children: [
              Expanded(
                child: Column(
                  children: [
                    const Icon(Icons.straighten, color: Colors.blue),
                    TextField(
                      controller: _distanceController,
                      textAlign: TextAlign.center,
                      decoration: InputDecoration(
                        labelText: '里程',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      style: const TextStyle(fontSize: 12),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  children: [
                    const Icon(Icons.bolt, color: Colors.orange),
                    Slider(
                      value: energyLevel,
                      min: 1,
                      max: 10,
                      divisions: 9,
                      onChanged: (v) => setState(() => energyLevel = v),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
          TextField(
            controller: _locationController,
            decoration: InputDecoration(
              labelText: '地点',
              prefixIcon: const Icon(Icons.place_outlined),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _detailController,
            maxLines: 4,
            decoration: InputDecoration(
              labelText: '故事',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          ),
          const SizedBox(height: 32),
          FilledButton(
            onPressed: () => Navigator.pop(context),
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(56),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
            child: const Text(
              "记录足迹",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
          ),
        ],
      ),
    );
  }

  IconData _getIconData(String name) {
    switch (name) {
      case "Restaurant":
        return Icons.restaurant;
      case "LocalCafe":
        return Icons.local_cafe;
      case "Park":
        return Icons.park;
      case "Flight":
        return Icons.flight;
      case "Train":
        return Icons.train;
      case "DirectionsBike":
        return Icons.directions_bike;
      case "ShoppingBag":
        return Icons.shopping_bag;
      case "CameraAlt":
        return Icons.camera_alt;
      default:
        return Icons.location_on;
    }
  }
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});
  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String themeModeStr = "SYSTEM";
  String nickname = "探索者";
  String avatarId = "avatar_1";
  String themeStyleStr = "CLASSIC";
  bool hapticEnabled = true;
  @override
  void initState() {
    super.initState();
    _loadAllSettings();
  }

  Future<void> _loadAllSettings() async {
    const channel = MethodChannel('com.footprint/data');
    try {
      final jsonStr = await channel.invokeMethod<String>('getSettings');
      if (jsonStr != null) {
        final data = jsonDecode(jsonStr);
        setState(() {
          nickname = data['nickname'] ?? "探索者";
          avatarId = data['avatarId'] ?? "avatar_1";
          themeModeStr = data['themeMode'] ?? "SYSTEM";
          themeStyleStr = data['themeStyle'] ?? "CLASSIC";
          hapticEnabled = data['hapticEnabled'] ?? true;
        });
      }
    } catch (e) {
      debugPrint("Settings Error: $e");
    }
  }

  ThemeMode _getThemeMode() {
    switch (themeModeStr) {
      case "LIGHT":
        return ThemeMode.light;
      case "DARK":
        return ThemeMode.dark;
      default:
        return ThemeMode.system;
    }
  }

  Color _getSeedColor() {
    switch (themeStyleStr) {
      case "CYBERPUNK":
        return Colors.cyanAccent;
      case "FOREST":
        return Colors.green;
      case "SAHARA":
        return Colors.orange;
      case "AUTO":
        return Colors.deepPurpleAccent;
      default:
        return const Color(0xFF1A73E8);
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      themeMode: _getThemeMode(),
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: _getSeedColor(),
          brightness: Brightness.light,
        ),
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: _getSeedColor(),
          brightness: Brightness.dark,
        ),
      ),
      home: MainContainer(
        nickname: nickname,
        avatarId: avatarId,
        themeMode: themeModeStr,
        themeStyle: themeStyleStr,
        hapticEnabled: hapticEnabled,
        onSettingsChanged: _loadAllSettings,
      ),
    );
  }
}

class MainContainer extends StatefulWidget {
  final String nickname;
  final String avatarId;
  final String themeMode;
  final String themeStyle;
  final bool hapticEnabled;
  final VoidCallback onSettingsChanged;
  const MainContainer({
    super.key,
    required this.nickname,
    required this.avatarId,
    required this.themeMode,
    required this.themeStyle,
    required this.hapticEnabled,
    required this.onSettingsChanged,
  });
  @override
  State<MainContainer> createState() => _MainContainerState();
}

class _MainContainerState extends State<MainContainer>
    with SingleTickerProviderStateMixin {
  int _selectedIndex = 0;
  late AnimationController _navController;
  late Animation<double> _elasticAnimation;
  bool _isHiding = false;

  @override
  void initState() {
    super.initState();
    _navController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    );
    _elasticAnimation = CurvedAnimation(
      parent: _navController,
      curve: Curves.easeOutBack,
      reverseCurve: Curves.easeInBack,
    );
    _navController.value = 1.0;
  }

  void _handleScroll(ScrollNotification notification) {
    if (notification is ScrollUpdateNotification) {
      final delta = notification.scrollDelta;
      if (delta == null || delta == 0) return;
      if (delta > 15.0 &&
          !_isHiding &&
          _navController.status != AnimationStatus.reverse) {
        _isHiding = true;
        _navController.reverse();
      } else if (delta < -15.0 &&
          _isHiding &&
          _navController.status != AnimationStatus.forward) {
        _isHiding = false;
        _navController.forward();
      }
    }
  }

  @override
  void dispose() {
    _navController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      DashboardScreen(nickname: widget.nickname, avatarId: widget.avatarId),
      const ExploreMapScreen(),
      const GoalPlannerPage(),
      SettingsScreen(
        nickname: widget.nickname,
        avatarId: widget.avatarId,
        themeMode: widget.themeMode,
        themeStyle: widget.themeStyle,
        hapticEnabled: widget.hapticEnabled,
        onUpdate: widget.onSettingsChanged,
      ),
    ];

    return Scaffold(
      extendBody: true,
      body: NotificationListener<ScrollNotification>(
        onNotification: (notification) {
          _handleScroll(notification);
          return false;
        },
        child: IndexedStack(index: _selectedIndex, children: pages),
      ),
      bottomNavigationBar: AnimatedBuilder(
        animation: _elasticAnimation,
        builder: (context, child) {
          final rawT = _elasticAnimation.value;
          final clampedT = rawT.clamp(0.0, 1.0);
          final fullWidth = MediaQuery.of(context).size.width - 64;
          const dropSize = 64.0;
          final currentWidth = math.max(
            dropSize,
            dropSize + (fullWidth - dropSize) * rawT,
          );
          final overallScale = clampedT < 0.15 ? (clampedT / 0.15) : 1.0;

          return Align(
            alignment: Alignment.bottomCenter,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 32),
              child: Transform.scale(
                scale: overallScale,
                child: Container(
                  width: currentWidth,
                  height: dropSize,
                  decoration: BoxDecoration(
                    color: Theme.of(
                      context,
                    ).colorScheme.surface.withValues(alpha: 0.95),
                    borderRadius: BorderRadius.circular(dropSize / 2),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.15 * clampedT),
                        blurRadius: 20 * clampedT,
                        offset: Offset(0, 10 * clampedT),
                      ),
                    ],
                    border: Border.all(
                      color: Theme.of(context).colorScheme.outlineVariant
                          .withValues(alpha: 0.4 * clampedT),
                    ),
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(dropSize / 2),
                    child: OverflowBox(
                      minWidth: fullWidth,
                      maxWidth: fullWidth,
                      minHeight: dropSize,
                      maxHeight: dropSize,
                      child: Stack(
                        alignment: Alignment.center,
                        children: List.generate(4, (index) {
                          final icons = [
                            Icons.dashboard,
                            Icons.explore,
                            Icons.flag,
                            Icons.settings,
                          ];
                          final outlines = [
                            Icons.dashboard_outlined,
                            Icons.explore_outlined,
                            Icons.outlined_flag,
                            Icons.settings_outlined,
                          ];
                          final double step = fullWidth / 4;
                          final double currentX =
                              (index - 1.5) * step * clampedT;

                          return Positioned(
                            left: (fullWidth / 2) + currentX - 24,
                            child: Opacity(
                              opacity: (clampedT * 5 - 4).clamp(0.0, 1.0),
                              child: GestureDetector(
                                onTap: () {
                                  setState(() => _selectedIndex = index);
                                  if (widget.hapticEnabled)
                                    HapticFeedback.lightImpact();
                                },
                                child: Container(
                                  width: 48,
                                  height: 48,
                                  decoration: BoxDecoration(
                                    color: _selectedIndex == index
                                        ? Theme.of(
                                            context,
                                          ).colorScheme.primaryContainer
                                        : Colors.transparent,
                                    shape: BoxShape.circle,
                                  ),
                                  child: Icon(
                                    _selectedIndex == index
                                        ? icons[index]
                                        : outlines[index],
                                    color: _selectedIndex == index
                                        ? Theme.of(
                                            context,
                                          ).colorScheme.onPrimaryContainer
                                        : Theme.of(
                                            context,
                                          ).colorScheme.onSurfaceVariant,
                                    size: 24,
                                  ),
                                ),
                              ),
                            ),
                          );
                        }),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

// --- 状态页 ---
class DashboardScreen extends StatefulWidget {
  final String nickname;
  final String avatarId;
  const DashboardScreen({
    super.key,
    required this.nickname,
    required this.avatarId,
  });
  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  int currentYear = DateTime.now().year;
  IconData _getAvatarIcon(String id) {
    switch (id) {
      case "avatar_2":
        return Icons.account_circle;
      case "avatar_3":
        return Icons.smart_toy;
      case "avatar_4":
        return Icons.fingerprint;
      default:
        return Icons.face;
    }
  }

  void _showDetail(String t, String l, String d) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const FootprintDetailPage()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    return Scaffold(
      body: Stack(
        children: [
          ListView(
            padding: const EdgeInsets.only(
              top: 190,
              bottom: 150,
              left: 16,
              right: 16,
            ),
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  IconButton(
                    icon: const Icon(Icons.chevron_left),
                    onPressed: () {
                      setState(() => currentYear--);
                    },
                  ),
                  Text(
                    '$currentYear',
                    style: tt.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                  ),
                  IconButton(
                    icon: const Icon(Icons.chevron_right),
                    onPressed: () {
                      setState(() => currentYear++);
                    },
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                "年度数据总览",
                style: TextStyle(
                  color: cs.primary,
                  fontWeight: FontWeight.bold,
                  fontSize: 13,
                ),
              ),
              const SizedBox(height: 8),
              Column(
                children: [
                  Row(
                    children: [
                      _sBox(cs, tt, "足迹", "42"),
                      const SizedBox(width: 8),
                      _sBox(cs, tt, "里程", "128.5", u: "km"),
                      const SizedBox(width: 8),
                      _sBox(cs, tt, "地点", "12"),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      _sBox(cs, tt, "记录", "28"),
                      const SizedBox(width: 8),
                      _sBox(cs, tt, "活力", "8.5", u: "指数"),
                      const SizedBox(width: 8),
                      _sBox(cs, tt, "主情绪", "愉快"),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Container(
                height: 120,
                width: double.infinity,
                decoration: BoxDecoration(
                  color: cs.surfaceContainerHighest.withValues(alpha: 0.3),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Center(
                  child: Text("情绪热力图", style: TextStyle(color: cs.outline)),
                ),
              ),
              const SizedBox(height: 24),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(24),
                  side: BorderSide(color: cs.outlineVariant),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(Icons.auto_awesome, color: cs.primary, size: 18),
                          const SizedBox(width: 8),
                          Text(
                            "那年今日 / 时光碎片",
                            style: TextStyle(
                              color: cs.primary,
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      InkWell(
                        onTap: () => _showDetail("川西穿越", "四姑娘山", "2年前"),
                        child: Row(
                          children: [
                            Container(
                              width: 56,
                              height: 56,
                              decoration: BoxDecoration(
                                color: Colors.orange.withValues(alpha: 0.2),
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: const Icon(
                                Icons.landscape,
                                color: Colors.orange,
                                size: 32,
                              ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const Text(
                                    '川西彩林穿越',
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 16,
                                    ),
                                  ),
                                  Text(
                                    '记录于 2022-10-24 · 愉快',
                                    style: TextStyle(
                                      color: cs.outline,
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(24),
                  side: BorderSide(color: cs.outlineVariant),
                ),
                child: ListTile(
                  leading: Icon(Icons.history, color: cs.primary),
                  title: const Text(
                    '时光足迹回放',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  subtitle: const Text(
                    '查看历史移动轨迹与时空分布',
                    style: TextStyle(fontSize: 12),
                  ),
                  trailing: const Icon(Icons.keyboard_arrow_right),
                ),
              ),
              const SizedBox(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: cs.outlineVariant),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.route, color: cs.primary, size: 16),
                        const SizedBox(width: 6),
                        Text(
                          "年度足迹轨迹",
                          style: TextStyle(
                            color: cs.primary,
                            fontWeight: FontWeight.bold,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: cs.outlineVariant),
                    ),
                    child: Text(
                      "新建 +",
                      style: TextStyle(
                        color: cs.primary,
                        fontWeight: FontWeight.bold,
                        fontSize: 12,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Text(
                    "11月",
                    style: TextStyle(
                      color: cs.primary,
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(child: Divider(color: cs.outlineVariant)),
                  const SizedBox(width: 8),
                  Icon(Icons.expand_less, color: cs.primary),
                ],
              ),
              const SizedBox(height: 12),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                  side: BorderSide(
                    color: cs.outlineVariant.withValues(alpha: 0.5),
                  ),
                ),
                child: ListTile(
                  leading: CircleAvatar(
                    backgroundColor: cs.primaryContainer,
                    child: Icon(Icons.place, color: cs.onPrimaryContainer),
                  ),
                  title: const Text(
                    '周末城市漫步',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  subtitle: const Text(
                    '11-12 · 城市公园',
                    style: TextStyle(fontSize: 12),
                  ),
                  trailing: Text(
                    '5.2 km',
                    style: TextStyle(
                      color: cs.primary,
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                    ),
                  ),
                ),
              ),
            ],
          ),
          _fixedTop(cs, tt),
        ],
      ),
      floatingActionButton: Padding(
        padding: const EdgeInsets.only(bottom: 110),
        child: FloatingActionButton(
          onPressed: () => Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => const AddFootprintPage()),
          ),
          backgroundColor: cs.primary,
          child: const Icon(Icons.add, color: Colors.white, size: 32),
        ),
      ),
    );
  }

  Widget _sBox(ColorScheme cs, TextTheme tt, String l, String v, {String? u}) =>
      Expanded(
        child: Card(
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
            side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.5)),
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.baseline,
                  textBaseline: TextBaseline.alphabetic,
                  children: [
                    Text(
                      v,
                      style: tt.titleLarge?.copyWith(
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    if (u != null)
                      Padding(
                        padding: const EdgeInsets.only(left: 2),
                        child: Text(u, style: tt.labelSmall),
                      ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(l, style: tt.labelSmall?.copyWith(color: cs.outline)),
              ],
            ),
          ),
        ),
      );

  Widget _fixedTop(ColorScheme cs, TextTheme tt) => Positioned(
    top: 0,
    left: 0,
    right: 0,
    child: Container(
      color: cs.surface.withValues(alpha: 0.95),
      padding: const EdgeInsets.only(top: 48, bottom: 12, left: 16, right: 16),
      child: Column(
        children: [
          Row(
            children: [
              buildAvatar(widget.avatarId, radius: 24, bgColor: cs.primaryContainer, fgColor: cs.onPrimaryContainer),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '早安, ${widget.nickname}',
                      style: tt.titleLarge?.copyWith(
                        fontWeight: FontWeight.w900,
                        color: cs.primary,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    Row(
                      children: [
                        Icon(Icons.explore, size: 12, color: cs.secondary),
                        const SizedBox(width: 4),
                        Text(
                          '进阶探索者',
                          style: TextStyle(
                            fontSize: 12,
                            color: cs.secondary,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              IconButton(
                icon: Icon(Icons.more_vert, color: cs.onSurface),
                onPressed: () {},
              ),
            ],
          ),
          const SizedBox(height: 12),
          TextField(
            decoration: InputDecoration(
              hintText: '搜索地点、标签...',
              prefixIcon: const Icon(Icons.search),
              filled: true,
              fillColor: cs.surfaceContainerHighest.withValues(alpha: 0.5),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide.none,
              ),
              contentPadding: const EdgeInsets.symmetric(vertical: 8),
            ),
          ),
        ],
      ),
    ),
  );
}

// --- 探索地图页 (ExploreMapScreen) - 1:1 复刻原生功能 ---
class ExploreMapScreen extends StatefulWidget {
  const ExploreMapScreen({super.key});
  @override
  State<ExploreMapScreen> createState() => _ExploreMapScreenState();
}

class _ExploreMapScreenState extends State<ExploreMapScreen> {
  static const dataChannel = MethodChannel('com.footprint/data');
  static const streamChannel = EventChannel('com.footprint/stream');
  String mapMode = 'STANDARD';
  bool isTracking = false;
  MethodChannel? _mapChannel;

  @override
  void initState() {
    super.initState();
    streamChannel.receiveBroadcastStream().listen((eventJson) {
      final event = jsonDecode(eventJson);
      if (mounted && event['type'] == 'status')
        setState(() => isTracking = event['isTracking']);
    });
  }

  void _onMapCreated(int id) {
    _mapChannel = MethodChannel('com.footprint/amap_$id');
    _updateNativeMap();
  }

  void _updateNativeMap() {
    _mapChannel?.invokeMethod('setFogEnabled', mapMode == 'FOG');
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Stack(
      children: [
        Positioned.fill(child: NativeMapView(onCreated: _onMapCreated)),
        // 模式选择器 (Top Center)
        Positioned(
          top: 56,
          left: 0,
          right: 0,
          child: Center(
            child: Card(
              elevation: 4,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(32),
              ),
              color: cs.surface.withValues(alpha: 0.9),
              child: Padding(
                padding: const EdgeInsets.all(4),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    "标准",
                    "迷雾",
                    "热力",
                    "胶囊",
                  ].map((m) => _buildModeBtn(m, cs)).toList(),
                ),
              ),
            ),
          ),
        ),
        // 右上角设置
        Positioned(
          top: 56,
          right: 16,
          child: FloatingActionButton.small(
            onPressed: () {},
            backgroundColor: cs.surface,
            child: const Icon(Icons.settings_outlined),
          ),
        ),
        // 右下角 FAB 组
        Positioned(
          bottom: 110,
          right: 16,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              FloatingActionButton.small(
                onPressed: () {},
                backgroundColor: cs.surfaceContainerHighest,
                child: const Icon(Icons.my_location),
              ),
              const SizedBox(height: 12),
              FloatingActionButton(
                onPressed: () => dataChannel.invokeMethod(
                  isTracking ? 'stopTracking' : 'startTracking',
                ),
                backgroundColor: isTracking ? Colors.red : cs.primary,
                child: Icon(
                  isTracking ? Icons.stop : Icons.play_arrow,
                  color: Colors.white,
                  size: 32,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildModeBtn(String m, ColorScheme cs) {
    final Map<String, String> modeMap = {
      "标准": "STANDARD",
      "迷雾": "FOG",
      "热力": "HEATMAP",
      "胶囊": "CAPSULE",
    };
    bool sel = mapMode == modeMap[m];
    return GestureDetector(
      onTap: () {
        setState(() => mapMode = modeMap[m]!);
        _updateNativeMap();
      },
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: sel ? cs.primary : Colors.transparent,
          borderRadius: BorderRadius.circular(24),
        ),
        child: Text(
          m,
          style: TextStyle(
            color: sel ? cs.onPrimary : cs.onSurface,
            fontWeight: sel ? FontWeight.bold : null,
            fontSize: 13,
          ),
        ),
      ),
    );
  }
}

// --- 设置页 (SettingsScreen) - 1:1 模块化复刻 ---
class SettingsScreen extends StatefulWidget {
  final String nickname;
  final String avatarId;
  final String themeMode;
  final String themeStyle;
  final bool hapticEnabled;
  final VoidCallback onUpdate;
  const SettingsScreen({
    super.key,
    required this.nickname,
    required this.avatarId,
    required this.themeMode,
    required this.themeStyle,
    required this.hapticEnabled,
    required this.onUpdate,
  });
  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  static const channel = MethodChannel('com.footprint/data');
  void _up(String m, dynamic v) async {
    await channel.invokeMethod(m, v);
    widget.onUpdate();
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('设置', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _t("数字身份", cs),
          _card(
            cs,
            Column(
              children: [
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: TextField(
                    controller: TextEditingController(text: widget.nickname),
                    decoration: const InputDecoration(
                      labelText: '代号 (Nickname)',
                      border: OutlineInputBorder(),
                    ),
                    onSubmitted: (v) => _up('updateNickname', v),
                  ),
                ),
                const Text(
                  "头像选择",
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: ["avatar_1", "avatar_2", "avatar_3", "avatar_4"]
                      .map(
                        (id) => GestureDetector(
                          onTap: () => _up('updateAvatar', id),
                          child: CircleAvatar(
                            radius: 26,
                            backgroundColor: widget.avatarId == id
                                ? cs.primary
                                : cs.surfaceContainerHighest,
                            child: Icon(
                              _getAv(id),
                              color: widget.avatarId == id
                                  ? Colors.white
                                  : cs.onSurfaceVariant,
                            ),
                          ),
                        ),
                      )
                      .toList(),
                ),
                const SizedBox(height: 16),
                if (widget.avatarId.contains('/') || widget.avatarId.contains('\\'))
                  Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: buildAvatar(widget.avatarId, radius: 40),
                  ),
                OutlinedButton.icon(
                  onPressed: () async {
                    final picker = ImagePicker();
                    final file = await picker.pickImage(source: ImageSource.gallery);
                    if (file != null) {
                      _up('updateAvatar', file.path);
                    }
                  },
                  icon: const Icon(Icons.upload),
                  label: const Text("上传自定义图片作为头像"),
                ),
                const SizedBox(height: 16),
              ],
            ),
          ),
          const SizedBox(height: 20),
          _t("系统模式", cs),
          _card(
            cs,
            Column(
              children: [
                RadioListTile(
                  title: const Text("跟随系统"),
                  value: "SYSTEM",
                  groupValue: widget.themeMode,
                  onChanged: (v) => _up('updateThemeMode', v),
                ),
                RadioListTile(
                  title: const Text("日间模式"),
                  value: "LIGHT",
                  groupValue: widget.themeMode,
                  onChanged: (v) => _up('updateThemeMode', v),
                ),
                RadioListTile(
                  title: const Text("夜间模式"),
                  value: "DARK",
                  groupValue: widget.themeMode,
                  onChanged: (v) => _up('updateThemeMode', v),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          _t("视觉风格", cs),
          _card(
            cs,
            Column(
              children: [
                _sTile("智能自适应", Icons.auto_awesome, "AUTO", cs),
                _sTile("经典蓝调", Icons.palette, "CLASSIC", cs),
                _sTile("赛博朋克", Icons.electric_bolt, "CYBERPUNK", cs),
                _sTile("森林氧吧", Icons.forest, "FOREST", cs),
                _sTile("撒哈拉之光", Icons.wb_sunny, "SAHARA", cs),
              ],
            ),
          ),
          const SizedBox(height: 20),
          _t("交互体验", cs),
          _card(
            cs,
            SwitchListTile(
              secondary: const Icon(Icons.vibration),
              title: const Text("开启触感反馈"),
              value: widget.hapticEnabled,
              onChanged: (v) => _up('updateHaptic', v),
            ),
          ),
          const SizedBox(height: 20),
          _t("数据管理", cs),
          _card(
            cs,
            Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.cloud_upload),
                  title: const Text("导出足迹备份"),
                  onTap: () => _up('exportData', null),
                ),
                ListTile(
                  leading: const Icon(Icons.cloud_download),
                  title: const Text("导入历史记录"),
                  onTap: () => _up('importData', null),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          _t("关于", cs),
          _card(
            cs,
            ListTile(
              leading: const Icon(Icons.info_outline),
              title: const Text("软件版本"),
              trailing: const Text("v2.11.1"),
            ),
          ),
          const SizedBox(height: 100),
        ],
      ),
    );
  }

  Widget _t(String t, ColorScheme cs) => Padding(
    padding: const EdgeInsets.only(left: 4, bottom: 8),
    child: Text(
      t,
      style: TextStyle(
        color: cs.primary,
        fontWeight: FontWeight.bold,
        fontSize: 13,
      ),
    ),
  );
  Widget _card(ColorScheme cs, Widget child) => Card(
    elevation: 0,
    shape: RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(24),
      side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.5)),
    ),
    child: child,
  );
  Widget _sTile(String t, IconData i, String v, ColorScheme cs) => ListTile(
    leading: Icon(i, color: widget.themeStyle == v ? cs.primary : cs.outline),
    title: Text(
      t,
      style: TextStyle(
        color: widget.themeStyle == v ? cs.primary : null,
        fontWeight: widget.themeStyle == v ? FontWeight.bold : null,
      ),
    ),
    trailing: widget.themeStyle == v
        ? Icon(Icons.check, color: cs.primary)
        : null,
    onTap: () => _up('updateThemeStyle', v),
  );
  IconData _getAv(String id) {
    switch (id) {
      case "avatar_2":
        return Icons.account_circle;
      case "avatar_3":
        return Icons.smart_toy;
      case "avatar_4":
        return Icons.fingerprint;
      default:
        return Icons.face;
    }
  }
}

class ArtStudioScreen extends StatelessWidget {
  const ArtStudioScreen({super.key});
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('生成艺术')),
      body: const Center(child: Text("工坊内容")),
    );
  }
}

class NativeMapView extends StatelessWidget {
  final Function(int) onCreated;
  const NativeMapView({super.key, required this.onCreated});
  @override
  Widget build(BuildContext context) {
    return AndroidView(
      viewType: 'com.footprint/amap',
      layoutDirection: TextDirection.ltr,
      creationParams: {"zoom": 15.0},
      onPlatformViewCreated: onCreated,
      creationParamsCodec: const StandardMessageCodec(),
    );
  }
}
