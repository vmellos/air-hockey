#!/usr/bin/env python3
"""
generate-store-screenshots.py
Captures and formats industry-standard Play Store & App Store screenshots for Air Hockey 2P.

Outputs:
- store_assets/screenshots/raw/ (Raw 1080x2340 unedited screen captures)
- store_assets/screenshots/en/  (English ASO transformed screenshots - Default)
- store_assets/screenshots/pt/  (Portuguese ASO transformed screenshots)
- store_assets/screenshots/es/  (Spanish ASO transformed screenshots)
- store_assets/screenshots/     (Root copies of English screenshots)
"""

import os
import sys
import subprocess
from PIL import Image, ImageDraw, ImageFont, ImageFilter

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STORE_DIR = os.path.join(BASE_DIR, "store_assets")
SHOTS_DIR = os.path.join(STORE_DIR, "screenshots")
RAW_DIR = os.path.join(SHOTS_DIR, "raw")

FONT_EXTRABOLD = "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-ExtraBold.otf"
FONT_BOLD = "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Bold.otf"
FONT_MEDIUM = "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Medium.otf"
if not os.path.exists(FONT_EXTRABOLD):
    FONT_EXTRABOLD = "/usr/share/fonts/google-droid-sans-fonts/DroidSans-Bold.ttf"
if not os.path.exists(FONT_BOLD):
    FONT_BOLD = "/usr/share/fonts/google-droid-sans-fonts/DroidSans-Bold.ttf"
if not os.path.exists(FONT_MEDIUM):
    FONT_MEDIUM = "/usr/share/fonts/google-droid-sans-fonts/DroidSans.ttf"

W, H = 1080, 2340

SCREENSHOT_SPECS = [
    {
        "id": "01_extended_table",
        "file": "01_extended_table.png",
        "accent": (0, 229, 255),  # Cyan
        "en": {
            "title": "EXTENDED TABLE",
            "sub": "Connect 2 phones into 1 giant table via Wi-Fi"
        },
        "pt": {
            "title": "MESA ESTENDIDA",
            "sub": "Junte 2 celulares em uma única mesa via Wi-Fi"
        },
        "es": {
            "title": "MESA EXTENDIDA",
            "sub": "Une 2 celulares en una sola mesa por Wi-Fi"
        }
    },
    {
        "id": "02_split_screen",
        "file": "02_split_screen.png",
        "accent": (255, 42, 109),  # Crimson / Pink
        "en": {
            "title": "SPLIT SCREEN DUEL",
            "sub": "2 players face-to-face on a single device"
        },
        "pt": {
            "title": "MESA DIVIDIDA",
            "sub": "2 jogadores frente a frente no mesmo celular"
        },
        "es": {
            "title": "PANTALLA DIVIDIDA",
            "sub": "2 jugadores cara a cara en un solo dispositivo"
        }
    },
    {
        "id": "03_solo_game",
        "file": "03_solo_game.png",
        "accent": (255, 180, 0),  # Amber
        "en": {
            "title": "SOLO VS SMART AI",
            "sub": "Realistic physics, ricochets & adaptive AI"
        },
        "pt": {
            "title": "SOLO CONTRA A IA",
            "sub": "Física realista, jogadas de tabela e IA esperta"
        },
        "es": {
            "title": "MODO SOLO VS IA",
            "sub": "Física realista, rebotes y dificultad adaptable"
        }
    },
    {
        "id": "04_leaderboard",
        "file": "04_leaderboard.png",
        "accent": (0, 229, 255),  # Cyan
        "en": {
            "title": "ELO LEADERBOARD",
            "sub": "Track skill rating, win streaks & match stats"
        },
        "pt": {
            "title": "RANKING ELO",
            "sub": "Acompanhe seu nível, vitórias e estatísticas"
        },
        "es": {
            "title": "RANKING ELO",
            "sub": "Sigue tu nivel, victorias y estadísticas"
        }
    },
    {
        "id": "05_settings",
        "file": "05_settings.png",
        "accent": (255, 42, 109),  # Crimson
        "en": {
            "title": "PRECISION CONTROLS",
            "sub": "Touch offset, puck speed and zero ads"
        },
        "pt": {
            "title": "CONTROLE ERGONÔMICO",
            "sub": "Deslocamento de toque, velocidade e sem anúncios"
        },
        "es": {
            "title": "CONTROL ERGONÓMICO",
            "sub": "Desplazamiento táctil, velocidad y sin anuncios"
        }
    }
]


