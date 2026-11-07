import bpy
import os
import math

OUTPUT_MODELS_DIR = "/home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/assets/models"
os.makedirs(OUTPUT_MODELS_DIR, exist_ok=True)

def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)

def create_pbr_material(name, base_color, metallic=0.0, roughness=0.5, emission_color=None, emission_strength=1.0):
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        if "Base Color" in bsdf.inputs:
            bsdf.inputs["Base Color"].default_value = base_color
        if "Metallic" in bsdf.inputs:
            bsdf.inputs["Metallic"].default_value = metallic
        if "Roughness" in bsdf.inputs:
            bsdf.inputs["Roughness"].default_value = roughness
        if emission_color:
            if "Emission Color" in bsdf.inputs:
                bsdf.inputs["Emission Color"].default_value = emission_color
                bsdf.inputs["Emission Strength"].default_value = emission_strength
            elif "Emission" in bsdf.inputs:
                bsdf.inputs["Emission"].default_value = emission_color
    return mat

def add_bevel(obj, width=0.003, segments=4):
    bev = obj.modifiers.new(name="Bevel", type='BEVEL')
    bev.width = width
    bev.segments = segments
    return bev

def create_semicircle_mesh(name, center_x, center_y, z_pos, r_out, r_in, n_segments=36):
    """Cria um semicírculo planar voltado para o interior da meia-mesa (-Y)"""
    verts = []
    faces = []
    for i in range(n_segments + 1):
        theta = math.pi + (i / n_segments) * math.pi
        verts.append((center_x + r_out * math.cos(theta), center_y + r_out * math.sin(theta), z_pos))
        verts.append((center_x + r_in * math.cos(theta), center_y + r_in * math.sin(theta), z_pos))
    for i in range(n_segments):
        v0 = 2 * i
        v1 = 2 * i + 1
        v2 = 2 * i + 3
        v3 = 2 * i + 2
        faces.append((v0, v1, v2, v3))
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    return obj

