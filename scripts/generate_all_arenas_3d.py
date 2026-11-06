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

def build_base_table(theme_name, surface_color, rail_color, bumper_color, p1_color, p2_color, neutral_color):
    reset_scene()
    mat_surface = create_pbr(f"{theme_name}_Surface", surface_color, metallic=0.55, roughness=0.28)
    mat_rail = create_pbr(f"{theme_name}_Rail", rail_color, metallic=0.92, roughness=0.22)
    mat_bumper = create_pbr(f"{theme_name}_Bumper", bumper_color, metallic=0.8, roughness=0.35)
    mat_p1_laser = create_pbr(f"{theme_name}_P1Laser", p1_color, emission=p1_color, emission_strength=5.0)
    mat_p2_laser = create_pbr(f"{theme_name}_P2Laser", p2_color, emission=p2_color, emission_strength=5.0)
    mat_neutral_laser = create_pbr(f"{theme_name}_NeutralLaser", neutral_color, emission=neutral_color, emission_strength=5.0)

    w = 1.08
    l = 1.92
    h_surface = 0.04
    rail_w = 0.075
    rail_h = 0.065
    goal_w = 0.38

    # 1. Base Play Surface
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, -h_surface / 2))
    floor = bpy.context.active_object
    floor.name = f"{theme_name}_Surface"
    floor.scale = (w, l, h_surface)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(floor, width=0.004, segments=4)
    floor.data.materials.append(mat_surface)

    # 2. Side Rails
    rail_len = l + rail_w * 2
    for sx, name in [(-(w + rail_w) / 2, "RailL"), ((w + rail_w) / 2, "RailR")]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(sx, 0, rail_h / 2))
        rail = bpy.context.active_object
        rail.name = f"{theme_name}_{name}"
        rail.scale = (rail_w, rail_len, rail_h)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(rail, width=0.004, segments=4)
        rail.data.materials.append(mat_rail)

    # LED frisos nos trilhos
    led_thick = 0.007
    led_h = 0.016
    for x_pos in [-(w - led_thick)/2, (w - led_thick)/2]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, -l/4, rail_h * 0.72))
        led1 = bpy.context.active_object
        led1.scale = (led_thick, l/2 * 0.98, led_h)
        bpy.ops.object.transform_apply(scale=True)
        led1.data.materials.append(mat_p1_laser)

        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, l/4, rail_h * 0.72))
        led2 = bpy.context.active_object
        led2.scale = (led_thick, l/2 * 0.98, led_h)
        bpy.ops.object.transform_apply(scale=True)
        led2.data.materials.append(mat_p2_laser)

    # 3. Top and Bottom Rails with Goal Openings
    side_seg_w = (w - goal_w) / 2
    top_y = (l + rail_w) / 2
    bottom_y = -(l + rail_w) / 2

    for (pos_y, mat_color) in [(bottom_y, mat_p1_laser), (top_y, mat_p2_laser)]:
        for seg_x in [-w/2 + side_seg_w/2, w/2 - side_seg_w/2]:
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(seg_x, pos_y, rail_h / 2))
            seg = bpy.context.active_object
            seg.scale = (side_seg_w, rail_w, rail_h)
            bpy.ops.object.transform_apply(scale=True)
            add_bevel(seg, width=0.003, segments=4)
            seg.data.materials.append(mat_rail)

            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(seg_x, pos_y, rail_h * 0.75))
            f_led = bpy.context.active_object
            f_led.scale = (side_seg_w * 0.95, led_thick, led_h)
            bpy.ops.object.transform_apply(scale=True)
            f_led.data.materials.append(mat_color)

    # 4. Corner Bumpers
    c_offset_x = (w + rail_w) / 2
    c_offset_y = (l + rail_w) / 2
    for cx, cy, c_mat in [
        (-c_offset_x, -c_offset_y, mat_p1_laser),
        (c_offset_x, -c_offset_y, mat_p1_laser),
        (-c_offset_x, c_offset_y, mat_p2_laser),
        (c_offset_x, c_offset_y, mat_p2_laser)
    ]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(cx, cy, rail_h * 0.55))
        bumper = bpy.context.active_object
        bumper.scale = (rail_w * 1.35, rail_w * 1.35, rail_h * 1.15)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(bumper, width=0.005, segments=4)
        bumper.data.materials.append(mat_bumper)

        bpy.ops.mesh.primitive_cylinder_add(radius=0.015, depth=rail_h * 1.2, vertices=16, location=(cx, cy, rail_h * 0.6))
        chev = bpy.context.active_object
        chev.data.materials.append(c_mat)

    # 5. Goal Posts & Barrier Beam
    for (goal_y, goal_mat, sign) in [(bottom_y, mat_p1_laser, -1.0), (top_y, mat_p2_laser, 1.0)]:
        post_h = 0.12
        post_r = 0.018
        for post_x in [-goal_w / 2, goal_w / 2]:
            bpy.ops.mesh.primitive_cylinder_add(radius=post_r, depth=post_h, vertices=24, location=(post_x, goal_y, post_h / 2))
            post = bpy.context.active_object
            post.data.materials.append(mat_rail)

            bpy.ops.mesh.primitive_cylinder_add(radius=post_r * 1.25, depth=0.02, vertices=24, location=(post_x, goal_y, post_h))
            cap = bpy.context.active_object
            cap.data.materials.append(goal_mat)

        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, goal_y, post_h))
        lintel = bpy.context.active_object
        lintel.scale = (goal_w + post_r * 2, 0.025, 0.018)
        bpy.ops.object.transform_apply(scale=True)
        add_bevel(lintel, width=0.003, segments=4)
        lintel.data.materials.append(mat_rail)

        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, goal_y - sign * 0.01, post_h * 0.5))
        barrier = bpy.context.active_object
        barrier.scale = (goal_w * 0.98, 0.008, post_h * 0.85)
        bpy.ops.object.transform_apply(scale=True)
        barrier.data.materials.append(goal_mat)

    # 6. Stadium Floor
    arena_w, arena_l = 10.0, 12.0
    arena_z = -h_surface - 0.06
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, arena_z))
    arena_floor = bpy.context.active_object
    arena_floor.scale = (arena_w, arena_l, 0.025)
    bpy.ops.object.transform_apply(scale=True)
    mat_stadium = create_pbr(f"{theme_name}_StadiumFloor", (0.012, 0.016, 0.024, 1.0), metallic=0.75, roughness=0.35)
    arena_floor.data.materials.append(mat_stadium)

    for px, py, mat_beacon in [
        (-arena_w/2 + 0.6, -arena_l/2 + 0.6, mat_p1_laser),
        (arena_w/2 - 0.6, -arena_l/2 + 0.6, mat_p1_laser),
        (-arena_w/2 + 0.6, arena_l/2 - 0.6, mat_p2_laser),
        (arena_w/2 - 0.6, arena_l/2 - 0.6, mat_p2_laser)
    ]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.08, depth=1.1, vertices=24, location=(px, py, arena_z + 0.55))
        pillar = bpy.context.active_object
        pillar.data.materials.append(mat_rail)

        bpy.ops.mesh.primitive_cylinder_add(radius=0.10, depth=0.14, vertices=24, location=(px, py, arena_z + 1.1))
        beacon = bpy.context.active_object
        beacon.data.materials.append(mat_beacon)

    return mat_surface, mat_rail, mat_bumper, mat_p1_laser, mat_p2_laser, mat_neutral_laser

