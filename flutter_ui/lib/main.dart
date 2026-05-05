import 'dart:convert';
import 'dart:io';
import 'dart:ui';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'dart:async';
import 'package:file_picker/file_picker.dart';
import 'package:image_picker/image_picker.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:amap_flutter_location/amap_flutter_location.dart';
import 'package:amap_flutter_location/amap_location_option.dart';
import 'package:city_pickers/city_pickers.dart';
import 'footprint_detail_page.dart';
import 'goal_planner_page.dart';
import 'badge_hall_screen.dart';
import 'easter_egg.dart';
import 'package:flutter/foundation.dart';

const Color kAtelierCanvas = Color(0xFFF4EFE7);
const Color kAtelierSurface = Color(0xFFFFFCF7);
const Color kAtelierPrimary = Color(0xFF163A59);
const Color kAtelierAccent = Color(0xFFD47A53);
const Color kAtelierMint = Color(0xFF5D948E);
const Color kAtelierInk = Color(0xFF1B2430);
const Color kAtelierMuted = Color(0xFF66727D);
const Color kAtelierOutline = Color(0xFFD8D0C3);
const Color kAtelierSurfaceSoft = Color(0xFFEAE2D6);

class _FootprintPalette {
  final Color seed;
  final Color canvas;
  final Color surface;
  final Color surfaceHigh;
  final Color primary;
  final Color secondary;
  final Color tertiary;
  final Color ink;
  final Color muted;
  final Color outline;
  final Color shadow;
  final double radius;
  final double cardElevation;

  const _FootprintPalette({
    required this.seed,
    required this.canvas,
    required this.surface,
    required this.surfaceHigh,
    required this.primary,
    required this.secondary,
    required this.tertiary,
    required this.ink,
    required this.muted,
    required this.outline,
    required this.shadow,
    required this.radius,
    required this.cardElevation,
  });
}

_FootprintPalette _paletteForStyle(String style, bool isDark) {
  switch (style) {
    case 'CYBERPUNK':
      return _FootprintPalette(
        seed: const Color(0xFF00B8C8),
        canvas: isDark ? const Color(0xFF091012) : const Color(0xFFEAF6F5),
        surface: isDark ? const Color(0xFF111A1D) : const Color(0xFFF8FFFE),
        surfaceHigh: isDark ? const Color(0xFF1A292D) : const Color(0xFFDDEDEC),
        primary: isDark ? const Color(0xFF54DCE8) : const Color(0xFF0B6D78),
        secondary: isDark ? const Color(0xFFFF8FB5) : const Color(0xFFB73562),
        tertiary: isDark ? const Color(0xFFA6E887) : const Color(0xFF4A7C35),
        ink: isDark ? const Color(0xFFE9FBFA) : const Color(0xFF102226),
        muted: isDark ? const Color(0xFF8BA3A8) : const Color(0xFF587075),
        outline: isDark ? const Color(0xFF31474D) : const Color(0xFFB7D1D0),
        shadow: isDark ? Colors.black : const Color(0xFF437A83),
        radius: 14,
        cardElevation: 0,
      );
    case 'FOREST':
      return _FootprintPalette(
        seed: const Color(0xFF426B45),
        canvas: isDark ? const Color(0xFF101811) : const Color(0xFFEFF4EC),
        surface: isDark ? const Color(0xFF172218) : const Color(0xFFFBFFF8),
        surfaceHigh: isDark ? const Color(0xFF243224) : const Color(0xFFDDE8D7),
        primary: isDark ? const Color(0xFFA8D5A2) : const Color(0xFF2F5E35),
        secondary: isDark ? const Color(0xFFD8BD80) : const Color(0xFF8A6B2F),
        tertiary: isDark ? const Color(0xFF95C8BA) : const Color(0xFF3D7469),
        ink: isDark ? const Color(0xFFF0F7EC) : const Color(0xFF1E2B1F),
        muted: isDark ? const Color(0xFF9AA995) : const Color(0xFF65745F),
        outline: isDark ? const Color(0xFF3A4937) : const Color(0xFFC9D7C3),
        shadow: isDark ? Colors.black : const Color(0xFF8AA27E),
        radius: 20,
        cardElevation: 0,
      );
    case 'SAHARA':
      return _FootprintPalette(
        seed: const Color(0xFFB8763E),
        canvas: isDark ? const Color(0xFF1A130E) : const Color(0xFFF5EDE0),
        surface: isDark ? const Color(0xFF241A13) : const Color(0xFFFFFBF4),
        surfaceHigh: isDark ? const Color(0xFF35261B) : const Color(0xFFEAD8BE),
        primary: isDark ? const Color(0xFFE3B77C) : const Color(0xFF8B5428),
        secondary: isDark ? const Color(0xFFE39573) : const Color(0xFFB35D45),
        tertiary: isDark ? const Color(0xFFDCCB92) : const Color(0xFF796C36),
        ink: isDark ? const Color(0xFFF9EFE3) : const Color(0xFF33251B),
        muted: isDark ? const Color(0xFFB19B83) : const Color(0xFF806E5A),
        outline: isDark ? const Color(0xFF4E3A2B) : const Color(0xFFD9C4A6),
        shadow: isDark ? Colors.black : const Color(0xFFB98D61),
        radius: 18,
        cardElevation: 0,
      );
    case 'EMBER':
      return _FootprintPalette(
        seed: const Color(0xFF9E2F2E),
        canvas: isDark ? const Color(0xFF1A0F10) : const Color(0xFFF6ECE8),
        surface: isDark ? const Color(0xFF241617) : const Color(0xFFFFFBF8),
        surfaceHigh: isDark ? const Color(0xFF372121) : const Color(0xFFF0DCD5),
        primary: isDark ? const Color(0xFFE98F86) : const Color(0xFF8F2C2C),
        secondary: isDark ? const Color(0xFFD6A15B) : const Color(0xFFA3632C),
        tertiary: isDark ? const Color(0xFFC7A3A0) : const Color(0xFF6F5552),
        ink: isDark ? const Color(0xFFFBEFED) : const Color(0xFF2E1D1D),
        muted: isDark ? const Color(0xFFB69A97) : const Color(0xFF7B6562),
        outline: isDark ? const Color(0xFF4D3131) : const Color(0xFFD8BFB8),
        shadow: isDark ? Colors.black : const Color(0xFFB27C73),
        radius: 16,
        cardElevation: 0,
      );
    case 'AUTO':
      return _paletteForStyle(isDark ? 'CYBERPUNK' : 'CLASSIC', isDark);
    case 'CLASSIC':
    default:
      return _FootprintPalette(
        seed: kAtelierPrimary,
        canvas: isDark ? const Color(0xFF111417) : kAtelierCanvas,
        surface: isDark ? const Color(0xFF1A1D22) : kAtelierSurface,
        surfaceHigh: isDark ? const Color(0xFF2A2E35) : kAtelierSurfaceSoft,
        primary: isDark ? const Color(0xFFA9C4E2) : kAtelierPrimary,
        secondary: isDark ? const Color(0xFFFFB794) : kAtelierAccent,
        tertiary: isDark ? const Color(0xFF9DD9D3) : kAtelierMint,
        ink: isDark ? const Color(0xFFF3EEE7) : kAtelierInk,
        muted: isDark ? const Color(0xFF8C939D) : kAtelierMuted,
        outline: isDark ? const Color(0xFF444B56) : kAtelierOutline,
        shadow: isDark ? Colors.black : const Color(0xFF8DA3B8),
        radius: 24,
        cardElevation: 0,
      );
  }
}

ThemeData buildFootprintTheme({
  required Brightness brightness,
  String style = 'CLASSIC',
}) {
  final isDark = brightness == Brightness.dark;
  final palette = _paletteForStyle(style, isDark);
  final colorScheme = ColorScheme.fromSeed(
    seedColor: palette.seed,
    brightness: brightness,
  ).copyWith(
    primary: palette.primary,
    onPrimary: Colors.white,
    primaryContainer: Color.alphaBlend(palette.primary.withOpacity(isDark ? 0.26 : 0.14), palette.surface),
    onPrimaryContainer: isDark ? Colors.white : palette.primary,
    secondary: palette.secondary,
    onSecondary: Colors.white,
    secondaryContainer: Color.alphaBlend(palette.secondary.withOpacity(isDark ? 0.24 : 0.16), palette.surface),
    onSecondaryContainer: isDark ? Colors.white : palette.secondary,
    tertiary: palette.tertiary,
    onTertiary: Colors.white,
    tertiaryContainer: Color.alphaBlend(palette.tertiary.withOpacity(isDark ? 0.22 : 0.16), palette.surface),
    onTertiaryContainer: isDark ? Colors.white : palette.tertiary,
    surface: palette.surface,
    onSurface: palette.ink,
    surfaceContainerHighest: palette.surfaceHigh,
    outline: palette.muted,
    outlineVariant: palette.outline,
    error: const Color(0xFFC75050),
  );

  return ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: colorScheme,
    scaffoldBackgroundColor: palette.canvas,
    canvasColor: palette.canvas,
    appBarTheme: AppBarTheme(
      backgroundColor: Colors.transparent,
      foregroundColor: colorScheme.onSurface,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: false,
      titleTextStyle: TextStyle(
        color: colorScheme.onSurface,
        fontSize: 20,
        fontWeight: FontWeight.w900,
      ),
      iconTheme: IconThemeData(color: colorScheme.onSurface),
      systemOverlayStyle: isDark
          ? SystemUiOverlayStyle.light
          : SystemUiOverlayStyle.dark,
    ),
    cardTheme: CardThemeData(
      color: colorScheme.surface,
      elevation: palette.cardElevation,
      shadowColor: palette.shadow.withOpacity(isDark ? 0.35 : 0.16),
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(palette.radius),
        side: BorderSide(color: colorScheme.outlineVariant),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: colorScheme.surface,
      labelStyle: TextStyle(color: colorScheme.outline),
      hintStyle: TextStyle(color: colorScheme.outline.withOpacity(0.8)),
      contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 18),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular((palette.radius - 6).clamp(10, 18).toDouble()),
        borderSide: BorderSide(color: colorScheme.outlineVariant),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular((palette.radius - 6).clamp(10, 18).toDouble()),
        borderSide: BorderSide(color: colorScheme.outlineVariant),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular((palette.radius - 6).clamp(10, 18).toDouble()),
        borderSide: BorderSide(color: colorScheme.primary, width: 1.4),
      ),
    ),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: colorScheme.surface,
      contentTextStyle: TextStyle(color: colorScheme.onSurface),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      behavior: SnackBarBehavior.floating,
      elevation: 0,
    ),
    chipTheme: ChipThemeData(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
      side: BorderSide(color: colorScheme.outlineVariant),
      backgroundColor: colorScheme.surface,
      selectedColor: colorScheme.primaryContainer,
      labelStyle: TextStyle(color: colorScheme.onSurface, fontWeight: FontWeight.w700),
      secondaryLabelStyle: TextStyle(color: colorScheme.onPrimaryContainer, fontWeight: FontWeight.w700),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: colorScheme.primary,
        foregroundColor: colorScheme.onPrimary,
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular((palette.radius - 4).clamp(10, 18).toDouble())),
        textStyle: const TextStyle(fontWeight: FontWeight.w800),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: colorScheme.primary,
        side: BorderSide(color: colorScheme.outlineVariant),
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular((palette.radius - 4).clamp(10, 18).toDouble())),
        textStyle: const TextStyle(fontWeight: FontWeight.w800),
      ),
    ),
    floatingActionButtonTheme: FloatingActionButtonThemeData(
      backgroundColor: colorScheme.primary,
      foregroundColor: colorScheme.onPrimary,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular((palette.radius - 2).clamp(12, 22).toDouble())),
    ),
    dividerTheme: DividerThemeData(color: colorScheme.outlineVariant),
    popupMenuTheme: PopupMenuThemeData(
      color: colorScheme.surface,
      surfaceTintColor: Colors.transparent,
      textStyle: TextStyle(color: colorScheme.onSurface),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: colorScheme.outlineVariant),
      ),
    ),
  );
}

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
  final dynamic initialEntry;
  const AddFootprintPage({super.key, this.initialEntry});
  @override
  State<AddFootprintPage> createState() => _AddFootprintPageState();
}

class _AddFootprintPageState extends State<AddFootprintPage> {
  late TextEditingController _titleController;
  late TextEditingController _detailedLocationController;

  String _selectedRegion = "";
  late TextEditingController _detailController;
  late TextEditingController _distanceController;
  late TextEditingController _tagsController;
  late TextEditingController _tempController;
  late TextEditingController _altController;

  String selectedIcon = "LocationOn";
  double energyLevel = 6.0;
  String selectedMood = "愉快";
  String selectedWeather = "晴朗";
  String selectedTransport = "步行";
  DateTime selectedDate = DateTime.now();
  List<File> photos = [];

  final List<String> availableIcons = [
    "LocationOn", "Restaurant", "LocalCafe", "Park", "Flight",
    "Train", "DirectionsBike", "ShoppingBag", "CameraAlt",
  ];

  final List<String> weathers = ["晴朗", "多云", "阴天", "雨", "雪", "风", "雾"];
  final List<String> transports = ["步行", "骑行", "自驾", "铁路", "航空", "未知"];
  final List<String> moods = ["愉快", "平静", "兴奋", "疲惫", "失落", "惊喜", "激情", "探索", "放松", "思考"];

