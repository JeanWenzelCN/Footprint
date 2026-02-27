import 'dart:convert';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/rendering.dart';

void main() {
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.dark,
    systemNavigationBarColor: Colors.transparent,
  ));
  runApp(const MyApp());
}

// --- 顶级定义的录入页面，1:1 复刻 ---
class AddFootprintPage extends StatefulWidget {
  const AddFootprintPage({super.key});
  @override State<AddFootprintPage> createState() => _AddFootprintPageState();
}
class _AddFootprintPageState extends State<AddFootprintPage> {
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _locationController = TextEditingController();
  final TextEditingController _detailController = TextEditingController();
  final TextEditingController _distanceController = TextEditingController(text: "5.0");
  String selectedIcon = "LocationOn"; double energyLevel = 6.0; String selectedMood = "愉快";
  final List<String> availableIcons = ["LocationOn", "Restaurant", "LocalCafe", "Park", "Flight", "Train", "DirectionsBike", "ShoppingBag", "CameraAlt"];
  @override Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme; final tt = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: const Text('记录足迹', style: TextStyle(fontWeight: FontWeight.bold)), leading: IconButton(icon: const Icon(Icons.close), onPressed: () => Navigator.pop(context))),
      body: ListView(padding: const EdgeInsets.all(24), children: [
        TextField(controller: _titleController, decoration: InputDecoration(labelText: '标题', border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)))),
        const SizedBox(height: 24),
        Text("选择图标", style: tt.labelSmall?.copyWith(color: cs.outline)),
        const SizedBox(height: 12),
        SizedBox(height: 50, child: ListView.separated(scrollDirection: Axis.horizontal, itemCount: availableIcons.length, separatorBuilder: (_, __) => const SizedBox(width: 12), itemBuilder: (context, index) { bool isSelected = selectedIcon == availableIcons[index]; return GestureDetector(onTap: () => setState(() => selectedIcon = availableIcons[index]), child: CircleAvatar(radius: 22, backgroundColor: isSelected ? cs.primary : cs.surfaceContainerHighest.withValues(alpha: 0.5), child: Icon(_getIconData(availableIcons[index]), color: isSelected ? Colors.white : cs.onSurfaceVariant, size: 20))); })),
        const SizedBox(height: 24),
        Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Expanded(child: Column(children: [const Icon(Icons.straighten, color: Colors.blue, size: 28), const SizedBox(height: 8), TextField(controller: _distanceController, textAlign: TextAlign.center, decoration: InputDecoration(labelText: '里程', suffixText: 'KM', border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)), contentPadding: const EdgeInsets.symmetric(horizontal: 8)), style: const TextStyle(fontSize: 12))])),
          const SizedBox(width: 12),
          Expanded(child: Column(children: [const Icon(Icons.bolt, color: Colors.orange, size: 28), Text("能量: ${energyLevel.toInt()}", style: tt.labelSmall), Slider(value: energyLevel, min: 1, max: 10, divisions: 9, onChanged: (v) => setState(() => energyLevel = v))])),
          const SizedBox(width: 12),
          Expanded(child: Column(children: [const Icon(Icons.mood, color: Colors.green, size: 28), const SizedBox(height: 8), DropdownButtonFormField<String>(value: selectedMood, decoration: InputDecoration(labelText: '心情', border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)), contentPadding: const EdgeInsets.symmetric(horizontal: 8)), items: ["愉快", "平静", "感悟", "疲惫"].map((m) => DropdownMenuItem(value: m, child: Text(m, style: const TextStyle(fontSize: 12)))).toList(), onChanged: (v) => setState(() => selectedMood = v!))])),
        ]),
        const SizedBox(height: 24),
        TextField(controller: _locationController, decoration: InputDecoration(labelText: '地点', prefixIcon: const Icon(Icons.place_outlined), border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)))),
        const SizedBox(height: 16),
        TextField(controller: _detailController, maxLines: 4, decoration: InputDecoration(labelText: '故事', alignLabelWithHint: true, border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)))),
        const SizedBox(height: 32),
        FilledButton(onPressed: () => Navigator.pop(context), style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(56), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16))), child: const Text("保存足迹", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16))),
        const SizedBox(height: 100),
      ]),
    );
  }
  IconData _getIconData(String name) {
    switch (name) { case "Restaurant": return Icons.restaurant; case "LocalCafe": return Icons.local_cafe; case "Park": return Icons.park; case "Flight": return Icons.flight; default: return Icons.location_on; }
  }
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});
  @override State<MyApp> createState() => _MyAppState();
}
class _MyAppState extends State<MyApp> {
  String themeModeStr = "SYSTEM"; String nickname = "探索者"; String avatarId = "avatar_1"; String themeStyleStr = "CLASSIC"; bool hapticEnabled = true;
  @override void initState() { super.initState(); _loadAllSettings(); }
  Future<void> _loadAllSettings() async {
    const channel = MethodChannel('com.footprint/data');
    try {
      final jsonStr = await channel.invokeMethod<String>('getSettings');
      if (jsonStr != null) {
        final data = jsonDecode(jsonStr);
        setState(() { nickname = data['nickname'] ?? "探索者"; avatarId = data['avatarId'] ?? "avatar_1"; themeModeStr = data['themeMode'] ?? "SYSTEM"; themeStyleStr = data['themeStyle'] ?? "CLASSIC"; hapticEnabled = data['hapticEnabled'] ?? true; });
      }
    } catch (e) { debugPrint("Settings Error: $e"); }
  }
  ThemeMode _getThemeMode() { switch (themeModeStr) { case "LIGHT": return ThemeMode.light; case "DARK": return ThemeMode.dark; default: return ThemeMode.system; } }
  Color _getSeedColor() { switch (themeStyleStr) { case "CYBERPUNK": return Colors.cyanAccent; case "FOREST": return Colors.green; case "SAHARA": return Colors.orange; case "AUTO": return Colors.deepPurpleAccent; default: return const Color(0xFF1A73E8); } }
  @override Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false, themeMode: _getThemeMode(),
      theme: ThemeData(useMaterial3: true, colorScheme: ColorScheme.fromSeed(seedColor: _getSeedColor(), brightness: Brightness.light)),
      darkTheme: ThemeData(useMaterial3: true, colorScheme: ColorScheme.fromSeed(seedColor: _getSeedColor(), brightness: Brightness.dark)),
      home: MainContainer(nickname: nickname, avatarId: avatarId, themeMode: themeModeStr, themeStyle: themeStyleStr, hapticEnabled: hapticEnabled, onSettingsChanged: _loadAllSettings),
    );
  }
}

