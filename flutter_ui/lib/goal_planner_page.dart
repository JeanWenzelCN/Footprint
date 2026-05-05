import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'main.dart';

class GoalPlannerPage extends StatefulWidget {
  const GoalPlannerPage({super.key});

  @override
  State<GoalPlannerPage> createState() => _GoalPlannerPageState();
}

class _GoalPlannerPageState extends State<GoalPlannerPage> {
  List<dynamic> _goals = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadGoals();
  }

  Future<void> _loadGoals() async {
    setState(() => _isLoading = true);
    try {
      final String? goalsJson = await const MethodChannel('com.footprint/data').invokeMethod('getAllGoals');
      if (goalsJson != null) {
        setState(() {
          _goals = jsonDecode(goalsJson);
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() => _isLoading = false);
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("加载失败: $e")));
    }
  }

  Future<void> _deleteGoal(dynamic goal) async {
    final bool? confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("确认删除"),
        content: Text("确定要删除目标 \"${goal['title']}\" 吗？此操作不可撤销。"),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text("取消")),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: TextButton.styleFrom(foregroundColor: Theme.of(context).colorScheme.error),
            child: const Text("删除"),
          ),
        ],
      ),
    );

    if (confirm == true) {
      try {
        await const MethodChannel('com.footprint/data').invokeMethod('deleteGoal', goal['id']);
        _loadGoals();
      } catch (e) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("删除失败: $e")));
      }
    }
  }

  void _showGoalDetail(dynamic goal) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        final cs = Theme.of(context).colorScheme;
        final tt = Theme.of(context).textTheme;
        return Container(
          decoration: BoxDecoration(
            color: cs.surface,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
          ),
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(color: cs.outlineVariant, borderRadius: BorderRadius.circular(2)),
                ),
              ),
              const SizedBox(height: 24),
              Row(
                children: [
                  Container(
                    width: 56,
                    height: 56,
                    decoration: BoxDecoration(color: cs.primaryContainer, shape: BoxShape.circle),
                    child: Icon(_getIconData(goal['icon']), color: cs.primary, size: 28),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(goal['title'], style: tt.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
                        Text(goal['targetLocation'], style: tt.bodyMedium?.copyWith(color: cs.primary)),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              _detailRow(Icons.calendar_today_outlined, "目标日期", goal['targetDate'], cs, tt),
              const SizedBox(height: 12),
              _detailRow(Icons.notes_outlined, "备注信息", goal['notes'] ?? "无", cs, tt),
              const SizedBox(height: 24),
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text("当前进度", style: tt.labelLarge),
                        const SizedBox(height: 8),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(4),
                          child: LinearProgressIndicator(
                            value: (goal['progress'] ?? 0) / 100,
                            minHeight: 8,
                            backgroundColor: cs.surfaceContainerHighest,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 16),
                  Text("${goal['progress']}%", style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold, color: cs.primary)),
                ],
              ),
              const SizedBox(height: 32),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () async {
                        Navigator.pop(context);
                        final result = await Navigator.push(
                          context,
                          MaterialPageRoute(builder: (context) => AddGoalPage(initialGoal: goal)),
                        );
                        if (result == true) _loadGoals();
                      },
                      icon: const Icon(Icons.edit_outlined),
                      label: const Text("编辑"),
                      style: OutlinedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 12)),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: FilledButton.icon(
                      onPressed: () => Navigator.pop(context),
                      icon: const Icon(Icons.check),
                      label: const Text("退出"),
                      style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 12)),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
            ],
          ),
        );
      },
    );
  }

  Widget _detailRow(IconData icon, String label, String value, ColorScheme cs, TextTheme tt) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 20, color: cs.outline),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: tt.labelSmall?.copyWith(color: cs.outline)),
              Text(value, style: tt.bodyLarge),
            ],
          ),
        ),
      ],
    );
  }

  IconData _getIconData(String? iconStr) {
    switch (iconStr) {
      case "CameraAlt": return Icons.camera_alt;
      case "LocalCafe": return Icons.local_cafe;
      case "Hiking": return Icons.hiking;
      case "Explore": return Icons.explore;
      case "Flag": return Icons.flag;
      case "Restaurant": return Icons.restaurant;
      case "Park": return Icons.park;
      case "Flight": return Icons.flight;
      case "Train": return Icons.train;
      case "DirectionsBike": return Icons.directions_bike;
      case "ShoppingBag": return Icons.shopping_bag;
      default: return Icons.flag;
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

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
                    Colors.white.withOpacity(0.74),
                    kAtelierCanvas,
                    const Color(0xFFEDE2D7),
                  ],
                ),
              ),
            ),
          ),
          Positioned(
            top: -90,
            right: -80,
            child: Container(
              width: 240,
              height: 240,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: const Color(0xFFDDEAF2).withOpacity(0.56),
              ),
            ),
          ),
          CustomScrollView(
            slivers: [
              SliverAppBar(
                expandedHeight: 120,
                pinned: true,
                backgroundColor: Colors.transparent,
                flexibleSpace: FlexibleSpaceBar(
                  titlePadding: const EdgeInsets.only(left: 16, bottom: 16),
                  title: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "计划与目标",
                        style: TextStyle(color: cs.onSurface, fontWeight: FontWeight.w900, fontSize: 20),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        "记录每一次向往",
                        style: TextStyle(color: cs.primary, fontSize: 12, fontWeight: FontWeight.w600),
                      ),
                    ],
                  ),
                ),
                actions: [
                  Padding(
                    padding: const EdgeInsets.only(right: 8.0),
                    child: IconButton(
                      icon: Container(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: [cs.primary, Color.alphaBlend(Colors.white.withOpacity(0.18), cs.primary)],
                          ),
                          borderRadius: BorderRadius.circular(16),
                        ),
                        padding: const EdgeInsets.all(10),
                        child: Icon(Icons.add, color: cs.onPrimary),
                      ),
                      onPressed: () async {
                        final result = await Navigator.push(
                          context,
                          MaterialPageRoute(builder: (context) => const AddGoalPage()),
                        );
                        if (result == true) _loadGoals();
                      },
                    ),
                  ),
                ],
              ),
              if (_isLoading)
                const SliverFillRemaining(child: Center(child: CircularProgressIndicator()))
              else if (_goals.isEmpty)
                SliverFillRemaining(
                  child: Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.flag_outlined, size: 64, color: cs.outlineVariant),
                        const SizedBox(height: 16),
                        Text("暂无目标，点击右上角添加", style: TextStyle(color: cs.outline)),
                      ],
                    ),
                  ),
                )
              else
                SliverPadding(
                  padding: const EdgeInsets.all(16),
                  sliver: SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (context, index) => _buildGoalItem(_goals[index], cs, tt),
                      childCount: _goals.length,
                    ),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildGoalItem(dynamic goal, ColorScheme cs, TextTheme tt) {
    final bool completed = goal['isCompleted'] ?? false;

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Slidable(
        key: ValueKey(goal['id']),
        endActionPane: ActionPane(
          motion: const DrawerMotion(),
          extentRatio: 0.5,
          children: [
            SlidableAction(
              onPressed: (context) async {
                final result = await Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => AddGoalPage(initialGoal: goal)),
                );
                if (result == true) _loadGoals();
              },
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
              icon: Icons.edit,
              label: '编辑',
              borderRadius: const BorderRadius.only(topLeft: Radius.circular(16), bottomLeft: Radius.circular(16)),
            ),
            SlidableAction(
              onPressed: (context) => _deleteGoal(goal),
              backgroundColor: Colors.red,
              foregroundColor: Colors.white,
              icon: Icons.delete,
              label: '删除',
              borderRadius: const BorderRadius.only(topRight: Radius.circular(16), bottomRight: Radius.circular(16)),
            ),
          ],
        ),
        child: Card(
          elevation: 0,
          margin: EdgeInsets.zero,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: BorderSide(color: cs.outlineVariant.withValues(alpha: 0.3)),
          ),
          child: InkWell(
            borderRadius: BorderRadius.circular(16),
            onTap: () => _showGoalDetail(goal),
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
                      completed ? Icons.check : _getIconData(goal['icon']),
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
                            decoration: completed ? TextDecoration.lineThrough : null,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text("${goal['targetLocation']} · ${goal['targetDate']}", style: tt.bodySmall?.copyWith(color: cs.outline)),
                        const SizedBox(height: 8),
                        LinearProgressIndicator(
                          value: (goal['progress'] ?? 0) / 100,
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
        ),
      ),
    );
  }
}
