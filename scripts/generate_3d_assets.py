import bpy
import os
import math

OUTPUT_DIR = "/home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/assets/models"
os.makedirs(OUTPUT_DIR, exist_ok=True)

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

# ============================================================================
# 1. DISCO CYBER (disco_cyber.glb)
# ============================================================================
def generate_puck():
    reset_scene()
    radius = 0.05
    depth = 0.015

    # Corpo principal cilíndrico
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    puck.name = "PuckBody"

    bevel = puck.modifiers.new(name="Bevel", type='BEVEL')
    bevel.width = 0.003
    bevel.segments = 3
    bpy.ops.object.shade_smooth()

    mat_metal = create_pbr_material("PuckTungsten", (0.04, 0.05, 0.08, 1.0), metallic=0.92, roughness=0.18)
    puck.data.materials.append(mat_metal)

    # Anel Neon Superior
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.70, depth=depth * 1.05, vertices=64, location=(0, 0, 0))
    ring = bpy.context.active_object
    ring.name = "PuckNeonRing"
    mat_glow = create_pbr_material("PuckGlow", (0.0, 0.95, 1.0, 1.0), emission_color=(0.0, 0.95, 1.0, 1.0), emission_strength=5.0)
    ring.data.materials.append(mat_glow)

    # Núcleo Central Escuro
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.45, depth=depth * 1.10, vertices=64, location=(0, 0, 0))
    core = bpy.context.active_object
    core.name = "PuckCore"
    core.data.materials.append(mat_metal)

    # Exporta
    out_path = os.path.join(OUTPUT_DIR, "disco_cyber.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported:", out_path)

# ============================================================================
# 2. REBATEDORES P1 e P2 (rebatedor_p1.glb e rebatedor_p2.glb)
# ============================================================================
def generate_mallet(name, neon_color, is_p1=True):
    reset_scene()
    base_radius = 0.08
    base_depth = 0.02
    knob_radius = 0.035
    knob_height = 0.05

    # 1. Base do Rebatedor
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius, depth=base_depth, vertices=64, location=(0, 0, base_depth / 2))
    base = bpy.context.active_object
    base.name = "MalletBase"
    bev = base.modifiers.new(name="Bevel", type='BEVEL')
    bev.width = 0.004
    bev.segments = 4
    bpy.ops.object.shade_smooth()

    mat_dark = create_pbr_material("MalletDarkCarbon", (0.08, 0.09, 0.12, 1.0), metallic=0.75, roughness=0.30)
    base.data.materials.append(mat_dark)

    # 2. Anel Neon Perimetral na Base
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius * 0.90, depth=base_depth * 1.08, vertices=64, location=(0, 0, base_depth / 2))
    ring = bpy.context.active_object
    ring.name = "MalletGlowRing"
    mat_neon = create_pbr_material("MalletNeon", neon_color, emission_color=neon_color, emission_strength=6.0)
    ring.data.materials.append(mat_neon)

    # 3. Anel Interno de Proteção
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius * 0.78, depth=base_depth * 1.15, vertices=64, location=(0, 0, base_depth / 2))
    inner = bpy.context.active_object
    inner.name = "MalletInnerRim"
    inner.data.materials.append(mat_dark)

    # 4. Manopla Anatômica (Pega do jogador)
    bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius, depth=knob_height, vertices=48, location=(0, 0, base_depth + knob_height / 2))
    knob = bpy.context.active_object
    knob.name = "MalletKnob"
    bev_k = knob.modifiers.new(name="Bevel", type='BEVEL')
    bev_k.width = 0.005
    bev_k.segments = 4
    bpy.ops.object.shade_smooth()
    knob.data.materials.append(mat_dark)

    # 5. Topo da Manopla com cúpula iluminada
    bpy.ops.mesh.primitive_uv_sphere_add(radius=knob_radius * 0.92, segments=32, ring_count=16, location=(0, 0, base_depth + knob_height))
    dome = bpy.context.active_object
    dome.name = "MalletDome"
    bpy.ops.object.shade_smooth()
    dome.data.materials.append(mat_neon)

    # Exporta
    filename = f"{name}.glb"
    out_path = os.path.join(OUTPUT_DIR, filename)
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported:", out_path)