class MainContainer extends StatefulWidget {
  final String nickname; final String avatarId; final String themeMode; final String themeStyle; final bool hapticEnabled; final VoidCallback onSettingsChanged;
  const MainContainer({super.key, required this.nickname, required this.avatarId, required this.themeMode, required this.themeStyle, required this.hapticEnabled, required this.onSettingsChanged});
  @override State<MainContainer> createState() => _MainContainerState();
}

class _MainContainerState extends State<MainContainer> with SingleTickerProviderStateMixin {
  int _selectedIndex = 0;
  late AnimationController _navController;
  late Animation<double> _elasticAnimation;
  bool _isHiding = false;

  @override void initState() {
    super.initState();
    _navController = AnimationController(vsync: this, duration: const Duration(milliseconds: 600));
    // 柔和的阻尼曲线，反向收缩使用 easeInBack 形成蓄力感
    _elasticAnimation = CurvedAnimation(parent: _navController, curve: Curves.elasticOut, reverseCurve: Curves.easeInBack);
    _navController.value = 1.0;
  }

  void _handleScroll(ScrollNotification notification) {
    if (notification is ScrollUpdateNotification) {
      final delta = notification.scrollDelta;
      if (delta == null || delta == 0) return;
      
      // 添加防抖阈值，更敏锐跟手
      if (delta > 8.0 && !_isHiding && _navController.status != AnimationStatus.reverse) {
        _isHiding = true; _navController.reverse();
      } else if (delta < -8.0 && _isHiding && _navController.status != AnimationStatus.forward) {
        _isHiding = false; _navController.forward();
      }
    }
  }

