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
    Left: Copywriting & Localized Badges/Pills (True claims, no "Zero Ads")
    Right: Stunning AI-rendered 3D Air Hockey Arena with neon split-screen duel!
    """
    canvas_w, canvas_h = 1024, 500
    banner = Image.new("RGBA", (canvas_w, canvas_h), (10, 14, 26, 255))

    # 1. Composite 3D Arena Artwork (Right Side)
    arena_path = os.path.join(STORE_DIR, "feature_graphic_arena_3d.jpg")
    if os.path.exists(arena_path):
        ai_img = Image.open(arena_path).convert("RGBA")
        scale_h = canvas_h / float(ai_img.size[1])
        new_w = int(ai_img.size[0] * scale_h)
        ai_scaled = ai_img.resize((new_w, canvas_h), Image.Resampling.LANCZOS)

        # Smooth horizontal alpha fade on the left edge to blend seamlessly into solid background
        fade_len = 320
        mask = Image.new("L", (new_w, canvas_h), 255)
        mask_draw = ImageDraw.Draw(mask)
        for x in range(fade_len):
            alpha = int(255 * (x / float(fade_len)) ** 1.6)
            mask_draw.line([(x, 0), (x, canvas_h)], fill=alpha)
        ai_scaled.putalpha(mask)
        banner.paste(ai_scaled, (canvas_w - new_w, 0), ai_scaled)

    # 2. Ambient Lighting Layer
    glow = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    # Left ambient soft blue glow behind copywriting
    glow_draw.ellipse([-80, 60, 440, 580], fill=(0, 100, 255, 30))
    # Subtle cyan accent behind title
    glow_draw.ellipse([80, 100, 320, 240], fill=(0, 229, 255, 18))
    glow = glow.filter(ImageFilter.GaussianBlur(55))
    banner = Image.alpha_composite(banner, glow)

    # 3. Subtle Tech Grid Pattern (Left side)
    grid = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    grid_draw = ImageDraw.Draw(grid)
    for y in range(0, canvas_h, 24):
        grid_draw.line([(0, y), (460, y)], fill=(120, 160, 220, 10), width=1)
    for x in range(0, 460, 24):
        grid_draw.line([(x, 0), (x, canvas_h)], fill=(120, 160, 220, 10), width=1)
    banner = Image.alpha_composite(banner, grid)

    draw = ImageDraw.Draw(banner)

    # 4. Typography & Badges
    font_badge = ImageFont.truetype(FONT_BOLD, 13)
    font_title = ImageFont.truetype(FONT_EXTRABOLD, 46)
    font_sub = ImageFont.truetype(FONT_BOLD, 20)
    font_desc = ImageFont.truetype(FONT_MEDIUM, 15)
    font_tag = ImageFont.truetype(FONT_BOLD, 12)

    if lang == "en":
        badge_text = "NEW 3D ARENAS & SPLIT SCREEN"
        title_text = "AIR HOCKEY 2P"
        sub_text = "3D ARENAS ON SAME DEVICE OR WI-FI"
        desc_text = "Battle head-to-head on split screen or connect 2 phones!\n11 futuristic 3D arenas, 100 stages & 100% offline."
        row1_tags = ["11 3D COURTS", "SPLIT SCREEN"]
        row2_tags = ["100 STAGES", "100% OFFLINE"]
    elif lang == "es":
        badge_text = "NUEVAS ARENAS 3D Y PANTALLA DIVIDIDA"
        title_text = "AIR HOCKEY 2P"
        sub_text = "ARENAS 3D EN UN SOLO CELULAR O WI-FI"
        desc_text = "¡Duelo de 2 jugadores en pantalla dividida o por Wi-Fi!\n11 arenas 3D futuristas, 100 niveles y 100% offline."
        row1_tags = ["11 ARENAS 3D", "PANTALLA DIVIDIDA"]
        row2_tags = ["100 NIVELES", "100% OFFLINE"]
    else:  # pt
        badge_text = "NOVO MOTOR 3D & SPLIT SCREEN"
        title_text = "AIR HOCKEY 2P"
        sub_text = "ARENAS 3D NO MESMO APARELHO OU WI-FI"
        desc_text = "Duelo 2 jogadores na tela dividida ou conecte 2 celulares!\n11 arenas 3D imersivas, 100 fases e 100% offline."
        row1_tags = ["11 ARENAS 3D", "MESA DIVIDIDA"]
        row2_tags = ["100 FASES", "100% OFFLINE"]

    margin_x = 60

    # Category Capsule (Pill) with dynamic width
    badge_w = int(draw.textlength(badge_text, font=font_badge))
    pill_w = badge_w + 32
    pill_h = 32
    pill_y = 54
    draw.rounded_rectangle([margin_x, pill_y, margin_x + pill_w, pill_y + pill_h], radius=16, fill=(16, 28, 52), outline=(0, 229, 255), width=2)
    draw.text((margin_x + 16, pill_y + 8), badge_text, font=font_badge, fill=(0, 240, 255))

    # Main Title with subtle shadow
    draw.text((margin_x + 2, 106), title_text, font=font_title, fill=(0, 100, 255, 120))
    draw.text((margin_x, 104), title_text, font=font_title, fill=(255, 255, 255))

    # Subtitle (Cyan)
    draw.text((margin_x, 168), sub_text, font=font_sub, fill=(0, 229, 255))

    # Description
    lines = desc_text.split("\n")
    cur_y = 212
    for line in lines:
        draw.text((margin_x, cur_y), line, font=font_desc, fill=(185, 205, 230))
        cur_y += 24

    # Feature Pills (2 Rows of 2 Pills)
    # Row 1 (Crimson / Player 2 theme)
    tx = margin_x
    ty = 295
    for tag in row1_tags:
        tw = int(draw.textlength(tag, font=font_tag)) + 24
        draw.rounded_rectangle([tx, ty, tx + tw, ty + 34], radius=10, fill=(24, 30, 48), outline=(255, 42, 109, 200), width=1)
        draw.text((tx + 12, ty + 9), tag, font=font_tag, fill=(255, 200, 225))
        tx += tw + 12

    # Row 2 (Cyan / Player 1 theme)
    tx = margin_x
    ty = 340
    for tag in row2_tags:
        tw = int(draw.textlength(tag, font=font_tag)) + 24
        draw.rounded_rectangle([tx, ty, tx + tw, ty + 34], radius=10, fill=(18, 28, 48), outline=(0, 229, 255, 180), width=1)
        draw.text((tx + 12, ty + 9), tag, font=font_tag, fill=(180, 240, 255))
        tx += tw + 12

    # Convert to pure RGB for Google Play Store compliance (no transparency)
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
