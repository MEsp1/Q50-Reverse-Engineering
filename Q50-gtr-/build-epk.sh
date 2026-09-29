#!/usr/bin/env bash
#
# Упаковать собранный APK в загружаемый .epk для InTouch / DCU Gen1.
#
# ГУ (AppManager) не распознаёт голый APK — только .epk. Для сборки нужен
# ТОЛЬКО ПУБЛИЧНЫЙ сертификат OBU: в формате нет подписи, dataKey заворачивается
# открытым ключом. Закрытый ключ нужен лишь чтобы распаковать чужой .epk, и его
# в этом репозитории нет и быть не должно.
#
# Запускать после build.sh и verify.sh: artifact/ уже создан verify.sh, здесь он
# только дополняется.
#
set -euo pipefail

cd "$(dirname "$0")"

VERSION="0.7"
APK="build/Q50-GTR-Plus-v$VERSION.apk"
CERT="${Q50_OBU_CERT:-keys/obu_cert.pem}"
OUT_DIR="artifact"
EPK="$OUT_DIR/Q50-GTR-Plus-v$VERSION.epk"

# Имя внутри контейнера: короткое и ASCII. Загрузчик ГУ старый, длинное имя с
# дефисами и точками лишний раз испытывать не стоит.
INNER_NAME="q50gtr.apk"
TMP_APK="build/$INNER_NAME"

say() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }
die() { printf '\033[31mОшибка: %s\033[0m\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------- проверки

[ -f "$APK" ] || die "нет APK: $APK — сначала выполните ./build.sh"
[ -f "$CERT" ] || die "нет сертификата OBU: $CERT"

say "Python и зависимость cryptography"
command -v python3 >/dev/null 2>&1 || die "не найден python3"
if ! python3 -c 'import cryptography' 2>/dev/null; then
    echo "cryptography не установлена, ставлю через pip"
    python3 -m pip install --quiet --disable-pip-version-check cryptography \
        || die "не удалось установить cryptography"
fi
python3 -c 'import cryptography; print("cryptography", cryptography.__version__)'

# ------------------------------------------------------------------ сборка

mkdir -p "$OUT_DIR"
cp "$APK" "$TMP_APK"

say "Упаковка $TMP_APK -> $EPK"
echo "сертификат: $CERT"
python3 tools/epktool.py build "$TMP_APK" \
    -o "$EPK" \
    --cert "$CERT" \
    --type 2 \
    --name "$INNER_NAME"

rm -f "$TMP_APK"

# ----------------------------------------------------------------- разбор

say "Разбор контейнера"
python3 tools/epktool.py info "$EPK" | tee "$OUT_DIR/epk-info.txt"

# --------------------------------------------------------------- проверка

say "Строгая проверка контейнера"
# Падает, если раскладка отличается от той, что ждёт загрузчик ГУ.
python3 tools/epktool.py verify "$EPK" \
    --expect-name "$INNER_NAME" \
    --expect-type 2 \
    --expect-blocks 1 \
    --expect-key-size 128

# ------------------------------------------------------------------ sha256

say "SHA-256"
EPK_NAME=$(basename "$EPK")
( cd "$OUT_DIR" && sha256sum "$EPK_NAME" > "$EPK_NAME.sha256" && cat "$EPK_NAME.sha256" )
( cd "$OUT_DIR" && sha256sum -c "$EPK_NAME.sha256" )

say "Готово"
ls -l "$OUT_DIR"
echo
echo "Скопировать $EPK_NAME в корень USB (FAT32) и выбрать в AppManager на ГУ."
