#!/usr/bin/env zsh
set -euo pipefail

SCRIPT_DIR="${0:A:h}"
PROJECT_ROOT="${SCRIPT_DIR:h}"

source "$PROJECT_ROOT/env.zsh"

export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$ANDROID_AARCH64_LINKER"

TARGET="aarch64-linux-android"
TARGET_API="35"
PACKAGE_NAME="com.zegois.demo"
ACTIVITY_NAME="${PACKAGE_NAME}.MainActivity"
LIBRARY_NAME="zegois_android"

# JAVA_SOURCE="$ANDROID_ROOT/app/src/main/java/com/zegois/demo/MainActivity.java"
JAVA_ROOT="$ANDROID_ROOT/app/src/main/java"
MANIFEST="$ANDROID_ROOT/app/src/main/AndroidManifest.xml"
RUST_LIBRARY="$RUST_ROOT/target/$TARGET/debug/lib${LIBRARY_NAME}.so"
ANDROID_LIBRARY="$ANDROID_ROOT/app/src/main/jniLibs/arm64-v8a/lib${LIBRARY_NAME}.so"
DEBUG_KEYSTORE="$HOME/.android/debug.keystore"

CLASSES_ROOT="$BUILD_ROOT/classes"
DEX_ROOT="$BUILD_ROOT/dex"
APK_ROOT="$BUILD_ROOT/apk"
BASE_APK="$APK_ROOT/base-unsigned.apk"
UNSIGNED_APK="$APK_ROOT/unsigned.apk"
ALIGNED_APK="$APK_ROOT/aligned-unsigned.apk"
DEBUG_APK="$APK_ROOT/debug.apk"

fail() {
    print -u2 "build.zsh: $*"
    exit 1
}

require_file() {
    [[ -f "$1" ]] || fail "arquivo não encontrado: $1"
}

require_command() {
    (( $+commands[$1] )) || fail "comando não encontrado: $1"
}

require_command cargo
require_command javac
require_command zip
# require_file "$JAVA_SOURCE"
require_file "$MANIFEST"
[[ -d "$JAVA_ROOT" ]] || fail "diretório Java não encontrado: $JAVA_ROOT"
[[ -x "$AAPT2" ]] || fail "aapt2 não executável: $AAPT2"
[[ -x "$D8" ]] || fail "d8 não executável: $D8"
[[ -x "$ZIPALIGN" ]] || fail "zipalign não executável: $ZIPALIGN"
[[ -x "$APKSIGNER" ]] || fail "apksigner não executável: $APKSIGNER"
[[ -f "$ANDROID_JAR" ]] || fail "android.jar não encontrado: $ANDROID_JAR"
[[ -f "$DEBUG_KEYSTORE" ]] || fail "keystore de desenvolvimento não encontrado: $DEBUG_KEYSTORE"
print "== Rust: cargo build =="
(cd "$RUST_ROOT" && cargo build --target "$TARGET")
require_file "$RUST_LIBRARY"

print "== Copiando biblioteca JNI =="
mkdir -p "${ANDROID_LIBRARY:h}"
cp "$RUST_LIBRARY" "$ANDROID_LIBRARY"

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

print "Classes produzidas:"
find "$CLASSES_ROOT" -type f -name '*.class' -print

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

cp "$BASE_APK" "$UNSIGNED_APK"
(
    cd "$DEX_ROOT"
    zip -q -0 "$UNSIGNED_APK" classes.dex
)

mkdir -p "$APK_ROOT/lib/arm64-v8a"
cp "$ANDROID_LIBRARY" "$APK_ROOT/lib/arm64-v8a/lib${LIBRARY_NAME}.so"
(
    cd "$APK_ROOT"
    zip -q -0 "$UNSIGNED_APK" lib/arm64-v8a/lib${LIBRARY_NAME}.so
)

print "== APK: zipalign =="
"$ZIPALIGN" -P 16 -f 4 "$UNSIGNED_APK" "$ALIGNED_APK"

print "== APK: apksigner =="
"$APKSIGNER" sign \
    --ks "$DEBUG_KEYSTORE" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$DEBUG_APK" \
    "$ALIGNED_APK"

print "== Verificando APK =="
"$ZIPALIGN" -c -P 16 -v 4 "$DEBUG_APK"
"$APKSIGNER" verify --verbose "$DEBUG_APK"
"$AAPT2" dump badging "$DEBUG_APK"

print ""
print "APK criado: $DEBUG_APK"
print "Activity: $ACTIVITY_NAME"
print "JNI: Java_com_zegois_demo_MainActivity_answer"

adb install -r "$BUILD_ROOT/apk/debug.apk"
adb shell am force-stop com.zegois.demo
adb shell am start -n com.zegois.demo/.MainActivity
