import bpy
import bmesh
import math
import os
import sys
import numpy as np
from mathutils import Vector

_REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__))) if "__file__" in globals() else os.getcwd()
_SKILL_DIR = os.environ.get(
    "CUSTOMIZER_SKILL_DIR",
    os.path.expanduser("~/.gemini/config/skills/3d-real-asset-customizer"),
)
if _SKILL_DIR not in sys.path:
    sys.path.insert(0, _SKILL_DIR)
from templates.customizer_helper import clear_scene_keep_world, import_and_normalize_glb

SRC_GLB = os.environ.get("KIARA_BASE_GLB", os.path.join(_REPO_ROOT, "ec407d89f7304194bc04f2b4710eab8c.glb"))
OUT_GLB = os.environ.get("KIARA_OUT_GLB", os.path.join(_REPO_ROOT, "app/src/main/raw_assets/models/kiara_3d.glb"))


def smoothstep(edge0, edge1, x):
    t = np.clip((x - edge0) / (edge1 - edge0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def smoothstep_scalar(edge0, edge1, x):
    t = max(0.0, min(1.0, (x - edge0) / (edge1 - edge0)))
    return t * t * (3.0 - 2.0 * t)


def create_pbr_mat(name, base_color, roughness=0.5, metallic=0.0, emission=None, emission_strength=0.0):
    mat = bpy.data.materials.get(name) or bpy.data.materials.new(name=name)
    mat.use_nodes = True
    if hasattr(mat, "blend_method"):
        mat.blend_method = "OPAQUE"
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()
    out = nodes.new("ShaderNodeOutputMaterial")
    bsdf = nodes.new("ShaderNodeBsdfPrincipled")
    bsdf.inputs["Base Color"].default_value = base_color
    bsdf.inputs["Roughness"].default_value = roughness
    bsdf.inputs["Metallic"].default_value = metallic
    if emission is not None and emission_strength > 0.0:
        if "Emission Color" in bsdf.inputs:
            bsdf.inputs["Emission Color"].default_value = emission
        if "Emission Strength" in bsdf.inputs:
            bsdf.inputs["Emission Strength"].default_value = emission_strength
    links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])
    mat.diffuse_color = base_color
    return mat


def evaluate_kiara_coat(pts_xyz):
    """Computes RGB [0..1] for Nx3 coordinates in 0.32m reference space (facing -Y, right=-X, left=+X)."""
    x = pts_xyz[:, 0]
    y = pts_xyz[:, 1]
    z = pts_xyz[:, 2]

    fur_noise = (
        np.sin(x * 260.0 + y * 140.0) * np.cos(z * 220.0 - y * 110.0) * 0.015
        + np.sin(y * 410.0 + z * 320.0) * 0.010
    )
    white = np.stack([
        np.clip(0.945 + fur_noise, 0.88, 0.98),
        np.clip(0.940 + fur_noise, 0.88, 0.98),
        np.clip(0.935 + fur_noise, 0.87, 0.98),
    ], axis=-1)

    black_rgb = np.array([0.055, 0.055, 0.062], dtype=np.float32)
    black_coat = black_rgb[None, :] + (fur_noise * 0.4)[:, None]

    # 1. Right-eye asymmetric black patch (-X side, around eye at (-0.016, -0.312, 0.268), sweeping up to right ear)
    dx_eye = (x - (-0.025)) / 0.026
    dy_eye = (y - (-0.285)) / 0.046
    dz_eye = (z - 0.285) / 0.036
    eye_patch_dist = np.sqrt(dx_eye**2 + dy_eye**2 + dz_eye**2)
    bridge_guard = smoothstep(-0.002, -0.010, x)
    cheek_guard = smoothstep(0.248, 0.260, z + np.clip(-y - 0.26, 0.0, 0.06) * 0.15)
    dx_ear_r = (x - (-0.030)) / 0.025
    dy_ear_r = (y - (-0.268)) / 0.036
    dz_ear_r = (z - 0.302) / 0.028
    ear_r_dist = np.sqrt(dx_ear_r**2 + dy_ear_r**2 + dz_ear_r**2)
    right_face_mask = np.maximum(
        (1.0 - smoothstep(0.85, 1.05, eye_patch_dist)) * bridge_guard * cheek_guard,
        (1.0 - smoothstep(0.85, 1.05, ear_r_dist)) * smoothstep(-0.005, -0.013, x) * smoothstep(0.272, 0.284, z),
    )

    # 2. Left-ear black patch (+X side: upper/inner brow edge & base of left ear, keeping left eye white!)
    dx_ear_l = (x - 0.022) / 0.020
    dy_ear_l = (y - (-0.280)) / 0.034
    dz_ear_l = (z - 0.299) / 0.022
    ear_l_dist = np.sqrt(dx_ear_l**2 + dy_ear_l**2 + dz_ear_l**2)
    left_ear_mask = (
        (1.0 - smoothstep(0.80, 1.05, ear_l_dist))
        * smoothstep(0.006, 0.014, x)
        * smoothstep(0.278, 0.288, z)
    )

    # 3. Left-rump oval black spot (+X hip near tail base)
    dx_rump = (x - 0.020) / 0.022
    dy_rump = (y - 0.040) / 0.032
    dz_rump = (z - 0.208) / 0.032
    rump_dist = np.sqrt(dx_rump**2 + dy_rump**2 + dz_rump**2)
    rump_mask = (1.0 - smoothstep(0.80, 1.02, rump_dist)) * smoothstep(0.000, 0.010, x)

    # 4. Solid black tail right from tail root (Y > +0.070)
    tail_mask = smoothstep(0.068, 0.082, y) * smoothstep(0.155, 0.175, z)

    total_black = np.clip(np.maximum.reduce([right_face_mask, left_ear_mask, rump_mask, tail_mask]), 0.0, 1.0)
    rgb = white * (1.0 - total_black[:, None]) + black_coat * total_black[:, None]
    return np.clip(rgb, 0.0, 1.0)


