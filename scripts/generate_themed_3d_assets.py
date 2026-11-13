import bpy
import os
import math

OUTPUT_DIR = "/home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/assets/models"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)

def create_pbr(name, base_color, metallic=0.0, roughness=0.5, emission=None, emission_strength=1.0):
    mat = bpy.data.materials.get(name) or bpy.data.materials.new(name=name)
    mat.use_nodes = True
    mat.diffuse_color = base_color if not emission else emission
    bsdf = next((n for n in mat.node_tree.nodes if n.type == "BSDF_PRINCIPLED"), None)
    if bsdf:
        if "Base Color" in bsdf.inputs:
            bsdf.inputs["Base Color"].default_value = base_color
        if "Metallic" in bsdf.inputs:
            bsdf.inputs["Metallic"].default_value = metallic
        if "Roughness" in bsdf.inputs:
            bsdf.inputs["Roughness"].default_value = roughness
        if emission:
            if "Emission Color" in bsdf.inputs:
                bsdf.inputs["Emission Color"].default_value = emission
                if "Emission Strength" in bsdf.inputs:
                    bsdf.inputs["Emission Strength"].default_value = emission_strength
            elif "Emission" in bsdf.inputs:
                bsdf.inputs["Emission"].default_value = emission
    return mat

def add_bevel(obj, width=0.003, segments=4):
    bev = obj.modifiers.new(name="Bevel", type='BEVEL')
    bev.width = width
    bev.segments = segments
    return bev

# ============================================================================
# 1. CYBER FUTEBOL (CYBER_SOCCER) - Calibração Estética Fiel ao 2D & Classic 3D
# ============================================================================

def generate_cyber_soccer_puck():
    reset_scene()
    radius = 0.05
    depth = 0.015

    # 1.1 Chassi Principal em Tungstênio Grafite Escuro com Chanfro Suave
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    puck.name = "SoccerPuckChassis"
    add_bevel(puck, width=0.0025, segments=4)
    bpy.ops.object.shade_smooth()
    # Base metálica escura idêntica à paleta do 2D (0x101C15)
    mat_metal = create_pbr("SoccerPuckGraphite", (0.035, 0.048, 0.04, 1.0), metallic=0.92, roughness=0.18)
    puck.data.materials.append(mat_metal)

    # 1.2 Friso Neon Perimetral na Borda Externa (Trincheira do Equador)
    mat_glow = create_pbr("SoccerPuckEmeraldGlow", (0.0, 0.90, 0.46, 1.0), emission=(0.0, 0.90, 0.46, 1.0), emission_strength=5.0)
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 1.015, depth=depth * 0.25, vertices=64, location=(0, 0, 0))
    equator_glow = bpy.context.active_object
    equator_glow.name = "SoccerPuckEquatorGlow"
    equator_glow.data.materials.append(mat_glow)

    # 1.3 Anel Superior Neon Concêntrico Fino
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.76, depth=depth * 1.04, vertices=64, location=(0, 0, 0))
    ring = bpy.context.active_object
    ring.name = "SoccerPuckTopRing"
    ring.data.materials.append(mat_glow)

    # 1.4 Pentágono Central de Futebol Fino (Relevo com Núcleo Escuro)
    # Borda externa do pentágono (fina)
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.36, depth=depth * 1.08, vertices=5, location=(0, 0, 0))
    p_edge = bpy.context.active_object
    p_edge.data.materials.append(mat_glow)

    # Interior do pentágono escuro (0x08140D do 2D)
    mat_dark_pentagon = create_pbr("SoccerPentagonDark", (0.015, 0.025, 0.02, 1.0), metallic=0.90, roughness=0.25)
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.31, depth=depth * 1.12, vertices=5, location=(0, 0, 0))
    p_inner = bpy.context.active_object
    p_inner.data.materials.append(mat_dark_pentagon)

    # 1.5 5 Linhas Radiais Finas conectando os vértices do pentágono ao anel externo
    for i in range(5):
        angle = (i * 72.0 - 90.0) * (math.pi / 180.0)
        mid_dist = radius * 0.56
        fx = mid_dist * math.cos(angle)
        fy = mid_dist * math.sin(angle)
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(fx, fy, depth * 0.51))
        radial_line = bpy.context.active_object
        radial_line.scale = (0.0020, radius * 0.40, 0.0015)
        radial_line.rotation_euler = (0, 0, angle + math.pi / 2.0)
        bpy.ops.object.transform_apply(scale=True, rotation=True)
        radial_line.data.materials.append(mat_glow)

    # 1.6 Ponto de Energia Central Discreto (Ponto Branco/Dourado)
    mat_node = create_pbr("SoccerCoreNode", (0.9, 1.0, 0.95, 1.0), emission=(0.9, 1.0, 0.95, 1.0), emission_strength=5.0)
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.12, depth=depth * 1.15, vertices=24, location=(0, 0, 0))
    core_node = bpy.context.active_object
    core_node.data.materials.append(mat_node)

    out_path = os.path.join(OUTPUT_DIR, "disco_cyber_soccer.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Soccer Puck:", out_path)

