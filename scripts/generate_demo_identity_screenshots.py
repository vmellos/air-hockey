#!/usr/bin/env python3
"""
generate_demo_identity_screenshots.py (Refined V2)
Renders high-conversion, professional Google Play Store screenshots in English
showcasing the Cyberpunk Neon & Dark Glassmorphism identity:
- Electric Cyan (#00E5FF), Incandescent Flame Red/Orange (#FF5722), Trophy Gold (#FFB400)
- 45° CutCorner dark glassmorphism cards
- High-energy fiery plasma puck & lightning
- Extended table across 2 phones top-to-top
- Procedural golden 5-point stars, touch offset indicator, and clean typography
"""

import math
import os
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H = 1080, 2340  # 19.5:9 Google Play Store standard

BASE_DIR = "/home/vinicius/AndroidStudioProjects/AirHockey"
DOWNLOADS_DIR = "/home/vinicius/Downloads/airhockey/Icon"
OUT_DIR = os.path.join(BASE_DIR, "store_assets", "screenshots", "demo_en")
EN_DIR = os.path.join(BASE_DIR, "store_assets", "screenshots", "en")
os.makedirs(OUT_DIR, exist_ok=True)
os.makedirs(EN_DIR, exist_ok=True)

# Fonts
FONT_EXTRA_BOLD = "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Black.otf"
FONT_BOLD = "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Bold.otf"
FONT_MEDIUM = "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Medium.otf"
if not os.path.exists(FONT_EXTRA_BOLD):
    FONT_EXTRA_BOLD = "/usr/share/fonts/google-droid-sans-fonts/DroidSans-Bold.ttf"
if not os.path.exists(FONT_BOLD):
    FONT_BOLD = "/usr/share/fonts/google-droid-sans-fonts/DroidSans-Bold.ttf"
if not os.path.exists(FONT_MEDIUM):
    FONT_MEDIUM = "/usr/share/fonts/google-droid-sans-fonts/DroidSans.ttf"

f_title = ImageFont.truetype(FONT_EXTRA_BOLD, 56)
f_sub = ImageFont.truetype(FONT_BOLD, 26)
f_card_title = ImageFont.truetype(FONT_EXTRA_BOLD, 36)
f_card_sub = ImageFont.truetype(FONT_MEDIUM, 22)
f_badge = ImageFont.truetype(FONT_BOLD, 20)
f_score = ImageFont.truetype(FONT_EXTRA_BOLD, 64)

# Assets
bg_fiery_path = os.path.join(DOWNLOADS_DIR, "bg_fiery_puck.jpg")
title_banner_path = os.path.join(DOWNLOADS_DIR, "title_banner_championship.png")
icon_path = os.path.join(DOWNLOADS_DIR, "icon_512x512.png")

bg_fiery_img = Image.open(bg_fiery_path).convert("RGBA") if os.path.exists(bg_fiery_path) else None
title_banner_img = Image.open(title_banner_path).convert("RGBA") if os.path.exists(title_banner_path) else None
icon_img = Image.open(icon_path).convert("RGBA") if os.path.exists(icon_path) else None


def draw_star(draw, cx, cy, r_out, r_in, fill, outline=None, width=1):
    """Draws a procedural 5-point star"""
    points = []
    for i in range(10):
        r = r_out if i % 2 == 0 else r_in
        angle = i * math.pi / 5 - math.pi / 2
        points.append((cx + r * math.cos(angle), cy + r * math.sin(angle)))
    draw.polygon(points, fill=fill, outline=outline)


