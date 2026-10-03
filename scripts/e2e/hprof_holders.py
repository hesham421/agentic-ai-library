#!/usr/bin/env python3
"""Who holds a byte pattern in a live-object heap dump (local E2E only, stdlib only).

    python3 scripts/e2e/hprof_holders.py <dump.hprof> <marker> [depth]

Finds every primitive array whose bytes contain ``marker`` (ASCII or UTF-16LE), then walks the references to it
backwards ``depth`` levels (default 4): instance fields and object-array elements that hold the object id. Prints
one chain of class names per array, e.g. ``byte[] <- java.nio.HeapByteBuffer <- org.apache.coyote...``. Used by
TC-INT-023 to attribute a copy found in the live heap. Prints nothing of the array content beyond the marker.
"""
import struct
import sys
from collections import defaultdict

SIZES = {2: None, 4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}
ROOT = {0xFF: 0, 0x01: -1, 0x02: 8, 0x03: 8, 0x04: 4, 0x05: 0, 0x06: 4, 0x07: 0, 0x08: 8}


def parse(path, markers, depth):
    data = open(path, "rb").read()
    end = data.index(b"\0")
    ids = struct.unpack(">I", data[end + 1:end + 5])[0]
    fmt = ">Q" if ids == 8 else ">I"
    rid = lambda off: struct.unpack(fmt, data[off:off + ids])[0]  # noqa: E731
    strings, class_name, inst_size = {}, {}, {}
    instances, objarrays, hits = [], [], []
    pos = end + 1 + 4 + 8
    while pos < len(data):
        tag = data[pos]
        length = struct.unpack(">I", data[pos + 5:pos + 9])[0]
        body = pos + 9
        if tag == 0x01:
            strings[rid(body)] = data[body + ids:body + length].decode("utf-8", "replace")
        elif tag == 0x02:
            class_name[rid(body + 4)] = rid(body + 8 + ids)
        elif tag in (0x0C, 0x1C):
            p, stop = body, body + length
            while p < stop:
                sub = data[p]
                p += 1
                if sub in ROOT:
                    p += ids + (ids if ROOT[sub] < 0 else ROOT[sub])   # 0x01 JNI global: object id + ref id
                elif sub == 0x20:
                    cid = rid(p)
                    p += ids + 4 + 6 * ids
                    p += 4
                    n = struct.unpack(">H", data[p:p + 2])[0]
                    p += 2
                    for _ in range(n):
                        t = data[p + 2]
                        p += 3 + (SIZES[t] or ids)
                    n = struct.unpack(">H", data[p:p + 2])[0]
                    p += 2
                    for _ in range(n):
                        t = data[p + ids]
                        p += ids + 1 + (SIZES[t] or ids)
                    n = struct.unpack(">H", data[p:p + 2])[0]
                    p += 2 + n * (ids + 1)
                    inst_size[cid] = True
                elif sub == 0x21:
                    oid, cid = rid(p), rid(p + ids + 4)
                    n = struct.unpack(">I", data[p + 2 * ids + 4:p + 2 * ids + 8])[0]
                    instances.append((oid, cid, p + 2 * ids + 8, n))
                    p += 2 * ids + 8 + n
                elif sub == 0x22:
                    oid = rid(p)
                    n = struct.unpack(">I", data[p + ids + 4:p + ids + 8])[0]
                    objarrays.append((oid, p + 2 * ids + 8, n))
                    p += 2 * ids + 8 + n * ids
                elif sub == 0x23:
                    oid = rid(p)
                    n = struct.unpack(">I", data[p + ids + 4:p + ids + 8])[0]
                    t = data[p + ids + 8]
                    size = n * SIZES[t]
                    chunk = data[p + ids + 9:p + ids + 9 + size]
                    if any(m in chunk for m in markers):
                        hits.append((oid, {8: "byte[]", 5: "char[]"}.get(t, f"prim[{t}]"), n))
                    p += ids + 9 + size
                else:
                    raise SystemExit(f"unknown heap sub-record 0x{sub:02x} at {p}")
        pos = body + length
    name = lambda cid: strings.get(class_name.get(cid), f"class@{cid:x}").replace("/", ".")  # noqa: E731

    def holders(target):
        key = struct.pack(fmt, target)
        out = []
        for oid, cid, off, n in instances:
            if key in data[off:off + n]:
                out.append((oid, name(cid)))
        for oid, off, n in objarrays:
            if key in data[off:off + n * ids]:
                out.append((oid, "Object[]"))
        return out

    for oid, kind, n in hits:
        chains = [[(oid, f"{kind}({n})")]]
        for _ in range(depth):
            nxt = []
            for chain in chains:
                hs = holders(chain[-1][0])[:3]
                nxt += [chain + [h] for h in hs] or [chain]
            chains = nxt
        for chain in chains:
            print(" <- ".join(c[1] for c in chain))


if __name__ == "__main__":
    marker = sys.argv[2]
    parse(sys.argv[1], [marker.encode(), marker.encode("utf-16-le")], int(sys.argv[3]) if len(sys.argv) > 3 else 4)
