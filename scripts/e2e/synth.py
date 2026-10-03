"""Synthetic document generators for the aias end-to-end simulation (stdlib only).

Everything produced here is SYNTHETIC test data — invented transcripts of invented students, no real
person or record. Two formats:

* ``make_pdf(lines)`` — a minimal one-page PDF with a real text layer (Helvetica), so Document
  Access reads it by text extraction (PDFBox), without any model call.
* ``make_png(lines)`` — a grayscale PNG with the text drawn in a built-in 5x7 bitmap font, so it has
  no text layer and Document Access must send it to the document-reading model.
"""
import struct
import zlib

# ---------------------------------------------------------------------------------------------- PDF


def _pdf_escape(text):
    return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")


def make_pdf(lines):
    """A minimal valid PDF 1.4 holding ``lines`` as a text layer. Kept small (< 2 KB) on purpose:
    the blob fixture embeds it as a hex literal in a SQL statement."""
    ops = ["BT", "/F1 12 Tf", "14 TL", "50 780 Td"]
    for line in lines:
        ops.append("(" + _pdf_escape(line) + ") Tj T*")
    ops.append("ET")
    stream = "\n".join(ops).encode("latin-1")
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Resources << /Font << /F1 4 0 R >> >> "
        b"/Contents 5 0 R >>",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        b"<< /Length " + str(len(stream)).encode() + b" >>\nstream\n" + stream + b"\nendstream",
    ]
    out = bytearray(b"%PDF-1.4\n")
    offsets = []
    for number, body in enumerate(objects, start=1):
        offsets.append(len(out))
        out += str(number).encode() + b" 0 obj\n" + body + b"\nendobj\n"
    xref = len(out)
    out += b"xref\n0 " + str(len(objects) + 1).encode() + b"\n0000000000 65535 f \n"
    for offset in offsets:
        out += ("%010d 00000 n \n" % offset).encode()
    out += (b"trailer\n<< /Size " + str(len(objects) + 1).encode() + b" /Root 1 0 R >>\nstartxref\n"
            + str(xref).encode() + b"\n%%EOF\n")
    return bytes(out)


# ---------------------------------------------------------------------------------------------- PNG
# 5x7 bitmap font: each glyph is 7 rows of 5 bits (MSB = leftmost column).
_FONT = {
    " ": [0, 0, 0, 0, 0, 0, 0],
    "A": [14, 17, 17, 31, 17, 17, 17], "B": [30, 17, 17, 30, 17, 17, 30], "C": [14, 17, 16, 16, 16, 17, 14],
    "D": [30, 17, 17, 17, 17, 17, 30], "E": [31, 16, 16, 30, 16, 16, 31], "F": [31, 16, 16, 30, 16, 16, 16],
    "G": [14, 17, 16, 23, 17, 17, 15], "H": [17, 17, 17, 31, 17, 17, 17], "I": [14, 4, 4, 4, 4, 4, 14],
    "J": [7, 2, 2, 2, 2, 18, 12], "K": [17, 18, 20, 24, 20, 18, 17], "L": [16, 16, 16, 16, 16, 16, 31],
    "M": [17, 27, 21, 21, 17, 17, 17], "N": [17, 17, 25, 21, 19, 17, 17], "O": [14, 17, 17, 17, 17, 17, 14],
    "P": [30, 17, 17, 30, 16, 16, 16], "Q": [14, 17, 17, 17, 21, 18, 13], "R": [30, 17, 17, 30, 20, 18, 17],
    "S": [15, 16, 16, 14, 1, 1, 30], "T": [31, 4, 4, 4, 4, 4, 4], "U": [17, 17, 17, 17, 17, 17, 14],
    "V": [17, 17, 17, 17, 17, 10, 4], "W": [17, 17, 17, 21, 21, 21, 10], "X": [17, 17, 10, 4, 10, 17, 17],
    "Y": [17, 17, 10, 4, 4, 4, 4], "Z": [31, 1, 2, 4, 8, 16, 31],
    "0": [14, 17, 19, 21, 25, 17, 14], "1": [4, 12, 4, 4, 4, 4, 14], "2": [14, 17, 1, 2, 4, 8, 31],
    "3": [31, 2, 4, 2, 1, 17, 14], "4": [2, 6, 10, 18, 31, 2, 2], "5": [31, 16, 30, 1, 1, 17, 14],
    "6": [6, 8, 16, 30, 17, 17, 14], "7": [31, 1, 2, 4, 8, 8, 8], "8": [14, 17, 17, 14, 17, 17, 14],
    "9": [14, 17, 17, 15, 1, 2, 12],
    ".": [0, 0, 0, 0, 0, 12, 12], ":": [0, 12, 12, 0, 12, 12, 0], "-": [0, 0, 0, 31, 0, 0, 0],
    "/": [1, 1, 2, 4, 8, 16, 16], "(": [2, 4, 8, 8, 8, 4, 2], ")": [8, 4, 2, 2, 2, 4, 8],
    ",": [0, 0, 0, 0, 12, 4, 8], "_": [0, 0, 0, 0, 0, 0, 31],
}


