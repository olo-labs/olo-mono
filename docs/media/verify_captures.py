# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Verify screenshot integrity and the documented page/state coverage without running a browser."""
import argparse
import hashlib
import json
import pathlib
from urllib.parse import urlsplit


def verify(root,integrity_only=False):
    manifest=json.loads((root/'screenshots.json').read_text(encoding='utf-8'))
    screens=manifest['screens']
    files=set();routes=set()
    for screen in screens:
        path=(root/screen['file']).resolve()
        if not path.is_relative_to(root.resolve()) or path.suffix!='.jpg':
            raise ValueError('Invalid screenshot path')
        if screen['file'] in files:raise ValueError('Duplicate screenshot: '+screen['file'])
        files.add(screen['file'])
        if hashlib.sha256(path.read_bytes()).hexdigest()!=screen['sha256']:
            raise ValueError('Screenshot checksum mismatch: '+screen['file'])
        url=urlsplit(screen['url'])
        routes.add(url.path+('#'+url.fragment.split('?',1)[0] if url.fragment else ''))
    missing=set(manifest['expectedRoutes'])-routes
    missing_states=set(manifest.get('requiredScreens',[]))-files
    if (missing or missing_states) and not integrity_only:raise ValueError('Missing coverage: '+str(sorted(missing|missing_states)))
    print(f"Verified {len(screens)} capture checksums; missing routes: {len(missing)}; missing required states: {len(missing_states)}.")


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('root',type=pathlib.Path,nargs='?',default=pathlib.Path(__file__).resolve().parent)
    parser.add_argument('--integrity-only',action='store_true',help='Check files and hashes without claiming complete capture coverage')
    args=parser.parse_args()
    verify(args.root,args.integrity_only)
