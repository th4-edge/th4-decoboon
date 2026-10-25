#!/usr/bin/env python3
"""Build a one-page A4 PDF from the 300dpi product-sheet render."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / '装得清地推产品页-A4-300dpi.png'
TARGET = ROOT / '装得清地推产品页-A4.pdf'

if not SOURCE.exists():
    raise SystemExit(f'missing print image: {SOURCE}')

with Image.open(SOURCE) as image:
    image = image.convert('RGB')
    if image.size != (2480, 3508):
        raise SystemExit(f'unexpected image size: {image.size}; expected 2480x3508')
    image.save(TARGET, 'PDF', resolution=300.0, quality=95)

print(f'created {TARGET}')