def build_half_table(
    name,
    surface_color,
    rail_color,
    bumper_color,
    faction_color,
    neutral_color=(1.0, 0.98, 0.85, 1.0),
    is_p1=True
):
    """
    Constrói a Meia-Mesa 3D proporcional (1:2) com extremidade superior aberta para Mesa Estendida.
    Dimensões métricas: Largura = 1.08m, Comprimento = 2.16m.
    Origem no centro da meia-mesa: X em [-0.54, 0.54], Y em [-1.08, +1.08].
    - Fundo (Y = -1.08): Meta, Barreira Laser e Protetores de Canto.
    - Topo (Y = +1.08): ABERTO (sem trilho), com arco neon semicircular de junção.
    """
    w = 1.08
    l_half = 2.16
    h_surface = 0.04
    rail_w = 0.075
    rail_h = 0.065
    goal_w = 0.38
    side_seg_w = (w - goal_w) / 2
    led_thick = 0.007
    led_h = 0.016

    mat_floor = create_pbr_material(f"{name}_Floor", surface_color, metallic=0.60, roughness=0.22)
    mat_rails = create_pbr_material(f"{name}_Rail", rail_color, metallic=0.92, roughness=0.22)
    mat_bumpers = create_pbr_material(f"{name}_Bumper", bumper_color, metallic=0.80, roughness=0.35)
    mat_neon = create_pbr_material(f"{name}_Neon", faction_color, emission_color=faction_color, emission_strength=5.5)
    mat_laser = create_pbr_material(f"{name}_Laser", neutral_color, emission_color=neutral_color, emission_strength=4.8)

    # 1. Piso da Meia-Mesa
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, -h_surface / 2))
    floor = bpy.context.active_object
    floor.name = f"{name}_Surface"
    floor.scale = (w, l_half, h_surface)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(floor, width=0.004, segments=4)
    floor.data.materials.append(mat_floor)

    # 2. Semicírculo Central Neon no Topo (Y = +1.08)
    top_y = l_half / 2
    arc_outer = create_semicircle_mesh(f"{name}_CenterArcOuter", 0, top_y, 0.0015, r_out=0.18, r_in=0.165)
    arc_outer.data.materials.append(mat_laser)

    # 3. Faixa de Junção Laser no Topo (Eletro-borda de passagem contínua)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, top_y - 0.003, 0.0015))
    top_edge_laser = bpy.context.active_object
    top_edge_laser.name = f"{name}_TopEdgeLaser"
    top_edge_laser.scale = (w * 0.98, 0.006, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    top_edge_laser.data.materials.append(mat_laser)

    # 4. Área da Meta Neon na Base (Y = -1.08 + 0.32 = -0.76)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -l_half/2 + 0.32, 0.0015))
    crease = bpy.context.active_object
    crease.name = f"{name}_Crease"
    crease.scale = (goal_w * 1.5, 0.008, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    crease.data.materials.append(mat_neon)

    # 5. Trilhos Laterais (Estendem-se do fundo até o topo)
    rail_len = l_half + rail_w
    rail_center_y = -rail_w / 2  # (-0.0375)

    for sx, r_name in [(-(w + rail_w)/2, "RailL"), ((w + rail_w)/2, "RailR")]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(sx, rail_center_y, rail_h / 2))
        rail = bpy.context.active_object
        rail.name = f"{name}_{r_name}"
        rail.scale = (rail_w, rail_len, rail_h)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(rail, width=0.004, segments=4)
        rail.data.materials.append(mat_rails)

        # Friso LED no trilho lateral
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(sx, rail_center_y, rail_h * 0.72))
        led = bpy.context.active_object
        led.scale = (led_thick, rail_len * 0.94, led_h)
        bpy.ops.object.transform_apply(scale=True)
        led.data.materials.append(mat_neon)

    # 6. Trilho Inferior (Fundo) com Abertura da Meta
    bottom_y = -l_half/2 - rail_w/2
    for seg_x in [-w/2 + side_seg_w/2, w/2 - side_seg_w/2]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(seg_x, bottom_y, rail_h / 2))
        seg = bpy.context.active_object
        seg.scale = (side_seg_w, rail_w, rail_h)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(seg, width=0.003, segments=4)
        seg.data.materials.append(mat_rails)

        # LED na borda da meta
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(seg_x, bottom_y, rail_h * 0.75))
        f_led = bpy.context.active_object
        f_led.scale = (side_seg_w * 0.95, led_thick, led_h)
        bpy.ops.object.transform_apply(scale=True)
        f_led.data.materials.append(mat_neon)

    # 7. Protetores de Canto Angulares (Apenas nos dois cantos inferiores)
    c_offset_x = (w + rail_w) / 2
    c_offset_y = bottom_y
    for cx in [-c_offset_x, c_offset_x]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(cx, c_offset_y, rail_h * 0.55))
        bumper = bpy.context.active_object
        bumper.scale = (rail_w * 1.35, rail_w * 1.35, rail_h * 1.15)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(bumper, width=0.005, segments=4)
        bumper.data.materials.append(mat_bumpers)

        # Indicador Chevron Luminoso
        bpy.ops.mesh.primitive_cylinder_add(radius=0.015, depth=rail_h * 1.2, vertices=16, location=(cx, c_offset_y, rail_h * 0.6))
        chev = bpy.context.active_object
        chev.data.materials.append(mat_neon)

    # 8. Barreira de Energia da Meta 3D (Portal Laser)
    post_h = 0.12
    post_r = 0.018
    for post_x in [-goal_w / 2, goal_w / 2]:
        bpy.ops.mesh.primitive_cylinder_add(radius=post_r, depth=post_h, vertices=24, location=(post_x, bottom_y, post_h / 2))
        post = bpy.context.active_object
        post.data.materials.append(mat_rails)

        bpy.ops.mesh.primitive_cylinder_add(radius=post_r * 1.25, depth=0.02, vertices=24, location=(post_x, bottom_y, post_h))
        cap = bpy.context.active_object
        cap.data.materials.append(mat_neon)

    # Arco/Trave Laser Horizontal Superior
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, bottom_y, post_h * 0.95))
    beam = bpy.context.active_object
    beam.scale = (goal_w * 0.96, post_r * 1.4, 0.014)
    bpy.ops.object.transform_apply(scale=True)
    beam.data.materials.append(mat_neon)

    # Cortina Laser Translúcida de Contenção
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, bottom_y, post_h * 0.45))
    curtain = bpy.context.active_object
    curtain.scale = (goal_w * 0.94, 0.004, post_h * 0.70)
    bpy.ops.object.transform_apply(scale=True)
    curtain.data.materials.append(mat_neon)

