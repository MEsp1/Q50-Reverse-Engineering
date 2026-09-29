#!/usr/bin/env bash
#
# Проверки готового APK: подпись, badging, содержимое, контрольная сумма.
# Складывает Q50-GTR-Plus-v0.7.apk, .sha256, signature.txt и badging.txt
# в каталог artifact/.
#
set -euo pipefail

cd "$(dirname "$0")"

APK="${1:-build/Q50-GTR-Plus-v0.7.apk}"
MIN_SDK=9
OUT_DIR="artifact"

[ -f "$APK" ] || { echo "Нет APK: $APK" >&2; exit 1; }

say() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }
fail() { printf '\033[31mПРОВАЛ: %s\033[0m\n' "$*" >&2; exit 1; }

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"
APK_NAME=$(basename "$APK")
cp "$APK" "$OUT_DIR/$APK_NAME"
APK="$OUT_DIR/$APK_NAME"

# ------------------------------------------------------------------ подпись

say "Подпись"
# Предупреждения apksigner идут в stderr; без 2>&1 проверки ниже были бы слепы.
apksigner verify --min-sdk-version "$MIN_SDK" --verbose --print-certs "$APK" 2>&1 \
    | tee "$OUT_DIR/signature.txt"

grep -q "Verified using v1 scheme (JAR signing): true" "$OUT_DIR/signature.txt" \
    || fail "нет подписи v1 (JAR signing) — этот DCU проверяет именно её"
grep -q "Verified using v2 scheme (APK Signature Scheme v2): false" "$OUT_DIR/signature.txt" \
    || fail "включена подпись v2 — для Android 2.3 она лишняя и не ожидается"
grep -q "Verified using v3 scheme (APK Signature Scheme v3): false" "$OUT_DIR/signature.txt" \
    || fail "включена подпись v3 — для Android 2.3 она лишняя и не ожидается"

# AGP-подобные маркеры под META-INF/ подписью не покрываются и Android их
# игнорирует; всё остальное вне подписи — реальная проблема.
if grep "not protected by signature" "$OUT_DIR/signature.txt" | grep -qv "WARNING: META-INF/"; then
    fail "часть содержимого APK не покрыта подписью v1"
fi

# Отпечаток сертификата подписи — то, по чему будущие релизы узнают, что ключ
# не подменили. Кладём его отдельным файлом, чтобы не разбирать signature.txt
# в каждом следующем шаге.
SIGNER_DN=$(grep -m1 "^Signer #1 certificate DN:" "$OUT_DIR/signature.txt" \
    | sed 's/^Signer #1 certificate DN: //')
SIGNER_SHA=$(grep -m1 "^Signer #1 certificate SHA-256 digest:" "$OUT_DIR/signature.txt" \
    | sed 's/^Signer #1 certificate SHA-256 digest: //' | tr -d '[:space:]')

[ -n "$SIGNER_SHA" ] || fail "не удалось прочитать SHA-256 сертификата подписи"

printf '%s\n' "$SIGNER_SHA" > "$OUT_DIR/apk-signer-cert.sha256"

# Режим подписи объявляет build.sh; сюда он доезжает файлом.
SIGNING_MODE=$(cat build/signing-mode.txt 2>/dev/null || echo "unknown")
printf '%s\n' "$SIGNING_MODE" > "$OUT_DIR/signing-mode.txt"

echo
echo "SIGNING_MODE=$SIGNING_MODE"
echo "Signer certificate DN:         $SIGNER_DN"
echo "Signer certificate SHA-256:    $SIGNER_SHA"

# ------------------------------------------------------------------ badging

say "aapt dump badging"
aapt dump badging "$APK" > "$OUT_DIR/badging.txt"

grep -E "^package:|^sdkVersion:|^targetSdkVersion:|^application-label:|^uses-permission:|^launchable-activity:" \
    "$OUT_DIR/badging.txt" || true

grep -q "^sdkVersion:'9'" "$OUT_DIR/badging.txt" \
    || fail "sdkVersion не 9: $(grep '^sdkVersion' "$OUT_DIR/badging.txt" || echo 'отсутствует')"
grep -q "^targetSdkVersion:'10'" "$OUT_DIR/badging.txt" \
    || fail "targetSdkVersion не 10: $(grep '^targetSdkVersion' "$OUT_DIR/badging.txt" || echo 'отсутствует')"
grep -q "com.ygomi.permission.IVI_CAN_READ" "$OUT_DIR/badging.txt" \
    || fail "в APK нет разрешения com.ygomi.permission.IVI_CAN_READ"

# --------------------------------------------------------------- содержимое

say "Содержимое APK"
unzip -l "$APK" | tee "$OUT_DIR/contents.txt" | grep -E "classes.dex|resources.arsc|AndroidManifest" || true

grep -qE "[[:space:]]classes\.dex$" "$OUT_DIR/contents.txt" \
    || fail "в APK нет classes.dex"

# Проект чисто на framework API, нативного кода в нём быть не должно.
if grep -qE "[[:space:]]lib/.*\.so$" "$OUT_DIR/contents.txt"; then
    fail "в APK есть нативные библиотеки, хотя они не нужны"
fi
echo "нативных библиотек нет — ок"
rm -f "$OUT_DIR/contents.txt"

# ------------------------------------------------------------------- sha256

say "SHA-256"
( cd "$OUT_DIR" && sha256sum "$APK_NAME" > "$APK_NAME.sha256" && cat "$APK_NAME.sha256" )
( cd "$OUT_DIR" && sha256sum -c "$APK_NAME.sha256" )

say "Артефакт"
ls -l "$OUT_DIR"
