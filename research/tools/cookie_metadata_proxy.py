"""Print school request and cookie metadata only; never save raw mitmproxy flows.

Run mitmdump without -w. Only the two business hosts are decrypted; CAS remains
pass-through. Cookie values, query strings, Location values and bodies are omitted.
"""

import json
import re
import time
from urllib.parse import urljoin, urlsplit

START = time.monotonic()
HOSTS = {"personal.neu.edu.cn", "jwxt.neu.edu.cn"}
SHAPE_PATHS = {
    "/portal/personal/frontend/data/info",
    "/jwapp/sys/homeapp/api/home/currentUser.do",
}
LAST_COOKIE_VALUE = {}
LAST_PORTAL_IDENTIFIERS = {}


def requestheaders(flow):
    flow.request.stream = True


def responseheaders(flow):
    flow.response.stream = _path(flow.request.path) not in SHAPE_PATHS


def _path(raw):
    path = urlsplit(raw).path
    path = re.sub(r"/(?=[^/]*\d)[^/]{16,}(?=/|$)", "/{id}", path)
    return re.sub(r"/\d{5,}(?=/|$)", "/{id}", path)


def _request_cookie_names(flow):
    value = flow.request.headers.get("cookie", "")
    return sorted({part.split("=", 1)[0].strip() for part in value.split(";") if "=" in part})


def _set_cookie_metadata(flow):
    result = []
    for raw in flow.response.headers.get_all("set-cookie"):
        parts = [part.strip() for part in raw.split(";")]
        if "=" not in parts[0]:
            continue
        name, value = parts[0].split("=", 1)
        cookie_key = (flow.request.pretty_host.lower(), name)
        previous = LAST_COOKIE_VALUE.get(cookie_key)
        LAST_COOKIE_VALUE[cookie_key] = value
        attrs = {}
        for part in parts[1:]:
            key, _, value = part.partition("=")
            attrs[key.lower()] = value
        result.append({
            "name": name,
            "domain": attrs.get("domain"),
            "path": attrs.get("path"),
            "max_age_s": attrs.get("max-age"),
            "has_expires": "expires" in attrs,
            "secure": "secure" in attrs,
            "http_only": "httponly" in attrs,
            "same_site": attrs.get("samesite"),
            "changed_since_previous": None if previous is None else previous != value,
        })
    return result


def response(flow):
    host = flow.request.pretty_host.lower()
    if host not in HOSTS:
        return
    item = {
        "t_s": round(time.monotonic() - START, 1),
        "host": host,
        "method": flow.request.method,
        "path": _path(flow.request.path),
        "status": flow.response.status_code,
        "request_cookie_names": _request_cookie_names(flow),
        "set_cookies": _set_cookie_metadata(flow),
    }
    if flow.response.status_code in range(300, 400):
        location = flow.response.headers.get("location", "")
        item["redirect_host"] = urlsplit(urljoin(flow.request.pretty_url, location)).hostname
    if _path(flow.request.path) in SHAPE_PATHS and flow.response.status_code == 200:
        try:
            body = json.loads(flow.response.get_text(strict=False))
            if isinstance(body, dict):
                item["json_keys"] = sorted(body.keys())
                for key in ("d", "data", "datas", "info", "user", "result"):
                    nested = body.get(key)
                    if isinstance(nested, dict):
                        item[f"{key}_keys"] = sorted(nested.keys())
                        if key == "d" and isinstance(nested.get("info"), dict):
                            item["d_info_keys"] = sorted(nested["info"].keys())
                        if key == "datas" and isinstance(nested.get("currentUser"), dict):
                            item["datas_currentUser_keys"] = sorted(nested["currentUser"].keys())
                if _path(flow.request.path) == "/portal/personal/frontend/data/info":
                    portal_data = body.get("d")
                    info = portal_data.get("info") if isinstance(portal_data, dict) else None
                    if isinstance(info, dict):
                        for key in ("xgh", "uid", "identity_id"):
                            LAST_PORTAL_IDENTIFIERS[key] = info.get(key)
                if _path(flow.request.path).endswith("/currentUser.do"):
                    academic_data = body.get("datas")
                    academic_id = academic_data.get("userId") if isinstance(academic_data, dict) else None
                    if academic_id is not None:
                        item["portal_id_matches_academic"] = {
                            key: value is not None and str(value) == str(academic_id)
                            for key, value in LAST_PORTAL_IDENTIFIERS.items()
                        }
        except (ValueError, UnicodeError):
            item["json_keys"] = None
    print(json.dumps(item, ensure_ascii=False), flush=True)