def bake_fur_texture_to_uv(body_obj, img_name="Kiara_Fur_Albedo", size=512):
    """Rasterizes 3D Kiara coat markings onto body_obj's active UVMap with 4px edge dilation."""
    mesh = body_obj.data
    mw = body_obj.matrix_world

    bm = bmesh.new()
    bm.from_mesh(mesh)
    bmesh.ops.triangulate(bm, faces=bm.faces[:])
    uv_lay = bm.loops.layers.uv.active

    img_buf = np.zeros((size, size, 4), dtype=np.float32)
    covered = np.zeros((size, size), dtype=bool)

    tri_pts = []
    tri_uvs = []
    for f in bm.faces:
        if len(f.verts) != 3:
            continue
        p0, p1, p2 = [mw @ v.co for v in f.verts]
        u0, u1, u2 = [l[uv_lay].uv.copy() for l in f.loops]
        tri_pts.append((p0, p1, p2))
        tri_uvs.append((u0, u1, u2))
    bm.free()

    for (p0, p1, p2), (uv0, uv1, uv2) in zip(tri_pts, tri_uvs):
        x0, y0 = uv0.x * (size - 1), uv0.y * (size - 1)
        x1, y1 = uv1.x * (size - 1), uv1.y * (size - 1)
        x2, y2 = uv2.x * (size - 1), uv2.y * (size - 1)

        min_x = max(0, int(math.floor(min(x0, x1, x2))) - 1)
        max_x = min(size - 1, int(math.ceil(max(x0, x1, x2))) + 1)
        min_y = max(0, int(math.floor(min(y0, y1, y2))) - 1)
        max_y = min(size - 1, int(math.ceil(max(y0, y1, y2))) + 1)

        denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
        if abs(denom) < 1e-8:
            continue

        xs = np.arange(min_x, max_x + 1, dtype=np.float32)
        ys = np.arange(min_y, max_y + 1, dtype=np.float32)
        gx, gy = np.meshgrid(xs, ys)

        w0 = ((y1 - y2) * (gx - x2) + (x2 - x1) * (gy - y2)) / denom
        w1 = ((y2 - y0) * (gx - x2) + (x0 - x2) * (gy - y2)) / denom
        w2 = 1.0 - w0 - w1

        inside = (w0 >= -0.02) & (w1 >= -0.02) & (w2 >= -0.02)
        if not np.any(inside):
            continue

        iw0 = np.clip(w0[inside], 0.0, 1.0)
        iw1 = np.clip(w1[inside], 0.0, 1.0)
        iw2 = np.clip(w2[inside], 0.0, 1.0)
        wsum = iw0 + iw1 + iw2
        iw0 /= wsum
        iw1 /= wsum
        iw2 /= wsum

        pts3d = (
            iw0[:, None] * np.array([p0.x, p0.y, p0.z], dtype=np.float32)
            + iw1[:, None] * np.array([p1.x, p1.y, p1.z], dtype=np.float32)
            + iw2[:, None] * np.array([p2.x, p2.y, p2.z], dtype=np.float32)
        )
        rgb = evaluate_kiara_coat(pts3d)

        iy = (gy[inside]).astype(np.int32)
        ix = (gx[inside]).astype(np.int32)
        img_buf[iy, ix, :3] = rgb
        img_buf[iy, ix, 3] = 1.0
        covered[iy, ix] = True

    for _ in range(4):
        if np.all(covered):
            break
        new_buf = img_buf.copy()
        new_cov = covered.copy()
        for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            shifted_cov = np.roll(np.roll(covered, dy, axis=0), dx, axis=1)
            shifted_buf = np.roll(np.roll(img_buf, dy, axis=0), dx, axis=1)
            fill = (~new_cov) & shifted_cov
            new_buf[fill] = shifted_buf[fill]
            new_cov |= fill
        img_buf = new_buf
        covered = new_cov

    img_buf[~covered, :3] = np.array([0.945, 0.940, 0.935], dtype=np.float32)
    img_buf[~covered, 3] = 1.0

    img = bpy.data.images.new(img_name, width=size, height=size, alpha=False)
    img.pixels.foreach_set(img_buf.ravel())
    img.pack()
    return img


