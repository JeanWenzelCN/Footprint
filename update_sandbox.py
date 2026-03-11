import sys
import re

file_path = r'e:\Code\IDE\AndroidStudio\Footprint\app\src\main\java\com\footprint\FlutterMapView.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Change mask color to pure black
text = text.replace('.fillColor(Color.parseColor("#D9F9F1E7"))', '.fillColor(Color.parseColor("#FF000000"))')

# 2. Add 'Isolated Sandbox' styling: multiple shadow layers
sandbox_styling = '''
        // --- 孤岛沙盘特效 (Isolated Sandbox) ---
        // 增加虚空背景的多层光影效果，产生悬浮厚度感
        val layers = 5
        for (i in 1..layers) {
            val offsetColor = Color.argb(100 - i * 15, 176, 196, 222) // 淡蓝光影逐渐淡出
            aMap?.addPolyline(PolylineOptions()
                .addAll(yunnanBoundary)
                .width(12f + i * 4f)
                .color(offsetColor)
                .zIndex(9f - i) // 放在遮罩下方实现发光层
            )
        }

        // 核心发光边缘 (琥珀边缘)
        aMap?.addPolyline(PolylineOptions()
            .addAll(yunnanBoundary)
            .width(10f)
            .color(Color.parseColor("#80FFFFFF")) // 极细高光
            .zIndex(11.1f)
            .lineCapType(PolylineOptions.LineCapType.LineCapRound)
        )
'''

# Use regex to find the strokeColor block and append after it (avoiding multiple appends if run twice)
if '孤岛沙盘特效' not in text:
    target = 'val strokeColor = Color.parseColor("#B0C4DE")'
    text = text.replace(target, target + sandbox_styling)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(text)
print('Applied sandbox visual')