  @override void dispose() { _navController.dispose(); super.dispose(); }

  @override Widget build(BuildContext context) {
    final pages = [
      DashboardScreen(nickname: widget.nickname, avatarId: widget.avatarId),
      const ExploreMapScreen(),
      const ArtStudioScreen(),
      SettingsScreen(nickname: widget.nickname, avatarId: widget.avatarId, themeMode: widget.themeMode, themeStyle: widget.themeStyle, hapticEnabled: widget.hapticEnabled, onUpdate: widget.onSettingsChanged),
    ];

    return Scaffold(
      extendBody: true,
      body: NotificationListener<ScrollNotification>(
        onNotification: (notification) { _handleScroll(notification); return false; },
        child: IndexedStack(index: _selectedIndex, children: pages),
      ),
      // --- 终极优化：彻底杜绝红屏的弹性水滴 Docker 栏 ---
      bottomNavigationBar: AnimatedBuilder(
        animation: _elasticAnimation,
        builder: (context, child) {
          final rawT = _elasticAnimation.value;
          // 绝对防御：在计算颜色透明度和阴影等不支持负数的属性时，强制 clamp 至 0.0~1.0 之间
          final clampedT = rawT.clamp(0.0, 1.0);
          
          final screenWidth = MediaQuery.of(context).size.width;
          final fullWidth = screenWidth - 64;
          const dropSize = 64.0;
          
          // 宽度允许微弱的物理弹性，但绝不能小于基础的圆形水滴尺寸
          final currentWidth = math.max(dropSize, dropSize + (fullWidth - dropSize) * rawT);
          // 收缩成水滴后，进行最后一段消失缩放
          final overallScale = clampedT < 0.15 ? (clampedT / 0.15) : 1.0;

          return Align(
            alignment: Alignment.bottomCenter,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 32),
              child: Transform.scale(
                scale: overallScale,
                child: Container(
                  width: currentWidth, height: dropSize,
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surface.withValues(alpha: 0.95),
                    borderRadius: BorderRadius.circular(dropSize / 2),
                    boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.15 * clampedT), blurRadius: 20 * clampedT, offset: Offset(0, 10 * clampedT))],
                    border: Border.all(color: Theme.of(context).colorScheme.outlineVariant.withValues(alpha: 0.4 * clampedT)),
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(dropSize / 2),
                    child: OverflowBox(
                      minWidth: fullWidth, maxWidth: fullWidth,
                      minHeight: dropSize, maxHeight: dropSize,
                      child: Stack(
                        alignment: Alignment.center,
                        children: List.generate(4, (index) {
                          final icons = [Icons.dashboard, Icons.explore, Icons.palette, Icons.settings];
                          final outlines = [Icons.dashboard_outlined, Icons.explore_outlined, Icons.palette_outlined, Icons.settings_outlined];
                          final double step = fullWidth / 4;
                          final double originX = (index - 1.5) * step;
                          final double currentX = originX * clampedT; // 两侧平滑向中心聚合

                          return Positioned(
                            left: (fullWidth / 2) + currentX - 24,
                            child: Opacity(
                              opacity: (clampedT * 4 - 3).clamp(0.0, 1.0),
                              child: GestureDetector(
                                onTap: () { setState(() => _selectedIndex = index); if (widget.hapticEnabled) HapticFeedback.lightImpact(); },
                                child: Container(
                                  width: 48, height: 48,
                                  decoration: BoxDecoration(color: _selectedIndex == index ? Theme.of(context).colorScheme.primaryContainer : Colors.transparent, shape: BoxShape.circle),
                                  child: Icon(_selectedIndex == index ? icons[index] : outlines[index], color: _selectedIndex == index ? Theme.of(context).colorScheme.onPrimaryContainer : Theme.of(context).colorScheme.onSurfaceVariant, size: 24),
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

// --- 状态页 (DashboardScreen) ---
class DashboardScreen extends StatefulWidget {
  final String nickname; final String avatarId;
  const DashboardScreen({super.key, required this.nickname, required this.avatarId});
  @override State<DashboardScreen> createState() => _DashboardScreenState();
}
class _DashboardScreenState extends State<DashboardScreen> {
  int currentYear = DateTime.now().year;
  IconData _getAvatarIcon(String id) {
    switch (id) { case "avatar_2": return Icons.account_circle; case "avatar_3": return Icons.smart_toy; case "avatar_4": return Icons.fingerprint; default: return Icons.face; }
  }
  void _showDetail(String t, String l, String d) {
    showModalBottomSheet(context: context, isScrollControlled: true, shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(32))), builder: (context) => DraggableScrollableSheet(initialChildSize: 0.6, expand: false, builder: (context, sc) => Container(padding: const EdgeInsets.all(24), child: ListView(controller: sc, children: [
      Center(child: Container(width: 32, height: 4, decoration: BoxDecoration(color: Colors.grey[300], borderRadius: BorderRadius.circular(2)))),
      const SizedBox(height: 24),
      Row(children: [CircleAvatar(radius: 28, backgroundColor: Colors.orange.withValues(alpha: 0.1), child: const Icon(Icons.landscape, color: Colors.orange, size: 32)), const SizedBox(width: 16), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(t, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold)), Text(d, style: TextStyle(color: Theme.of(context).colorScheme.outline))]))]),
      const SizedBox(height: 24),
      _info(Icons.place_outlined, "地点", l), _info(Icons.straighten_outlined, "距离", "18.4 km"),
      const SizedBox(height: 32),
      Row(children: [Expanded(child: OutlinedButton.icon(onPressed: () => Navigator.pop(context), icon: const Icon(Icons.edit), label: const Text('编辑'))), const SizedBox(width: 12), Expanded(child: FilledButton.icon(onPressed: () => Navigator.pop(context), icon: const Icon(Icons.share), label: const Text('分享')))]),
    ]))));
  }
  Widget _info(IconData i, String l, String v) => Padding(padding: const EdgeInsets.symmetric(vertical: 12), child: Row(children: [Icon(i, size: 20, color: Theme.of(context).colorScheme.primary), const SizedBox(width: 12), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(l, style: const TextStyle(fontSize: 12, color: Colors.grey)), Text(v, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500))]))]));
  @override Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme; final tt = Theme.of(context).textTheme;
    return Scaffold(
      body: Stack(children: [
        ListView(padding: const EdgeInsets.only(top: 190, bottom: 150, left: 16, right: 16), children: [
          Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [const Text('年份筛选', style: TextStyle(fontWeight: FontWeight.bold)), Text('$currentYear', style: tt.titleMedium)]),
          const SizedBox(height: 12),
          Column(children: [Row(children: [_sBox(cs, tt, "足迹", "42"), const SizedBox(width: 8), _sBox(cs, tt, "里程", "128.5", u: "km")]), const SizedBox(height: 8), Row(children: [_sBox(cs, tt, "地点", "12"), const SizedBox(width: 8), _sBox(cs, tt, "记录", "28")])]),
          const SizedBox(height: 12),
          Card(elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24), side: BorderSide(color: cs.outlineVariant)), child: InkWell(onTap: () => _showDetail("川西穿越", "四姑娘山", "2年前"), child: Padding(padding: const EdgeInsets.all(16), child: Row(children: [Container(width: 56, height: 56, decoration: BoxDecoration(color: Colors.orange.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(12)), child: const Icon(Icons.landscape, color: Colors.orange, size: 32)), const SizedBox(width: 16), const Expanded(child: Text('那年今日：川西彩林穿越', style: TextStyle(fontWeight: FontWeight.bold)))])))),
          const SizedBox(height: 12),
          Card(elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28), side: BorderSide(color: cs.outlineVariant)), child: const ListTile(leading: Icon(Icons.history), title: Text('时光足迹回放', style: TextStyle(fontWeight: FontWeight.bold)), trailing: Icon(Icons.keyboard_arrow_right))),
        ]),
        _fixedTop(cs, tt),
      ]),
      floatingActionButton: Padding(padding: const EdgeInsets.only(bottom: 110), child: FloatingActionButton(onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (context) => const AddFootprintPage())), backgroundColor: cs.primary, child: const Icon(Icons.add, color: Colors.white, size: 32))),
    );
  }
  Widget _sBox(ColorScheme cs, TextTheme tt, String l, String v, {String? u}) => Expanded(child: Card(elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28), side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.3))), child: Padding(padding: const EdgeInsets.symmetric(vertical: 16), child: Column(children: [Text(v, style: tt.titleLarge?.copyWith(fontWeight: FontWeight.w900)), Text(l, style: tt.labelSmall)]))));
  Widget _fixedTop(ColorScheme cs, TextTheme tt) => Positioned(top: 0, left: 0, right: 0, child: Container(color: cs.surface.withValues(alpha: 0.95), padding: const EdgeInsets.only(top: 48, bottom: 12, left: 16, right: 16), child: Column(children: [
    Row(children: [CircleAvatar(radius: 24, backgroundColor: cs.primaryContainer, child: Icon(_getAvatarIcon(widget.avatarId), color: cs.onPrimaryContainer)), const SizedBox(width: 12), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('早安, ${widget.nickname}', style: tt.titleLarge?.copyWith(fontWeight: FontWeight.w900, color: cs.primary), maxLines: 1, overflow: TextOverflow.ellipsis), const Text('进阶探索者', style: TextStyle(fontSize: 12))]))]),
    const SizedBox(height: 12), TextField(decoration: InputDecoration(hintText: '搜索...', prefixIcon: const Icon(Icons.search), filled: true, fillColor: cs.surfaceContainerHighest.withValues(alpha: 0.5), border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none), contentPadding: const EdgeInsets.symmetric(vertical: 8))),
  ])));
}

