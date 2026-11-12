import bpy
import mathutils
import math
import os

def create_pbr_material(name, base_color, metallic=0.0, roughness=0.5, emission=None, emission_strength=1.0):
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
            elif "Emission" in bsdf.inputs:
                bsdf.inputs["Emission"].default_value = emission
            if "Emission Strength" in bsdf.inputs:
                bsdf.inputs["Emission Strength"].default_value = emission_strength
    return mat

def build_quadruped_kiara():
    # 1. Limpa objetos da cena atual
    for obj in list(bpy.data.objects):
        bpy.data.objects.remove(obj, do_unlink=True)

    # 2. Materiais Canônicos da Kiara (Gata Branca, Olhos Dourados, Coleira Rosa e Cauda Escura)
    mat_white = create_pbr_material("Mat_Kiara_White", (0.96, 0.96, 0.98, 1.0), metallic=0.0, roughness=0.55)
    mat_tail_dark = create_pbr_material("Mat_Kiara_TailDark", (0.10, 0.10, 0.12, 1.0), metallic=0.0, roughness=0.65)
    mat_collar = create_pbr_material("Mat_Kiara_Collar", (1.0, 0.08, 0.58, 1.0), emission=(1.0, 0.08, 0.58, 1.0), emission_strength=3.5)
    mat_gold = create_pbr_material("Mat_Kiara_Gold", (1.0, 0.82, 0.15, 1.0), metallic=0.92, roughness=0.22)
    mat_eyes = create_pbr_material("Mat_Kiara_Eyes", (1.0, 0.85, 0.08, 1.0), emission=(1.0, 0.85, 0.08, 1.0), emission_strength=2.8)
    mat_pupil = create_pbr_material("Mat_Kiara_Pupil", (0.02, 0.02, 0.02, 1.0), metallic=0.0, roughness=0.2)
    mat_pink = create_pbr_material("Mat_Kiara_Pink", (1.0, 0.55, 0.68, 1.0), metallic=0.0, roughness=0.65)

    created_mesh_objects = []

    def tag_vertex_group(obj, group_name):
        vg = obj.vertex_groups.new(name=group_name)
        vg.add(list(range(len(obj.data.vertices))), 1.0, 'REPLACE')

    def set_mat_and_smooth(obj, material):
        obj.data.materials.clear()
        obj.data.materials.append(material)
        for poly in obj.data.polygons:
            poly.use_smooth = True

    def make_sphere(name, radius, loc, scale, mat, bone_group):
        bpy.ops.mesh.primitive_uv_sphere_add(segments=18, ring_count=14, radius=radius, location=loc)
        obj = bpy.context.active_object
        obj.name = name
        obj.scale = scale
        bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
        set_mat_and_smooth(obj, mat)
        tag_vertex_group(obj, bone_group)
        created_mesh_objects.append(obj)
        return obj

    def make_cylinder(name, radius, depth, loc, rot, mat, bone_group):
        bpy.ops.mesh.primitive_cylinder_add(vertices=16, radius=radius, depth=depth, location=loc, rotation=rot)
        obj = bpy.context.active_object
        obj.name = name
        bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
        set_mat_and_smooth(obj, mat)
        tag_vertex_group(obj, bone_group)
        created_mesh_objects.append(obj)
        return obj

    def make_cone(name, radius1, depth, loc, rot, mat, bone_group):
        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=radius1, depth=depth, location=loc, rotation=rot)
        obj = bpy.context.active_object
        obj.name = name
        bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
        set_mat_and_smooth(obj, mat)
        tag_vertex_group(obj, bone_group)
        created_mesh_objects.append(obj)
        return obj

    # --- ANATOMIA FELINA QUADRÚPEDE DA KIARA (Branca, Chibi, Proporcional) ---
    # 1. Torso Horizontal (Branco)
    make_sphere("Kiara_Chest", 0.080, (0, -0.06, 0.18), (0.98, 1.25, 0.98), mat_white, 'Spine')
    make_cylinder("Kiara_Belly", 0.070, 0.14, (0, 0.02, 0.18), (math.pi / 2, 0, 0), mat_white, 'Spine')
    make_sphere("Kiara_Pelvis", 0.076, (0, 0.10, 0.18), (0.90, 1.05, 0.95), mat_white, 'Pelvis')

    # 2. Pescoço, Coleira Rosa Neon e Medalha KIARA
    make_cylinder("Kiara_Neck", 0.046, 0.085, (0, -0.15, 0.22), (math.radians(35), 0, 0), mat_white, 'Neck')
    bpy.ops.mesh.primitive_torus_add(major_radius=0.052, minor_radius=0.009, location=(0, -0.165, 0.225), rotation=(math.radians(35), 0, 0))
    collar_obj = bpy.context.active_object
    collar_obj.name = "Kiara_Collar"
    set_mat_and_smooth(collar_obj, mat_collar)
    tag_vertex_group(collar_obj, 'Neck')
    created_mesh_objects.append(collar_obj)

    # Anel dourado e Medalha Rosa Redonda
    bpy.ops.mesh.primitive_torus_add(major_radius=0.009, minor_radius=0.002, location=(0, -0.198, 0.190), rotation=(0, math.pi / 2, 0))
    ring_obj = bpy.context.active_object
    set_mat_and_smooth(ring_obj, mat_gold)
    tag_vertex_group(ring_obj, 'Neck')
    created_mesh_objects.append(ring_obj)

    make_cylinder("Kiara_Medal", 0.016, 0.004, (0, -0.205, 0.175), (math.radians(35), 0, 0), mat_collar, 'Neck')

    # 3. Cabeça Chibi Redonda, Focinho e Bochechas Puffy
    make_sphere("Kiara_Head", 0.080, (0, -0.23, 0.265), (1.05, 0.96, 0.96), mat_white, 'Head')
    make_sphere("Kiara_Muzzle_L", 0.026, (-0.022, -0.295, 0.235), (1.1, 0.85, 0.8), mat_white, 'Head')
    make_sphere("Kiara_Muzzle_R", 0.026, (0.022, -0.295, 0.235), (1.1, 0.85, 0.8), mat_white, 'Head')
    make_sphere("Kiara_Chin", 0.018, (0, -0.285, 0.215), (1.0, 0.8, 0.7), mat_white, 'Head')
    make_sphere("Kiara_Nose", 0.009, (0, -0.318, 0.248), (1.2, 0.8, 0.8), mat_pink, 'Head')

    # 4. Olhos Dourados Expressivos da Kiara com Pupila Vertical Feline
    make_sphere("Kiara_Eye_L", 0.018, (-0.040, -0.294, 0.275), (0.75, 0.35, 1.2), mat_eyes, 'Head')
    make_sphere("Kiara_Pupil_L", 0.006, (-0.040, -0.306, 0.275), (0.35, 0.25, 1.3), mat_pupil, 'Head')
    make_sphere("Kiara_Eye_R", 0.018, (0.040, -0.294, 0.275), (0.75, 0.35, 1.2), mat_eyes, 'Head')
    make_sphere("Kiara_Pupil_R", 0.006, (0.040, -0.306, 0.275), (0.35, 0.25, 1.3), mat_pupil, 'Head')

    # 5. Orelhas Pontiagudas Brancas com Interior Rosa
    make_cone("Kiara_Ear_L", 0.034, 0.075, (-0.052, -0.22, 0.335), (math.radians(-15), math.radians(-18), math.radians(15)), mat_white, 'Head')
    make_cone("Kiara_EarIn_L", 0.024, 0.058, (-0.051, -0.226, 0.335), (math.radians(-15), math.radians(-18), math.radians(15)), mat_pink, 'Head')
    make_cone("Kiara_Ear_R", 0.034, 0.075, (0.052, -0.22, 0.335), (math.radians(-15), math.radians(18), math.radians(-15)), mat_white, 'Head')
    make_cone("Kiara_EarIn_R", 0.024, 0.058, (0.051, -0.226, 0.335), (math.radians(-15), math.radians(18), math.radians(-15)), mat_pink, 'Head')

    # 6. Bigodes Brancos (3 de cada lado)
    whisker_specs = [
        (-1, 0.012, math.radians(10)),
        (-1, 0.000, 0.0),
        (-1, -0.012, math.radians(-10)),
        (1, 0.012, math.radians(10)),
        (1, 0.000, 0.0),
        (1, -0.012, math.radians(-10))
    ]
    for side, z_off, pitch in whisker_specs:
        wx = side * 0.052
        wy = -0.295
        wz = 0.235 + z_off
        make_cylinder(
            f"Kiara_Whisker_{'L' if side < 0 else 'R'}_{z_off}",
            radius=0.0016,
            depth=0.060,
            loc=(wx, wy, wz),
            rot=(pitch, math.radians(side * 85), 0),
            mat=mat_white,
            bone_group='Head'
        )

    # 7. Quatro Patas Quadrúpedes com Almofadinhas Rosa
    # Dianteira Esquerda (FL)
    make_cylinder("Kiara_Leg_FL", 0.022, 0.15, (-0.060, -0.08, 0.09), (0, 0, 0), mat_white, 'Leg_FL')
    make_sphere("Kiara_Paw_FL", 0.028, (-0.060, -0.090, 0.020), (0.9, 1.25, 0.75), mat_white, 'Leg_FL')
    make_sphere("Kiara_Pad_FL", 0.010, (-0.060, -0.092, 0.008), (1.0, 1.0, 0.4), mat_pink, 'Leg_FL')

    # Dianteira Direita (FR)
    make_cylinder("Kiara_Leg_FR", 0.022, 0.15, (0.060, -0.08, 0.09), (0, 0, 0), mat_white, 'Leg_FR')
    make_sphere("Kiara_Paw_FR", 0.028, (0.060, -0.090, 0.020), (0.9, 1.25, 0.75), mat_white, 'Leg_FR')
    make_sphere("Kiara_Pad_FR", 0.010, (0.060, -0.092, 0.008), (1.0, 1.0, 0.4), mat_pink, 'Leg_FR')

    # Traseira Esquerda (BL) - com ancas anatômicas
    make_sphere("Kiara_Thigh_BL", 0.042, (-0.075, 0.08, 0.145), (0.85, 1.2, 1.3), mat_white, 'Leg_BL')
    make_cylinder("Kiara_Leg_BL", 0.020, 0.12, (-0.075, 0.095, 0.070), (math.radians(-16), 0, 0), mat_white, 'Leg_BL')
    make_sphere("Kiara_Paw_BL", 0.028, (-0.075, 0.088, 0.020), (0.9, 1.25, 0.75), mat_white, 'Leg_BL')

    # Traseira Direita (BR)
    make_sphere("Kiara_Thigh_BR", 0.042, (0.075, 0.08, 0.145), (0.85, 1.2, 1.3), mat_white, 'Leg_BR')
    make_cylinder("Kiara_Leg_BR", 0.020, 0.12, (0.075, 0.095, 0.070), (math.radians(-16), 0, 0), mat_white, 'Leg_BR')
    make_sphere("Kiara_Paw_BR", 0.028, (0.075, 0.088, 0.020), (0.9, 1.25, 0.75), mat_white, 'Leg_BR')

    # 8. Cauda Curvada Escura (Carvão/Preta Original da Kiara)
    make_cylinder("Kiara_Tail_01", 0.016, 0.10, (0, 0.18, 0.21), (math.radians(-42), 0, 0), mat_tail_dark, 'Tail_01')
    make_cylinder("Kiara_Tail_02", 0.014, 0.11, (0, 0.25, 0.28), (math.radians(-58), 0, 0), mat_tail_dark, 'Tail_02')
    make_sphere("Kiara_Tail_Tip", 0.014, (0, 0.295, 0.330), (1.0, 1.0, 1.0), mat_tail_dark, 'Tail_02')

    # UNIFICAÇÃO DA MALHA
    bpy.ops.object.select_all(action='DESELECT')
    for m in created_mesh_objects:
        m.select_set(True)
    bpy.context.view_layer.objects.active = created_mesh_objects[0]
    bpy.ops.object.join()
    cat_mesh = bpy.context.active_object
    cat_mesh.name = "Kiara_Mesh"

    # 3. CRIAÇÃO DA ARMAÇÃO ESQUELÉTICA (ARMATURE)
    bpy.ops.object.armature_add(location=(0, 0, 0))
    arm = bpy.context.active_object
    arm.name = "Kiara_Armature"

    bpy.ops.object.mode_set(mode='EDIT')
    ebones = arm.data.edit_bones
    ebones.remove(ebones[0])

    b_root = ebones.new('Root')
    b_root.head = (0, 0.0, 0.18)
    b_root.tail = (0, -0.04, 0.18)

    b_spine = ebones.new('Spine')
    b_spine.head = (0, -0.04, 0.18)
    b_spine.tail = (0, -0.12, 0.18)
    b_spine.parent = b_root

    b_neck = ebones.new('Neck')
    b_neck.head = (0, -0.12, 0.18)
    b_neck.tail = (0, -0.18, 0.22)
    b_neck.parent = b_spine

    b_head = ebones.new('Head')
    b_head.head = (0, -0.18, 0.22)
    b_head.tail = (0, -0.27, 0.27)
    b_head.parent = b_neck

    b_fl = ebones.new('Leg_FL')
    b_fl.head = (-0.060, -0.08, 0.17)
    b_fl.tail = (-0.060, -0.08, 0.01)
    b_fl.parent = b_spine

    b_fr = ebones.new('Leg_FR')
    b_fr.head = (0.060, -0.08, 0.17)
    b_fr.tail = (0.060, -0.08, 0.01)
    b_fr.parent = b_spine

    b_pelvis = ebones.new('Pelvis')
    b_pelvis.head = (0, 0.0, 0.18)
    b_pelvis.tail = (0, 0.10, 0.18)
    b_pelvis.parent = b_root

    b_bl = ebones.new('Leg_BL')
    b_bl.head = (-0.075, 0.08, 0.17)
    b_bl.tail = (-0.075, 0.08, 0.01)
    b_bl.parent = b_pelvis

    b_br = ebones.new('Leg_BR')
    b_br.head = (0.075, 0.08, 0.17)
    b_br.tail = (0.075, 0.08, 0.01)
    b_br.parent = b_pelvis

    b_t1 = ebones.new('Tail_01')
    b_t1.head = (0, 0.14, 0.18)
    b_t1.tail = (0, 0.22, 0.24)
    b_t1.parent = b_pelvis

    b_t2 = ebones.new('Tail_02')
    b_t2.head = (0, 0.22, 0.24)
    b_t2.tail = (0, 0.30, 0.33)
    b_t2.parent = b_t1

    bpy.ops.object.mode_set(mode='OBJECT')

    cat_mesh.parent = arm
    mod = cat_mesh.modifiers.new(name="Armature", type='ARMATURE')
    mod.object = arm

    # 4. CRIAÇÃO DAS 4 AÇÕES ANIMADAS
    bpy.ops.object.mode_set(mode='POSE')
    for pb in arm.pose.bones:
        pb.rotation_mode = 'XYZ'
    bpy.ops.object.mode_set(mode='OBJECT')

    if not arm.animation_data:
        arm.animation_data_create()

    def add_nla_action(action_name, num_frames, keyframes_func):
        act = bpy.data.actions.new(name=action_name)
        arm.animation_data.action = act
        keyframes_func(act)
        track = arm.animation_data.nla_tracks.new()
        track.name = action_name
        strip = track.strips.new(action_name, start=1, action=act)
        strip.action_frame_end = num_frames
        return act

    def insert_bone_rot(pb_name, frame, rx, ry, rz):
        pb = arm.pose.bones.get(pb_name)
        if pb:
            pb.rotation_euler = (rx, ry, rz)
            pb.keyframe_insert(data_path="rotation_euler", frame=frame)

    def insert_bone_loc(pb_name, frame, lx, ly, lz):
        pb = arm.pose.bones.get(pb_name)
        if pb:
            pb.location = (lx, ly, lz)
            pb.keyframe_insert(data_path="location", frame=frame)

    # --- AÇÃO 1: Idle ---
    def make_idle(act):
        for f, z in [(1, 0.0), (20, 0.004), (40, 0.0)]:
            insert_bone_loc('Root', f, 0.0, 0.0, z)
        for f, ry in [(1, 0.0), (20, 0.06), (40, 0.0)]:
            insert_bone_rot('Head', f, 0.0, ry, 0.0)
        for f, rz in [(1, -0.08), (20, 0.08), (40, -0.08)]:
            insert_bone_rot('Tail_01', f, 0.0, 0.0, rz)
            insert_bone_rot('Tail_02', f, 0.0, 0.0, rz * 2.0)

    # --- AÇÃO 2: Walk (Trote Quadrúpede) ---
    def make_walk(act):
        steps = [
            (1,  0.35, -0.35, -0.35,  0.35, 0.0, -0.15),
            (7,  0.10,  0.00,  0.00,  0.10, 0.006, 0.0),
            (13, -0.35,  0.35,  0.35, -0.35, 0.0, 0.15),
            (19,  0.00,  0.10,  0.10,  0.00, 0.006, 0.0),
            (25,  0.35, -0.35, -0.35,  0.35, 0.0, -0.15)
        ]
        for f, r_fl, r_fr, r_bl, r_br, z_root, tail_yaw in steps:
            insert_bone_rot('Leg_FL', f, r_fl, 0.0, 0.0)
            insert_bone_rot('Leg_FR', f, r_fr, 0.0, 0.0)
            insert_bone_rot('Leg_BL', f, r_bl, 0.0, 0.0)
            insert_bone_rot('Leg_BR', f, r_br, 0.0, 0.0)
            insert_bone_loc('Root', f, 0.0, 0.0, z_root)
            insert_bone_rot('Tail_01', f, 0.0, 0.0, tail_yaw)
            insert_bone_rot('Tail_02', f, 0.0, 0.0, tail_yaw * 1.5)

    # --- AÇÃO 3: LickPaw (Ergue a pata dianteira esquerda até o focinho) ---
    def make_lickpaw(act):
        insert_bone_rot('Leg_FL', 1, 0.0, 0.0, 0.0)
        insert_bone_rot('Head', 1, 0.0, 0.0, 0.0)
        insert_bone_loc('Root', 1, 0.0, 0.0, 0.0)

        insert_bone_rot('Leg_FL', 12, math.radians(-70), math.radians(20), math.radians(15))
        insert_bone_rot('Head', 12, math.radians(28), math.radians(-15), math.radians(-10))
        insert_bone_loc('Root', 12, 0.0, 0.015, -0.006)

        for f, (rx_head, rz_paw) in [(20, (32, 22)), (26, (26, 12)), (32, (32, 22))]:
            insert_bone_rot('Head', f, math.radians(rx_head), math.radians(-15), math.radians(-10))
            insert_bone_rot('Leg_FL', f, math.radians(-70), math.radians(rz_paw), math.radians(15))

        insert_bone_rot('Leg_FL', 44, 0.0, 0.0, 0.0)
        insert_bone_rot('Head', 44, 0.0, 0.0, 0.0)
        insert_bone_loc('Root', 44, 0.0, 0.0, 0.0)

    # --- AÇÃO 4: Swat (Patada ágil da pata dianteira direita) ---
    def make_swat(act):
        insert_bone_rot('Leg_FR', 1, 0.0, 0.0, 0.0)
        insert_bone_rot('Head', 1, 0.0, 0.0, 0.0)

        insert_bone_rot('Leg_FR', 6, math.radians(45), 0.0, math.radians(15))
        insert_bone_rot('Head', 6, math.radians(-10), 0.0, math.radians(-5))

        insert_bone_rot('Leg_FR', 10, math.radians(-75), 0.0, math.radians(-25))
        insert_bone_rot('Head', 10, math.radians(15), 0.0, math.radians(10))

        insert_bone_rot('Leg_FR', 20, 0.0, 0.0, 0.0)
        insert_bone_rot('Head', 20, 0.0, 0.0, 0.0)

    act_idle = add_nla_action("Idle", 40, make_idle)
    act_walk = add_nla_action("Walk", 25, make_walk)
    act_lick = add_nla_action("LickPaw", 44, make_lickpaw)
    act_swat = add_nla_action("Swat", 20, make_swat)

    arm.animation_data.action = act_idle

    # 5. AJUSTA VIEWPORT DO BLENDER PARA MODO MATERIAL
    for screen in bpy.data.screens:
        for area in screen.areas:
            if area.type == 'VIEW_3D':
                for space in area.spaces:
                    if space.type == 'VIEW_3D':
                        space.shading.type = 'MATERIAL'

    # 6. EXPORTA O MODELO .GLB FINAL
    out_dir = "/home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/assets/models"
    os.makedirs(out_dir, exist_ok=True)
    out_glb = os.path.join(out_dir, "kiara_3d.glb")

    bpy.ops.export_scene.gltf(
        filepath=out_glb,
        export_format='GLB',
        export_animations=True,
        export_animation_mode='NLA_TRACKS'
    )

    print(f"Kiara quadruped cat successfully built and exported to: {out_glb}")
    print(f"File size: {os.path.getsize(out_glb)} bytes")

if __name__ == "__main__":
    build_quadruped_kiara()
