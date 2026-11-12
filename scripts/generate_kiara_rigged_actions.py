import bpy
import mathutils
import math
import os
import json

src_path = 'app/src/main/assets/models/kiara_3d.glb'
bak_path = 'app/src/main/assets/models/kiara_3d_orig.glb'
if not os.path.exists(bak_path):
    import shutil
    shutil.copyfile(src_path, bak_path)

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=bak_path)

# Remove unwanted non-cat objects
for name in ['Studio_Pedestal', 'Cam_Target', 'Camera', 'Cube', 'Icosphere', 'Light']:
    obj = bpy.data.objects.get(name)
    if obj:
        bpy.data.objects.remove(obj, do_unlink=True)
        print(f"Removed non-cat object: {name}")

arm = bpy.data.objects.get('Kiara_Armature')
bpy.context.view_layer.objects.active = arm

if not arm.animation_data:
    arm.animation_data_create()

for track in list(arm.animation_data.nla_tracks):
    arm.animation_data.nla_tracks.remove(track)

# Add functional limb bones
bpy.ops.object.mode_set(mode='EDIT')
ebones = arm.data.edit_bones
spine = ebones.get('Spine')

# Front paws
if 'Paw_L' not in ebones:
    b = ebones.new('Paw_L')
    b.head = mathutils.Vector((-0.075, -0.120, 0.220))
    b.tail = mathutils.Vector((-0.076, -0.235, 0.038))
    b.parent = spine

if 'Paw_R' not in ebones:
    b = ebones.new('Paw_R')
    b.head = mathutils.Vector((0.075, -0.120, 0.220))
    b.tail = mathutils.Vector((0.076, -0.235, 0.038))
    b.parent = spine

# Rear haunches / hind legs
if 'Haunch_L' not in ebones:
    b = ebones.new('Haunch_L')
    b.head = mathutils.Vector((-0.181, 0.057, 0.240))
    b.tail = mathutils.Vector((-0.181, 0.057, 0.050))
    b.parent = spine

if 'Haunch_R' not in ebones:
    b = ebones.new('Haunch_R')
    b.head = mathutils.Vector((0.178, 0.057, 0.240))
    b.tail = mathutils.Vector((0.178, 0.057, 0.050))
    b.parent = spine

bpy.ops.object.mode_set(mode='OBJECT')

# Reparent meshes to functional bones
for name in ['Kiara_Leg_L', 'Kiara_Paw_L', 'Kiara_PawPad_L', 'Kiara_Toe_L_0', 'Kiara_Toe_L_1', 'Kiara_Toe_L_2']:
    obj = bpy.data.objects.get(name)
    if obj:
        obj.parent_bone = 'Paw_L'

for name in ['Kiara_Leg_R', 'Kiara_Paw_R', 'Kiara_PawPad_R', 'Kiara_Toe_R_0', 'Kiara_Toe_R_1', 'Kiara_Toe_R_2']:
    obj = bpy.data.objects.get(name)
    if obj:
        obj.parent_bone = 'Paw_R'

obj_hl = bpy.data.objects.get('Kiara_Haunch_L')
if obj_hl:
    obj_hl.parent_bone = 'Haunch_L'

obj_hr = bpy.data.objects.get('Kiara_Haunch_R')
if obj_hr:
    obj_hr.parent_bone = 'Haunch_R'

bpy.ops.object.mode_set(mode='POSE')
for pb in arm.pose.bones:
    pb.rotation_mode = 'XYZ'
bpy.ops.object.mode_set(mode='OBJECT')

def insert_rot(pb_name, frame, rx, ry, rz):
    pb = arm.pose.bones.get(pb_name)
    if pb:
        pb.rotation_euler = (math.radians(rx), math.radians(ry), math.radians(rz))
        pb.keyframe_insert(data_path="rotation_euler", frame=frame)

def insert_loc(pb_name, frame, lx, ly, lz):
    pb = arm.pose.bones.get(pb_name)
    if pb:
        pb.location = (lx, ly, lz)
        pb.keyframe_insert(data_path="location", frame=frame)

# ----------------------------------------------------
# ACTION 1: Idle (60 frames)
# ----------------------------------------------------
act_idle = bpy.data.actions.new(name="Idle")
act_idle.use_fake_user = True
arm.animation_data.action = act_idle

for fr, ang in [(1, 0.0), (15, 12.0), (30, 0.0), (45, -12.0), (60, 0.0)]:
    insert_rot("Tail_1", fr, 0, 0, ang * 0.5)
    insert_rot("Tail_2", fr, 0, 0, ang * 0.8)
    insert_rot("Tail_3", fr, 0, 0, ang * 1.2)
    insert_rot("Tail_4", fr, 0, 0, ang * 1.5)

for fr, z_off in [(1, 0.0), (30, 0.005), (60, 0.0)]:
    insert_loc("Spine", fr, 0, 0, z_off)
    insert_rot("Chest", fr, (z_off / 0.005) * 1.5, 0, 0)

