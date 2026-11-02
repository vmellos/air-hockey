#!/usr/bin/env python3
import os
from PIL import Image, ImageDraw

def generate_favicons():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    icon_path = os.path.join(base_dir, "store_assets/icon_512x512.png")
    web_dir = os.path.join(base_dir, "web")

    src = Image.open(icon_path).convert("RGBA")
    size = src.size[0]

    # Create rounded squircle mask (22% radius for modern app icon look)
    radius = int(size * 0.22)
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)

    # Apply mask with transparent background
    rounded = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    rounded.paste(src, (0, 0), mask)

    # Save sizes
    fav16 = rounded.resize((16, 16), Image.Resampling.LANCZOS)
    fav16.save(os.path.join(web_dir, "favicon-16x16.png"), "PNG", optimize=True)

    fav32 = rounded.resize((32, 32), Image.Resampling.LANCZOS)
    fav32.save(os.path.join(web_dir, "favicon-32x32.png"), "PNG", optimize=True)

    fav192 = rounded.resize((192, 192), Image.Resampling.LANCZOS)
    fav192.save(os.path.join(web_dir, "favicon.png"), "PNG", optimize=True)

    apple = rounded.resize((180, 180), Image.Resampling.LANCZOS)
    apple.save(os.path.join(web_dir, "apple-touch-icon.png"), "PNG", optimize=True)

    # Multi-resolution ICO
    fav32.save(os.path.join(web_dir, "favicon.ico"), format="ICO", sizes=[(16, 16), (32, 32), (48, 48)])
    print("✓ Successfully generated modern rounded favicons in", web_dir)

if __name__ == "__main__":
    generate_favicons()