def export_glb(filename):
    out_path = os.path.join(OUTPUT_MODELS_DIR, filename)
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported GLB:", out_path)
    return out_path

THEMES = [
    {
        "id": "mesa_arcade_half",
        "name": "MesaArcade",
        "surface": (0.015, 0.022, 0.04, 1.0),
        "rail": (0.05, 0.06, 0.08, 1.0),
        "bumper": (0.03, 0.035, 0.045, 1.0),
        "p1": (0.0, 0.92, 1.0, 1.0),
        "p2": (1.0, 0.40, 0.05, 1.0),
        "neutral": (1.0, 0.98, 0.85, 1.0)
    },
    {
        "id": "mesa_cyber_soccer_half",
        "name": "Soccer",
        "surface": (0.012, 0.028, 0.018, 1.0),
        "rail": (0.05, 0.06, 0.08, 1.0),
        "bumper": (0.03, 0.035, 0.045, 1.0),
        "p1": (0.0, 0.90, 0.46, 1.0),
        "p2": (1.0, 0.12, 0.25, 1.0),
        "neutral": (1.0, 0.98, 0.92, 1.0)
    },
    {
        "id": "mesa_neo_basketball_half",
        "name": "Basketball",
        "surface": (0.035, 0.018, 0.010, 1.0),
        "rail": (0.06, 0.05, 0.04, 1.0),
        "bumper": (0.04, 0.03, 0.02, 1.0),
        "p1": (1.0, 0.45, 0.0, 1.0),
        "p2": (0.0, 0.70, 1.0, 1.0),
        "neutral": (1.0, 0.90, 0.50, 1.0)
    },
    {
        "id": "mesa_gravity_volley_half",
        "name": "Volley",
        "surface": (0.025, 0.015, 0.05, 1.0),
        "rail": (0.04, 0.045, 0.065, 1.0),
        "bumper": (0.025, 0.02, 0.04, 1.0),
        "p1": (0.54, 0.17, 0.89, 1.0),
        "p2": (1.0, 0.0, 0.5, 1.0),
        "neutral": (1.0, 0.85, 0.2, 1.0)
    },
    {
        "id": "mesa_gridiron_football_half",
        "name": "Gridiron",
        "surface": (0.012, 0.035, 0.02, 1.0),
        "rail": (0.03, 0.05, 0.04, 1.0),
        "bumper": (0.02, 0.03, 0.02, 1.0),
        "p1": (0.0, 0.90, 1.0, 1.0),
        "p2": (1.0, 0.70, 0.0, 1.0),
        "neutral": (1.0, 1.0, 1.0, 1.0)
    },
    {
        "id": "mesa_cyber_baseball_half",
        "name": "Baseball",
        "surface": (0.035, 0.025, 0.02, 1.0),
        "rail": (0.06, 0.05, 0.04, 1.0),
        "bumper": (0.04, 0.03, 0.02, 1.0),
        "p1": (1.0, 0.09, 0.27, 1.0),
        "p2": (0.0, 0.90, 1.0, 1.0),
        "neutral": (1.0, 0.95, 0.85, 1.0)
    },
    {
        "id": "mesa_wasteland_apocalypse_half",
        "name": "Wasteland",
        "surface": (0.04, 0.025, 0.015, 1.0),
        "rail": (0.08, 0.04, 0.02, 1.0),
        "bumper": (0.04, 0.02, 0.01, 1.0),
        "p1": (1.0, 0.24, 0.0, 1.0),
        "p2": (1.0, 0.84, 0.0, 1.0),
        "neutral": (0.8, 0.3, 0.1, 1.0)
    },
    {
        "id": "mesa_retro_synthwave_half",
        "name": "Synthwave",
        "surface": (0.03, 0.01, 0.06, 1.0),
        "rail": (0.05, 0.03, 0.08, 1.0),
        "bumper": (0.03, 0.015, 0.05, 1.0),
        "p1": (1.0, 0.0, 0.5, 1.0),
        "p2": (0.0, 0.94, 1.0, 1.0),
        "neutral": (0.9, 0.6, 1.0, 1.0)
    },
    {
        "id": "mesa_tokamak_reactor_half",
        "name": "Tokamak",
        "surface": (0.015, 0.035, 0.05, 1.0),
        "rail": (0.02, 0.045, 0.065, 1.0),
        "bumper": (0.01, 0.025, 0.04, 1.0),
        "p1": (0.46, 1.0, 0.01, 1.0),
        "p2": (0.0, 0.90, 1.0, 1.0),
        "neutral": (0.46, 1.0, 0.01, 1.0)
    },
    {
        "id": "mesa_cyber_pong_half",
        "name": "Pong",
        "surface": (0.01, 0.025, 0.01, 1.0),
        "rail": (0.02, 0.05, 0.02, 1.0),
        "bumper": (0.01, 0.03, 0.01, 1.0),
        "p1": (0.22, 1.0, 0.08, 1.0),
        "p2": (1.0, 0.70, 0.0, 1.0),
        "neutral": (0.22, 1.0, 0.08, 1.0)
    },
    {
        "id": "mesa_cyber_colosseum_half",
        "name": "Colosseum",
        "surface": (0.035, 0.025, 0.015, 1.0),
        "rail": (0.07, 0.05, 0.03, 1.0),
        "bumper": (0.04, 0.03, 0.015, 1.0),
        "p1": (1.0, 0.84, 0.0, 1.0),
        "p2": (0.83, 0.0, 0.0, 1.0),
        "neutral": (1.0, 0.84, 0.0, 1.0)
    }
]

def main():
    print("=== INICIANDO GERAÇÃO DE TODAS AS MEIAS-MESAS 3D (11 TEMAS x 2 FACÇÕES = 22 MODELOS) ===")
    for theme in THEMES:
        tid = theme["id"]
        tname = theme["name"]
        print(f"--- Gerando {tname} (P1 e P2) ---")
        
        # P1 (Anfitrião)
        reset_scene()
        build_half_table(
            name=f"{tname}HalfP1",
            surface_color=theme["surface"],
            rail_color=theme["rail"],
            bumper_color=theme["bumper"],
            faction_color=theme["p1"],
            neutral_color=theme["neutral"],
            is_p1=True
        )
        export_glb(f"{tid}_p1.glb")
        
        # P2 (Convidado)
        reset_scene()
        build_half_table(
            name=f"{tname}HalfP2",
            surface_color=theme["surface"],
            rail_color=theme["rail"],
            bumper_color=theme["bumper"],
            faction_color=theme["p2"],
            neutral_color=theme["neutral"],
            is_p1=False
        )
        export_glb(f"{tid}_p2.glb")

    print(">>> TODOS OS 22 MODELOS DE MEIA-MESA FORAM GERADOS COM SUCESSO!")

if __name__ == "__main__":
    main()
