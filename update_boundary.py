import sys
import re

file_path = r'e:\Code\IDE\AndroidStudio\Footprint\app\src\main\java\com\footprint\FlutterMapView.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    text = f.read()

with open('yunnan_boundary.txt', 'r', encoding='utf-8') as f:
    bounds_str = f.read()

pattern = r'val yunnanBoundary\s*=\s*listOf\([^)]+\)'

if re.search(pattern, text):
    new_text = re.sub(pattern, bounds_str, text)
    stroke_str = '''
        // 美化云南省边缘的描边
        val strokeColor = Color.parseColor("#B0C4DE")
        map.addPolyline(PolylineOptions()
            .addAll(yunnanBoundary)
            .width(8f)
            .color(strokeColor)
            .zIndex(11f)
            .lineCapType(PolylineOptions.LineCapType.LineCapRound)
        )
'''
    new_text = new_text.replace(
        '.strokeWidth(0f)\n                .zIndex(10f)\n        )',
        '.strokeWidth(0f)\n                .zIndex(10f)\n        )' + stroke_str
    )
    new_text = new_text.replace(
        '.fillColor(Color.parseColor("#F9F1E7"))',
        '.fillColor(Color.parseColor("#D9F9F1E7"))'
    )
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_text)
    print('replaced successfully')
else:
    print('regex match failed')