def make_collar_and_tag():
    """Creates a snug-fitting hot-pink collar with full neck clearance, gold ring, and KIARA medal."""
    bpy.ops.mesh.primitive_torus_add(
        major_segments=22,
        minor_segments=10,
        major_radius=0.039,
        minor_radius=0.0044,
        location=(0.0, -0.226, 0.226),
        rotation=(math.radians(46), 0.0, 0.0),
    )
    collar = bpy.context.active_object
    collar.name = "Kiara_Collar"
    collar.scale = (0.98, 1.24, 1.08)
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)

    bpy.ops.mesh.primitive_torus_add(
        major_segments=14,
        minor_segments=6,
        major_radius=0.0048,
        minor_radius=0.0012,
        location=(0.0, -0.260, 0.186),
        rotation=(0.0, math.pi / 2.0, 0.0),
    )
    ring = bpy.context.active_object
    ring.name = "Kiara_GoldRing"

    bpy.ops.mesh.primitive_cylinder_add(
        vertices=20,
        radius=0.0105,
        depth=0.0028,
        location=(0.0, -0.266, 0.174),
        rotation=(math.radians(78), 0.0, 0.0),
    )
    medal = bpy.context.active_object
    medal.name = "Kiara_Medal"

    bpy.ops.object.text_add(
        location=(0.0, -0.2680, 0.173),
        rotation=(math.radians(78), 0.0, 0.0),
    )
    txt = bpy.context.active_object
    txt.data.body = "KIARA"
    txt.data.align_x = "CENTER"
    txt.data.align_y = "CENTER"
    txt.data.size = 0.0046
    txt.data.extrude = 0.0005
    bpy.ops.object.convert(target="MESH")
    txt.name = "Kiara_MedalText"

    mat_collar = create_pbr_mat(
        "Mat_Kiara_Collar",
        (0.88, 0.05, 0.42, 1.0),
        roughness=0.38,
        emission=(0.88, 0.05, 0.42, 1.0),
        emission_strength=0.45,
    )
    mat_gold = create_pbr_mat("Mat_Kiara_Gold", (0.98, 0.78, 0.16, 1.0), roughness=0.22, metallic=0.92)
    mat_medal_txt = create_pbr_mat("Mat_Kiara_TagText", (0.98, 0.98, 0.98, 1.0), roughness=0.35)

    for obj, mat in ((collar, mat_collar), (ring, mat_gold), (medal, mat_collar), (txt, mat_medal_txt)):
        obj.data.materials.clear()
        obj.data.materials.append(mat)
        for p in obj.data.polygons:
            p.use_smooth = True

    return [collar, ring, medal, txt]


def add_weight(obj, bone_name, vert_idx, w):
    if w <= 1e-4:
        return
    vg = obj.vertex_groups.get(bone_name)
    if vg is None:
        vg = obj.vertex_groups.new(name=bone_name)
    vg.add([vert_idx], float(w), "ADD")


def normalize_weights(obj):
    for v in obj.data.vertices:
        total = sum(g.weight for g in v.groups)
        if total > 1e-6:
            for g in v.groups:
                g.weight /= total


def assign_rigid_group(obj, bone_name):
    vg = obj.vertex_groups.get(bone_name) or obj.vertex_groups.new(name=bone_name)
    vg.add(list(range(len(obj.data.vertices))), 1.0, "REPLACE")


