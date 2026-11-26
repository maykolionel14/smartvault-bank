"""HTTP helpers for an isolated loopback-only SmartVault test stack."""
import json
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, build_opener, ProxyHandler, HTTPRedirectHandler
from urllib.parse import urlparse


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


class Api:
    def __init__(self, base):
        url = urlparse(base)
        if url.scheme != "http" or url.hostname != "127.0.0.1" or not url.port or url.path or url.query or url.username or url.password:
            raise ValueError("Las pruebas solo admiten http://127.0.0.1:PUERTO")
        self.base = base

    def call(self, path, method="GET", body=None, token=None, timeout=10):
        if not path.startswith("/") or path.startswith("//"):
            raise ValueError("Ruta local inválida")
        headers = {"Accept": "application/json", "Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        data = None if body is None else json.dumps(body).encode()
        request = Request(self.base + path, data=data, headers=headers, method=method)
        opener = build_opener(ProxyHandler({}), NoRedirect())
        try:
            with opener.open(request, timeout=timeout) as response:
                status, raw = response.status, response.read()
        except HTTPError as error:
            status, raw = error.code, error.read()
        except (URLError, TimeoutError, OSError):
            return 0, None
        try:
            return status, json.loads(raw)
        except (ValueError, UnicodeError):
            return status, raw.decode("utf-8", errors="replace")

    def expect(self, path, status=200, **kwargs):
        actual, data = self.call(path, **kwargs)
        assert actual == status, f"{path}: esperado {status}, recibido {actual}"
        return data

    def login(self, email, password):
        result = self.expect("/api/v1/identity/login", method="POST", body={"email": email, "password": password})
        assert result["activated"] is True
        return result["access_token"]


def wait_until(check, timeout=120):
    started = time.monotonic()
    while time.monotonic() - started < timeout:
        if check():
            return time.monotonic() - started
        time.sleep(1)
    raise AssertionError("El servicio no se recuperó dentro del límite")
