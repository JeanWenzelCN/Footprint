import os
import shutil
from PIL import Image, ImageOps

def create_adaptive_foreground(source_path, target_path, size, content_ratio=0.66):
    """
    Creates a foreground PNG for an adaptive icon.
    Puts the source image in the center of a larger transparent background.
    size: Final image size in pixels (e.g. 432 for xxxhdpi)
    content_ratio: Ratio of the central icon to the total viewport (standard is 72/108 ~= 0.66)
    """
    img = Image.open(source_path).convert("RGBA")
    
    # Calculate dimensions
    content_size = int(size * content_ratio)
    
    # Resize source with Lanczos resampler for best quality
    try:
        resample_filter = Image.Resampling.LANCZOS
    except AttributeError:
        resample_filter = Image.LANCZOS
        
    img_resized = img.resize((content_size, content_size), resample=resample_filter)
    
    # Create empty transparent canvas
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # Paste in center
    offset = (size - content_size) // 2
    canvas.paste(img_resized, (offset, offset), img_resized)
    
    canvas.save(target_path, "PNG", optimize=True)
    print(f"Created adaptive foreground: {target_path} ({size}x{size})")

def create_legacy_icon(source_path, target_path, size):
    """ Creates a standard legacy icon. """
    img = Image.open(source_path).convert("RGBA")
    try:
        resample_filter = Image.Resampling.LANCZOS
    except AttributeError:
        resample_filter = Image.LANCZOS
        
    img_resized = img.resize((size, size), resample=resample_filter)
    img_resized.save(target_path, "PNG", optimize=True)
    print(f"Created legacy icon: {target_path} ({size}x{size})")

def update_all_icons(source_path, res_dir):
    # Density configs for legacy icons (standard sizes)
    legacy_configs = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    
    # Density configs for adaptive foregrounds (108dp base)
    # mdpi (1x) = 108px, hdpi (1.5x) = 162px, xhdpi (2x) = 216px, xxhdpi (3x) = 324px, xxxhdpi (4x) = 432px
    foreground_configs = {
        "mipmap-mdpi": 108,
        "mipmap-hdpi": 162,
        "mipmap-xhdpi": 216,
        "mipmap-xxhdpi": 324,
        "mipmap-xxxhdpi": 432,
    }

    if not os.path.exists(source_path):
        print(f"Error: Source {source_path} not found.")
        return

    # 1. Generate PNGs in each density folder
    for folder in legacy_configs.keys():
        dest_folder = os.path.join(res_dir, folder)
        os.makedirs(dest_folder, exist_ok=True)
        
        # Legacy icons
        l_size = legacy_configs[folder]
        create_legacy_icon(source_path, os.path.join(dest_folder, "footprint.png"), l_size)
        create_legacy_icon(source_path, os.path.join(dest_folder, "ic_launcher.png"), l_size)
        
        # Adaptive foregrounds
        f_size = foreground_configs[folder]
        create_adaptive_foreground(source_path, os.path.join(dest_folder, "footprint_foreground.png"), f_size)
        create_adaptive_foreground(source_path, os.path.join(dest_folder, "ic_launcher_foreground.png"), f_size)

    # 2. Update anydpi-v26 XML files
    anydpi_dir = os.path.join(res_dir, "mipmap-anydpi-v26")
    os.makedirs(anydpi_dir, exist_ok=True)
    
    # Remove old high-res PNG from anydpi-v26 if present (it should be an XML folder mostly)
    old_png = os.path.join(anydpi_dir, "footprint.png")
    if os.path.exists(old_png):
        os.remove(old_png)
    
    xml_content_footprint = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/footprint_foreground" />
</adaptive-icon>
"""
    xml_content_launcher = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
"""

    with open(os.path.join(anydpi_dir, "footprint.xml"), "w", encoding="utf-8") as f:
        f.write(xml_content_footprint)
    with open(os.path.join(anydpi_dir, "footprint_round.xml"), "w", encoding="utf-8") as f:
        f.write(xml_content_footprint)
    with open(os.path.join(anydpi_dir, "ic_launcher.xml"), "w", encoding="utf-8") as f:
        f.write(xml_content_launcher)
    with open(os.path.join(anydpi_dir, "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
        f.write(xml_content_launcher)
        
    print("Generated adaptive icon XMLs in mipmap-anydpi-v26")

if __name__ == "__main__":
    SOURCE = r"e:\Code\IDE\AndroidStudio\Footprint\Footprint.png"
    RES = r"e:\Code\IDE\AndroidStudio\Footprint\app\src\main\res"
    update_all_icons(SOURCE, RES)