def render_screenshot(raw_path, title, subtitle, accent_color):
    """
    Renders an industry-standard ASO screenshot with:
    - Top header: Bold uppercase title + descriptive subtitle
    - Device mockup: Realistic bezel, curved corners, camera punch-hole, glass sheen, drop shadow
    - Deep cyberpunk neon background with ambient glow
    """
    canvas = Image.new("RGBA", (W, H), (10, 14, 26, 255))

    # Ambient radial lighting
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    g_draw.ellipse([W // 2 - 420, -120, W // 2 + 420, 460], fill=(accent_color[0], accent_color[1], accent_color[2], 45))
    g_draw.ellipse([W // 2 - 460, 1450, W // 2 + 460, 2400], fill=(accent_color[0], accent_color[1], accent_color[2], 35))
    glow = glow.filter(ImageFilter.GaussianBlur(80))
    canvas = Image.alpha_composite(canvas, glow)

    # Ambient tech grid
    grid = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    grid_draw = ImageDraw.Draw(grid)
    for y in range(0, H, 36):
        grid_draw.line([(0, y), (W, y)], fill=(120, 160, 220, 12), width=1)
    for x in range(0, W, 36):
        grid_draw.line([(x, 0), (x, H)], fill=(120, 160, 220, 12), width=1)
    canvas = Image.alpha_composite(canvas, grid)

    # Typography Header
    draw = ImageDraw.Draw(canvas)
    f_title = ImageFont.truetype(FONT_EXTRABOLD, 60)
    f_sub = ImageFont.truetype(FONT_BOLD, 28)

    title_w = draw.textlength(title, font=f_title)
    draw.text(((W - title_w) // 2, 150), title, font=f_title, fill=(255, 255, 255))

    sub_w = draw.textlength(subtitle, font=f_sub)
    draw.text(((W - sub_w) // 2, 248), subtitle, font=f_sub, fill=accent_color)

    # Load raw image
    raw = Image.open(raw_path).convert("RGBA")

    # Smartphone Mockup Dimensions
    dev_w = 880
    dev_h = 1860
    dev_x = (W - dev_w) // 2
    dev_y = 380

    # Drop shadow
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sh_draw = ImageDraw.Draw(shadow)
    sh_draw.rounded_rectangle([dev_x - 14, dev_y + 14, dev_x + dev_w + 14, dev_y + dev_h + 36], radius=56, fill=(0, 0, 0, 175))
    shadow = shadow.filter(ImageFilter.GaussianBlur(34))
    canvas = Image.alpha_composite(canvas, shadow)

    draw = ImageDraw.Draw(canvas)

    # Smartphone Outer Chassis (Titanium dark border)
    draw.rounded_rectangle([dev_x, dev_y, dev_x + dev_w, dev_y + dev_h], radius=52, fill=(24, 30, 44), outline=(75, 90, 120), width=3)

    # Screen Bezel
    screen_pad = 16
    screen_x = dev_x + screen_pad
    screen_y = dev_y + screen_pad
    screen_w = dev_w - screen_pad * 2
    screen_h = dev_h - screen_pad * 2

    # Resize raw screenshot to fit inner screen
    raw_resized = raw.resize((screen_w, screen_h), Image.Resampling.LANCZOS)

    # Rounded mask for screen corners
    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    m_draw = ImageDraw.Draw(screen_mask)
    m_draw.rounded_rectangle([0, 0, screen_w, screen_h], radius=38, fill=255)

    # Paste raw screenshot inside screen
    canvas.paste(raw_resized, (screen_x, screen_y), screen_mask)

    # Screen outline highlight
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle([screen_x, screen_y, screen_x + screen_w, screen_y + screen_h], radius=38, outline=(accent_color[0], accent_color[1], accent_color[2], 75), width=2)

    # Camera punch hole
    ch_x = W // 2
    ch_y = screen_y + 26
    draw.ellipse([ch_x - 10, ch_y - 10, ch_x + 10, ch_y + 10], fill=(12, 14, 20), outline=(40, 48, 65), width=1)

    # Glass reflection sheen
    sheen = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sheen_draw = ImageDraw.Draw(sheen)
    sheen_draw.polygon([(screen_x, screen_y), (screen_x + 360, screen_y), (screen_x + 120, screen_y + screen_h), (screen_x, screen_y + screen_h)], fill=(255, 255, 255, 12))
    canvas = Image.alpha_composite(canvas, sheen)

    # Convert to pure RGB
    final_rgb = Image.new("RGB", (W, H), (10, 14, 26))
    final_rgb.paste(canvas, (0, 0), canvas)
    return final_rgb


def main():
    os.makedirs(RAW_DIR, exist_ok=True)
    for lang in ["en", "pt", "es"]:
        os.makedirs(os.path.join(SHOTS_DIR, lang), exist_ok=True)

    print("==================================================")
    print("  Generating Industry Standard ASO Screenshots")
    print(f"  Target Resolution: {W}x{H} (19.5:9)")
    print("==================================================")

    # Verify raw screenshots exist
    for spec in SCREENSHOT_SPECS:
        raw_file = os.path.join(RAW_DIR, spec["file"])
        if not os.path.exists(raw_file):
            print(f"Error: Missing raw capture at {raw_file}")
            sys.exit(1)

    # Generate screenshots for each language with matching in-app localized capture
    for spec in SCREENSHOT_SPECS:
        accent = spec["accent"]

        for lang in ["en", "pt", "es"]:
            localized_raw = os.path.join(RAW_DIR, f"{lang}_{spec['file']}")
            raw_path = localized_raw if os.path.exists(localized_raw) else os.path.join(RAW_DIR, spec["file"])
            copy = spec[lang]
            img = render_screenshot(raw_path, copy["title"], copy["sub"], accent)
            out_path = os.path.join(SHOTS_DIR, lang, spec["file"])
            img.save(out_path, "PNG", optimize=True)
            print(f"✓ [{lang.upper()}] Generated: {out_path} (UI: {os.path.basename(raw_path)})")

        # Also copy English (default) to root screenshots directory
        en_raw = os.path.join(RAW_DIR, f"en_{spec['file']}")
        raw_path = en_raw if os.path.exists(en_raw) else os.path.join(RAW_DIR, spec["file"])
        root_path = os.path.join(SHOTS_DIR, spec["file"])
        img_en = render_screenshot(raw_path, spec["en"]["title"], spec["en"]["sub"], accent)
        img_en.save(root_path, "PNG", optimize=True)

    print("==================================================")
    print(f"  All 15 localized screenshots successfully generated in {SHOTS_DIR}!")
    print("==================================================")


if __name__ == "__main__":
    main()
