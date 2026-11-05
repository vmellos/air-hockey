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

def add_bevel(obj, width=0.003, segments=4):
    bev = obj.modifiers.new(name="Bevel", type='BEVEL')
    bev.width = width
    bev.segments = segments
    return bev

# ============================================================================
# 1. DISCO CYBER (disco_cyber.glb) - Estilo Reator Sci-Fi com Turbina de Plasma
# ============================================================================
def generate_puck():
    reset_scene()
    radius = 0.05
    depth = 0.015

    # 1.1 Chassi Principal em Tungstênio Aeroespacial Facetado
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    puck.name = "PuckChassis"
    add_bevel(puck, width=0.0025, segments=4)
    bpy.ops.object.shade_smooth()
    mat_metal = create_pbr_material("PuckTungsten", (0.04, 0.05, 0.07, 1.0), metallic=0.95, roughness=0.15)
    puck.data.materials.append(mat_metal)

    # 1.2 Friso Neon Perimetral na Borda Externa (Trincheira do Equador)
    mat_glow = create_pbr_material("PuckPlasmaCyan", (0.0, 0.95, 1.0, 1.0), emission_color=(0.0, 0.95, 1.0, 1.0), emission_strength=5.5)
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 1.015, depth=depth * 0.25, vertices=64, location=(0, 0, 0))
    equator_glow = bpy.context.active_object
    equator_glow.name = "PuckEquatorGlow"
    equator_glow.data.materials.append(mat_glow)

    # 1.3 Anel Superior Neon Concêntrico
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.72, depth=depth * 1.04, vertices=64, location=(0, 0, 0))
    ring = bpy.context.active_object
    ring.name = "PuckTopNeonRing"
    ring.data.materials.append(mat_glow)

    # 1.4 Aletas Radiais da Turbina de Resfriamento (8 Aletas Geométricas)
    mat_fins = create_pbr_material("PuckFinDark", (0.02, 0.025, 0.035, 1.0), metallic=0.90, roughness=0.30)
    for i in range(8):
        angle = i * (math.pi / 4.0)
        dist = radius * 0.58
        fx = dist * math.cos(angle)
        fy = dist * math.sin(angle)
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(fx, fy, depth * 0.51))
        fin = bpy.context.active_object
        fin.scale = (0.0022, 0.012, 0.0015)
        fin.rotation_euler = (0, 0, angle)
        bpy.ops.object.transform_apply(scale=True, rotation=True)
        fin.data.materials.append(mat_fins)

    # 1.5 Núcleo Central de Contenção de Plasma (Reator Inset)
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.44, depth=depth * 1.08, vertices=48, location=(0, 0, 0))
    core_rim = bpy.context.active_object
    core_rim.name = "PuckCoreRim"
    core_rim.data.materials.append(mat_metal)

    # 1.6 Cristal/Nó de Energia Central Hiperluminoso
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.22, depth=depth * 1.15, vertices=32, location=(0, 0, 0))
    crystal = bpy.context.active_object
    crystal.name = "PuckCentralNode"
    mat_node = create_pbr_material("PuckCoreNode", (0.4, 0.98, 1.0, 1.0), emission_color=(0.2, 0.95, 1.0, 1.0), emission_strength=6.0)
    crystal.data.materials.append(mat_node)

    out_path = os.path.join(OUTPUT_DIR, "disco_cyber.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Puck:", out_path)

