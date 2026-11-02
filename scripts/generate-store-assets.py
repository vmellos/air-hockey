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
FONT_EXTRABOLD = "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-ExtraBold.otf"
FONT_MEDIUM = "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Medium.otf"
FONT_REGULAR = "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Regular.otf"
if not os.path.exists(FONT_BOLD):
    FONT_BOLD = "/usr/share/fonts/google-droid-sans-fonts/DroidSans-Bold.ttf"
if not os.path.exists(FONT_EXTRABOLD):
    FONT_EXTRABOLD = FONT_BOLD
if not os.path.exists(FONT_MEDIUM):
    FONT_MEDIUM = "/usr/share/fonts/google-droid-sans-fonts/DroidSans.ttf"
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


def generate_feature_graphic(lang="en"):
    """
    Renders the official 1024x500 Feature Graphic banner for Google Play.
    Left: Copywriting & Badges (Clean, legible, dynamic width, no FPS reference)
    Right: Dual Phone Mockup showing the Mesa Estendida experience!
    """
    canvas_w, canvas_h = 1024, 500
    banner = Image.new("RGBA", (canvas_w, canvas_h), (10, 14, 26, 255))

    # 1. Ambient Lighting Layer
    glow = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    
    # Left ambient soft blue
    glow_draw.ellipse([-80, 80, 480, 620], fill=(0, 100, 255, 35))
    # Top right crimson glow behind top phone
    glow_draw.ellipse([640, -60, 960, 260], fill=(255, 42, 109, 65))
    # Bottom right cyan glow behind bottom phone
    glow_draw.ellipse([630, 260, 950, 580], fill=(0, 229, 255, 60))
    # Golden center glow behind seam/puck
    glow_draw.ellipse([670, 150, 890, 370], fill=(255, 180, 0, 80))
    
    glow = glow.filter(ImageFilter.GaussianBlur(55))
    banner = Image.alpha_composite(banner, glow)

    # 2. Subtle Tech Grid Pattern
    grid = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    grid_draw = ImageDraw.Draw(grid)
    for y in range(0, canvas_h, 24):
        grid_draw.line([(0, y), (canvas_w, y)], fill=(120, 160, 220, 14), width=1)
    for x in range(0, canvas_w, 24):
        grid_draw.line([(x, 0), (x, canvas_h)], fill=(120, 160, 220, 14), width=1)
    banner = Image.alpha_composite(banner, grid)

    # 3. Dual Phone Mockup (Right Side)
    cx = 780
    pw = 184
    ph = 196
    gap = 8
    total_h = ph * 2 + gap
    top_y = (canvas_h - total_h) // 2

    p1_box = [cx - pw//2, top_y, cx + pw//2, top_y + ph]
    p2_box = [cx - pw//2, top_y + ph + gap, cx + pw//2, top_y + ph + gap + ph]

    # Drop shadows for phones
    shadow = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    s_draw.rounded_rectangle([p1_box[0]-6, p1_box[1]+6, p1_box[2]+6, p1_box[3]+10], radius=24, fill=(0, 0, 0, 140))
    s_draw.rounded_rectangle([p2_box[0]-6, p2_box[1]+6, p2_box[2]+6, p2_box[3]+10], radius=24, fill=(0, 0, 0, 140))
    shadow = shadow.filter(ImageFilter.GaussianBlur(14))
    banner = Image.alpha_composite(banner, shadow)

    draw = ImageDraw.Draw(banner)

    # ------------------ Phone 1 (Top Device) ------------------
    draw.rounded_rectangle(p1_box, radius=22, fill=(24, 30, 44), outline=(70, 85, 120), width=2)
    s1 = [p1_box[0] + 6, p1_box[1] + 6, p1_box[2] - 6, p1_box[3] - 4]
    draw.rounded_rectangle(s1, radius=16, fill=(8, 12, 22))
    draw.rounded_rectangle(s1, radius=16, outline=(255, 42, 109, 90), width=2)
    draw.arc([cx - 44, s1[1] - 18, cx + 44, s1[1] + 46], start=0, end=180, fill=(255, 42, 109), width=3)
    striker1_y = s1[1] + 55
    draw.ellipse([cx - 28, striker1_y - 28, cx + 28, striker1_y + 28], fill=(255, 42, 109, 30))
    draw.ellipse([cx - 24, striker1_y - 24, cx + 24, striker1_y + 24], fill=(180, 20, 60), outline=(255, 120, 160), width=2)
    draw.ellipse([cx - 11, striker1_y - 11, cx + 11, striker1_y + 11], fill=(255, 60, 100), outline=(255, 210, 230), width=1)
    draw.rounded_rectangle([cx - 16, p1_box[1] + 2, cx + 16, p1_box[1] + 4], radius=2, fill=(60, 75, 105))

    # ------------------ Phone 2 (Bottom Device) ------------------
    draw.rounded_rectangle(p2_box, radius=22, fill=(24, 30, 44), outline=(70, 85, 120), width=2)
    s2 = [p2_box[0] + 6, p2_box[1] + 4, p2_box[2] - 6, p2_box[3] - 6]
    draw.rounded_rectangle(s2, radius=16, fill=(8, 12, 22))
    draw.rounded_rectangle(s2, radius=16, outline=(0, 229, 255, 90), width=2)
    draw.arc([cx - 44, s2[3] - 46, cx + 44, s2[3] + 18], start=180, end=360, fill=(0, 229, 255), width=3)
    striker2_y = s2[3] - 55
    draw.ellipse([cx - 28, striker2_y - 28, cx + 28, striker2_y + 28], fill=(0, 229, 255, 30))
    draw.ellipse([cx - 24, striker2_y - 24, cx + 24, striker2_y + 24], fill=(0, 140, 185), outline=(100, 240, 255), width=2)
    draw.ellipse([cx - 11, striker2_y - 11, cx + 11, striker2_y + 11], fill=(0, 229, 255), outline=(220, 255, 255), width=1)
    draw.rounded_rectangle([cx - 22, p2_box[3] - 4, cx + 22, p2_box[3] - 2], radius=2, fill=(60, 75, 105))

    # ------------------ Center Seam Table Graphics ------------------
    seam_center_y = top_y + ph + gap // 2
    draw.arc([cx - 40, seam_center_y - 40, cx + 40, seam_center_y + 40], start=180, end=360, fill=(255, 42, 109, 180), width=2)
    draw.arc([cx - 40, seam_center_y - 40, cx + 40, seam_center_y + 40], start=0, end=180, fill=(0, 229, 255, 180), width=2)
    draw.line([(s1[0], s1[3]), (s1[2], s1[3])], fill=(255, 42, 109, 220), width=2)
    draw.line([(s2[0], s2[1]), (s2[2], s2[1])], fill=(0, 229, 255, 220), width=2)

    # Translucent glass reflection sheen
    sheen = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    sheen_draw = ImageDraw.Draw(sheen)
    sheen_draw.polygon([(cx - 70, top_y), (cx + 10, top_y), (cx - 30, top_y + total_h), (cx - 110, top_y + total_h)], fill=(255, 255, 255, 15))
    banner = Image.alpha_composite(banner, sheen)
    draw = ImageDraw.Draw(banner)

    # Dynamic Motion Trail from bottom to top crossing the seam
    puck_x = cx + 18
    puck_y = seam_center_y - 4
    for i in range(18):
        t = i / 18.0
        tx = (cx - 8) + (puck_x - (cx - 8)) * t
        ty = (striker2_y - 18) + (puck_y - (striker2_y - 18)) * t
        r = int(5 + 8 * t)
        alpha = int(190 * (t ** 1.6))
        draw.ellipse([tx - r, ty - r, tx + r, ty + r], fill=(255, 175, 0, alpha))

    # Glowing Puck crossing the seam!
    draw.ellipse([puck_x - 22, puck_y - 22, puck_x + 22, puck_y + 22], fill=(255, 160, 0, 60))
    draw.ellipse([puck_x - 16, puck_y - 16, puck_x + 16, puck_y + 16], fill=(255, 180, 0, 110))
    draw.ellipse([puck_x - 13, puck_y - 13, puck_x + 13, puck_y + 13], fill=(255, 220, 0, 230), outline=(255, 255, 255), width=2)
    draw.ellipse([puck_x - 6, puck_y - 6, puck_x + 6, puck_y + 6], fill=(255, 255, 240))

    # Sparks around puck
    sparks = [(14, -16), (-12, -14), (18, 10), (-16, 14), (10, 18), (-10, -20), (22, -4), (-20, 6)]
    for dx, dy in sparks:
        draw.line([(puck_x, puck_y), (puck_x + dx, puck_y + dy)], fill=(255, 255, 210, 220), width=2)

    # 4. Left Side Typography & Elements
    font_badge = ImageFont.truetype(FONT_BOLD, 13)
    font_title = ImageFont.truetype(FONT_EXTRABOLD, 46)
    font_sub = ImageFont.truetype(FONT_BOLD, 21)
    font_desc = ImageFont.truetype(FONT_MEDIUM, 16)
    font_tag = ImageFont.truetype(FONT_BOLD, 12)

    if lang == "en":
        badge_text = "DUAL SCREEN MULTIPLAYER"
        title_text = "AIR HOCKEY 2P"
        sub_text = "EXTENDED TABLE ACROSS 2 PHONES"
        desc_text = "Place two phones top-to-top to play on one huge table!\n100% offline via local Wi-Fi or Hotspot."
        tag1 = "100% OFFLINE"
        tag2 = "ZERO LAG WI-FI"
        tag3 = "INSTANT QR PAIR"
    elif lang == "es":
        badge_text = "MULTIJUGADOR EN 2 CELULARES"
        title_text = "AIR HOCKEY 2P"
        sub_text = "MESA EXTENDIDA EN 2 PANTALLAS"
        desc_text = "Conecta dos celulares punta a punta por Wi-Fi o Hotspot.\nEl disco pasa fluidamente de una pantalla a la otra!"
        tag1 = "100% SIN INTERNET"
        tag2 = "CERO LAG WI-FI"
        tag3 = "CONEXION POR QR"
    else: # pt
        badge_text = "MULTIJOGADOR EM 2 CELULARES"
        title_text = "AIR HOCKEY 2P"
        sub_text = "MESA ESTENDIDA EM 2 TELAS"
        desc_text = "Encoste dois celulares topo-a-topo para jogar em tela dupla!\n100% offline via Wi-Fi local ou Ponto de Acesso."
        tag1 = "100% OFFLINE"
        tag2 = "ZERO LAG WI-FI"
        tag3 = "PAREAMENTO POR QR"

    margin_x = 64

    # Category Capsule (Pill) with dynamic width
    badge_w = int(draw.textlength(badge_text, font=font_badge))
    pill_w = badge_w + 32
    pill_h = 32
    pill_y = 60
    draw.rounded_rectangle([margin_x, pill_y, margin_x + pill_w, pill_y + pill_h], radius=16, fill=(16, 28, 52), outline=(0, 229, 255), width=2)
    text_x = margin_x + 16
    text_y = pill_y + 8
    draw.text((text_x, text_y), badge_text, font=font_badge, fill=(0, 240, 255))

    # Main Title
    draw.text((margin_x, 112), title_text, font=font_title, fill=(255, 255, 255))

    # Subtitle (Cyan)
    draw.text((margin_x, 178), sub_text, font=font_sub, fill=(0, 229, 255))

    # Description
    lines = desc_text.split("\n")
    cur_y = 226
    for line in lines:
        draw.text((margin_x, cur_y), line, font=font_desc, fill=(185, 205, 230))
        cur_y += 26

    # Tags (Pills)
    tags = [tag1, tag2, tag3]
    tx = margin_x
    ty = 320
    for tag in tags:
        tw = int(draw.textlength(tag, font=font_tag)) + 24
        draw.rounded_rectangle([tx, ty, tx + tw, ty + 36], radius=10, fill=(24, 30, 48), outline=(255, 42, 109, 180), width=1)
        draw.text((tx + 12, ty + 10), tag, font=font_tag, fill=(255, 190, 215))
        tx += tw + 14

    # Convert to pure RGB for Google Play Store compliance
    rgb_banner = Image.new("RGB", (canvas_w, canvas_h), (10, 14, 26))
    rgb_banner.paste(banner, (0, 0), banner)
    return rgb_banner


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
    # English (default)
    fg_en = generate_feature_graphic("en")
    fg_en_root = os.path.join(STORE_DIR, "feature_graphic_1024x500.png")
    fg_en.save(fg_en_root, "PNG", optimize=True)
    os.makedirs(os.path.join(STORE_DIR, "feature_graphic/en"), exist_ok=True)
    fg_en.save(os.path.join(STORE_DIR, "feature_graphic/en/feature_graphic_1024x500.png"), "PNG", optimize=True)
    fg_en.save(os.path.join(BASE_DIR, "web/feature_graphic.png"), "PNG", optimize=True)
    print(f"✓ Saved Feature Graphic (EN / Default): {fg_en_root}")

    # Portuguese
    fg_pt = generate_feature_graphic("pt")
    os.makedirs(os.path.join(STORE_DIR, "feature_graphic/pt"), exist_ok=True)
    fg_pt_path = os.path.join(STORE_DIR, "feature_graphic/pt/feature_graphic_1024x500.png")
    fg_pt.save(fg_pt_path, "PNG", optimize=True)
    print(f"✓ Saved Feature Graphic (PT): {fg_pt_path}")

    # Spanish
    fg_es = generate_feature_graphic("es")
    os.makedirs(os.path.join(STORE_DIR, "feature_graphic/es"), exist_ok=True)
    fg_es_path = os.path.join(STORE_DIR, "feature_graphic/es/feature_graphic_1024x500.png")
    fg_es.save(fg_es_path, "PNG", optimize=True)
    print(f"✓ Saved Feature Graphic (ES): {fg_es_path}")


if __name__ == "__main__":
    generate_launcher_icons()