def generate_cyber_soccer_mallet(name, neon_color, is_p1=True):
    reset_scene()
    base_radius = 0.08
    base_depth = 0.02
    knob_radius = 0.034
    knob_height = 0.052

    mat_dark_carbon = create_pbr("SoccerMalletCarbon", (0.05, 0.055, 0.065, 1.0), metallic=0.88, roughness=0.22)
    mat_rubber = create_pbr("SoccerGripRubber", (0.02, 0.022, 0.025, 1.0), metallic=0.05, roughness=0.85)
    mat_neon = create_pbr("SoccerMalletNeon", neon_color, emission=neon_color, emission_strength=5.0)

    # Base com Chanfro
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius, depth=base_depth, vertices=64, location=(0, 0, base_depth / 2))
    base = bpy.context.active_object
    add_bevel(base, width=0.0035, segments=4)
    bpy.ops.object.shade_smooth()
    base.data.materials.append(mat_dark_carbon)

    # Anel Neon Inferior na Borda Perimetral (Fino)
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius * 1.012, depth=base_depth * 0.25, vertices=64, location=(0, 0, base_depth * 0.25))
    lower_ring = bpy.context.active_object
    lower_ring.data.materials.append(mat_neon)

    # Anel Neon no Degrau Superior
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius * 0.88, depth=base_depth * 1.06, vertices=64, location=(0, 0, base_depth / 2))
    step_ring = bpy.context.active_object
    step_ring.data.materials.append(mat_neon)

    # Haste Manopla (Knob)
    bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius, depth=knob_height, vertices=48, location=(0, 0, base_depth + knob_height / 2))
    knob = bpy.context.active_object
    add_bevel(knob, width=0.003, segments=4)
    bpy.ops.object.shade_smooth()
    knob.data.materials.append(mat_dark_carbon)

    # Anéis Emborrachados de Tração (Grip Ribs)
    for z_off in [0.012, 0.024, 0.036]:
        bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius * 1.05, depth=0.004, vertices=48, location=(0, 0, base_depth + z_off))
        rib = bpy.context.active_object
        rib.data.materials.append(mat_rubber)

    # Cúpula Superior
    dome_z = base_depth + knob_height
    bpy.ops.mesh.primitive_uv_sphere_add(radius=knob_radius * 0.90, segments=32, ring_count=16, location=(0, 0, dome_z))
    dome = bpy.context.active_object
    bpy.ops.object.shade_smooth()
    dome.data.materials.append(mat_neon)

    # Coroa Protetora com Entalhes
    bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius * 0.96, depth=0.007, vertices=32, location=(0, 0, dome_z))
    crown = bpy.context.active_object
    crown.data.materials.append(mat_dark_carbon)

    filename = f"{name}.glb"
    out_path = os.path.join(OUTPUT_DIR, filename)
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Soccer Mallet:", out_path)

