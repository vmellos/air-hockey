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

# Paleta Kiara Cat
PINK_MAGENTA = (1.0, 0.08, 0.58, 1.0)
CYAN_ELECTRIC = (0.0, 0.9, 1.0, 1.0)
GOLD_AMBER = (1.0, 0.78, 0.1, 1.0)
DARK_SURFACE = (0.025, 0.018, 0.035, 1.0)
DARK_RAIL = (0.04, 0.035, 0.05, 1.0)
DARK_BUMPER = (0.06, 0.04, 0.07, 1.0)

# ============================================================================
# 1. DISCO KIARA PAW (disco_kiara_paw.glb)
# ============================================================================
def generate_puck():
    reset_scene()
    radius = 0.05
    depth = 0.015

    # 1. Chassi Principal
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    puck.name = "KiaraPuckChassis"
    mat_chassis = create_pbr("KiaraPuckChassisMat", (0.05, 0.04, 0.06, 1.0), metallic=0.9, roughness=0.2)
    puck.data.materials.append(mat_chassis)
    add_bevel(puck, width=0.002, segments=4)

    # 2. Anel de Energia Superior
    mat_glow = create_pbr("KiaraPuckGlowMat", PINK_MAGENTA, emission=PINK_MAGENTA, emission_strength=4.0)
    bpy.ops.mesh.primitive_torus_add(
        major_radius=radius * 0.82,
        minor_radius=0.002,
        major_segments=48,
        minor_segments=16,
        location=(0, 0, depth / 2)
    )
    ring = bpy.context.active_object
    ring.name = "KiaraPuckEnergyRing"
    ring.data.materials.append(mat_glow)

    # 3. Patinha de Gato (Almofada Principal + 4 Dedinhos)
    mat_paw = create_pbr("KiaraPuckPawMat", GOLD_AMBER, emission=GOLD_AMBER, emission_strength=4.5)
    
    # Almofada principal (elipsoide achatada)
    bpy.ops.mesh.primitive_cylinder_add(radius=0.013, depth=0.0025, vertices=32, location=(0, -0.004, depth / 2 + 0.001))
    pad = bpy.context.active_object
    pad.name = "KiaraMainPad"
    pad.scale = (1.2, 0.9, 1.0)
    bpy.ops.object.transform_apply(scale=True)
    pad.data.materials.append(mat_paw)

    # 4 Dedinhos
    toe_offsets = [
        (-0.016, 0.012, 0.0042),
        (-0.006, 0.018, 0.0046),
        (0.006, 0.018, 0.0046),
        (0.016, 0.012, 0.0042),
    ]
    for idx, (tx, ty, tr) in enumerate(toe_offsets):
        bpy.ops.mesh.primitive_cylinder_add(radius=tr, depth=0.0025, vertices=24, location=(tx, ty, depth / 2 + 0.001))
        toe = bpy.context.active_object
        toe.name = f"KiaraToe_{idx}"
        toe.data.materials.append(mat_paw)

    out_path = os.path.join(OUTPUT_DIR, "disco_kiara_paw.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Puck:", out_path)

