#!/usr/bin/env python3
"""
EPK v2 builder — calibrated against Infiniti InTouch (Bosch DCU Gen1)
and the official OBU public certificate (CN=tt18002, IT5.YGOMI.COM CA).

Cryptographic model verified from InTouch DCU framework.jar (com.connexis.ivi.utils.epk.v2):
  - Magic: .epk (4 bytes)
  - Version: 2, Payload type: 2 (APK), Block count: 1
  - dataKey: 20 random ASCII bytes
  - Wrapped key: RSA-1024 / PKCS1v15 on dataKey (128 bytes)
  - Key field: 256 bytes (128B wrapped key + 128B zero padding)
  - Cipher: AES-256-CBC, key = dataKey + 12 zero bytes
  - Fixed IV: 01 23 45 67 89 ab cd ef 00*8 (16 bytes)
  - Padding: PKCS7 (1..16 bytes)
  - Inner filename: 128 bytes (ASCII, zero-padded)

Usage:
    python tools/epk_builder.py <app.apk> <obu_cert.pem> <output.epk> [--name inner_name.apk]
"""

import argparse
import hashlib
import os
from pathlib import Path
import struct
import sys
import uuid

try:
    from cryptography import x509
    from cryptography.hazmat.primitives import serialization
    from cryptography.hazmat.primitives.asymmetric import padding as asym_padding
    from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
except ImportError:
    print("ERROR: 'cryptography' package required. Install with: pip install cryptography")
    sys.exit(1)

MAGIC = b".epk"
EPK_VERSION = 2
PAYLOAD_TYPE_APK = 2
KEY_FIELD_SIZE = 256
NAME_FIELD_SIZE = 128
RSA_1024_OUTPUT = 128
DCU_FIXED_IV = bytes([0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef]) + b"\x00" * 8


def load_public_key(cert_or_key_path: Path):
    """Load RSA public key from X.509 certificate or public key PEM/DER."""
    data = cert_or_key_path.read_bytes()
    loaders = [
        lambda d: x509.load_pem_x509_certificate(d).public_key(),
        lambda d: x509.load_der_x509_certificate(d).public_key(),
        serialization.load_pem_public_key,
        serialization.load_der_public_key,
    ]
    for loader in loaders:
        try:
            return loader(data)
        except Exception:
            continue
    sys.exit(f"ERROR: Could not load RSA public key or certificate from {cert_or_key_path}")


def build_epk(apk_path: str, cert_path: str, output_path: str, inner_name: str | None = None) -> None:
    apk_file = Path(apk_path)
    cert_file = Path(cert_path)
    out_file = Path(output_path)

    if not apk_file.exists():
        sys.exit(f"ERROR: APK not found: {apk_file}")
    if not cert_file.exists():
        sys.exit(f"ERROR: Certificate/key not found: {cert_file}")

    apk_data = apk_file.read_bytes()
    print(f"APK:        {apk_file.name} ({len(apk_data):,} bytes)")
    print(f"APK SHA256: {hashlib.sha256(apk_data).hexdigest()}")

    if apk_data[:4] != b"PK\x03\x04":
        print("WARNING: Input file does not start with ZIP magic (PK).")

    # Determine inner filename
    name_str = inner_name or apk_file.name
    try:
        name_bytes = name_str.encode("ascii")
    except UnicodeEncodeError:
        sys.exit(f"ERROR: Inner filename must be ASCII: {name_str}")

    if len(name_bytes) > NAME_FIELD_SIZE:
        sys.exit(f"ERROR: Inner filename exceeds {NAME_FIELD_SIZE} bytes")

    filename_padded = name_bytes.ljust(NAME_FIELD_SIZE, b"\x00")

    # Generate 20-byte dataKey
    data_key = uuid.uuid4().hex[:20].encode("ascii")

    # AES-256 key is data_key padded with zeros to 32 bytes
    aes_key = data_key + b"\x00" * (32 - len(data_key))

    # PKCS7 padding
    pad_len = 16 - (len(apk_data) % 16)
    padded_data = apk_data + bytes([pad_len] * pad_len)

    # Encrypt with AES-256-CBC using DCU fixed IV
    cipher = Cipher(algorithms.AES(aes_key), modes.CBC(DCU_FIXED_IV))
    encryptor = cipher.encryptor()
    encrypted_payload = encryptor.update(padded_data) + encryptor.finalize()

    # Wrap data_key with RSA-1024
    pub_key = load_public_key(cert_file)
    wrapped_key = pub_key.encrypt(data_key, asym_padding.PKCS1v15())

    if len(wrapped_key) != RSA_1024_OUTPUT:
        print(f"WARNING: RSA key output is {len(wrapped_key)} bytes (expected {RSA_1024_OUTPUT} for RSA-1024).")

    wrapped_key_padded = wrapped_key + b"\x00" * (KEY_FIELD_SIZE - len(wrapped_key))

    with open(out_file, "wb") as f:
        # 268-byte header
        f.write(MAGIC)
        f.write(struct.pack(">HHHH", EPK_VERSION, PAYLOAD_TYPE_APK, 1, len(wrapped_key)))
        f.write(wrapped_key_padded)

        # Payload block
        f.write(filename_padded)
        f.write(struct.pack(">i", len(encrypted_payload)))
        f.write(encrypted_payload)

    total_size = 268 + 128 + 4 + len(encrypted_payload)
    print(f"\nEPK created: {out_file} ({total_size:,} bytes)")
    print(f"  Inner name: {name_str}")
    print(f"  EPK SHA256: {hashlib.sha256(out_file.read_bytes()).hexdigest()}")
    print("\nCopy this file to the root of a FAT32 USB drive and install via AppsManager on the IVI.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Build an InTouch DCU compatible EPK v2 package")
    parser.add_argument("apk", help="Path to signed Android APK")
    parser.add_argument("cert", help="Path to OBU X.509 certificate or RSA-1024 public key")
    parser.add_argument("output", help="Output .epk file path")
    parser.add_argument("--name", help="Inner filename inside the EPK (default: APK basename)")
    args = parser.parse_args()

    build_epk(args.apk, args.cert, args.output, args.name)
