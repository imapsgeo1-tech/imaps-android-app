#!/usr/bin/env python3
"""Precompute the actual original IMAPS logo's reveal layers during CI.

Each pixel of the three transparent layers is taken from the original 500px
transparent company PNG. No recoloring/redrawing of the brand is performed.
Perform this only at build time, NEVER at app startup.
"""
from pathlib import Path
from PIL import Image, ImageFilter

assets = Path("app/src/main/res/drawable-nodpi")
source = Image.open(assets / "imaps_full_logo.png").convert("RGBA")
w, h = source.size
assert min(w, h) >= 290, (w, h)
cx, cy = w * 0.50, h * 0.50
r2 = (min(w, h) * 0.345) ** 2

px = source.load()
navy_mask = Image.new("L", (w, h))
mask_px = navy_mask.load()
for y in range(h):
    for x in range(w):
        R,G,B,A = px[x,y]
        if (x-cx)**2 + (y-cy)**2 < r2 and A >= 30 and B > R and B > G * .83 and R < 105:
            mask_px[x,y] = 255

# Keep authentic thin gold outline surrounding the original navy Iran silhouette.
ring = navy_mask.filter(ImageFilter.MaxFilter(7))
ring_px = ring.load()
iran = Image.new("RGBA", (w,h))
globe = Image.new("RGBA", (w,h))
letters = Image.new("RGBA", (w,h))
out1, out2, out3 = iran.load(), globe.load(), letters.load()
count = [0,0,0]
for y in range(h):
    for x in range(w):
        rgba = px[x,y]
        if rgba[3] == 0:
            continue
        d2 = (x-cx)**2 + (y-cy)**2
        if ring_px[x,y] and d2 < r2:
            out1[x,y] = rgba
            count[0] += 1
        elif d2 < r2:
            out2[x,y] = rgba
            count[1] += 1
        else:
            out3[x,y] = rgba
            count[2] += 1

for name, im in (
    ("imaps_intro_iran.png", iran),
    ("imaps_intro_globe.png", globe),
    ("imaps_intro_letters.png", letters),
):
    im.save(assets / name, optimize=True, compress_level=9)

# Ensure nothing from the owner's original mark was dropped during separation.
merged = Image.new("RGBA", source.size)
for im in (iran, globe, letters):
    merged.alpha_composite(im)
if merged.tobytes() != source.tobytes():
    raise RuntimeError("Brand layers do not exactly reassemble original iMaps artwork")
if min(count) < 1000:
    raise RuntimeError(f"Missing logo component: {count}")
print(f"Authentic IMAPS logo layers: {w}x{h}, pixel counts {count}")