def build_mallet(filename, neon_color, body_color=(0.05, 0.055, 0.065, 1.0)):
    reset_scene()
    base_radius = 0.08
    base_depth = 0.02
    knob_radius = 0.034
    knob_height = 0.052

    mat_body = create_pbr("MalletBody", body_color, metallic=0.88, roughness=0.22)
    mat_rubber = create_pbr("MalletGrip", (0.02, 0.022, 0.025, 1.0), metallic=0.05, roughness=0.85)
    mat_neon = create_pbr("MalletNeon", neon_color, emission=neon_color, emission_strength=5.0)

    # Base
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius, depth=base_depth, vertices=64, location=(0, 0, base_depth / 2))
    base = bpy.context.active_object
    add_bevel(base, width=0.0035, segments=4)
    bpy.ops.object.shade_smooth()
    base.data.materials.append(mat_body)

    # Ring
    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius * 1.012, depth=base_depth * 0.25, vertices=64, location=(0, 0, base_depth * 0.25))
    lower_ring = bpy.context.active_object
    lower_ring.data.materials.append(mat_neon)

    bpy.ops.mesh.primitive_cylinder_add(radius=base_radius * 0.88, depth=base_depth * 1.06, vertices=64, location=(0, 0, base_depth / 2))
    step_ring = bpy.context.active_object
    step_ring.data.materials.append(mat_neon)

    # Knob
    bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius, depth=knob_height, vertices=48, location=(0, 0, base_depth + knob_height / 2))
    knob = bpy.context.active_object
    add_bevel(knob, width=0.003, segments=4)
    bpy.ops.object.shade_smooth()
    knob.data.materials.append(mat_body)

    # Grips
    for z_off in [0.012, 0.024, 0.036]:
        bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius * 1.05, depth=0.004, vertices=48, location=(0, 0, base_depth + z_off))
        rib = bpy.context.active_object
        rib.data.materials.append(mat_rubber)

    # Dome
    dome_z = base_depth + knob_height
    bpy.ops.mesh.primitive_uv_sphere_add(radius=knob_radius * 0.90, segments=32, ring_count=16, location=(0, 0, dome_z))
    dome = bpy.context.active_object
    bpy.ops.object.shade_smooth()
    dome.data.materials.append(mat_neon)

    bpy.ops.mesh.primitive_cylinder_add(radius=knob_radius * 0.96, depth=0.007, vertices=32, location=(0, 0, dome_z))
    crown = bpy.context.active_object
    crown.data.materials.append(mat_body)

    out_path = os.path.join(OUTPUT_DIR, filename)
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB')
    print(">>> Exported Mallet:", out_path)

