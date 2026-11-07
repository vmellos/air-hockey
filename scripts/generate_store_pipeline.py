#!/usr/bin/env python3
"""
generate_store_pipeline.py
Orquestrador mestre dos ativos visuais oficiais da Google Play Store para Air Hockey 2P.
- Renderiza as 6 Capturas Oficiais de Loja (1080x2340 px) em 3 idiomas (PT, EN, ES)
- Diagramação especial de 2 celulares topo-a-topo para a Tela 01 (Mesa Estendida)
- Mockups 3D de titânio escuro com glass sheen para as Telas 02 a 06
- Gera a Imagem de Destaque oficial (1024x500 px) em PT, EN, ES
"""

import json
import math
import os
import subprocess
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H = 1080, 2340
BASE_DIR = "/home/vinicius/AndroidStudioProjects/AirHockey"
STORE_DIR = os.path.join(BASE_DIR, "store_assets")
SHOTS_DIR = os.path.join(STORE_DIR, "screenshots")
RAW_DIR = os.path.join(SHOTS_DIR, "raw")
CONFIG_PATH = os.path.join(STORE_DIR, "screenshots.json")

# Fontes
def find_font(candidates):
    for p in candidates:
        if os.path.exists(p):
            return p
    return None

FONT_TITLE = find_font([
    "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Black.otf",
    "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-ExtraBold.otf",
    "/usr/share/fonts/google-droid-sans-fonts/DroidSans-Bold.ttf",
])
FONT_SUB = find_font([
    "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Bold.otf",
    "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Medium.otf",
    "/usr/share/fonts/google-droid-sans-fonts/DroidSans.ttf",
])
FONT_TAG = find_font([
    "/usr/share/fonts/julietaula-montserrat-fonts/Montserrat-Bold.otf",
    "/usr/share/fonts/google-droid-sans-fonts/DroidSans-Bold.ttf",
])

f_title = ImageFont.truetype(FONT_TITLE, 56) if FONT_TITLE else ImageFont.load_default()
f_sub = ImageFont.truetype(FONT_SUB, 26) if FONT_SUB else ImageFont.load_default()
f_tag = ImageFont.truetype(FONT_TAG, 20) if FONT_TAG else ImageFont.load_default()


def parse_color(c):
    if isinstance(c, (list, tuple)) and len(c) >= 3:
        return tuple(c[:3])
    if isinstance(c, str):
        c = c.lstrip("#")
        if len(c) == 6:
            return tuple(int(c[i:i+2], 16) for i in (0, 2, 4))
    return (0, 229, 255)


