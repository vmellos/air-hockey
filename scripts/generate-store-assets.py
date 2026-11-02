#!/usr/bin/env python3
"""
generate-store-assets.py
Generates Google Play Store assets and Android launcher icons for Air Hockey 2P.
- store_assets/icon_512x512.png (Play Store official 512x512 icon)
- store_assets/feature_graphic_1024x500.png (Portuguese, English, Spanish)
- app/src/main/res/mipmap-* (Android launcher icons and round variants)
- app/src/main/res/drawable/ic_launcher_foreground.png
"""

import math
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES_DIR = os.path.join(BASE_DIR, "app/src/main/res")
STORE_DIR = os.path.join(BASE_DIR, "store_assets")

FONT_BOLD = "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Bold.otf"
FONT_REGULAR = "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Regular.otf"
if not os.path.exists(FONT_BOLD):
    FONT_BOLD = "/usr/share/fonts/google-droid-sans-fonts/DroidSans-Bold.ttf"
if not os.path.exists(FONT_REGULAR):
    FONT_REGULAR = "/usr/share/fonts/google-droid-sans-fonts/DroidSans.ttf"


def draw_glow_circle(draw, center, radius, color, blur_steps=4):
    cx, cy = center
    for i in range(blur_steps, 0, -1):
        r = radius + i * 3
        alpha = int(color[3] * (0.3 / i))
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(color[0], color[1], color[2], alpha))
    draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=color)


