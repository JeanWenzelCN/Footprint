import 'dart:async';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

// ─────────────────────────────────────────────────────────────────────────────
// Public entry-point – call this from the Badge Hall "Share" button
// ─────────────────────────────────────────────────────────────────────────────
Future<void> shareBadgePoster({
  required BuildContext context,
  required ui.FragmentProgram program,
  required Map<String, dynamic> badge,
}) async {
  // Show a loading sheet immediately so the user knows something is happening
  showModalBottomSheet<void>(
    context: context,
    backgroundColor: Colors.transparent,
    builder: (_) => const _RenderingSheet(),
  );

  try {
    // 1. Capture the badge at a perfect isometric angle
    final Uint8List? badgeBytes = await _captureBadgeAt(
      program: program,
      badge: badge,
      pitchRad: -0.26,  // −15 ° tilt toward viewer
      yawRad:    0.35,  //  20 ° tilt right (shows side thickness)
      pxSize: 800,
    );

    if (badgeBytes == null || !context.mounted) return;

    // 2. Pass PNG bytes + badge metadata to native compositing pipeline
    final Map<String, dynamic> args = {
      'badge_png_bytes': badgeBytes,
      'badge_title': badge['title'] ?? '',
      'badge_color': badge['visual_meta']?['color'] ?? '#FFFFFF',
      'material_type': badge['visual_meta']?['material'] ?? 'Base',
    };

    final result = await const MethodChannel('com.footprint/badge_poster')
        .invokeMethod<String>('composePoster', args);

    if (!context.mounted) return;
    Navigator.of(context).pop(); // dismiss loading sheet

    if (result == null) {
      _showError(context, '合成失败，请重试');
      return;
    }

    // 3. Show preview & share button
    _showPosterPreview(context, result);
  } catch (e) {
    if (context.mounted) {
      Navigator.of(context).pop();
      _showError(context, '渲染异常: $e');
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 – render the badge widget off-screen and grab PNG bytes
// ─────────────────────────────────────────────────────────────────────────────
Future<Uint8List?> _captureBadgeAt({
  required ui.FragmentProgram program,
  required Map<String, dynamic> badge,
  required double pitchRad,
  required double yawRad,
  required int pxSize,
}) async {
  // Build the matrix: perspective projection + isometric tilt
  final Matrix4 iso = Matrix4.identity()
    ..setEntry(3, 2, 0.001)   // perspective depth
    ..rotateX(pitchRad)
    ..rotateY(yawRad);

  final Color baseColor = _hexToColor(
      badge['visual_meta']?['color'], true);
  final double matType =
      badge['visual_meta']?['material'] == 'Cyber' ? 1.0 :
      badge['visual_meta']?['material'] == 'Liquid' ? 2.0 : 0.0;
  // Light from upper-left — fixed for export
  const Offset exportLight = Offset(-0.5, -0.4);

  final painter = _IsoShaderPainter(
    program: program,
    materialType: matType,
    baseColor: baseColor,
    lightOffset: exportLight,
  );

  // Record into a Picture with the isometric matrix applied
  final recorder = ui.PictureRecorder();
  final canvas = Canvas(recorder);
  final double sz = pxSize.toDouble();
  final center = Offset(sz / 2, sz / 2);

  canvas.save();
  canvas.translate(center.dx, center.dy);
  canvas.transform(Float64List.fromList(iso.storage));
  canvas.translate(-center.dx, -center.dy);
  painter.paint(canvas, Size(sz, sz));
  canvas.restore();

  final picture = recorder.endRecording();
  final img = await picture.toImage(pxSize, pxSize);
  final bytes = await img.toByteData(format: ui.ImageByteFormat.png);
  return bytes?.buffer.asUint8List();
}

// ─────────────────────────────────────────────────────────────────────────────
// Minimal painter that replicates BadgeShaderPainter without importing it
// (keeps this file self-contained)
// ─────────────────────────────────────────────────────────────────────────────
class _IsoShaderPainter extends CustomPainter {
  final ui.FragmentProgram program;
  final double materialType;
  final Color baseColor;
  final Offset lightOffset;

  const _IsoShaderPainter({
    required this.program,
    required this.materialType,
    required this.baseColor,
    required this.lightOffset,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final shader = program.fragmentShader()
      ..setFloat(0, size.width)
      ..setFloat(1, size.height)
      ..setFloat(2, lightOffset.dx * 2.0)
      ..setFloat(3, lightOffset.dy * 2.0)
      ..setFloat(4, 1.5)
      ..setFloat(5, baseColor.r)
      ..setFloat(6, baseColor.g)
      ..setFloat(7, baseColor.b)
      ..setFloat(8, baseColor.a)
      ..setFloat(9, materialType);

    canvas.drawRect(Offset.zero & size, Paint()..shader = shader);
  }

  @override
  bool shouldRepaint(covariant _IsoShaderPainter old) => false;
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
Color _hexToColor(String? hex, bool unlocked) {
  if (!unlocked) return Colors.grey.shade900;
  if (hex == null) return Colors.amber;
  final buf = StringBuffer();
  if (hex.length == 6 || hex.length == 7) buf.write('ff');
  buf.write(hex.replaceFirst('#', ''));
  return Color(int.parse(buf.toString(), radix: 16));
}

void _showError(BuildContext ctx, String msg) {
  ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(content: Text(msg)));
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading bottom-sheet
// ─────────────────────────────────────────────────────────────────────────────
class _RenderingSheet extends StatelessWidget {
  const _RenderingSheet();

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 180,
      decoration: BoxDecoration(
        color: const Color(0xFF1A1A1A),
        borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
        border: Border.all(color: Colors.white10),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const SizedBox(
            width: 48, height: 48,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              color: Colors.white54,
            ),
          ),
          const SizedBox(height: 20),
          Text(
            '正在锻造纪念封',
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.7),
              fontSize: 14,
              letterSpacing: 2,
            ),
          ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Poster preview + share
// ─────────────────────────────────────────────────────────────────────────────
void _showPosterPreview(BuildContext context, String posterPath) {
  showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (ctx) => _PosterPreviewSheet(posterPath: posterPath),
  );
}

class _PosterPreviewSheet extends StatelessWidget {
  final String posterPath;
  const _PosterPreviewSheet({required this.posterPath});

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.85,
      maxChildSize: 0.95,
      builder: (_, sc) => Container(
        decoration: BoxDecoration(
          color: const Color(0xFF0D0D0D),
          borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
          border: Border.all(color: Colors.white12),
        ),
        child: Column(
          children: [
            const SizedBox(height: 12),
            Container(
              width: 40, height: 4,
              decoration: BoxDecoration(
                color: Colors.white24,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 20),
            Expanded(
              child: ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: Image.file(
                  File(posterPath),
                  fit: BoxFit.contain,
                ),
              ),
            ),
            const SizedBox(height: 20),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Row(
                children: [
                  Expanded(
                    child: FilledButton.icon(
                      onPressed: () => _share(context, posterPath),
                      icon: const Icon(Icons.share_rounded),
                      label: const Text('分享'),
                      style: FilledButton.styleFrom(
                        backgroundColor: Colors.white,
                        foregroundColor: Colors.black,
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  IconButton.outlined(
                    onPressed: () => Navigator.pop(context),
                    icon: const Icon(Icons.close_rounded, color: Colors.white70),
                    style: IconButton.styleFrom(
                      side: const BorderSide(color: Colors.white24),
                      padding: const EdgeInsets.all(16),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }

  Future<void> _share(BuildContext context, String path) async {
    try {
      await const MethodChannel('com.footprint/badge_poster')
          .invokeMethod('sharePoster', {'path': path});
    } catch (e) {
      if (context.mounted) _showError(context, '分享失败: $e');
    }
  }
}

