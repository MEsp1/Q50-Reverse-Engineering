#!/usr/bin/env bash
#
# Записать BUILD-PROVENANCE.txt: из чего именно собраны опубликованные APK и
# EPK. Файл генерируется сборкой и вручную не правится.
#
# Значения берутся из готовых артефактов (artifact/) и из окружения CI.
# Запускать после verify.sh и build-epk.sh.
#
set -euo pipefail

cd "$(dirname "$0")"

VERSION="0.7"
OUT_DIR="artifact"
APK_NAME="Q50-GTR-Plus-v$VERSION.apk"
EPK_NAME="Q50-GTR-Plus-v$VERSION.epk"
OUT="$OUT_DIR/BUILD-PROVENANCE.txt"

die() { printf '\033[31mОшибка: %s\033[0m\n' "$*" >&2; exit 1; }

for f in "$APK_NAME" "$APK_NAME.sha256" "$EPK_NAME" "$EPK_NAME.sha256" \
         "apk-signer-cert.sha256" "epk-info.txt"; do
    [ -f "$OUT_DIR/$f" ] || die "нет $OUT_DIR/$f — сначала verify.sh и build-epk.sh"
done

field() {
    # Достать значение поля из epk-info.txt: "  version       = 2"
    grep -m1 "^  $1 " "$OUT_DIR/epk-info.txt" | sed 's/.*= *//' | awk '{print $1}'
}

APK_SHA=$(cut -d' ' -f1 "$OUT_DIR/$APK_NAME.sha256")
EPK_SHA=$(cut -d' ' -f1 "$OUT_DIR/$EPK_NAME.sha256")
SIGNER_SHA=$(cat "$OUT_DIR/apk-signer-cert.sha256")
SIGNING_MODE=$(cat "$OUT_DIR/signing-mode.txt" 2>/dev/null || echo unknown)

EPK_VERSION=$(field version)
EPK_TYPE=$(field payloadType)
EPK_BLOCKS=$(field blockCount)
EPK_KEYSIZE=$(field keySize)
EPK_INNER=$(grep -m1 "^  #0 " "$OUT_DIR/epk-info.txt" | awk '{print $2}')

cat > "$OUT" <<EOF
Q50 GTR+ build provenance
=========================

repository                  ${GITHUB_REPOSITORY:-doudineugene-dot/Q50-gtr-}
version                     $VERSION
commit SHA                  ${GITHUB_SHA:-(local build)}
tag SHA                     ${PROV_TAG_SHA:-(not a tagged build)}
workflow run ID             ${GITHUB_RUN_ID:-(local build)}

APK filename                $APK_NAME
APK SHA-256                 $APK_SHA
APK size                    $(stat -c%s "$OUT_DIR/$APK_NAME") bytes
APK signing mode            $SIGNING_MODE
APK signer cert SHA-256     $SIGNER_SHA

EPK filename                $EPK_NAME
EPK SHA-256                 $EPK_SHA
EPK size                    $(stat -c%s "$OUT_DIR/$EPK_NAME") bytes
EPK inner filename          $EPK_INNER
EPK version                 $EPK_VERSION
EPK payloadType             $EPK_TYPE
EPK blockCount              $EPK_BLOCKS
EPK keySize                 $EPK_KEYSIZE

build timestamp UTC         $(date -u +"%Y-%m-%dT%H:%M:%SZ")

Note: the EPK container layout is verified structurally. Compatibility of
keys/obu_cert.pem with this specific Infiniti Q50 2017 DCU is NOT verified.
EOF

cat "$OUT"