def create_base_poster(title, subtitle, accent_primary, accent_secondary=None):
    """Creates standard 1080x2340 poster with cyber background, ambient glow and typography"""
    if accent_secondary is None:
        accent_secondary = accent_primary

    canvas = Image.new("RGBA", (W, H), (6, 9, 18, 255))

    # Ambient dual radial glow
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    g_draw.ellipse([W // 2 - 450, -150, W // 2 + 450, 480], fill=(accent_primary[0], accent_primary[1], accent_primary[2], 55))
    g_draw.ellipse([W // 2 - 480, 1500, W // 2 + 480, 2450], fill=(accent_secondary[0], accent_secondary[1], accent_secondary[2], 45))
    glow = glow.filter(ImageFilter.GaussianBlur(90))
    canvas = Image.alpha_composite(canvas, glow)

    # Cyber grid
    grid = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    grid_draw = ImageDraw.Draw(grid)
    for y in range(0, H, 36):
        grid_draw.line([(0, y), (W, y)], fill=(80, 140, 220, 12), width=1)
    for x in range(0, W, 36):
        grid_draw.line([(x, 0), (x, H)], fill=(80, 140, 220, 12), width=1)
    canvas = Image.alpha_composite(canvas, grid)

    # Top Typography Header
    draw = ImageDraw.Draw(canvas)
    title_w = draw.textlength(title, font=f_title)
    draw.text(((W - title_w) // 2, 140), title, font=f_title, fill=(255, 255, 255))

    sub_w = draw.textlength(subtitle, font=f_sub)
    draw.text(((W - sub_w) // 2, 238), subtitle, font=f_sub, fill=accent_primary)

    return canvas


def draw_cut_corner_rect(draw, bbox, cut_size, fill=None, outline=None, width=1):
    """Draws a 45° chamfered / cut-corner tactical glassmorphic rectangle"""
    x1, y1, x2, y2 = bbox
    c = cut_size
    pts = [
        (x1 + c, y1),
        (x2 - c, y1),
        (x2, y1 + c),
        (x2, y2 - c),
        (x2 - c, y2),
        (x1 + c, y2),
        (x1, y2 - c),
        (x1, y1 + c)
    ]
    if fill:
        draw.polygon(pts, fill=fill)
    if outline:
        pts_closed = pts + [pts[0]]
        for i in range(len(pts)):
            draw.line([pts_closed[i], pts_closed[i+1]], fill=outline, width=width)


def render_phone_mockup_frame(dev_w=880, dev_h=1860, dev_x=100, dev_y=350, accent_color=(0, 229, 255)):
    """Returns a tuple of (screen_x, screen_y, screen_w, screen_h, frame_canvas)"""
    frame = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    # Shadow
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sh_draw = ImageDraw.Draw(shadow)
    sh_draw.rounded_rectangle([dev_x - 14, dev_y + 14, dev_x + dev_w + 14, dev_y + dev_h + 36], radius=56, fill=(0, 0, 0, 195))
    shadow = shadow.filter(ImageFilter.GaussianBlur(36))
    frame = Image.alpha_composite(frame, shadow)

    draw = ImageDraw.Draw(frame)
    # Chassis: Dark titanium with clean chamfered border
    draw.rounded_rectangle([dev_x, dev_y, dev_x + dev_w, dev_y + dev_h], radius=52, fill=(18, 24, 38), outline=(65, 80, 110), width=3)

    screen_pad = 16
    screen_x = dev_x + screen_pad
    screen_y = dev_y + screen_pad
    screen_w = dev_w - screen_pad * 2
    screen_h = dev_h - screen_pad * 2

    return screen_x, screen_y, screen_w, screen_h, frame


def apply_phone_finishing(canvas, screen_x, screen_y, screen_w, screen_h, accent_color=(0, 229, 255)):
    draw = ImageDraw.Draw(canvas)
    # Bezel glow border
    draw.rounded_rectangle([screen_x, screen_y, screen_x + screen_w, screen_y + screen_h], radius=38, outline=(accent_color[0], accent_color[1], accent_color[2], 95), width=2)

    # Punch hole
    ch_x = screen_x + screen_w // 2
    ch_y = screen_y + 26
    draw.ellipse([ch_x - 10, ch_y - 10, ch_x + 10, ch_y + 10], fill=(8, 10, 16), outline=(40, 48, 65), width=1)

    # Glass sheen
    sheen = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sheen_draw = ImageDraw.Draw(sheen)
    sheen_draw.polygon([(screen_x, screen_y), (screen_x + 360, screen_y), (screen_x + 120, screen_y + screen_h), (screen_x, screen_y + screen_h)], fill=(255, 255, 255, 12))
    return Image.alpha_composite(canvas, sheen)


# -------------------------------------------------------------
# SCREEN 1: EXTENDED TABLE (DUAL PHONES TOPO-A-TOPO)
# -------------------------------------------------------------
def make_screen_01():
    print("🎨 Gerando Screenshot 01: Dual Phone Extended Table...")
    canvas = create_base_poster(
        "EXTENDED TABLE (2 PHONES)",
        "Connect 2 phones into 1 continuous arena via Wi-Fi • 0% Lag",
        (0, 229, 255), (255, 87, 34)
    )

    pw = 680
    ph = 860
    px = (W - pw) // 2

    top_y = 350
    bot_y = top_y + ph + 24  # 24px physical bezel gap!

    # Draw both phone frames
    for py, border_color in [(top_y, (255, 87, 34)), (bot_y, (0, 229, 255))]:
        sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        sh_draw = ImageDraw.Draw(sh)
        sh_draw.rounded_rectangle([px - 10, py + 10, px + pw + 10, py + ph + 24], radius=44, fill=(0, 0, 0, 190))
        sh = sh.filter(ImageFilter.GaussianBlur(28))
        canvas = Image.alpha_composite(canvas, sh)

        draw = ImageDraw.Draw(canvas)
        draw.rounded_rectangle([px, py, px + pw, py + ph], radius=40, fill=(16, 22, 34), outline=(55, 70, 95), width=3)

    pad = 14
    sw = pw - pad * 2
    sh_h = ph - pad * 2
    top_sx, top_sy = px + pad, top_y + pad
    bot_sx, bot_sy = px + pad, bot_y + pad

    # Carbon fiber table texture helper
    def draw_table_texture(draw, w, h):
        for y in range(0, h, 8):
            draw.line([(0, y), (w, y)], fill=(20, 28, 44), width=1)

    # TOP PHONE: Player 2 (Red half of table)
    top_screen = Image.new("RGBA", (sw, sh_h), (8, 12, 22, 255))
    ts_draw = ImageDraw.Draw(top_screen)
    draw_table_texture(ts_draw, sw, sh_h)

    draw_cut_corner_rect(ts_draw, [18, 18, sw - 18, sh_h + 30], cut_size=36, fill=(10, 16, 28, 230), outline=(255, 87, 34, 190), width=4)
    gw = 260
    gx = (sw - gw) // 2
    ts_draw.arc([gx, -40, gx + gw, 80], start=0, end=180, fill=(255, 87, 34), width=5)
    # Player 2 Striker (Red)
    ts_draw.ellipse([sw // 2 - 50, 160, sw // 2 + 50, 260], fill=(255, 42, 60), outline=(255, 180, 190), width=5)
    ts_draw.ellipse([sw // 2 - 25, 185, sw // 2 + 25, 235], fill=(220, 20, 40), outline=(255, 255, 255), width=2)
    # Center circle
    ts_draw.arc([sw // 2 - 120, sh_h - 120, sw // 2 + 120, sh_h + 120], start=180, end=360, fill=(255, 87, 34), width=4)

    # BOTTOM PHONE: Player 1 (Cyan half of table)
    bot_screen = Image.new("RGBA", (sw, sh_h), (8, 12, 22, 255))
    bs_draw = ImageDraw.Draw(bot_screen)
    draw_table_texture(bs_draw, sw, sh_h)

    draw_cut_corner_rect(bs_draw, [18, -30, sw - 18, sh_h - 18], cut_size=36, fill=(10, 16, 28, 230), outline=(0, 229, 255, 190), width=4)
    bs_draw.arc([sw // 2 - 120, -120, sw // 2 + 120, 120], start=0, end=180, fill=(0, 229, 255), width=4)
    bs_draw.arc([gx, sh_h - 80, gx + gw, sh_h + 40], start=180, end=360, fill=(0, 229, 255), width=5)
    # Player 1 Striker (Cyan)
    bs_draw.ellipse([sw // 2 - 50, sh_h - 260, sw // 2 + 50, sh_h - 160], fill=(0, 200, 255), outline=(180, 245, 255), width=5)
    bs_draw.ellipse([sw // 2 - 25, sh_h - 235, sw // 2 + 25, sh_h - 185], fill=(0, 160, 230), outline=(255, 255, 255), width=2)

    # Masks
    mask = Image.new("L", (sw, sh_h), 0)
    m_draw = ImageDraw.Draw(mask)
    m_draw.rounded_rectangle([0, 0, sw, sh_h], radius=32, fill=255)

    canvas.paste(top_screen, (top_sx, top_sy), mask)
    canvas.paste(bot_screen, (bot_sx, bot_sy), mask)

    # CROSSING FIERY PUCKS & LIGHTNING at the bezel seam
    puck_size = 80
    puck_x = W // 2 - puck_size // 2
    puck_y = top_y + ph - 28

    # Trail & Plasma
    trail = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    tr_draw = ImageDraw.Draw(trail)
    for i in range(14):
        rad = 80 + i * 8
        alpha = max(0, 140 - i * 10)
        tr_draw.ellipse([W // 2 - rad // 2, puck_y + 16 + i * 7, W // 2 + rad // 2, puck_y + 16 + i * 7 + rad // 2], fill=(255, 120, 0, alpha))
        tr_draw.ellipse([W // 2 - rad // 2, puck_y - 20 - i * 6, W // 2 + rad // 2, puck_y - 20 - i * 6 + rad // 2], fill=(255, 80, 20, max(0, alpha - 20)))
    trail = trail.filter(ImageFilter.GaussianBlur(16))
    canvas = Image.alpha_composite(canvas, trail)

    puck_draw = ImageDraw.Draw(canvas)
    puck_draw.ellipse([puck_x - 6, puck_y - 6, puck_x + puck_size + 6, puck_y + puck_size + 6], fill=(255, 200, 50, 250), outline=(255, 255, 200), width=3)
    puck_draw.ellipse([puck_x + 8, puck_y + 8, puck_x + puck_size - 8, puck_y + puck_size - 8], fill=(255, 90, 0), outline=(255, 230, 120), width=2)
    puck_draw.ellipse([puck_x + 22, puck_y + 22, puck_x + puck_size - 22, puck_y + puck_size - 22], fill=(255, 240, 180))

    # Tactical Floating Badges
    badge_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    b_draw = ImageDraw.Draw(badge_layer)
    b1_x1, b1_y1 = 110, 2120
    draw_cut_corner_rect(b_draw, [b1_x1, b1_y1, b1_x1 + 410, b1_y1 + 72], cut_size=16, fill=(15, 22, 38, 240), outline=(0, 229, 255, 210), width=2)
    b_draw.ellipse([b1_x1 + 24, b1_y1 + 28, b1_x1 + 40, b1_y1 + 44], fill=(0, 229, 255))
    b_draw.text((b1_x1 + 52, b1_y1 + 24), "BEZEL GAP COMPENSATED", font=f_badge, fill=(0, 229, 255))

    b2_x1 = b1_x1 + 440
    draw_cut_corner_rect(b_draw, [b2_x1, b1_y1, b2_x1 + 420, b1_y1 + 72], cut_size=16, fill=(15, 22, 38, 240), outline=(255, 87, 34, 210), width=2)
    b_draw.ellipse([b2_x1 + 24, b1_y1 + 28, b2_x1 + 40, b1_y1 + 44], fill=(255, 87, 34))
    b_draw.text((b2_x1 + 52, b1_y1 + 24), "1-SEC QR CODE PAIRING", font=f_badge, fill=(255, 140, 80))

    canvas = Image.alpha_composite(canvas, badge_layer)
    return canvas


# -------------------------------------------------------------
# SCREEN 2: CYBERPUNK ARCADE PHYSICS & FIERY PUCK
# -------------------------------------------------------------
def make_screen_02():
    print("🎨 Gerando Screenshot 02: Cyberpunk Arcade Physics...")
    canvas = create_base_poster(
        "CYBERPUNK ARCADE PHYSICS",
        "Fiery plasma puck, kinetic ricochets & twitch reflexes",
        (255, 87, 34), (0, 229, 255)
    )

    sx, sy, sw, sh, frame = render_phone_mockup_frame(dev_w=880, dev_h=1860, dev_x=100, dev_y=350, accent_color=(255, 87, 34))
    canvas = Image.alpha_composite(canvas, frame)

    screen_img = Image.new("RGBA", (sw, sh), (10, 14, 24, 255))
    if bg_fiery_img:
        # Scale properly so height >= sh
        scale_ratio = max(sw / bg_fiery_img.width, sh / bg_fiery_img.height)
        new_w = int(bg_fiery_img.width * scale_ratio)
        new_h = int(bg_fiery_img.height * scale_ratio)
        arena = bg_fiery_img.resize((new_w, new_h), Image.Resampling.LANCZOS)
        # Center crop into screen
        crop_x = (new_w - sw) // 2
        crop_y = (new_h - sh) // 2
        arena_cropped = arena.crop((crop_x, crop_y, crop_x + sw, crop_y + sh))
        screen_img.paste(arena_cropped, (0, 0))

    # Dark Glassmorphism Score HUD on top with 45° cut corners
    hud = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    h_draw = ImageDraw.Draw(hud)

    hud_w, hud_h = 740, 190
    hud_x = (sw - hud_w) // 2
    hud_y = 60
    draw_cut_corner_rect(h_draw, [hud_x, hud_y, hud_x + hud_w, hud_y + hud_h], cut_size=24, fill=(12, 18, 32, 230), outline=(255, 87, 34, 200), width=2)

    # Scores
    h_draw.text((hud_x + 60, hud_y + 36), "PLAYER 1", font=f_badge, fill=(0, 229, 255))
    h_draw.text((hud_x + 90, hud_y + 70), "4", font=f_score, fill=(0, 229, 255))

    h_draw.text((hud_x + hud_w - 180, hud_y + 36), "AI (HARD)", font=f_badge, fill=(255, 87, 34))
    h_draw.text((hud_x + hud_w - 130, hud_y + 70), "2", font=f_score, fill=(255, 87, 34))

    # Target Goal
    h_draw.text((hud_x + 290, hud_y + 54), "TARGET: 7", font=f_badge, fill=(255, 255, 255))
    h_draw.text((hud_x + 255, hud_y + 98), "SOVEREIGN WIN", font=f_card_sub, fill=(255, 180, 0))

    # Tactile Touch Offset Indicator in bottom half
    t_y = sh - 280
    draw_cut_corner_rect(h_draw, [50, t_y, sw - 50, t_y + 120], cut_size=20, fill=(10, 16, 28, 235), outline=(0, 229, 255, 190), width=2)
    h_draw.text((80, t_y + 24), "ERGONOMIC TOUCH OFFSET", font=f_card_title, fill=(255, 255, 255))
    h_draw.text((80, t_y + 70), "Fingers never obstruct the puck • Full view control", font=f_card_sub, fill=(0, 229, 255))

    screen_img = Image.alpha_composite(screen_img, hud)

    # Mask screen into phone
    mask = Image.new("L", (sw, sh), 0)
    m_draw = ImageDraw.Draw(mask)
    m_draw.rounded_rectangle([0, 0, sw, sh], radius=38, fill=255)
    canvas.paste(screen_img, (sx, sy), mask)

    canvas = apply_phone_finishing(canvas, sx, sy, sw, sh, (255, 87, 34))
    return canvas


# -------------------------------------------------------------
# SCREEN 3: 100-STAGE SOLO CAMPAIGN
# -------------------------------------------------------------
def make_screen_03():
    print("🎨 Gerando Screenshot 03: 100-Stage Solo Campaign...")
    canvas = create_base_poster(
        "100-STAGE SOLO CAMPAIGN",
        "Tactical AI, anti-opponent aim & 3-star ratings",
        (255, 180, 0), (0, 229, 255)
    )

    sx, sy, sw, sh, frame = render_phone_mockup_frame(dev_w=880, dev_h=1860, dev_x=100, dev_y=350, accent_color=(255, 180, 0))
    canvas = Image.alpha_composite(canvas, frame)

    screen_img = Image.new("RGBA", (sw, sh), (8, 12, 22, 255))
    s_draw = ImageDraw.Draw(screen_img)

    cw, ch = 740, 1100
    cx = (sw - cw) // 2
    cy = 240
    draw_cut_corner_rect(s_draw, [cx, cy, cx + cw, cy + ch], cut_size=32, fill=(14, 20, 36, 240), outline=(255, 180, 0, 220), width=3)

    # 3 Glowing Golden Stars (drawn via math polygon)
    star_center_y = cy + 180
    star_x = sw // 2
    draw_star(s_draw, star_x - 120, star_center_y, 48, 22, fill=(255, 190, 0), outline=(255, 240, 150), width=2)
    draw_star(s_draw, star_x, star_center_y - 20, 62, 28, fill=(255, 215, 0), outline=(255, 255, 200), width=2)
    draw_star(s_draw, star_x + 120, star_center_y, 48, 22, fill=(255, 190, 0), outline=(255, 240, 150), width=2)

    # Texts
    st_title = "STAGE 42 CLEARED!"
    st_w = s_draw.textlength(st_title, font=f_card_title)
    s_draw.text(((sw - st_w) // 2, cy + 300), st_title, font=f_card_title, fill=(255, 255, 255))

    st_sub = "Defensive Mastery: 0 Goals Allowed"
    st_sub_w = s_draw.textlength(st_sub, font=f_card_sub)
    s_draw.text(((sw - st_sub_w) // 2, cy + 360), st_sub, font=f_card_sub, fill=(0, 229, 255))

    # Stats Card
    stats_y = cy + 440
    draw_cut_corner_rect(s_draw, [cx + 40, stats_y, cx + cw - 40, stats_y + 260], cut_size=20, fill=(20, 28, 48), outline=(60, 80, 115), width=2)
    s_draw.text((cx + 70, stats_y + 36), "Opponent: Adaptive Expert AI", font=f_card_sub, fill=(220, 230, 245))
    s_draw.text((cx + 70, stats_y + 90), "Max Puck Speed: 2,850 px/s", font=f_card_sub, fill=(255, 87, 34))
    s_draw.text((cx + 70, stats_y + 144), "Bank Shots Converted: 5 / 5", font=f_card_sub, fill=(0, 229, 255))
    s_draw.text((cx + 70, stats_y + 198), "Rating Unlocked: +45 ELO", font=f_card_sub, fill=(255, 200, 0))

    # Next Stage Action Button
    btn_y = cy + 760
    draw_cut_corner_rect(s_draw, [cx + 60, btn_y, cx + cw - 60, btn_y + 110], cut_size=22, fill=(255, 180, 0), outline=(255, 240, 150), width=2)
    btn_txt = "NEXT STAGE ▶"
    btn_w = s_draw.textlength(btn_txt, font=f_card_title)
    s_draw.text(((sw - btn_w) // 2, btn_y + 34), btn_txt, font=f_card_title, fill=(12, 16, 26))

    # Bottom Stage Indicator
    map_y = sh - 280
    draw_cut_corner_rect(s_draw, [50, map_y, sw - 50, map_y + 120], cut_size=20, fill=(12, 18, 32, 235), outline=(255, 180, 0, 190), width=2)
    s_draw.text((80, map_y + 24), "100 LEVELS OF ARCADE MASTERY", font=f_card_title, fill=(255, 255, 255))
    s_draw.text((80, map_y + 70), "Linear difficulty curve • Unique boss battle mechanics", font=f_card_sub, fill=(255, 180, 0))

    # Mask screen
    mask = Image.new("L", (sw, sh), 0)
    m_draw = ImageDraw.Draw(mask)
    m_draw.rounded_rectangle([0, 0, sw, sh], radius=38, fill=255)
    canvas.paste(screen_img, (sx, sy), mask)

    canvas = apply_phone_finishing(canvas, sx, sy, sw, sh, (255, 180, 0))
    return canvas


# -------------------------------------------------------------
# SCREEN 4: SAME-DEVICE SPLIT SCREEN DUEL
# -------------------------------------------------------------
def make_screen_04():
    print("🎨 Gerando Screenshot 04: Split Screen Local Duel...")
    canvas = create_base_poster(
        "SAME-DEVICE SPLIT SCREEN",
        "Face-to-face 2-player battle on a single device",
        (255, 42, 109), (0, 229, 255)
    )

    sx, sy, sw, sh, frame = render_phone_mockup_frame(dev_w=880, dev_h=1860, dev_x=100, dev_y=350, accent_color=(255, 42, 109))
    canvas = Image.alpha_composite(canvas, frame)

    screen_img = Image.new("RGBA", (sw, sh), (8, 12, 22, 255))
    s_draw = ImageDraw.Draw(screen_img)

    # Full table perimeter
    draw_cut_corner_rect(s_draw, [24, 24, sw - 24, sh - 24], cut_size=40, fill=(10, 15, 28), outline=(60, 80, 115), width=3)

    # Dividing center line
    s_draw.line([(24, sh // 2), (sw - 24, sh // 2)], fill=(255, 255, 255, 140), width=3)
    s_draw.ellipse([sw // 2 - 120, sh // 2 - 120, sw // 2 + 120, sh // 2 + 120], outline=(255, 255, 255, 140), width=3)

    # Player 2 Half (Top - Red)
    gw = 280
    gx = (sw - gw) // 2
    s_draw.arc([gx, -40, gx + gw, 90], start=0, end=180, fill=(255, 42, 109), width=6)
    s_draw.ellipse([sw // 2 - 60, 220, sw // 2 + 60, 340], fill=(255, 42, 109), outline=(255, 200, 220), width=6)

    # Player 1 Half (Bottom - Cyan)
    s_draw.arc([gx, sh - 90, gx + gw, sh + 40], start=180, end=360, fill=(0, 229, 255), width=6)
    s_draw.ellipse([sw // 2 - 60, sh - 340, sw // 2 + 60, sh - 220], fill=(0, 229, 255), outline=(200, 245, 255), width=6)

    # Fast Moving Fiery Puck with Ricochet Vectors
    px, py = sw // 2 + 80, sh // 2 - 140
    s_draw.line([(sw // 2, sh - 280), (sw - 60, sh // 2)], fill=(255, 200, 50, 180), width=4)
    s_draw.line([(sw - 60, sh // 2), (px, py)], fill=(255, 200, 50, 220), width=4)

    s_draw.ellipse([px - 36, py - 36, px + 36, py + 36], fill=(255, 180, 0), outline=(255, 255, 220), width=3)
    s_draw.ellipse([px - 14, py - 14, px + 14, py + 14], fill=(255, 240, 180))

    # Clean bottom badge
    touch_y = sh - 180
    draw_cut_corner_rect(s_draw, [60, touch_y, sw - 60, touch_y + 90], cut_size=18, fill=(12, 18, 32, 235), outline=(0, 229, 255, 190), width=2)
    s_draw.ellipse([84, touch_y + 36, 100, touch_y + 52], fill=(0, 229, 255))
    s_draw.text((116, touch_y + 30), "PERFECT MULTITOUCH & ZERO LATENCY", font=f_badge, fill=(0, 229, 255))

    # Mask screen
    mask = Image.new("L", (sw, sh), 0)
    m_draw = ImageDraw.Draw(mask)
    m_draw.rounded_rectangle([0, 0, sw, sh], radius=38, fill=255)
    canvas.paste(screen_img, (sx, sy), mask)

    canvas = apply_phone_finishing(canvas, sx, sy, sw, sh, (255, 42, 109))
    return canvas


# -------------------------------------------------------------
# SCREEN 5: ZERO ADS & 100% OFFLINE (PURE ARCADE PRIVACY)
# -------------------------------------------------------------
def make_screen_05():
    print("🎨 Gerando Screenshot 05: Zero Ads & 100% Offline...")
    canvas = create_base_poster(
        "ZERO ADS • 100% OFFLINE",
        "Pure arcade fliperama • No tracking, logins or subscriptions",
        (0, 229, 255), (255, 180, 0)
    )

    sx, sy, sw, sh, frame = render_phone_mockup_frame(dev_w=880, dev_h=1860, dev_x=100, dev_y=350, accent_color=(0, 229, 255))
    canvas = Image.alpha_composite(canvas, frame)

    screen_img = Image.new("RGBA", (sw, sh), (8, 12, 22, 255))
    s_draw = ImageDraw.Draw(screen_img)

    # Title Banner from Downloads if available
    if title_banner_img:
        tb_w = sw - 120
        tb_h = int(tb_w * title_banner_img.height / title_banner_img.width)
        tb_resized = title_banner_img.resize((tb_w, tb_h), Image.Resampling.LANCZOS)
        screen_img.paste(tb_resized, (60, 80), tb_resized)

    # 4 Tactical Glassmorphism Value Proposition Cards (without emojis, using pill badges)
    cards = [
        ("ZERO INVASIVE ADS", "No video ads, banners or interruptions ever.", (255, 87, 34)),
        ("100% OFFLINE & WI-FI HOTSPOT", "Play anywhere without cellular data or internet.", (0, 229, 255)),
        ("60-120 FPS FLUID RENDERING", "Jetpack Compose Canvas with zero GC pauses.", (255, 180, 0)),
        ("TOTAL PRIVACY & ZERO TRACKING", "No account needed, no telemetry, no tracking.", (0, 229, 255)),
    ]

    card_y = 480
    cw = sw - 100
    cx = 50
    for title, desc, col in cards:
        draw_cut_corner_rect(s_draw, [cx, card_y, cx + cw, card_y + 170], cut_size=20, fill=(14, 20, 36, 235), outline=(col[0], col[1], col[2], 190), width=2)
        # Bullet indicator
        s_draw.ellipse([cx + 32, card_y + 44, cx + 46, card_y + 58], fill=col)
        s_draw.text((cx + 60, card_y + 36), title, font=f_card_title, fill=col)
        s_draw.text((cx + 60, card_y + 96), desc, font=f_card_sub, fill=(210, 220, 235))
        card_y += 210

    # Bottom badge
    b_y = sh - 280
    draw_cut_corner_rect(s_draw, [50, b_y, sw - 50, b_y + 120], cut_size=22, fill=(0, 229, 255), outline=(200, 245, 255), width=2)
    b_txt = "AVAILABLE ON GOOGLE PLAY"
    b_w = s_draw.textlength(b_txt, font=f_card_title)
    s_draw.text(((sw - b_w) // 2, b_y + 36), b_txt, font=f_card_title, fill=(10, 15, 26))

    # Mask screen
    mask = Image.new("L", (sw, sh), 0)
    m_draw = ImageDraw.Draw(mask)
    m_draw.rounded_rectangle([0, 0, sw, sh], radius=38, fill=255)
    canvas.paste(screen_img, (sx, sy), mask)

    canvas = apply_phone_finishing(canvas, sx, sy, sw, sh, (0, 229, 255))
    return canvas


def main():
    print("==================================================")
    print("  Generating New Identity Screenshots (English V2)")
    print(f"  Target Resolution: {W}x{H} (19.5:9)")
    print("==================================================")

    screens = [
        ("01_extended_table.png", make_screen_01),
        ("02_split_screen.png", make_screen_02),
        ("03_solo_game.png", make_screen_03),
        ("04_leaderboard.png", make_screen_04),
        ("05_settings.png", make_screen_05),
    ]

    for filename, func in screens:
        img = func()
        # Save to demo_en
        p1 = os.path.join(OUT_DIR, filename)
        img.save(p1, "PNG", optimize=True)
        # Also update en/
        p2 = os.path.join(EN_DIR, filename)
        img.save(p2, "PNG", optimize=True)
        print(f"✓ Saved: {p1}")

    print("==================================================")
    print(f"🎉 All 5 demonstration screenshots generated in:\n➡️ {OUT_DIR}")
    print("==================================================")


if __name__ == "__main__":
    main()