def make_png(lines, scale=4, margin=24):
    """A grayscale PNG (black text on white) of ``lines`` in the 5x7 font. Unknown characters are
    drawn as blanks; text is upper-cased."""
    lines = [line.upper() for line in lines]
    cols = max(len(line) for line in lines)
    glyph_w, glyph_h = 6 * scale, 9 * scale  # 1 column / 2 rows of spacing
    width = margin * 2 + cols * glyph_w
    height = margin * 2 + len(lines) * glyph_h
    pixels = [bytearray([255] * width) for _ in range(height)]
    for row_index, line in enumerate(lines):
        top = margin + row_index * glyph_h
        for col_index, char in enumerate(line):
            glyph = _FONT.get(char, _FONT[" "])
            left = margin + col_index * glyph_w
            for gy, bits in enumerate(glyph):
                for gx in range(5):
                    if bits & (1 << (4 - gx)):
                        for dy in range(scale):
                            y = top + gy * scale + dy
                            start = left + gx * scale
                            pixels[y][start:start + scale] = b"\x00" * scale
    raw = b"".join(b"\x00" + bytes(row) for row in pixels)

    def chunk(kind, data):
        return (struct.pack(">I", len(data)) + kind + data
                + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF))

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 0, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


# ------------------------------------------------------------------------------------- transcripts


def transcript_lines(request_number, gpa, credits, student="SYNTHETIC STUDENT 0001"):
    """The text of a synthetic transcript. ``request_number`` may be None (host-file fixtures whose
    service knowledge does not compare it)."""
    lines = ["SYNTHETIC TEST TRANSCRIPT - NOT A REAL RECORD",
             "Student: " + student,
             "Grade point average (GPA): %s on a 4.00 scale" % gpa,
             "Completed credit hours: %s" % credits]
    if request_number is not None:
        lines.insert(1, "Request number: " + request_number)
    return lines


# ================================================================== DOC formats (doc-* E2E groups)
# Every generator below is stdlib only and produces SYNTHETIC content; the DOC groups list or upload these
# files to exercise Document Access's format detection (by content signature) and its three readers.
import hashlib  # noqa: E402
import io  # noqa: E402
import zipfile  # noqa: E402


def _render(lines, scale, margin):
    """The 8-bit grayscale pixel rows of ``lines`` drawn in the 5x7 font (shared by PNG and scanned PDF)."""
    lines = [line.upper() for line in lines]
    cols = max(len(line) for line in lines)
    glyph_w, glyph_h = 6 * scale, 9 * scale
    width = margin * 2 + cols * glyph_w
    height = margin * 2 + len(lines) * glyph_h
    pixels = [bytearray([255] * width) for _ in range(height)]
    for row_index, line in enumerate(lines):
        top = margin + row_index * glyph_h
        for col_index, char in enumerate(line):
            glyph = _FONT.get(char, _FONT[" "])
            left = margin + col_index * glyph_w
            for gy, bits in enumerate(glyph):
                for gx in range(5):
                    if bits & (1 << (4 - gx)):
                        for dy in range(scale):
                            start = left + gx * scale
                            pixels[top + gy * scale + dy][start:start + scale] = b"\x00" * scale
    return width, height, pixels


def _pdf(objects, trailer_extra=b""):
    """A PDF 1.4 from object bodies (object n = objects[n-1]) with a correct xref table."""
    out = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = []
    for number, body in enumerate(objects, start=1):
        offsets.append(len(out))
        out += str(number).encode() + b" 0 obj\n" + body + b"\nendobj\n"
    xref = len(out)
    out += b"xref\n0 " + str(len(objects) + 1).encode() + b"\n0000000000 65535 f \n"
    for offset in offsets:
        out += ("%010d 00000 n \n" % offset).encode()
    out += (b"trailer\n<< /Size " + str(len(objects) + 1).encode() + b" /Root 1 0 R " + trailer_extra
            + b">>\nstartxref\n" + str(xref).encode() + b"\n%%EOF\n")
    return bytes(out)