# ============================================================================
# 3. MESA ARCADE COMPLETA (mesa_arcade.glb)
# ============================================================================
def generate_table():
    reset_scene()
    
    # Dimensões da mesa em unidades (Metros)
    # Proporção 1080x1920 -> Largura: 1.08m, Comprimento: 1.92m
    w = 1.08
    l = 1.92
    h_surface = 0.04
    rail_w = 0.07
    rail_h = 0.06
    goal_w = 0.38

    # 1. Superfície de Jogo (Rink floor)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, -h_surface / 2))
    floor = bpy.context.active_object
    floor.name = "TableFloor"
    floor.scale = (w, l, h_surface)
    bpy.ops.object.transform_apply(scale=True)
    
    mat_floor = create_pbr_material("TableFloorMat", (0.02, 0.04, 0.08, 1.0), metallic=0.40, roughness=0.15)
    floor.data.materials.append(mat_floor)

    # 2. Trilhos Laterais (Trilho Esquerdo e Direito)
    mat_rails = create_pbr_material("RailAnodizedMetal", (0.05, 0.06, 0.08, 1.0), metallic=0.90, roughness=0.25)
    mat_p1_neon = create_pbr_material("P1NeonCyan", (0.0, 0.9, 1.0, 1.0), emission_color=(0.0, 0.9, 1.0, 1.0), emission_strength=4.5)
    mat_p2_neon = create_pbr_material("P2NeonOrange", (1.0, 0.35, 0.05, 1.0), emission_color=(1.0, 0.35, 0.05, 1.0), emission_strength=4.5)

    # Trilho Esquerdo (X = -w/2 - rail_w/2)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-(w + rail_w) / 2, 0, rail_h / 2))
    rail_l = bpy.context.active_object
    rail_l.name = "RailLeft"
    rail_l.scale = (rail_w, l + rail_w * 2, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    rail_l.data.materials.append(mat_rails)

    # Trilho Direito (X = +w/2 + rail_w/2)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=((w + rail_w) / 2, 0, rail_h / 2))
    rail_r = bpy.context.active_object
    rail_r.name = "RailRight"
    rail_r.scale = (rail_w, l + rail_w * 2, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    rail_r.data.materials.append(mat_rails)

    # Trilhos de Fundo com Abertura para as Metas
    side_segment_w = (w - goal_w) / 2

    # Topo (Meta P2, Y > 0 / Z < 0)
    top_y = (l + rail_w) / 2
    # Topo Esquerda
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-w/2 + side_segment_w/2, top_y, rail_h / 2))
    t_left = bpy.context.active_object
    t_left.scale = (side_segment_w, rail_w, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    t_left.data.materials.append(mat_rails)

    # Topo Direita
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(w/2 - side_segment_w/2, top_y, rail_h / 2))
    t_right = bpy.context.active_object
    t_right.scale = (side_segment_w, rail_w, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    t_right.data.materials.append(mat_rails)

    # Base (Meta P1, Y < 0 / Z > 0)
    bottom_y = -(l + rail_w) / 2
    # Base Esquerda
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-w/2 + side_segment_w/2, bottom_y, rail_h / 2))
    b_left = bpy.context.active_object
    b_left.scale = (side_segment_w, rail_w, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    b_left.data.materials.append(mat_rails)

    # Base Direita
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(w/2 - side_segment_w/2, bottom_y, rail_h / 2))
    b_right = bpy.context.active_object
    b_right.scale = (side_segment_w, rail_w, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    b_right.data.materials.append(mat_rails)

    # Faixas de LED Neon Embutidas nos Trilhos Internos
    led_thickness = 0.008
    # LED Esquerdo P1 (Ciano)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-w/2 + led_thickness/2, -l/4, rail_h * 0.7))
    led_l_p1 = bpy.context.active_object
    led_l_p1.scale = (led_thickness, l/2, 0.015)
    bpy.ops.object.transform_apply(scale=True)
    led_l_p1.data.materials.append(mat_p1_neon)

    # LED Direito P1 (Ciano)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(w/2 - led_thickness/2, -l/4, rail_h * 0.7))
    led_r_p1 = bpy.context.active_object
    led_r_p1.scale = (led_thickness, l/2, 0.015)
    bpy.ops.object.transform_apply(scale=True)
    led_r_p1.data.materials.append(mat_p1_neon)

    # LED Esquerdo P2 (Laranja)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-w/2 + led_thickness/2, l/4, rail_h * 0.7))
    led_l_p2 = bpy.context.active_object
    led_l_p2.scale = (led_thickness, l/2, 0.015)
    bpy.ops.object.transform_apply(scale=True)
    led_l_p2.data.materials.append(mat_p2_neon)

    # LED Direito P2 (Laranja)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(w/2 - led_thickness/2, l/4, rail_h * 0.7))
    led_r_p2 = bpy.context.active_object
    led_r_p2.scale = (led_thickness, l/2, 0.015)
    bpy.ops.object.transform_apply(scale=True)
    led_r_p2.data.materials.append(mat_p2_neon)

    # Linha Central Divisória (Neon Branco / Amarelo)
    mat_center = create_pbr_material("CenterLineGlow", (1.0, 1.0, 1.0, 1.0), emission_color=(1.0, 0.9, 0.4, 1.0), emission_strength=3.0)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.001))
    center_line = bpy.context.active_object
    center_line.scale = (w, 0.015, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    center_line.data.materials.append(mat_center)

    # Círculo Central Neon
    bpy.ops.mesh.primitive_cylinder_add(radius=0.18, depth=0.002, vertices=48, location=(0, 0, 0.001))
    center_circle = bpy.context.active_object
    center_circle.data.materials.append(mat_center)

    # 7. Piso da Arena 3D (Plataforma Estádio Arcade com Grid Cibernético e Balizadores)
    arena_w = 8.0
    arena_l = 10.0
    arena_z = -h_surface - 0.05

    # Base do piso
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, arena_z))
    arena_floor = bpy.context.active_object
    arena_floor.name = "ArenaFloor"
    arena_floor.scale = (arena_w, arena_l, 0.02)
    bpy.ops.object.transform_apply(scale=True)
    mat_arena_floor = create_pbr_material("ArenaFloorDark", (0.015, 0.02, 0.035, 1.0), metallic=0.7, roughness=0.4)
    arena_floor.data.materials.append(mat_arena_floor)

    # Linhas de Grid Cibernético Neon
    mat_grid = create_pbr_material("ArenaGridGlow", (0.0, 0.7, 0.9, 1.0), emission_color=(0.0, 0.6, 0.85, 1.0), emission_strength=2.0)
    for gx in [-3.0, -2.0, -1.0, 1.0, 2.0, 3.0]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(gx, 0, arena_z + 0.011))
        gline = bpy.context.active_object
        gline.scale = (0.012, arena_l * 0.92, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_grid)

    for gy in [-4.0, -2.5, -1.25, 1.25, 2.5, 4.0]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, gy, arena_z + 0.011))
        gline = bpy.context.active_object
        gline.scale = (arena_w * 0.92, 0.012, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_grid)

    # 4 Balizadores Perimetrais de Iluminação da Arena
    mat_pillar = create_pbr_material("PillarMat", (0.04, 0.05, 0.07, 1.0), metallic=0.85, roughness=0.2)
    mat_beacon_cyan = create_pbr_material("BeaconCyan", (0.0, 0.9, 1.0, 1.0), emission_color=(0.0, 0.9, 1.0, 1.0), emission_strength=5.0)
    mat_beacon_orange = create_pbr_material("BeaconOrange", (1.0, 0.4, 0.1, 1.0), emission_color=(1.0, 0.4, 0.1, 1.0), emission_strength=5.0)

    pillar_coords = [
        (-arena_w/2 + 0.45, -arena_l/2 + 0.45, mat_beacon_cyan),
        (arena_w/2 - 0.45, -arena_l/2 + 0.45, mat_beacon_cyan),
        (-arena_w/2 + 0.45, arena_l/2 - 0.45, mat_beacon_orange),
        (arena_w/2 - 0.45, arena_l/2 - 0.45, mat_beacon_orange),
    ]
    for px, py, mat_beacon in pillar_coords:
        # Coluna
        bpy.ops.mesh.primitive_cylinder_add(radius=0.07, depth=0.9, vertices=24, location=(px, py, arena_z + 0.45))
        pillar = bpy.context.active_object
        pillar.data.materials.append(mat_pillar)
        # Topo Luminoso
        bpy.ops.mesh.primitive_cylinder_add(radius=0.085, depth=0.1, vertices=24, location=(px, py, arena_z + 0.9))
        beacon = bpy.context.active_object
        beacon.data.materials.append(mat_beacon)

    # Exporta
    out_path = os.path.join(OUTPUT_DIR, "mesa_arcade.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported:", out_path)

if __name__ == "__main__":
    generate_puck()
    generate_mallet("rebatedor_p1", (0.0, 0.9, 1.0, 1.0), is_p1=True)
    generate_mallet("rebatedor_p2", (1.0, 0.35, 0.05, 1.0), is_p1=False)
    generate_table()
    print(">>> ALL_MODELS_GENERATED_SUCCESSFULLY")
