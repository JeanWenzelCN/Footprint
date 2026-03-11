import re

with open('lib/main.dart', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add import
if "import 'easter_egg.dart';" not in content:
    content = content.replace("import 'badge_hall_screen.dart';", "import 'badge_hall_screen.dart';\nimport 'easter_egg.dart';")

# 2. Replace _startSecretSequence and _showMagicPopup
new_sequence = """  void _startSecretSequence(BuildContext context) async {
    // Stage 1: The Whisper Trigger
    FocusManager.instance.primaryFocus?.unfocus();
    
    // Play with audio ducking feel by showing a subtle overlay
    final bool? unlocked = await showGeneralDialog<bool>(
      context: context,
      barrierColor: Colors.black.withValues(alpha: 0.8),
      transitionDuration: const Duration(milliseconds: 1500),
      pageBuilder: (context, anim, _) {
        return SecretAstrolabeSequence(
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
            filter: ImageFilter.blur(sigmaX: 10 * anim.value, sigmaY: 10 * anim.value),
            child: Container(
              color: const Color(0xFFD9C5B2).withValues(alpha: 0.15 * anim.value),
              child: FadeTransition(opacity: anim, child: child),
            ),
          ),
        );
      }
    );

    if (unlocked == true) {
      if (mounted) _showMagicPopup(context);
    }
  }

  void _showMagicPopup(BuildContext context) {
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
                  filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
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
                        const Icon(Icons.favorite, color: Colors.pinkAccent, size: 64),
                        const SizedBox(height: 24),
                        const Text(
                          "致 Lucas",
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 28,
                            fontWeight: FontWeight.w900,
                            letterSpacing: 2,
                          ),
                        ),
                        const SizedBox(height: 16),
                        const Text(
                          "在这个星球的经纬交错中，\\n遇见你是最美的坐标。\\n\\n新功能入口已开启，\\n愿此后的每一段足迹都有光。",
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
  }"""

# Splitting to replace
start_idx = content.find("void _startSecretSequence(BuildContext context) async {")
end_idx = content.find("@override\n  Widget build(BuildContext context) {", start_idx)

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + new_sequence + "\n\n  " + content[end_idx:]

# 3. Trim everything from "class EternalRealmScreen" to EOF (which basically deletes the old classes)
class_start = content.find("class EternalRealmScreen extends StatefulWidget {")
if class_start != -1:
    content = content[:class_start].rstrip() + "\n"

# 4. Check for 'class _SecretAstrolabeSequence' if it was before EternalRealmScreen
secret_class_start = content.find("class _SecretAstrolabeSequence extends StatefulWidget {")
if secret_class_start != -1:
    content = content[:secret_class_start].rstrip() + "\n"

with open('lib/main.dart', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done updating main.dart")