def skin_kiara_meshes(all_meshes, arm):
    """Assigns smooth region-isolated anatomical vertex weights across the multi-joint quadruped rig."""
    head_rigid_mats = {
        "Mat_Kiara_Iris",
        "Mat_Kiara_Pupil",
        "Mat_Kiara_EyeWhite",
        "Mat_Kiara_EyeRim",
        "Mat_Kiara_Nose",
        "Mat_Kiara_Nostrils",
        "Mat_Kiara_Whiskers",
        "Mat_Kiara_InnerEar",
    }
    neck_rigid_mats = {"Mat_Kiara_Collar", "Mat_Kiara_Gold", "Mat_Kiara_TagText"}

    def limb_weights_front(p, side_prefix):
        # Below elbow (z < 0.110), vertices belong strictly to Forearm/Paw (never Chest)
        if p.z < 0.110:
            chest_blend = 0.0
        else:
            chest_blend = smoothstep_scalar(0.112, 0.165, p.z)
            chest_blend = max(chest_blend, smoothstep_scalar(-0.152, -0.125, p.y))
            chest_blend = max(chest_blend, 1.0 - smoothstep_scalar(0.014, 0.025, abs(p.x)))
        leg_factor = 1.0 - chest_blend

        if p.z >= 0.105:
            t_elbow = smoothstep_scalar(0.125, 0.095, p.z)
            raw = [(f"UpperArm_{side_prefix}", 1.0 - t_elbow), (f"Forearm_{side_prefix}", t_elbow)]
        elif p.z >= 0.022:
            t_wrist = smoothstep_scalar(0.042, 0.020, p.z)
            raw = [(f"Forearm_{side_prefix}", 1.0 - t_wrist), (f"Paw_F{side_prefix}", t_wrist)]
        else:
            raw = [(f"Paw_F{side_prefix}", 1.0)]

        out = [("Chest", chest_blend)]
        for b, w in raw:
            out.append((b, w * leg_factor))
        return out

    def limb_weights_back(p, side_prefix):
        if p.z < 0.115:
            pelvis_blend = 0.0
        else:
            pelvis_blend = smoothstep_scalar(0.142, 0.184, p.z)
            pelvis_blend = max(pelvis_blend, smoothstep_scalar(0.005, -0.028, p.y))
            pelvis_blend = max(pelvis_blend, 1.0 - smoothstep_scalar(0.014, 0.025, abs(p.x)))
        leg_factor = 1.0 - pelvis_blend

        if p.z >= 0.098:
            t_knee = smoothstep_scalar(0.118, 0.088, p.z)
            raw = [(f"Thigh_{side_prefix}", 1.0 - t_knee), (f"Shin_{side_prefix}", t_knee)]
        elif p.z >= 0.022:
            t_hock = smoothstep_scalar(0.044, 0.020, p.z)
            raw = [(f"Shin_{side_prefix}", 1.0 - t_hock), (f"Paw_B{side_prefix}", t_hock)]
        else:
            raw = [(f"Paw_B{side_prefix}", 1.0)]

        out = [("Pelvis", pelvis_blend)]
        for b, w in raw:
            out.append((b, w * leg_factor))
        return out

    for obj in all_meshes:
        obj.vertex_groups.clear()
        mat_name = obj.data.materials[0].name if obj.data.materials else ""

        if mat_name in head_rigid_mats:
            if mat_name == "Mat_Kiara_InnerEar":
                for i, v in enumerate(obj.data.vertices):
                    p = obj.matrix_world @ v.co
                    if p.z > 0.292:
                        t = min(1.0, (p.z - 0.292) / 0.018)
                        ear = "Ear_L" if p.x >= 0 else "Ear_R"
                        add_weight(obj, "Head", i, 1.0 - t)
                        add_weight(obj, ear, i, t)
                    else:
                        add_weight(obj, "Head", i, 1.0)
                normalize_weights(obj)
            else:
                assign_rigid_group(obj, "Head")
            continue

        if mat_name in neck_rigid_mats:
            assign_rigid_group(obj, "Neck")
            continue

        if mat_name in {"Mat_Kiara_FootPads", "Mat_Kiara_Claws"}:
            for i, v in enumerate(obj.data.vertices):
                p = obj.matrix_world @ v.co
                side = "L" if p.x >= 0 else "R"
                bone = f"Paw_F{side}" if p.y < -0.09 else f"Paw_B{side}"
                add_weight(obj, bone, i, 1.0)
            normalize_weights(obj)
            continue

        for i, v in enumerate(obj.data.vertices):
            p = obj.matrix_world @ v.co
            side = "L" if p.x >= 0 else "R"

            # 1. Tail (Y > 0.072, Z > 0.165)
            if p.y > 0.072 and p.z > 0.165:
                if p.y < 0.098:
                    t = smoothstep_scalar(0.072, 0.098, p.y)
                    add_weight(obj, "Pelvis", i, 1.0 - t)
                    add_weight(obj, "Tail_01", i, t)
                elif p.y < 0.150:
                    t = smoothstep_scalar(0.098, 0.150, p.y)
                    add_weight(obj, "Tail_01", i, 1.0 - t)
                    add_weight(obj, "Tail_02", i, t)
                elif p.y < 0.215:
                    t = smoothstep_scalar(0.150, 0.215, p.y)
                    add_weight(obj, "Tail_02", i, 1.0 - t)
                    add_weight(obj, "Tail_03", i, t)
                elif p.y < 0.275:
                    t = smoothstep_scalar(0.215, 0.275, p.y)
                    add_weight(obj, "Tail_03", i, 1.0 - t)
                    add_weight(obj, "Tail_04", i, t)
                else:
                    add_weight(obj, "Tail_04", i, 1.0)
                continue

            # 2. Front legs: any lower-leg vertex (Z < 0.110) or shoulder vertex ahead of ribcage
            if p.y < -0.118 and ((p.z < 0.110) or (p.z < 0.176 and abs(p.x) > 0.012)):
                for bname, w in limb_weights_front(p, side):
                    add_weight(obj, bname, i, w)
                continue

            # 3. Back legs: any lower-leg vertex (Z < 0.115) or hip vertex behind mid-belly
            if -0.028 < p.y < 0.085 and ((p.z < 0.115) or (p.z < 0.184 and abs(p.x) > 0.012)):
                for bname, w in limb_weights_back(p, side):
                    add_weight(obj, bname, i, w)
                continue

            # 4. Head & Ears
            if (p.y < -0.242 and p.z > 0.205) or p.z > 0.268:
                if p.z > 0.292 and abs(p.x) > 0.015:
                    t = min(1.0, (p.z - 0.292) / 0.018)
                    ear = f"Ear_{side}"
                    add_weight(obj, "Head", i, 1.0 - t)
                    add_weight(obj, ear, i, t)
                elif p.y > -0.255 and p.z < 0.255:
                    t = smoothstep_scalar(-0.242, -0.255, p.y)
                    add_weight(obj, "Neck", i, 1.0 - t)
                    add_weight(obj, "Head", i, t)
                else:
                    add_weight(obj, "Head", i, 1.0)
                continue

            # 5. Neck / Chest / Spine / Pelvis along Y
            if p.y <= -0.205:
                t = smoothstep_scalar(-0.205, -0.245, p.y)
                add_weight(obj, "Chest", i, 1.0 - t)
                add_weight(obj, "Neck", i, t)
            elif p.y <= -0.135:
                t = smoothstep_scalar(-0.135, -0.205, p.y)
                add_weight(obj, "Spine", i, 1.0 - t)
                add_weight(obj, "Chest", i, t)
            elif p.y <= -0.035:
                t = smoothstep_scalar(-0.035, -0.135, p.y)
                add_weight(obj, "Pelvis", i, 1.0 - t)
                add_weight(obj, "Spine", i, t)
            else:
                add_weight(obj, "Pelvis", i, 1.0)

        normalize_weights(obj)


