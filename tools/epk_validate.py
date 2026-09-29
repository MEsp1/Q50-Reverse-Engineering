#!/usr/bin/env python3
"""
EPK v2 structural validator — compares a built EPK against the reference
structure observed in the working gtr.epk.

Validates:
  - Magic bytes (.epk)
  - Header field sizes and encoding (big-endian)
  - Version, payload type, block count
  - Wrapped-key length (128 bytes for RSA-1024)
  - Key field padding (trailing zeros)
  - Payload block filename field (128B zero-padded ASCII)
  - Encrypted payload length alignment (multiple of 16)
  - Total file size consistency
  - Shannon entropy of encrypted payload (should be >7.5)

Usage:
    python epk_validate.py <file.epk>
"""

import struct
import sys
import math
from collections import Counter
from pathlib import Path

MAGIC = b".epk"
EXPECTED_VERSION = 2
EXPECTED_PAYLOAD_TYPE = 2
EXPECTED_BLOCK_COUNT = 1
EXPECTED_KEY_LEN = 128        # RSA-1024 output
KEY_FIELD_SIZE = 256
NAME_FIELD_SIZE = 128


def validate_epk(filepath: str) -> bool:
    """Validate an EPK file against the gtr.epk reference structure."""

    path = Path(filepath)
    if not path.exists():
        print(f"FAIL: File not found: {path}")
        return False

    data = path.read_bytes()
    errors = 0
    warnings = 0

    def ok(msg: str):
        print(f"  OK   {msg}")

    def fail(msg: str):
        nonlocal errors
        errors += 1
        print(f"  FAIL {msg}")

    def warn(msg: str):
        nonlocal warnings
        warnings += 1
        print(f"  WARN {msg}")

    print(f"Validating: {path} ({len(data):,} bytes)")
    print()

    # ── Magic ──
    if len(data) < 268:
        fail(f"File too small for EPK v2 header ({len(data)} < 268 bytes)")
        return False

    magic = data[0:4]
    if magic == MAGIC:
        ok(f"Magic: {magic!r}")
    else:
        fail(f"Magic: expected {MAGIC!r}, got {magic!r}")

    # ── Version ──
    version = struct.unpack(">H", data[4:6])[0]
    if version == EXPECTED_VERSION:
        ok(f"Version: {version}")
    else:
        fail(f"Version: expected {EXPECTED_VERSION}, got {version}")

    # ── Payload type ──
    ptype = struct.unpack(">H", data[6:8])[0]
    if ptype == EXPECTED_PAYLOAD_TYPE:
        ok(f"Payload type: {ptype}")
    else:
        warn(f"Payload type: expected {EXPECTED_PAYLOAD_TYPE}, got {ptype}")

    # ── Block count ──
    blocks = struct.unpack(">H", data[8:10])[0]
    if blocks == EXPECTED_BLOCK_COUNT:
        ok(f"Block count: {blocks}")
    else:
        warn(f"Block count: expected {EXPECTED_BLOCK_COUNT}, got {blocks}")

    # ── Wrapped-key length ──
    key_len = struct.unpack(">H", data[10:12])[0]
    if key_len == EXPECTED_KEY_LEN:
        ok(f"Wrapped-key length: {key_len} bytes (RSA-1024)")
    else:
        fail(f"Wrapped-key length: expected {EXPECTED_KEY_LEN}, got {key_len}")

    # ── Key field ──
    key_field = data[12:12 + KEY_FIELD_SIZE]
    key_data = key_field[:key_len]
    key_padding = key_field[key_len:]

    non_zero_key = sum(1 for b in key_data if b != 0)
    if non_zero_key > 0:
        ok(f"Key data: {non_zero_key}/{key_len} non-zero bytes")
    else:
        fail("Key data: all zeros (no wrapped key present)")

    if all(b == 0 for b in key_padding):
        ok(f"Key padding: {KEY_FIELD_SIZE - key_len} bytes of zeros")
    else:
        fail(f"Key padding: trailing bytes are not zero-padded")

    # ── Payload block ──
    offset = 268
    if len(data) < offset + NAME_FIELD_SIZE + 4:
        fail("File too small for payload block header")
        return False

    filename_raw = data[offset:offset + NAME_FIELD_SIZE]
    filename = filename_raw.split(b"\x00", 1)[0].decode("ascii", errors="replace")
    if filename:
        ok(f"Filename: '{filename}'")
    else:
        warn("Filename: empty")

    if filename.endswith(".apk"):
        ok("Filename ends with .apk")
    else:
        warn(f"Filename does not end with .apk: '{filename}'")

    enc_len = struct.unpack(">I", data[offset + NAME_FIELD_SIZE:offset + NAME_FIELD_SIZE + 4])[0]
    ok(f"Encrypted length: {enc_len:,} bytes")

    # ── AES alignment ──
    if enc_len % 16 == 0:
        ok("Encrypted length aligned to 16 bytes (AES block)")
    else:
        fail(f"Encrypted length not aligned to 16 bytes ({enc_len} % 16 = {enc_len % 16})")

    # ── Total size ──
    payload_start = offset + NAME_FIELD_SIZE + 4
    expected_total = payload_start + enc_len

    if expected_total == len(data):
        ok(f"Total size: {len(data):,} bytes (matches header + payload)")
    elif expected_total < len(data):
        warn(f"File has {len(data) - expected_total} trailing bytes after payload")
    else:
        fail(f"File too small: expected {expected_total:,}, got {len(data):,}")

    # ── Entropy check ──
    if len(data) >= payload_start + 1024:
        sample = data[payload_start:payload_start + 4096]
        counter = Counter(sample)
        entropy = sum(-p * math.log2(p) for p in (c / len(sample) for c in counter.values()) if p > 0)

        if entropy > 7.5:
            ok(f"Payload entropy: {entropy:.4f} (encrypted/compressed)")
        elif entropy > 6.0:
            warn(f"Payload entropy: {entropy:.4f} (possibly not encrypted)")
        else:
            fail(f"Payload entropy: {entropy:.4f} (likely plaintext — not encrypted)")

    # ── ZIP magic check ──
    if len(data) > payload_start + 4:
        if data[payload_start:payload_start + 4] == b"PK\x03\x04":
            warn("Payload starts with ZIP/APK magic — appears UNENCRYPTED")
        else:
            ok("Payload does not start with ZIP magic (encrypted as expected)")

    # ── Summary ──
    print()
    if errors == 0 and warnings == 0:
        print(f"PASSED: All checks passed for {path.name}")
    elif errors == 0:
        print(f"PASSED with {warnings} warning(s)")
    else:
        print(f"FAILED: {errors} error(s), {warnings} warning(s)")

    return errors == 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        print("Usage: python epk_validate.py <file.epk>")
        sys.exit(1)

    success = validate_epk(sys.argv[1])
    sys.exit(0 if success else 1)
