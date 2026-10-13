#!/usr/bin/env python3
"""Package a compiled plugin, source, resource pack and Windows localhost launcher."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib
import json
import sys
from build_pack import build

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / 'target/DurcheinanderMobArmy-1.6.0-local-test.zip'
JAR = ROOT / 'target/DurcheinanderMobArmy-1.6.0.jar'
PACK = ROOT / 'target/MobArmy-resource-pack-26.3.zip'
build()  # Recreate the ZIP after Maven clean; model assets were generated before compilation.
if not JAR.is_file() or not PACK.is_file(): raise SystemExit('Run mvn verify and python3 tools/build_pack.py first.')
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
with ZipFile(OUTPUT, 'w', ZIP_DEFLATED) as archive:
    archive.write(JAR, 'plugin/' + JAR.name)
    archive.write(PACK, 'resource-pack/' + PACK.name)
    for name in ['README.md', 'TESTING.md', 'ITEMS.md','EQUIPMENT.md','RITUALS.md']:
        archive.write(ROOT / name, name)
    archive.write(ROOT / 'docs/item-textures-16px.png', 'item-textures-16px.png')
    for file in sorted((ROOT / 'local-test').glob('*')):
        if file.is_file(): archive.write(file, 'local-test/' + file.name)
    for file in sorted(ROOT.rglob('*')):
        relative = file.relative_to(ROOT)
        if not file.is_file() or relative.parts[0] in {'.git', 'target'} or any(p in {'server', 'buildtools', 'runtime', '__pycache__'} for p in relative.parts): continue
        archive.write(file, 'source/' + str(relative))
    manifest = {'version': '1.6.0', 'minecraft': '26.3', 'java': 25, 'sha256': {JAR.name: hashlib.sha256(JAR.read_bytes()).hexdigest(), PACK.name: hashlib.sha256(PACK.read_bytes()).hexdigest()}}
    archive.writestr('checksums.json', json.dumps(manifest, indent=2) + '\n')
with ZipFile(OUTPUT) as archive:
    assert archive.testzip() is None
    assert archive.read('plugin/' + JAR.name) == JAR.read_bytes()
print(OUTPUT)
