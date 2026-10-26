"""A base64 payload must never masquerade as encrypted Temporal history."""
import base64
import json
import unittest
from qualification_pipeline import protected_history


class PipelineHistoryTest(unittest.TestCase):
    def test_encrypted_marker_does_not_hide_another_clear_payload(self):
        canary='private context'
        raw=json.dumps({'events':[{'payloads':[
            {'metadata':{'encoding':'binary/olo-aes256gcm'},'data':'AA=='},
            {'metadata':{'encoding':'json/plain'},'data':base64.b64encode(canary.encode()).decode()}]}]})
        self.assertFalse(protected_history(raw,canary))

    def test_plain_or_unprotected_history_is_rejected(self):
        self.assertFalse(protected_history('{"data":"private context"}','private context'))
        self.assertFalse(protected_history('{"events":[]}','private context'))

    def test_encrypted_payload_without_the_private_text_is_accepted(self):
        self.assertTrue(protected_history(json.dumps({'payloads':[{
            'metadata':{'encoding':base64.b64encode(b'binary/olo-aes256gcm').decode()},
            'data':base64.b64encode(b'opaque ciphertext').decode()}]}),'private context'))


if __name__=='__main__':unittest.main()