def align_bone_x_axis(ebone, desired_x=Vector((1.0, 0.0, 0.0))):
    """Rotates ebone.roll so ebone.x_axis aligns strictly with desired_x (+X world)."""
    best_roll = 0.0
    best_dot = -2.0
    # Coarse search + golden refinement
    for deg in range(0, 360, 5):
        ebone.roll = math.radians(deg)
        d = ebone.x_axis.dot(desired_x)
        if d > best_dot:
            best_dot = d
            best_roll = math.radians(deg)
    step = math.radians(5.0)
    for _ in range(12):
        ebone.roll = best_roll + step
        d_plus = ebone.x_axis.dot(desired_x)
        ebone.roll = best_roll - step
        d_minus = ebone.x_axis.dot(desired_x)
        if d_plus > best_dot:
            best_dot = d_plus
            best_roll += step
        elif d_minus > best_dot:
            best_dot = d_minus
            best_roll -= step
        step *= 0.5
    ebone.roll = best_roll


def build_armature():
    """Creates a clean quadruped armature with guaranteed +X local lateral axis on every bone."""
    bpy.ops.object.armature_add(location=(0, 0, 0))
    arm = bpy.context.active_object
    arm.name = "Kiara_Armature"

    bpy.ops.object.mode_set(mode="EDIT")
    eb = arm.data.edit_bones
    eb.remove(eb[0])

    def add_bone(name, head, tail, parent=None):
        b = eb.new(name)
        b.head = Vector(head)
        b.tail = Vector(tail)
        if parent is not None:
            b.parent = parent
        align_bone_x_axis(b, Vector((1.0, 0.0, 0.0)))
        return b

    root = add_bone("Root", (0.0, -0.04, 0.16), (0.0, -0.07, 0.16))
    pelvis = add_bone("Pelvis", (0.0, -0.04, 0.16), (0.0, 0.06, 0.18), root)
    spine = add_bone("Spine", (0.0, -0.04, 0.16), (0.0, -0.14, 0.17), root)
    chest = add_bone("Chest", (0.0, -0.14, 0.17), (0.0, -0.20, 0.20), spine)
    neck = add_bone("Neck", (0.0, -0.20, 0.20), (0.0, -0.245, 0.24), chest)
    head = add_bone("Head", (0.0, -0.245, 0.24), (0.0, -0.305, 0.27), neck)
    add_bone("Ear_L", (0.025, -0.272, 0.285), (0.036, -0.274, 0.318), head)
    add_bone("Ear_R", (-0.025, -0.272, 0.285), (-0.036, -0.274, 0.318), head)

    for side, sx in (("L", 1.0), ("R", -1.0)):
        ua = add_bone(f"UpperArm_{side}", (sx * 0.037, -0.178, 0.178), (sx * 0.032, -0.173, 0.112), chest)
        fa = add_bone(f"Forearm_{side}", (sx * 0.032, -0.173, 0.112), (sx * 0.031, -0.184, 0.032), ua)
        add_bone(f"Paw_F{side}", (sx * 0.031, -0.184, 0.032), (sx * 0.031, -0.204, 0.008), fa)

    for side, sx in (("L", 1.0), ("R", -1.0)):
        th = add_bone(f"Thigh_{side}", (sx * 0.038, 0.024, 0.172), (sx * 0.036, 0.018, 0.105), pelvis)
        sh = add_bone(f"Shin_{side}", (sx * 0.036, 0.018, 0.105), (sx * 0.037, 0.041, 0.042), th)
        add_bone(f"Paw_B{side}", (sx * 0.037, 0.041, 0.042), (sx * 0.038, 0.022, 0.006), sh)

    t1 = add_bone("Tail_01", (0.0, 0.072, 0.192), (0.0, 0.132, 0.208), pelvis)
    t2 = add_bone("Tail_02", (0.0, 0.132, 0.208), (0.0, 0.198, 0.210), t1)
    t3 = add_bone("Tail_03", (0.0, 0.198, 0.210), (0.0, 0.264, 0.210), t2)
    add_bone("Tail_04", (0.0, 0.264, 0.210), (0.0, 0.332, 0.210), t3)

    bpy.ops.object.mode_set(mode="OBJECT")
    return arm


