import 'dart:io';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'main.dart';

class FootprintDetailPage extends StatefulWidget {
  final dynamic entry;
  const FootprintDetailPage({super.key, this.entry});

  @override
  State<FootprintDetailPage> createState() => _FootprintDetailPageState();
}

class _FootprintDetailPageState extends State<FootprintDetailPage> {
  late Map<String, dynamic> entry;
  List<Map<String, double>> trackPoints = [];
  MethodChannel? _mapChannel;

  late String title;
  late String location;
  late String date;
  late String mood;
  late Color moodColor;
  late String weather;
  late String detail;
  late double distance;
  late int energy;
  late List<String> photos;

  @override
  void initState() {
    super.initState();
    entry = widget.entry != null ? Map<String, dynamic>.from(widget.entry!) : {};

    if (entry['happenedOn'] != null) {
      _loadTrackPoints(entry['happenedOn']);
    }

    title = entry['title'] ?? "未命名足迹";
    location = entry['location'] ?? "未知地点";
    date = entry['happenedOn'] ?? "未知日期";
    
    String rawMood = entry['mood'] ?? "";
    mood = _mapMoodToChinese(rawMood);
    moodColor = _getMoodColor(rawMood);
    
    weather = entry['weather'] ?? "-";
    detail = entry['detail'] ?? entry['notes'] ?? "没有记录详细内容。";
    distance = (entry['distanceKm'] as num?)?.toDouble() ?? 0.0;
    energy = (entry['energyLevel'] as num?)?.toInt() ?? 0;
    
    // Parse photos if it's a list or comma-separated string
    final rawPhotos = entry['photos'];
    if (rawPhotos is List) {
      photos = rawPhotos.map((e) => e.toString()).toList();
    } else if (rawPhotos is String && rawPhotos.isNotEmpty) {
      photos = rawPhotos.split(',').map((s) => s.trim()).toList();
    } else {
      photos = [];
    }
  }

