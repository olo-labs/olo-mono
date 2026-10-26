"""Build owned backend/worker images from clean fresh-install source checkouts."""
import argparse
import json
import pathlib
import subprocess

OWNER = 'io.olo.qualification.owner=olo-knowledge'


def build(source, image, dockerfile):
    source = source.resolve(strict=True)
    dirty = subprocess.check_output(['git', 'status', '--porcelain'], cwd=source, text=True)
    if dirty.strip():
        raise ValueError('Pipeline qualification requires a clean source checkout: ' + str(source))
    current = subprocess.run(['docker', 'image', 'inspect', image], capture_output=True, text=True)
    if current.returncode == 0:
        labels = json.loads(current.stdout)[0]['Config'].get('Labels') or {}
        if labels.get('io.olo.qualification.owner') != 'olo-knowledge':
            raise ValueError('Refusing to overwrite an unowned pipeline image: ' + image)
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=source, text=True).strip()
    subprocess.run(['docker', 'build', '--label', OWNER,
                    '--label', 'io.olo.qualification.source-revision=' + revision,
                    '-f', dockerfile, '-t', image, '.'], cwd=source, check=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--backend-source', type=pathlib.Path)
    parser.add_argument('--worker-source', type=pathlib.Path)
    args = parser.parse_args()
    if not args.backend_source and not args.worker_source:
        parser.error('Select at least one source checkout')
    if args.backend_source:
        build(args.backend_source, 'olo-backend:olo-qualification', 'olo/Dockerfile')
    if args.worker_source:
        build(args.worker_source, 'olo-worker:olo-qualification', 'olo-worker/Dockerfile')