# ============================================================================
# 1. GRAVITY VOLLEY (VÔLEI GRAVITACIONAL)
# ============================================================================
def generate_gravity_volley():
    # Puck
    reset_scene()
    radius, depth = 0.05, 0.015
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    add_bevel(puck, width=0.0025, segments=4)
    bpy.ops.object.shade_smooth()
    mat_dark_purple = create_pbr("VolleyPuckDark", (0.04, 0.02, 0.08, 1.0), metallic=0.9, roughness=0.2)
    puck.data.materials.append(mat_dark_purple)

    mat_violet = create_pbr("VolleyViolet", (0.54, 0.17, 0.89, 1.0), emission=(0.54, 0.17, 0.89, 1.0), emission_strength=5.0)
    mat_magenta = create_pbr("VolleyMagenta", (1.0, 0.0, 0.5, 1.0), emission=(1.0, 0.0, 0.5, 1.0), emission_strength=5.0)
    mat_core = create_pbr("VolleyCore", (1.0, 0.85, 0.2, 1.0), emission=(1.0, 0.85, 0.2, 1.0), emission_strength=5.0)

    # Perimeter friso
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 1.015, depth=depth * 0.25, vertices=64, location=(0, 0, 0))
    bpy.context.active_object.data.materials.append(mat_violet)

    # 3-Panel Volleyball Curved Seam Emulation
    for i in range(3):
        angle = i * (2 * math.pi / 3)
        bx = radius * 0.45 * math.cos(angle)
        by = radius * 0.45 * math.sin(angle)
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(bx, by, depth * 0.51))
        strip = bpy.context.active_object
        strip.scale = (radius * 0.55, 0.0025, 0.0015)
        strip.rotation_euler = (0, 0, angle + 0.5)
        bpy.ops.object.transform_apply(scale=True, rotation=True)
        strip.data.materials.append(mat_magenta if i % 2 == 0 else mat_violet)

    # Center gravity vortex
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.18, depth=depth * 1.12, vertices=24, location=(0, 0, 0))
    bpy.context.active_object.data.materials.append(mat_core)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "disco_gravity_volley.glb"), export_format='GLB')
    print(">>> Exported disco_gravity_volley.glb")

    # Mallets
    build_mallet("rebatedor_gravity_volley_p1.glb", (0.54, 0.17, 0.89, 1.0))
    build_mallet("rebatedor_gravity_volley_p2.glb", (1.0, 0.0, 0.5, 1.0))

    # Table
    mat_surf, mat_rail, mat_bumper, mat_violet, mat_magenta, mat_core = build_base_table(
        "Volley",
        (0.025, 0.015, 0.05, 1.0),
        (0.04, 0.045, 0.065, 1.0),
        (0.025, 0.02, 0.04, 1.0),
        (0.54, 0.17, 0.89, 1.0),
        (1.0, 0.0, 0.5, 1.0),
        (1.0, 0.85, 0.2, 1.0)
    )

    # Laser Net
    w, l = 1.08, 1.92
    for post_x in [-w/2 + 0.02, w/2 - 0.02]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.012, depth=0.08, vertices=24, location=(post_x, 0, 0.04))
        p = bpy.context.active_object
        p.data.materials.append(mat_rail)
        bpy.ops.mesh.primitive_cylinder_add(radius=0.015, depth=0.015, vertices=24, location=(post_x, 0, 0.08))
        bpy.context.active_object.data.materials.append(mat_core)

    for nz in [0.02, 0.04, 0.06]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, nz))
        beam = bpy.context.active_object
        beam.scale = (w * 0.94, 0.005, 0.004)
        bpy.ops.object.transform_apply(scale=True)
        beam.data.materials.append(mat_core)

    for ay in [-l * 0.22, l * 0.22]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, ay, 0.0015))
        aline = bpy.context.active_object
        aline.scale = (w * 0.95, 0.009, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        aline.data.materials.append(mat_violet if ay < 0 else mat_magenta)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "mesa_gravity_volley.glb"), export_format='GLB')
    print(">>> Exported mesa_gravity_volley.glb")