def _text_objects(lines, stream_filter=lambda number, data: data):
    ops = ["BT", "/F1 12 Tf", "14 TL", "50 780 Td"] + ["(" + _pdf_escape(x) + ") Tj T*" for x in lines] + ["ET"]
    stream = stream_filter(5, "\n".join(ops).encode("latin-1"))
    return [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Resources << /Font << /F1 4 0 R >> >> "
        b"/Contents 5 0 R >>",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        b"<< /Length " + str(len(stream)).encode() + b" >>\nstream\n" + stream + b"\nendstream",
    ]


def make_pdf_sized(lines, total_size):
    """A text-layer PDF of EXACTLY ``total_size`` bytes: the text of ``lines`` plus an unreferenced padding
    stream (spaces), so the readable text stays short whatever the size (boundary fixtures)."""
    def build(n):
        pad = b"<< /Length " + str(n).encode() + b" >>\nstream\n" + b" " * n + b"\nendstream"
        return _pdf(_text_objects(lines) + [pad])
    n = max(0, total_size - len(build(0)))
    for _ in range(4):
        size = len(build(n))
        if size == total_size:
            return build(n)
        n += total_size - size
    raise ValueError(f"cannot build a PDF of exactly {total_size} bytes")


def _rc4(key, data):
    s = list(range(256))
    j = 0
    for i in range(256):
        j = (j + s[i] + key[i % len(key)]) % 256
        s[i], s[j] = s[j], s[i]
    out = bytearray()
    i = j = 0
    for byte in data:
        i = (i + 1) % 256
        j = (j + s[i]) % 256
        s[i], s[j] = s[j], s[i]
        out.append(byte ^ s[(s[i] + s[j]) % 256])
    return bytes(out)


_PDF_PAD = bytes.fromhex("28BF4E5E4E758A4164004E56FFFA01082E2E00B6D0683E802F0CA9FE6453697A")


def make_encrypted_pdf(lines, user_password="e2e-user-secret", owner_password="e2e-owner-secret"):
    """A password-protected text PDF (Standard security handler, V1/R2, 40-bit RC4) whose USER password is
    not empty, so it cannot be opened without the password. The passwords are synthetic test strings."""
    def padded(pw):
        return (pw.encode("latin-1") + _PDF_PAD)[:32]
    file_id = hashlib.md5(b"aias-e2e-synthetic-encrypted").digest()
    permissions = -4
    o_value = _rc4(hashlib.md5(padded(owner_password)).digest()[:5], padded(user_password))
    key = hashlib.md5(padded(user_password) + o_value + struct.pack("<i", permissions) + file_id).digest()[:5]
    u_value = _rc4(key, _PDF_PAD)

    def encrypt(number, data):
        return _rc4(hashlib.md5(key + struct.pack("<I", number)[:3] + b"\x00\x00").digest()[:10], data)
    objects = _text_objects(lines, encrypt)
    objects.append(b"<< /Filter /Standard /V 1 /R 2 /O <" + o_value.hex().encode() + b"> /U <"
                   + u_value.hex().encode() + b"> /P " + str(permissions).encode() + b" >>")
    return _pdf(objects, b"/Encrypt 6 0 R /ID [<" + file_id.hex().encode() + b"> <" + file_id.hex().encode()
                + b">] ")


def make_scanned_pdf(lines, scale=3, margin=12):
    """A PDF with NO text layer: one page showing ``lines`` as a grayscale image (a scanned document)."""
    width, height, pixels = _render(lines, scale, margin)
    image = zlib.compress(b"".join(bytes(row) for row in pixels), 9)
    content = f"q {width} 0 0 {height} 40 {800 - height} cm /Im1 Do Q".encode()
    return _pdf([
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Resources << /XObject << /Im1 4 0 R >> >> "
        b"/Contents 5 0 R >>",
        (f"<< /Type /XObject /Subtype /Image /Width {width} /Height {height} /ColorSpace /DeviceGray "
         f"/BitsPerComponent 8 /Filter /FlateDecode /Length {len(image)} >>\nstream\n").encode() + image
        + b"\nendstream",
        b"<< /Length " + str(len(content)).encode() + b" >>\nstream\n" + content + b"\nendstream",
    ])


def make_damaged_pdf():
    """Starts with the PDF signature, then holds no object, no xref and no trailer (a damaged PDF)."""
    return b"%PDF-1.4\n" + b"SYNTHETIC DAMAGED PDF - the body of this file was cut off " * 8 + b"\n"