  @override
  void initState() {
    super.initState();
    final e = widget.initialEntry;
    _titleController = TextEditingController(text: e?['title'] ?? '');
    final locStr = e?['location'] as String? ?? '';
    final locParts = locStr.split(' ');
    if (locParts.length > 1) {
      _selectedRegion = locParts[0];
      _detailedLocationController = TextEditingController(text: locParts.sublist(1).join(' '));
    } else {
      _selectedRegion = "";
      _detailedLocationController = TextEditingController(text: locStr);
    }
    _detailController = TextEditingController(text: e?['detail'] ?? '');
    _distanceController = TextEditingController(text: e != null ? (e['distanceKm']?.toString() ?? '5.0') : "5.0");
    _tagsController = TextEditingController(text: (e?['tags'] as List<dynamic>?)?.join(', ') ?? '');
    _tempController = TextEditingController();
    _altController = TextEditingController();

    if (e != null) {
      if (e['icon'] != null && availableIcons.contains(e['icon'])) selectedIcon = e['icon'];
      if (e['energyLevel'] != null) energyLevel = (e['energyLevel'] as num).toDouble();
      if (e['mood'] != null) selectedMood = moods.contains(e['mood']) ? e['mood'] : "愉快";
      if (e['weather'] != null) selectedWeather = weathers.contains(e['weather']) ? e['weather'] : "晴朗";
      final t = e['transportType'] ?? e['transportMethod'];
      if (t != null) {
        switch(t) {
          case "WALK": selectedTransport = "步行"; break;
          case "BIKE": selectedTransport = "骑行"; break;
          case "CAR": selectedTransport = "自驾"; break;
          case "TRAIN": selectedTransport = "铁路"; break;
          case "PLANE": selectedTransport = "航空"; break;
          default: selectedTransport = transports.contains(t) ? t : "步行";
        }
      }
      
      final dateStr = e['happenedOn'] as String?;
      if (dateStr != null) {
        try { selectedDate = DateTime.parse(dateStr); } catch (_) {}
      }

      final ps = (e['photos'] ?? e['photoPaths']) as List<dynamic>?;
      if (ps != null) {
        photos.addAll(ps.where((p) => p != null).map((p) => File(p.toString())));
      }
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _detailedLocationController.dispose();
    _distanceController.dispose();
    _tagsController.dispose();
    _tempController.dispose();
    _altController.dispose();
    _detailController.dispose();
    super.dispose();
  }

  Future<void> _pickImage() async {
    try {
      // 1. 权限检查与申请
      if (Platform.isAndroid) {
        // 根据 Android 版本申请不同权限
        bool granted = false;
        // 如果是 Android 13 (API 33) 及以上，申请媒体图片权限
        if (await Permission.photos.isGranted || await Permission.photos.request().isGranted) {
          granted = true;
        } else if (await Permission.storage.isGranted || await Permission.storage.request().isGranted) {
          // 对于旧版本 Android，尝试申请存储权限
          granted = true;
        }
        
        if (!granted) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("需要存储权限才能读取照片")));
          }
          return;
        }
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("正在打开系统选择器..."), duration: Duration(milliseconds: 500))
        );
      }

      // 2. 使用 FilePicker 的 Custom 模式，这比 FileType.image 在 Android 上更稳健
      // 且能规避 ImagePicker 常见的 missing_valid_image_uri 平台错误
      FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['jpg', 'jpeg', 'png', 'webp', 'gif'],
        allowMultiple: true,
      );
      
      if (result != null && result.paths.isNotEmpty) {
        final List<File> validFiles = [];
        for (String? path in result.paths) {
          if (path != null) {
            final file = File(path);
            if (await file.exists()) {
              validFiles.add(file);
            }
          }
        }
        
        if (mounted && validFiles.isNotEmpty) {
          setState(() => photos.addAll(validFiles));
        }
      }
    } catch (e) {
      debugPrint("Photo Picker Error: $e");
      String errorMsg = e.toString();
      // 如果依然报错，可能是环境极端问题，提供更详细的提示
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text("选择照片失败: $errorMsg"),
            action: SnackBarAction(label: "重试", onPressed: _pickImage),
          )
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final overlayStyle = (isDark ? SystemUiOverlayStyle.light : SystemUiOverlayStyle.dark)
        .copyWith(statusBarColor: Theme.of(context).scaffoldBackgroundColor);
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: overlayStyle,
      child: Scaffold(
        backgroundColor: Theme.of(context).scaffoldBackgroundColor,
        appBar: AppBar(
          backgroundColor: Theme.of(context).scaffoldBackgroundColor,
          surfaceTintColor: Colors.transparent,
          elevation: 0,
          scrolledUnderElevation: 0,
          systemOverlayStyle: overlayStyle,
          title: Text(widget.initialEntry == null ? '记录新的足迹' : '编辑足迹记录', style: const TextStyle(fontWeight: FontWeight.w900)),
          leading: IconButton(
            icon: const Icon(Icons.close),
            onPressed: () => Navigator.pop(context),
          ),
        ),
        body: Stack(
          children: [
            Positioned.fill(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      isDark
                          ? Color.alphaBlend(cs.primary.withOpacity(0.06), Theme.of(context).scaffoldBackgroundColor)
                          : Color.alphaBlend(cs.primary.withOpacity(0.04), Colors.white),
                      Theme.of(context).scaffoldBackgroundColor,
                      Color.alphaBlend(cs.secondary.withOpacity(isDark ? 0.05 : 0.08), Theme.of(context).scaffoldBackgroundColor),
                    ],
                  ),
                ),
              ),
            ),
            Positioned(
              top: -80,
              right: -70,
              child: Container(
                width: 220,
                height: 220,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: const Color(0xFFDCEAF3).withOpacity(0.54),
                ),
              ),
            ),
            ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.88),
              borderRadius: BorderRadius.circular(28),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF10355B).withOpacity(0.08),
                  blurRadius: 22,
                  offset: const Offset(0, 12),
                ),
              ],
            ),
            child: Row(
              children: [
                Container(
                  width: 54,
                  height: 54,
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFF163A59), Color(0xFF3C6B8F)],
                    ),
                    borderRadius: BorderRadius.circular(18),
                  ),
                  child: const Icon(Icons.edit_location_alt_outlined, color: Colors.white),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.initialEntry == null ? '把这次抵达认真收藏' : '把这段记忆重新润色',
                        style: tt.titleMedium?.copyWith(
                          color: kAtelierInk,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        '地点、心情、照片和故事会一起沉淀成这次足迹的完整样子。',
                        style: tt.bodySmall?.copyWith(
                          color: kAtelierMuted,
                          height: 1.5,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          TextField(
            controller: _titleController,
            decoration: InputDecoration(
              labelText: '标题',
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            ),
          ),
          const SizedBox(height: 16),
          InkWell(
            onTap: () async {
              final picked = await showDatePicker(
                context: context,
                initialDate: selectedDate,
                firstDate: DateTime(2000),
                lastDate: DateTime.now(),
                locale: const Locale('zh'),
              );
              if (picked != null) setState(() => selectedDate = picked);
            },
            borderRadius: BorderRadius.circular(12),
            child: InputDecorator(
              decoration: InputDecoration(
                labelText: '记录日期',
                prefixIcon: const Icon(Icons.calendar_today_outlined),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: Text(
                "${selectedDate.year}-${selectedDate.month.toString().padLeft(2, '0')}-${selectedDate.day.toString().padLeft(2, '0')}",
                style: const TextStyle(fontSize: 16),
              ),
            ),
          ),
          const SizedBox(height: 16),
          // --- 省市区选择框 ---
          InkWell(
            onTap: () async {
              FocusScope.of(context).unfocus();
              Result? result = await CityPickers.showCityPicker(
                 context: context,
                 itemExtent: 45,
                     theme: ThemeData(
                         brightness: Theme.of(context).brightness,
                         primaryColor: cs.primary,
                         colorScheme: Theme.of(context).brightness == Brightness.dark 
                            ? ColorScheme.dark(primary: cs.primary, onPrimary: Colors.white, surface: const Color(0xFF121212))
                            : ColorScheme.light(primary: cs.primary, onPrimary: Colors.white),
                         textButtonTheme: TextButtonThemeData(
                           style: TextButton.styleFrom(
                             foregroundColor: cs.primary, 
                             textStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)
                           )
                         ),
                      ),
              );
              if (result != null) {
                final region = "${result.provinceName ?? ''} ${result.cityName ?? ''} ${result.areaName ?? ''}".trim();
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text("已确认地点: $region"),
                      duration: const Duration(seconds: 2),
                    )
                  );
                }
                setState(() {
                   _selectedRegion = region;
                });
              }
            },
            borderRadius: BorderRadius.circular(12),
            child: InputDecorator(
              decoration: InputDecoration(
                labelText: '地点/行政区划',
                prefixIcon: const Icon(Icons.map_outlined),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: Text(
                _selectedRegion.isEmpty ? "点击选择省/市/区" : _selectedRegion,
                style: TextStyle(
                  fontSize: 16,
                  color: _selectedRegion.isEmpty ? cs.outline : cs.onSurface,
                  overflow: TextOverflow.ellipsis,
                ),
                maxLines: 2,
              ),
            ),
          ),
          const SizedBox(height: 16),
          // --- 详细目的地输入框 ---
          TextField(
             controller: _detailedLocationController,
             decoration: InputDecoration(
               labelText: '详细地址 (如景点、餐馆、小区)',
               prefixIcon: const Icon(Icons.place_outlined),
               border: OutlineInputBorder(borderRadius: BorderRadius.circular(12))
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
                      ClipRRect(
                        borderRadius: BorderRadius.circular(12), 
                        child: Image.file(
                          f, 
                          width: 80, height: 80, fit: BoxFit.cover,
                          errorBuilder: (context, error, stackTrace) {
                            debugPrint("Error loading image ${f.path}: $error");
                            return Container(
                              width: 80, height: 80, color: Colors.red,
                              child: const Icon(Icons.error, color: Colors.white),
                            );
                          }
                        )
                      ),
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
            onPressed: () async {
              // Map UI mood to Kotlin/Internal Mood
              String mappedMood = "RELAXED";
              if (selectedMood == "激情" || selectedMood == "兴奋" || selectedMood == "惊喜") mappedMood = "EXCITED";
              else if (selectedMood == "探索") mappedMood = "CURIOUS";
              else if (selectedMood == "思考" || selectedMood == "疲惫" || selectedMood == "失落") mappedMood = "REFLECTIVE";
              else mappedMood = "RELAXED";

              // Map Transport
              String mappedTransport = "UNKNOWN";
              switch(selectedTransport) {
                case "步行": mappedTransport = "WALK"; break;
                case "骑行": mappedTransport = "BIKE"; break;
                case "自驾": mappedTransport = "CAR"; break;
                case "铁路": mappedTransport = "TRAIN"; break;
                case "航空": mappedTransport = "PLANE"; break;
              }

              final entryData = {
                "id": widget.initialEntry?['id'] ?? 0,
                "title": _titleController.text,
                "location": "${_selectedRegion} ${_detailedLocationController.text}".trim(),
                "detail": _detailController.text,
                "mood": mappedMood,
                "tags": _tagsController.text.split(',').map((e) => e.trim()).where((e) => e.isNotEmpty).toList(),
                "distanceKm": double.tryParse(_distanceController.text) ?? 5.0,
                "photos": photos.map((e) => e.path).toList(),
                "photoPaths": photos.map((e) => e.path).toList(), // For compatibility
                "energyLevel": energyLevel.toInt(),
                "happenedOn": "${selectedDate.year}-${selectedDate.month.toString().padLeft(2, '0')}-${selectedDate.day.toString().padLeft(2, '0')}",
                "icon": selectedIcon,
                "weather": selectedWeather,
                "temperature": double.tryParse(_tempController.text),
                "altitude": double.tryParse(_altController.text),
                "transportType": mappedTransport,
                "latitude": widget.initialEntry?['latitude'],
                "longitude": widget.initialEntry?['longitude'],
              };

              try {
                await const MethodChannel('com.footprint/data').invokeMethod('saveFootprint', jsonEncode(entryData));
                if (mounted) Navigator.pop(context, true);
              } catch (e) {
                if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("保存失败: $e")));
              }
            },
            style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(56), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16))),
            child: Text(widget.initialEntry == null ? "记录足迹" : "保存修改", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          ),
        ],
            ),
          ],
        ),
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

class AddGoalPage extends StatefulWidget {
  final dynamic initialGoal;
  const AddGoalPage({super.key, this.initialGoal});

  @override
  State<AddGoalPage> createState() => _AddGoalPageState();
}

class _AddGoalPageState extends State<AddGoalPage> {
  late TextEditingController _titleController;
  late TextEditingController _detailedLocationController;
  String _selectedRegion = "";
  late TextEditingController _notesController;
  DateTime _selectedDate = DateTime.now();

  @override
  void initState() {
    super.initState();
    final g = widget.initialGoal;
    _titleController = TextEditingController(text: g?['title'] ?? '');
    final locStr = g?['targetLocation'] as String? ?? '';
    final locParts = locStr.split(' ');
    if (locParts.length > 1) {
      _selectedRegion = locParts[0];
      _detailedLocationController = TextEditingController(text: locParts.sublist(1).join(' '));
    } else {
      _selectedRegion = "";
      _detailedLocationController = TextEditingController(text: locStr);
    }
    _notesController = TextEditingController(text: g?['notes'] ?? '');
    if (g?['targetDate'] != null) {
      try { _selectedDate = DateTime.parse(g['targetDate']); } catch(_) {}
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _detailedLocationController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final overlayStyle = (isDark ? SystemUiOverlayStyle.light : SystemUiOverlayStyle.dark)
        .copyWith(statusBarColor: Theme.of(context).scaffoldBackgroundColor);
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: overlayStyle,
      child: Scaffold(
        backgroundColor: Theme.of(context).scaffoldBackgroundColor,
        appBar: AppBar(
          backgroundColor: Theme.of(context).scaffoldBackgroundColor,
          surfaceTintColor: Colors.transparent,
          elevation: 0,
          scrolledUnderElevation: 0,
          systemOverlayStyle: overlayStyle,
          title: Text(widget.initialGoal == null ? '新增旅行目标' : '编辑旅行目标', style: const TextStyle(fontWeight: FontWeight.w900)),
          leading: IconButton(
            icon: const Icon(Icons.close),
            onPressed: () => Navigator.pop(context),
          ),
        ),
        body: Stack(
          children: [
            Positioned.fill(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      isDark
                          ? Color.alphaBlend(cs.primary.withOpacity(0.06), Theme.of(context).scaffoldBackgroundColor)
                          : Color.alphaBlend(cs.primary.withOpacity(0.04), Colors.white),
                      Theme.of(context).scaffoldBackgroundColor,
                      Color.alphaBlend(cs.secondary.withOpacity(isDark ? 0.05 : 0.08), Theme.of(context).scaffoldBackgroundColor),
                    ],
                  ),
                ),
              ),
            ),
            Positioned(
              top: -70,
              left: -50,
              child: Container(
                width: 220,
                height: 220,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: const Color(0xFFF5D6BF).withOpacity(0.4),
                ),
              ),
            ),
            ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.9),
              borderRadius: BorderRadius.circular(28),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF10355B).withOpacity(0.08),
                  blurRadius: 22,
                  offset: const Offset(0, 12),
                ),
              ],
            ),
            child: Row(
              children: [
                Container(
                  width: 54,
                  height: 54,
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFFD47A53), Color(0xFFE7A27E)],
                    ),
                    borderRadius: BorderRadius.circular(18),
                  ),
                  child: const Icon(Icons.flag_outlined, color: Colors.white),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.initialGoal == null ? '给下一次远行一个方向' : '继续打磨这段计划',
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: kAtelierInk,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        '把想去的地方、时间和注记写清楚，之后回看会更像一份真正的旅程提案。',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: kAtelierMuted,
                          height: 1.5,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          TextField(
            controller: _titleController,
            decoration: InputDecoration(
              labelText: '目标名称',
              hintText: '例如：川西环线摄影',
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            ),
          ),
          const SizedBox(height: 16),
          const SizedBox(height: 16),
          const SizedBox(height: 16),
          // --- 省市区选择框 ---
          InkWell(
            onTap: () async {
              FocusScope.of(context).unfocus();
              Result? result = await CityPickers.showCityPicker(
                 context: context,
                 itemExtent: 45,
                     theme: ThemeData(
                         brightness: Theme.of(context).brightness,
                         primaryColor: cs.primary,
                         colorScheme: Theme.of(context).brightness == Brightness.dark 
                            ? ColorScheme.dark(primary: cs.primary, onPrimary: Colors.white, surface: const Color(0xFF121212))
                            : ColorScheme.light(primary: cs.primary, onPrimary: Colors.white),
                         textButtonTheme: TextButtonThemeData(
                           style: TextButton.styleFrom(
                             foregroundColor: cs.primary, 
                             textStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)
                           )
                         ),
                      ),
              );
              if (result != null) {
                final region = "${result.provinceName ?? ''} ${result.cityName ?? ''} ${result.areaName ?? ''}".trim();
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text("已确认地点: $region"),
                      duration: const Duration(seconds: 2),
                    )
                  );
                }
                setState(() {
                   _selectedRegion = region;
                });
              }
            },
            borderRadius: BorderRadius.circular(12),
            child: InputDecorator(
              decoration: InputDecoration(
                labelText: '地点/行政区划',
                prefixIcon: const Icon(Icons.map_outlined),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: Text(
                _selectedRegion.isEmpty ? "点击选择省/市/区" : _selectedRegion,
                style: TextStyle(
                  fontSize: 16,
                  color: _selectedRegion.isEmpty ? cs.outline : cs.onSurface,
                  overflow: TextOverflow.ellipsis,
                ),
                maxLines: 2,
              ),
            ),
          ),
          const SizedBox(height: 16),
          // --- 详细目的地输入框 ---
          TextField(
             controller: _detailedLocationController,
             decoration: InputDecoration(
               labelText: '详细目的地 (如景区或具体位置)',
               prefixIcon: const Icon(Icons.place_outlined),
               border: OutlineInputBorder(borderRadius: BorderRadius.circular(12))
             ),
          ),
          const SizedBox(height: 16),
          InkWell(
            onTap: () async {
              final picked = await showDatePicker(
                context: context,
                initialDate: _selectedDate,
                firstDate: DateTime(2000),
                lastDate: DateTime(2030),
                locale: const Locale('zh'),
              );
              if (picked != null) setState(() => _selectedDate = picked);
            },
            borderRadius: BorderRadius.circular(12),
            child: InputDecorator(
              decoration: InputDecoration(
                labelText: '预期日期',
                prefixIcon: const Icon(Icons.calendar_today_outlined),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: Text(
                "${_selectedDate.year}/${_selectedDate.month.toString().padLeft(2, '0')}/${_selectedDate.day.toString().padLeft(2, '0')}",
                style: const TextStyle(fontSize: 16),
              ),
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _notesController,
            maxLines: 3,
            decoration: InputDecoration(
              labelText: '计划备注',
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            ),
          ),
          const SizedBox(height: 32),
          FilledButton(
            onPressed: () async {
              final goalData = {
                "id": widget.initialGoal?['id'] ?? 0,
                "title": _titleController.text,
                "targetLocation": "${_selectedRegion} ${_detailedLocationController.text}".trim(),
                "targetDate": "${_selectedDate.year}-${_selectedDate.month.toString().padLeft(2, '0')}-${_selectedDate.day.toString().padLeft(2, '0')}",
                "notes": _notesController.text,
                "isCompleted": widget.initialGoal?['isCompleted'] ?? false,
                "progress": widget.initialGoal?['progress'] ?? 0,
                "icon": widget.initialGoal?['icon'] ?? "Flag",
              };

              try {
                await const MethodChannel('com.footprint/data').invokeMethod('saveGoal', jsonEncode(goalData));
                if (mounted) {
                  Navigator.pop(context, true);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(widget.initialGoal == null ? "目标已添加" : "修改已保存")),
                  );
                }
              } catch (e) {
                if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("保存失败: $e")));
              }
            },
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(56),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            ),
            child: Text(widget.initialGoal == null ? "保存目标" : "保存修改", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          ),
        ],
            ),
          ],
        ),
      ),
    );
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
  bool isMaintValid = false;
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
          isMaintValid = data['isMaintValid'] ?? false;
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

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      themeMode: _getThemeMode(),
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [
        Locale('zh', 'CN'),
        Locale('en', 'US'),
      ],
      locale: const Locale('zh', 'CN'),
      theme: buildFootprintTheme(
        brightness: Brightness.light,
        style: themeStyleStr,
      ),
      darkTheme: buildFootprintTheme(
        brightness: Brightness.dark,
        style: themeStyleStr,
      ),
      builder: (context, child) {
        final isDark = Theme.of(context).brightness == Brightness.dark;
        final overlayStyle = (isDark ? SystemUiOverlayStyle.light : SystemUiOverlayStyle.dark)
            .copyWith(
          statusBarColor: Colors.transparent,
          systemNavigationBarColor: Colors.transparent,
          systemNavigationBarIconBrightness:
              isDark ? Brightness.light : Brightness.dark,
        );
        return AnnotatedRegion<SystemUiOverlayStyle>(
          value: overlayStyle,
          child: child ?? const SizedBox.shrink(),
        );
      },
      home: MainContainer(
        nickname: nickname,
        avatarId: avatarId,
        themeMode: themeModeStr,
        themeStyle: themeStyleStr,
        hapticEnabled: hapticEnabled,
        isMaintValid: isMaintValid,
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
  final bool isMaintValid;
  final VoidCallback onSettingsChanged;
  const MainContainer({
    super.key,
    required this.nickname,
    required this.avatarId,
    required this.themeMode,
    required this.themeStyle,
    required this.hapticEnabled,
    required this.isMaintValid,
    required this.onSettingsChanged,
  });
  @override
  State<MainContainer> createState() => _MainContainerState();
}

