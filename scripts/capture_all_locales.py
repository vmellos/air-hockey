#!/usr/bin/env python3
"""
capture_all_locales.py
Captura automaticamente todas as telas do aplicativo nos 3 idiomas (Português, Inglês e Espanhol)
diretamente do dispositivo físico conectado via ADB, garantindo que o texto dentro das telas
corresponda exatamente ao idioma da ficha da Play Store.
"""

import os
import subprocess
import time
import sys

DEVICE = "RQCX803C9ZT"
RAW_DIR = "/home/vinicius/AndroidStudioProjects/AirHockey/store_assets/screenshots/raw"
os.makedirs(RAW_DIR, exist_ok=True)

LANGUAGES = [
    ("pt-BR", "pt"),
    ("en", "en"),
    ("es", "es"),
]

def adb(cmd):
    full_cmd = f"adb -s {DEVICE} {cmd}"
    res = subprocess.run(full_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    return res.stdout.strip()

def tap(x, y, delay=1.5):
    adb(f"shell input tap {x} {y}")
    time.sleep(delay)

def back(delay=1.0):
    adb("shell input keyevent 4")
    time.sleep(delay)

def screencap(filename):
    out_path = os.path.join(RAW_DIR, filename)
    cmd = f"adb -s {DEVICE} exec-out screencap -p > {out_path}"
    subprocess.run(cmd, shell=True, check=True)
    size_kb = os.path.getsize(out_path) // 1024
    print(f"  ✓ Capturado: {filename} ({size_kb} KB)")

def set_language(lang_code):
    print(f"\n🌐 Mudando idioma do app para: [{lang_code}]...")
    adb(f"shell am start -n dev.vmellos.airhockey/.MainActivity --es override_lang {lang_code}")
    time.sleep(2.5)

def capture_suite(prefix):
    print(f"\n📸 Iniciando suíte de capturas para o idioma [{prefix}]...")

    # 1. Customization (Vitrine 3D)
    print("  -> Abrindo Vitrine de Customização...")
    tap(670, 200, delay=2.5)
    screencap(f"{prefix}_04_customization.png")
    back(delay=1.5)

    # 2. Stage Mode (Seleção de Fases)
    print("  -> Abrindo Modo Fases...")
    tap(540, 1295, delay=2.5)
    screencap(f"{prefix}_05_stage_mode.png")
    back(delay=1.5)

    # 3. Leaderboard (Estatísticas & Elo)
    print("  -> Abrindo Estatísticas & Elo...")
    tap(120, 200, delay=2.5)
    screencap(f"{prefix}_06_leaderboard.png")
    back(delay=1.5)

    # 4. Settings (Configurações & Ergonomia)
    print("  -> Abrindo Configurações...")
    tap(960, 200, delay=2.5)
    screencap(f"{prefix}_07_settings.png")
    back(delay=1.5)

    # 5. Split Screen 3D (Mesa Dividida)
    print("  -> Abrindo Mesa Dividida 3D...")
    tap(540, 1507, delay=2.0) # Abre pré-jogo
    tap(540, 2095, delay=3.5) # Inicia partida
    screencap(f"{prefix}_03_split_screen.png")
    back(delay=1.0)           # Abre pause
    tap(540, 1687, delay=2.0) # Sair para o menu

    # 6. Arenas 3D Solo (Partida Solo 3D)
    print("  -> Abrindo Partida Solo 3D...")
    tap(540, 1295, delay=2.0) # Entra nas fases
    tap(204, 539, delay=4.5)  # Inicia fase 1 (aguarda voo da câmera)
    screencap(f"{prefix}_02_arenas_3d.png")
    back(delay=1.0)           # Abre pause
    tap(540, 1687, delay=2.0) # Sair para o menu

def main():
    print(f"🚀 Iniciando automação de capturas multilíngue no device {DEVICE}...")

    for lang_code, prefix in LANGUAGES:
        set_language(lang_code)
        capture_suite(prefix)

    # Restaura para português
    set_language("pt-BR")
    print("\n🎉 Todas as capturas multilíngues brutas foram concluídas com sucesso!")

if __name__ == "__main__":
    main()
