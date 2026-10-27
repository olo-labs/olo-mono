# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Build GitHub-compatible animated tours from the original, unmodified browser captures."""
import hashlib
import json
import pathlib
import textwrap
from PIL import Image, ImageDraw, ImageFont, ImageStat

ROOT=pathlib.Path(__file__).resolve().parent


def main():
    manifest=json.loads((ROOT/'screenshots.json').read_text(encoding='utf-8'))
    try:font=ImageFont.truetype('DejaVuSans.ttf',20)
    except OSError:font=ImageFont.load_default(size=20)
    for group,name in [('chat','olo-chat-tour.gif'),('studio','olo-ui-tour.gif')]:
        entries=[entry for entry in manifest['screens'] if entry['group']==group]
        if not entries:raise ValueError('No captures for '+group)
        frames=[]
        for index,entry in enumerate(entries,1):
            path=ROOT/entry['file']
            if hashlib.sha256(path.read_bytes()).hexdigest()!=entry['sha256']:
                raise ValueError('Capture checksum mismatch: '+entry['file'])
            with Image.open(path) as original:
                image=original.convert('RGB')
                if max(ImageStat.Stat(image).stddev)<3:
                    raise ValueError('Blank or unrendered capture: '+entry['file'])
                image.thumbnail((1280,720),Image.Resampling.LANCZOS)
                frame=Image.new('RGB',(1280,776),'#0f172a')
                frame.paste(image,((1280-image.width)//2,(720-image.height)//2))
                draw=ImageDraw.Draw(frame)
                caption=f"{index:02}/{len(entries):02}  {entry['title']}"
                draw.multiline_text((22,725),'\n'.join(textwrap.wrap(caption,95)),font=font,spacing=2,fill='#eef4fa')
                frames.append(frame.quantize(colors=160,method=Image.Quantize.MEDIANCUT))
        frames[0].save(ROOT/name,save_all=True,append_images=frames[1:],duration=3500,loop=0,optimize=False)
        print(f'{name}: {len(frames)} frames, {(ROOT/name).stat().st_size:,} bytes')


if __name__=='__main__':main()