# ============================================================================
# 2. GRIDIRON FOOTBALL (FUTEBOL AMERICANO SCI-FI)
# ============================================================================
def generate_gridiron_football():
    # Puck
    reset_scene()
    radius, depth = 0.05, 0.015
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    add_bevel(puck, width=0.0025, segments=4)
    bpy.ops.object.shade_smooth()
    mat_dark_green = create_pbr("GridironPuckDark", (0.015, 0.045, 0.025, 1.0), metallic=0.9, roughness=0.2)
    puck.data.materials.append(mat_dark_green)

    mat_gold = create_pbr("GridironGold", (1.0, 0.70, 0.0, 1.0), emission=(1.0, 0.70, 0.0, 1.0), emission_strength=5.0)
    mat_cyan = create_pbr("GridironCyan", (0.0, 0.90, 1.0, 1.0), emission=(0.0, 0.90, 1.0, 1.0), emission_strength=5.0)
    mat_white = create_pbr("GridironWhite", (1.0, 1.0, 1.0, 1.0), emission=(1.0, 1.0, 1.0, 1.0), emission_strength=4.5)

    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 1.015, depth=depth * 0.25, vertices=64, location=(0, 0, 0))
    bpy.context.active_object.data.materials.append(mat_gold)

    # Football Laces
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, depth * 0.51))
    main_lace = bpy.context.active_object
    main_lace.scale = (0.003, radius * 0.9, 0.0015)
    bpy.ops.object.transform_apply(scale=True)
    main_lace.data.materials.append(mat_white)

    for ly in [-0.02, -0.01, 0.0, 0.01, 0.02]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, ly, depth * 0.515))
        cross = bpy.context.active_object
        cross.scale = (0.016, 0.0022, 0.0015)
        bpy.ops.object.transform_apply(scale=True)
        cross.data.materials.append(mat_white)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "disco_gridiron_football.glb"), export_format='GLB')
    print(">>> Exported disco_gridiron_football.glb")

    # Mallets
    build_mallet("rebatedor_gridiron_football_p1.glb", (0.0, 0.90, 1.0, 1.0), body_color=(0.03, 0.05, 0.07, 1.0))
    build_mallet("rebatedor_gridiron_football_p2.glb", (1.0, 0.70, 0.0, 1.0), body_color=(0.07, 0.05, 0.03, 1.0))

    # Table
    mat_surf, mat_rail, mat_bumper, mat_cyan, mat_gold, mat_white = build_base_table(
        "Gridiron",
        (0.012, 0.035, 0.02, 1.0),
        (0.03, 0.05, 0.04, 1.0),
        (0.02, 0.03, 0.02, 1.0),
        (0.0, 0.90, 1.0, 1.0),
        (1.0, 0.70, 0.0, 1.0),
        (1.0, 1.0, 1.0, 1.0)
    )

    w, l = 1.08, 1.92
    for i in range(1, 10):
        y = -l/2 + (l / 10.0) * i
        is_50 = (i == 5)
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, y, 0.0015))
        yline = bpy.context.active_object
        yline.scale = (w * 0.94, 0.012 if is_50 else 0.006, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        yline.data.materials.append(mat_gold if is_50 else mat_cyan)

    bpy.ops.mesh.primitive_cylinder_add(radius=0.18, depth=0.002, vertices=48, location=(0, 0, 0.0016))
    bpy.context.active_object.data.materials.append(mat_gold)
    bpy.ops.mesh.primitive_cylinder_add(radius=0.166, depth=0.0025, vertices=48, location=(0, 0, 0.0016))
    bpy.context.active_object.data.materials.append(mat_surf)

    for gy, gmat in [(-l/2 - 0.04, mat_cyan), (l/2 + 0.04, mat_gold)]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.012, depth=0.10, vertices=16, location=(0, gy, 0.05))
        bpy.context.active_object.data.materials.append(gmat)
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, gy, 0.10))
        cbar = bpy.context.active_object
        cbar.scale = (0.28, 0.012, 0.012)
        bpy.ops.object.transform_apply(scale=True)
        cbar.data.materials.append(gmat)
        for ux in [-0.14, 0.14]:
            bpy.ops.mesh.primitive_cylinder_add(radius=0.008, depth=0.10, vertices=16, location=(ux, gy, 0.15))
            bpy.context.active_object.data.materials.append(gmat)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "mesa_gridiron_football.glb"), export_format='GLB')
    print(">>> Exported mesa_gridiron_football.glb")