class _MainContainerState extends State<MainContainer>
    with TickerProviderStateMixin {
  static const List<String> _tabLabels = <String>[
    '总览',
    '探索',
    '计划',
    '工坊',
  ];
  static const List<IconData> _selectedTabIcons = <IconData>[
    Icons.dashboard,
    Icons.explore,
    Icons.flag,
    Icons.palette,
  ];
  static const List<IconData> _unselectedTabIcons = <IconData>[
    Icons.dashboard_outlined,
    Icons.explore_outlined,
    Icons.outlined_flag,
    Icons.palette_outlined,
  ];
  int _selectedIndex = 0;
  late AnimationController _navController;
  late Animation<double> _elasticAnimation;
  bool _isHiding = false;

  final EventChannel _badgeChannel = const EventChannel('com.footprint/badge_events');
  StreamSubscription? _badgeSub;
  final List<dynamic> _badgeQueue = [];
  bool _isShowingBadge = false;

  // GlobalKey 用于通知地图页面 tab 切换事件
  final GlobalKey<_ExploreMapScreenState> _mapKey = GlobalKey<_ExploreMapScreenState>();

  // Tab 切换弹性动画
  late AnimationController _tabBounceController;
  late Animation<double> _tabBounceAnimation;

  @override
  void initState() {
    super.initState();
    _navController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    _elasticAnimation = CurvedAnimation(
      parent: _navController,
      curve: const ElasticOutCurve(0.8),
      reverseCurve: Curves.easeInCubic,
    );
    _navController.value = 1.0;

    _tabBounceController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    _tabBounceAnimation = TweenSequence<double>([
      TweenSequenceItem(tween: Tween(begin: 1.0, end: 0.85).chain(CurveTween(curve: Curves.easeOut)), weight: 20),
      TweenSequenceItem(tween: Tween(begin: 0.85, end: 1.12).chain(CurveTween(curve: Curves.easeOut)), weight: 30),
      TweenSequenceItem(tween: Tween(begin: 1.12, end: 0.95).chain(CurveTween(curve: Curves.easeInOut)), weight: 25),
      TweenSequenceItem(tween: Tween(begin: 0.95, end: 1.0).chain(CurveTween(curve: Curves.easeInOut)), weight: 25),
    ]).animate(_tabBounceController);

    _badgeSub = _badgeChannel.receiveBroadcastStream().listen((event) {
      if (event != null) {
        try {
          final badgeData = jsonDecode(event.toString());
          _badgeQueue.add(badgeData);
          _processBadgeQueue();
        } catch (e) {
          debugPrint("Failed to parse badge: $e");
        }
      }
    });
  }

  void _processBadgeQueue() async {
    if (_isShowingBadge || _badgeQueue.isEmpty) return;
    _isShowingBadge = true;
    final badge = _badgeQueue.removeAt(0);

    // Show capsule using Overlay
    if (mounted) {
      _showBadgeCapsule(badge);
    }

    await Future.delayed(const Duration(seconds: 4));
    _isShowingBadge = false;
    _processBadgeQueue();
  }

  void _showBadgeCapsule(dynamic badge) {
    if (!mounted) return;
    OverlayState? overlayState = Overlay.of(context);
    late OverlayEntry overlayEntry;

    overlayEntry = OverlayEntry(
      builder: (context) {
        return Positioned(
          top: MediaQuery.of(context).padding.top + 16,
          left: 16,
          right: 16,
          child: TweenAnimationBuilder<double>(
            duration: const Duration(milliseconds: 600),
            tween: Tween(begin: -100.0, end: 0.0),
            curve: Curves.elasticOut,
            builder: (context, value, child) {
              return Transform.translate(
                offset: Offset(0, value),
                child: child,
              );
            },
            child: Material(
              color: Colors.transparent,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(24),
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
                  child: Container(
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surface.withValues(alpha: 0.85),
                      borderRadius: BorderRadius.circular(24),
                      border: Border.all(color: Colors.white.withValues(alpha: 0.2)),
                      boxShadow: [
                        BoxShadow(
                          color: Theme.of(context).colorScheme.shadow.withValues(alpha: 0.1),
                          blurRadius: 10,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                    child: Row(
                      children: [
                        const Text("🏆", style: TextStyle(fontSize: 28)),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text("边界突破！解锁 [${badge['title']}]", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                              Text(badge['description'] ?? '', style: TextStyle(fontSize: 12, color: Theme.of(context).colorScheme.onSurfaceVariant), maxLines: 1, overflow: TextOverflow.ellipsis),
                            ],
                          ),
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

    overlayState.insert(overlayEntry);

    Future.delayed(const Duration(milliseconds: 3500), () {
      if (overlayEntry.mounted) {
        overlayEntry.remove();
      }
    });
  }

  void _handleScroll(ScrollNotification notification) {
    if (notification is ScrollUpdateNotification) {
      final delta = notification.scrollDelta;
      if (delta == null || delta == 0) return;
      if (delta > 8.0 && !_isHiding && _navController.status != AnimationStatus.reverse) {
        _isHiding = true;
        _navController.reverse();
      } else if (delta < -8.0 && _isHiding && _navController.status != AnimationStatus.forward) {
        _isHiding = false;
        _navController.forward();
      }
    }
  }

  void _onTabTap(int index) {
    if (_selectedIndex == index) return;
    final prevIndex = _selectedIndex;
    setState(() => _selectedIndex = index);
    _tabBounceController.forward(from: 0.0);
    if (widget.hapticEnabled) HapticFeedback.lightImpact();

    // 离开地图页时通知停止后台定位（除非正在追踪）
    if (prevIndex == 1 && index != 1) {
      _mapKey.currentState?.onTabDeselected();
    }
    // 进入地图页时通知恢复
    if (index == 1 && prevIndex != 1) {
      _mapKey.currentState?.onTabActivated();
    }
  }

  @override
  void dispose() {
    _badgeSub?.cancel();
    _navController.dispose();
    _tabBounceController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      DashboardScreen(
        nickname: widget.nickname,
        avatarId: widget.avatarId,
        themeMode: widget.themeMode,
        themeStyle: widget.themeStyle,
        hapticEnabled: widget.hapticEnabled,
        isMaintValid: widget.isMaintValid,
        onSettingsChanged: widget.onSettingsChanged,
      ),
      ExploreMapScreen(key: _mapKey, themeMode: widget.themeMode),
      const GoalPlannerPage(),
      ArtStudioScreen(
        nickname: widget.nickname,
        isMaintValid: widget.isMaintValid,
      ),
    ];

    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      extendBody: true,
      body: Stack(
        children: [
          Positioned.fill(
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    isDark
                        ? Color.alphaBlend(cs.primary.withOpacity(0.06), Theme.of(context).scaffoldBackgroundColor)
                        : Color.alphaBlend(cs.primary.withOpacity(0.04), Colors.white),
                    Theme.of(context).scaffoldBackgroundColor,
                    Color.alphaBlend(cs.secondary.withOpacity(isDark ? 0.05 : 0.08), Theme.of(context).scaffoldBackgroundColor),
                  ],
                ),
              ),
            ),
          ),
          Positioned(
            top: -110,
            left: -70,
            child: Container(
              width: 260,
              height: 260,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: (isDark ? cs.secondaryContainer : const Color(0xFFF8D8BE))
                    .withOpacity(isDark ? 0.12 : 0.42),
              ),
            ),
          ),
          Positioned(
            top: 110,
            right: -90,
            child: Container(
              width: 240,
              height: 240,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: (isDark ? cs.primaryContainer : const Color(0xFFDCEBF3))
                    .withOpacity(isDark ? 0.16 : 0.58),
              ),
            ),
          ),
          NotificationListener<ScrollNotification>(
            onNotification: (notification) { _handleScroll(notification); return false; },
            child: IndexedStack(index: _selectedIndex, children: pages),
          ),
        ],
      ),
      bottomNavigationBar: AnimatedBuilder(
        animation: Listenable.merge([_elasticAnimation, _tabBounceAnimation]),
        builder: (context, child) {
          final rawT = _elasticAnimation.value;
          final clampedT = rawT.clamp(0.0, 1.0);
          final fullWidth = MediaQuery.of(context).size.width - 64;
          const dropSize = 64.0;
          final currentWidth = math.max(dropSize, dropSize + (fullWidth - dropSize) * rawT);
          final overallScale = clampedT < 0.15 ? (clampedT / 0.15) : 1.0;
          final cs = Theme.of(context).colorScheme;

          return Align(
            alignment: Alignment.bottomCenter,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 32),
              child: Transform.scale(
                scale: overallScale,
                child: Container(
                  width: currentWidth, height: dropSize,
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: [
                        cs.surface.withValues(alpha: 0.98),
                        Color.alphaBlend(
                          (isDark ? cs.surfaceContainerHighest : cs.surfaceContainerHighest)
                              .withOpacity(0.46),
                          cs.surface,
                        ),
                      ],
                    ),
                    borderRadius: BorderRadius.circular(dropSize / 2),
                    boxShadow: [
                      BoxShadow(
                        color: (isDark ? Colors.black : const Color(0xFF8DA3B8))
                            .withValues(alpha: (isDark ? 0.28 : 0.12) * clampedT),
                        blurRadius: 26 * clampedT,
                        offset: Offset(0, 14 * clampedT),
                      ),
                      BoxShadow(
                        color: (isDark ? Colors.white : Colors.white)
                            .withValues(alpha: (isDark ? 0.08 : 0.65) * clampedT),
                        blurRadius: 16 * clampedT,
                        offset: Offset(0, -4 * clampedT),
                      ),
                    ],
                    border: Border.all(color: cs.outlineVariant.withValues(alpha: 0.65 * clampedT)),
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(dropSize / 2),
                    child: OverflowBox(
                      minWidth: fullWidth, maxWidth: fullWidth, minHeight: dropSize, maxHeight: dropSize,
                      child: Stack(
                        alignment: Alignment.center,
                        children: List.generate(4, (index) {
                          final double step = fullWidth / 4;
                          final double currentX = (index - 1.5) * step * clampedT;
                          final bool isSelected = _selectedIndex == index;
                          // 当前选中 tab 的弹性缩放
                          final double iconScale = isSelected ? _tabBounceAnimation.value : 1.0;

                          return Positioned(
                            left: (fullWidth / 2) + currentX - 24,
                            child: Opacity(
                              opacity: (clampedT * 5 - 4).clamp(0.0, 1.0),
                              child: GestureDetector(
                                onTap: () => _onTabTap(index),
                                child: Transform.scale(
                                  scale: iconScale,
                                  child: Container(
                                    width: 58,
                                    height: 58,
                                    decoration: BoxDecoration(
                                      borderRadius: BorderRadius.circular(24),
                                      gradient: isSelected
                                          ? LinearGradient(
                                              begin: Alignment.topLeft,
                                              end: Alignment.bottomRight,
                                              colors: [
                                                cs.primary,
                                                Color.alphaBlend(
                                                  cs.secondary
                                                      .withOpacity(0.3),
                                                  cs.primary,
                                                ),
                                              ],
                                            )
                                          : null,
                                      color: isSelected ? null : Colors.transparent,
                                    ),
                                    child: Column(
                                      mainAxisAlignment: MainAxisAlignment.center,
                                      children: [
                                        Icon(
                                          isSelected ? _selectedTabIcons[index] : _unselectedTabIcons[index],
                                          color: isSelected ? cs.onPrimary : cs.onSurfaceVariant,
                                          size: 22,
                                        ),
                                        const SizedBox(height: 2),
                                        Text(
                                          _tabLabels[index],
                                          style: TextStyle(
                                            color: isSelected ? cs.onPrimary : cs.onSurfaceVariant,
                                            fontSize: 10,
                                            fontWeight: FontWeight.w800,
                                            letterSpacing: 0.2,
                                          ),
                                        ),
                                      ],
                                    ),
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
  final String themeMode;
  final String themeStyle;
  final bool hapticEnabled;
  final bool isMaintValid;
  final VoidCallback onSettingsChanged;

  const DashboardScreen({
    super.key,
    required this.nickname,
    required this.avatarId,
    required this.themeMode,
    required this.themeStyle,
    required this.hapticEnabled,
    required this.isMaintValid,
    required this.onSettingsChanged,
  });
  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _TimelineMonth {
  final int month;
  final List<dynamic> entries;

  const _TimelineMonth({
    required this.month,
    required this.entries,
  });
}

class _DashboardScreenState extends State<DashboardScreen> {
  int currentYear = DateTime.now().year;
  List<dynamic> allEntries = [];
  List<dynamic> allTimeEntries = [];
  List<dynamic> onThisDayEntries = [];
  List<dynamic> yearlyGoals = [];
  List<dynamic> _searchResults = const [];
  List<_TimelineMonth> _timelineMonths = const [];
  Map<String, Color> _heatmapDateColors = const {};
  double totalDistance = 0.0;
  int uniquePlacesLength = 0;
  double avgEnergy = 0.0;
  Set<int> _expandedMonths = {};
  String _searchQuery = "";
  final TextEditingController _searchController = TextEditingController();

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

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
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

  Color _moodColor(String mood, ColorScheme cs) {
    switch (_mapMoodToChinese(mood)) {
      case "激情":
        return Colors.orange;
      case "探索":
        return Colors.teal;
      case "放松":
        return Colors.blue;
      case "思考":
        return Colors.purple;
      case "愉快":
        return Colors.amber;
      case "平静":
        return Colors.lightBlue;
      default:
        return cs.primary;
    }
  }

  List<dynamic> _buildSearchResults(List<dynamic> entries, String query) {
    if (query.isEmpty) return const [];
    return entries.where((e) {
      final loc = (e['location'] as String? ?? '').toLowerCase();
      final title = (e['title'] as String? ?? '').toLowerCase();
      final tags = ((e['tags'] as List?)?.join(' ') ?? '').toLowerCase();
      return loc.contains(query) || title.contains(query) || tags.contains(query);
    }).toList(growable: false);
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
        if (!mounted) return;
        final List<dynamic> entries = jsonDecode(jsonStr);
        final today = DateTime.now();
        List<dynamic> onThisDay = [];
        final colorScheme = Theme.of(context).colorScheme;

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
        
        // Sort chronologically (newest first, then highest ID first if on same day)
        yearEntries.sort((a,b) {
          int cmp = (b['happenedOn'] ?? '').compareTo(a['happenedOn'] ?? '');
          if (cmp != 0) return cmp;
          return (b['id'] as int? ?? 0).compareTo(a['id'] as int? ?? 0);
        });

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

        final heatmapDateColors = <String, Color>{};
        for (final entry in yearEntries) {
          final dateStr = entry['happenedOn'] as String?;
          final moodStr = entry['mood'] as String?;
          if (dateStr != null && dateStr.length >= 10 && moodStr != null) {
            heatmapDateColors[dateStr.substring(0, 10)] = _moodColor(moodStr, colorScheme);
          }
        }

        final groupedByMonth = <int, List<dynamic>>{};
        for (final entry in yearEntries) {
          final dateStr = entry['happenedOn'] as String?;
          if (dateStr != null && dateStr.length >= 7) {
            final month = int.tryParse(dateStr.split('-')[1]) ?? 1;
            groupedByMonth.putIfAbsent(month, () => []).add(entry);
          }
        }
        final timelineMonths =
            (groupedByMonth.keys.toList()..sort((a, b) => b.compareTo(a)))
                .map((month) => _TimelineMonth(month: month, entries: groupedByMonth[month]!))
                .toList(growable: false);

        final searchResults = _buildSearchResults(entries, _searchQuery);

        setState(() {
          allTimeEntries = entries;
          allEntries = yearEntries;
          onThisDayEntries = onThisDay;
          yearlyGoals = yGoals;
          _heatmapDateColors = heatmapDateColors;
          _timelineMonths = timelineMonths;
          _searchResults = searchResults;
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

  Future<void> _showDetail(dynamic entryData) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => FootprintDetailPage(entry: entryData)),
    );
    if (result == true) {
      _loadEntries();
    }
  }

  void _showStatDetail(BuildContext context, String title, String type, List<dynamic> entriesToList) {
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
                     child: entriesToList.isEmpty
                       ? Center(child: Text("暂无记录", style: TextStyle(color: cs.outline)))
                       : ListView.builder(
                       controller: controller,
                       itemCount: entriesToList.length,
                       itemBuilder: (context, index) {
                         final entry = entriesToList[index];
                         final eDate = entry['happenedOn'] ?? '';
                         switch (type) {
                           case 'distance':
                             final dist = (entry['distanceKm'] as num?)?.toDouble() ?? 0.0;
                             return ListTile(
                               leading: Icon(Icons.straighten, color: cs.primary),
                               title: Text(eDate),
                               subtitle: Text(entry['title'] ?? '未知足迹'),
                               trailing: Text("${dist.toStringAsFixed(3)} km", style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold, fontSize: 12)),
                               onTap: () { Navigator.pop(context); _showDetail(entry); },
                             );
                           case 'location':
                             return ListTile(
                               leading: Icon(Icons.location_on, color: cs.primary),
                               title: Text(eDate),
                               subtitle: Builder(builder: (context) {
                                 String loc = entry['location'] ?? '未知地点';
                                 final coordRegex = RegExp(r'(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)');
                                 final match = coordRegex.firstMatch(loc);
                                 if (match != null) {
                                   try {
                                     double lat = double.parse(match.group(1)!);
                                     double lng = double.parse(match.group(2)!);
                                     loc = "${lat.toStringAsFixed(3)}, ${lng.toStringAsFixed(3)}";
                                   } catch (_) {}
                                 }
                                 return Text(loc, maxLines: 2, overflow: TextOverflow.ellipsis);
                               }),
                               onTap: () { Navigator.pop(context); _showDetail(entry); },
                             );
                           case 'energy':
                             final energy = (entry['energyLevel'] as num?)?.toInt() ?? 0;
                             return ListTile(
                               leading: Icon(Icons.bolt, color: Colors.amber),
                               title: Text(eDate),
                               subtitle: Text(entry['title'] ?? '未知足迹'),
                               trailing: Row(mainAxisSize: MainAxisSize.min, children: List.generate(5, (i) => Icon(i < energy ? Icons.star : Icons.star_border, size: 14, color: i < energy ? Colors.amber : cs.outline))),
                               onTap: () { Navigator.pop(context); _showDetail(entry); },
                             );
                           case 'mood':
                             final mood = _mapMoodToChinese(entry['mood'] ?? '');
                             return ListTile(
                               leading: Icon(Icons.mood, color: cs.tertiary),
                               title: Text(entry['title'] ?? '未知足迹'),
                               subtitle: Text(eDate),
                               trailing: Container(
                                 padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                 decoration: BoxDecoration(color: cs.tertiaryContainer, borderRadius: BorderRadius.circular(8)),
                                 child: Text(mood, style: TextStyle(color: cs.onTertiaryContainer, fontWeight: FontWeight.bold, fontSize: 12)),
                               ),
                               onTap: () { Navigator.pop(context); _showDetail(entry); },
                             );
                           default:
                             return ListTile(
                               leading: Icon(Icons.place, color: cs.primary),
                               title: Text(entry['title'] ?? '未知足迹'),
                               subtitle: Text("$eDate · ${entry['location'] ?? '未知地点'}"),
                               trailing: Text("${(entry['distanceKm'] as num?)?.toDouble().toStringAsFixed(3) ?? '0'} km", style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold)),
                               onTap: () { Navigator.pop(context); _showDetail(entry); },
                             );
                         }
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
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final todayHint = getEternalTodayBannerText();
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Stack(
        children: [
          Positioned.fill(
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    isDark
                        ? Color.alphaBlend(cs.primary.withOpacity(0.06), Theme.of(context).scaffoldBackgroundColor)
                        : Color.alphaBlend(cs.primary.withOpacity(0.04), Colors.white),
                    Theme.of(context).scaffoldBackgroundColor,
                    Color.alphaBlend(cs.secondary.withOpacity(isDark ? 0.05 : 0.08), Theme.of(context).scaffoldBackgroundColor),
                  ],
                ),
              ),
            ),
          ),
          Positioned(
            top: -90,
            left: -60,
            child: Container(
              width: 220,
              height: 220,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: (isDark ? cs.secondaryContainer : cs.secondaryContainer)
                    .withOpacity(isDark ? 0.12 : 0.38),
              ),
            ),
          ),
          Positioned(
            top: 120,
            right: -80,
            child: Container(
              width: 200,
              height: 200,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: (isDark ? cs.primaryContainer : cs.primaryContainer)
                    .withOpacity(isDark ? 0.16 : 0.5),
              ),
            ),
          ),
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
                      _sBox(cs, tt, "足迹", "${allEntries.length}", onTap: () => _showStatDetail(context, "所有足迹 (${allEntries.length})", "footprint", allEntries)),
                      const SizedBox(width: 8),
                      _sBox(cs, tt, "里程", totalDistance.toStringAsFixed(1), u: "km", onTap: () {
                        final sorted = List.of(allEntries)..sort((a,b) => ((b['distanceKm'] as num?)?.toDouble() ?? 0.0).compareTo((a['distanceKm'] as num?)?.toDouble() ?? 0.0));
                        _showStatDetail(context, "年度总里程 (${totalDistance.toStringAsFixed(1)} km)", "distance", sorted);
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
                        _showStatDetail(context, "探索地点 ($uniquePlacesLength)", "location", unique);
                      }),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      _sBox(cs, tt, "记录", "${allEntries.length}", onTap: () => _showStatDetail(context, "记录条数 (${allEntries.length})", "footprint", allEntries)),
                      const SizedBox(width: 8),
                      _sBox(cs, tt, "活力", avgEnergy.toStringAsFixed(1), u: "指数", onTap: () {
                        final sorted = List.of(allEntries)..sort((a,b) => ((b['energyLevel'] as num?)?.toDouble() ?? 0.0).compareTo((a['energyLevel'] as num?)?.toDouble() ?? 0.0));
                        _showStatDetail(context, "平均活力指数 (${avgEnergy.toStringAsFixed(1)})", "energy", sorted);
                      }),
                      const SizedBox(width: 8),
                      _sBox(cs, tt, "主情绪", dominantMoodStr, onTap: () {
                         final filtered = allEntries.where((e) => e['mood'] == dominantMoodStr).toList();
                         _showStatDetail(context, "主导心情 ($dominantMoodStr)", "mood", filtered);
                      }),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16), side: BorderSide(color: cs.outlineVariant)),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Row(children: [
                      Icon(Icons.grid_view, color: cs.primary, size: 16),
                      const SizedBox(width: 6),
                      Text("情绪热力图", style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold, fontSize: 13)),
                      const Spacer(),
                      Text("$currentYear", style: TextStyle(color: cs.outline, fontSize: 11)),
                    ]),
                    const SizedBox(height: 8),
                    _buildMoodHeatmap(cs),
                    const SizedBox(height: 6),
                    Wrap(spacing: 10, runSpacing: 4, children: [
                      _heatLegend("激情", Colors.orange, cs), _heatLegend("探索", Colors.teal, cs),
                      _heatLegend("放松", Colors.blue, cs), _heatLegend("思考", Colors.purple, cs),
                      _heatLegend("愉快", Colors.amber, cs), _heatLegend("平静", Colors.lightBlue, cs),
                    ]),
                  ]),
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
                            "$currentYear 年度旅行目标",
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
                              Navigator.push(context, MaterialPageRoute(builder: (_) => const GoalPlannerPage()))
                                .then((_) => _loadEntries());
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
                  InkWell(
                    onTap: () {
                      if (widget.hapticEnabled) HapticFeedback.mediumImpact();
                      Navigator.push(
                        context,
                        MaterialPageRoute(builder: (context) => const AddFootprintPage()),
                      ).then((_) => widget.onSettingsChanged());
                    },
                    borderRadius: BorderRadius.circular(12),
                    child: Container(
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
                  ),
                ],
              ),
              const SizedBox(height: 16),
              ..._buildTimeline(cs),
            ],
          ),
          if (_searchQuery.isNotEmpty)
            Positioned.fill(
              top: 180,
              child: Container(
                color: cs.surface,
                child: ListView(
                  padding: const EdgeInsets.all(16).copyWith(bottom: 120),
                  children: [
                    Text("搜索结果", style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold, fontSize: 13)),
                    const SizedBox(height: 12),
                    ..._searchResults.map((e) => Card(
                      elevation: 0,
                      margin: const EdgeInsets.only(bottom: 8),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12), side: BorderSide(color: cs.outlineVariant)),
                      child: ListTile(
                        leading: Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(color: cs.primaryContainer, shape: BoxShape.circle),
                          child: Icon(Icons.place, color: cs.primary, size: 20),
                        ),
                        title: Text(e['title'] ?? '未知记录', style: const TextStyle(fontWeight: FontWeight.bold)),
                        subtitle: Builder(builder: (context) {
                          String loc = e['location'] ?? '';
                          final coordRegex = RegExp(r'(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)');
                          final match = coordRegex.firstMatch(loc);
                          if (match != null) {
                            try {
                              double lat = double.parse(match.group(1)!);
                              double lng = double.parse(match.group(2)!);
                              loc = "${lat.toStringAsFixed(3)}, ${lng.toStringAsFixed(3)}";
                            } catch (_) {}
                          }
                          return Text(loc);
                        }),
                        trailing: const Icon(Icons.arrow_forward_ios, size: 14),
                        onTap: () {
                           FocusScope.of(context).unfocus();
                           _showDetail(e);
                        },
                      ),
                    )),
                    if (_searchResults.isEmpty)
                      const Padding(
                        padding: EdgeInsets.only(top: 32),
                        child: Center(child: Text("没有找到匹配的足迹", style: TextStyle(color: Colors.grey))),
                      )
                  ],
                ),
              ),
            ),
          _fixedTop(cs, tt),
        ],
      ),
      floatingActionButton: Padding(
        padding: const EdgeInsets.only(bottom: 110),
        child: FloatingActionButton(
          onPressed: () {
            if (widget.hapticEnabled) HapticFeedback.mediumImpact();
            Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => const AddFootprintPage()),
            ).then((_) => widget.onSettingsChanged()); // Refresh data
          },
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

  Widget _heatLegend(String label, Color color, ColorScheme cs) => Row(
    mainAxisSize: MainAxisSize.min,
    children: [
      Container(width: 8, height: 8, decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(2))),
      const SizedBox(width: 4),
      Text(label, style: TextStyle(fontSize: 10, color: cs.outline)),
    ],
  );

  Widget _buildMoodHeatmap(ColorScheme cs) {
    // Generate dates for the current year
    final now = DateTime.now();
    final year = currentYear;
    final firstDay = DateTime(year, 1, 1);
    final daysInYear = DateTime(year + 1, 1, 1).difference(firstDay).inDays;

    // Determine grid dimensions
    final firstWeekday = (firstDay.weekday) % 7; // 0 for Sunday
    const int rows = 7;
    final int cols = ((daysInYear + firstWeekday) / 7).ceil();

    return LayoutBuilder(builder: (context, constraints) {
      final cellSize = math.max(4.0, (constraints.maxWidth - (cols - 1) * 2) / cols);
      
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Month labels
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: List.generate(12, (i) => Text('${i+1}月', style: TextStyle(fontSize: 10, color: cs.outline))),
          ),
          const SizedBox(height: 4),
          // Heatmap grid
          RepaintBoundary(
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: List.generate(cols, (colIndex) {
                  return Padding(
                    padding: const EdgeInsets.only(right: 2),
                    child: Column(
                      children: List.generate(rows, (rowIndex) {
                        final dayIndex = colIndex * rows + rowIndex - firstWeekday;
                        if (dayIndex < 0 || dayIndex >= daysInYear) {
                           return Container(width: cellSize, height: cellSize, margin: const EdgeInsets.only(bottom: 2));
                        }
                        
                        final date = firstDay.add(Duration(days: dayIndex));
                        if (date.year > year || (year == now.year && date.isAfter(now))) {
                           return Container(width: cellSize, height: cellSize, margin: const EdgeInsets.only(bottom: 2));
                        }
                        
                        final dateKey = "${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}";
                        final color = _heatmapDateColors[dateKey] ?? cs.surfaceContainerHighest.withValues(alpha: 0.5);
                        
                        return Container(
                          width: cellSize,
                          height: cellSize,
                          margin: const EdgeInsets.only(bottom: 2),
                          decoration: BoxDecoration(
                            color: color,
                            borderRadius: BorderRadius.circular(2),
                          ),
                        );
                      }),
                    ),
                  );
                }),
              ),
            ),
          ),
        ],
      );
    });
  }
  
  // Custom timeline renderer grouping by month
  List<Widget> _buildTimeline(ColorScheme cs) {
    if (_timelineMonths.isEmpty) {
      return [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 32),
          child: Center(
            child: Text("此年份暂无足迹记录", style: TextStyle(color: cs.outline)),
          ),
        )
      ];
    }

    return _timelineMonths.map((bucket) {
      final month = bucket.month;
      final isExpanded = _expandedMonths.contains(month);
      final monthEntries = bucket.entries;
      
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          InkWell(
            onTap: () {
              setState(() {
                if (isExpanded) _expandedMonths.remove(month);
                else _expandedMonths.add(month);
              });
            },
            borderRadius: BorderRadius.circular(8),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
              child: Row(
                children: [
                  Text("$month 月", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: cs.primary)),
                  const Spacer(),
                  Text("${monthEntries.length} 篇", style: TextStyle(color: cs.outline, fontSize: 12)),
                  Icon(isExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down, color: cs.outline, size: 20),
                ],
              ),
            ),
          ),
          AnimatedSize(
            duration: const Duration(milliseconds: 800),
            curve: Curves.elasticOut,
            child: isExpanded
                ? Column(
                    children: monthEntries.map((entry) {
                      final String dateStr = entry['happenedOn'] ?? '';
                      final fragments = dateStr.split('-');
                      final String displayDate = fragments.length >= 3
                          ? "${fragments[1]}-${fragments[2].substring(0, 2)}"
                          : dateStr;

                      String? thumbPath;
                      final photos = entry['photoPaths'] as List<dynamic>?;
                      if (photos != null &&
                          photos.isNotEmpty &&
                          photos[0] is String) {
                        thumbPath = photos[0];
                      }

                      final iconStr = entry['icon'] as String? ?? '';
                      IconData iconData = Icons.location_on;
                      switch (iconStr) {
                        case "Restaurant":
                          iconData = Icons.restaurant;
                          break;
                        case "LocalCafe":
                          iconData = Icons.local_cafe;
                          break;
                        case "Park":
                          iconData = Icons.park;
                          break;
                        case "Flight":
                          iconData = Icons.flight;
                          break;
                        case "Train":
                          iconData = Icons.train;
                          break;
                        case "DirectionsBike":
                          iconData = Icons.directions_bike;
                          break;
                        case "ShoppingBag":
                          iconData = Icons.shopping_bag;
                          break;
                        case "CameraAlt":
                          iconData = Icons.camera_alt;
                          break;
                      }

                      final dist =
                          (entry['distanceKm'] as num?)?.toDouble() ?? 0.0;
                      final rDist = dist.toStringAsFixed(1);

                      return Padding(
                        padding: const EdgeInsets.only(bottom: 12, left: 8),
                        child: InkWell(
                          onTap: () => _showDetail(entry),
                          borderRadius: BorderRadius.circular(16),
                          child: Card(
                            elevation: 0,
                            margin: EdgeInsets.zero,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                              side: BorderSide(
                                  color: cs.outlineVariant
                                      .withValues(alpha: 0.5)),
                            ),
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Row(
                                children: [
                                  CircleAvatar(
                                    backgroundColor: cs.primaryContainer,
                                    child: Icon(iconData,
                                        color: cs.onPrimaryContainer, size: 20),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          entry['title'] ?? '未知足迹',
                                          style: const TextStyle(
                                              fontWeight: FontWeight.bold),
                                          maxLines: 3,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                        const SizedBox(height: 4),
                                        Builder(builder: (context) {
                                          String loc =
                                              entry['location'] ?? '未知地点';
                                          final coordRegex = RegExp(
                                              r'(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)');
                                          final match =
                                              coordRegex.firstMatch(loc);
                                          if (match != null) {
                                            try {
                                              double lat =
                                                  double.parse(match.group(1)!);
                                              double lng =
                                                  double.parse(match.group(2)!);
                                              loc =
                                                  "${lat.toStringAsFixed(3)}, ${lng.toStringAsFixed(3)}";
                                            } catch (_) {}
                                          }
                                          return Text(
                                            '$displayDate · $loc',
                                            style:
                                                const TextStyle(fontSize: 12),
                                          );
                                        }),
                                      ],
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  if (thumbPath != null)
                                    ClipRRect(
                                      borderRadius: BorderRadius.circular(8),
                                      child: Image.file(
                                        File(thumbPath),
                                        width: 40,
                                        height: 40,
                                        fit: BoxFit.cover,
                                        errorBuilder: (c, e, s) => Icon(
                                            Icons.image_not_supported,
                                            size: 40,
                                            color: cs.outline),
                                      ),
                                    )
                                  else
                                    Text(
                                      '$rDist km',
                                      style: TextStyle(
                                        color: cs.primary,
                                        fontWeight: FontWeight.bold,
                                        fontSize: 12,
                                      ),
                                    ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      );
                    }).toList(),
                  )
                : const SizedBox(width: double.infinity, height: 0),
          ),
          const SizedBox(height: 8),
        ],
      );
    }).toList();
  }

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
                icon: Icon(Icons.military_tech, color: cs.primary),
                tooltip: "荣誉勋章",
                onPressed: () async {
                  if (widget.hapticEnabled) HapticFeedback.mediumImpact();
                  try {
                    const channel = MethodChannel('com.footprint/data');
                    final idsStr = await channel.invokeMethod<String>('getUnlockedBadgeIds');
                    final dictStr = await channel.invokeMethod<String>('getBadgeDictionary');
                    if (idsStr != null && dictStr != null && mounted) {
                        List<String> unlockedIds = List<String>.from(jsonDecode(idsStr));
                        final List<dynamic> rawList = jsonDecode(dictStr);
                        
                        // Group by category for the BadgeHallScreen's expected format
                        final Map<String, List<dynamic>> dict = {};
                        for (var item in rawList) {
                            final cat = item['category'] ?? 'Other';
                            dict.putIfAbsent(cat, () => []).add(item);
                        }

                        Navigator.push(context, MaterialPageRoute(builder: (_) => BadgeHallScreen(badgeDictionary: dict, unlockedIds: unlockedIds)));
                    }
                  } catch (e) {
                    debugPrint("Badge screen error: $e");
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text("无法打开勋章馆: $e"), backgroundColor: Theme.of(context).colorScheme.error),
                      );
                    }
                  }
                },
              ),
              PopupMenuButton<String>(
                icon: Icon(Icons.more_vert, color: cs.onSurface),
                onSelected: (value) {
                  if (value == 'settings') {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => SettingsScreen(
                          nickname: widget.nickname,
                          avatarId: widget.avatarId,
                          themeMode: widget.themeMode,
                          themeStyle: widget.themeStyle,
                          hapticEnabled: widget.hapticEnabled,
                          isMaintValid: widget.isMaintValid,
                          onUpdate: widget.onSettingsChanged,
                        ),
                      ),
                    );
                  } else if (value == 'about') {
                    _showAboutDialog(context);
                  }
                },
                color: cs.surface,
                surfaceTintColor: Colors.transparent,
                itemBuilder: (context) => [
                  PopupMenuItem(
                    value: 'settings',
                    child: Row(
                      children: [
                        Icon(Icons.settings, size: 20, color: cs.onSurfaceVariant),
                        const SizedBox(width: 12),
                        Text("设置", style: TextStyle(color: cs.onSurface)),
                      ],
                    ),
                  ),
                  PopupMenuItem(
                    value: 'about',
                    child: Row(
                      children: [
                        Icon(Icons.info_outline, size: 20, color: cs.onSurfaceVariant),
                        const SizedBox(width: 12),
                        Text("关于", style: TextStyle(color: cs.onSurface)),
                      ],
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _searchController,
            onChanged: (val) {
              final query = val.trim().toLowerCase();
              setState(() {
                _searchQuery = query;
                _searchResults = _buildSearchResults(allTimeEntries, query);
              });
            },
            decoration: InputDecoration(
              hintText: '搜索地点、标签...',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: _searchQuery.isNotEmpty 
                  ? IconButton(
                      icon: const Icon(Icons.close),
                      onPressed: () {
                        setState(() {
                          _searchQuery = "";
                          _searchResults = const [];
                          _searchController.clear();
                        });
                        FocusScope.of(context).unfocus();
                      },
                    ) 
                  : null,
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
  void _showAboutDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) {
        final cs = Theme.of(ctx).colorScheme;
        final isDark = Theme.of(ctx).brightness == Brightness.dark;
        return AlertDialog(
          backgroundColor: isDark ? const Color(0xFF1A1A1A) : null,
          title: const Center(child: Text("关于 Footprint")),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Center(
                child: Icon(Icons.explore, size: 72, color: Colors.blueAccent),
              ),
              const SizedBox(height: 20),
              const Center(
                child: Text(
                  "Footprint v3.6.6",
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20, color: Colors.blueAccent),
                ),
              ),
              const SizedBox(height: 8),
              Center(
                child: Text(
                  "记录足迹，遇见更好的自己。",
                  textAlign: TextAlign.center,
                  style: TextStyle(color: isDark ? Colors.white70 : Colors.black87),
                ),
              ),
              const SizedBox(height: 20),
              Center(
                child: Text(
                  "一款基于 Flutter 构建的 Material Design 3 风格足迹探索应用。",
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 14, color: isDark ? Colors.white60 : Colors.black54),
                ),
              ),
              const SizedBox(height: 20),
              const Divider(),
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.person_outline, color: Colors.blueAccent),
                title: const Text("作者主页", style: TextStyle(fontSize: 14)),
                subtitle: const Text("StarsUnsurpass", style: TextStyle(fontSize: 12)),
                onTap: () {
                  const channel = MethodChannel('com.footprint/data');
                  channel.invokeMethod('openUrl', "https://github.com/StarsUnsurpass");
                },
              ),
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.code),
                title: const Text("项目源码"),
                subtitle: const Text("GitHub / Footprint"),
                onTap: () {
                  const channel = MethodChannel('com.footprint/data');
                  channel.invokeMethod('openUrl', "https://github.com/StarsUnsurpass/Footprint");
                },
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text("关闭"),
            ),
          ],
        );
      },
    );
  }
}

