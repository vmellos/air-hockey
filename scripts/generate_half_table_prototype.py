import bpy
import os
import math

OUTPUT_MODELS_DIR = "/home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/assets/models"
OUTPUT_RENDER_DIR = "/home/vinicius/.gemini/antigravity-cli/brain/8604706b-0282-49d7-a48b-f431e9edc2f4"

os.makedirs(OUTPUT_MODELS_DIR, exist_ok=True)
os.makedirs(OUTPUT_RENDER_DIR, exist_ok=True)

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

def build_half_table(name, faction_color, is_p1=True):
    """
    Constrói a Meia-Mesa 3D com extremidade superior aberta para Mesa Estendida.
    Dimensões métricas calibradas: Largura = 1.08m, Comprimento da metade = 0.96m.
    Origem no centro da meia-mesa: X em [-0.54, 0.54], Y em [-0.48, +0.48].
    - Fundo (Y = -0.48): Meta, Barreira Laser e Protetores de Canto.
    - Topo (Y = +0.48): ABERTO (sem trilho), com arco neon semicircular de junção.
    """
    w = 1.08
    l_half = 0.96
    h_surface = 0.04
    rail_w = 0.075
    rail_h = 0.065
    goal_w = 0.38
    side_seg_w = (w - goal_w) / 2
    led_thick = 0.007
    led_h = 0.016

    mat_floor = create_pbr_material(f"{name}_Floor", (0.015, 0.022, 0.04, 1.0), metallic=0.60, roughness=0.18)
    mat_rails = create_pbr_material(f"{name}_Rail", (0.05, 0.06, 0.08, 1.0), metallic=0.92, roughness=0.22)
    mat_bumpers = create_pbr_material(f"{name}_Bumper", (0.03, 0.035, 0.045, 1.0), metallic=0.80, roughness=0.35)
    mat_neon = create_pbr_material(f"{name}_Neon", faction_color, emission_color=faction_color, emission_strength=5.5)
    mat_white_laser = create_pbr_material(f"{name}_Laser", (1.0, 0.98, 0.85, 1.0), emission_color=(1.0, 0.98, 0.85, 1.0), emission_strength=4.8)

    # 1. Piso da Meia-Mesa
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, -h_surface / 2))
    floor = bpy.context.active_object
    floor.name = f"{name}_Surface"
    floor.scale = (w, l_half, h_surface)
    bpy.ops.object.transform_apply(scale=True)
    add_bevel(floor, width=0.004, segments=4)
    floor.data.materials.append(mat_floor)

    # 2. Semicírculo Central Neon no Topo (Y = +0.48)
    top_y = l_half / 2
    arc_outer = create_semicircle_mesh(f"{name}_CenterArcOuter", 0, top_y, 0.0015, r_out=0.18, r_in=0.165)
    arc_outer.data.materials.append(mat_white_laser)

    # 3. Faixa de Junção Laser no Topo (Eletro-borda de passagem contínua)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, top_y - 0.003, 0.0015))
    top_edge_laser = bpy.context.active_object
    top_edge_laser.name = f"{name}_TopEdgeLaser"
    top_edge_laser.scale = (w * 0.98, 0.006, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    top_edge_laser.data.materials.append(mat_white_laser)

    # 4. Área da Meta Neon na Base (Y = -0.48 + 0.32 = -0.16)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -l_half/2 + 0.32, 0.0015))
    crease = bpy.context.active_object
    crease.name = f"{name}_Crease"
    crease.scale = (goal_w * 1.5, 0.008, 0.002)
    bpy.ops.object.transform_apply(scale=True)
    crease.data.materials.append(mat_neon)

    # 5. Trilhos Laterais (Estendem-se do fundo Y = -0.555 até o topo Y = +0.48)
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

def setup_render_lighting(is_zenital=False):
    # Luz Principal Key
    light_data = bpy.data.lights.new(name="KeyLight", type='AREA')
    light_data.energy = 80.0
    light_data.size = 1.2
    light_obj = bpy.data.objects.new(name="KeyLight", object_data=light_data)
    bpy.context.collection.objects.link(light_obj)
    light_obj.location = (0, 0, 1.8)
    
    # Luz de Preenchimento Suave
    fill_data = bpy.data.lights.new(name="FillLight", type='POINT')
    fill_data.energy = 30.0
    fill_obj = bpy.data.objects.new(name="FillLight", object_data=fill_data)
    bpy.context.collection.objects.link(fill_obj)
    fill_obj.location = (0.8, -0.6, 1.2)

def render_image(filepath, cam_loc, cam_rot_euler, res_x=1080, res_y=1920, is_ortho=False, ortho_scale=1.4):
    scene = bpy.context.scene
    scene.render.engine = 'CYCLES'
    scene.cycles.samples = 32
    scene.render.resolution_x = res_x
    scene.render.resolution_y = res_y
    scene.render.image_settings.file_format = 'PNG'
    scene.render.filepath = filepath

    cam_data = bpy.data.cameras.new("RenderCamera")
    if is_ortho:
        cam_data.type = 'ORTHO'
        cam_data.ortho_scale = ortho_scale
    else:
        cam_data.type = 'PERSP'
        cam_data.lens = 38

    cam_obj = bpy.data.objects.new("RenderCamera", cam_data)
    bpy.context.collection.objects.link(cam_obj)
    cam_obj.location = cam_loc
    cam_obj.rotation_euler = cam_rot_euler
    scene.camera = cam_obj

    bpy.ops.render.render(write_still=True)
    print(">>> Rendered:", filepath)
    
    bpy.data.objects.remove(cam_obj)
    bpy.data.cameras.remove(cam_data)

def main():
    p1_color = (0.0, 0.92, 1.0, 1.0)
    p2_color = (1.0, 0.40, 0.05, 1.0)

    # 1. Gerar e Exportar Meia-Mesa P1 (Anfitrião)
    print("=== Gerando Meia-Mesa P1 ===")
    reset_scene()
    build_half_table("MesaArcadeHalfP1", p1_color, is_p1=True)
    export_glb("mesa_arcade_half_p1.glb")

    # 2. Gerar e Exportar Meia-Mesa P2 (Convidado)
    print("=== Gerando Meia-Mesa P2 ===")
    reset_scene()
    build_half_table("MesaArcadeHalfP2", p2_color, is_p1=False)
    export_glb("mesa_arcade_half_p2.glb")

    # 3. Render 1: Perspectiva 3D Oblíqua (Meia-Mesa P1)
    print("=== Renderizando Perspectiva 3D ===")
    setup_render_lighting(is_zenital=False)
    render_image(
        filepath=os.path.join(OUTPUT_RENDER_DIR, "render_half_table_perspective.png"),
        cam_loc=(0.0, -1.35, 1.15),
        cam_rot_euler=(math.radians(52), 0, 0),
        res_x=1080,
        res_y=1920
    )

    # 4. Render 2: Visão Zenital Top-Down (Simulando a tela do smartphone na Mesa Estendida)
    print("=== Renderizando Visão Zenital 90° ===")
    render_image(
        filepath=os.path.join(OUTPUT_RENDER_DIR, "render_half_table_zenital.png"),
        cam_loc=(0.0, 0.0, 2.2),
        cam_rot_euler=(0, 0, 0),
        res_x=1080,
        res_y=1920,
        is_ortho=True,
        ortho_scale=1.35
    )

    # 5. Render 3: Simulação Dual-Phone (Dois Celulares Topo-a-Topo no Vão Físico)
    print("=== Renderizando Simulação Dual-Phone ===")
    reset_scene()
    setup_render_lighting()
    
    # Anfitrião P1 (Metade Inferior)
    build_half_table("HostP1", p1_color, is_p1=True)
    # Desloca 5mm para baixo para simular a moldura física do vão
    for obj in bpy.context.collection.objects:
        obj.location.y -= 0.005

    # Convidado P2 (Metade Superior, rotacionado 180° e colocado acima do vão)
    # Criamos a coleção ou instanciamos
    # Para construir P2 acima:
    build_half_table("GuestP2", p2_color, is_p1=False)
    # Todos os objetos com prefixo GuestP2: rotacionar 180 e deslocar
    for obj in bpy.context.collection.objects:
        if obj.name.startswith("GuestP2"):
            # Rotaciona 180 ao redor de Z e translada
            obj.rotation_euler.z += math.pi
            obj.location.y += 0.005
            # Inverte coordenadas Y
            obj.location.y = -obj.location.y + 0.005

    # Render Dual-Phone na vertical
    render_image(
        filepath=os.path.join(OUTPUT_RENDER_DIR, "render_mesa_estendida_joined.png"),
        cam_loc=(0.0, 0.0, 3.8),
        cam_rot_euler=(0, 0, 0),
        res_x=1080,
        res_y=1920,
        is_ortho=True,
        ortho_scale=2.4
    )

    print(">>> TODAS AS MALHAS E RENDERS FORAM GERADOS COM SUCESSO!")

if __name__ == "__main__":
    main()