class ExploreMapScreen extends StatelessWidget {
  const ExploreMapScreen({super.key});
  @override Widget build(BuildContext context) { return const Stack(children: [Positioned.fill(child: NativeMapView())]); }
}
class ArtStudioScreen extends StatelessWidget {
  const ArtStudioScreen({super.key});
  @override Widget build(BuildContext context) { return Scaffold(appBar: AppBar(title: const Text('生成艺术')), body: const Center(child: Text("工坊内容"))); }
}

// --- 设置页 (SettingsScreen) ---
class SettingsScreen extends StatefulWidget {
  final String nickname; final String avatarId; final String themeMode; final String themeStyle; final bool hapticEnabled; final VoidCallback onUpdate;
  const SettingsScreen({super.key, required this.nickname, required this.avatarId, required this.themeMode, required this.themeStyle, required this.hapticEnabled, required this.onUpdate});
  @override State<SettingsScreen> createState() => _SettingsScreenState();
}
class _SettingsScreenState extends State<SettingsScreen> {
  static const channel = MethodChannel('com.footprint/data');
  void _update(String m, dynamic v) async { await channel.invokeMethod(m, v); widget.onUpdate(); }
  @override Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('设置', style: TextStyle(fontWeight: FontWeight.bold))),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        _t("数字身份", cs),
        Card(elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24), side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.5))), child: Padding(padding: const EdgeInsets.all(20), child: Column(children: [
          TextField(controller: TextEditingController(text: widget.nickname), decoration: const InputDecoration(labelText: '代号 (Nickname)', border: OutlineInputBorder()), onSubmitted: (v) => _update('updateNickname', v)),
          const SizedBox(height: 20),
          const Align(alignment: Alignment.centerLeft, child: Text("头像接入点", style: TextStyle(fontWeight: FontWeight.bold))),
          const SizedBox(height: 12),
          Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: ["avatar_1", "avatar_2", "avatar_3", "avatar_4"].map((id) => GestureDetector(onTap: () => _update('updateAvatar', id), child: CircleAvatar(radius: 26, backgroundColor: widget.avatarId == id ? cs.primary : cs.surfaceContainerHighest, child: Icon(_getIcon(id), color: widget.avatarId == id ? Colors.white : cs.onSurfaceVariant)))).toList()),
        ]))),
        const SizedBox(height: 24),
        _t("系统模式", cs), 
        Card(elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24), side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.5))), child: Column(children: [
          RadioListTile(title: const Text("跟随系统"), value: "SYSTEM", groupValue: widget.themeMode, onChanged: (v) => _update('updateThemeMode', v)),
          RadioListTile(title: const Text("日间模式"), value: "LIGHT", groupValue: widget.themeMode, onChanged: (v) => _update('updateThemeMode', v)),
          RadioListTile(title: const Text("夜间模式"), value: "DARK", groupValue: widget.themeMode, onChanged: (v) => _update('updateThemeMode', v)),
        ])),
        const SizedBox(height: 24),
        _t("视觉风格", cs),
        Card(elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24), side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.5))), child: Column(children: [
          _sTile("智能自适应", Icons.auto_awesome, "AUTO", cs),
          _sTile("经典蓝调", Icons.palette, "CLASSIC", cs),
          _sTile("赛博朋克", Icons.electric_bolt, "CYBERPUNK", cs),
          _sTile("森林氧吧", Icons.forest, "FOREST", cs),
          _sTile("撒哈拉之光", Icons.wb_sunny, "SAHARA", cs),
        ])),
        const SizedBox(height: 24),
        _t("交互体验", cs), // 彻底分离触感反馈
        Card(elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24), side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.5))), child: SwitchListTile(secondary: const Icon(Icons.vibration), title: const Text("开启触感反馈"), value: widget.hapticEnabled, onChanged: (v) => _update('updateHaptic', v))),
        const SizedBox(height: 24),
        _t("数据管理", cs), // 独立的备份导入
        Card(elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24), side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.5))), child: Column(children: [
          ListTile(leading: const Icon(Icons.cloud_upload), title: const Text("导出足迹备份"), trailing: const Icon(Icons.keyboard_arrow_right), onTap: () => _update('exportData', null)),
          const Divider(indent: 16, endIndent: 16),
          ListTile(leading: const Icon(Icons.cloud_download), title: const Text("导入历史记录"), trailing: const Icon(Icons.keyboard_arrow_right), onTap: () => _update('importData', null)),
        ])),
        const SizedBox(height: 24),
        _t("关于", cs), // 补全关于应用
        Card(elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24), side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.5))), child: const ListTile(leading: Icon(Icons.info_outline), title: Text("软件版本"), trailing: Text("v2.11.1"))),
        const SizedBox(height: 100),
      ]),
    );
  }
  Widget _t(String t, ColorScheme cs) => Padding(padding: const EdgeInsets.only(left: 4, bottom: 12), child: Text(t, style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold, fontSize: 13)));
  Widget _sTile(String t, IconData i, String v, ColorScheme cs) => ListTile(leading: Icon(i, color: widget.themeStyle == v ? cs.primary : cs.outline), title: Text(t, style: TextStyle(color: widget.themeStyle == v ? cs.primary : null, fontWeight: widget.themeStyle == v ? FontWeight.bold : null)), trailing: widget.themeStyle == v ? Icon(Icons.check, color: cs.primary) : null, onTap: () => _update('updateThemeStyle', v));
  IconData _getIcon(String id) { switch (id) { case "avatar_2": return Icons.account_circle; case "avatar_3": return Icons.smart_toy; case "avatar_4": return Icons.fingerprint; default: return Icons.face; } }
}

class NativeMapView extends StatelessWidget {
  const NativeMapView({super.key});
  @override Widget build(BuildContext context) { return const AndroidView(viewType: 'com.footprint/amap', layoutDirection: TextDirection.ltr, creationParams: {"zoom": 15.0}, creationParamsCodec: StandardMessageCodec()); }
}