// --- 探索地图页 (ExploreMapScreen) - 1:1 复刻原生功能 ---
class ExploreMapScreen extends StatefulWidget {
  final String themeMode;
  const ExploreMapScreen({super.key, required this.themeMode});
  @override
  State<ExploreMapScreen> createState() => _ExploreMapScreenState();
}

class _TrackingOverlayData {
  final double totalDistanceMeters;
  final int pointCount;
  final double? lat;
  final double? lng;

  const _TrackingOverlayData({
    this.totalDistanceMeters = 0.0,
    this.pointCount = 0,
    this.lat,
    this.lng,
  });
}

class _ExploreMapScreenState extends State<ExploreMapScreen>
    with WidgetsBindingObserver {
  static const dataChannel = MethodChannel('com.footprint/data');
  static const streamChannel = EventChannel('com.footprint/stream');
  String mapMode = 'STANDARD';
  MethodChannel? _mapChannel;
  List<dynamic> _allEntries = [];
  bool? _lastNativeShowHistory;
  String? _lastNativeMode;
  bool? _lastNativeThemeIsDark;
  bool? _lastNativeFogEnabled;
  int? _lastNativeEntriesHash;
  int? _lastNativeCapsulesHash;
  int? _lastNativeFogHash;

  // Flutter 高德定位客户端 (仅供单击定位使用)
  final AMapFlutterLocation _locationClient = AMapFlutterLocation();
  StreamSubscription<Map<String, Object>>? _locationSubscription;
  StreamSubscription? _streamSubscription;

  // === 定位状态管理 ===
  bool _isLocating = false;    // 是否正在执行单次定位
  bool _isMapActive = true;    // 当前是否在地图 tab
  bool _userRequestedLocation = false; // 是否手动点击了定位

  // === 追踪状态 (Flutter 实现，匹配原 Kotlin LocationTrackingService) ===
  bool _isTracking = false;
  bool _isPaused = false;
  final List<Map<String, double>> _trackingPath = [];
  double _totalDistance = 0.0;
  int _sessionStartTime = 0;
  int _lastKnownDurationMs = 0;
  double? _lastLat;
  double? _lastLng;
  int? _lastPointTime;
  int _lastSaveTime = 0;
  String _lastAddress = '';
  List<dynamic> _capsules = [];
  double? _lastAltitude;
  bool _batteryOptimizationPromptShown = false;
  Timer? _durationTimer;
  final ValueNotifier<String> _durationNotifier = ValueNotifier<String>('00:00:00');
  final ValueNotifier<_TrackingOverlayData> _trackingOverlayNotifier =
      ValueNotifier(const _TrackingOverlayData());

  // 匹配原 Kotlin 服务的过滤阈值
  static const double _maxSpeedMs = 50.0;
  static const double _minDistanceM = 0.5;
  static const double _minValidLatLng = 0.1;
  static const int _saveIntervalMs = 2000;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadEntries();
    // 初始化高德隐私合规（不自动开启定位）
    AMapFlutterLocation.updatePrivacyShow(true, true);
    AMapFlutterLocation.updatePrivacyAgree(true);
    _initApiKey();

    _recoverTrackingState();

    _streamSubscription = streamChannel.receiveBroadcastStream().listen((event) {
      if (!mounted) return;
      try {
        final data = jsonDecode(event as String);
        if (data['type'] == 'status') {
          setState(() {
            _isTracking = data['isTracking'];
            _isPaused = data['isPaused'] ?? false;
            
            // 如果 native 传回了持续时间，我们优先用它来校准 sessionStartTime
            if (data['durationMs'] != null && _isTracking) {
              _lastKnownDurationMs = (data['durationMs'] as num).toInt();
              _sessionStartTime = DateTime.now().millisecondsSinceEpoch - _lastKnownDurationMs;
            }

            _updateNativeLocationState();
            _updateNativeMap();
            if (!_isTracking) {
              _durationNotifier.value = '00:00:00';
              _trackingOverlayNotifier.value = const _TrackingOverlayData();
              _durationTimer?.cancel();
            } else if (_durationTimer == null || !_durationTimer!.isActive) {
               _startDurationTimer();
            }
          });
        } else if (data['type'] == 'location') {
          if (_isTracking && data['data'] != null) {
            final lat = (data['data']['latitude'] as num).toDouble();
            final lng = (data['data']['longitude'] as num).toDouble();

            _lastLat = lat;
            _lastLng = lng;
            _trackingPath.add({'latitude': lat, 'longitude': lng});
            _updateTrackingOverlay(
              totalDistanceMeters: _totalDistance,
              pointCount: _trackingPath.length,
              lat: lat,
              lng: lng,
            );
            _mapChannel?.invokeMethod('setTrackingPath', _trackingPath);
          }
        } else if (data['type'] == 'distance') {
           _totalDistance = (data['distance'] as num).toDouble();
           _updateTrackingOverlay(
             totalDistanceMeters: _totalDistance,
             pointCount: _trackingPath.length,
             lat: _lastLat,
             lng: _lastLng,
           );
        }
      } catch (e) {
        debugPrint('Stream Error: $e');
      }
    });
  }

  Future<void> _initApiKey() async {
    try {
      final String jsonStr = await dataChannel.invokeMethod('getAppCredentials');
      final creds = jsonDecode(jsonStr);
      final String amapKey = creds['amapKey'] ?? "";
      if (amapKey.isNotEmpty) {
        AMapFlutterLocation.setApiKey(amapKey, "");
        debugPrint("AMap API Key set in Flutter: $amapKey");
      }
    } catch (e) {
      debugPrint("Error initializing API Key: $e");
    }
  }

  Future<void> _recoverTrackingState() async {
    try {
      final json = await dataChannel.invokeMethod('getTrackingState');
      final state = jsonDecode(json);
      if (state['isTracking'] == true) {
        setState(() {
          _isTracking = true;
          _isPaused = state['isPaused'] == true;
          _totalDistance = (state['totalDistance'] as num).toDouble();
          _lastKnownDurationMs = (state['totalDurationMs'] as num).toInt();
          _sessionStartTime = DateTime.now().millisecondsSinceEpoch - _lastKnownDurationMs;
          _trackingPath.clear();
          for (var p in state['path']) {
            _trackingPath.add({'latitude': p['latitude'], 'longitude': p['longitude']});
          }
        });
        final lastPoint = _trackingPath.isNotEmpty ? _trackingPath.last : null;
        _lastLat = lastPoint?['latitude'];
        _lastLng = lastPoint?['longitude'];
        _updateTrackingOverlay(
          totalDistanceMeters: _totalDistance,
          pointCount: _trackingPath.length,
          lat: _lastLat,
          lng: _lastLng,
        );
        _startDurationTimer();
        _mapChannel?.invokeMethod('setTrackingPath', _trackingPath);
        _updateNativeMap();
      }
    } catch (e) {
      debugPrint('Error recovering state: $e');
    }
  }

  void _startDurationTimer() {
    _durationTimer?.cancel();
    _durationTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted || !_isTracking || _isPaused) return;
      
      int now = DateTime.now().millisecondsSinceEpoch;
      late final String nextDuration;
      // 核心修复：如果 _sessionStartTime 为 0 或异常，直接使用 _lastKnownDurationMs
      if (_sessionStartTime <= 1000000) { // 甚至没到 1970-01-01 00:16
        nextDuration = _formatDuration(_lastKnownDurationMs);
      } else {
        int elapsed = now - _sessionStartTime;
        // 这里的 1000 小时是一个合理的上限检查，超过此值认为计算溢出（通常是因为 startTime 错误）
        if (elapsed > 3600000 * 1000 || elapsed < 0) {
          elapsed = _lastKnownDurationMs;
        }
        nextDuration = _formatDuration(elapsed);
      }

      if (_durationNotifier.value != nextDuration) {
        _durationNotifier.value = nextDuration;
      }
    });
  }

  void _updateTrackingOverlay({
    required double totalDistanceMeters,
    required int pointCount,
    double? lat,
    double? lng,
  }) {
    final current = _trackingOverlayNotifier.value;
    final sameDistance =
        (current.totalDistanceMeters - totalDistanceMeters).abs() < 0.01;
    final samePointCount = current.pointCount == pointCount;
    final sameLat = (current.lat == null && lat == null) ||
        (current.lat != null &&
            lat != null &&
            (current.lat! - lat).abs() < 0.000001);
    final sameLng = (current.lng == null && lng == null) ||
        (current.lng != null &&
            lng != null &&
            (current.lng! - lng).abs() < 0.000001);
    if (sameDistance && samePointCount && sameLat && sameLng) {
      return;
    }
    _trackingOverlayNotifier.value = _TrackingOverlayData(
      totalDistanceMeters: totalDistanceMeters,
      pointCount: pointCount,
      lat: lat,
      lng: lng,
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _locationSubscription?.cancel();
    _streamSubscription?.cancel();
    _locationClient.destroy();
    _durationTimer?.cancel();
    _durationNotifier.dispose();
    _trackingOverlayNotifier.dispose();
    super.dispose();
  }

  // === 应用生命周期管理：后台时停止定位（除非正在记录足迹）===
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.inactive) {
      // 应用进入后台，如果没有在记录足迹，停止定位
      _stopLocationIfIdle();
      // 通知原生地图也关闭一直定位
      _mapChannel?.invokeMethod('setLocationEnabled', false);
    } else if (state == AppLifecycleState.resumed) {
      // 应用回到前台，如果在地图页且正在追踪，确保事实定位运行
      if (_isMapActive && _isTracking) {
        _locationClient.startLocation();
      }
      _updateNativeLocationState();
    }
  }

  /// Tab 切换离开地图页时调用
  void onTabDeselected() {
    _isMapActive = false;
    _stopLocationIfIdle();
    _updateNativeLocationState();
  }

  /// Tab 切换进入地图页时调用
  void onTabActivated() {
    _isMapActive = true;
    _updateNativeLocationState();
    // 如果正在追踪，确保定位活跃
    if (_isTracking) {
      _locationClient.startLocation();
    }
  }

  /// 在非追踪/非定位时停止定位服务
  void _stopLocationIfIdle() {
    if (!_isTracking && !_isLocating) {
      _locationSubscription?.cancel();
      _locationClient.stopLocation();
    }
  }

  void _updateNativeLocationState() {
    // 只有在点击定位时并且当前界面是地图界面时才会定位 -> (_userRequestedLocation && _isMapActive)
    // 开始记录足迹后，只要没有停止记录足迹，不管在前后台都会一直定位 (由服务控制)。为保持地图篮点显示，跟踪时也开启
    bool shouldEnable = false;
    if (_isTracking) {
      shouldEnable = _isMapActive;
    } else {
      // 如果用户点击了定位，或者正在定位过程中，且地图处于激活状态，开启原生定位
      shouldEnable = (_userRequestedLocation || _isLocating) && _isMapActive;
    }
    _mapChannel?.invokeMethod('setLocationEnabled', shouldEnable);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _updateNativeMap();
  }

  @override
  void didUpdateWidget(covariant ExploreMapScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.themeMode != widget.themeMode) {
      _lastNativeThemeIsDark = null;
      _updateNativeMap();
    }
  }

  List<dynamic> _fogPoints = [];

  Future<void> _loadEntries() async {
    try {
      final String jsonStr = await dataChannel.invokeMethod('getAllEntries');
      final String fogPointsJson = await dataChannel.invokeMethod('getAllFogPoints');
      final String capsulesJson = await dataChannel.invokeMethod('getAllTimeCapsules');
      setState(() {
        _allEntries = jsonDecode(jsonStr);
        _fogPoints = jsonDecode(fogPointsJson);
        _capsules = jsonDecode(capsulesJson);
      });
      _lastNativeEntriesHash = null;
      _lastNativeCapsulesHash = null;
      _lastNativeFogHash = null;
      _updateNativeMap();
    } catch (e) {
      debugPrint("Failed to load map entries: $e");
    }
  }

  void _onMapCreated(int id) {
    _mapChannel = MethodChannel('com.footprint/amap_$id');
    _lastNativeShowHistory = null;
    _lastNativeMode = null;
    _lastNativeThemeIsDark = null;
    _lastNativeFogEnabled = null;
    _lastNativeEntriesHash = null;
    _lastNativeCapsulesHash = null;
    _lastNativeFogHash = null;
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
      } else if (call.method == 'onCapsuleClick') {
        int capsuleId = call.arguments;
        _showCapsuleDialog(capsuleId);
      }
    });
    _updateNativeMap();
  }

  void _showCapsuleDialog(int capsuleId) {
    if (!mounted) return;
    final capsule = _capsules.firstWhere((c) => c['id'] == capsuleId, orElse: () => null);
    if (capsule == null) return;

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: const Color(0xFF1E1E1E),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Row(
          children: [
            const Icon(Icons.timer, color: Colors.cyan),
            const SizedBox(width: 10),
            Text(
              capsule['isUnlocked'] ? "时光胶囊已开启" : "时光胶囊锁定中",
              style: const TextStyle(color: Colors.white, fontSize: 18),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (capsule['isUnlocked']) ...[
              Text(
                capsule['message'],
                style: const TextStyle(color: Colors.white70, fontSize: 16),
              ),
              const SizedBox(height: 15),
              Text(
                "埋藏时间: ${DateTime.fromMillisecondsSinceEpoch(capsule['creationTime']).toString().split('.')[0]}",
                style: const TextStyle(color: Colors.grey, fontSize: 12),
              ),
            ] else ...[
              const Text(
                "这是一颗未来的胶囊，还没到开启时刻，或者您还没到达预设位置。",
                style: TextStyle(color: Colors.white70),
              ),
            ],
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("我知道了", style: TextStyle(color: Colors.cyan)),
          ),
        ],
      ),
    );
  }

  void _updateNativeMap() {
    final bool isDark = Theme.of(context).brightness == Brightness.dark;
    final bool fogEnabled = mapMode == 'FOG' || mapMode == 'ETERNAL_REALM';
    if (_lastNativeThemeIsDark != isDark) {
      _mapChannel?.invokeMethod('setTheme', isDark);
      _lastNativeThemeIsDark = isDark;
    }
    if (_lastNativeMode != mapMode) {
      _mapChannel?.invokeMethod('setMapMode', mapMode);
      _lastNativeMode = mapMode;
    }
    if (_lastNativeFogEnabled != fogEnabled) {
      _mapChannel?.invokeMethod('setFogEnabled', fogEnabled);
      _lastNativeFogEnabled = fogEnabled;
    }
    
    // 用户要求：非追踪状态下且处于标准模式（STANDARD）时，隐藏历史足迹 Marker 和轨迹
    final bool showHistory = mapMode != 'STANDARD' || _isTracking;
    final entriesHash = Object.hash(_allEntries.length, _allEntries.isNotEmpty ? _allEntries.last['id'] : null);
    final capsulesHash = Object.hash(_capsules.length, _capsules.isNotEmpty ? _capsules.last['id'] : null);
    final fogHash = Object.hash(
      _fogPoints.length,
      _fogPoints.isNotEmpty ? _fogPoints.first['timestamp'] : null,
      _fogPoints.isNotEmpty ? _fogPoints.last['timestamp'] : null,
    );
    
    if (showHistory) {
      if (_lastNativeShowHistory != true || _lastNativeEntriesHash != entriesHash) {
        _mapChannel?.invokeMethod('setEntries', _allEntries);
        _lastNativeEntriesHash = entriesHash;
      }
      if (_lastNativeShowHistory != true || _lastNativeCapsulesHash != capsulesHash) {
        _mapChannel?.invokeMethod('setCapsules', _capsules);
        _lastNativeCapsulesHash = capsulesHash;
      }
      if (_lastNativeShowHistory != true || _lastNativeFogHash != fogHash) {
        _mapChannel?.invokeMethod('setHistoryPoints', _fogPoints);
        _lastNativeFogHash = fogHash;
      }
    } else {
      if (_lastNativeShowHistory != false) {
        _mapChannel?.invokeMethod('setEntries', []);
        _mapChannel?.invokeMethod('setCapsules', []);
        _mapChannel?.invokeMethod('setHistoryPoints', []);
        _lastNativeEntriesHash = null;
        _lastNativeCapsulesHash = null;
        _lastNativeFogHash = null;
      }
    }
    _lastNativeShowHistory = showHistory;
  }

  // === 定位按钮：直接使用原生地图的位置居中 ===
  Future<void> _handleLocateMe() async {
    try {
      final status = await Permission.locationWhenInUse.request();
      if (!status.isGranted) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: const Text("需要位置权限才能进行定位"), backgroundColor: Colors.orange,
            action: SnackBarAction(label: "去设置", textColor: Colors.white, onPressed: () => openAppSettings()),
          ));
        }
        return;
      }

      // === 策略1：直接让原生地图居中到它自己已知的蓝点位置 ===
      setState(() {
        _userRequestedLocation = true;
        _isLocating = true;
      });
      _updateNativeLocationState();

      try {
        final nativeResult = await _mapChannel?.invokeMethod('centerLocation', {'zoom': 17.0});
        if (nativeResult == true) {
          setState(() {
            _isLocating = false;
            _userRequestedLocation = false;
          });
          if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("定位成功"), backgroundColor: Colors.green, duration: Duration(seconds: 1)));
          return;
        }
      } catch (e) {
        debugPrint("Native centerLocation failed: $e, trying Flutter SDK fallback...");
      }

      // === 策略2：原生没有位置数据时，使用 Flutter AMap 定位 SDK 获取坐标 ===
      _locationClient.setLocationOption(AMapLocationOption(onceLocation: true, locationMode: AMapLocationMode.Hight_Accuracy, needAddress: true));
      _locationSubscription?.cancel();
      bool hasResult = false;
      _locationSubscription = _locationClient.onLocationChanged().listen((result) async {
        if (!mounted || hasResult) return;
        final double? lat = result['latitude'] as double?;
        final double? lng = result['longitude'] as double?;
        final errorCode = result['errorCode'];
        
        if (errorCode != null && errorCode != 0) {
          hasResult = true;
          setState(() {
            _isLocating = false;
            _userRequestedLocation = false;
          });
          _updateNativeLocationState();
          _locationSubscription?.cancel();
          _locationClient.stopLocation();
          if (!mounted) return;
          String errMsg;
          switch (errorCode) {
            case 7: errMsg = "高德Key鉴权失败：请到设置中检查API Key是否正确，包名和SHA1是否匹配";
            case 12: errMsg = "缺少定位权限：请在系统设置中授予位置权限";
            case 4: errMsg = "网络连接异常：请检查网络设置";
            case 13: errMsg = "GPS信号不可用：请到开阔地带重试";
            default: errMsg = "定位失败(错误码:$errorCode): ${result['errorInfo'] ?? '未知错误'}";
          }
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text(errMsg), backgroundColor: Colors.redAccent,
            duration: const Duration(seconds: 4),
            action: errorCode == 7 ? SnackBarAction(label: "设置Key", textColor: Colors.white, onPressed: () => _showApiKeyDialog()) : null,
          ));
          return;
        }
        if (lat != null && lng != null && lat > 1.0 && lng > 1.0) {
          hasResult = true;
          setState(() {
            _isLocating = false;
            _userRequestedLocation = false;
          });
          _updateNativeLocationState();
          await _mapChannel?.invokeMethod('centerLocation', {'latitude': lat, 'longitude': lng, 'zoom': 17.0});
          _locationSubscription?.cancel();
          _locationClient.stopLocation();
          if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("定位成功"), backgroundColor: Colors.green, duration: Duration(seconds: 1)));
        }
      });
      _locationClient.startLocation();
      Future.delayed(const Duration(seconds: 10), () {
        if (!hasResult && mounted) {
          setState(() {
            _isLocating = false;
            _userRequestedLocation = false;
          });
          _updateNativeLocationState();
          _locationSubscription?.cancel();
          _locationClient.stopLocation();
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("定位超时：请检查GPS是否开启及网络状况"), backgroundColor: Colors.orange));
        }
      });
    } catch (e) {
      setState(() {
        _isLocating = false;
        _userRequestedLocation = false;
      });
      _updateNativeLocationState();
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("定位异常: $e"), backgroundColor: Colors.redAccent));
    }
  }

  // === 开始追踪：启动原生前台服务 ===
  Future<void> _startTracking() async {
    final status = await Permission.locationWhenInUse.request();
    if (!status.isGranted) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("需要位置权限才能记录足迹"), backgroundColor: Colors.orange));
      return;
    }
    final alwaysStatus = await Permission.locationAlways.status;
    if (!alwaysStatus.isGranted) {
      final requestedAlways = await Permission.locationAlways.request();
      if (!requestedAlways.isGranted && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text("建议授予“始终允许”位置权限，后台记录会更稳定"),
            backgroundColor: Colors.orange,
            action: SnackBarAction(label: "去设置", textColor: Colors.white, onPressed: () => openAppSettings()),
          ),
        );
      }
    }
    try {
      final ignoringBattery = await dataChannel.invokeMethod('isIgnoringBatteryOptimizations') == true;
      if (!ignoringBattery && !_batteryOptimizationPromptShown) {
        _batteryOptimizationPromptShown = true;
        await dataChannel.invokeMethod('requestBatteryOptimizationExemption');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text("建议允许后台电池保护例外，长时间记录不易中断"), duration: Duration(seconds: 3)),
          );
        }
      }
    } catch (_) {}
    setState(() {
      _trackingPath.clear();
      _totalDistance = 0.0;
      _lastLat = null;
      _lastLng = null;
      _sessionStartTime = DateTime.now().millisecondsSinceEpoch;
    });
    _durationNotifier.value = '00:00:00';
    _trackingOverlayNotifier.value = const _TrackingOverlayData();

    try {
      await _mapChannel?.invokeMethod('centerLocation', {'zoom': 18.0});
    } catch (_) {}

    try {
      await dataChannel.invokeMethod('startTracking');
      setState(() {
        _isTracking = true;
        _isPaused = false;
        _lastKnownDurationMs = 0;
        _sessionStartTime = DateTime.now().millisecondsSinceEpoch;
      });

      _startDurationTimer();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("启动追踪失败: $e"), backgroundColor: Colors.orange));
    }
  }

  // === 暂停追踪 ===
  Future<void> _pauseTracking() async {
    try {
      await dataChannel.invokeMethod('pauseTracking');
      final json = await dataChannel.invokeMethod('getTrackingState');
      final state = jsonDecode(json);
      setState(() {
         _isPaused = true;
         _lastKnownDurationMs = (state['totalDurationMs'] as num).toInt();
      });
    } catch (e) {
      debugPrint('Error pausing tracking: $e');
    }
  }

  // === 恢复追踪 ===
  Future<void> _resumeTracking() async {
    try {
      await dataChannel.invokeMethod('resumeTracking');
      setState(() {
        _isPaused = false;
        _sessionStartTime = DateTime.now().millisecondsSinceEpoch - _lastKnownDurationMs;
      });
    } catch (e) {
      debugPrint('Error resuming tracking: $e');
    }
  }

  // === 停止追踪 ===
  Future<void> _stopTracking() async {
    _durationTimer?.cancel();
    
    try {
      await dataChannel.invokeMethod('stopTracking');
    } catch (e) {
      debugPrint('Error stopping tracking: $e');
    }

    setState(() {
      _isTracking = false;
      _isPaused = false;
      _trackingPath.clear();
      _totalDistance = 0.0;
      _lastLat = null;
      _lastLng = null;
    });
    _durationNotifier.value = '00:00:00';
    _trackingOverlayNotifier.value = const _TrackingOverlayData();
    _mapChannel?.invokeMethod('setTrackingPath', <Map<String, double>>[]);
    
    // 延迟重新加载，给 Kotlin 保存 DB 留一点时间
    Future.delayed(const Duration(milliseconds: 500), () {
      if (mounted) _loadEntries();
    });
  }

  // === 位置回调处理被删除，因为使用原生服务了 ===
  // === UI 组装 (仅用于 Track Drawer 显示) ===
  double _haversineDistance(double lat1, double lng1, double lat2, double lng2) {
    const R = 6371000.0;
    final dLat = (lat2 - lat1) * math.pi / 180;
    final dLng = (lng2 - lng1) * math.pi / 180;
    final a = math.sin(dLat / 2) * math.sin(dLat / 2) +
        math.cos(lat1 * math.pi / 180) * math.cos(lat2 * math.pi / 180) * math.sin(dLng / 2) * math.sin(dLng / 2);
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a));
  }

  String _formatDuration(int ms) {
    final s = (ms ~/ 1000) % 60, m = (ms ~/ 60000) % 60, h = ms ~/ 3600000;
    return '${h.toString().padLeft(2, '0')}:${m.toString().padLeft(2, '0')}:${s.toString().padLeft(2, '0')}';
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
            foregroundColor: cs.onSurface,
            child: const Icon(Icons.settings_outlined),
          ),
        ),
        // 追踪信息面板（仅在追踪时显示）
        if (_isTracking)
          Positioned(
            bottom: 185,
            left: 16,
            right: 90,
            child: ValueListenableBuilder<_TrackingOverlayData>(
              valueListenable: _trackingOverlayNotifier,
              builder: (context, tracking, _) => Card(
                elevation: 6,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                color: cs.surface.withValues(alpha: 0.95),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: [
                          ValueListenableBuilder<String>(
                            valueListenable: _durationNotifier,
                            builder: (context, value, _) =>
                                _trackingStat(Icons.timer, value, "时长", cs),
                          ),
                          _trackingStat(
                            Icons.straighten,
                            "${(tracking.totalDistanceMeters / 1000).toStringAsFixed(3)} km",
                            "距离",
                            cs,
                          ),
                          _trackingStat(Icons.scatter_plot, "${tracking.pointCount}", "点位", cs),
                        ],
                      ),
                      if (tracking.lat != null && tracking.lng != null) ...[
                        const Divider(height: 24),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.location_on, size: 14, color: cs.primary),
                            const SizedBox(width: 4),
                            Text(
                              "Lat: ${tracking.lat!.toStringAsFixed(3)}  Lng: ${tracking.lng!.toStringAsFixed(3)}",
                              style: TextStyle(
                                fontSize: 12,
                                fontFamily: "monospace",
                                color: cs.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ),
          ),
        // 右下角 FAB 组
        Positioned(
          bottom: 110,
          right: 16,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // 定位按钮
              FloatingActionButton(
                heroTag: "locate_btn",
                onPressed: _handleLocateMe,
                backgroundColor: cs.surfaceContainerHighest,
                child: Icon(Icons.my_location, color: cs.primary),
              ),
              const SizedBox(height: 16),
              // 开始/停止/暂停足迹记录按钮组
              if (!_isTracking)
                FloatingActionButton(
                  heroTag: "track_start_btn",
                  onPressed: _startTracking,
                  backgroundColor: cs.primary,
                  child: const Icon(Icons.play_arrow, color: Colors.white, size: 32),
                )
              else
                Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // 暂停/继续按钮
                    FloatingActionButton(
                      heroTag: "track_pause_btn",
                      onPressed: () => _isPaused ? _resumeTracking() : _pauseTracking(),
                      backgroundColor: Colors.orangeAccent,
                      child: Icon(_isPaused ? Icons.play_arrow : Icons.pause, color: Colors.white, size: 32),
                    ),
                    const SizedBox(height: 12),
                    // 停止按钮
                    FloatingActionButton(
                      heroTag: "track_stop_btn",
                      onPressed: _stopTracking,
                      backgroundColor: Colors.red,
                      child: const Icon(Icons.stop, color: Colors.white, size: 32),
                    ),
                  ],
                ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _trackingStat(IconData icon, String value, String label, ColorScheme cs) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, color: cs.primary, size: 20),
        const SizedBox(height: 4),
        Text(value, style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: cs.onSurface)),
        Text(label, style: TextStyle(color: cs.outline, fontSize: 11)),
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
      final String amapKey = creds['amapKey'] ?? "";
      final String googleKey = creds['googleKey'] ?? "";
      final String selectedType = creds['selectedMapType'] ?? "AMAP";
      
      if (!mounted) return;
      
      final TextEditingController amapCtrl = TextEditingController(text: amapKey);
      final TextEditingController googleCtrl = TextEditingController(text: googleKey);
      String currentType = selectedType;
      
      await showDialog(
        context: context,
        builder: (ctx) {
          return StatefulBuilder(
            builder: (context, setDialogState) {
              final colorScheme = Theme.of(context).colorScheme;
              return AlertDialog(
                title: const Text("地图 API 设置"),
                content: SingleChildScrollView(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text("应用凭证 (用于申请 Key):", style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: colorScheme.primary)),
                      const SizedBox(height: 8),
                      Text("Package Name:", style: TextStyle(fontSize: 11, color: colorScheme.onSurface.withValues(alpha: 0.6))),
                      _buildCopyableRow(context, pkgName, "已复制包名"),
                      const SizedBox(height: 8),
                      Text("SHA1:", style: TextStyle(fontSize: 11, color: colorScheme.onSurface.withValues(alpha: 0.6))),
                      _buildCopyableRow(context, sha1, "已复制 SHA1"),
                      const Divider(height: 32),
                      
                      const Text("选择默认地图类型:", style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      SegmentedButton<String>(
                        segments: const [
                          ButtonSegment(value: 'AMAP', label: Text('高德地图'), icon: Icon(Icons.map)),
                          ButtonSegment(value: 'GOOGLE', label: Text('谷歌地图'), icon: Icon(Icons.language)),
                        ],
                        selected: {currentType},
                        onSelectionChanged: (newSelection) {
                          setDialogState(() => currentType = newSelection.first);
                        },
                      ),
                      const SizedBox(height: 20),
                      
                      if (currentType == 'AMAP') ...[
                        TextField(
                          controller: amapCtrl,
                          style: const TextStyle(fontSize: 14),
                          decoration: const InputDecoration(
                            labelText: "AMAP Key (高德)",
                            hintText: "请输入您的高德地图 API Key",
                            border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(12))),
                            contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text("提示: 请确保在控制台开启了 Android 平台的 SDK 权限。", 
                          style: TextStyle(fontSize: 11, color: colorScheme.outline)),
                      ] else ...[
                        TextField(
                          controller: googleCtrl,
                          style: const TextStyle(fontSize: 14),
                          decoration: const InputDecoration(
                            labelText: "Google Maps Key",
                            hintText: "请输入您的 Google API Key",
                            border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(12))),
                            contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                          ),
                        ),
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: colorScheme.secondaryContainer.withValues(alpha: 0.3),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text("如何获取 Google Key?", style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: colorScheme.onSecondaryContainer)),
                              const SizedBox(height: 4),
                              Text("1. 访问 Google Cloud Console\n2. 启用 'Maps SDK for Android'\n3. 创建 API Key 并限制为 Android 应用\n4. 添加上方的包名和 SHA1 指纹", 
                                style: TextStyle(fontSize: 11, color: colorScheme.onSecondaryContainer)),
                              TextButton.icon(
                                onPressed: () => dataChannel.invokeMethod('openUrl', 'https://console.cloud.google.com/google/maps-apis/credentials'),
                                icon: const Icon(Icons.open_in_new, size: 14),
                                label: const Text("前往控制台", style: TextStyle(fontSize: 12)),
                                style: TextButton.styleFrom(visualDensity: VisualDensity.compact),
                              ),
                            ],
                          ),
                        ),
                      ],
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
                      dataChannel.invokeMethod('saveAmapKey', amapCtrl.text);
                      dataChannel.invokeMethod('saveGoogleKey', googleCtrl.text);
                      dataChannel.invokeMethod('saveMapType', currentType);
                      Navigator.pop(ctx);
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text("设置已保存，部分更改需重启应用生效"))
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
          color: colorScheme.onSurface.withValues(alpha: 0.05),
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
  final bool isMaintValid;
  final VoidCallback onUpdate;
  const SettingsScreen({
    super.key,
    required this.nickname,
    required this.avatarId,
    required this.themeMode,
    required this.themeStyle,
    required this.hapticEnabled,
    required this.isMaintValid,
    required this.onUpdate,
  });
  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  static const channel = MethodChannel('com.footprint/data');
  late TextEditingController _nicknameController;
  late String _avatarId;
  late String _themeMode;
  late String _themeStyle;
  late bool _hapticEnabled;
  Timer? _debounce;

  @override
  void initState() {
    super.initState();
    _nicknameController = TextEditingController(text: widget.nickname);
    _avatarId = widget.avatarId;
    _themeMode = widget.themeMode;
    _themeStyle = widget.themeStyle;
    _hapticEnabled = widget.hapticEnabled;
  }

  @override
  void didUpdateWidget(SettingsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.nickname != widget.nickname &&
        _nicknameController.text != widget.nickname) {
      _nicknameController.text = widget.nickname;
    }
    if (oldWidget.avatarId != widget.avatarId) _avatarId = widget.avatarId;
    if (oldWidget.themeMode != widget.themeMode) _themeMode = widget.themeMode;
    if (oldWidget.themeStyle != widget.themeStyle) _themeStyle = widget.themeStyle;
    if (oldWidget.hapticEnabled != widget.hapticEnabled) _hapticEnabled = widget.hapticEnabled;
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  void _up(String m, dynamic v) async {
    setState(() {
      switch (m) {
        case 'updateAvatar':
          _avatarId = v as String;
          break;
        case 'updateThemeMode':
          _themeMode = v as String;
          break;
        case 'updateThemeStyle':
          _themeStyle = v as String;
          break;
        case 'updateHaptic':
          _hapticEnabled = v as bool;
          break;
      }
    });
    await channel.invokeMethod(m, v);
    widget.onUpdate();
  }

  Future<void> _handleDataOperation(String method, String label) async {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        content: Row(
          children: [
            const CircularProgressIndicator(),
            const SizedBox(width: 24),
            Text('正在$label数据...'),
          ],
        ),
      ),
    );

    try {
      final result = await channel.invokeMethod<bool>(method);
      if (context.mounted) {
        Navigator.pop(context); // Close dialog
        if (result == true) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$label成功！'), backgroundColor: Colors.green));
          widget.onUpdate();
        } else {
          // Some operations might return false or null if cancelled (like file picker)
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('已取消$label或$label失败')));
        }
      }
    } catch (e) {
      if (context.mounted) {
        Navigator.pop(context); // Close dialog
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$label失败: $e'), backgroundColor: Colors.red));
      }
    }
  }

  void _onNicknameChanged(String v) {
    if (_debounce?.isActive ?? false) _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 600), () async {
      _up('updateNickname', v);
      
      // Covert check for "Lucas"
      // Requirement: Only "Lucas" or "Ace" can trigger the cosmic secret.
      final magicLucas = String.fromCharCodes([76, 117, 99, 97, 115]);
      final magicAce = String.fromCharCodes([65, 99, 101]);
      
      if (v.trim() == magicLucas || v.trim() == magicAce) {
        if (!widget.isMaintValid) {
          _startSecretSequence(context, v.trim());
        }
      } else {
        if (widget.isMaintValid) {
          await channel.invokeMethod('syncMaintMode', false);
          widget.onUpdate();
        }
      }
    });
  }

    void _startSecretSequence(BuildContext context, String username) async {
    // Stage 1: The Whisper Trigger
    FocusManager.instance.primaryFocus?.unfocus();
    
    // Play with audio ducking feel by showing a subtle overlay
    final bool? unlocked = await showGeneralDialog<bool>(
      context: context,
      barrierColor: Colors.black.withValues(alpha: 0.8),
      transitionDuration: const Duration(milliseconds: 1500),
      pageBuilder: (context, anim, _) {
        return SecretAstrolabeSequence(
          username: username,
          onSuccess: () {
            Navigator.pop(context, true);
          },
        );
      },
      transitionBuilder: (context, anim, _, child) {
        // Desaturation Wash: 1.5s
        return ColorFiltered(
          colorFilter: ColorFilter.matrix([
            0.2126 + 0.7874 * (1 - anim.value), 0.7152 - 0.7152 * (1 - anim.value), 0.0722 - 0.0722 * (1 - anim.value), 0, 0,
            0.2126 - 0.2126 * (1 - anim.value), 0.7152 + 0.2848 * (1 - anim.value), 0.0722 - 0.0722 * (1 - anim.value), 0, 0,
            0.2126 - 0.2126 * (1 - anim.value), 0.7152 - 0.7152 * (1 - anim.value), 0.0722 + 0.9278 * (1 - anim.value), 0, 0,
            0, 0, 0, 1, 0,
          ]),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 8 * anim.value, sigmaY: 8 * anim.value),
            child: Container(
              color: const Color(0xFFD9C5B2).withValues(alpha: 0.15 * anim.value),
              child: FadeTransition(opacity: anim, child: child),
            ),
          ),
        );
      }
    );

    if (unlocked == true) {
      if (mounted) _showMagicPopup(context, username);
    }
  }

  void _showMagicPopup(BuildContext context, String username) {
    showGeneralDialog(
      context: context,
      barrierDismissible: true,
      barrierLabel: "Magic",
      transitionDuration: const Duration(milliseconds: 600),
      pageBuilder: (ctx, anim1, anim2) {
        return Center(
          child: Container(
            margin: const EdgeInsets.all(32),
            child: Material(
              color: Colors.transparent,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(32),
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: 14, sigmaY: 14),
                  child: Container(
                    padding: const EdgeInsets.all(32),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.1),
                      border: Border.all(color: Colors.white.withValues(alpha: 0.2)),
                      borderRadius: BorderRadius.circular(32),
                    ),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                         const Icon(
                          Icons.bolt, 
                          color: Colors.blueAccent, 
                          size: 64
                         ),
                        const SizedBox(height: 24),
                        Text(
                          "致 $username",
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 28,
                            fontWeight: FontWeight.w900,
                            letterSpacing: 2,
                          ),
                        ),
                        const SizedBox(height: 16),
                        const Text(
                          "在这个星球的经纬交错中，\n遇见你是最美的坐标。\n\n新功能入口已开启，\n愿此后的每一段足迹都有光。",
                          textAlign: TextAlign.center,
                          style: TextStyle(color: Colors.white70, fontSize: 16, height: 1.6),
                        ),
                        const SizedBox(height: 32),
                        FilledButton(
                          onPressed: () {
                            channel.invokeMethod('syncMaintMode');
                            Navigator.pop(ctx);
                            widget.onUpdate();
                          },
                          style: FilledButton.styleFrom(
                            backgroundColor: Colors.white,
                            foregroundColor: Colors.black,
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                            padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
                          ),
                          child: const Text("开启探索", style: TextStyle(fontWeight: FontWeight.bold)),
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
      transitionBuilder: (ctx, anim1, anim2, child) {
        return ScaleTransition(
          scale: CurvedAnimation(parent: anim1, curve: Curves.elasticOut),
          child: FadeTransition(opacity: anim1, child: child),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
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
                            backgroundColor: _avatarId == id
                                ? cs.primary
                                : cs.surfaceContainerHighest,
                            child: Icon(
                              _getAv(id),
                              color: _avatarId == id
                                  ? Colors.white
                                  : cs.onSurfaceVariant,
                            ),
                          ),
                        ),
                      )
                      .toList(),
                ),
                const SizedBox(height: 16),
                if (_avatarId.contains('/') || _avatarId.contains('\\'))
                  Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: buildAvatar(_avatarId, radius: 40),
                  ),
                OutlinedButton.icon(
                  onPressed: () async {
                    try {
                      final picker = ImagePicker();
                      final file = await picker.pickImage(
                        source: ImageSource.gallery,
                        requestFullMetadata: false,
                      );
                      if (file != null) {
                        _up('updateAvatar', file.path);
                      }
                    } catch (e) {
                      debugPrint("Avatar Picker Error: $e");
                      String errorMsg = e.toString();
                      if (errorMsg.contains("missing_valid_image_uri")) {
                        errorMsg = "系统无法找到选定的图片，请尝试从本地相册选取而非云端同步图片。";
                      }
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text("上传头像失败: $errorMsg"))
                        );
                      }
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
                  groupValue: _themeMode,
                  onChanged: (v) => _up('updateThemeMode', v),
                ),
                RadioListTile(
                  title: const Text("日间模式"),
                  value: "LIGHT",
                  groupValue: _themeMode,
                  onChanged: (v) => _up('updateThemeMode', v),
                ),
                RadioListTile(
                  title: const Text("夜间模式"),
                  value: "DARK",
                  groupValue: _themeMode,
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
                _sTile("赤陶朱砂", Icons.local_fire_department, "EMBER", cs),
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
              value: _hapticEnabled,
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
                  onTap: () => _handleDataOperation('exportData', '导出'),
                ),
                ListTile(
                  leading: const Icon(Icons.cloud_download),
                  title: const Text("导入历史记录"),
                  onTap: () => _handleDataOperation('importData', '导入'),
                ),
              ],
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
    leading: Icon(i, color: _themeStyle == v ? cs.primary : cs.outline),
    title: Text(
      t,
      style: TextStyle(
        color: _themeStyle == v ? cs.primary : null,
        fontWeight: _themeStyle == v ? FontWeight.bold : null,
      ),
    ),
    trailing: _themeStyle == v
        ? Icon(Icons.check, color: cs.primary)
        : null,
    onTap: () => _up('updateThemeStyle', v),
  );
  void _launchURL(String url) async {
    const channel = MethodChannel('com.footprint/data');
    await channel.invokeMethod('openUrl', url);
  }

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
  final String nickname;
  final bool isMaintValid;
  const ArtStudioScreen({
    super.key,
    required this.nickname,
    this.isMaintValid = false,
  });
  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final todayHint = getEternalTodayBannerText();
    
    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            pinned: true,
            elevation: 0,
            scrolledUnderElevation: 0,
            surfaceTintColor: Colors.transparent,
            backgroundColor: Theme.of(context).scaffoldBackgroundColor,
            foregroundColor: cs.onSurface,
            title: Text(
              '足迹工坊',
              style: TextStyle(
                fontWeight: FontWeight.w900,
                letterSpacing: 1.2,
                color: cs.onSurface,
              ),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 8, 18, 28),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildStudioIntroCard(context, nickname),
                  const SizedBox(height: 18),
                  _buildStudioHeroStats(context),
                  const SizedBox(height: 18),
                  _buildCuratorStrip(todayHint),
                  const SizedBox(height: 22),
                  _buildSectionHeader(
                    context,
                    "艺术创作",
                    "把足迹整理成可以分享、打印和反复回看的视觉作品",
                  ),
                  const SizedBox(height: 16),
                  _buildStudioCard(
                    context,
                    "艺术足迹导出",
                    "将您的轨迹转化为精美的极简主义艺术海报",
                    Icons.auto_awesome,
                    const Color(0xFF0D4C78),
                    const Color(0xFF55B7C4),
                    const ['极简海报', '打印感', '高完成度'],
                    () {
                      const MethodChannel('com.footprint/data').invokeMethod('openNativeScreen', {'screen_type': 'art_studio'});
                    }
                  ),
                  const SizedBox(height: 16),
                  _buildStudioCard(
                    context,
                    "时空热力图",
                    "可视化您的活动密集区域",
                    Icons.grid_view,
                    const Color(0xFF24514F),
                    const Color(0xFF9ED3B5),
                    const ['密度分析', '轨迹纹理', '城市节奏'],
                    () {
                      const MethodChannel('com.footprint/data').invokeMethod('openNativeScreen', {'screen_type': 'generative_art'});
                    }
                  ),
                  const SizedBox(height: 18),
                  _buildMiniInspirationGrid(),
                  const SizedBox(height: 32),
                  _buildSectionHeader(
                    context,
                    "实验室功能",
                    "更具空间感和沉浸感的足迹回放方式",
                  ),
                  const SizedBox(height: 16),
                  _buildStudioCard(
                    context,
                    "3D 足迹漫游",
                    "在 3D 地球上回放您的旅行故事",
                    Icons.view_in_ar,
                    const Color(0xFF5A3827),
                    const Color(0xFFF0B071),
                    const ['空间回放', '地球视角', '故事感'],
                    () {
                      const MethodChannel('com.footprint/data').invokeMethod('openNativeScreen', {'screen_type': 'export_trace'});
                    }
                  ),
                  if (isMaintValid) ...[
                    const SizedBox(height: 32),
                    _buildSectionHeader(
                      context,
                      "永恒之境",
                      "更私人的入口，只给特定的人和特定的日子",
                      accent: Colors.pinkAccent,
                    ),
                    const SizedBox(height: 16),
                    _buildStudioCard(
                      context,
                      "Lucas 的时空密室",
                      todayHint ?? "遇见你，是我最美的意外。这里珍藏着属于我们的每一刻。",
                      Icons.favorite,
                      const Color(0xFF6F1D4F),
                      const Color(0xFFFFA7C4),
                      const ['私密彩蛋', '纪念空间', '只对她开放'],
                      () {
                        Navigator.push(context, MaterialPageRoute(builder: (_) => const EternalRealmScreen()));
                      }
                    ),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStudioIntroCard(BuildContext context, String nickname) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(30),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            isDark ? cs.surfaceContainerHighest : const Color(0xFFECE3D3),
            cs.surface,
            isDark ? const Color(0xFF182523) : const Color(0xFFD7E5E0),
          ],
        ),
        border: Border.all(color: cs.outlineVariant),
      ),
      child: Stack(
        children: [
          Positioned(
            top: -24,
            right: -10,
            child: Container(
              width: 120,
              height: 120,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: cs.primary.withOpacity(isDark ? 0.12 : 0.07),
              ),
            ),
          ),
          Positioned(
            bottom: -18,
            left: -10,
            child: Container(
              width: 88,
              height: 88,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: cs.secondary.withOpacity(isDark ? 0.12 : 0.09),
              ),
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
                decoration: BoxDecoration(
                  color: cs.primary.withOpacity(0.10),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Text(
                  'FOOTPRINT ATELIER',
                  style: TextStyle(
                    color: cs.primary,
                    fontSize: 11,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 1.6,
                  ),
                ),
              ),
              const SizedBox(height: 14),
              Text(
                '$nickname，把轨迹做成值得收藏的作品。',
                style: tt.titleLarge?.copyWith(
                  color: cs.onSurface,
                  fontWeight: FontWeight.w900,
                  height: 1.2,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '从极简海报到时空热力图，再到 3D 漫游，把日常足迹整理成更有记忆点的表达。',
                style: tt.bodyMedium?.copyWith(
                  color: cs.onSurfaceVariant,
                  height: 1.5,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStudioHeroStats(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: cs.surface,
        borderRadius: BorderRadius.circular(28),
        boxShadow: [
          BoxShadow(
            color: cs.shadow.withOpacity(0.08),
            blurRadius: 22,
            offset: const Offset(0, 12),
          ),
        ],
        border: Border.all(color: cs.outlineVariant),
      ),
      child: const Row(
        children: [
          Expanded(child: _StudioMetric(value: '3', label: '创作入口', icon: Icons.layers_outlined)),
          SizedBox(width: 12),
          Expanded(child: _StudioMetric(value: '∞', label: '回忆延展', icon: Icons.auto_awesome_motion)),
          SizedBox(width: 12),
          Expanded(child: _StudioMetric(value: '1', label: '私密空间', icon: Icons.favorite_border)),
        ],
      ),
    );
  }

  Widget _buildCuratorStrip(String? todayHint) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFF183048),
        borderRadius: BorderRadius.circular(28),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.10),
              borderRadius: BorderRadius.circular(14),
            ),
            child: const Icon(Icons.tips_and_updates_outlined, color: Color(0xFFFFD18C)),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '今日策展建议',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  todayHint ?? '如果想先做一张最容易出效果的作品，优先试试“艺术足迹导出”，它最适合把一段路线收成纪念海报。',
                  style: TextStyle(
                    color: Colors.white.withOpacity(0.76),
                    height: 1.5,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMiniInspirationGrid() {
    final items = const [
      (
        '适合分享',
        '海报更适合发朋友圈或打印留存',
        Color(0xFFDB6C4B),
        Color(0xFFFFD39C),
        Color(0xFFFFF4E6),
        Icons.auto_awesome,
      ),
      (
        '适合复盘',
        '热力图更适合看一段时间的活动重心',
        Color(0xFF1D5E73),
        Color(0xFF6FD7D3),
        Color(0xFFEAFBFA),
        Icons.insights_outlined,
      ),
    ];
    return Row(
      children: items
          .map(
            (item) => Expanded(
              child: Container(
                margin: EdgeInsets.only(right: item == items.first ? 10 : 0),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [
                      item.$3,
                      Color.alphaBlend(Colors.white.withOpacity(0.14), item.$3),
                    ],
                  ),
                  borderRadius: BorderRadius.circular(22),
                  border: Border.all(color: Colors.white.withOpacity(0.14)),
                  boxShadow: [
                    BoxShadow(
                      color: item.$3.withOpacity(0.18),
                      blurRadius: 18,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 38,
                      height: 38,
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.14),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: Colors.white.withOpacity(0.16)),
                      ),
                      child: Icon(item.$6, color: item.$4, size: 20),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      item.$1,
                      style: TextStyle(
                        color: item.$5,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      item.$2,
                      style: TextStyle(
                        color: item.$5.withOpacity(0.82),
                        fontSize: 12,
                        height: 1.4,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          )
          .toList(),
    );
  }

  Widget _buildSectionHeader(
    BuildContext context,
    String title,
    String subtitle, {
    Color accent = const Color(0xFF10355B),
  }) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final effectiveAccent = accent == const Color(0xFF10355B) ? cs.primary : accent;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(color: effectiveAccent, shape: BoxShape.circle),
            ),
            const SizedBox(width: 10),
            Text(
              title,
              style: tt.titleLarge?.copyWith(
                fontWeight: FontWeight.w900,
                color: cs.onSurface,
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Text(
          subtitle,
          style: tt.bodyMedium?.copyWith(
            color: cs.onSurfaceVariant,
            height: 1.45,
          ),
        ),
      ],
    );
  }

  Widget _buildStudioCard(
    BuildContext context,
    String title,
    String subtitle,
    IconData icon,
    Color startColor,
    Color accentColor,
    List<String> tags,
    VoidCallback onTap,
  ) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(28),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(28),
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              startColor,
              Color.alphaBlend(Colors.white.withOpacity(0.08), startColor),
            ],
          ),
          boxShadow: [
            BoxShadow(
              color: startColor.withOpacity(0.22),
              blurRadius: 22,
              offset: const Offset(0, 14),
            ),
          ],
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.12),
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: Colors.white.withOpacity(0.12)),
              ),
              child: Icon(icon, color: accentColor, size: 28),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: tags
                        .map(
                          (tag) => Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: Colors.white.withOpacity(0.12),
                              borderRadius: BorderRadius.circular(999),
                            ),
                            child: Text(
                              tag,
                              style: TextStyle(
                                color: accentColor,
                                fontSize: 10,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                        )
                        .toList(),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    title,
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w900,
                      fontSize: 18,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    subtitle,
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.78),
                      fontSize: 13,
                      height: 1.45,
                    ),
                  ),
                  const SizedBox(height: 14),
                  Row(
                    children: [
                      Text(
                        '进入工坊',
                        style: TextStyle(
                          color: accentColor,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Icon(Icons.arrow_forward_rounded, color: accentColor, size: 18),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StudioMetric extends StatelessWidget {
  final String value;
  final String label;
  final IconData icon;

  const _StudioMetric({
    required this.value,
    required this.label,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: cs.surfaceContainerHighest.withOpacity(0.56),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: cs.primary, size: 18),
          const SizedBox(height: 12),
          Text(
            value,
            style: TextStyle(
              color: cs.onSurface,
              fontSize: 22,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              color: cs.onSurfaceVariant,
              fontSize: 12,
            ),
          ),
        ],
      ),
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

class TimeFootprintPlaybackPage extends StatefulWidget {
  const TimeFootprintPlaybackPage({super.key});

  @override
  State<TimeFootprintPlaybackPage> createState() => _TimeFootprintPlaybackPageState();
}

class _TimeFootprintPlaybackPageState extends State<TimeFootprintPlaybackPage> {
  static const dataChannel = MethodChannel('com.footprint/data');
  MethodChannel? _mapChannel;
  
  DateTime _startDate = DateTime.now().subtract(const Duration(days: 30));
  DateTime _endDate = DateTime.now();
  
  List<dynamic> _allEntries = [];
  List<dynamic> _filteredEntries = [];
  bool _isLoading = true;
  double _totalDistance = 0.0;
  
  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final String jsonStr = await dataChannel.invokeMethod('getAllEntries');
      if (!mounted) return;
      setState(() {
        _allEntries = jsonDecode(jsonStr);
      });
      _filterData();
    } catch (e) {
      debugPrint("Failed to load map entries: $e");
    }
  }

  Future<void> _filterData() async {
    setState(() { _isLoading = true; });
    final startStr = "${_startDate.year}-${_startDate.month.toString().padLeft(2, '0')}-${_startDate.day.toString().padLeft(2, '0')}";
    final endStr = "${_endDate.year}-${_endDate.month.toString().padLeft(2, '0')}-${_endDate.day.toString().padLeft(2, '0')}";
    
    _filteredEntries = _allEntries.where((e) {
      final d = e['happenedOn'] as String?;
      if (d == null) return false;
      return d.compareTo(startStr) >= 0 && d.compareTo(endStr) <= 0;
    }).toList();
    
    _totalDistance = 0.0;
    for (var e in _filteredEntries) {
      _totalDistance += (e['distanceKm'] as num?)?.toDouble() ?? 0.0;
    }

    try {
      final startMs = DateTime(_startDate.year, _startDate.month, _startDate.day).millisecondsSinceEpoch;
      final endMs = DateTime(_endDate.year, _endDate.month, _endDate.day).add(const Duration(days: 1)).millisecondsSinceEpoch;
      final String trackJson = await dataChannel.invokeMethod('getTrackPoints', {'startTime': startMs, 'endTime': endMs});
      final List<dynamic> tracks = jsonDecode(trackJson);
      
      _mapChannel?.invokeMethod('setEntries', _filteredEntries);
      _mapChannel?.invokeMethod('setTrackingPath', tracks.map((t) => {
        'lat': (t['latitude'] as num?)?.toDouble(),
        'lng': (t['longitude'] as num?)?.toDouble()
      }).toList());
      
      // Auto center to the first point of the track or entry
      if (tracks.isNotEmpty) {
        _mapChannel?.invokeMethod('centerLocation', {
          'latitude': (tracks.first['latitude'] as num?)?.toDouble(),
          'longitude': (tracks.first['longitude'] as num?)?.toDouble(),
          'zoom': 13.0
        });
      } else if (_filteredEntries.isNotEmpty) {
        final entry = _filteredEntries.firstWhere((e) => e['latitude'] != null, orElse: () => null);
        if (entry != null) {
          _mapChannel?.invokeMethod('centerLocation', {
            'latitude': (entry['latitude'] as num?)?.toDouble(),
            'longitude': (entry['longitude'] as num?)?.toDouble(),
            'zoom': 13.0
          });
        }
      }
    } catch (e) {
      debugPrint("Failed to load track points: $e");
    }
    
    if (mounted) {
      setState(() { _isLoading = false; });
    }
  }

  void _onMapCreated(int id) {
    _mapChannel = MethodChannel('com.footprint/amap_$id');
    final bool isDark = Theme.of(context).brightness == Brightness.dark;
    _mapChannel?.invokeMethod('setTheme', isDark);
    _mapChannel?.invokeMethod('setMapMode', "STANDARD"); // or FOG
    // after map created, apply the filtered data immediately if available
    _mapChannel?.invokeMethod('setEntries', _filteredEntries);
    _filterData(); // trigger re-fetch and apply
  }

  Future<void> _selectDate(BuildContext context, bool isStart) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: isStart ? _startDate : _endDate,
      firstDate: DateTime(2000),
      lastDate: DateTime.now(),
      locale: const Locale('zh'),
    );
    if (picked != null) {
      setState(() {
        if (isStart) _startDate = picked;
        else _endDate = picked;
        if (_startDate.isAfter(_endDate)) {
          _startDate = _endDate;
        }
      });
      _filterData();
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final df = "${_startDate.year}-${_startDate.month.toString().padLeft(2, '0')}-${_startDate.day.toString().padLeft(2, '0')}";
    final dt = "${_endDate.year}-${_endDate.month.toString().padLeft(2, '0')}-${_endDate.day.toString().padLeft(2, '0')}";

    return Scaffold(
      appBar: AppBar(
        title: const Text('时光足迹回放', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: cs.surface,
        scrolledUnderElevation: 0,
      ),
      body: Column(
        children: [
          // Time Selector
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
            child: Row(
              children: [
                Expanded(
                  child: InkWell(
                    onTap: () => _selectDate(context, true),
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      decoration: BoxDecoration(
                        color: cs.surfaceContainerHighest.withValues(alpha: 0.5),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Center(child: Text("起始: $df", style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold))),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: InkWell(
                    onTap: () => _selectDate(context, false),
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      decoration: BoxDecoration(
                        color: cs.surfaceContainerHighest.withValues(alpha: 0.5),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Center(child: Text("结束: $dt", style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold))),
                    ),
                  ),
                ),
              ],
            ),
          ),
          
          // Map View
          Expanded(
            flex: 5,
            child: ClipRRect(
              borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
              child: Stack(
                children: [
                  NativeMapView(onCreated: _onMapCreated),
                  if (_isLoading)
                    Container(
                      color: cs.surface.withValues(alpha: 0.5),
                      child: const Center(child: CircularProgressIndicator()),
                    ),
                  Positioned(
                    bottom: 16,
                    left: 16,
                    right: 16,
                    child: Card(
                      elevation: 4,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text("区间总里程", style: TextStyle(color: cs.outline, fontSize: 12)),
                                Text("${_totalDistance.toStringAsFixed(3)} km", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                              ],
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Text("区间足迹篇数", style: TextStyle(color: cs.outline, fontSize: 12)),
                                Text("${_filteredEntries.length} 篇", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),
                  )
                ],
              ),
            ),
          ),

          // Footprint List (BottomSheet-like)
          Expanded(
            flex: 4,
            child: Container(
              color: cs.surface,
              child: ListView.builder(
                itemCount: _filteredEntries.length,
                itemBuilder: (context, index) {
                  final e = _filteredEntries[index];
                  final dist = (e['distanceKm'] as num?)?.toDouble() ?? 0.0;
                  return ListTile(
                    leading: CircleAvatar(
                      backgroundColor: cs.primaryContainer,
                      child: Icon(Icons.location_on, color: cs.primary, size: 20),
                    ),
                    title: Text(e['title'] ?? '未知记录', style: const TextStyle(fontWeight: FontWeight.bold), maxLines: 1, overflow: TextOverflow.ellipsis),
                    subtitle: Builder(
                      builder: (context) {
                        String loc = e['location'] ?? '未知地点';
                        final coordRegex = RegExp(r'(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)');
                        final match = coordRegex.firstMatch(loc);
                        if (match != null) {
                          try {
                            double lat = double.parse(match.group(1)!);
                            double lng = double.parse(match.group(2)!);
                            loc = "${lat.toStringAsFixed(3)}, ${lng.toStringAsFixed(3)}";
                          } catch (_) {}
                        }
                        return Text("${e['happenedOn'] ?? ''} · $loc", maxLines: 1, overflow: TextOverflow.ellipsis);
                      }
                    ),
                    trailing: Text("${dist.toStringAsFixed(3)} km", style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold)),
                    onTap: () {
                      if (e['latitude'] != null) {
                         _mapChannel?.invokeMethod('centerLocation', {
                           'latitude': (e['latitude'] as num?)?.toDouble(),
                           'longitude': (e['longitude'] as num?)?.toDouble(),
                           'zoom': 16.0
                         });
                      }
                      Navigator.push(context, MaterialPageRoute(builder: (_) => FootprintDetailPage(entry: e)));
                    }
                  );
                },
              ),
            ),
          )
        ],
      ),
    );
  }
}
