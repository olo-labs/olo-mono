"""Isolated test IdP and deterministic model; never records requests or credentials."""
import http.server
import json
import pathlib
import ssl
import threading
import time

ROOT = pathlib.Path('/fixture')
LOCK = threading.Lock()
CALLS = 0


def incident_reply(request, plan):
    """Select scripted calls only from the actual agent catalog; never fabricate tool results."""
    available = {tool['function']['name'] for tool in request.get('tools', [])}
    selected = {call['function']['name'] for message in request.get('messages', [])
                for call in message.get('tool_calls', [])}
    for name, arguments in plan:
        if name in available and name not in selected:
            # Verification consumes the remediation ID actually returned by the package.
            if name.endswith('_verify'):
                actual = None
                for message in request.get('messages', []):
                    if message.get('role') == 'tool':
                        try:
                            value = json.loads(message.get('content', ''))
                            if 'remediationId' in value:
                                actual = value['remediationId']
                        except (ValueError, TypeError):
                            pass
                if actual is None:
                    raise ValueError('Verification requires the actual remediation result')
                arguments = {'remediationId': actual}
            return {'role': 'assistant', 'content': '',
                    'tool_calls': [{'function': {'name': name, 'arguments': arguments}}]}
    return {'role': 'assistant', 'content': 'Scripted incident stage complete.'}


def incident_delegation_reply(request):
    """The test planner consumes the rendered agent catalog rather than inventing delegation authority."""
    marker = 'INCIDENT_DELEGATION_CATALOG:\n'
    content = next((str(message.get('content','')) for message in request.get('messages', [])
                    if marker in str(message.get('content',''))), None)
    if content is None:
        return None
    catalog, _ = json.JSONDecoder().raw_decode(content.split(marker,1)[1].lstrip())
    allowed = {entry['agentId'] for entry in catalog}
    calls = [dict(agentId=name, message='Complete your delegated INC-5001 stage.')
             for name in ['triage-agent','remediation-agent','incident-report-agent'] if name in allowed]
    return {'role':'assistant','content':json.dumps(dict(agentCalls=calls,
        directResponse=None if calls else 'No incident agents are available.'))}


class Handler(http.server.BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass

    def reply(self, status, value):
        body = json.dumps(value).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        try:
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def do_GET(self):
        if self.path == '/jwks.json':
            return self.reply(200, json.loads((ROOT/'jwks.json').read_text()))
        if self.path == '/api/tags':
            return self.reply(200, {'models': [{'name': 'qualification-model', 'model': 'qualification-model'}]})
        if self.path == '/counts':
            return self.reply(200, {'modelCalls': CALLS})
        self.reply(404, {})

    def do_POST(self):
        global CALLS
        length = int(self.headers.get('Content-Length', '0'))
        if self.path != '/api/chat' or not 0 < length <= 1048576:
            return self.reply(400, {})
        request = json.loads(self.rfile.read(length))
        with LOCK:
            CALLS += 1
        deadline = time.monotonic()+30
        while (ROOT/'hold-model').exists() and time.monotonic() < deadline:
            time.sleep(.1)
        if (ROOT/'hold-model').exists():
            return self.reply(503, {'error': 'Fixture model deadline'})
        message = {'role': 'assistant', 'content': (ROOT/'reply.txt').read_text()}
        if (ROOT/'incident-plan.json').exists():
            try:
                if request.get('tools'):
                    message = incident_reply(request, json.loads((ROOT/'incident-plan.json').read_text()))
                else:
                    message = incident_delegation_reply(request) or message
            except ValueError:
                return self.reply(503, {'error': 'Incident prerequisite missing'})
        self.reply(200, {'model': request.get('model'), 'done': True, 'message': message})


if __name__ == '__main__':
    secure = http.server.ThreadingHTTPServer(('0.0.0.0', 8443), Handler)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(ROOT/'server.crt', ROOT/'server.key')
    secure.socket = context.wrap_socket(secure.socket, server_side=True)
    threading.Thread(target=secure.serve_forever, daemon=True).start()
    http.server.ThreadingHTTPServer(('0.0.0.0', 11434), Handler).serve_forever()