def generate_cyber_soccer_table():
    reset_scene()
    w = 1.08
    l = 1.92
    h_surface = 0.04
    rail_w = 0.075
    rail_h = 0.065
    goal_w = 0.38

    # Superfície do gramado: ZERO emissão, idêntico à mesa arcade clássica
    mat_turf_base = create_pbr("SoccerTurfDark", (0.012, 0.028, 0.018, 1.0), metallic=0.55, roughness=0.25)
    mat_turf_stripe = create_pbr("SoccerTurfStripe", (0.016, 0.035, 0.022, 1.0), metallic=0.55, roughness=0.25)
    mat_rails = create_pbr("RailTitaniumDark", (0.05, 0.06, 0.08, 1.0), metallic=0.92, roughness=0.22)
    mat_bumpers = create_pbr("CornerArmorCarbon", (0.03, 0.035, 0.045, 1.0), metallic=0.80, roughness=0.35)

    # Linhas de laser calibradas exatamente para 4.5 e 5.0 (sem ofuscamento)
    mat_emerald_laser = create_pbr("EmeraldLaser", (0.0, 0.90, 0.46, 1.0), emission=(0.0, 0.90, 0.46, 1.0), emission_strength=5.0)
    mat_ruby_laser = create_pbr("RubyLaser", (1.0, 0.12, 0.25, 1.0), emission=(1.0, 0.12, 0.25, 1.0), emission_strength=5.0)
    mat_white_laser = create_pbr("WhiteFieldLine", (1.0, 0.98, 0.92, 1.0), emission=(1.0, 0.98, 0.92, 1.0), emission_strength=4.5)

    # 1. Base do Campo de Jogo (Sem emissão)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, -h_surface / 2))
    floor = bpy.context.active_object
    floor.name = "TablePlaySurface"
    floor.scale = (w, l, h_surface)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(floor, width=0.004, segments=4)
    floor.data.materials.append(mat_turf_base)

    # Faixas de gramado alternadas (Sem emissão)
    stripe_count = 12
    stripe_len = l / stripe_count
    for i in range(0, stripe_count, 2):
        sy = -l/2 + (i + 0.5) * stripe_len
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, sy, 0.0005))
        stripe = bpy.context.active_object
        stripe.scale = (w * 0.99, stripe_len * 0.98, 0.001)
        bpy.ops.object.transform_apply(scale=True)
        stripe.data.materials.append(mat_turf_stripe)

    # 2. Linhas de Campo Finas e Elegantes (Espessura 0.010m)
    # Linha Central Divisória
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.0015))
    center_line = bpy.context.active_object
    center_line.scale = (w * 0.98, 0.012, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    center_line.data.materials.append(mat_white_laser)

    # Círculo Central Neon com Núcleo Inset Escuro
    bpy.ops.mesh.primitive_cylinder_add(radius=0.18, depth=0.002, vertices=48, location=(0, 0, 0.0015))
    c_out = bpy.context.active_object
    c_out.data.materials.append(mat_white_laser)

    bpy.ops.mesh.primitive_cylinder_add(radius=0.166, depth=0.0025, vertices=48, location=(0, 0, 0.0015))
    c_in = bpy.context.active_object
    c_in.data.materials.append(mat_turf_base)

    # Ponto Central
    bpy.ops.mesh.primitive_cylinder_add(radius=0.018, depth=0.003, vertices=24, location=(0, 0, 0.002))
    c_spot = bpy.context.active_object
    c_spot.data.materials.append(mat_white_laser)

    # Arcos e Linhas das Áreas de Meta (Traços Finos de 0.008m)
    for (pos_y, sign, mat_color) in [(-l/2, -1.0, mat_emerald_laser), (l/2, 1.0, mat_ruby_laser)]:
        box_y = pos_y - sign * 0.32
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, box_y, 0.0015))
        arc_box = bpy.context.active_object
        arc_box.scale = (goal_w * 1.5, 0.008, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        arc_box.data.materials.append(mat_color)

        # Laterais da área
        for sx in [-goal_w * 0.75, goal_w * 0.75]:
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(sx, pos_y - sign * 0.16, 0.0015))
            side_line = bpy.context.active_object
            side_line.scale = (0.008, 0.32, 0.002)
            bpy.ops.object.transform_apply(scale=True)
            side_line.data.materials.append(mat_color)

    # 3. Trilhos Laterais em Titânio com LEDs Finos
    rail_len = l + rail_w * 2

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-(w + rail_w) / 2, 0, rail_h / 2))
    rail_l = bpy.context.active_object
    rail_l.name = "RailLeft"
    rail_l.scale = (rail_w, rail_len, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(rail_l, width=0.004, segments=4)
    rail_l.data.materials.append(mat_rails)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=((w + rail_w) / 2, 0, rail_h / 2))
    rail_r = bpy.context.active_object
    rail_r.name = "RailRight"
    rail_r.scale = (rail_w, rail_len, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(rail_r, width=0.004, segments=4)
    rail_r.data.materials.append(mat_rails)

    # Frisos LED nos Trilhos (Espessura fina calibrada de 0.007m)
    led_thick = 0.007
    led_h = 0.016
    for x_pos in [-(w - led_thick)/2, (w - led_thick)/2]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, -l/4, rail_h * 0.72))
        led = bpy.context.active_object
        led.scale = (led_thick, l/2 * 0.98, led_h)
        bpy.ops.object.transform_apply(scale=True)
        led.data.materials.append(mat_emerald_laser)

        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, l/4, rail_h * 0.72))
        led = bpy.context.active_object
        led.scale = (led_thick, l/2 * 0.98, led_h)
        bpy.ops.object.transform_apply(scale=True)
        led.data.materials.append(mat_ruby_laser)

    # 4. Trilhos de Topo e Fundo com Abertura da Meta de 0.38m
    side_seg_w = (w - goal_w) / 2
    top_y = (l + rail_w) / 2
    bottom_y = -(l + rail_w) / 2

    for (pos_y, mat_color) in [(bottom_y, mat_emerald_laser), (top_y, mat_ruby_laser)]:
        for seg_x in [-w/2 + side_seg_w/2, w/2 - side_seg_w/2]:
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(seg_x, pos_y, rail_h / 2))
            seg = bpy.context.active_object
            seg.scale = (side_seg_w, rail_w, rail_h)
            bpy.ops.object.transform_apply(scale=True)
            add_bevel(seg, width=0.003, segments=4)
            seg.data.materials.append(mat_rails)

            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(seg_x, pos_y, rail_h * 0.75))
            f_led = bpy.context.active_object
            f_led.scale = (side_seg_w * 0.95, led_thick, led_h)
            bpy.ops.object.transform_apply(scale=True)
            f_led.data.materials.append(mat_color)

    # 5. Cantos Protetores de Impacto
    c_offset_x = (w + rail_w) / 2
    c_offset_y = (l + rail_w) / 2
    for cx, cy, c_mat in [
        (-c_offset_x, -c_offset_y, mat_emerald_laser),
        (c_offset_x, -c_offset_y, mat_emerald_laser),
        (-c_offset_x, c_offset_y, mat_ruby_laser),
        (c_offset_x, c_offset_y, mat_ruby_laser)
    ]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(cx, cy, rail_h * 0.55))
        bumper = bpy.context.active_object
        bumper.scale = (rail_w * 1.35, rail_w * 1.35, rail_h * 1.15)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(bumper, width=0.005, segments=4)
        bumper.data.materials.append(mat_bumpers)

        bpy.ops.mesh.primitive_cylinder_add(radius=0.015, depth=rail_h * 1.2, vertices=16, location=(cx, cy, rail_h * 0.6))
        chev = bpy.context.active_object
        chev.data.materials.append(c_mat)

    # 6. Postes de Trave em Titânio com Feixe Laser Fino (Idêntico ao padrão arcade clássico)
    for (goal_y, goal_mat, sign) in [
        (bottom_y, mat_emerald_laser, -1.0),
        (top_y, mat_ruby_laser, 1.0)
    ]:
        post_h = 0.12
        post_r = 0.018

        for post_x in [-goal_w / 2, goal_w / 2]:
            bpy.ops.mesh.primitive_cylinder_add(radius=post_r, depth=post_h, vertices=24, location=(post_x, goal_y, post_h / 2))
            post = bpy.context.active_object
            post.data.materials.append(mat_rails)

            bpy.ops.mesh.primitive_cylinder_add(radius=post_r * 1.25, depth=0.02, vertices=24, location=(post_x, goal_y, post_h))
            cap = bpy.context.active_object
            cap.data.materials.append(goal_mat)

        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, goal_y, post_h))
        lintel = bpy.context.active_object
        lintel.scale = (goal_w + post_r * 2, 0.025, 0.018)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(lintel, width=0.003, segments=4)
        lintel.data.materials.append(mat_rails)

        # Feixe Laser Fino de Contenção (0.008m)
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, goal_y - sign * 0.01, post_h * 0.5))
        barrier = bpy.context.active_object
        barrier.scale = (goal_w * 0.98, 0.008, post_h * 0.85)
        bpy.ops.object.transform_apply(scale=True)
        barrier.data.materials.append(goal_mat)

    # 7. Piso Estádio Expandido (10m x 12m)
    arena_w = 10.0
    arena_l = 12.0
    arena_z = -h_surface - 0.06

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, arena_z))
    arena_floor = bpy.context.active_object
    arena_floor.name = "ArenaStadiumFloor"
    arena_floor.scale = (arena_w, arena_l, 0.025)
    bpy.ops.object.transform_apply(scale=True)
    mat_stadium = create_pbr("StadiumFloorDark", (0.012, 0.018, 0.03, 1.0), metallic=0.75, roughness=0.35)
    arena_floor.data.materials.append(mat_stadium)

    mat_stadium_grid = create_pbr("StadiumGridGlow", (0.0, 0.70, 0.40, 1.0), emission=(0.0, 0.65, 0.35, 1.0), emission_strength=2.2)
    for gx in [-4.0, -2.5, -1.25, 1.25, 2.5, 4.0]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(gx, 0, arena_z + 0.013))
        gline = bpy.context.active_object
        gline.scale = (0.015, arena_l * 0.94, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_stadium_grid)

    for gy in [-5.0, -3.5, -2.0, 0.0, 2.0, 3.5, 5.0]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, gy, arena_z + 0.013))
        gline = bpy.context.active_object
        gline.scale = (arena_w * 0.94, 0.015, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_stadium_grid)

    for px, py, mat_beacon in [
        (-arena_w/2 + 0.6, -arena_l/2 + 0.6, mat_emerald_laser),
        (arena_w/2 - 0.6, -arena_l/2 + 0.6, mat_emerald_laser),
        (-arena_w/2 + 0.6, arena_l/2 - 0.6, mat_ruby_laser),
        (arena_w/2 - 0.6, arena_l/2 - 0.6, mat_ruby_laser)
    ]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.08, depth=1.1, vertices=24, location=(px, py, arena_z + 0.55))
        pillar = bpy.context.active_object
        pillar.data.materials.append(mat_rails)

        bpy.ops.mesh.primitive_cylinder_add(radius=0.092, depth=0.04, vertices=24, location=(px, py, arena_z + 0.70))
        p_ring = bpy.context.active_object
        p_ring.data.materials.append(mat_beacon)

        bpy.ops.mesh.primitive_cylinder_add(radius=0.10, depth=0.14, vertices=24, location=(px, py, arena_z + 1.1))
        beacon = bpy.context.active_object
        beacon.data.materials.append(mat_beacon)

    out_path = os.path.join(OUTPUT_DIR, "mesa_cyber_soccer.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Soccer Table:", out_path)