# ============================================================================
# 3. CYBER BASEBALL (BEISEBOL CIBERNÉTICO)
# ============================================================================
def generate_cyber_baseball():
    # Puck
    reset_scene()
    radius, depth = 0.05, 0.015
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    add_bevel(puck, width=0.0025, segments=4)
    bpy.ops.object.shade_smooth()
    mat_dark_clay = create_pbr("BaseballPuckDark", (0.05, 0.04, 0.035, 1.0), metallic=0.9, roughness=0.2)
    puck.data.materials.append(mat_dark_clay)

    mat_red = create_pbr("BaseballRed", (1.0, 0.09, 0.27, 1.0), emission=(1.0, 0.09, 0.27, 1.0), emission_strength=5.0)
    mat_cyan = create_pbr("BaseballCyan", (0.0, 0.90, 1.0, 1.0), emission=(0.0, 0.90, 1.0, 1.0), emission_strength=5.0)

    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 1.015, depth=depth * 0.25, vertices=64, location=(0, 0, 0))
    bpy.context.active_object.data.materials.append(mat_red)

    for sign in [-1.0, 1.0]:
        for si in range(7):
            sy = -0.03 + si * 0.01
            sx = sign * (0.025 + 0.008 * math.cos(sy * 40))
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(sx, sy, depth * 0.51))
            stitch = bpy.context.active_object
            stitch.scale = (0.008, 0.002, 0.0015)
            stitch.rotation_euler = (0, 0, sign * 0.5)
            bpy.ops.object.transform_apply(scale=True, rotation=True)
            stitch.data.materials.append(mat_red)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "disco_cyber_baseball.glb"), export_format='GLB')
    print(">>> Exported disco_cyber_baseball.glb")

    # Mallets
    build_mallet("rebatedor_cyber_baseball_p1.glb", (1.0, 0.09, 0.27, 1.0))
    build_mallet("rebatedor_cyber_baseball_p2.glb", (0.0, 0.90, 1.0, 1.0))

    # Table
    mat_surf, mat_rail, mat_bumper, mat_red, mat_cyan, _ = build_base_table(
        "Baseball",
        (0.035, 0.025, 0.02, 1.0),
        (0.06, 0.05, 0.04, 1.0),
        (0.04, 0.03, 0.02, 1.0),
        (1.0, 0.09, 0.27, 1.0),
        (0.0, 0.90, 1.0, 1.0),
        (0.0, 0.90, 1.0, 1.0)
    )

    w, l = 1.08, 1.92
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.0015))
    diamond = bpy.context.active_object
    diamond.scale = (0.32, 0.32, 0.002)
    diamond.rotation_euler = (0, 0, math.pi / 4)
    bpy.ops.object.transform_apply(scale=True, rotation=True)
    diamond.data.materials.append(mat_cyan)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.0016))
    inner_diamond = bpy.context.active_object
    inner_diamond.scale = (0.30, 0.30, 0.0025)
    inner_diamond.rotation_euler = (0, 0, math.pi / 4)
    bpy.ops.object.transform_apply(scale=True, rotation=True)
    inner_diamond.data.materials.append(mat_surf)

    diag = 0.32 * math.sqrt(2) / 2
    for bx, by in [(0, -diag), (diag, 0), (0, diag), (-diag, 0)]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(bx, by, 0.002))
        base = bpy.context.active_object
        base.scale = (0.035, 0.035, 0.003)
        base.rotation_euler = (0, 0, math.pi / 4)
        bpy.ops.object.transform_apply(scale=True, rotation=True)
        base.data.materials.append(mat_red)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "mesa_cyber_baseball.glb"), export_format='GLB')
    print(">>> Exported mesa_cyber_baseball.glb")

# ============================================================================
# 4. WASTELAND APOCALYPSE (WASTELAND PÓS-APOCALÍPTICO)
# ============================================================================
def generate_wasteland_apocalypse():
    # Puck (Sawblade)
    reset_scene()
    radius, depth = 0.05, 0.015
    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.9, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    add_bevel(puck, width=0.0025, segments=4)
    bpy.ops.object.shade_smooth()
    mat_rust = create_pbr("WastelandRust", (0.07, 0.035, 0.02, 1.0), metallic=0.92, roughness=0.3)
    puck.data.materials.append(mat_rust)

    mat_flame = create_pbr("WastelandFlame", (1.0, 0.24, 0.0, 1.0), emission=(1.0, 0.24, 0.0, 1.0), emission_strength=5.0)
    mat_hazard = create_pbr("WastelandHazard", (1.0, 0.84, 0.0, 1.0), emission=(1.0, 0.84, 0.0, 1.0), emission_strength=5.0)

    for i in range(8):
        angle = i * (math.pi / 4)
        tx = radius * 0.95 * math.cos(angle)
        ty = radius * 0.95 * math.sin(angle)
        bpy.ops.mesh.primitive_cone_add(radius1=0.014, depth=0.022, vertices=3, location=(tx, ty, 0))
        tooth = bpy.context.active_object
        tooth.rotation_euler = (0, math.pi / 2, angle + 0.2)
        bpy.ops.object.transform_apply(rotation=True)
        tooth.data.materials.append(mat_flame)

    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.32, depth=depth * 1.15, vertices=24, location=(0, 0, 0))
    bpy.context.active_object.data.materials.append(mat_hazard)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "disco_wasteland_apocalypse.glb"), export_format='GLB')
    print(">>> Exported disco_wasteland_apocalypse.glb")

    # Mallets
    build_mallet("rebatedor_wasteland_apocalypse_p1.glb", (1.0, 0.24, 0.0, 1.0), body_color=(0.06, 0.03, 0.02, 1.0))
    build_mallet("rebatedor_wasteland_apocalypse_p2.glb", (1.0, 0.84, 0.0, 1.0), body_color=(0.06, 0.05, 0.02, 1.0))

    # Table
    mat_surf, mat_rail, mat_bumper, mat_flame, mat_hazard, _ = build_base_table(
        "Wasteland",
        (0.04, 0.025, 0.015, 1.0),
        (0.08, 0.04, 0.02, 1.0),
        (0.04, 0.02, 0.01, 1.0),
        (1.0, 0.24, 0.0, 1.0),
        (1.0, 0.84, 0.0, 1.0),
        (1.0, 0.24, 0.0, 1.0)
    )

    w, l = 1.08, 1.92
    for gx in [-w * 0.3, 0, w * 0.3]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(gx, 0, 0.0015))
        gline = bpy.context.active_object
        gline.scale = (0.008, l * 0.95, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_flame)

    for gy in [-l * 0.3, 0, l * 0.3]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, gy, 0.0015))
        gline = bpy.context.active_object
        gline.scale = (w * 0.95, 0.008, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_flame)

    bpy.ops.mesh.primitive_cylinder_add(radius=0.18, depth=0.002, vertices=48, location=(0, 0, 0.0016))
    bpy.context.active_object.data.materials.append(mat_hazard)
    bpy.ops.mesh.primitive_cylinder_add(radius=0.166, depth=0.0025, vertices=48, location=(0, 0, 0.0016))
    bpy.context.active_object.data.materials.append(mat_surf)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "mesa_wasteland_apocalypse.glb"), export_format='GLB')
    print(">>> Exported mesa_wasteland_apocalypse.glb")