insert_rot("Head", 1, 0, 0, 0)
insert_rot("Head", 20, 2.0, 0, 4.0)
insert_rot("Head", 40, -1.0, 0, -3.0)
insert_rot("Head", 60, 0, 0, 0)

insert_rot("Ear_L", 1, 0, 0, 0)
insert_rot("Ear_L", 22, -4.0, 0, 0)
insert_rot("Ear_L", 26, 0, 0, 0)
insert_rot("Ear_L", 60, 0, 0, 0)

insert_rot("Ear_R", 1, 0, 0, 0)
insert_rot("Ear_R", 42, -4.0, 0, 0)
insert_rot("Ear_R", 46, 0, 0, 0)
insert_rot("Ear_R", 60, 0, 0, 0)

insert_rot("Paw_L", 1, 0, 0, 0)
insert_rot("Paw_L", 60, 0, 0, 0)
insert_rot("Paw_R", 1, 0, 0, 0)
insert_rot("Paw_R", 60, 0, 0, 0)
insert_rot("Haunch_L", 1, 0, 0, 0)
insert_rot("Haunch_L", 60, 0, 0, 0)
insert_rot("Haunch_R", 1, 0, 0, 0)
insert_rot("Haunch_R", 60, 0, 0, 0)

# ----------------------------------------------------
# ACTION 2: Walk (30 frames loop) - Feline Quadruped Trot
# ----------------------------------------------------
act_walk = bpy.data.actions.new(name="Walk")
act_walk.use_fake_user = True
arm.animation_data.action = act_walk

# Diagonal pair 1: Paw_L (front left) and Haunch_R (rear right)
for fr, p_front, p_rear in [(1, 16.0, 10.0), (8, 0.0, 0.0), (15, -14.0, -8.0), (23, 0.0, 0.0), (30, 16.0, 10.0)]:
    insert_rot("Paw_L", fr, p_front, 0, 0)
    insert_rot("Haunch_R", fr, p_rear, 0, 0)

# Diagonal pair 2: Paw_R (front right) and Haunch_L (rear left)
for fr, p_front, p_rear in [(1, -14.0, -8.0), (8, 0.0, 0.0), (15, 16.0, 10.0), (23, 0.0, 0.0), (30, -14.0, -8.0)]:
    insert_rot("Paw_R", fr, p_front, 0, 0)
    insert_rot("Haunch_L", fr, p_rear, 0, 0)

# Spine vertical bobbing & roll sway
for fr, z_off in [(1, 0.012), (8, -0.006), (15, 0.012), (23, -0.006), (30, 0.012)]:
    insert_loc("Spine", fr, 0, 0, z_off)

for fr, roll in [(1, 2.5), (8, 0.0), (15, -2.5), (23, 0.0), (30, 2.5)]:
    insert_rot("Spine", fr, 0, roll, 0)

# Head cadence
for fr, pitch in [(1, 2.5), (8, -2.5), (15, 2.5), (23, -2.5), (30, 2.5)]:
    insert_rot("Head", fr, pitch, 0, 0)

# Tail dynamic counter-balance
for fr, sway in [(1, -14.0), (8, 0.0), (15, 14.0), (23, 0.0), (30, -14.0)]:
    insert_rot("Tail_1", fr, 0, 0, sway * 0.4)
    insert_rot("Tail_2", fr, 0, 0, sway * 0.7)
    insert_rot("Tail_3", fr, 0, 0, sway * 1.0)
    insert_rot("Tail_4", fr, 0, 0, sway * 1.4)

# ----------------------------------------------------
# ACTION 3: LickPaw (90 frames) - Cat Sitting Grooming
# ----------------------------------------------------
act_lick = bpy.data.actions.new(name="LickPaw")
act_lick.use_fake_user = True
arm.animation_data.action = act_lick

# Rest pose at frame 1
insert_rot("Paw_L", 1, 0, 0, 0)
insert_loc("Paw_L", 1, 0, 0, 0)
insert_rot("Paw_R", 1, 0, 0, 0)
insert_rot("Haunch_L", 1, 0, 0, 0)
insert_rot("Haunch_R", 1, 0, 0, 0)
insert_rot("Head", 1, 0, 0, 0)
insert_rot("Ear_L", 1, 0, 0, 0)
insert_rot("Spine", 1, 0, 0, 0)
insert_loc("Spine", 1, 0, 0, 0)

# Raise Paw_L to chin / muzzle: frames 1..20
insert_rot("Paw_L", 20, 72.0, 10.0, 16.0)
insert_rot("Head", 20, 14.0, 0.0, -10.0)
insert_rot("Ear_L", 20, -12.0, 0.0, 0.0)
insert_rot("Spine", 20, 2.0, 0.0, 0.0)

