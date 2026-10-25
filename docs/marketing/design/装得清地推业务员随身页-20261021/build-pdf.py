#!/usr/bin/env python3
"""Build a two-page A4 PDF from the salesperson field-sheet renders."""
from pathlib import Path
from PIL import Image
ROOT = Path(__file__).resolve().parent
FRONT = ROOT / '装得清地推业务员随身页-正面-300dpi.png'
BACK = ROOT / '装得清地推业务员随身页-背面-300dpi.png'
TARGET = ROOT / '装得清地推业务员随身页-双面A4.pdf'
for image_path in (FRONT, BACK):
    if not image_path.exists():
        raise SystemExit(f'missing print image: {image_path}')
with Image.open(FRONT) as front, Image.open(BACK) as back:
    front = front.convert('RGB')
    back = back.convert('RGB')
    for image, label in ((front, 'front'), (back, 'back')):
        if image.size != (2480, 3508):
            raise SystemExit(f'unexpected {label} image size: {image.size}; expected 2480x3508')
    front.save(
        TARGET,
        'PDF',
        resolution=300.0,
        quality=95,
        save_all=True,
        append_images=[back],
    )
print(f'created {TARGET}')
