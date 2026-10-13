#!/usr/bin/env python3
"""Format an already-designed sprite as a real 16x16, six-color Minecraft PNG.

Requires Pillow for artwork preparation only; the plugin and ZIP setup do not.
"""
import sys
from pathlib import Path
from PIL import Image

def format_texture(source, destination):
    sprite=Image.open(source).convert('RGBA').resize((16,16),Image.Resampling.NEAREST)
    pixels=[sprite.getpixel((x,y)) for y in range(16) for x in range(16)]
    opaque=[rgb[:3] for rgb in pixels if rgb[3]>=128]
    if not opaque: raise ValueError('Sprite contains no opaque pixels')
    colors=Image.new('RGB',(len(opaque),1));colors.putdata(opaque)
    palette=colors.quantize(colors=6,method=Image.Quantize.FASTOCTREE,dither=Image.Dither.NONE).convert('RGB')
    reduced=iter(palette.getpixel((x,0)) for x in range(len(opaque)))
    result=Image.new('RGBA',(16,16))
    formatted=[(*next(reduced),255) if rgba[3]>=128 else (0,0,0,0) for rgba in pixels]
    result.putdata(formatted)
    destination=Path(destination);destination.parent.mkdir(parents=True,exist_ok=True);result.save(destination)
    assert result.size==(16,16)
    assert len({p[:3] for p in formatted if p[3]})<=6
    assert {p[3] for p in formatted}=={0,255}

if __name__=='__main__':
    if len(sys.argv)!=3: raise SystemExit('Usage: format_item_texture.py SOURCE.png DESTINATION.png')
    format_texture(sys.argv[1],sys.argv[2])
