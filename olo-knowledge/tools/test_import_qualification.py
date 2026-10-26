"""Guard fixture ownership, archive identity and checksum enforcement."""
import hashlib
import io
import json
import pathlib
import tarfile
import tempfile
import unittest
from unittest.mock import patch

import import_qualification as importer
import build_qualification as builder
import build_pipeline_qualification as pipeline_builder


class ImportQualificationTest(unittest.TestCase):
    def test_pipeline_dirty_source_never_reaches_docker(self):
        with patch.object(pipeline_builder.subprocess, 'check_output', return_value=' M Dockerfile\n'), \
             patch.object(pipeline_builder.subprocess, 'run') as docker:
            with self.assertRaisesRegex(ValueError, 'clean source checkout'):
                pipeline_builder.build(pathlib.Path(__file__).parent,'fixture:test','Dockerfile')
            docker.assert_not_called()

    def test_pipeline_build_cannot_overwrite_unowned_application(self):
        with patch.object(pipeline_builder.subprocess, 'check_output', return_value=''), \
             patch.object(pipeline_builder.subprocess, 'run') as docker:
            docker.return_value.returncode=0
            docker.return_value.stdout=json.dumps([{'Config':{'Labels':{}}}])
            with self.assertRaisesRegex(ValueError, 'unowned pipeline image'):
                pipeline_builder.build(pathlib.Path(__file__).parent,'production:untouched','Dockerfile')
            self.assertEqual(1,docker.call_count)
            self.assertEqual(['docker','image','inspect','production:untouched'],docker.call_args.args[0])

    def test_dirty_source_is_rejected_before_any_build(self):
        with patch.object(builder.subprocess, 'check_output', return_value=' M VERSION\n'), \
             patch.object(builder.subprocess, 'run') as docker:
            with self.assertRaisesRegex(ValueError, 'clean ToolGate'):
                builder.build(pathlib.Path(__file__).parent)
            docker.assert_not_called()

    def test_import_replaces_original_tag_and_preserves_layers(self):
        original = io.BytesIO()
        layer = b'fixture filesystem layer'
        with tarfile.open(fileobj=original, mode='w') as archive:
            values = {'config.json': json.dumps({'config': {'Labels': None}, 'rootfs': {
                'type': 'layers', 'diff_ids': ['sha256:' + hashlib.sha256(layer).hexdigest()]}}).encode(),
                'layer.tar': layer,
                'manifest.json': json.dumps([{'Config': 'config.json',
                    'RepoTags': ['production:do-not-overwrite'], 'Layers': ['layer.tar']}]).encode(),
                'index.json': b'original OCI index must not be copied'}
            for name, data in values.items():
                entry = tarfile.TarInfo(name)
                entry.size = len(data)
                archive.addfile(entry, io.BytesIO(data))
        original.seek(0)
        rewritten = io.BytesIO()
        with tarfile.open(fileobj=original) as source:
            importer.write_owned_archive(source, rewritten)
        rewritten.seek(0)
        with tarfile.open(fileobj=rewritten) as result:
            manifest = json.load(result.extractfile('manifest.json'))[0]
            self.assertEqual(manifest['RepoTags'], [importer.TAG])
            self.assertNotIn('index.json', result.getnames())
            self.assertEqual(result.extractfile('layer.tar').read(), layer)
            config_bytes = result.extractfile(manifest['Config']).read()
            self.assertEqual(manifest['Config'], hashlib.sha256(config_bytes).hexdigest() + '.json')
            self.assertEqual(json.loads(config_bytes)['config']['Labels'][importer.LABEL], importer.OWNER)

    def test_bad_or_ambiguous_checksum_never_calls_docker(self):
        with tempfile.TemporaryDirectory() as folder:
            image = pathlib.Path(folder) / 'image.tar'
            sums = image.with_name('SHA256SUMS')
            image.write_bytes(b'archive')
            for value in ['0' * 64 + '  image.tar\n', '',
                          ('0' * 64 + '  image.tar\n') * 2]:
                sums.write_text(value, encoding='utf-8')
                with patch.object(importer.subprocess, 'run') as docker:
                    with self.assertRaises(ValueError):
                        importer.import_image(image, sums)
                    docker.assert_not_called()

    def test_unowned_tag_is_never_loaded(self):
        with patch.object(importer, 'verified_archive', return_value='verified'), \
             patch.object(importer.subprocess, 'run') as inspect, \
             patch.object(importer.subprocess, 'Popen') as load:
            inspect.return_value.returncode = 0
            inspect.return_value.stdout = json.dumps([{'Config': {'Labels': {}}}])
            with self.assertRaisesRegex(ValueError, 'unowned'):
                importer.import_image(None, None)
            load.assert_not_called()


if __name__ == '__main__':
    unittest.main()