# ============================================================================
# 2. REBATEDOR NEKO STRIKER (rebatedor_kiara_p1.glb / p2.glb)
# ============================================================================
def generate_mallet(name, faction_color, is_p1=True):
    reset_scene()
    base_r = 0.075
    base_h = 0.02
    handle_r_bottom = 0.035
    handle_r_top = 0.024
    handle_h = 0.05
    grip_r = 0.030
    grip_h = 0.014

    mat_chassis = create_pbr(f"{name}_Chassis", (0.04, 0.035, 0.05, 1.0), metallic=0.88, roughness=0.25)
    mat_glow = create_pbr(f"{name}_Glow", faction_color, emission=faction_color, emission_strength=4.5)
    mat_gold = create_pbr(f"{name}_Gold", GOLD_AMBER, emission=GOLD_AMBER, emission_strength=3.5)

    # 1. Base Disk
    bpy.ops.mesh.primitive_cylinder_add(radius=base_r, depth=base_h, vertices=64, location=(0, 0, base_h / 2))
    base = bpy.context.active_object
    base.name = f"{name}_Base"
    base.data.materials.append(mat_chassis)
    add_bevel(base, width=0.002, segments=4)

    # 2. Halo Luminoso na Base
    bpy.ops.mesh.primitive_torus_add(
        major_radius=base_r * 0.92,
        minor_radius=0.0022,
        major_segments=48,
        minor_segments=16,
        location=(0, 0, base_h)
    )
    halo = bpy.context.active_object
    halo.name = f"{name}_BaseHalo"
    halo.data.materials.append(mat_glow)

    # 3. Manopla Cônica Central
    bpy.ops.mesh.primitive_cone_add(
        radius1=handle_r_bottom,
        radius2=handle_r_top,
        depth=handle_h,
        vertices=48,
        location=(0, 0, base_h + handle_h / 2)
    )
    handle = bpy.context.active_object
    handle.name = f"{name}_Handle"
    handle.data.materials.append(mat_chassis)
    add_bevel(handle, width=0.002, segments=4)

    # 4. Grip Superior Abobadado
    bpy.ops.mesh.primitive_cylinder_add(
        radius=grip_r,
        depth=grip_h,
        vertices=48,
        location=(0, 0, base_h + handle_h + grip_h / 2)
    )
    grip = bpy.context.active_object
    grip.name = f"{name}_GripCap"
    grip.data.materials.append(mat_chassis)
    add_bevel(grip, width=0.0025, segments=4)

    # Anel Superior no Grip
    bpy.ops.mesh.primitive_torus_add(
        major_radius=grip_r * 0.88,
        minor_radius=0.002,
        major_segments=36,
        minor_segments=16,
        location=(0, 0, base_h + handle_h + grip_h)
    )
    g_ring = bpy.context.active_object
    g_ring.name = f"{name}_GripRing"
    g_ring.data.materials.append(mat_glow)

    # 5. Orelhinhas de Gato Neon na Cúpula do Rebatedor
    ear_w = 0.008
    ear_h = 0.016
    ear_d = 0.003
    for side, ex in [("L", -0.014), ("R", 0.014)]:
        bpy.ops.mesh.primitive_cone_add(
            radius1=ear_w,
            radius2=0.0005,
            depth=ear_h,
            vertices=3,
            location=(ex, 0, base_h + handle_h + grip_h + ear_h / 2 - 0.002)
        )
        ear = bpy.context.active_object
        ear.name = f"{name}_Ear_{side}"
        ear.rotation_euler = (0, 0, math.radians(180 if side == "L" else 0))
        ear.data.materials.append(mat_gold if is_p1 else mat_glow)

    out_path = os.path.join(OUTPUT_DIR, f"{name}.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(f">>> Exported Mallet ({name}):", out_path)

# ============================================================================
# 3. MESA KIARA CAT COMPLETA (mesa_kiara_cat.glb)
# ============================================================================
def generate_table():
    reset_scene()
    w = 1.08
    l = 1.92
    h_surface = 0.04
    rail_w = 0.075
    rail_h = 0.065
    goal_w = 0.38

    mat_surface = create_pbr("KiaraTable_Surface", DARK_SURFACE, metallic=0.15, roughness=0.55)
    mat_rail = create_pbr("KiaraTable_Rail", DARK_RAIL, metallic=0.92, roughness=0.2)
    mat_bumper = create_pbr("KiaraTable_Bumper", DARK_BUMPER, metallic=0.8, roughness=0.35)
    mat_p1_laser = create_pbr("KiaraTable_P1Laser", PINK_MAGENTA, emission=PINK_MAGENTA, emission_strength=5.0)
    mat_p2_laser = create_pbr("KiaraTable_P2Laser", CYAN_ELECTRIC, emission=CYAN_ELECTRIC, emission_strength=5.0)
    mat_neutral = create_pbr("KiaraTable_Neutral", GOLD_AMBER, emission=GOLD_AMBER, emission_strength=4.5)

    # 1. Superfície de Jogo
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, -h_surface / 2))
    floor = bpy.context.active_object
    floor.name = "KiaraTableSurface"
    floor.scale = (w, l, h_surface)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(floor, width=0.004, segments=4)
    floor.data.materials.append(mat_surface)

    # 2. Trilhos Laterais (Longitudinais)
    for x_pos in [-w/2 - rail_w/2, w/2 + rail_w/2]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, 0, rail_h / 2))
        rail = bpy.context.active_object
        rail.scale = (rail_w, l + 2 * rail_w, rail_h)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(rail, width=0.005, segments=4)
        rail.data.materials.append(mat_rail)

        # Fita Laser Neon no Trilho Lateral
        bpy.ops.mesh.primitive_cube_add(
            size=1.0,
            location=(x_pos + (-0.015 if x_pos > 0 else 0.015), 0, rail_h * 0.65)
        )
        laser = bpy.context.active_object
        laser.scale = (0.006, l, 0.006)
        bpy.ops.object.transform_apply(scale=True)
        laser.data.materials.append(mat_p1_laser if x_pos < 0 else mat_p2_laser)

    # 3. Trilhos de Fundo (com Abertura da Meta)
    seg_w = (w - goal_w) / 2
    for y_pos, laser_mat in [(-l/2 - rail_w/2, mat_p1_laser), (l/2 + rail_w/2, mat_p2_laser)]:
        for x_dir in [-1, 1]:
            x_pos = x_dir * (goal_w/2 + seg_w/2)
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, y_pos, rail_h / 2))
            back_rail = bpy.context.active_object
            back_rail.scale = (seg_w, rail_w, rail_h)
            bpy.ops.object.transform_apply(scale=True)
            add_bevel(back_rail, width=0.005, segments=4)
            back_rail.data.materials.append(mat_rail)

            # Laser no trilho de fundo
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, y_pos + (0.015 if y_pos < 0 else -0.015), rail_h * 0.65))
            blaser = bpy.context.active_object
            blaser.scale = (seg_w * 0.9, 0.006, 0.006)
            bpy.ops.object.transform_apply(scale=True)
            blaser.data.materials.append(laser_mat)

        # Barreira de Energia da Meta (Arco Laser)
        bpy.ops.mesh.primitive_cylinder_add(
            radius=0.012,
            depth=goal_w * 0.98,
            vertices=24,
            location=(0, y_pos + (0.01 if y_pos < 0 else -0.01), rail_h * 0.75)
        )
        arch = bpy.context.active_object
        arch.rotation_euler = (0, math.radians(90), 0)
        arch.data.materials.append(laser_mat)

    # 4. Linha Central e Círculo Central com Orelhas de Gato
    # Linha central divisória
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.001))
    cline = bpy.context.active_object
    cline.scale = (w * 0.96, 0.008, 0.001)
    bpy.ops.object.transform_apply(scale=True)
    cline.data.materials.append(mat_neutral)

    # Círculo central
    bpy.ops.mesh.primitive_torus_add(
        major_radius=0.18,
        minor_radius=0.004,
        major_segments=48,
        minor_segments=16,
        location=(0, 0, 0.002)
    )
    ccirc = bpy.context.active_object
    ccirc.data.materials.append(mat_neutral)

    # Orelhas de Gato no Círculo Central
    for side, ox in [("L", -0.11), ("R", 0.11)]:
        for dir_y, oy, rz in [(1, 0.16, 0), (-1, -0.16, 180)]:
            bpy.ops.mesh.primitive_cone_add(
                radius1=0.035,
                radius2=0.002,
                depth=0.05,
                vertices=3,
                location=(ox, oy, 0.002)
            )
            cear = bpy.context.active_object
            cear.rotation_euler = (0, 0, math.radians(rz))
            cear.data.materials.append(mat_neutral)

    # 5. Piso do Estádio Futurista (ArenaStadiumFloor)
    arena_w = 4.0
    arena_l = 6.0
    arena_z = -0.045
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, arena_z))
    arena_floor = bpy.context.active_object
    arena_floor.name = "ArenaStadiumFloor"
    arena_floor.scale = (arena_w, arena_l, 0.025)
    bpy.ops.object.transform_apply(scale=True)
    mat_stadium = create_pbr("KiaraStadiumFloor", (0.015, 0.012, 0.022, 1.0), metallic=0.75, roughness=0.35)
    arena_floor.data.materials.append(mat_stadium)

    # Grade Luminosa no Piso
    mat_grid = create_pbr("KiaraGridGlow", PINK_MAGENTA, emission=PINK_MAGENTA, emission_strength=2.0)
    for gx in [-1.5, -0.8, 0.8, 1.5]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(gx, 0, arena_z + 0.013))
        gline = bpy.context.active_object
        gline.scale = (0.015, arena_l * 0.94, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_grid)

    for gy in [-2.5, -1.2, 0.0, 1.2, 2.5]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, gy, arena_z + 0.013))
        gline = bpy.context.active_object
        gline.scale = (arena_w * 0.94, 0.015, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_grid)

    out_path = os.path.join(OUTPUT_DIR, "mesa_kiara_cat.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Table:", out_path)