# ============================================================================
# 2. REBATEDORES P1 e P2 (rebatedor_p1.glb e rebatedor_p2.glb)
# ============================================================================
def generate_mallet(name, neon_color, is_p1=True):
    reset_scene()
    base_radius = 0.08
    base_depth = 0.02
    knob_radius = 0.034
    knob_height = 0.052

    mat_dark_carbon = create_pbr_material("MalletCarbon", (0.06, 0.07, 0.09, 1.0), metallic=0.85, roughness=0.25)
    mat_rubber = create_pbr_material("MalletGripRubber", (0.02, 0.02, 0.025, 1.0), metallic=0.05, roughness=0.85)
    mat_neon = create_pbr_material("MalletFactionNeon", neon_color, emission_color=neon_color, emission_strength=5.8)

    # 2.1 Saia Base do Rebatedor com Chanfro Duplo
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius, depth=base_depth, vertices=64, location=(0, 0, base_depth / 2))
    base = bpy.context.active_object
    base.name = "MalletBaseChassis"
    add_bevel(base, width=0.0035, segments=4)
    bpy.ops.object.shade_smooth()
    base.data.materials.append(mat_dark_carbon)

    # 2.2 Anel Neon Inferior na Borda Perimetral
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius * 1.012, depth=base_depth * 0.25, vertices=64, location=(0, 0, base_depth * 0.25))
    lower_ring = bpy.context.active_object
    lower_ring.name = "MalletLowerRing"
    lower_ring.data.materials.append(mat_neon)

    # 2.3 Anel Neon no Degrau Superior da Base
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius * 0.88, depth=base_depth * 1.06, vertices=64, location=(0, 0, base_depth / 2))
    step_ring = bpy.context.active_object
    step_ring.name = "MalletUpperRing"
    step_ring.data.materials.append(mat_neon)

    # 2.4 Anel de Contenção em Carbono
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius * 0.74, depth=base_depth * 1.12, vertices=64, location=(0, 0, base_depth / 2))
    collar = bpy.context.active_object
    collar.data.materials.append(mat_dark_carbon)

    # 2.5 Haste Anatômica / Manopla de Controle (Knob)
    bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius, depth=knob_height, vertices=48, location=(0, 0, base_depth + knob_height / 2))
    knob = bpy.context.active_object
    knob.name = "MalletGripStem"
    add_bevel(knob, width=0.003, segments=4)
    bpy.ops.object.shade_smooth()
    knob.data.materials.append(mat_dark_carbon)

    # 2.6 Aletas e Ranhuras Emborrachadas de Tração Cibernética (Grip Ribs)
    for z_off in [0.012, 0.024, 0.036]:
        bpy.ops.mesh.primitive_cylinder_add(
            radius=knob_radius * 1.05,
            depth=0.004,
            vertices=48,
            location=(0, 0, base_depth + z_off)
        )
        rib = bpy.context.active_object
        rib.data.materials.append(mat_rubber)

    # 2.7 Cúpula Superior com Reator de Facção Iluminado
    dome_z = base_depth + knob_height
    bpy.ops.mesh.primitive_uv_sphere_add(radius=knob_radius * 0.90, segments=32, ring_count=16, location=(0, 0, dome_z))
    dome = bpy.context.active_object
    dome.name = "MalletReactorDome"
    bpy.ops.object.shade_smooth()
    dome.data.materials.append(mat_neon)

    # 2.8 Coroa Protetora com Janelas Notched
    bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius * 0.96, depth=0.007, vertices=32, location=(0, 0, dome_z))
    crown = bpy.context.active_object
    crown.data.materials.append(mat_dark_carbon)

    filename = f"{name}.glb"
    out_path = os.path.join(OUTPUT_DIR, filename)
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Mallet:", out_path)

