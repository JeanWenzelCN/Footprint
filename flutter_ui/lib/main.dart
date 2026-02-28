import 'dart:convert';
import 'dart:io';
import 'dart:ui';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/rendering.dart';
import 'dart:async';
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
  final TextEditingController _distanceController = TextEditingController(text: "5.0");
  final TextEditingController _tagsController = TextEditingController();
  final TextEditingController _tempController = TextEditingController();
  final TextEditingController _altController = TextEditingController();

  String selectedIcon = "LocationOn";
  double energyLevel = 6.0;
  String selectedMood = "愉快";
  String selectedWeather = "晴朗";
  String selectedTransport = "步行";
  List<File> photos = [];

  final List<String> availableIcons = [
    "LocationOn", "Restaurant", "LocalCafe", "Park", "Flight",
    "Train", "DirectionsBike", "ShoppingBag", "CameraAlt",
  ];

  final List<String> weathers = ["晴朗", "多云", "阴天", "雨", "雪", "风", "雾"];
  final List<String> transports = ["步行", "骑行", "自驾", "铁路", "航空", "未知"];
  final List<String> moods = ["愉快", "平静", "兴奋", "疲惫", "失落", "惊喜"];

  Future<void> _pickImage() async {
    final ImagePicker picker = ImagePicker();
    final List<XFile> images = await picker.pickMultiImage();
    if (images.isNotEmpty) {
      setState(() => photos.addAll(images.map((img) => File(img.path))));
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('记录新的足迹', style: TextStyle(fontWeight: FontWeight.bold)),
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
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _tagsController,
            decoration: InputDecoration(
              labelText: '标签 (用逗号分隔)',
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
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
                  onTap: () => setState(() => selectedIcon = availableIcons[index]),
                  child: CircleAvatar(
                    radius: 22,
                    backgroundColor: isSelected ? cs.primary : cs.surfaceContainerHighest.withValues(alpha: 0.5),
                    child: Icon(_getIconData(availableIcons[index]), color: isSelected ? Colors.white : cs.onSurfaceVariant, size: 20),
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 24),
          Row(
            children: [
              Expanded(child: DropdownButtonFormField<String>(
                value: selectedMood,
                decoration: InputDecoration(labelText: '心情', border: OutlineInputBorder(borderRadius: BorderRadius.circular(12))),
                items: moods.map((m) => DropdownMenuItem(value: m, child: Text(m))).toList(),
                onChanged: (v) => setState(() => selectedMood = v!),
              )),
              const SizedBox(width: 12),
              Expanded(child: DropdownButtonFormField<String>(
                value: selectedWeather,
                decoration: InputDecoration(labelText: '天气', border: OutlineInputBorder(borderRadius: BorderRadius.circular(12))),
                items: weathers.map((w) => DropdownMenuItem(value: w, child: Text(w))).toList(),
                onChanged: (v) => setState(() => selectedWeather = v!),
              )),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(child: DropdownButtonFormField<String>(
                value: selectedTransport,
                decoration: InputDecoration(labelText: '出行方式', border: OutlineInputBorder(borderRadius: BorderRadius.circular(12))),
                items: transports.map((t) => DropdownMenuItem(value: t, child: Text(t))).toList(),
                onChanged: (v) => setState(() => selectedTransport = v!),
              )),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text("活力指数 ${energyLevel.toInt()}", style: tt.bodySmall),
                    Slider(
                      value: energyLevel, min: 1, max: 10, divisions: 9,
                      onChanged: (v) => setState(() => energyLevel = v),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(child: TextField(
                controller: _distanceController,
                decoration: InputDecoration(labelText: '里程(km)', border: OutlineInputBorder(borderRadius: BorderRadius.circular(12))),
                keyboardType: TextInputType.number,
              )),
              const SizedBox(width: 12),
              Expanded(child: TextField(
                controller: _tempController,
                decoration: InputDecoration(labelText: '气温(℃)', border: OutlineInputBorder(borderRadius: BorderRadius.circular(12))),
                keyboardType: TextInputType.number,
              )),
              const SizedBox(width: 12),
              Expanded(child: TextField(
                controller: _altController,
                decoration: InputDecoration(labelText: '海拔(m)', border: OutlineInputBorder(borderRadius: BorderRadius.circular(12))),
                keyboardType: TextInputType.number,
              )),
            ],
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _locationController,
            decoration: InputDecoration(labelText: '地点', prefixIcon: const Icon(Icons.place_outlined), border: OutlineInputBorder(borderRadius: BorderRadius.circular(12))),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _detailController,
            maxLines: 4,
            decoration: InputDecoration(labelText: '故事', border: OutlineInputBorder(borderRadius: BorderRadius.circular(12))),
          ),
          const SizedBox(height: 16),
          Text("照片记录", style: tt.labelMedium),
          const SizedBox(height: 8),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                GestureDetector(
                  onTap: _pickImage,
                  child: Container(
                    width: 80, height: 80,
                    decoration: BoxDecoration(color: cs.surfaceContainerHighest.withValues(alpha: 0.5), borderRadius: BorderRadius.circular(12)),
                    child: const Icon(Icons.add_a_photo, size: 32),
                  ),
                ),
                const SizedBox(width: 12),
                ...photos.map((f) => Padding(
                  padding: const EdgeInsets.only(right: 12),
                  child: Stack(
                    children: [
                      ClipRRect(borderRadius: BorderRadius.circular(12), child: Image.file(f, width: 80, height: 80, fit: BoxFit.cover)),
                      Positioned(
                        right: 0, top: 0,
                        child: GestureDetector(
                          onTap: () => setState(() => photos.remove(f)),
                          child: Container(decoration: const BoxDecoration(color: Colors.black54, shape: BoxShape.circle), padding: const EdgeInsets.all(4), child: const Icon(Icons.close, color: Colors.white, size: 16)),
                        ),
                      )
                    ],
                  ),
                )),
              ],
            ),
          ),
          const SizedBox(height: 32),
          FilledButton(
            onPressed: () => Navigator.pop(context),
            style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(56), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16))),
            child: const Text("记录足迹", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
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
  List<dynamic> allEntries = [];
  List<dynamic> onThisDayEntries = [];
  List<dynamic> yearlyGoals = [];
  double totalDistance = 0.0;
  int uniquePlacesLength = 0;
  double avgEnergy = 0.0;

  String _getGreeting() {
    final hour = DateTime.now().hour;
    if (hour >= 5 && hour < 9) return "早安";
    if (hour >= 9 && hour < 12) return "上午好";
    if (hour >= 12 && hour < 14) return "午安";
    if (hour >= 14 && hour < 18) return "下午好";
    if (hour >= 18 && hour < 23) return "晚安";
    return "深夜了";
  }
  String dominantMoodStr = "愉快";

  final List<String> dailyQuotes = [
    "每一次相遇都是久别重逢，每一步足迹都是人生印记。",
    "走过的路，看过的风景，最终都会成为更好的自己。",
    "时间沉淀了情绪，留下了故事的余温。",
    "心之所向，步履以往，世界在脚下延展。",
    "这世界的辽阔，由你的脚步去丈量。",
    "记忆是一张地图，每个坐标都写着从前。",
    "风带来的气息，是我们曾经路过的证明。"
  ];

  @override
  void initState() {
    super.initState();
    _loadEntries();
  }

  String _mapMoodToChinese(String englishMood) {
    if (englishMood.contains(RegExp(r'[\u4e00-\u9fa5]'))) return englishMood;
    switch (englishMood.toUpperCase()) {
      case "EXCITED": return "激情";
      case "CURIOUS": return "探索";
      case "RELAXED": return "放松";
      case "REFLECTIVE": return "思考";
      case "HAPPY": return "愉快";
      case "CALM": return "平静";
      default: return englishMood;
    }
  }

  Future<void> _loadEntries() async {
    const channel = MethodChannel('com.footprint/data');
    try {
      final jsonStr = await channel.invokeMethod<String>('getAllEntries');
      final goalsJsonStr = await channel.invokeMethod<String>('getAllGoals');
      
      List<dynamic> yGoals = [];
      if (goalsJsonStr != null) {
        final List<dynamic> allGoals = jsonDecode(goalsJsonStr);
        yGoals = allGoals.where((g) {
          final dateStr = g['targetDate'] as String?;
          if (dateStr != null && dateStr.length >= 4) {
             final y = int.tryParse(dateStr.substring(0,4));
             return y == currentYear;
          }
          return false;
        }).toList();
        yGoals.sort((a,b) => (a['targetDate'] ?? '').compareTo(b['targetDate'] ?? ''));
      }

      if (jsonStr != null) {
        final List<dynamic> entries = jsonDecode(jsonStr);
        final today = DateTime.now();
        List<dynamic> onThisDay = [];

        final yearEntries = entries.where((e) {
          final dateStr = e['happenedOn'] as String?;
          if (dateStr != null && dateStr.length >= 10) {
            final parts = dateStr.split('-');
            final y = int.tryParse(parts[0]);
            final m = int.tryParse(parts[1]);
            final d = int.tryParse(parts[2].substring(0, 2));
            if (y != null && y < today.year && m == today.month && d == today.day) {
              onThisDay.add(e);
            }
            return y == currentYear;
          }
          return false;
        }).toList();
        
        // Sort chronologically (newest first)
        yearEntries.sort((a,b) => (b['happenedOn'] ?? '').compareTo(a['happenedOn'] ?? ''));

        double dist = 0.0;
        double energySum = 0.0;
        final places = <String>{};
        final moodCounts = <String, int>{};

        for (var e in yearEntries) {
          dist += (e['distanceKm'] as num?)?.toDouble() ?? 0.0;
          final String? loc = e['location'];
          if (loc != null && loc.isNotEmpty) places.add(loc);
          energySum += (e['energyLevel'] as num?)?.toDouble() ?? 0.0;
          final String? mood = e['mood'];
          if (mood != null && mood.isNotEmpty) moodCounts[mood] = (moodCounts[mood] ?? 0) + 1;
        }

        String topMood = "未知";
        int topMoodCount = -1;
        moodCounts.forEach((k, v) {
          if (v > topMoodCount) {
             topMoodCount = v;
             topMood = k;
          }
        });

        setState(() {
          allEntries = yearEntries;
          onThisDayEntries = onThisDay;
          yearlyGoals = yGoals;
          totalDistance = dist;
          uniquePlacesLength = places.length;
          avgEnergy = yearEntries.isNotEmpty ? energySum / yearEntries.length : 0.0;
          if (topMoodCount > 0) dominantMoodStr = _mapMoodToChinese(topMood);
        });
      }
    } catch (e) {
      debugPrint("Error loading entries: $e");
    }
  }

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

  void _showDetail(dynamic entryData) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => FootprintDetailPage(entry: entryData)),
    );
  }

  void _showStatDetail(BuildContext context, String title, String subtitle, List<dynamic> entriesToList) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) {
        final cs = Theme.of(ctx).colorScheme;
        return DraggableScrollableSheet(
          initialChildSize: 0.6,
          minChildSize: 0.4,
          maxChildSize: 0.9,
          builder: (_, controller) {
            return Container(
              decoration: BoxDecoration(
                color: cs.surface,
                borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
              ),
              child: Column(
                children: [
                   Padding(
                     padding: const EdgeInsets.all(16.0),
                     child: Text(title, style: TextStyle(color: cs.primary, fontSize: 18, fontWeight: FontWeight.bold)),
                   ),
                   const Divider(),
                   Expanded(
                     child: entriesToList.isEmpty ? 
                       Center(child: Text("暂无记录", style: TextStyle(color: cs.outline))) :
                       ListView.builder(
                       controller: controller,
                       itemCount: entriesToList.length,
                       itemBuilder: (context, index) {
                         final entry = entriesToList[index];
                         final eTitle = entry['title'] ?? '未知足迹';
                         final eLoc = entry['location'] ?? '未知地点';
                         final eDist = entry['distanceKm']?.toString() ?? '0';
                         final eDate = entry['happenedOn'] ?? '';
                         return ListTile(
                           leading: Icon(Icons.place, color: cs.primary),
                           title: Text(eTitle),
                           subtitle: Text("$eDate · $eLoc"),
                           trailing: Text("${eDist}km", style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold)),
                         );
                       },
                     ),
                   ),
                ],
              ),
            );
          },
        );
      },
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
                      setState(() { currentYear--; _loadEntries(); });
                    },
                  ),
                  Text(
                    '$currentYear',
                    style: tt.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                  ),
                  IconButton(
                    icon: const Icon(Icons.chevron_right),
                    onPressed: () {
                      setState(() { currentYear++; _loadEntries(); });
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
                      _sBox(cs, tt, "足迹", "${allEntries.length}", onTap: () => _showStatDetail(context, "所有足迹 (${allEntries.length})", "年度足迹概览", allEntries)),
                      const SizedBox(width: 8),
                      _sBox(cs, tt, "里程", totalDistance.toStringAsFixed(1), u: "km", onTap: () {
                        final sorted = List.of(allEntries)..sort((a,b) => ((b['distanceKm'] as num?)?.toDouble() ?? 0.0).compareTo((a['distanceKm'] as num?)?.toDouble() ?? 0.0));
                        _showStatDetail(context, "年度总里程 (${totalDistance.toStringAsFixed(1)} km)", "按距离降序排序", sorted);
                      }),
                      const SizedBox(width: 8),
                      _sBox(cs, tt, "地点", "$uniquePlacesLength", onTap: () {
                        final seen = <String>{};
                        final unique = allEntries.where((e) {
                          final loc = e['location'] as String? ?? '';
                          if (seen.contains(loc) || loc.isEmpty) return false;
                          seen.add(loc);
                          return true;
                        }).toList();
                        _showStatDetail(context, "探索地点 ($uniquePlacesLength)", "不重复地点列表", unique);
                      }),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      _sBox(cs, tt, "记录", "${allEntries.length}", onTap: () => _showStatDetail(context, "记录条数 (${allEntries.length})", "今年所有打卡记录", allEntries)),
                      const SizedBox(width: 8),
                      _sBox(cs, tt, "活力", avgEnergy.toStringAsFixed(1), u: "指数", onTap: () {
                        final sorted = List.of(allEntries)..sort((a,b) => ((b['energyLevel'] as num?)?.toDouble() ?? 0.0).compareTo((a['energyLevel'] as num?)?.toDouble() ?? 0.0));
                        _showStatDetail(context, "平均活力指数 (${avgEnergy.toStringAsFixed(1)})", "按活力等级排序", sorted);
                      }),
                      const SizedBox(width: 8),
                      _sBox(cs, tt, "主情绪", dominantMoodStr, onTap: () {
                         final filtered = allEntries.where((e) => e['mood'] == dominantMoodStr).toList();
                         _showStatDetail(context, "主导心情 ($dominantMoodStr)", "心情统计明细", filtered);
                      }),
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
                             "时光碎片",
                             style: TextStyle(
                               color: cs.primary,
                               fontWeight: FontWeight.bold,
                               fontSize: 13,
                             ),
                           ),
                         ],
                       ),
                       const SizedBox(height: 8),
                       Text(
                         dailyQuotes[DateTime.now().day % dailyQuotes.length],
                         style: TextStyle(
                           color: cs.outline,
                           fontStyle: FontStyle.italic,
                           fontSize: 12,
                         ),
                       ),
                       const SizedBox(height: 16),
                       if (onThisDayEntries.isEmpty)
                         Text(
                           "往年的今天暂时没有足迹记录。",
                           style: TextStyle(color: cs.outline, fontSize: 13),
                         ),
                       ...onThisDayEntries.map((e) {
                         int pastYear = int.tryParse(e['happenedOn']?.toString().split('-')[0] ?? '0') ?? 0;
                         String yearDiff = (DateTime.now().year - pastYear).toString();
                         return Padding(
                           padding: const EdgeInsets.only(bottom: 12),
                           child: InkWell(
                             onTap: () => _showDetail(e),
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
                                       Text(
                                         e['title'] ?? '未知足迹',
                                         style: const TextStyle(
                                           fontWeight: FontWeight.bold,
                                           fontSize: 16,
                                         ),
                                       ),
                                       Text(
                                         '${yearDiff}年前 · ${_mapMoodToChinese(e['mood'] ?? '')}',
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
                         );
                       }),
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
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(Icons.flag, color: cs.secondary, size: 18),
                          const SizedBox(width: 8),
                          Text(
                            "年度旅行目标",
                            style: TextStyle(
                              color: cs.secondary,
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      if (yearlyGoals.isEmpty)
                        Text(
                          "暂无年度计划，去添加一个吧！",
                          style: TextStyle(color: cs.outline, fontSize: 13),
                        ),
                      ...yearlyGoals.map((g) {
                        final isCompleted = g['isCompleted'] == true;
                        final dateStr = g['targetDate'] ?? '';
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: InkWell(
                            onTap: () {
                              Navigator.push(context, MaterialPageRoute(builder: (_) => const GoalPlannerPage()));
                            },
                            child: Row(
                              children: [
                                Container(
                                  width: 36,
                                  height: 36,
                                  decoration: BoxDecoration(
                                    color: isCompleted ? cs.secondary : cs.secondaryContainer.withValues(alpha: 0.4),
                                    shape: BoxShape.circle,
                                  ),
                                  child: Icon(
                                    isCompleted ? Icons.check : Icons.flag,
                                    color: isCompleted ? Colors.white : cs.secondary,
                                    size: 18,
                                  ),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        g['title'] ?? '未命名目标',
                                        style: TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 14,
                                          color: cs.onSurface,
                                        ),
                                      ),
                                      Text(
                                        "${g['targetLocation'] ?? '未知位置'} · $dateStr",
                                        style: TextStyle(
                                          color: cs.onSurfaceVariant,
                                          fontSize: 12,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      }),
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
                  onTap: () {
                    Navigator.push(context, MaterialPageRoute(builder: (_) => const TimeFootprintPlaybackPage()));
                  },
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
              if (allEntries.isEmpty) ...[
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 32),
                  child: Center(
                    child: Text(
                      "此年份暂无足迹记录",
                      style: TextStyle(color: cs.outline),
                    ),
                  ),
                )
              ] else ...[
                // Render the list of footprints chronologically
                ...allEntries.map((entry) {
                   final String dateStr = entry['happenedOn'] ?? '';
                   final fragments = dateStr.split('-');
                   final String displayDate = fragments.length >= 3 ? "${fragments[1]}-${fragments[2].substring(0,2)}" : dateStr;
                   return Padding(
                     padding: const EdgeInsets.only(bottom: 12),
                     child: InkWell(
                       onTap: () => _showDetail(entry),
                       borderRadius: BorderRadius.circular(16),
                       child: Card(
                         elevation: 0,
                         margin: EdgeInsets.zero,
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
                           title: Text(
                             entry['title'] ?? '未知足迹',
                             style: const TextStyle(fontWeight: FontWeight.bold),
                             maxLines: 1,
                             overflow: TextOverflow.ellipsis,
                           ),
                           subtitle: Text(
                             '$displayDate · ${entry['location'] ?? '未知地点'}',
                             style: const TextStyle(fontSize: 12),
                           ),
                           trailing: Text(
                             '${entry['distanceKm']?.toString() ?? '0'} km',
                             style: TextStyle(
                               color: cs.primary,
                               fontWeight: FontWeight.bold,
                               fontSize: 12,
                             ),
                           ),
                         ),
                       ),
                     ),
                   );
                }).toList(),
              ],
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

  Widget _sBox(ColorScheme cs, TextTheme tt, String l, String v, {String? u, VoidCallback? onTap}) =>
      Expanded(
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(20),
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
                      '${_getGreeting()}, ${widget.nickname}',
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
  List<dynamic> _allEntries = [];

  @override
  void initState() {
    super.initState();
    _loadEntries();
    streamChannel.receiveBroadcastStream().listen((eventJson) {
      final event = jsonDecode(eventJson);
      if (mounted && event['type'] == 'status') {
        final newStatus = event['isTracking'];
        if (isTracking && !newStatus) {
          // If tracking was running and now stopped, reload to reflect the newly saved tracking footprint
          _loadEntries();
        }
        setState(() => isTracking = newStatus);
      }
    });
  }

  Future<void> _loadEntries() async {
    try {
      final String jsonStr = await dataChannel.invokeMethod('getAllEntries');
      setState(() {
        _allEntries = jsonDecode(jsonStr);
      });
      _updateNativeMap();
    } catch (e) {
      debugPrint("Failed to load map entries: $e");
    }
  }

  void _onMapCreated(int id) {
    _mapChannel = MethodChannel('com.footprint/amap_$id');
    _mapChannel?.setMethodCallHandler((call) async {
      if (call.method == 'onMarkerClick') {
        int entryId = call.arguments;
        if (!mounted) return;
        final entry = _allEntries.firstWhere((e) => e['id'] == entryId, orElse: () => null);
        if (entry != null) {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => FootprintDetailPage(entry: entry),
            ),
          );
        }
      }
    });
    _updateNativeMap();
  }

  void _updateNativeMap() {
    _mapChannel?.invokeMethod('setMapMode', mapMode);
    _mapChannel?.invokeMethod('setFogEnabled', mapMode == 'FOG');
    _mapChannel?.invokeMethod('setEntries', _allEntries);
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
            onPressed: () => _showApiKeyDialog(),
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
                onPressed: () => _mapChannel?.invokeMethod('centerLocation'),
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

  Future<void> _showApiKeyDialog() async {
    try {
      final String jsonStr = await dataChannel.invokeMethod('getAppCredentials');
      final creds = jsonDecode(jsonStr);
      final String pkgName = creds['packageName'] ?? "";
      final String sha1 = creds['sha1'] ?? "";
      final String currentKey = creds['amapKey'] ?? "";
      
      if (!mounted) return;
      
      final TextEditingController keyCtrl = TextEditingController(text: currentKey);
      
      await showDialog(
        context: context,
        builder: (ctx) {
          final colorScheme = Theme.of(context).colorScheme;
          return AlertDialog(
            title: const Text("API Key 设置"),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    "Package Name:",
                    style: TextStyle(
                      fontSize: 12,
                      color: colorScheme.onSurface.withOpacity(0.7),
                    ),
                  ),
                  const SizedBox(height: 4),
                  _buildCopyableRow(context, pkgName, "已复制包名"),
                  const SizedBox(height: 12),
                  Text(
                    "SHA1:",
                    style: TextStyle(
                      fontSize: 12,
                      color: colorScheme.onSurface.withOpacity(0.7),
                    ),
                  ),
                  const SizedBox(height: 4),
                  _buildCopyableRow(context, sha1, "已复制 SHA1"),
                  const SizedBox(height: 20),
                  TextField(
                    controller: keyCtrl,
                    style: const TextStyle(fontSize: 14),
                    decoration: const InputDecoration(
                      labelText: "AMAP Key",
                      hintText: "请输入您的高德地图 API Key",
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.all(Radius.circular(12)),
                      ),
                      contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    ),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx),
                child: Text("取消", style: TextStyle(color: colorScheme.outline)),
              ),
              FilledButton(
                onPressed: () {
                  dataChannel.invokeMethod('saveAmapKey', keyCtrl.text);
                  Navigator.pop(ctx);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text("API Key 已保存，请重启应用生效"))
                  );
                },
                style: FilledButton.styleFrom(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: const Text("保存"),
              ),
            ],
          );
        }
      );
    } catch (e) {
      debugPrint("获取凭证失败: $e");
    }
  }

  Widget _buildCopyableRow(BuildContext context, String text, String message) {
    final colorScheme = Theme.of(context).colorScheme;
    return InkWell(
      onTap: () {
        Clipboard.setData(ClipboardData(text: text));
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message), duration: const Duration(seconds: 1)));
      },
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: colorScheme.onSurface.withOpacity(0.05),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                text,
                style: const TextStyle(
                  fontSize: 11,
                  fontFamily: "monospace",
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            Icon(
              Icons.content_copy,
              size: 14,
              color: colorScheme.primary,
            ),
          ],
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
  late TextEditingController _nicknameController;
  Timer? _debounce;

  @override
  void initState() {
    super.initState();
    _nicknameController = TextEditingController(text: widget.nickname);
  }

  @override
  void didUpdateWidget(SettingsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.nickname != widget.nickname &&
        _nicknameController.text != widget.nickname) {
      _nicknameController.text = widget.nickname;
    }
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  void _up(String m, dynamic v) async {
    await channel.invokeMethod(m, v);
    widget.onUpdate();
  }

  void _onNicknameChanged(String v) {
    if (_debounce?.isActive ?? false) _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 600), () {
      _up('updateNickname', v);
    });
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
                    controller: _nicknameController,
                    decoration: const InputDecoration(
                      labelText: '代号 (Nickname)',
                      border: OutlineInputBorder(),
                    ),
                    onChanged: _onNicknameChanged,
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

class TimeFootprintPlaybackPage extends StatelessWidget {
  const TimeFootprintPlaybackPage({super.key});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('时光足迹回放', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.map, size: 80, color: cs.primary.withValues(alpha: 0.5)),
            const SizedBox(height: 16),
             Text("轨迹回放地图加载中...", style: TextStyle(color: cs.primary)),
            const SizedBox(height: 8),
             Text("这里将展示您的年度轨迹绿线动态回放", style: TextStyle(color: cs.outline)),
          ],
        ),
      ),
    );
  }
}
