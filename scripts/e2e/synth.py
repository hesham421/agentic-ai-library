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