# ============================================================================
# 3. MESA ARCADE COMPLETA COM BARREIRAS DE META NEON (mesa_arcade.glb)
# ============================================================================
def generate_table():
    reset_scene()

    # Dimensões exatas padronizadas (Metros)
    w = 1.08
    l = 1.92
    h_surface = 0.04
    rail_w = 0.075
    rail_h = 0.065
    goal_w = 0.38

    # Cores de Facção PBR
    mat_floor = create_pbr_material("TableFloorDark", (0.015, 0.022, 0.04, 1.0), metallic=0.60, roughness=0.18)
    mat_rails = create_pbr_material("RailTitanium", (0.05, 0.06, 0.08, 1.0), metallic=0.92, roughness=0.22)
    mat_bumpers = create_pbr_material("CornerArmor", (0.03, 0.035, 0.045, 1.0), metallic=0.80, roughness=0.35)
    
    mat_p1_neon = create_pbr_material("P1ElectricCyan", (0.0, 0.92, 1.0, 1.0), emission_color=(0.0, 0.92, 1.0, 1.0), emission_strength=5.0)
    mat_p2_neon = create_pbr_material("P2PlasmaOrange", (1.0, 0.40, 0.05, 1.0), emission_color=(1.0, 0.40, 0.05, 1.0), emission_strength=5.0)
    mat_white_laser = create_pbr_material("WhiteLaserGlow", (1.0, 0.98, 0.85, 1.0), emission_color=(1.0, 0.98, 0.85, 1.0), emission_strength=4.5)

    # 3.1 Superfície de Jogo (Placa Superior de Carbono)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, -h_surface / 2))
    floor = bpy.context.active_object
    floor.name = "TablePlaySurface"
    floor.scale = (w, l, h_surface)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(floor, width=0.004, segments=4)
    floor.data.materials.append(mat_floor)

    # 3.2 Linha Central Divisória Sci-Fi e Nó de Energia
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.001))
    center_line = bpy.context.active_object
    center_line.scale = (w * 0.98, 0.016, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    center_line.data.materials.append(mat_white_laser)

    # Círculo Central Neon com Núcleo Inset
    bpy.ops.mesh.primitive_cylinder_add(radius=0.18, depth=0.002, vertices=48, location=(0, 0, 0.001))
    center_outer = bpy.context.active_object
    center_outer.data.materials.append(mat_white_laser)

    bpy.ops.mesh.primitive_cylinder_add(radius=0.165, depth=0.0025, vertices=48, location=(0, 0, 0.001))
    center_inner = bpy.context.active_object
    center_inner.data.materials.append(mat_floor)

    # 3.3 Demarcação da Área da Meta (Arcos Defensivos Neon Embutidos)
    # Área da Meta Jogador 1 (Ciano) em Y = -l/2 + 0.35
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -l/2 + 0.32, 0.001))
    arc_p1 = bpy.context.active_object
    arc_p1.scale = (goal_w * 1.5, 0.008, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    arc_p1.data.materials.append(mat_p1_neon)

    # Área da Meta Jogador 2 (Laranja) em Y = l/2 - 0.35
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, l/2 - 0.32, 0.001))
    arc_p2 = bpy.context.active_object
    arc_p2.scale = (goal_w * 1.5, 0.008, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    arc_p2.data.materials.append(mat_p2_neon)

    # 3.4 Trilhos Laterais Chanfrados
    rail_len = l + rail_w * 2

    # Trilho Esquerdo
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-(w + rail_w) / 2, 0, rail_h / 2))
    rail_l = bpy.context.active_object
    rail_l.name = "RailLeft"
    rail_l.scale = (rail_w, rail_len, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(rail_l, width=0.004, segments=4)
    rail_l.data.materials.append(mat_rails)

    # Trilho Direito
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=((w + rail_w) / 2, 0, rail_h / 2))
    rail_r = bpy.context.active_object
    rail_r.name = "RailRight"
    rail_r.scale = (rail_w, rail_len, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(rail_r, width=0.004, segments=4)
    rail_r.data.materials.append(mat_rails)

    # Frisos Duplos de LED nos Trilhos (Interno e Superior)
    led_thick = 0.007
    led_h = 0.016
    
    # P1 LEDs (Ciano, Metade Inferior Y < 0)
    for x_pos in [-(w - led_thick)/2, (w - led_thick)/2]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, -l/4, rail_h * 0.72))
        led = bpy.context.active_object
        led.scale = (led_thick, l/2 * 0.98, led_h)
        bpy.ops.object.transform_apply(scale=True)
        led.data.materials.append(mat_p1_neon)

    # P2 LEDs (Laranja, Metade Superior Y > 0)
    for x_pos in [-(w - led_thick)/2, (w - led_thick)/2]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, l/4, rail_h * 0.72))
        led = bpy.context.active_object
        led.scale = (led_thick, l/2 * 0.98, led_h)
        bpy.ops.object.transform_apply(scale=True)
        led.data.materials.append(mat_p2_neon)

    # 3.5 Trilhos de Topo e Fundo (com Abertura da Meta de 0.38m)
    side_seg_w = (w - goal_w) / 2
    top_y = (l + rail_w) / 2
    bottom_y = -(l + rail_w) / 2

    for (pos_y, mat_color, prefix) in [(bottom_y, mat_p1_neon, "P1"), (top_y, mat_p2_neon, "P2")]:
        # Segmento Esquerdo
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-w/2 + side_seg_w/2, pos_y, rail_h / 2))
        seg_l = bpy.context.active_object
        seg_l.scale = (side_seg_w, rail_w, rail_h)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(seg_l, width=0.003, segments=4)
        seg_l.data.materials.append(mat_rails)

        # Segmento Direito
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(w/2 - side_seg_w/2, pos_y, rail_h / 2))
        seg_r = bpy.context.active_object
        seg_r.scale = (side_seg_w, rail_w, rail_h)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(seg_r, width=0.003, segments=4)
        seg_r.data.materials.append(mat_rails)

        # Frisos Luminosos nas Bordas da Meta
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-w/2 + side_seg_w/2, pos_y, rail_h * 0.75))
        f_l = bpy.context.active_object
        f_l.scale = (side_seg_w * 0.95, led_thick, led_h)
        bpy.ops.object.transform_apply(scale=True)
        f_l.data.materials.append(mat_color)

        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(w/2 - side_seg_w/2, pos_y, rail_h * 0.75))
        f_r = bpy.context.active_object
        f_r.scale = (side_seg_w * 0.95, led_thick, led_h)
        bpy.ops.object.transform_apply(scale=True)
        f_r.data.materials.append(mat_color)

    # 3.6 Protetores Angulares de Impacto nos 4 Cantos (Corner Armor Bumpers)
    c_offset_x = (w + rail_w) / 2
    c_offset_y = (l + rail_w) / 2
    corner_coords = [
        (-c_offset_x, -c_offset_y, mat_p1_neon),
        (c_offset_x, -c_offset_y, mat_p1_neon),
        (-c_offset_x, c_offset_y, mat_p2_neon),
        (c_offset_x, c_offset_y, mat_p2_neon),
    ]
    for cx, cy, c_mat in corner_coords:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(cx, cy, rail_h * 0.55))
        bumper = bpy.context.active_object
        bumper.scale = (rail_w * 1.35, rail_w * 1.35, rail_h * 1.15)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(bumper, width=0.005, segments=4)
        bumper.data.materials.append(mat_bumpers)

        # Indicador Chevron Luminoso no Canto
        bpy.ops.mesh.primitive_cylinder_add(radius=0.015, depth=rail_h * 1.2, vertices=16, location=(cx, cy, rail_h * 0.6))
        chev = bpy.context.active_object
        chev.data.materials.append(c_mat)

    # 3.7 Barreiras de Energia da Meta 3D (Portais Laser Futuristas)
    # Portal P1 (Ciano) e Portal P2 (Laranja)
    for (goal_y, goal_mat, sign) in [(bottom_y, mat_p1_neon, -1.0), (top_y, mat_p2_neon, 1.0)]:
        post_h = 0.12
        post_r = 0.018
        
        # Postes Flanqueadores da Meta (Esquerdo e Direito)
        for post_x in [-goal_w / 2, goal_w / 2]:
            bpy.ops.mesh.primitive_cylinder_add(
                radius=post_r,
                depth=post_h,
                vertices=24,
                location=(post_x, goal_y, post_h / 2)
            )
            post = bpy.context.active_object
            post.data.materials.append(mat_rails)

            # Cap luminoso do poste
            bpy.ops.mesh.primitive_cylinder_add(
                radius=post_r * 1.25,
                depth=0.02,
                vertices=24,
                location=(post_x, goal_y, post_h)
            )
            cap = bpy.context.active_object
            cap.data.materials.append(goal_mat)

        # Lintel Superior em Arco Transversal
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, goal_y, post_h))
        lintel = bpy.context.active_object
        lintel.scale = (goal_w + post_r * 2, 0.025, 0.018)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(lintel, width=0.003, segments=4)
        lintel.data.materials.append(mat_rails)

        # Feixe Laser Emissor da Barreira de Energia (Threshold de Contenção)
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, goal_y - sign * 0.01, post_h * 0.5))
        barrier_laser = bpy.context.active_object
        barrier_laser.scale = (goal_w * 0.98, 0.008, post_h * 0.85)
        bpy.ops.object.transform_apply(scale=True)
        barrier_laser.data.materials.append(goal_mat)

    # 3.8 Piso da Arena 3D Expandido (Plataforma Estádio 10m x 12m)
    arena_w = 10.0
    arena_l = 12.0
    arena_z = -h_surface - 0.06

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, arena_z))
    arena_floor = bpy.context.active_object
    arena_floor.name = "ArenaStadiumFloor"
    arena_floor.scale = (arena_w, arena_l, 0.025)
    bpy.ops.object.transform_apply(scale=True)
    mat_arena_floor = create_pbr_material("ArenaFloorDarkTitanium", (0.012, 0.018, 0.03, 1.0), metallic=0.75, roughness=0.35)
    arena_floor.data.materials.append(mat_arena_floor)

    # Grade Cibernética Neon
    mat_grid = create_pbr_material("ArenaGridCyberGlow", (0.0, 0.65, 0.9, 1.0), emission_color=(0.0, 0.60, 0.85, 1.0), emission_strength=2.2)
    for gx in [-4.0, -2.5, -1.25, 1.25, 2.5, 4.0]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(gx, 0, arena_z + 0.013))
        gline = bpy.context.active_object
        gline.scale = (0.015, arena_l * 0.94, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_grid)

    for gy in [-5.0, -3.5, -2.0, 0.0, 2.0, 3.5, 5.0]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, gy, arena_z + 0.013))
        gline = bpy.context.active_object
        gline.scale = (arena_w * 0.94, 0.015, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_grid)

    # 4 Balizadores Perimetrais de Iluminação da Arena (Torres Cilíndricas Multi-estágio)
    mat_pillar = create_pbr_material("PillarMat", (0.04, 0.05, 0.07, 1.0), metallic=0.88, roughness=0.20)
    pillar_coords = [
        (-arena_w/2 + 0.6, -arena_l/2 + 0.6, mat_p1_neon),
        (arena_w/2 - 0.6, -arena_l/2 + 0.6, mat_p1_neon),
        (-arena_w/2 + 0.6, arena_l/2 - 0.6, mat_p2_neon),
        (arena_w/2 - 0.6, arena_l/2 - 0.6, mat_p2_neon),
    ]
    for px, py, mat_beacon in pillar_coords:
        # Coluna Principal
        bpy.ops.mesh.primitive_cylinder_add(radius=0.08, depth=1.1, vertices=24, location=(px, py, arena_z + 0.55))
        pillar = bpy.context.active_object
        pillar.data.materials.append(mat_pillar)
        # Anel Intermediário de Emissão
        bpy.ops.mesh.primitive_cylinder_add(radius=0.092, depth=0.04, vertices=24, location=(px, py, arena_z + 0.70))
        p_ring = bpy.context.active_object
        p_ring.data.materials.append(mat_beacon)
        # Topo Luminoso
        bpy.ops.mesh.primitive_cylinder_add(radius=0.10, depth=0.14, vertices=24, location=(px, py, arena_z + 1.1))
        beacon = bpy.context.active_object
        beacon.data.materials.append(mat_beacon)

    out_path = os.path.join(OUTPUT_DIR, "mesa_arcade.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Table:", out_path)

if __name__ == "__main__":
    generate_puck()
    generate_mallet("rebatedor_p1", (0.0, 0.92, 1.0, 1.0), is_p1=True)
    generate_mallet("rebatedor_p2", (1.0, 0.40, 0.05, 1.0), is_p1=False)
    generate_table()
    print(">>> ALL_SCIFI_MODELS_GENERATED_SUCCESSFULLY")