  Future<void> _loadTrackPoints(String dateStr) async {
    try {
      final date = DateTime.parse(dateStr);
      final startTime = DateTime.utc(date.year, date.month, date.day).millisecondsSinceEpoch;
      final endTime = startTime + 86400000;
      final result = await const MethodChannel('com.footprint/data')
          .invokeMethod('getTrackPoints', {'startTime': startTime, 'endTime': endTime});
      if (result != null) {
        final List<dynamic> list = jsonDecode(result);
        if (mounted) {
          setState(() {
            trackPoints = list.map((p) => {
              'lat': p['latitude'] as double,
              'lng': p['longitude'] as double
            }).toList();
          });
          if (_mapChannel != null && trackPoints.isNotEmpty) {
             _mapChannel?.invokeMethod('setTrackingPath', trackPoints);
             _mapChannel?.invokeMethod('centerLocation', {
               'latitude': trackPoints.first['lat'],
               'longitude': trackPoints.first['lng'],
               'zoom': 15.0
             });
          }
        }
      }
    } catch (e) {
      debugPrint("Load track points fail: $e");
    }
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
      default: return englishMood.isEmpty ? "平静" : englishMood;
    }
  }

  Color _getMoodColor(String englishMood) {
    switch (englishMood.toUpperCase()) {
      case "EXCITED": return Colors.orange;
      case "CURIOUS": return Colors.teal;
      case "RELAXED": return Colors.blue;
      case "REFLECTIVE": return Colors.purple;
      case "HAPPY": return Colors.orange;
      case "CALM": return Colors.blue;
      default: return Colors.blueGrey;
    }
  }

  int? selectedPhotoIndex;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return Scaffold(
      backgroundColor: cs.surface,
      body: Stack(
        children: [
          CustomScrollView(
            slivers: [
              SliverAppBar(
                expandedHeight: 300,
                pinned: true,
                backgroundColor: cs.surface.withOpacity(0.8),
                iconTheme: IconThemeData(color: cs.onSurface),
                actions: [
                  IconButton(
                    icon: const Icon(Icons.edit),
                    onPressed: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => AddFootprintPage(initialEntry: widget.entry),
                        ),
                      );
                    },
                  ),
                ],
                flexibleSpace: FlexibleSpaceBar(
                  background: Stack(
                    fit: StackFit.expand,
                    children: [
                      // Hero background image or color
                      Container(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                            colors: [
                              moodColor.withOpacity(0.6),
                              moodColor.withOpacity(0.2),
                            ],
                          ),
                        ),
                      ),
                      // Gradient overlay
                      Container(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                            colors: [
                              Colors.transparent,
                              cs.surface,
                            ],
                          ),
                        ),
                      ),
                      // Text overlay
                      Positioned(
                        left: 24,
                        bottom: 24,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                              decoration: BoxDecoration(
                                color: moodColor.withOpacity(0.2),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(mood, style: tt.labelMedium?.copyWith(color: moodColor)),
                            ),
                            const SizedBox(height: 8),
                            Text(title, style: tt.headlineLarge?.copyWith(fontWeight: FontWeight.w900, color: cs.onSurface)),
                            Row(
                              children: [
                                Icon(Icons.location_on, size: 16, color: cs.primary),
                                const SizedBox(width: 4),
                                Text(location, style: tt.bodyMedium?.copyWith(color: cs.primary)),
                                Padding(
                                  padding: const EdgeInsets.symmetric(horizontal: 8),
                                  child: Text("•", style: TextStyle(color: cs.outline)),
                                ),
                                Text(date, style: tt.bodyMedium?.copyWith(color: cs.outline)),
                              ],
                            ),
                            const SizedBox(height: 12),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                              decoration: BoxDecoration(
                                color: cs.primaryContainer.withOpacity(0.15),
                                borderRadius: BorderRadius.circular(16),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.wb_sunny, size: 20, color: Colors.orange), // hologram placeholder
                                  const SizedBox(width: 8),
                                  Text(weather, style: tt.labelLarge?.copyWith(color: Colors.orange)),
                                ],
                              ),
                            )
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              SliverList(
                delegate: SliverChildListDelegate([
                  // Stats row
                  Padding(
                    padding: const EdgeInsets.only(top: 24, bottom: 24, left: 16, right: 16),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        _statDrop("里程", "${distance.toStringAsFixed(3)} KM", Icons.directions_walk, cs, tt),
                        _statDrop("能量", "$energy", Icons.bolt, cs, tt),
                        _statDrop("天气", weather, Icons.wb_sunny, cs, tt),
                        _statDrop("心情", mood, Icons.mood, cs, tt),
                      ],
                    ),
                  ),

                  // Detail Body
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24),
                    child: Text(
                      detail,
                      style: tt.bodyLarge?.copyWith(
                        height: 1.8,
                        letterSpacing: 0.5,
                        color: cs.onSurfaceVariant,
                      ),
                    ),
                  ),
                  
                  // Gallery
                  if (photos.isNotEmpty) ...[
                    const SizedBox(height: 32),
                    Padding(
                      padding: const EdgeInsets.only(left: 24, bottom: 12),
                      child: Text("瞬间", style: tt.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
                    ),
                    SizedBox(
                      height: 200,
                      child: ListView.separated(
                        padding: const EdgeInsets.symmetric(horizontal: 24),
                        scrollDirection: Axis.horizontal,
                        itemCount: photos.length,
                        separatorBuilder: (context, index) => const SizedBox(width: 12),
                        itemBuilder: (context, index) {
                          final photoPath = photos[index];
                          final file = File(photoPath);
                          return GestureDetector(
                            onTap: () => _showFullImage(context, photoPath),
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(16),
                              child: file.existsSync()
                                ? Image.file(file, width: 150, height: 200, fit: BoxFit.cover,
                                    errorBuilder: (_, __, ___) => _photoPlaceholder(cs))
                                : _photoPlaceholder(cs),
                            ),
                          );
                        },
                      ),
                    ),
                  ],

                  const SizedBox(height: 32),
                  Padding(
                    padding: const EdgeInsets.only(left: 24, bottom: 12),
                    child: Text("足迹轨迹", style: tt.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24),
                    child: Container(
                      height: 250,
                      decoration: BoxDecoration(
                        color: cs.surfaceContainerHighest,
                        borderRadius: BorderRadius.circular(24),
                      ),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(24),
                        child: AndroidView(
                          viewType: 'com.footprint/amap',
                          onPlatformViewCreated: (id) {
                            _mapChannel = MethodChannel('com.footprint/amap_$id');
                            if (trackPoints.isNotEmpty) {
                               _mapChannel?.invokeMethod('setTrackingPath', trackPoints);
                               _mapChannel?.invokeMethod('centerLocation', {
                                  'latitude': trackPoints.first['lat'],
                                  'longitude': trackPoints.first['lng'],
                                  'zoom': 15.0
                               });
                            }
                          },
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 48),
                ]),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _statDrop(String label, String value, IconData icon, ColorScheme cs, TextTheme tt) {
    return Column(
      children: [
        Container(
          width: 48,
          height: 48,
          decoration: BoxDecoration(
            color: cs.surfaceContainerHighest,
            shape: BoxShape.circle,
          ),
          child: Icon(icon, color: cs.primary),
        ),
        const SizedBox(height: 8),
        Text(value, style: tt.bodyMedium?.copyWith(fontWeight: FontWeight.bold)),
        Text(label, style: tt.labelSmall?.copyWith(color: cs.outline)),
      ],
    );
  }
  Widget _photoPlaceholder(ColorScheme cs) {
    return Container(
      width: 150, height: 200,
      decoration: BoxDecoration(color: cs.surfaceContainerHighest, borderRadius: BorderRadius.circular(16)),
      child: const Center(child: Icon(Icons.broken_image, size: 48, color: Colors.grey)),
    );
  }

  void _showFullImage(BuildContext context, String path) {
    final file = File(path);
    if (!file.existsSync()) return;
    showDialog(
      context: context,
      builder: (ctx) => Dialog(
        backgroundColor: Colors.transparent,
        insetPadding: const EdgeInsets.all(16),
        child: GestureDetector(
          onTap: () => Navigator.pop(ctx),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: Image.file(file, fit: BoxFit.contain),
          ),
        ),
      ),
    );
  }
}