def create_animations(arm):
    """Creates 4 distinct multi-joint NLA animations with +X verified bone axes."""
    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.mode_set(mode="POSE")
    for pb in arm.pose.bones:
        pb.rotation_mode = "XYZ"
    bpy.ops.object.mode_set(mode="OBJECT")

    if not arm.animation_data:
        arm.animation_data_create()
    for tr in list(arm.animation_data.nla_tracks):
        arm.animation_data.nla_tracks.remove(tr)

    def reset_all_pose_bones():
        for pb in arm.pose.bones:
            pb.location = (0.0, 0.0, 0.0)
            pb.rotation_euler = (0.0, 0.0, 0.0)
            pb.scale = (1.0, 1.0, 1.0)

    def key_rot(pb_name, frame, rx_deg, ry_deg, rz_deg):
        pb = arm.pose.bones.get(pb_name)
        if pb:
            pb.rotation_euler = (math.radians(rx_deg), math.radians(ry_deg), math.radians(rz_deg))
            pb.keyframe_insert(data_path="rotation_euler", frame=frame)

    def key_loc(pb_name, frame, lx, ly, lz):
        pb = arm.pose.bones.get(pb_name)
        if pb:
            pb.location = (lx, ly, lz)
            pb.keyframe_insert(data_path="location", frame=frame)

    def add_action(name, end_frame, build_fn):
        act = bpy.data.actions.new(name=name)
        act.use_fake_user = True
        arm.animation_data.action = act
        reset_all_pose_bones()
        # Seed neutral keyframes at frame 1 & end_frame for all bones so no stale pose leaks across actions
        for pb in arm.pose.bones:
            pb.location = (0.0, 0.0, 0.0)
            pb.rotation_euler = (0.0, 0.0, 0.0)
            pb.keyframe_insert(data_path="location", frame=1)
            pb.keyframe_insert(data_path="rotation_euler", frame=1)
            pb.keyframe_insert(data_path="location", frame=end_frame)
            pb.keyframe_insert(data_path="rotation_euler", frame=end_frame)
        build_fn()
        tr = arm.animation_data.nla_tracks.new()
        tr.name = name
        strip = tr.strips.new(name, start=1, action=act)
        strip.action_frame_end = end_frame
        return act

    # With local +X strictly aligned to World +X:
    # - Downward leg bones (-Z): rx < 0 swings forward (-Y); rx > 0 swings backward (+Y).
    # - Tail (+Y): rx > 0 curves UP (+Z), rx < 0 curves DOWN (-Z); ry/rz sways left-right.
    # - Neck/Head (-Y/+Z): rx < 0 tilts nose DOWN, rx > 0 tilts nose UP.

    # 1. IDLE (60 frames)
    def build_idle():
        for fr in (1, 15, 30, 45, 60):
            phase = (fr - 1) / 59.0 * 2.0 * math.pi
            breath = math.sin(phase)
            sway = math.sin(phase) * 12.0
            key_loc("Root", fr, 0.0, 0.0, 0.002 * breath)
            key_rot("Chest", fr, 1.5 * breath, 0.0, 0.0)
            key_rot("Head", fr, 2.0 * math.cos(phase), 0.0, 6.0 * math.sin(phase * 0.5))
            key_rot("Tail_01", fr, -8.0, 0.0, sway * 0.30)
            key_rot("Tail_02", fr, 22.0, 0.0, sway * 0.55)
            key_rot("Tail_03", fr, 26.0, 0.0, sway * 0.85)
            key_rot("Tail_04", fr, 20.0, 0.0, sway * 1.15)
        for fr, el, er in [(1, 0, 0), (20, -10, 0), (24, 0, 0), (44, 0, -10), (48, 0, 0), (60, 0, 0)]:
            key_rot("Ear_L", fr, el, 0.0, 0.0)
            key_rot("Ear_R", fr, er, 0.0, 0.0)

    # 2. WALK (24-frame feline diagonal trot across all 4 articulated legs)
    def build_walk():
        for fr in (1, 4, 7, 10, 13, 16, 19, 22, 25):
            t = ((fr - 1) / 24.0) * 2.0 * math.pi
            for side, phase in (("L", t), ("R", t + math.pi)):
                s_f = math.sin(phase)
                c_f = math.cos(phase)
                swing_lift_f = max(0.0, c_f)
                # Forward swing: rx < 0 at UpperArm, elbow bends backward (+rx) to lift paw cleanly!
                ua_rx = -24.0 * s_f - 6.0 * swing_lift_f
                fa_rx = 44.0 * swing_lift_f + max(0.0, s_f) * 6.0
                paw_rx = -18.0 * swing_lift_f + 8.0 * max(0.0, -s_f)
                key_rot(f"UpperArm_{side}", fr, ua_rx, 0.0, 0.0)
                key_rot(f"Forearm_{side}", fr, fa_rx, 0.0, 0.0)
                key_rot(f"Paw_F{side}", fr, paw_rx, 0.0, 0.0)

                # Back leg in diagonal phase (opposite to front leg on same side)
                b_phase = phase + math.pi
                s_b = math.sin(b_phase)
                c_b = math.cos(b_phase)
                swing_lift_b = max(0.0, c_b)
                th_rx = -22.0 * s_b - 6.0 * swing_lift_b
                sh_rx = -34.0 * swing_lift_b + 6.0 * max(0.0, s_b)
                paw_b_rx = 22.0 * swing_lift_b
                key_rot(f"Thigh_{side}", fr, th_rx, 0.0, 0.0)
                key_rot(f"Shin_{side}", fr, sh_rx, 0.0, 0.0)
                key_rot(f"Paw_B{side}", fr, paw_b_rx, 0.0, 0.0)

            bob = abs(math.sin(t)) * 0.005
            roll = math.sin(t) * 2.5
            key_loc("Root", fr, 0.0, 0.0, bob)
            key_rot("Spine", fr, 0.0, roll, 0.0)
            key_rot("Chest", fr, -2.0 * math.cos(2 * t), -roll * 0.6, 0.0)
            key_rot("Head", fr, 2.5 * math.cos(2 * t), 0.0, 2.0 * math.sin(t))
            sway = math.sin(t) * 14.0
            key_rot("Tail_01", fr, 14.0, 0.0, sway * 0.35)
            key_rot("Tail_02", fr, 24.0, 0.0, sway * 0.65)
            key_rot("Tail_03", fr, 22.0, 0.0, sway * 0.90)
            key_rot("Tail_04", fr, 16.0, 0.0, sway * 1.15)

    # 3. LICKPAW (72 frames) - lifts front left paw cleanly FORWARD toward tilted muzzle
    def build_lickpaw():
        for fr in (1, 72):
            key_loc("Root", fr, 0.0, 0.0, 0.0)
            key_rot("Pelvis", fr, 0.0, 0.0, 0.0)
            key_rot("Chest", fr, 0.0, 0.0, 0.0)
            key_rot("Neck", fr, 0.0, 0.0, 0.0)
            key_rot("Head", fr, 0.0, 0.0, 0.0)
            key_rot("UpperArm_L", fr, 0.0, 0.0, 0.0)
            key_rot("Forearm_L", fr, 0.0, 0.0, 0.0)
            key_rot("Paw_FL", fr, 0.0, 0.0, 0.0)
            key_rot("Tail_01", fr, 10.0, 0.0, 0.0)
            key_rot("Tail_02", fr, 22.0, 0.0, 0.0)
            key_rot("Tail_03", fr, 20.0, 0.0, 0.0)

        for fr, lick in [(16, 0.0), (26, 1.0), (34, -0.4), (42, 1.0), (50, -0.4), (58, 0.4)]:
            key_loc("Root", fr, 0.0, 0.005, -0.004)
            key_rot("Chest", fr, 8.0, 0.0, 0.0)
            # Positive X pitches Neck/Head nose DOWN toward the lifted front-left paw
            key_rot("Neck", fr, 18.0 + 4.0 * lick, 0.0, 10.0)
            key_rot("Head", fr, 24.0 + 6.0 * lick, 0.0, 8.0)
            # UpperArm_L swings forward (-56) and inward; Forearm_L bends up (-44) right under muzzle
            key_rot("UpperArm_L", fr, -56.0 - 4.0 * lick, 0.0, -10.0)
            key_rot("Forearm_L", fr, -44.0 - 6.0 * lick, 0.0, -6.0)
            key_rot("Paw_FL", fr, -16.0, 0.0, 0.0)
            key_rot("Tail_01", fr, 12.0, 0.0, 8.0 * lick)
            key_rot("Tail_02", fr, 24.0, 0.0, 14.0 * lick)
            key_rot("Tail_03", fr, 22.0, 0.0, 18.0 * lick)

    # 4. SWAT (30 frames) - alert coil and quick front-right paw swipe forward toward the barrier
    def build_swat():
        for fr in (1, 30):
            key_loc("Root", fr, 0.0, 0.0, 0.0)
            key_rot("Chest", fr, 0.0, 0.0, 0.0)
            key_rot("Head", fr, 0.0, 0.0, 0.0)
            key_rot("UpperArm_R", fr, 0.0, 0.0, 0.0)
            key_rot("Forearm_R", fr, 0.0, 0.0, 0.0)
            key_rot("Paw_FR", fr, 0.0, 0.0, 0.0)
            key_rot("Tail_01", fr, 12.0, 0.0, 0.0)
            key_rot("Tail_02", fr, 24.0, 0.0, 0.0)
            key_rot("Tail_03", fr, 22.0, 0.0, 0.0)

        # Coil back (fr 7)
        key_loc("Root", 7, 0.0, 0.008, -0.006)
        key_rot("Chest", 7, 6.0, 0.0, -6.0)
        key_rot("Head", 7, 8.0, 0.0, -8.0)
        key_rot("UpperArm_R", 7, 16.0, 0.0, 8.0)
        key_rot("Forearm_R", 7, 48.0, 0.0, 0.0)
        key_rot("Paw_FR", 7, 15.0, 0.0, 0.0)
        key_rot("Tail_01", 7, 18.0, 0.0, -14.0)

        # Lightning swipe forward & slightly right (fr 13)
        key_loc("Root", 13, 0.0, -0.010, 0.003)
        key_rot("Chest", 13, -4.0, 0.0, 8.0)
        key_rot("Head", 13, -10.0, 0.0, 8.0)
        key_rot("UpperArm_R", 13, -60.0, 0.0, 12.0)
        key_rot("Forearm_R", 13, -14.0, 0.0, 6.0)
        key_rot("Paw_FR", 13, -8.0, 0.0, 0.0)
        key_rot("Tail_01", 13, 24.0, 0.0, 20.0)
        key_rot("Tail_02", 13, 26.0, 0.0, 24.0)
        key_rot("Tail_03", 13, 22.0, 0.0, 28.0)

        # Follow-through (fr 20)
        key_loc("Root", 20, 0.0, -0.004, 0.0)
        key_rot("UpperArm_R", 20, -28.0, 0.0, 6.0)
        key_rot("Forearm_R", 20, 12.0, 0.0, 0.0)
        key_rot("Head", 20, 0.0, 0.0, 4.0)

    act_idle = add_action("Idle", 60, build_idle)
    add_action("Walk", 25, build_walk)
    add_action("LickPaw", 72, build_lickpaw)
    add_action("Swat", 30, build_swat)
    arm.animation_data.action = act_idle


