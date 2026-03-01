import 'package:flutter/material.dart';
import 'main.dart';

class GoalPlannerPage extends StatefulWidget {
  const GoalPlannerPage({super.key});

  @override
  State<GoalPlannerPage> createState() => _GoalPlannerPageState();
}

class _GoalPlannerPageState extends State<GoalPlannerPage> {
  // Mock data to match original design
  final List<Map<String, dynamic>> goals = [
    {
      "title": "川西环线摄影",
      "targetLocation": "理塘 / 稻城",
      "date": "2024/09/15",
      "notes": "计划秋季自驾川西，主要拍摄彩林、雪山和星空。需要提前预定线路上的民宿。",
      "progress": 65,
      "isCompleted": false,
      "icon": Icons.camera_alt,
    },
    {
      "title": "周末城市漫步",
      "targetLocation": "老城区",
      "date": "2024/05/20",
      "notes": "探索未知的街巷，寻找有故事的咖啡馆。",
      "progress": 100,
      "isCompleted": true,
      "icon": Icons.local_cafe,
    }
  ];

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return Scaffold(
      backgroundColor: cs.surface,
      body: Stack(
        children: [
          // Background pattern/color if needed
          CustomScrollView(
            slivers: [
              SliverAppBar(
                expandedHeight: 120,
                pinned: true,
                backgroundColor: cs.surface.withValues(alpha: 0.8),
                flexibleSpace: FlexibleSpaceBar(
                  titlePadding: const EdgeInsets.only(left: 16, bottom: 16),
                  title: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "计划与目标",
                        style: TextStyle(
                            color: cs.onSurface,
                            fontWeight: FontWeight.bold,
                            fontSize: 20),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        "完成进度: 1/2",
                        style: TextStyle(
                            color: cs.primary,
                            fontSize: 12,
                            fontWeight: FontWeight.w500),
                      ),
                    ],
                  ),
                ),
                actions: [
                  Padding(
                    padding: const EdgeInsets.only(right: 8.0),
                    child: Center(
                      child: Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: cs.surfaceContainerHighest,
                          shape: BoxShape.circle,
                        ),
                        child: IconButton(
                          padding: EdgeInsets.zero,
                          icon: Icon(Icons.add, color: cs.primary),
                          onPressed: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(builder: (context) => const AddGoalPage()),
                            );
                          },
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              SliverPadding(
                padding: const EdgeInsets.all(16),
                sliver: SliverList(
                  delegate: SliverChildListDelegate([
                    // Summary Card
                    Card(
                      elevation: 0,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(20),
                        side: BorderSide(
                            color: cs.outlineVariant.withValues(alpha: 0.5)),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(20),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceAround,
                          children: [
                            _statItem("年度记录", "28", Icons.query_stats, cs, tt),
                            _statItem("活跃天数", "12", Icons.flag, cs, tt),
                            _statItem("连续天数", "3", Icons.check, cs, tt),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 24),
                    Text(
                      "进行中",
                      style: tt.labelLarge?.copyWith(color: cs.primary),
                    ),
                    const SizedBox(height: 12),
                    ...goals.map((g) => _buildGoalItem(g, cs, tt)).toList(),
                  ]),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _statItem(String label, String value, IconData icon, ColorScheme cs, TextTheme tt) {
    return Column(
      children: [
        Icon(icon, color: cs.primary, size: 20),
        const SizedBox(height: 4),
        Text(value, style: tt.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
        Text(label, style: tt.labelSmall?.copyWith(color: cs.onSurfaceVariant)),
      ],
    );
  }

  Widget _buildGoalItem(Map<String, dynamic> goal, ColorScheme cs, TextTheme tt) {
    final bool completed = goal['isCompleted'];

    return Card(
      elevation: 0,
      margin: const EdgeInsets.only(bottom: 16),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.3)),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () {},
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: completed ? cs.primary : cs.primaryContainer,
                ),
                child: Icon(
                  completed ? Icons.check : goal['icon'],
                  color: completed ? cs.onPrimary : cs.primary,
                  size: 24,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      goal['title'],
                      style: tt.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: completed ? cs.onSurfaceVariant.withValues(alpha: 0.6) : cs.onSurface,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text("目的地: ${goal['targetLocation']}", style: tt.bodySmall?.copyWith(color: cs.primary)),
                    Text("预计: ${goal['date']}", style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant)),
                    const SizedBox(height: 4),
                    Text(
                      goal['notes'],
                      style: tt.bodySmall?.copyWith(color: cs.outline),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 12),
                    LinearProgressIndicator(
                      value: goal['progress'] / 100,
                      backgroundColor: cs.surfaceContainerHighest,
                      color: completed ? cs.outline : cs.primary,
                      borderRadius: BorderRadius.circular(4),
                      minHeight: 4,
                    ),
                  ],
                ),
              )
            ],
          ),
        ),
      ),
    );
  }
}