def create_base_poster(title, subtitle, accent_primary, accent_secondary=None):
    if accent_secondary is None:
        accent_secondary = accent_primary

    canvas = Image.new("RGBA", (W, H), (7, 10, 20, 255))

    # Glow radial ambiente duplo
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    g_draw.ellipse([W // 2 - 460, -140, W // 2 + 460, 480], fill=(accent_primary[0], accent_primary[1], accent_primary[2], 50))
    g_draw.ellipse([W // 2 - 480, 1480, W // 2 + 480, 2450], fill=(accent_secondary[0], accent_secondary[1], accent_secondary[2], 40))
    glow = glow.filter(ImageFilter.GaussianBlur(85))
    canvas = Image.alpha_composite(canvas, glow)

    # Grade tecnológica sutil
    grid = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    grid_draw = ImageDraw.Draw(grid)
    for y in range(0, H, 36):
        grid_draw.line([(0, y), (W, y)], fill=(120, 160, 220, 12), width=1)
    for x in range(0, W, 36):
        grid_draw.line([(x, 0), (x, H)], fill=(120, 160, 220, 12), width=1)
    canvas = Image.alpha_composite(canvas, grid)

    # Tipografia do cabeçalho
    draw = ImageDraw.Draw(canvas)
    tw = draw.textlength(title, font=f_title)
    draw.text(((W - tw) // 2, 135), title, font=f_title, fill=(255, 255, 255))

    sw = draw.textlength(subtitle, font=f_sub)
    draw.text(((W - sw) // 2, 230), subtitle, font=f_sub, fill=accent_primary)

    return canvas


def draw_cut_corner_rect(draw, box, cut_size=20, fill=None, outline=None, width=1):
    x1, y1, x2, y2 = box
    c = cut_size
    pts = [
        (x1 + c, y1), (x2 - c, y1),
        (x2, y1 + c), (x2, y2 - c),
        (x2 - c, y2), (x1 + c, y2),
        (x1, y2 - c), (x1, y1 + c)
    ]
    draw.polygon(pts, fill=fill, outline=outline)
    if outline and width > 1:
        for i in range(len(pts)):
            p1 = pts[i]
            p2 = pts[(i + 1) % len(pts)]
            draw.line([p1, p2], fill=outline, width=width)


def render_dual_phone_screen_01(title, subtitle, lang="pt"):
    """
    Renderiza o pôster mestre da Tela 01: 2 smartphones topo-a-topo
    com a arena contínua e disco flamejante cruzando o vão físico eletrizado.
    """
    canvas = create_base_poster(title, subtitle, (0, 229, 255), (255, 87, 34))

    pw = 680
    ph = 860
    px = (W - pw) // 2
    top_y = 350
    bot_y = top_y + ph + 24  # vão físico padronizado de 8mm / 24px

    # Sombras dos dois aparelhos
    for py in [top_y, bot_y]:
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

    def draw_table_texture(draw, w, h):
        for y in range(0, h, 8):
            draw.line([(0, y), (w, y)], fill=(20, 28, 44), width=1)

    # 1. APARELHO SUPERIOR (Jogador 2 / Vermelho Neon)
    top_screen = Image.new("RGBA", (sw, sh_h), (8, 12, 22, 255))
    ts_draw = ImageDraw.Draw(top_screen)
    draw_table_texture(ts_draw, sw, sh_h)
    draw_cut_corner_rect(ts_draw, [18, 18, sw - 18, sh_h + 30], cut_size=36, fill=(10, 16, 28, 230), outline=(255, 87, 34, 190), width=4)
    gw = 260
    gx = (sw - gw) // 2
    ts_draw.arc([gx, -40, gx + gw, 80], start=0, end=180, fill=(255, 87, 34), width=5)
    # Rebatedor P2
    ts_draw.ellipse([sw // 2 - 50, 160, sw // 2 + 50, 260], fill=(255, 42, 60), outline=(255, 180, 190), width=5)
    ts_draw.ellipse([sw // 2 - 25, 185, sw // 2 + 25, 235], fill=(220, 20, 40), outline=(255, 255, 255), width=2)
    # Círculo central aberto no topo
    ts_draw.arc([sw // 2 - 120, sh_h - 120, sw // 2 + 120, sh_h + 120], start=180, end=360, fill=(255, 87, 34), width=4)

    # 2. APARELHO INFERIOR (Jogador 1 / Ciano Neon)
    bot_screen = Image.new("RGBA", (sw, sh_h), (8, 12, 22, 255))
    bs_draw = ImageDraw.Draw(bot_screen)
    draw_table_texture(bs_draw, sw, sh_h)
    draw_cut_corner_rect(bs_draw, [18, -30, sw - 18, sh_h - 18], cut_size=36, fill=(10, 16, 28, 230), outline=(0, 229, 255, 190), width=4)
    bs_draw.arc([sw // 2 - 120, -120, sw // 2 + 120, 120], start=0, end=180, fill=(0, 229, 255), width=4)
    bs_draw.arc([gx, sh_h - 80, gx + gw, sh_h + 40], start=180, end=360, fill=(0, 229, 255), width=5)
    # Rebatedor P1
    bs_draw.ellipse([sw // 2 - 50, sh_h - 260, sw // 2 + 50, sh_h - 160], fill=(0, 200, 255), outline=(180, 245, 255), width=5)
    bs_draw.ellipse([sw // 2 - 25, sh_h - 235, sw // 2 + 25, sh_h - 185], fill=(0, 160, 230), outline=(255, 255, 255), width=2)

    # Máscaras de bordas curvas
    mask = Image.new("L", (sw, sh_h), 0)
    m_draw = ImageDraw.Draw(mask)
    m_draw.rounded_rectangle([0, 0, sw, sh_h], radius=32, fill=255)

    canvas.paste(top_screen, (top_sx, top_sy), mask)
    canvas.paste(bot_screen, (bot_sx, bot_sy), mask)

    # DISCO FLAMEJANTE E PLASMA CRUZANDO O VÃO FÍSICO
    puck_size = 80
    puck_x = W // 2 - puck_size // 2
    puck_y = top_y + ph - 28

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

    # Pílulas táticas no rodapé
    tags_by_lang = {
        "pt": ("MESA ESTENDIDA WI-FI", "COMPENSAÇÃO DE VÃO FÍSICO"),
        "en": ("WI-FI EXTENDED TABLE", "BEZEL GAP COMPENSATED"),
        "es": ("MESA EXTENDIDA POR WI-FI", "COMPENSACIÓN DE BORDES")
    }
    tag_left, tag_right = tags_by_lang.get(lang, tags_by_lang["en"])

    badge_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    b_draw = ImageDraw.Draw(badge_layer)
    b1_x1, b1_y1 = 80, 2125
    tw1 = int(b_draw.textlength(tag_left, font=f_tag)) + 60
    draw_cut_corner_rect(b_draw, [b1_x1, b1_y1, b1_x1 + tw1, b1_y1 + 68], cut_size=16, fill=(15, 22, 38, 240), outline=(0, 229, 255, 210), width=2)
    b_draw.ellipse([b1_x1 + 20, b1_y1 + 26, b1_x1 + 36, b1_y1 + 42], fill=(0, 229, 255))
    b_draw.text((b1_x1 + 46, b1_y1 + 22), tag_left, font=f_tag, fill=(0, 229, 255))

    b2_x1 = b1_x1 + tw1 + 24
    b2_y1 = b1_y1
    tw2 = int(b_draw.textlength(tag_right, font=f_tag)) + 60
    if b2_x1 + tw2 < W - 60:
        draw_cut_corner_rect(b_draw, [b2_x1, b2_y1, b2_x1 + tw2, b2_y1 + 68], cut_size=16, fill=(15, 22, 38, 240), outline=(255, 87, 34, 210), width=2)
        b_draw.ellipse([b2_x1 + 20, b2_y1 + 26, b2_x1 + 36, b2_y1 + 42], fill=(255, 87, 34))
        b_draw.text((b2_x1 + 46, b2_y1 + 22), tag_right, font=f_tag, fill=(255, 120, 60))

    canvas = Image.alpha_composite(canvas, badge_layer)

    # Converte para RGB final
    rgb = Image.new("RGB", (W, H), (7, 10, 20))
    rgb.paste(canvas, (0, 0), canvas)
    return rgb


def render_single_phone_mockup(raw_path, title, subtitle, accent_color):
    """
    Renderiza pôster diagramado com 1 smartphone em destaque de titânio escuro:
    - Tela interna com print bruto de alta precisão
    - Punch hole, cantos arredondados, reflexo de vidro e drop shadow realista
    """
    canvas = create_base_poster(title, subtitle, accent_color)

    if not os.path.exists(raw_path):
        raise FileNotFoundError(f"Arquivo bruto não encontrado: {raw_path}")
    raw = Image.open(raw_path).convert("RGBA")

    # Dimensões do Smartphone
    dev_w = 880
    dev_h = 1860
    dev_x = (W - dev_w) // 2
    dev_y = 380

    # Drop shadow suave
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sh_draw = ImageDraw.Draw(shadow)
    sh_draw.rounded_rectangle([dev_x - 14, dev_y + 14, dev_x + dev_w + 14, dev_y + dev_h + 36], radius=56, fill=(0, 0, 0, 180))
    shadow = shadow.filter(ImageFilter.GaussianBlur(34))
    canvas = Image.alpha_composite(canvas, shadow)

    draw = ImageDraw.Draw(canvas)
    # Chassi externo de titânio escuro
    draw.rounded_rectangle([dev_x, dev_y, dev_x + dev_w, dev_y + dev_h], radius=52, fill=(22, 28, 42), outline=(75, 90, 120), width=3)

    # Área da tela interna
    screen_pad = 16
    screen_x = dev_x + screen_pad
    screen_y = dev_y + screen_pad
    screen_w = dev_w - screen_pad * 2
    screen_h = dev_h - screen_pad * 2

    raw_resized = raw.resize((screen_w, screen_h), Image.Resampling.LANCZOS)

    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    m_draw = ImageDraw.Draw(screen_mask)
    m_draw.rounded_rectangle([0, 0, screen_w, screen_h], radius=38, fill=255)

    canvas.paste(raw_resized, (screen_x, screen_y), screen_mask)

    # Borda luminosa neon na tela
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle([screen_x, screen_y, screen_x + screen_w, screen_y + screen_h], radius=38, outline=(accent_color[0], accent_color[1], accent_color[2], 80), width=2)

    # Câmera punch-hole
    ch_x = W // 2
    ch_y = screen_y + 26
    draw.ellipse([ch_x - 10, ch_y - 10, ch_x + 10, ch_y + 10], fill=(12, 14, 20), outline=(40, 48, 65), width=1)

    # Reflexo diagonal de vidro (Glass sheen)
    sheen = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sheen_draw = ImageDraw.Draw(sheen)
    sheen_draw.polygon([(screen_x, screen_y), (screen_x + 360, screen_y), (screen_x + 120, screen_y + screen_h), (screen_x, screen_y + screen_h)], fill=(255, 255, 255, 12))
    canvas = Image.alpha_composite(canvas, sheen)

    rgb = Image.new("RGB", (W, H), (7, 10, 20))
    rgb.paste(canvas, (0, 0), canvas)
    return rgb


def main():
    if not os.path.exists(CONFIG_PATH):
        print(f"❌ Arquivo de configuração ausente: {CONFIG_PATH}", file=sys.stderr)
        sys.exit(1)

    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        config = json.load(f)

    screens = config.get("screens", [])
    languages = config.get("languages", ["pt", "en", "es"])
    default_lang = config.get("default_language", "en")

    for lang in languages:
        os.makedirs(os.path.join(SHOTS_DIR, lang), exist_ok=True)

    print("🚀 Iniciando geração do pipeline completo da Play Store...")
    total = 0

    for screen in screens:
        s_id = screen["id"]
        s_file = screen["file"]
        accent = parse_color(screen.get("accent", "#00E5FF"))
        trans = screen.get("translations", {})

        print(f"\n📸 Processando tela: [{s_id}] ({s_file})")

        for lang in languages:
            lang_copy = trans.get(lang, trans.get(default_lang, {}))
            title = lang_copy.get("title", s_id.upper())
            subtitle = lang_copy.get("sub", "")

            if s_id == "01_extended_table":
                # Renderiza composição especial de 2 celulares
                img = render_dual_phone_screen_01(title, subtitle, lang=lang)
            else:
                raw_localized = os.path.join(RAW_DIR, f"{lang}_{s_file}")
                raw_path = raw_localized if os.path.exists(raw_localized) else os.path.join(RAW_DIR, s_file)
                if not os.path.exists(raw_path):
                    print(f"  ⚠️ Raw ausente para {raw_path}. Pulando...")
                    continue
                img = render_single_phone_mockup(raw_path, title, subtitle, accent)

            out_path = os.path.join(SHOTS_DIR, lang, s_file)
            img.save(out_path, "PNG", optimize=True)
            print(f"  ✓ [{lang.upper()}] Salvo em: {out_path}")
            total += 1

            if lang == default_lang:
                root_path = os.path.join(SHOTS_DIR, s_file)
                img.save(root_path, "PNG", optimize=True)

    print(f"\n🎉 Total de {total} capturas oficiais geradas com sucesso!")


if __name__ == "__main__":
    main()
