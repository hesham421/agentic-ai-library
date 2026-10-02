"""
BEST-EFFORT resolution of an error code's human-readable message from the
application's own i18n resource bundles.

On this platform an error code's VALUE is simultaneously the wire `code` in
the `ApiError` envelope and the i18n message key (`FinErrorCodes`' own class
Javadoc states it, and every module follows it). So the message a consumer
will actually receive for a documented code is already in the repository --
it just was never read: the published catalogue listed the code and its HTTP
status, and left the integrator to guess what the API would say.

Nothing here is module-specific: the bundles are shared, the lookup is by the
code's own value, and a code with no bundle entry simply carries no message
(never a placeholder, never a guessed translation).

Properties parsing is deliberately minimal but faithful to what
`java.util.Properties` accepts for this repository's bundles: `key=value` or
`key:value`, `#`/`!` comments, blank lines, and backslash line continuations.
Unicode escapes (\\uXXXX) are decoded, since a bundle may be written either
escaped or as literal UTF-8.
"""

import re
from pathlib import Path
from typing import Optional

# "messages_ar.properties" -> "ar"; "messages.properties" -> "" (the base
# bundle, which is the fallback every locale resolves through). The base name
# comes from spring.messages.basename, so nothing here hardcodes "messages".
_CONTINUATION_RE = re.compile(r"\\\s*$")
_UNICODE_ESCAPE_RE = re.compile(r"\\u([0-9a-fA-F]{4})")


def locale_tag(path: Path, base_name: str) -> str:
    """The locale suffix of a bundle file, or "" for the base bundle."""
    stem = path.stem
    if stem == base_name:
        return ""
    if stem.startswith(base_name + "_"):
        return stem[len(base_name) + 1:]
    return stem


def _decode(value: str) -> str:
    return _UNICODE_ESCAPE_RE.sub(lambda m: chr(int(m.group(1), 16)), value)


def parse_properties(text: str) -> dict[str, str]:
    """key -> value for one .properties file. Later duplicate keys win, the
    same way java.util.Properties resolves them."""
    entries: dict[str, str] = {}
    pending: Optional[str] = None
    for raw_line in text.splitlines():
        line = raw_line if pending is not None else raw_line.lstrip()
        if pending is None:
            if not line or line[0] in "#!":
                continue
        if pending is not None:
            joined = pending + line.strip()
        else:
            joined = line
        if _CONTINUATION_RE.search(joined):
            pending = _CONTINUATION_RE.sub("", joined)
            continue
        pending = None
        match = re.match(r"\s*([^=:\s][^=:]*?)\s*[=:]\s*(.*)$", joined)
        if not match:
            continue
        entries[match.group(1)] = _decode(match.group(2).rstrip())
    return entries


def load_bundles(bundle_files: list[Path], base_name: str) -> dict[str, dict[str, str]]:
    """{locale tag -> {key -> message}} for the given bundle files. A file that
    cannot be read is skipped rather than failing the run -- documentation of
    everything else is still worth producing."""
    bundles: dict[str, dict[str, str]] = {}
    for path in sorted(bundle_files):
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        bundles[locale_tag(path, base_name)] = parse_properties(text)
    return bundles


def attach_messages(error_codes, bundles: dict[str, dict[str, str]]) -> None:
    """Fills ErrorCode.messages from the bundles, keyed by the code's own
    value. A locale whose bundle has no entry for a code contributes nothing
    to that code -- an absent translation must render as absent, not as the
    base bundle's text under a locale's name."""
    if not bundles:
        return
    for code in error_codes:
        for tag in sorted(bundles):
            message = bundles[tag].get(code.value)
            if message:
                code.messages[tag] = message


def locales_present(error_codes) -> list[str]:
    """Every locale tag that actually carries at least one message, base
    bundle ("") first, then the rest alphabetically -- so a column is only
    rendered when there is something in it."""
    tags = {tag for code in error_codes for tag in code.messages}
    ordered = [""] if "" in tags else []
    return ordered + sorted(t for t in tags if t)
