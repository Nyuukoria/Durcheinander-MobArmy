#!/usr/bin/env python3
"""Build original animated golem bone models. Uses only vanilla texture references."""
import json
import struct
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'resource-pack'

def write(path, data):
    target = PACK / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def box(a, b, texture):
    return {'from': a, 'to': b, 'faces': {face: {'texture': '#' + texture, 'uv': [0, 0, 16, 16]} for face in ['north', 'south', 'east', 'west', 'up', 'down']}}

def build():
    write('pack.mcmeta', {'pack': {'description': 'Yuuki • MobArmy 26.3 • Chaos Guardian', 'min_format': [97, 1], 'max_format': [97, 1]}})
    items=sorted(path.stem for path in (PACK/'assets/mobarmy/textures/item').glob('*.png'))
    for item in items+['chaos_guardian']:
        model=f'mobarmy:item/{item}'
        write(f'assets/mobarmy/items/{item}.json', {'model': {'type': 'minecraft:model', 'model': model}})
    for item in items:
        texture = PACK / f'assets/mobarmy/textures/item/{item}.png'
        if not texture.is_file(): raise SystemExit(f'Missing item texture: {texture}')
        data=texture.read_bytes()
        if data[:8]!=b'\x89PNG\r\n\x1a\n' or struct.unpack('>II',data[16:24])!=(16,16): raise SystemExit(f'Item texture must be a real 16x16 PNG: {texture}')
        parent = 'minecraft:item/handheld_rod' if item=='angler_rod' else 'minecraft:item/crossbow' if item=='ranger_crossbow' else 'minecraft:item/bow' if item.endswith('_bow') else 'minecraft:item/handheld' if item.endswith(('_blade','_sword','_axe','_mace','_pickaxe','_shovel','_hoe','_shears','_trident')) else 'minecraft:item/generated'
        write(f'assets/mobarmy/models/item/{item}.json', {'parent': parent, 'textures': {'layer0': f'mobarmy:item/{item}'}})
    def item_model(name):
        return {'type':'minecraft:model','model':'mobarmy:item/'+name}
    # Material-specific states keep our own textures while ordinary game mechanics run unchanged.
    write('assets/mobarmy/models/item/angler_rod_cast.json',{'parent':'mobarmy:item/angler_rod','display':{'firstperson_righthand':{'rotation':[0,90,25],'translation':[0,1,0],'scale':[.68,.68,.68]},'firstperson_lefthand':{'rotation':[0,-90,-25],'translation':[0,1,0],'scale':[.68,.68,.68]}}})
    write('assets/mobarmy/items/angler_rod.json',{'model':{'type':'minecraft:condition','property':'minecraft:fishing_rod/cast','on_true':item_model('angler_rod_cast'),'on_false':item_model('angler_rod')}})
    for state,tilt in [('pulling_0',-5),('pulling_1',-10),('pulling_2',-15),('arrow',0),('rocket',0)]:
        write(f'assets/mobarmy/models/item/ranger_crossbow_{state}.json',{'parent':'mobarmy:item/ranger_crossbow','display':{'firstperson_righthand':{'rotation':[0,-90,tilt],'translation':[1.13,3.2,1.13],'scale':[.68,.68,.68]},'firstperson_lefthand':{'rotation':[0,90,-tilt],'translation':[1.13,3.2,1.13],'scale':[.68,.68,.68]}}})
    idle={'type':'minecraft:select','property':'minecraft:charge_type','cases':[{'when':state,'model':item_model('ranger_crossbow_'+state)} for state in ['arrow','rocket']],'fallback':item_model('ranger_crossbow')}
    pulling={'type':'minecraft:range_dispatch','property':'minecraft:crossbow/pull','entries':[{'threshold':threshold,'model':item_model('ranger_crossbow_pulling_'+str(i))} for i,threshold in enumerate([0,.58,1])],'fallback':item_model('ranger_crossbow')}
    write('assets/mobarmy/items/ranger_crossbow.json',{'model':{'type':'minecraft:condition','property':'minecraft:using_item','on_true':pulling,'on_false':idle}})
    for bow in ['tempest_bow','star_bow']:
        for step in range(3):
            write(f'assets/mobarmy/models/item/{bow}_pulling_{step}.json',{'parent':f'mobarmy:item/{bow}','display':{'firstperson_righthand':{'rotation':[0,-90,-(5+step*5)],'translation':[1.13,3.2,1.13],'scale':[.68,.68,.68]},'firstperson_lefthand':{'rotation':[0,90,5+step*5],'translation':[1.13,3.2,1.13],'scale':[.68,.68,.68]}}})
        charge={'type':'minecraft:range_dispatch','property':'minecraft:use_duration','scale':.05,'entries':[{'threshold':threshold,'model':item_model(f'{bow}_pulling_{step}')} for step,threshold in enumerate([0,.65,.9])],'fallback':item_model(bow)}
        write(f'assets/mobarmy/items/{bow}.json',{'model':{'type':'minecraft:condition','property':'minecraft:using_item','on_true':charge,'on_false':item_model(bow)}})
    utility=['silk_pickaxe','forester_axe','pathfinder_shovel','cultivator_hoe','harvest_shears','angler_rod','ranger_crossbow','returning_trident','warden_blade','gale_axe','abyss_sword','abyss_mace','tempest_bow','star_bow','echo_pickaxe','runic_shovel','harvest_hoe']
    # A separate GUI meter leaves native fishing/shooting/stripping usable during skill cooldowns.
    # Quads reference vanilla flat block colors; no extra or softened sprite pixels are introduced.
    for step in range(8):
        def face(x0,x1,y0,y1,z,texture):
            return {'from':[x0,y0,7.3],'to':[x1,y1,8.7],'faces':{side:{'texture':'#'+texture,'uv':[0,0,16,16]} for side in ['north','south']}}
        elements=[face(1,15,1,3,9,'dark')]
        if step:elements.append(face(1,1+int(14*step/8),1,2,9.1,'ready'))
        write(f'assets/mobarmy/models/item/cooldown/meter_{step}.json',{'gui_light':'front','textures':{'dark':'minecraft:block/black_concrete','ready':'minecraft:block/lime_concrete','particle':'minecraft:block/lime_concrete'},'elements':elements})
    for item in utility:
        target=PACK/f'assets/mobarmy/items/{item}.json';native=json.loads(target.read_text())['model']
        meters={'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'entries':[{'threshold':step/8,'model':{'type':'minecraft:composite','models':[native,item_model(f'cooldown/meter_{step}')]}} for step in range(8)],'fallback':native}
        active={'type':'minecraft:condition','property':'minecraft:custom_model_data','index':0,'on_true':meters,'on_false':native}
        write(f'assets/mobarmy/items/{item}.json',{'model':{'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':'gui','model':active}],'fallback':native}})
    parts = {
        'body': [box([3,0,4],[13,16,12],'stone'),box([4,2,3.5],[12,15,4],'armor'),box([6,6,3.1],[10,10,3.5],'eyes'),box([1,13,3],[5,17,13],'crystal'),box([11,13,3],[15,17,13],'crystal')],
        'head': [box([3.5,3.5,3.5],[12.5,12.5,12.5],'stone'),box([4.7,8.2,3.1],[6.7,9.7,3.5],'eyes'),box([9.3,8.2,3.1],[11.3,9.7,3.5],'eyes'),box([6,4.3,3.2],[10,6,3.5],'armor')],
        'left_arm': [box([5,-12,4],[11,8,12],'stone'),box([4.7,-12.2,3.7],[11.3,-8,12.3],'armor')],
        'right_arm': [box([5,-12,4],[11,8,12],'stone'),box([4.7,-12.2,3.7],[11.3,-8,12.3],'armor')],
        'left_leg': [box([5,-10,4],[11,8,12],'stone'),box([4.5,-10,2.5],[11.5,-7,12.5],'armor')],
        'right_leg': [box([5,-10,4],[11,8,12],'stone'),box([4.5,-10,2.5],[11.5,-7,12.5],'armor')],
    }
    pivots={'body':(0,1.65,0),'head':(0,2.44,0),'left_arm':(.65,2.08,0),'right_arm':(-.65,2.08,0),'left_leg':(.24,1.125,0),'right_leg':(-.24,1.125,0)}
    for variant,plate,crystal,eyes in [('abyss','purple_concrete','amethyst_block','sea_lantern'),('frost','cyan_concrete','blue_ice','sea_lantern'),('ember','orange_concrete','magma_block','shroomlight')]:
        textures={'stone':'minecraft:block/obsidian','armor':'minecraft:block/'+plate,'crystal':'minecraft:block/'+crystal,'eyes':'minecraft:block/'+eyes,'particle':'minecraft:block/obsidian'}
        whole=[]
        for part,elements in parts.items():
            name=f'golem/{variant}/{part}'
            write(f'assets/mobarmy/models/item/{name}.json',{'textures':textures,'elements':elements,'display':{'none':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]}}})
            write(f'assets/mobarmy/items/{name}.json',{'model':{'type':'minecraft:model','model':f'mobarmy:item/{name}'}})
            # Keep the legacy whole-model item within Minecraft's [-16,32] geometry limits.
            for element in elements:
                clone=json.loads(json.dumps(element))
                for bound in ('from','to'):clone[bound]=[(v-8)/2+pivots[part][axis]*8+8 for axis,v in enumerate(element[bound])]
                whole.append(clone)
        legacy='chaos_guardian' if variant=='abyss' else f'{variant}_guardian'
        write(f'assets/mobarmy/models/item/{legacy}.json',{'textures':textures,'elements':whole,'display':{'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[2,2,2]}}})
        write(f'assets/mobarmy/items/{legacy}.json',{'model':{'type':'minecraft:model','model':f'mobarmy:item/{legacy}'}})
    paths=[path for path in sorted(PACK.rglob('*')) if path.is_file() and path.name!='index.txt']
    (PACK/'index.txt').write_text(''.join(str(path.relative_to(PACK))+'\n' for path in paths),encoding='utf-8')
    output = ROOT / 'target/MobArmy-resource-pack-26.3.zip'
    output.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(output, 'w', ZIP_DEFLATED) as archive:
        for path in paths:
            if path.is_file():
                if path.suffix == '.json' or path.name == 'pack.mcmeta': json.loads(path.read_text(encoding='utf-8'))
                archive.write(path, path.relative_to(PACK))
    print(output)

if __name__ == '__main__': build()
