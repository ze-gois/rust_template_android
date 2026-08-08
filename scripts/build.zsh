#!/usr/bin/env zsh
set -euo pipefail

SCRIPT_DIR="${0:A:h}"
PROJECT_ROOT="${SCRIPT_DIR:h}"

source "$PROJECT_ROOT/env.zsh"

TARGET_API="35"

PACKAGE_NAME="com.zegois.demo"
ACTIVITY_NAME="${PACKAGE_NAME}.MainActivity"
LIBRARY_NAME="zegois_android"

JAVA_ROOT="$ANDROID_ROOT/app/src/main/java"
MANIFEST="$ANDROID_ROOT/app/src/main/AndroidManifest.xml"
DEBUG_KEYSTORE="$HOME/.android/debug.keystore"

CLASSES_ROOT="$BUILD_ROOT/classes"
DEX_ROOT="$BUILD_ROOT/dex"
APK_ROOT="$BUILD_ROOT/apk"

BASE_APK="$APK_ROOT/base-unsigned.apk"
UNSIGNED_APK="$APK_ROOT/unsigned.apk"
ALIGNED_APK="$APK_ROOT/aligned-unsigned.apk"
DEBUG_APK="$APK_ROOT/debug.apk"


# -----------------------------------------------------------------------------
# ABIs
# -----------------------------------------------------------------------------

ABI_CONFIGS=(
    "aarch64-linux-android|arm64-v8a|$ANDROID_AARCH64_LINKER"
    "x86_64-linux-android|x86_64|$ANDROID_X86_64_LINKER"
)


# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------

fail() {
    print -u2 "build.zsh: $*"
    exit 1
}

require_file() {
    [[ -f "$1" ]] ||
        fail "arquivo não encontrado: $1"
}

require_command() {
    (( $+commands[$1] )) ||
        fail "comando não encontrado: $1"
}


# -----------------------------------------------------------------------------
# Preflight
# -----------------------------------------------------------------------------

require_command cargo
require_command javac
require_command zip
require_command unzip
require_command file
require_command adb
require_command awk

require_file "$MANIFEST"

[[ -d "$JAVA_ROOT" ]] ||
    fail "diretório Java não encontrado: $JAVA_ROOT"

[[ -x "$AAPT2" ]] ||
    fail "aapt2 não executável: $AAPT2"

[[ -x "$D8" ]] ||
    fail "d8 não executável: $D8"

[[ -x "$ZIPALIGN" ]] ||
    fail "zipalign não executável: $ZIPALIGN"

[[ -x "$APKSIGNER" ]] ||
    fail "apksigner não executável: $APKSIGNER"

[[ -f "$ANDROID_JAR" ]] ||
    fail "android.jar não encontrado: $ANDROID_JAR"

[[ -f "$DEBUG_KEYSTORE" ]] ||
    fail "keystore de desenvolvimento não encontrado: $DEBUG_KEYSTORE"


# -----------------------------------------------------------------------------
# Rust / JNI
# -----------------------------------------------------------------------------

print ""
print "== Rust / JNI =="

for CONFIG in "${ABI_CONFIGS[@]}"; do
    IFS='|' read -r TARGET ABI LINKER <<< "$CONFIG"

    LINKER_ENV="CARGO_TARGET_${${TARGET:u}//-/_}_LINKER"

    export "$LINKER_ENV=$LINKER"

    RUST_LIBRARY="$RUST_ROOT/target/$TARGET/debug/lib${LIBRARY_NAME}.so"
    ANDROID_LIBRARY="$ANDROID_ROOT/app/src/main/jniLibs/$ABI/lib${LIBRARY_NAME}.so"

    print ""
    print "== ABI:    $ABI"
    print "   target: $TARGET"
    print "   linker: $LINKER"

    (
        cd "$RUST_ROOT"

        cargo build \
            --target "$TARGET"
    )

    require_file "$RUST_LIBRARY"

    mkdir -p "${ANDROID_LIBRARY:h}"

    cp \
        "$RUST_LIBRARY" \
        "$ANDROID_LIBRARY"

    print "   ELF:"
    file "$ANDROID_LIBRARY"
done


# -----------------------------------------------------------------------------
# Java
# -----------------------------------------------------------------------------

print ""
print "== Java: javac =="

rm -rf "$CLASSES_ROOT"
mkdir -p "$CLASSES_ROOT"

JAVA_SOURCES=(
    "$JAVA_ROOT"/**/*.java(N)
)