# Grooming strokes 1..3
insert_rot("Paw_L", 28, 78.0, 12.0, 20.0)
insert_rot("Head", 28, 16.0, 0.0, -12.0)
insert_rot("Paw_L", 35, 70.0, 8.0, 14.0)
insert_rot("Head", 35, 13.0, 0.0, -9.0)

insert_rot("Paw_L", 43, 79.0, 14.0, 22.0)
insert_rot("Head", 43, 17.0, 0.0, -13.0)
insert_rot("Ear_L", 43, -15.0, 0.0, 0.0)
insert_rot("Paw_L", 50, 71.0, 8.0, 14.0)
insert_rot("Head", 50, 14.0, 0.0, -10.0)

insert_rot("Paw_L", 58, 77.0, 12.0, 18.0)
insert_rot("Head", 58, 15.0, 0.0, -11.0)
insert_rot("Paw_L", 65, 70.0, 6.0, 12.0)
insert_rot("Head", 65, 10.0, 0.0, -6.0)

# Return Paw_L to floor
insert_rot("Paw_L", 82, 0, 0, 0)
insert_rot("Head", 82, 0, 0, 0)
insert_rot("Ear_L", 82, 0, 0, 0)
insert_rot("Spine", 82, 0, 0, 0)

insert_rot("Paw_L", 90, 0, 0, 0)
insert_rot("Head", 90, 0, 0, 0)
insert_rot("Ear_L", 90, 0, 0, 0)

for fr, ang in [(1, 0), (25, 8.0), (45, -8.0), (65, 10.0), (90, 0)]:
    insert_rot("Tail_1", fr, 0, 0, ang * 0.4)
    insert_rot("Tail_2", fr, 0, 0, ang * 0.7)
    insert_rot("Tail_3", fr, 0, 0, ang * 1.0)
    insert_rot("Tail_4", fr, 0, 0, ang * 1.4)

# ----------------------------------------------------
# ACTION 4: Swat (30 frames) - Playful Wall Swat at Puck
# ----------------------------------------------------
act_swat = bpy.data.actions.new(name="Swat")
act_swat.use_fake_user = True
arm.animation_data.action = act_swat

# Frame 1: Stance ready
insert_rot("Paw_L", 1, 0, 0, 0)
insert_rot("Paw_R", 1, 0, 0, 0)
insert_rot("Head", 1, 0, 0, 0)
insert_rot("Ear_L", 1, 0, 0, 0)
insert_rot("Ear_R", 1, 0, 0, 0)
insert_rot("Spine", 1, 0, 0, 0)

# Frames 1..7: Alert coil back & head locks on puck
insert_rot("Head", 7, 12.0, 0, 8.0)
insert_rot("Ear_L", 7, 5.0, 0, 0)
insert_rot("Ear_R", 7, 5.0, 0, 0)
insert_rot("Paw_L", 7, -15.0, 0, -10.0) # paw draws back
insert_rot("Spine", 7, -3.0, 0, 0)

# Frames 7..14: Quick lightning swat forward against glass barrier
insert_rot("Paw_L", 14, 58.0, 15.0, 22.0) # paw strikes forward & high!
insert_rot("Head", 14, 15.0, 0, 12.0)
insert_rot("Spine", 14, 4.0, 0, 0)

# Frames 14..22: Follow-through recoil
insert_rot("Paw_L", 22, 25.0, 8.0, 10.0)
insert_rot("Head", 22, 8.0, 0, 4.0)

# Frames 22..30: Return to ready stance
insert_rot("Paw_L", 30, 0, 0, 0)
insert_rot("Paw_R", 30, 0, 0, 0)
insert_rot("Head", 30, 0, 0, 0)
insert_rot("Ear_L", 30, 0, 0, 0)
insert_rot("Ear_R", 30, 0, 0, 0)
insert_rot("Spine", 30, 0, 0, 0)

# Tail playful flick during swat
insert_rot("Tail_1", 1, 0, 0, 0)
insert_rot("Tail_1", 14, 0, 0, 25.0)
insert_rot("Tail_1", 30, 0, 0, 0)

# ----------------------------------------------------
# NLA Tracks
# ----------------------------------------------------
arm.animation_data.action = None

for name, act in [("Idle", act_idle), ("Walk", act_walk), ("LickPaw", act_lick), ("Swat", act_swat)]:
    tr = arm.animation_data.nla_tracks.new()
    tr.name = name
    tr.strips.new(name, 1, act)

# Export GLB
bpy.ops.export_scene.gltf(
    filepath=src_path,
    export_format='GLB',
    export_animations=True,
    export_animation_mode='NLA_TRACKS'
)

print(f"Exported {src_path} with 4 actions: Idle, Walk, LickPaw, Swat")
with open(src_path, 'rb') as f:
    f.read(12)
    chunk_len = int.from_bytes(f.read(4), 'little')
    f.read(4)
    data = json.loads(f.read(chunk_len).decode('utf-8'))
    print("Actions verified:", [a.get('name') for a in data.get('animations', [])])