# ============================================================================
# 5. RETRO SYNTHWAVE (RETRO SYNTHWAVE 80S / OUTRUN)
# ============================================================================
def generate_retro_synthwave():
    # Puck (Outrun Sun Disc)
    reset_scene()
    radius, depth = 0.05, 0.015
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    add_bevel(puck, width=0.0025, segments=4)
    bpy.ops.object.shade_smooth()
    mat_indigo = create_pbr("SynthPuckDark", (0.04, 0.01, 0.08, 1.0), metallic=0.9, roughness=0.2)
    puck.data.materials.append(mat_indigo)

    mat_pink = create_pbr("SynthPink", (1.0, 0.0, 0.5, 1.0), emission=(1.0, 0.0, 0.5, 1.0), emission_strength=5.0)
    mat_cyan = create_pbr("SynthCyan", (0.0, 0.94, 1.0, 1.0), emission=(0.0, 0.94, 1.0, 1.0), emission_strength=5.0)
    mat_yellow = create_pbr("SynthYellow", (1.0, 0.84, 0.0, 1.0), emission=(1.0, 0.84, 0.0, 1.0), emission_strength=5.0)

    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 1.015, depth=depth * 0.25, vertices=64, location=(0, 0, 0))
    bpy.context.active_object.data.materials.append(mat_pink)

    for si, sy in enumerate([-0.025, -0.012, 0.0, 0.012, 0.025]):
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, sy, depth * 0.51))
        stripe = bpy.context.active_object
        stripe.scale = (radius * 1.4 * math.sqrt(max(0.1, 1 - (sy / radius)**2)), 0.003, 0.0015)
        bpy.ops.object.transform_apply(scale=True)
        stripe.data.materials.append(mat_yellow if si > 2 else mat_pink)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "disco_retro_synthwave.glb"), export_format='GLB')
    print(">>> Exported disco_retro_synthwave.glb")

    # Mallets
    build_mallet("rebatedor_retro_synthwave_p1.glb", (1.0, 0.0, 0.5, 1.0))
    build_mallet("rebatedor_retro_synthwave_p2.glb", (0.0, 0.94, 1.0, 1.0))

    # Table
    mat_surf, mat_rail, mat_bumper, mat_pink, mat_cyan, _ = build_base_table(
        "Synthwave",
        (0.03, 0.01, 0.06, 1.0),
        (0.05, 0.03, 0.08, 1.0),
        (0.03, 0.015, 0.05, 1.0),
        (1.0, 0.0, 0.5, 1.0),
        (0.0, 0.94, 1.0, 1.0),
        (1.0, 0.0, 0.5, 1.0)
    )

    w, l = 1.08, 1.92
    for gx in [-0.4, -0.25, -0.12, 0, 0.12, 0.25, 0.4]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(gx, 0, 0.0015))
        gline = bpy.context.active_object
        gline.scale = (0.008, l * 0.95, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_pink)

    for gy in [-0.7, -0.5, -0.3, -0.15, 0, 0.15, 0.3, 0.5, 0.7]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, gy, 0.0015))
        gline = bpy.context.active_object
        gline.scale = (w * 0.95, 0.008, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        gline.data.materials.append(mat_cyan)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "mesa_retro_synthwave.glb"), export_format='GLB')
    print(">>> Exported mesa_retro_synthwave.glb")

