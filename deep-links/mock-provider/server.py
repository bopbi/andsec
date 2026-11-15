#!/usr/bin/env python3
import base64
import hashlib
import hmac
import html
import json
import secrets
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlencode, urlsplit

HOST = "127.0.0.1"
PORT = 8765
CLIENT_ID = "andsec-victim"
REDIRECT_URI = "andsec-login://auth/callback"
VALUE_TTL_SECONDS = 120
MAX_BODY_BYTES = 8192


class ProviderState:
    def __init__(self):
        self.lock = threading.Lock()
        self.codes = {}
        self.tokens = {}

    def prune(self):
        now = time.time()
        self.codes = {key: value for key, value in self.codes.items() if value["expires_at"] > now}
        self.tokens = {key: value for key, value in self.tokens.items() if value > now}

    def issue_token(self):
        token = secrets.token_urlsafe(32)
        self.tokens[token] = time.time() + VALUE_TTL_SECONDS
        return token


STATE = ProviderState()


def valid_parameters(values, required, optional=()):
    allowed = set(required) | set(optional)
    return set(values).issubset(allowed) and set(required).issubset(values) and all(
        len(values[key]) == 1 for key in values
    )


def valid_authorization_request(values):
    response_type = values.get("response_type", [""])[0]
    required = {"client_id", "redirect_uri", "response_type", "state"}
    optional = {"code_challenge", "code_challenge_method"} if response_type == "code" else set()
    if not valid_parameters(values, required, optional):
        return False
    if response_type not in {"token", "code"}:
        return False
    if values["client_id"][0] != CLIENT_ID or values["redirect_uri"][0] != REDIRECT_URI:
        return False
    state = values["state"][0]
    if not 16 <= len(state) <= 256:
        return False
    if response_type == "code":
        if values.get("code_challenge_method", [""])[0] != "S256":
            return False
        challenge = values.get("code_challenge", [""])[0]
        if not 43 <= len(challenge) <= 128:
            return False
    return True


def authorization_page(values):
    fields = []
    for key in sorted(values):
        fields.append(
            f'<input type="hidden" name="{html.escape(key)}" value="{html.escape(values[key][0], quote=True)}">'
        )
    return f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Synthetic Login Lab</title>
</head>
<body>
  <main>
    <h1>Synthetic Login Lab</h1>
    <p>This local provider uses only a fixed lab profile. No account or credentials are involved.</p>
    <form method="post" action="/authorize/accept">
      {''.join(fields)}
      <button type="submit">Sign in as Demo User</button>
    </form>
  </main>
</body>
</html>"""


class Handler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass

    def end_headers(self):
        self.send_header("Cache-Control", "no-store")
        self.send_header("Pragma", "no-cache")
        super().end_headers()

    def send_bytes(self, status, content_type, body, extra_headers=None):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        for key, value in (extra_headers or {}).items():
            self.send_header(key, value)
        self.end_headers()
        self.wfile.write(body)

    def send_json(self, status, value):
        self.send_bytes(status, "application/json", json.dumps(value).encode("utf-8"))

    def send_html(self, status, body):
        self.send_bytes(status, "text/html; charset=utf-8", body.encode("utf-8"))

    def read_form(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            return None
        if length <= 0 or length > MAX_BODY_BYTES:
            return None
        if self.headers.get_content_type() != "application/x-www-form-urlencoded":
            return None
        try:
            return parse_qs(self.rfile.read(length).decode("utf-8"), keep_blank_values=True)
        except (UnicodeDecodeError, ValueError):
            return None

    def do_GET(self):
        with STATE.lock:
            STATE.prune()
        path = urlsplit(self.path).path
        if path == "/authorize":
            self.authorize()
        elif path == "/profile":
            self.profile()
        else:
            self.send_json(404, {"error": "not_found"})

    def do_POST(self):
        with STATE.lock:
            STATE.prune()
        path = urlsplit(self.path).path
        if path == "/authorize/accept":
            self.accept_authorization()
        elif path == "/token":
            self.redeem_code()
        else:
            self.send_json(404, {"error": "not_found"})

    def authorize(self):
        values = parse_qs(urlsplit(self.path).query, keep_blank_values=True)
        if not valid_authorization_request(values):
            self.send_html(400, "<!doctype html><title>Lab request rejected</title><p>Invalid lab authorization request.</p>")
            return
        self.send_html(200, authorization_page(values))

    def accept_authorization(self):
        values = self.read_form()
        if values is None or not valid_authorization_request(values):
            self.send_html(400, "<!doctype html><title>Lab request rejected</title><p>Invalid lab authorization request.</p>")
            return
        state = values["state"][0]
        if values["response_type"][0] == "token":
            with STATE.lock:
                token = STATE.issue_token()
            query = {"access_token": token, "state": state}
        else:
            code = secrets.token_urlsafe(32)
            with STATE.lock:
                STATE.codes[code] = {
                    "client_id": CLIENT_ID,
                    "redirect_uri": REDIRECT_URI,
                    "challenge": values["code_challenge"][0],
                    "expires_at": time.time() + VALUE_TTL_SECONDS,
                    "used": False,
                }
            query = {"code": code, "state": state}
        location = f"{REDIRECT_URI}?{urlencode(query)}"
        self.send_bytes(302, "text/plain; charset=utf-8", b"Redirecting to the lab app.\n", {"Location": location})

    def redeem_code(self):
        values = self.read_form()
        required = {"grant_type", "client_id", "redirect_uri", "code"}
        if values is None or not valid_parameters(values, required, {"code_verifier"}):
            self.send_json(400, {"error": "invalid_grant"})
            return
        if (
            values["grant_type"][0] != "authorization_code"
            or values["client_id"][0] != CLIENT_ID
            or values["redirect_uri"][0] != REDIRECT_URI
        ):
            self.send_json(400, {"error": "invalid_grant"})
            return
        code = values["code"][0]
        verifier = values.get("code_verifier", [""])[0]
        with STATE.lock:
            STATE.prune()
            record = STATE.codes.get(code)
            if record is None or record["used"]:
                self.send_json(400, {"error": "invalid_grant"})
                return
            calculated = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode("ascii", errors="ignore")).digest()).rstrip(b"=").decode("ascii")
            if not verifier or not hmac.compare_digest(calculated, record["challenge"]):
                self.send_json(400, {"error": "invalid_grant"})
                return
            if record["client_id"] != CLIENT_ID or record["redirect_uri"] != REDIRECT_URI:
                self.send_json(400, {"error": "invalid_grant"})
                return
            record["used"] = True
            token = STATE.issue_token()
        self.send_json(200, {"access_token": token, "token_type": "Bearer", "expires_in": VALUE_TTL_SECONDS})

    def profile(self):
        authorization = self.headers.get("Authorization", "")
        token = authorization[7:] if authorization.startswith("Bearer ") else ""
        with STATE.lock:
            STATE.prune()
            valid = bool(token) and token in STATE.tokens
        if not valid:
            self.send_json(401, {"error": "invalid_token"})
            return
        self.send_json(200, {"display_name": "Demo User"})


def main():
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    server.daemon_threads = True
    print(f"Synthetic lab provider listening on http://{HOST}:{PORT}", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
        print("Synthetic lab provider stopped", flush=True)


if __name__ == "__main__":
    main()
