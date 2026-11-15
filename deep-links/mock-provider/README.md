# Local Mock Provider

This dependency-free Python 3 server is exclusively for the synthetic deep-link lab. It binds to `127.0.0.1:8765`; the Android emulator reaches it as `http://10.0.2.2:8765`.

Start it from the repository root:

```bash
python3 deep-links/mock-provider/server.py
```

Stop it with Ctrl-C. All codes, tokens, and pending state are in memory and disappear when the process stops. The server logs only fixed startup and shutdown messages, never request URIs or response secrets.
