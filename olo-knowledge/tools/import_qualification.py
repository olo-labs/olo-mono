"""Import a checksum-verified CI image under an exclusively owned fixture tag."""
import argparse
import hashlib
import io
import json
import pathlib
import subprocess
import tarfile

TAG = 'olo-toolgate-quickstart:olo-m8'
LABEL = 'io.olo.qualification.owner'
OWNER = 'olo-knowledge'


def verified_archive(path, checksums):
    matches = [line.split()[0] for line in checksums.read_text(encoding='utf-8').splitlines()
               if len(line.split()) == 2 and line.split()[1].lstrip('*') == path.name]
    if len(matches) != 1:
        raise ValueError('Expected exactly one image checksum')
    with path.open('rb') as stream:
        actual = hashlib.file_digest(stream, 'sha256').hexdigest()
    if actual != matches[0]:
        raise ValueError('CI image checksum mismatch')
    return actual


def write_owned_archive(source, destination):
    """Stream layers unchanged; replace only image config, labels and repository tag.

    Emit the Docker archive format without the source OCI index, which otherwise
    could cause Docker to load the original repository tag instead.
    """
    manifests = json.load(source.extractfile('manifest.json'))
    if len(manifests) != 1:
        raise ValueError('Expected a single Quickstart image')
    original = manifests[0]
    config = json.load(source.extractfile(original['Config']))
    config.setdefault('config', {}).setdefault('Labels', {})
    if config['config']['Labels'] is None:
        config['config']['Labels'] = {}
    config['config']['Labels'][LABEL] = OWNER
    config_bytes = json.dumps(config, separators=(',', ':')).encode()
    config_name = hashlib.sha256(config_bytes).hexdigest() + '.json'
    with tarfile.open(fileobj=destination, mode='w|') as output:
        def add(name, value):
            entry = tarfile.TarInfo(name)
            entry.size = len(value)
            output.addfile(entry, io.BytesIO(value))

        add(config_name, config_bytes)
        for name in dict.fromkeys(original['Layers']):
            member = source.getmember(name)
            if not member.isfile():
                raise ValueError('Image layer must be a regular file')
            output.addfile(member, source.extractfile(member))
        add('manifest.json', json.dumps([dict(Config=config_name, RepoTags=[TAG],
                                            Layers=original['Layers'])]).encode())


def import_image(path, checksums):
    digest = verified_archive(path, checksums)
    existing = subprocess.run(['docker', 'image', 'inspect', TAG], capture_output=True, text=True)
    if existing.returncode == 0:
        labels = json.loads(existing.stdout)[0]['Config'].get('Labels') or {}
        if labels.get(LABEL) != OWNER:
            raise ValueError('Refusing to overwrite an unowned qualification image')
    loader = subprocess.Popen(['docker', 'load'], stdin=subprocess.PIPE)
    try:
        with tarfile.open(path) as source:
            write_owned_archive(source, loader.stdin)
        loader.stdin.close()
        if loader.wait() != 0:
            raise RuntimeError('Docker image import failed')
    except BaseException:
        loader.stdin.close()
        if loader.poll() is None:
            loader.terminate()
        loader.wait()
        raise
    print('Imported owned qualification image; source archive sha256:' + digest)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('archive', type=pathlib.Path)
    parser.add_argument('--checksums', type=pathlib.Path)
    args = parser.parse_args()
    import_image(args.archive, args.checksums or args.archive.with_name('SHA256SUMS'))