# ============================================================================
# 6. TOKAMAK REACTOR (REATOR NUCLEAR DE FUSÃO TOKAMAK)
# ============================================================================
def generate_tokamak_reactor():
    # Puck (Atomic Core)
    reset_scene()
    radius, depth = 0.05, 0.015
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    add_bevel(puck, width=0.0025, segments=4)
    bpy.ops.object.shade_smooth()
    mat_chamber = create_pbr("TokamakPuckDark", (0.01, 0.035, 0.05, 1.0), metallic=0.9, roughness=0.2)
    puck.data.materials.append(mat_chamber)

    mat_rad = create_pbr("TokamakRad", (0.46, 1.0, 0.01, 1.0), emission=(0.46, 1.0, 0.01, 1.0), emission_strength=5.0)
    mat_plasma = create_pbr("TokamakPlasma", (0.0, 0.90, 1.0, 1.0), emission=(0.0, 0.90, 1.0, 1.0), emission_strength=5.0)

    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 1.015, depth=depth * 0.25, vertices=64, location=(0, 0, 0))
    bpy.context.active_object.data.materials.append(mat_rad)

    for i in range(3):
        angle = i * (math.pi / 3)
        bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.65, depth=depth * 1.05, vertices=48, location=(0, 0, 0))
        ring = bpy.context.active_object
        ring.scale = (1.0, 0.45, 1.0)
        ring.rotation_euler = (0, 0, angle)
        bpy.ops.object.transform_apply(scale=True, rotation=True)
        ring.data.materials.append(mat_plasma if i % 2 == 0 else mat_rad)

    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.16, depth=depth * 1.15, vertices=24, location=(0, 0, 0))
    bpy.context.active_object.data.materials.append(mat_rad)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "disco_tokamak_reactor.glb"), export_format='GLB')
    print(">>> Exported disco_tokamak_reactor.glb")

    # Mallets
    build_mallet("rebatedor_tokamak_reactor_p1.glb", (0.46, 1.0, 0.01, 1.0))
    build_mallet("rebatedor_tokamak_reactor_p2.glb", (0.0, 0.90, 1.0, 1.0))

    # Table
    mat_surf, mat_rail, mat_bumper, mat_rad, mat_plasma, _ = build_base_table(
        "Tokamak",
        (0.015, 0.035, 0.05, 1.0),
        (0.02, 0.045, 0.065, 1.0),
        (0.01, 0.025, 0.04, 1.0),
        (0.46, 1.0, 0.01, 1.0),
        (0.0, 0.90, 1.0, 1.0),
        (0.46, 1.0, 0.01, 1.0)
    )

    for r, rmat in [(0.15, mat_rad), (0.28, mat_plasma), (0.42, mat_rad)]:
        bpy.ops.mesh.primitive_cylinder_add(radius=r, depth=0.002, vertices=48, location=(0, 0, 0.0015))
        bpy.context.active_object.data.materials.append(rmat)
        bpy.ops.mesh.primitive_cylinder_add(radius=r - 0.012, depth=0.0025, vertices=48, location=(0, 0, 0.0015))
        bpy.context.active_object.data.materials.append(mat_surf)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "mesa_tokamak_reactor.glb"), export_format='GLB')
    print(">>> Exported mesa_tokamak_reactor.glb")

# ============================================================================
# 7. CYBER PONG (PONG RETRÔ CRT)
# ============================================================================
def generate_cyber_pong():
    # Puck (Square CRT Block)
    reset_scene()
    side, depth = 0.075, 0.015
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0))
    puck = bpy.context.active_object
    puck.scale = (side, side, depth)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(puck, width=0.003, segments=4)
    bpy.ops.object.shade_smooth()

    mat_crt = create_pbr("PongPuckDark", (0.01, 0.04, 0.01, 1.0), metallic=0.88, roughness=0.22)
    puck.data.materials.append(mat_crt)

    mat_green = create_pbr("PongGreen", (0.22, 1.0, 0.08, 1.0), emission=(0.22, 1.0, 0.08, 1.0), emission_strength=5.0)
    mat_amber = create_pbr("PongAmber", (1.0, 0.70, 0.0, 1.0), emission=(1.0, 0.70, 0.0, 1.0), emission_strength=5.0)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0))
    core = bpy.context.active_object
    core.scale = (side * 0.7, side * 0.7, depth * 1.08)
    bpy.ops.object.transform_apply(scale=True)
    core.data.materials.append(mat_green)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "disco_cyber_pong.glb"), export_format='GLB')
    print(">>> Exported disco_cyber_pong.glb")

    # Mallets
    build_mallet("rebatedor_cyber_pong_p1.glb", (0.22, 1.0, 0.08, 1.0))
    build_mallet("rebatedor_cyber_pong_p2.glb", (1.0, 0.70, 0.0, 1.0))

    # Table
    mat_surf, mat_rail, mat_bumper, mat_green, mat_amber, _ = build_base_table(
        "Pong",
        (0.01, 0.025, 0.01, 1.0),
        (0.02, 0.05, 0.02, 1.0),
        (0.01, 0.03, 0.01, 1.0),
        (0.22, 1.0, 0.08, 1.0),
        (1.0, 0.70, 0.0, 1.0),
        (0.22, 1.0, 0.08, 1.0)
    )

    w, l = 1.08, 1.92
    dash_count = 14
    dash_w = w * 0.90 / dash_count
    for i in range(0, dash_count, 2):
        dx = -w * 0.45 + (i + 0.5) * dash_w
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(dx, 0, 0.0015))
        d = bpy.context.active_object
        d.scale = (dash_w * 0.7, 0.014, 0.002)
        bpy.ops.object.transform_apply(scale=True)
        d.data.materials.append(mat_green)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "mesa_cyber_pong.glb"), export_format='GLB')
    print(">>> Exported mesa_cyber_pong.glb")