def build_and_export_kiara():
    clear_scene_keep_world()
    for act in list(bpy.data.actions):
        act.use_fake_user = False
        bpy.data.actions.remove(act)
    root, objs, meshes = import_and_normalize_glb(SRC_GLB, root_name="Kiara_RawRoot", target_height=0.32, z_rot_deg=0.0)

    bpy.ops.object.select_all(action="DESELECT")
    for o in meshes:
        o.select_set(True)
    bpy.context.view_layer.objects.active = meshes[0]
    bpy.ops.object.parent_clear(type="CLEAR_KEEP_TRANSFORM")
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)

    for o in list(bpy.data.objects):
        if o.type != "MESH":
            bpy.data.objects.remove(o, do_unlink=True)

    drop_mats = {"InnerMouth", "Teeth", "Tongue", "Cornea"}
    for o in list(bpy.data.objects):
        if o.type == "MESH" and any(m and m.name in drop_mats for m in o.data.materials):
            bpy.data.objects.remove(o, do_unlink=True)

    body_parts = [o for o in bpy.data.objects if o.type == "MESH" and any(m and m.name == "material" for m in o.data.materials)]
    bpy.ops.object.select_all(action="DESELECT")
    for bp in body_parts:
        bp.select_set(True)
    bpy.context.view_layer.objects.active = body_parts[0]
    bpy.ops.object.join()
    body = bpy.context.active_object
    body.name = "Kiara_Body"

    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.mesh.remove_doubles(threshold=1e-5)
    bpy.ops.object.mode_set(mode="OBJECT")

    dec = body.modifiers.new("Decimate", "DECIMATE")
    dec.ratio = 0.16
    bpy.context.view_layer.objects.active = body
    bpy.ops.object.modifier_apply(modifier=dec.name)

    dec_targets = {"FootPads": 0.20, "Claws": 0.25, "InnerEar": 0.40, "Whiskers": 0.40}
    for o in list(bpy.data.objects):
        if o.type == "MESH" and o != body:
            for m in o.data.materials:
                if m and m.name in dec_targets:
                    d = o.modifiers.new("Decimate", "DECIMATE")
                    d.ratio = dec_targets[m.name]
                    bpy.context.view_layer.objects.active = o
                    bpy.ops.object.modifier_apply(modifier=d.name)

    fur_img = bake_fur_texture_to_uv(body, "Kiara_Fur_Albedo", size=512)
    mat_fur = bpy.data.materials.new("Mat_Kiara_Fur")
    mat_fur.use_nodes = True
    mat_fur.blend_method = "OPAQUE"
    nodes = mat_fur.node_tree.nodes
    links = mat_fur.node_tree.links
    nodes.clear()
    out = nodes.new("ShaderNodeOutputMaterial")
    bsdf = nodes.new("ShaderNodeBsdfPrincipled")
    bsdf.inputs["Roughness"].default_value = 0.68
    bsdf.inputs["Metallic"].default_value = 0.0
    tex = nodes.new("ShaderNodeTexImage")
    tex.image = fur_img
    links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])
    body.data.materials.clear()
    body.data.materials.append(mat_fur)

    mat_map = {
        "Iris": create_pbr_mat("Mat_Kiara_Iris", (0.78, 0.50, 0.08, 1.0), roughness=0.08, emission=(0.78, 0.50, 0.08, 1.0), emission_strength=0.18),
        "Pupil": create_pbr_mat("Mat_Kiara_Pupil", (0.012, 0.012, 0.015, 1.0), roughness=0.08),
        "EyeWhite": create_pbr_mat("Mat_Kiara_EyeWhite", (0.90, 0.88, 0.85, 1.0), roughness=0.15),
        "EyeRim": create_pbr_mat("Mat_Kiara_EyeRim", (0.10, 0.09, 0.09, 1.0), roughness=0.45),
        "Nose": create_pbr_mat("Mat_Kiara_Nose", (0.90, 0.48, 0.54, 1.0), roughness=0.48),
        "Nostrils": create_pbr_mat("Mat_Kiara_Nostrils", (0.40, 0.16, 0.20, 1.0), roughness=0.60),
        "InnerEar": create_pbr_mat("Mat_Kiara_InnerEar", (0.92, 0.62, 0.66, 1.0), roughness=0.62),
        "FootPads": create_pbr_mat("Mat_Kiara_FootPads", (0.88, 0.52, 0.58, 1.0), roughness=0.58),
        "Claws": create_pbr_mat("Mat_Kiara_Claws", (0.88, 0.85, 0.78, 1.0), roughness=0.35),
        "Whiskers": create_pbr_mat("Mat_Kiara_Whiskers", (0.96, 0.96, 0.96, 1.0), roughness=0.35),
    }

    for o in list(bpy.data.objects):
        if o.type == "MESH" and o != body and o.data.materials:
            old_name = o.data.materials[0].name
            if old_name in mat_map:
                o.data.materials[0] = mat_map[old_name]
                for p in o.data.polygons:
                    p.use_smooth = True

    make_collar_and_tag()

    all_meshes = [o for o in bpy.data.objects if o.type == "MESH"]
    arm = build_armature()
    skin_kiara_meshes(all_meshes, arm)

    for o in all_meshes:
        o.parent = arm
        mod = o.modifiers.new(name="Armature", type="ARMATURE")
        mod.object = arm

    create_animations(arm)

    for img in list(bpy.data.images):
        if img != fur_img and img.users == 0:
            bpy.data.images.remove(img)
    for m in list(bpy.data.materials):
        if m.users == 0:
            bpy.data.materials.remove(m)

    total_polys = sum(len(o.data.polygons) for o in all_meshes)
    print(f"Rigged Kiara total polys: {total_polys}, bones: {len(arm.data.bones)}")

    os.makedirs(os.path.dirname(OUT_GLB), exist_ok=True)
    bpy.ops.object.select_all(action="DESELECT")
    arm.select_set(True)
    for o in all_meshes:
        o.select_set(True)
    bpy.context.view_layer.objects.active = arm

    bpy.ops.export_scene.gltf(
        filepath=OUT_GLB,
        export_format="GLB",
        use_selection=True,
        export_animations=True,
        export_animation_mode="NLA_TRACKS",
        export_yup=True,
    )
    print(f"Exported {OUT_GLB} ({os.path.getsize(OUT_GLB) / 1024.0:.1f} KB)")


if __name__ == "__main__":
    build_and_export_kiara()