# ============================================================================
# 2. NEO-TOKYO BASQUETE (NEO_BASKETBALL) - Calibração Estética Fiel ao 2D
# ============================================================================

def generate_neo_basketball_puck():
    reset_scene()
    radius = 0.05
    depth = 0.015

    # 2.1 Chassi em Couro Esportivo Laranja Queimado (Idêntico ao 2D 0xD84315)
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    puck.name = "BasketballPuckChassis"
    add_bevel(puck, width=0.003, segments=4)
    bpy.ops.object.shade_smooth()
    mat_ball = create_pbr(
        "BasketballLeather",
        (0.95, 0.36, 0.04, 1.0),
        metallic=0.15,
        roughness=0.50,
        emission=(0.90, 0.30, 0.02, 1.0),
        emission_strength=2.4
    )
    puck.data.materials.append(mat_ball)

    # 2.2 Friso Neon Equatorial Dourado Fino (Calibrado 5.0)
    mat_gold_glow = create_pbr("BasketballGoldPlasma", (1.0, 0.70, 0.05, 1.0), emission=(1.0, 0.70, 0.05, 1.0), emission_strength=5.0)
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 1.015, depth=depth * 0.25, vertices=64, location=(0, 0, 0))
    equator_glow = bpy.context.active_object
    equator_glow.data.materials.append(mat_gold_glow)

    # 2.3 Ranhuras Pretas Recuadas Características do Basquete (0x140803 do 2D)
    mat_black_seam = create_pbr("BasketballSeamBlack", (0.010, 0.008, 0.008, 1.0), metallic=0.10, roughness=0.85)

    # Seams Cruzadas
    for rot in [0.0, math.pi / 2.0]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, depth * 0.505))
        seam = bpy.context.active_object
        seam.scale = (radius * 1.95, 0.0038, 0.0022)
        seam.rotation_euler = (0, 0, rot)
        bpy.ops.object.transform_apply(scale=True, rotation=True)
        seam.data.materials.append(mat_black_seam)

    # Arcos Parabólicos de Basquete Recuados
    for sign in [-1.0, 1.0]:
        bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.58, depth=depth * 1.04, vertices=48, location=(sign * radius * 0.42, 0, 0))
        arc_seam = bpy.context.active_object
        arc_seam.scale = (0.75, 1.25, 1.0)
        bpy.ops.object.transform_apply(scale=True)
        arc_seam.data.materials.append(mat_black_seam)

        bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.55, depth=depth * 1.08, vertices=48, location=(sign * radius * 0.42, 0, 0))
        arc_inner = bpy.context.active_object
        arc_inner.scale = (0.75, 1.25, 1.0)
        bpy.ops.object.transform_apply(scale=True)
        arc_inner.data.materials.append(mat_ball)

    # Anel Superior Fino Dourado
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.74, depth=depth * 1.04, vertices=64, location=(0, 0, 0))
    top_ring = bpy.context.active_object
    top_ring.data.materials.append(mat_gold_glow)

    out_path = os.path.join(OUTPUT_DIR, "disco_neo_basketball.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Basketball Puck:", out_path)