def _zip(entries):
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as archive:
        for name, text in entries:
            archive.writestr(zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0)), text)
    return buffer.getvalue()


_CT = "http://schemas.openxmlformats.org/package/2006/content-types"
_REL = "http://schemas.openxmlformats.org/package/2006/relationships"
_ODR = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
_SML = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"


def _xml_escape(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")


def make_xlsx(rows, sheet="Grades"):
    """A minimal .xlsx (one sheet, inline-string cells) holding ``rows`` (a list of lists of text)."""
    cells = []
    for r, row in enumerate(rows, start=1):
        cs = "".join(f'<c r="{chr(65 + c)}{r}" t="inlineStr"><is><t>{_xml_escape(v)}</t></is></c>'
                     for c, v in enumerate(row))
        cells.append(f'<row r="{r}">{cs}</row>')
    return _zip([
        ("[Content_Types].xml",
         f'<?xml version="1.0" encoding="UTF-8"?><Types xmlns="{_CT}">'
         '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
         '<Default Extension="xml" ContentType="application/xml"/>'
         '<Override PartName="/xl/workbook.xml" '
         'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
         '<Override PartName="/xl/worksheets/sheet1.xml" '
         'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>'),
        ("_rels/.rels",
         f'<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="{_REL}"><Relationship Id="rId1" '
         f'Type="{_ODR}/officeDocument" Target="xl/workbook.xml"/></Relationships>'),
        ("xl/workbook.xml",
         f'<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="{_SML}" xmlns:r="{_ODR}"><sheets>'
         f'<sheet name="{_xml_escape(sheet)}" sheetId="1" r:id="rId1"/></sheets></workbook>'),
        ("xl/_rels/workbook.xml.rels",
         f'<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="{_REL}"><Relationship Id="rId1" '
         f'Type="{_ODR}/worksheet" Target="worksheets/sheet1.xml"/></Relationships>'),
        ("xl/worksheets/sheet1.xml",
         f'<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="{_SML}"><sheetData>{"".join(cells)}'
         '</sheetData></worksheet>'),
    ])


def make_damaged_xlsx():
    """A ZIP that names xl/workbook.xml (so it is detected as .xlsx) whose parts are not a workbook."""
    return _zip([("[Content_Types].xml", "<Types"), ("xl/workbook.xml", "SYNTHETIC DAMAGED WORKBOOK <<<")])


def make_docx(text="SYNTHETIC LETTER - not a real document"):
    """A minimal .docx (a ZIP holding word/document.xml) — a format Document Access does not read."""
    w = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    return _zip([
        ("[Content_Types].xml",
         f'<?xml version="1.0" encoding="UTF-8"?><Types xmlns="{_CT}">'
         '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
         '<Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" '
         'ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>'
         '</Types>'),
        ("_rels/.rels",
         f'<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="{_REL}"><Relationship Id="rId1" '
         f'Type="{_ODR}/officeDocument" Target="word/document.xml"/></Relationships>'),
        ("word/document.xml",
         f'<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="{w}"><w:body><w:p><w:r><w:t>'
         f'{_xml_escape(text)}</w:t></w:r></w:p></w:body></w:document>'),
    ])


# ---- .xls (BIFF8 inside an OLE2 compound file) -----------------------------------------------------
def _biff(sid, data=b""):
    return struct.pack("<HH", sid, len(data)) + data


def _biff_text(text):
    raw = text.encode("latin-1")
    return struct.pack("<HB", len(raw), 0) + raw          # 16-bit length, compressed (8-bit) characters


def _xls_workbook_stream(rows, sheet):
    strings = [v for row in rows for v in row]
    sst = struct.pack("<II", len(strings), len(strings)) + b"".join(_biff_text(v) for v in strings)
    bof_globals = _biff(0x0809, struct.pack("<HHHHII", 0x0600, 0x0005, 0x0DBB, 0x07CC, 0, 0x0006))
    font = _biff(0x0031, struct.pack("<HHHHHBBBB", 200, 0, 0x7FFF, 400, 0, 0, 0, 0, 0)
                 + struct.pack("<B", 5) + b"\x00" + b"Arial")
    xf_style = struct.pack("<HHHBBBBIIH", 0, 0, 0xFFF5, 0x20, 0, 0, 0, 0, 0, 0x20C0)
    xf_cell = struct.pack("<HHHBBBBIIH", 0, 0, 0x0001, 0x20, 0, 0, 0, 0, 0, 0x20C0)
    globals_tail = [_biff(0x0042, struct.pack("<H", 1252)), _biff(0x003D, struct.pack("<HHHHHHHHH", 0, 0, 0x4000,
                    0x2000, 0x38, 0, 0, 1, 0x258)), font, font, font, font]
    globals_tail += [_biff(0x00E0, xf_style)] * 15 + [_biff(0x00E0, xf_cell)]
    boundsheet_index = None

    def assemble(sheet_offset):
        body = bof_globals + b"".join(globals_tail)
        bs = _biff(0x0085, struct.pack("<IBB", sheet_offset, 0, 0) + struct.pack("<B", len(sheet)) + b"\x00"
                   + sheet.encode("latin-1"))
        return body + bs + _biff(0x00FC, sst) + _biff(0x000A)
    globals_len = len(assemble(0))
    sheet_stream = _biff(0x0809, struct.pack("<HHHHII", 0x0600, 0x0010, 0x0DBB, 0x07CC, 0, 0x0006))
    sheet_stream += _biff(0x0200, struct.pack("<IIHHH", 0, len(rows), 0, max(len(r) for r in rows), 0))
    index = 0
    for r, row in enumerate(rows):
        sheet_stream += _biff(0x0208, struct.pack("<HHHHHHI", r, 0, len(row), 0x0F, 0, 0, 0x000F0100))
    for r, row in enumerate(rows):
        for c, _ in enumerate(row):
            sheet_stream += _biff(0x00FD, struct.pack("<HHHI", r, c, 15, index))
            index += 1
    sheet_stream += _biff(0x023E, struct.pack("<HHHH", 0x06B6, 0, 0, 0x40) + b"\x00" * 10)
    sheet_stream += _biff(0x000A)
    del boundsheet_index
    return assemble(globals_len) + sheet_stream


def make_xls(rows, sheet="Grades"):
    """A minimal .xls: a BIFF8 workbook stream (globals + one sheet of LABELSST cells) in an OLE2 compound
    file (512-byte sectors; the stream padded to 4096 bytes so it lives in regular sectors, not the
    mini stream)."""
    stream = _xls_workbook_stream(rows, sheet)
    size = len(stream)
    if size < 4096:
        stream += b"\x00" * (4096 - size)          # bytes after the sheet's EOF record are never parsed
        size = len(stream)
    sector = 512
    stream_sectors = (size + sector - 1) // sector
    stream += b"\x00" * (stream_sectors * sector - len(stream))
    # layout: [stream sectors][directory sector][FAT sector]
    dir_sector = stream_sectors
    fat_sector = stream_sectors + 1
    ENDOFCHAIN, FATSECT, FREESECT = 0xFFFFFFFE, 0xFFFFFFFD, 0xFFFFFFFF
    fat = [i + 1 for i in range(stream_sectors - 1)] + [ENDOFCHAIN, ENDOFCHAIN, FATSECT]
    fat += [FREESECT] * (128 - len(fat))
    fat_bytes = struct.pack("<128I", *fat)

    def entry(name, kind, start, length, child=0xFFFFFFFF, color=1):
        encoded = (name + "\x00").encode("utf-16-le")
        return (encoded.ljust(64, b"\x00") + struct.pack("<HBB", len(encoded), kind, color)
                + struct.pack("<III", 0xFFFFFFFF, 0xFFFFFFFF, child) + b"\x00" * 16 + struct.pack("<I", 0)
                + b"\x00" * 16 + struct.pack("<IIII", start, length, 0, 0)[:8] + struct.pack("<I", 0))
    root = entry("Root Entry", 5, ENDOFCHAIN, 0, child=1)
    book = entry("Workbook", 2, 0, size)
    empty = b"\x00" * 64 + struct.pack("<HBB", 0, 0, 0) + struct.pack("<III", 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF) \
        + b"\x00" * 48
    directory = root + book + empty + empty
    header = (bytes.fromhex("D0CF11E0A1B11AE1") + b"\x00" * 16 + struct.pack("<HHHHH", 0x003E, 0x0003, 0xFFFE, 9, 6)
              + b"\x00" * 6 + struct.pack("<IIIIIIIII", 0, 1, dir_sector, 0, 4096, ENDOFCHAIN, 0, ENDOFCHAIN, 0)
              + struct.pack("<109I", fat_sector, *([FREESECT] * 108)))
    return header + stream + directory + fat_bytes
