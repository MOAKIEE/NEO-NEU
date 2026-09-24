"""Local metadata-only capture. Run without -w; never persist raw flows."""
import json
import re
import time
from urllib.parse import urlsplit

START = time.monotonic()
HOSTS = {"personal.neu.edu.cn", "jwxt.neu.edu.cn"}


def http_connect(flow):
    host = flow.request.host.lower()
    if host.endswith(".neu.edu.cn"):
        print(json.dumps({"t_s": round(time.monotonic() - START, 1),
                          "event": "connect", "host": host}), flush=True)


def requestheaders(flow):
    flow.request.stream = True


def responseheaders(flow):
    flow.response.stream = True


def response(flow):
    host = flow.request.pretty_host.lower()
    if host not in HOSTS:
        return
    path = urlsplit(flow.request.path).path
    path = re.sub(r"/(?=[^/]*(?:[0-9]))[^/]{16,}(?=/|$)", "/{id}", path)
    path = re.sub(r"/\d{5,}(?=/|$)", "/{id}", path)
    print(json.dumps({"t_s": round(time.monotonic() - START, 1),
                      "host": host, "method": flow.request.method,
                      "path": path, "status": flow.response.status_code}), flush=True)