def generate_neo_basketball_mallet(name, neon_color, is_p1=True):
    reset_scene()
    base_radius = 0.08
    base_depth = 0.02
    knob_radius = 0.034
    knob_height = 0.052

    mat_dark_titanium = create_pbr("DunkStrikerTitanium", (0.05, 0.05, 0.06, 1.0), metallic=0.90, roughness=0.22)
    mat_basketball_leather = create_pbr("DunkGripLeather", (0.35, 0.14, 0.03, 1.0), metallic=0.10, roughness=0.75)
    mat_neon = create_pbr("DunkStrikerNeon", neon_color, emission=neon_color, emission_strength=5.0)

    # Base com Chanfro
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius, depth=base_depth, vertices=64, location=(0, 0, base_depth / 2))
    base = bpy.context.active_object
    add_bevel(base, width=0.0035, segments=4)
    bpy.ops.object.shade_smooth()
    base.data.materials.append(mat_dark_titanium)

    # Anel Neon Inferior
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius * 1.012, depth=base_depth * 0.25, vertices=64, location=(0, 0, base_depth * 0.25))
    lower_ring = bpy.context.active_object
    lower_ring.data.materials.append(mat_neon)

    # Anel Neon Superior da Base
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius * 0.88, depth=base_depth * 1.06, vertices=64, location=(0, 0, base_depth / 2))
    step_ring = bpy.context.active_object
    step_ring.data.materials.append(mat_neon)

    # Haste Manopla
    bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius, depth=knob_height, vertices=48, location=(0, 0, base_depth + knob_height / 2))
    knob = bpy.context.active_object
    add_bevel(knob, width=0.003, segments=4)
    bpy.ops.object.shade_smooth()
    knob.data.materials.append(mat_dark_titanium)

    # Pegada Texturizada em Couro
    bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius * 1.04, depth=knob_height * 0.65, vertices=48, location=(0, 0, base_depth + knob_height * 0.5))
    grip = bpy.context.active_object
    grip.data.materials.append(mat_basketball_leather)

    # Cúpula Superior
    dome_z = base_depth + knob_height
    bpy.ops.mesh.primitive_uv_sphere_add(radius=knob_radius * 0.90, segments=32, ring_count=16, location=(0, 0, dome_z))
    dome = bpy.context.active_object
    bpy.ops.object.shade_smooth()
    dome.data.materials.append(mat_neon)

    # Coroa Protetora
    bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius * 0.96, depth=0.007, vertices=32, location=(0, 0, dome_z))
    crown = bpy.context.active_object
    crown.data.materials.append(mat_dark_titanium)

    filename = f"{name}.glb"
    out_path = os.path.join(OUTPUT_DIR, filename)
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Basketball Mallet:", out_path)