# ============================================================================
# 8. CYBER COLOSSEUM (COLISEU CIBERNÉTICO)
# ============================================================================
def generate_cyber_colosseum():
    # Puck (Gladiator Shield)
    reset_scene()
    radius, depth = 0.05, 0.015
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=64, location=(0, 0, 0))
    puck = bpy.context.active_object
    add_bevel(puck, width=0.0025, segments=4)
    bpy.ops.object.shade_smooth()
    mat_stone = create_pbr("ColosseumPuckDark", (0.06, 0.045, 0.03, 1.0), metallic=0.9, roughness=0.2)
    puck.data.materials.append(mat_stone)

    mat_gold = create_pbr("ColosseumGold", (1.0, 0.84, 0.0, 1.0), emission=(1.0, 0.84, 0.0, 1.0), emission_strength=5.0)
    mat_crimson = create_pbr("ColosseumCrimson", (0.83, 0.0, 0.0, 1.0), emission=(0.83, 0.0, 0.0, 1.0), emission_strength=5.0)

    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 1.015, depth=depth * 0.25, vertices=64, location=(0, 0, 0))
    bpy.context.active_object.data.materials.append(mat_gold)

    for i in range(4):
        angle = i * (math.pi / 2)
        cx = radius * 0.45 * math.cos(angle)
        cy = radius * 0.45 * math.sin(angle)
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(cx, cy, depth * 0.51))
        strip = bpy.context.active_object
        strip.scale = (radius * 0.35, 0.003, 0.0015)
        strip.rotation_euler = (0, 0, angle)
        bpy.ops.object.transform_apply(scale=True, rotation=True)
        strip.data.materials.append(mat_crimson)

    bpy.ops.mesh.primitive_cylinder_add(radius=radius * 0.22, depth=depth * 1.15, vertices=24, location=(0, 0, 0))
    bpy.context.active_object.data.materials.append(mat_gold)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "disco_cyber_colosseum.glb"), export_format='GLB')
    print(">>> Exported disco_cyber_colosseum.glb")

    # Mallets
    build_mallet("rebatedor_cyber_colosseum_p1.glb", (1.0, 0.84, 0.0, 1.0), body_color=(0.08, 0.06, 0.03, 1.0))
    build_mallet("rebatedor_cyber_colosseum_p2.glb", (0.83, 0.0, 0.0, 1.0), body_color=(0.08, 0.03, 0.03, 1.0))

    # Table
    mat_surf, mat_rail, mat_bumper, mat_gold, mat_crimson, _ = build_base_table(
        "Colosseum",
        (0.035, 0.025, 0.015, 1.0),
        (0.07, 0.05, 0.03, 1.0),
        (0.04, 0.03, 0.015, 1.0),
        (1.0, 0.84, 0.0, 1.0),
        (0.83, 0.0, 0.0, 1.0),
        (1.0, 0.84, 0.0, 1.0)
    )

    bpy.ops.mesh.primitive_cylinder_add(radius=0.22, depth=0.002, vertices=48, location=(0, 0, 0.0015))
    bpy.context.active_object.data.materials.append(mat_gold)
    bpy.ops.mesh.primitive_cylinder_add(radius=0.205, depth=0.0025, vertices=48, location=(0, 0, 0.0015))
    bpy.context.active_object.data.materials.append(mat_surf)

    bpy.ops.export_scene.gltf(filepath=os.path.join(OUTPUT_DIR, "mesa_cyber_colosseum.glb"), export_format='GLB')
    print(">>> Exported mesa_cyber_colosseum.glb")

# ============================================================================
# MAIN DISPATCHER
# ============================================================================
if __name__ == "__main__":
    print("=== Generating Remaining 8 Themed 3D Asset Kits ===")
    generate_gravity_volley()
    generate_gridiron_football()
    generate_cyber_baseball()
    generate_wasteland_apocalypse()
    generate_retro_synthwave()
    generate_tokamak_reactor()
    generate_cyber_pong()
    generate_cyber_colosseum()
    print("=== ALL REMAINING 8 THEMES GENERATED SUCCESSFULLY ===")
