#!/usr/bin/env python3
"""Build an original static voxel boss model. Uses only vanilla texture references."""
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
        write(f'assets/mobarmy/models/item/{item}.json', {'parent': ('minecraft:item/bow' if item.endswith('_bow') else 'minecraft:item/handheld' if item.endswith(('_blade','_sword','_axe','_mace','_pickaxe','_shovel','_hoe')) else 'minecraft:item/generated'), 'textures': {'layer0': f'mobarmy:item/{item}'}})
    elements = [
        box([4, 2, 5], [7.5, 10, 11], 'stone'), box([8.5, 2, 5], [12, 10, 11], 'stone'),
        box([3, 10, 4], [13, 21, 12], 'armor'), box([5, 21, 5], [11, 27, 11], 'stone'),
        box([0, 12, 5], [3, 21, 11], 'stone'), box([13, 12, 5], [16, 21, 11], 'stone'),
        box([5.7, 23, 4.8], [7.3, 24.5, 5], 'eyes'), box([8.7, 23, 4.8], [10.3, 24.5, 5], 'eyes'),
        box([6, 13, 3.5], [10, 17, 4], 'eyes'),
        box([4, 26, 5], [6, 30, 7], 'crystal'), box([10, 26, 5], [12, 30, 7], 'crystal'),
        box([-1, 20, 4], [4, 23, 12], 'crystal'), box([12, 20, 4], [17, 23, 12], 'crystal'),
    ]
    write('assets/mobarmy/models/item/chaos_guardian.json', {
        'textures': {'stone': 'minecraft:block/obsidian', 'armor': 'minecraft:block/purple_concrete', 'crystal': 'minecraft:block/amethyst_block', 'eyes': 'minecraft:block/sea_lantern', 'particle': 'minecraft:block/amethyst_block'},
        'elements': elements, 'display': {'fixed': {'rotation': [0, 0, 0], 'translation': [0, -8, 0], 'scale': [1, 1, 1]}}
    })
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