# ============================================================================
# 4. MEIAS-MESAS (mesa_kiara_cat_half_p1.glb / half_p2.glb)
# ============================================================================
def generate_half_table(name, faction_color, is_p1=True):
    reset_scene()
    w = 1.08
    l_half = 2.16
    h_surface = 0.04
    rail_w = 0.075
    rail_h = 0.065
    goal_w = 0.38
    side_seg_w = (w - goal_w) / 2
    led_thick = 0.007
    led_h = 0.016

    mat_surface = create_pbr(f"{name}_Surface", DARK_SURFACE, metallic=0.15, roughness=0.55)
    mat_rail = create_pbr(f"{name}_Rail", DARK_RAIL, metallic=0.92, roughness=0.22)
    mat_bumper = create_pbr(f"{name}_Bumper", DARK_BUMPER, metallic=0.80, roughness=0.35)
    mat_laser = create_pbr(f"{name}_Laser", faction_color, emission=faction_color, emission_strength=5.5)
    mat_neutral = create_pbr(f"{name}_Neutral", GOLD_AMBER, emission=GOLD_AMBER, emission_strength=4.5)

    # 1. Piso da Meia-Mesa (X em [-w/2, w/2], Y em [-l_half/2, l_half/2])
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, -h_surface / 2))
    floor = bpy.context.active_object
    floor.name = f"{name}_Surface"
    floor.scale = (w, l_half, h_surface)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(floor, width=0.004, segments=4)
    floor.data.materials.append(mat_surface)

    # 2. Semicírculo Central Neon no Topo (Y = +1.08)
    top_y = l_half / 2
    arc_outer = create_semicircle_mesh(f"{name}_CenterArcOuter", 0, top_y, 0.0015, r_out=0.18, r_in=0.165)
    arc_outer.data.materials.append(mat_neutral)

    # Orelhas de Gato no Semicírculo Central (voltadas para o interior da meia-mesa, em direção a -Y)
    for side, ox in [("L", -0.11), ("R", 0.11)]:
        bpy.ops.mesh.primitive_cone_add(
            radius1=0.035,
            radius2=0.002,
            depth=0.05,
            vertices=3,
            location=(ox, top_y - 0.16, 0.002)
        )
        cear = bpy.context.active_object
        cear.name = f"{name}_Ear_{side}"
        cear.rotation_euler = (0, 0, math.radians(180))
        cear.data.materials.append(mat_neutral)

    # 3. Faixa de Junção Laser no Topo (Eletro-borda de passagem contínua)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, top_y - 0.003, 0.0015))
    top_edge_laser = bpy.context.active_object
    top_edge_laser.name = f"{name}_TopEdgeLaser"
    top_edge_laser.scale = (w * 0.98, 0.006, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    top_edge_laser.data.materials.append(mat_neutral)

    # 4. Área da Meta Neon na Base (Y = -l_half/2 + 0.32 = -0.76)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -l_half/2 + 0.32, 0.0015))
    crease = bpy.context.active_object
    crease.name = f"{name}_Crease"
    crease.scale = (goal_w * 1.5, 0.008, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    crease.data.materials.append(mat_laser)

    # 5. Trilhos Laterais (Estendem-se do fundo até o topo)
    rail_len = l_half + rail_w
    rail_center_y = -rail_w / 2

    for sx, r_name in [(-(w + rail_w)/2, "RailL"), ((w + rail_w)/2, "RailR")]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(sx, rail_center_y, rail_h / 2))
        rail = bpy.context.active_object
        rail.name = f"{name}_{r_name}"
        rail.scale = (rail_w, rail_len, rail_h)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(rail, width=0.004, segments=4)
        rail.data.materials.append(mat_rail)

        # Friso LED no trilho lateral
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(sx, rail_center_y, rail_h * 0.72))
        led = bpy.context.active_object
        led.scale = (led_thick, rail_len * 0.94, led_h)
        bpy.ops.object.transform_apply(scale=True)
        led.data.materials.append(mat_laser)

    # 6. Trilho Inferior (Fundo) com Abertura da Meta
    bottom_y = -l_half/2 - rail_w/2
    for seg_x in [-w/2 + side_seg_w/2, w/2 - side_seg_w/2]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(seg_x, bottom_y, rail_h / 2))
        seg = bpy.context.active_object
        seg.scale = (side_seg_w, rail_w, rail_h)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(seg, width=0.003, segments=4)
        seg.data.materials.append(mat_rail)

        # LED na borda da meta
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(seg_x, bottom_y, rail_h * 0.75))
        f_led = bpy.context.active_object
        f_led.scale = (side_seg_w * 0.95, led_thick, led_h)
        bpy.ops.object.transform_apply(scale=True)
        f_led.data.materials.append(mat_laser)

    # 7. Protetores de Canto Angulares (Dois cantos inferiores)
    c_offset_x = (w + rail_w) / 2
    c_offset_y = bottom_y
    for cx in [-c_offset_x, c_offset_x]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(cx, c_offset_y, rail_h * 0.55))
        bumper = bpy.context.active_object
        bumper.scale = (rail_w * 1.35, rail_w * 1.35, rail_h * 1.15)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(bumper, width=0.005, segments=4)
        bumper.data.materials.append(mat_bumper)

        # Indicador Chevron Luminoso
        bpy.ops.mesh.primitive_cylinder_add(radius=0.015, depth=rail_h * 1.2, vertices=16, location=(cx, c_offset_y, rail_h * 0.6))
        chev = bpy.context.active_object
        chev.data.materials.append(mat_laser)

    # 8. Barreira de Energia da Meta 3D (Portal Laser)
    post_h = 0.12
    post_r = 0.018
    for post_x in [-goal_w / 2, goal_w / 2]:
        bpy.ops.mesh.primitive_cylinder_add(radius=post_r, depth=post_h, vertices=24, location=(post_x, bottom_y, post_h / 2))
        post = bpy.context.active_object
        post.data.materials.append(mat_rail)

        bpy.ops.mesh.primitive_cylinder_add(radius=post_r * 1.25, depth=0.02, vertices=24, location=(post_x, bottom_y, post_h))
        cap = bpy.context.active_object
        cap.data.materials.append(mat_laser)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, bottom_y, post_h))
    lintel = bpy.context.active_object
    lintel.scale = (goal_w + post_r * 2, 0.025, 0.018)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(lintel, width=0.003, segments=4)
    lintel.data.materials.append(mat_rail)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, bottom_y + 0.01, post_h * 0.5))
    barrier = bpy.context.active_object
    barrier.scale = (goal_w * 0.98, 0.008, post_h * 0.85)
    bpy.ops.object.transform_apply(scale=True)
    barrier.data.materials.append(mat_laser)

    out_path = os.path.join(OUTPUT_DIR, f"{name}.glb")
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(f">>> Exported Half Table ({name}):", out_path)

if __name__ == "__main__":
    print("=== Generating Kiara Cat 3D Assets ===")
    generate_puck()
    generate_mallet("rebatedor_kiara_p1", PINK_MAGENTA, is_p1=True)
    generate_mallet("rebatedor_kiara_p2", CYAN_ELECTRIC, is_p1=False)
    generate_table()
    generate_half_table("mesa_kiara_cat_half_p1", PINK_MAGENTA, is_p1=True)
    generate_half_table("mesa_kiara_cat_half_p2", CYAN_ELECTRIC, is_p1=False)
    print("=== All Kiara Cat 3D Assets Generated Successfully ===")
