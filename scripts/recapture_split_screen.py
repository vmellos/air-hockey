#!/usr/bin/env python3
"""
recapture_split_screen.py
Recaptura a tela 03 (Mesa Dividida / Split Screen) para os 3 idiomas (PT, EN, ES),
garantindo que o aplicativo inicie limpo em cada idioma e entre na partida com
os textos do HUD (TARGET: 3, OBJETIVO: 3, ALVO: 3) no idioma correto.
"""

import os
import subprocess
import time

DEVICE = "RQCX803C9ZT"
RAW_DIR = "/home/vinicius/AndroidStudioProjects/AirHockey/store_assets/screenshots/raw"

LANGS = [
    ("pt-BR", "pt", "ALVO"),
    ("en", "en", "TARGET"),
    ("es", "es", "OBJETIVO")
]

def adb(cmd):
    full_cmd = f"adb -s {DEVICE} {cmd}"
    res = subprocess.run(full_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    return res.stdout.strip()

for lang_code, prefix, expected_word in LANGS:
    print(f"\n==========================================")
    print(f"🎮 Recapturando Imagem 03 para [{prefix.upper()}] ({lang_code})...")
    print(f"==========================================")

    # 1. Force stop e inicia no idioma
    adb("shell am force-stop dev.vmellos.airhockey")
    time.sleep(1.0)
    adb(f"shell am start -n dev.vmellos.airhockey/.MainActivity --es override_lang {lang_code}")
    time.sleep(3.0)

    # 2. Abre Pré-jogo da Mesa Dividida
    print("  -> Clicando em Mesa Dividida / Split Screen no Menu...")
    adb("shell input tap 540 1507")
    time.sleep(2.0)

    # 3. Clica em INICIAR PARTIDA
    print("  -> Clicando em INICIAR PARTIDA / START MATCH...")
    adb("shell input tap 540 2095")
    print("  -> Aguardando contagem regressiva (4.5s)...")
    time.sleep(4.5)

    # 4. Verifica dump de texto para confirmar o idioma na tela
    adb("shell uiautomator dump /sdcard/dump.xml")
    dump_text = adb("shell cat /sdcard/dump.xml")
    if expected_word in dump_text:
        print(f"  ✅ Confirmado texto '{expected_word}' presente na tela!")
    else:
        print(f"  ⚠️ Aviso: '{expected_word}' não encontrado no dump (pode ser SurfaceView). Prosseguindo...")

    # 5. Captura a tela
    out_file = os.path.join(RAW_DIR, f"{prefix}_03_split_screen.png")
    cmd = f"adb -s {DEVICE} exec-out screencap -p > {out_file}"
    subprocess.run(cmd, shell=True, check=True)
    size_kb = os.path.getsize(out_file) // 1024
    print(f"  ✓ Salvo: {out_file} ({size_kb} KB)")

# Finaliza fechando o app
adb("shell am force-stop dev.vmellos.airhockey")
adb("shell am start -n dev.vmellos.airhockey/.MainActivity --es override_lang pt-BR")
print("\n🎉 Recaptura da Imagem 03 nos 3 idiomas concluída!")