def generate_air_hockey_icon(size=512, is_foreground_only=False):
    """
    Renders an ultra-modern neon arcade Air Hockey icon.
    If is_foreground_only=True, renders with transparent background for adaptive foreground layer.
    """
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    scale = size / 512.0

    # 1. Background (if full icon)
    if not is_foreground_only:
        # Radial / vertical gradient
        for y in range(size):
            ratio = y / size
            # Dark navy gradient
            r = int(7 + ratio * 6)
            g = int(11 + ratio * 12)
            b = int(24 + ratio * 20)
            for x in range(size):
                # Subtle center lighting
                dx = (x - size / 2) / (size / 2)
                dy = (y - size / 2) / (size / 2)
                dist = math.sqrt(dx * dx + dy * dy)
                glow = max(0.0, 1.0 - dist * 0.7)
                cr = min(255, int(r + 15 * glow))
                cg = min(255, int(g + 20 * glow))
                cb = min(255, int(b + 35 * glow))
                img.putpixel((x, y), (cr, cg, cb, 255))

    # Glow overlay layer
    glow_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_layer)

    # 2. Arena Rink Bounds (Rounded Rect)
    pad = int(36 * scale)
    rink_box = [pad, pad, size - pad, size - pad]
    rink_radius = int(54 * scale)

    # Outer table border glow
    glow_draw.rounded_rectangle(
        rink_box,
        radius=rink_radius,
        outline=(0, 229, 255, 60),
        width=int(14 * scale)
    )
    glow_draw.rounded_rectangle(
        rink_box,
        radius=rink_radius,
        outline=(0, 229, 255, 120),
        width=int(8 * scale)
    )
    glow_draw.rounded_rectangle(
        rink_box,
        radius=rink_radius,
        outline=(180, 245, 255, 220),
        width=int(3 * scale)
    )

    # Center Line (Dashed or Solid neon line)
    cy = size // 2
    glow_draw.line(
        [(pad + 10, cy), (size - pad - 10, cy)],
        fill=(255, 42, 109, 100),
        width=int(6 * scale)
    )
    glow_draw.line(
        [(pad + 10, cy), (size - pad - 10, cy)],
        fill=(255, 120, 160, 220),
        width=int(2 * scale)
    )

    # Center Circle
    cc_rad = int(68 * scale)
    glow_draw.ellipse(
        [size // 2 - cc_rad, cy - cc_rad, size // 2 + cc_rad, cy + cc_rad],
        outline=(0, 229, 255, 80),
        width=int(5 * scale)
    )
    glow_draw.ellipse(
        [size // 2 - cc_rad, cy - cc_rad, size // 2 + cc_rad, cy + cc_rad],
        outline=(0, 229, 255, 200),
        width=int(2 * scale)
    )

    # Top Goal Crease (Red)
    goal_w = int(130 * scale)
    glow_draw.arc(
        [size // 2 - goal_w // 2, pad - int(30 * scale), size // 2 + goal_w // 2, pad + int(50 * scale)],
        start=0,
        end=180,
        fill=(255, 42, 109, 180),
        width=int(4 * scale)
    )

    # Bottom Goal Crease (Cyan)
    glow_draw.arc(
        [size // 2 - goal_w // 2, size - pad - int(50 * scale), size // 2 + goal_w // 2, size - pad + int(30 * scale)],
        start=180,
        end=360,
        fill=(0, 229, 255, 180),
        width=int(4 * scale)
    )

    # 3. Dynamic Puck Motion Trail
    # From Bottom Striker impact towards Top Right
    p1 = (int(210 * scale), int(340 * scale))  # Striker hit point
    p2 = (int(275 * scale), int(230 * scale))  # Puck location
    trail_points = 25
    for i in range(trail_points):
        t = i / trail_points
        tx = p1[0] + (p2[0] - p1[0]) * t
        ty = p1[1] + (p2[1] - p1[1]) * t
        tr = int((8 + 14 * t) * scale)
        alpha = int(220 * (t ** 1.8))
        glow_draw.ellipse(
            [tx - tr, ty - tr, tx + tr, ty + tr],
            fill=(255, 159, 28, alpha // 2)
        )
        glow_draw.ellipse(
            [tx - tr // 2, ty - tr // 2, tx + tr // 2, ty + tr // 2],
            fill=(255, 230, 0, alpha)
        )

    # 4. Top Striker (Opponent - Red/Crimson Neon)
    ts_x = int(325 * scale)
    ts_y = int(145 * scale)
    ts_r = int(44 * scale)
    # Glow
    glow_draw.ellipse([ts_x - ts_r - 12, ts_y - ts_r - 12, ts_x + ts_r + 12, ts_y + ts_r + 12], fill=(255, 42, 109, 60))
    glow_draw.ellipse([ts_x - ts_r, ts_y - ts_r, ts_x + ts_r, ts_y + ts_r], fill=(220, 20, 60, 255), outline=(255, 120, 160, 255), width=int(3 * scale))
    glow_draw.ellipse([ts_x - ts_r // 2, ts_y - ts_r // 2, ts_x + ts_r // 2, ts_y + ts_r // 2], fill=(255, 50, 90, 255), outline=(255, 200, 220, 255), width=int(2 * scale))
    glow_draw.ellipse([ts_x - ts_r // 4, ts_y - ts_r // 4, ts_x + ts_r // 4, ts_y + ts_r // 4], fill=(255, 255, 255, 230))

    # 5. Bottom Striker (Player - Cyan/Teal Neon)
    bs_x = int(195 * scale)
    bs_y = int(365 * scale)
    bs_r = int(48 * scale)
    # Glow
    glow_draw.ellipse([bs_x - bs_r - 16, bs_y - bs_r - 16, bs_x + bs_r + 16, bs_y + bs_r + 16], fill=(0, 229, 255, 70))
    glow_draw.ellipse([bs_x - bs_r, bs_y - bs_r, bs_x + bs_r, bs_y + bs_r], fill=(0, 160, 210, 255), outline=(130, 240, 255, 255), width=int(4 * scale))
    glow_draw.ellipse([bs_x - bs_r // 2, bs_y - bs_r // 2, bs_x + bs_r // 2, bs_y + bs_r // 2], fill=(0, 210, 255, 255), outline=(200, 250, 255, 255), width=int(2 * scale))
    glow_draw.ellipse([bs_x - bs_r // 4, bs_y - bs_r // 4, bs_x + bs_r // 4, bs_y + bs_r // 4], fill=(255, 255, 255, 240))

    # 6. Glowing Puck (Golden Amber / White Center)
    pk_x = p2[0]
    pk_y = p2[1]
    pk_r = int(24 * scale)
    # Outer intense glow
    glow_draw.ellipse([pk_x - pk_r - 18, pk_y - pk_r - 18, pk_x + pk_r + 18, pk_y + pk_r + 18], fill=(255, 160, 0, 80))
    glow_draw.ellipse([pk_x - pk_r - 8, pk_y - pk_r - 8, pk_x + pk_r + 8, pk_y + pk_r + 8], fill=(255, 200, 0, 160))
    glow_draw.ellipse([pk_x - pk_r, pk_y - pk_r, pk_x + pk_r, pk_y + pk_r], fill=(255, 150, 0, 255), outline=(255, 240, 180, 255), width=int(3 * scale))
    # Beveled center core
    glow_draw.ellipse([pk_x - pk_r // 2, pk_y - pk_r // 2, pk_x + pk_r // 2, pk_y + pk_r // 2], fill=(255, 255, 255, 255))

    # Impact Sparks
    spark_angles = [30, 65, 110, 140, 190, 240, 310]
    for angle in spark_angles:
        rad = math.radians(angle)
        dist1 = int(pk_r * 1.2)
        dist2 = int(pk_r * 1.8 + (angle % 7) * 4 * scale)
        x1 = pk_x + int(math.cos(rad) * dist1)
        y1 = pk_y + int(math.sin(rad) * dist1)
        x2 = pk_x + int(math.cos(rad) * dist2)
        y2 = pk_y + int(math.sin(rad) * dist2)
        glow_draw.line([(x1, y1), (x2, y2)], fill=(255, 230, 100, 220), width=int(2 * scale))

    # Composite layers
    if not is_foreground_only:
        img = Image.alpha_composite(img, glow_layer)
        # Convert to pure RGB for Google Play Store compliance (no transparency on store icon)
        final_rgb = Image.new("RGB", (size, size), (7, 11, 24))
        final_rgb.paste(img, (0, 0), img)
        return final_rgb
    else:
        return glow_layer


def generate_feature_graphic(lang="pt"):
    """
    Renders the official 1024x500 Feature Graphic banner for Google Play.
    Left: Copywriting & Badges
    Right: Dual Phone Mockup showing the Mesa Estendida experience!
    """
    canvas_w, canvas_h = 1024, 500
    banner = Image.new("RGB", (canvas_w, canvas_h), (8, 12, 26))
    draw = ImageDraw.Draw(banner)

    # 1. Background grid and ambient glow
    for y in range(0, canvas_h, 32):
        draw.line([(0, y), (canvas_w, y)], fill=(16, 24, 48), width=1)
    for x in range(0, canvas_w, 32):
        draw.line([(x, 0), (x, canvas_h)], fill=(16, 24, 48), width=1)

    # Ambient neon glows
    # Cyan glow bottom left
    for r in range(250, 0, -10):
        alpha = int(45 * (1.0 - r / 250))
        draw.ellipse([80 - r, 420 - r, 80 + r, 420 + r], fill=(0, 229, 255, alpha))
    # Crimson glow top right
    for r in range(320, 0, -10):
        alpha = int(50 * (1.0 - r / 320))
        draw.ellipse([850 - r, 80 - r, 850 + r, 80 + r], fill=(255, 42, 109, alpha))

    # 2. Right Side: Dual-Device "Mesa Estendida" Mockup
    # Two phones placed top-to-top with seamless table inside
    phone_w = 175
    phone_h = 295
    center_x = 760
    
    # Phone 1 (Top device - upside down or top half)
    p1_box = [center_x - phone_w // 2, 35, center_x + phone_w // 2, 35 + phone_h]
    # Phone 2 (Bottom device)
    gap_y = 12
    p2_box = [center_x - phone_w // 2, 35 + phone_h + gap_y, center_x + phone_w // 2, 35 + phone_h * 2 + gap_y]

    # Draw Phone 1 frame
    draw.rounded_rectangle([p1_box[0] - 5, p1_box[1] - 5, p1_box[2] + 5, p1_box[3] + 5], radius=24, fill=(28, 34, 52), outline=(0, 229, 255, 120), width=2)
    draw.rounded_rectangle(p1_box, radius=20, fill=(10, 16, 32))

    # Draw Phone 2 frame
    draw.rounded_rectangle([p2_box[0] - 5, p2_box[1] - 5, p2_box[2] + 5, p2_box[3] + 5], radius=24, fill=(28, 34, 52), outline=(0, 229, 255, 120), width=2)
    draw.rounded_rectangle(p2_box, radius=20, fill=(10, 16, 32))

    # Draw table extending across both phones!
    # Goal at Top Phone
    draw.arc([center_x - 45, 45, center_x + 45, 95], start=0, end=180, fill=(255, 42, 109), width=3)
    # Opponent Striker (Top)
    draw.ellipse([center_x - 22, 110, center_x + 22, 154], fill=(220, 20, 60), outline=(255, 120, 160), width=2)

    # Dividing line right between the two phones (simulating the physical seam)
    seam_y = 35 + phone_h + gap_y // 2
    draw.line([(center_x - phone_w // 2, seam_y), (center_x + phone_w // 2, seam_y)], fill=(0, 229, 255, 180), width=2)

    # Center circle spanning the seam!
    draw.ellipse([center_x - 40, seam_y - 40, center_x + 40, seam_y + 40], outline=(0, 229, 255, 180), width=2)

    # Glowing Puck crossing the seam!
    pk_x = center_x + 12
    pk_y = seam_y - 6
    draw.ellipse([pk_x - 14, pk_y - 14, pk_x + 14, pk_y + 14], fill=(255, 200, 0), outline=(255, 255, 255), width=2)

    # Motion trail across the seam
    draw.line([(center_x - 15, seam_y + 55), (pk_x, pk_y)], fill=(255, 160, 0, 180), width=5)

    # Player Striker (Bottom Phone)
    draw.ellipse([center_x - 24, 35 + phone_h + gap_y + 60, center_x + 24, 35 + phone_h + gap_y + 108], fill=(0, 180, 230), outline=(140, 245, 255), width=2)

    # 3. Left Side: Typography and ASO Selling Points
    font_badge = ImageFont.truetype(FONT_BOLD, 14)
    font_title = ImageFont.truetype(FONT_BOLD, 46)
    font_sub = ImageFont.truetype(FONT_BOLD, 22)
    font_desc = ImageFont.truetype(FONT_REGULAR, 17)
    font_tag = ImageFont.truetype(FONT_BOLD, 13)

    if lang == "en":
        badge_text = "REAL-TIME DUAL DEVICE MULTIPLAYER"
        title_text = "AIR HOCKEY 2P"
        sub_text = "EXTENDED TABLE ACROSS 2 PHONES"
        desc_text = "Connect two phones top-to-top via Wi-Fi or Hotspot.\nThe puck glides seamlessly from screen to screen!"
        tag1 = "100% OFFLINE"
        tag2 = "ZERO LAG HOTSPOT"
        tag3 = "ARCADE 60/120 FPS"
    elif lang == "es":
        badge_text = "MULTIJUGADOR LOCAL EN 2 CELULARES"
        title_text = "AIR HOCKEY 2P"
        sub_text = "MESA EXTENDIDA EN 2 PANTALLAS"
        desc_text = "Coloca dos celulares punta a punta por Wi-Fi o Punto de Acceso.\n¡El disco pasa fluidamente de una pantalla a la otra!"
        tag1 = "100% SIN INTERNET"
        tag2 = "CERO LAG HOTSPOT"
        tag3 = "ARCADE 60/120 FPS"
    else: # pt
        badge_text = "MULTIJOGADOR LOCAL EM 2 CELULARES"
        title_text = "AIR HOCKEY 2P"
        sub_text = "MESA ESTENDIDA EM 2 TELAS"
        desc_text = "Junte dois celulares topo-a-topo por Wi-Fi ou Ponto de Acesso.\nO disco desliza suavemente de uma tela para a outra!"
        tag1 = "100% OFFLINE"
        tag2 = "ZERO LAG HOTSPOT"
        tag3 = "ARCADE 60/120 FPS"

    # Category capsule
    draw.rounded_rectangle([60, 52, 60 + 340, 84], radius=16, fill=(16, 32, 64), outline=(0, 229, 255, 180), width=1)
    draw.text((76, 60), badge_text, font=font_badge, fill=(0, 229, 255))

    # Main Title
    draw.text((60, 102), title_text, font=font_title, fill=(255, 255, 255))

    # Subtitle
    draw.text((60, 166), sub_text, font=font_sub, fill=(0, 229, 255))

    # Description lines
    lines = desc_text.split("\n")
    y_off = 214
    for line in lines:
        draw.text((60, y_off), line, font=font_desc, fill=(180, 195, 220))
        y_off += 28

    # Feature tags
    tags = [tag1, tag2, tag3]
    tx = 60
    ty = 305
    for tag in tags:
        tw = int(draw.textlength(tag, font=font_tag)) + 24
        draw.rounded_rectangle([tx, ty, tx + tw, ty + 34], radius=8, fill=(20, 28, 48), outline=(255, 42, 109, 160), width=1)
        draw.text((tx + 12, ty + 8), tag, font=font_tag, fill=(255, 180, 205))
        tx += tw + 14

    return banner


def generate_launcher_icons():
    """
    Generates legacy and adaptive launcher mipmaps for Android:
    - mdpi (48x48)
    - hdpi (72x72)
    - xhdpi (96x96)
    - xxhdpi (144x144)
    - xxxhdpi (192x192)
    """
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    base_icon_512 = generate_air_hockey_icon(size=512, is_foreground_only=False)
    
    # Save 512x512 Store icon
    os.makedirs(STORE_DIR, exist_ok=True)
    icon_512_path = os.path.join(STORE_DIR, "icon_512x512.png")
    base_icon_512.save(icon_512_path, "PNG", optimize=True)
    print(f"✓ Saved Play Store 512x512 Icon: {icon_512_path}")

    # Generate Mipmap densities
    for folder, size in densities.items():
        folder_path = os.path.join(RES_DIR, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Square icon
        resized = base_icon_512.resize((size, size), Image.Resampling.LANCZOS)
        square_path = os.path.join(folder_path, "ic_launcher.png")
        resized.save(square_path, "PNG", optimize=True)

        # Round icon with circular mask
        mask = Image.new("L", (size, size), 0)
        mask_draw = ImageDraw.Draw(mask)
        mask_draw.ellipse([0, 0, size, size], fill=255)
        round_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        round_img.paste(resized, (0, 0), mask)
        
        round_path = os.path.join(folder_path, "ic_launcher_round.png")
        round_img.save(round_path, "PNG", optimize=True)
        print(f"✓ Generated {folder} ({size}x{size})")

    # Generate Feature Graphics in 3 languages
    fg_pt = generate_feature_graphic("pt")
    fg_pt_path = os.path.join(STORE_DIR, "feature_graphic_1024x500.png")
    fg_pt.save(fg_pt_path, "PNG", optimize=True)
    fg_pt.save(os.path.join(STORE_DIR, "feature_graphic/pt/feature_graphic_1024x500.png"), "PNG", optimize=True)
    print(f"✓ Saved Feature Graphic (PT): {fg_pt_path}")

    fg_en = generate_feature_graphic("en")
    fg_en.save(os.path.join(STORE_DIR, "feature_graphic/en/feature_graphic_1024x500.png"), "PNG", optimize=True)
    print("✓ Saved Feature Graphic (EN)")

    fg_es = generate_feature_graphic("es")
    fg_es.save(os.path.join(STORE_DIR, "feature_graphic/es/feature_graphic_1024x500.png"), "PNG", optimize=True)
    print("✓ Saved Feature Graphic (ES)")


if __name__ == "__main__":
    generate_launcher_icons()