(( ${#JAVA_SOURCES[@]} > 0 )) ||
    fail "nenhum fonte Java encontrado em $JAVA_ROOT"

javac \
    -source 8 \
    -target 8 \
    -bootclasspath "$ANDROID_JAR" \
    -d "$CLASSES_ROOT" \
    "${JAVA_SOURCES[@]}"

print ""
print "Classes produzidas:"

find \
    "$CLASSES_ROOT" \
    -type f \
    -name '*.class' \
    -print


# -----------------------------------------------------------------------------
# DEX
# -----------------------------------------------------------------------------

print ""
print "== Java: d8 =="

rm -rf "$DEX_ROOT"
mkdir -p "$DEX_ROOT"

CLASS_FILES=(
    "$CLASSES_ROOT"/**/*.class(N)
)

(( ${#CLASS_FILES[@]} > 0 )) ||
    fail "nenhum .class produzido pelo javac"

"$D8" \
    --min-api "$ANDROID_MIN_API" \
    --lib "$ANDROID_JAR" \
    --output "$DEX_ROOT" \
    "${CLASS_FILES[@]}"

require_file "$DEX_ROOT/classes.dex"


# -----------------------------------------------------------------------------
# Base APK
# -----------------------------------------------------------------------------

print ""
print "== Android: aapt2 link =="

rm -rf "$APK_ROOT"
mkdir -p "$APK_ROOT"

"$AAPT2" link \
    -o "$BASE_APK" \
    --manifest "$MANIFEST" \
    -I "$ANDROID_JAR" \
    --min-sdk-version "$ANDROID_MIN_API" \
    --target-sdk-version "$TARGET_API" \
    --version-code 1 \
    --version-name 0.1.0

require_file "$BASE_APK"

cp \
    "$BASE_APK" \
    "$UNSIGNED_APK"


# -----------------------------------------------------------------------------
# classes.dex
# -----------------------------------------------------------------------------

print ""
print "== Adicionando classes.dex =="

(
    cd "$DEX_ROOT"

    zip \
        -q \
        -0 \
        "$UNSIGNED_APK" \
        classes.dex
)


# -----------------------------------------------------------------------------
# JNI libraries
# -----------------------------------------------------------------------------

print ""
print "== Adicionando bibliotecas JNI =="

for CONFIG in "${ABI_CONFIGS[@]}"; do
    IFS='|' read -r TARGET ABI LINKER <<< "$CONFIG"

    ANDROID_LIBRARY="$ANDROID_ROOT/app/src/main/jniLibs/$ABI/lib${LIBRARY_NAME}.so"

    APK_LIBRARY_DIR="$APK_ROOT/lib/$ABI"
    APK_LIBRARY="$APK_LIBRARY_DIR/lib${LIBRARY_NAME}.so"

    require_file "$ANDROID_LIBRARY"

    mkdir -p "$APK_LIBRARY_DIR"

    cp \
        "$ANDROID_LIBRARY" \
        "$APK_LIBRARY"

    (
        cd "$APK_ROOT"

        zip \
            -q \
            -0 \
            "$UNSIGNED_APK" \
            "lib/$ABI/lib${LIBRARY_NAME}.so"
    )
done


# -----------------------------------------------------------------------------
# Inspect unsigned APK
# -----------------------------------------------------------------------------

print ""
print "== Bibliotecas nativas no APK =="

unzip -l "$UNSIGNED_APK" |
    grep "lib${LIBRARY_NAME}\.so"


# -----------------------------------------------------------------------------
# zipalign
# -----------------------------------------------------------------------------

print ""
print "== APK: zipalign =="

"$ZIPALIGN" \
    -P 16 \
    -f \
    4 \
    "$UNSIGNED_APK" \
    "$ALIGNED_APK"

require_file "$ALIGNED_APK"


# -----------------------------------------------------------------------------
# Signing
# -----------------------------------------------------------------------------

print ""
print "== APK: apksigner =="

"$APKSIGNER" sign \
    --ks "$DEBUG_KEYSTORE" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$DEBUG_APK" \
    "$ALIGNED_APK"

require_file "$DEBUG_APK"


# -----------------------------------------------------------------------------
# Verification
# -----------------------------------------------------------------------------

print ""
print "== Verificando APK =="

"$ZIPALIGN" \
    -c \
    -P 16 \
    -v \
    4 \
    "$DEBUG_APK"

"$APKSIGNER" \
    verify \
    --verbose \
    "$DEBUG_APK"

"$AAPT2" \
    dump badging \
    "$DEBUG_APK"


# -----------------------------------------------------------------------------
# Result
# -----------------------------------------------------------------------------

print ""
print "============================================================"
print "APK criado"
print "============================================================"
print ""
print "APK:      $DEBUG_APK"
print "Package:  $PACKAGE_NAME"
print "Activity: $ACTIVITY_NAME"
print "JNI:      Java_com_zegois_demo_MainActivity_answer"


# -----------------------------------------------------------------------------
# ADB devices
# -----------------------------------------------------------------------------

print ""
print "== Dispositivos ADB =="

DEVICES=(
    "${(@f)$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')}"
)

(( ${#DEVICES[@]} > 0 )) ||
    fail "nenhum dispositivo ADB conectado"

printf '  %s\n' "${DEVICES[@]}"


# -----------------------------------------------------------------------------
# Install / run on every connected device
# -----------------------------------------------------------------------------

print ""
print "== Instalando em todos os dispositivos =="

INSTALL_FAILURES=0

for DEVICE in "${DEVICES[@]}"; do
    print ""
    print "------------------------------------------------------------"
    print "ADB: $DEVICE"
    print "------------------------------------------------------------"

    print "Instalando APK..."

    if ! adb \
        -s "$DEVICE" \
        install \
        -r \
        "$DEBUG_APK"
    then
        print -u2 "falha ao instalar APK em: $DEVICE"

        (( INSTALL_FAILURES += 1 ))

        continue
    fi

    print "Parando instância anterior..."

    if ! adb \
        -s "$DEVICE" \
        shell am force-stop \
        "$PACKAGE_NAME"
    then
        print -u2 "aviso: não foi possível parar $PACKAGE_NAME em $DEVICE"
    fi

    print "Iniciando Activity..."

    if ! adb \
        -s "$DEVICE" \
        shell am start \
        -n "$PACKAGE_NAME/.MainActivity"
    then
        print -u2 "falha ao iniciar Activity em: $DEVICE"

        (( INSTALL_FAILURES += 1 ))

        continue
    fi

    print "OK: $DEVICE"
done


# -----------------------------------------------------------------------------
# Final status
# -----------------------------------------------------------------------------

print ""
print "============================================================"

if (( INSTALL_FAILURES == 0 )); then
    print "Build e deploy concluídos com sucesso."
else
    print -u2 "Build concluído, mas ocorreram $INSTALL_FAILURES falha(s) no deploy."
    exit 1
fi