def generate_neo_basketball_table():
    reset_scene()
    w = 1.08
    l = 1.92
    h_surface = 0.04
    rail_w = 0.075
    rail_h = 0.065
    goal_w = 0.38

    # Piso da quadra: ZERO emissão, madeira cibernética tratada idêntica ao 2D
    mat_court_base = create_pbr("CourtHardwoodDark", (0.022, 0.016, 0.012, 1.0), metallic=0.45, roughness=0.30)
    mat_court_plank = create_pbr("CourtHardwoodPlank", (0.028, 0.020, 0.015, 1.0), metallic=0.45, roughness=0.30)
    mat_rails = create_pbr("RailMatteBlack", (0.05, 0.06, 0.08, 1.0), metallic=0.92, roughness=0.22)
    mat_bumpers = create_pbr("CornerArmorBasketball", (0.03, 0.035, 0.045, 1.0), metallic=0.80, roughness=0.35)

    mat_gold_laser = create_pbr("BasketballGoldLaser", (1.0, 0.70, 0.05, 1.0), emission=(1.0, 0.70, 0.05, 1.0), emission_strength=5.0)
    mat_cyan_laser = create_pbr("BasketballCyanLaser", (0.0, 0.88, 1.0, 1.0), emission=(0.0, 0.88, 1.0, 1.0), emission_strength=5.0)

    # 1. Base da Quadra
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, -h_surface / 2))
    floor = bpy.context.active_object
    floor.name = "TablePlaySurface"
    floor.scale = (w, l, h_surface)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(floor, width=0.004, segments=4)
    floor.data.materials.append(mat_court_base)

    # Tábuas de madeira longitudinais sutis (sem emissão)
    plank_count = 10
    plank_w = w / plank_count
    for i in range(0, plank_count, 2):
        px = -w/2 + (i + 0.5) * plank_w
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(px, 0, 0.0005))
        plank = bpy.context.active_object
        plank.scale = (plank_w * 0.98, l * 0.99, 0.001)
        bpy.ops.object.transform_apply(scale=True)
        plank.data.materials.append(mat_court_plank)

    # 2. Linhas de Quadra Finas (Espessura 0.010m)
    # Linha Central
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.0015))
    center_line = bpy.context.active_object
    center_line.scale = (w * 0.98, 0.012, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    center_line.data.materials.append(mat_cyan_laser)

    # Círculo Central Dourado com Núcleo Inset
    bpy.ops.mesh.primitive_cylinder_add(radius=0.18, depth=0.002, vertices=48, location=(0, 0, 0.0015))
    c_out = bpy.context.active_object
    c_out.data.materials.append(mat_gold_laser)

    bpy.ops.mesh.primitive_cylinder_add(radius=0.166, depth=0.0025, vertices=48, location=(0, 0, 0.0015))
    c_in = bpy.context.active_object
    c_in.data.materials.append(mat_court_base)

    # Demarcação de Garrafão (Linhas Finas sem blocos sólidos)
    for (pos_y, sign, mat_color) in [(-l/2, -1.0, mat_gold_laser), (l/2, 1.0, mat_cyan_laser)]:
        key_len = 0.45
        key_w = 0.36

        # Linha de Lance Livre
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, pos_y - sign * key_len, 0.0015))
        ft_line = bpy.context.active_object
        ft_line.scale = (key_w, 0.008, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        ft_line.data.materials.append(mat_color)

        # Laterais do Garrafão
        for side_x in [-key_w/2, key_w/2]:
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(side_x, pos_y - sign * key_len / 2, 0.0015))
            s_line = bpy.context.active_object
            s_line.scale = (0.008, key_len, 0.002)
            bpy.ops.object.transform_apply(scale=True)
            s_line.data.materials.append(mat_color)

        # Círculo de Lance Livre Fino
        bpy.ops.mesh.primitive_cylinder_add(radius=0.13, depth=0.002, vertices=32, location=(0, pos_y - sign * key_len, 0.0015))
        ft_circle = bpy.context.active_object
        ft_circle.data.materials.append(mat_color)
        bpy.ops.mesh.primitive_cylinder_add(radius=0.122, depth=0.0025, vertices=32, location=(0, pos_y - sign * key_len, 0.0015))
        ft_in = bpy.context.active_object
        ft_in.data.materials.append(mat_court_base)

    # 3. Trilhos Laterais em Titânio
    rail_len = l + rail_w * 2

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-(w + rail_w) / 2, 0, rail_h / 2))
    rail_l = bpy.context.active_object
    rail_l.name = "RailLeft"
    rail_l.scale = (rail_w, rail_len, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(rail_l, width=0.004, segments=4)
    rail_l.data.materials.append(mat_rails)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=((w + rail_w) / 2, 0, rail_h / 2))
    rail_r = bpy.context.active_object
    rail_r.name = "RailRight"
    rail_r.scale = (rail_w, rail_len, rail_h)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(rail_r, width=0.004, segments=4)
    rail_r.data.materials.append(mat_rails)

    # Frisos LED
    led_thick = 0.007
    led_h = 0.016
    for x_pos in [-(w - led_thick)/2, (w - led_thick)/2]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, -l/4, rail_h * 0.72))
        led = bpy.context.active_object
        led.scale = (led_thick, l/2 * 0.98, led_h)
        bpy.ops.object.transform_apply(scale=True)
        led.data.materials.append(mat_gold_laser)

        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, l/4, rail_h * 0.72))
        led = bpy.context.active_object
        led.scale = (led_thick, l/2 * 0.98, led_h)
        bpy.ops.object.transform_apply(scale=True)
        led.data.materials.append(mat_cyan_laser)

    # 4. Trilhos de Topo e Fundo com Abertura da Meta
    side_seg_w = (w - goal_w) / 2
    top_y = (l + rail_w) / 2
    bottom_y = -(l + rail_w) / 2

    for (pos_y, mat_color) in [(bottom_y, mat_gold_laser), (top_y, mat_cyan_laser)]:
        for seg_x in [-w/2 + side_seg_w/2, w/2 - side_seg_w/2]:
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(seg_x, pos_y, rail_h / 2))
            seg = bpy.context.active_object
            seg.scale = (side_seg_w, rail_w, rail_h)
            bpy.ops.object.transform_apply(scale=True)
            add_bevel(seg, width=0.003, segments=4)
            seg.data.materials.append(mat_rails)

            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(seg_x, pos_y, rail_h * 0.75))
            f_led = bpy.context.active_object
            f_led.scale = (side_seg_w * 0.95, led_thick, led_h)
            bpy.ops.object.transform_apply(scale=True)
            f_led.data.materials.append(mat_color)

    # 5. Cantos Protetores
    c_offset_x = (w + rail_w) / 2
    c_offset_y = (l + rail_w) / 2
    for cx, cy, c_mat in [
        (-c_offset_x, -c_offset_y, mat_gold_laser),
        (c_offset_x, -c_offset_y, mat_gold_laser),
        (-c_offset_x, c_offset_y, mat_cyan_laser),
        (c_offset_x, c_offset_y, mat_cyan_laser)
    ]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(cx, cy, rail_h * 0.55))
        bumper = bpy.context.active_object
        bumper.scale = (rail_w * 1.35, rail_w * 1.35, rail_h * 1.15)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(bumper, width=0.005, segments=4)
        bumper.data.materials.append(mat_bumpers)

        bpy.ops.mesh.primitive_cylinder_add(radius=0.015, depth=rail_h * 1.2, vertices=16, location=(cx, cy, rail_h * 0.6))
        chev = bpy.context.active_object
        chev.data.materials.append(c_mat)

    # 6. Postes de Trave em Titânio com Feixe Laser Fino
    for (goal_y, goal_mat, sign) in [
        (bottom_y, mat_gold_laser, -1.0),
        (top_y, mat_cyan_laser, 1.0)
    ]:
        post_h = 0.12
        post_r = 0.018

        for post_x in [-goal_w / 2, goal_w / 2]:
            bpy.ops.mesh.primitive_cylinder_add(radius=post_r, depth=post_h, vertices=24, location=(post_x, goal_y, post_h / 2))
            post = bpy.context.active_object
            post.data.materials.append(mat_rails)

            bpy.ops.mesh.primitive_cylinder_add(radius=post_r * 1.25, depth=0.02, vertices=24, location=(post_x, goal_y, post_h))
            cap = bpy.context.active_object
            cap.data.materials.append(goal_mat)

        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, goal_y, post_h))
        lintel = bpy.context.active_object
        lintel.scale = (goal_w + post_r * 2, 0.025, 0.018)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(lintel, width=0.003, segments=4)
        lintel.data.materials.append(mat_rails)

        # Feixe Laser Fino de Contenção (0.008m)
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, goal_y - sign * 0.01, post_h * 0.5))
        barrier = bpy.context.active_object
        barrier.scale = (goal_w * 0.98, 0.008, post_h * 0.85)
        bpy.ops.object.transform_apply(scale=True)
        barrier.data.materials.append(goal_mat)

    # 7. Piso Estádio Expandido
    arena_w = 10.0
    arena_l = 12.0
    arena_z = -h_surface - 0.06

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, arena_z))
    arena_floor = bpy.context.active_object
    arena_floor.name = "ArenaStadiumFloor"
    arena_floor.scale = (arena_w, arena_l, 0.025)
    bpy.ops.object.transform_apply(scale=True)
    mat_stadium = create_pbr("BasketballStadiumFloorDark", (0.012, 0.018, 0.03, 1.0), metallic=0.75, roughness=0.35)
    arena_floor.data.materials.append(mat_stadium)

    mat_stadium_grid = create_pbr("BasketballGridGlow", (1.0, 0.65, 0.05, 1.0), emission=(0.95, 0.60, 0.05, 1.0), emission_strength=2.2)
    for gx in [-4.0, -2.5, -1.25, 1.25, 2.5, 4.0]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(gx, 0, arena_z + 0.013))
        gline = bpy.context.active_object
        gline.scale = (0.015, arena_l * 0.94, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_stadium_grid)

    for gy in [-5.0, -3.5, -2.0, 0.0, 2.0, 3.5, 5.0]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, gy, arena_z + 0.013))
        gline = bpy.context.active_object
        gline.scale = (arena_w * 0.94, 0.015, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_stadium_grid)

    for px, py, mat_beacon in [
        (-arena_w/2 + 0.6, -arena_l/2 + 0.6, mat_gold_laser),
        (arena_w/2 - 0.6, -arena_l/2 + 0.6, mat_gold_laser),
        (-arena_w/2 + 0.6, arena_l/2 - 0.6, mat_cyan_laser),
        (arena_w/2 - 0.6, arena_l/2 - 0.6, mat_cyan_laser)
    ]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.08, depth=1.1, vertices=24, location=(px, py, arena_z + 0.55))
        pillar = bpy.context.active_object
        pillar.data.materials.append(mat_rails)

        bpy.ops.mesh.primitive_cylinder_add(radius=0.092, depth=0.04, vertices=24, location=(px, py, arena_z + 0.70))
        p_ring = bpy.context.active_object
        p_ring.data.materials.append(mat_beacon)

        bpy.ops.mesh.primitive_cylinder_add(radius=0.10, depth=0.14, vertices=24, location=(px, py, arena_z + 1.1))
        beacon = bpy.context.active_object
        beacon.data.materials.append(mat_beacon)

    out_path = os.path.join(OUTPUT_DIR, "mesa_neo_basketball.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Basketball Table:", out_path)

if __name__ == "__main__":
    print("=== Generating Recalibrated Cyber Futebol 3D Assets ===")
    generate_cyber_soccer_puck()
    generate_cyber_soccer_mallet("rebatedor_cyber_soccer_p1", (0.0, 0.90, 0.46, 1.0), is_p1=True)
    generate_cyber_soccer_mallet("rebatedor_cyber_soccer_p2", (1.0, 0.12, 0.25, 1.0), is_p1=False)
    generate_cyber_soccer_table()

    print("=== Generating Recalibrated Neo-Tokyo Basquete 3D Assets ===")
    generate_neo_basketball_puck()
    generate_neo_basketball_mallet("rebatedor_neo_basketball_p1", (1.0, 0.70, 0.05, 1.0), is_p1=True)
    generate_neo_basketball_mallet("rebatedor_neo_basketball_p2", (0.0, 0.88, 1.0, 1.0), is_p1=False)
    generate_neo_basketball_table()

    print("=== ALL RECALIBRATED ASSETS EXPORTED SUCCESSFULLY ===")
