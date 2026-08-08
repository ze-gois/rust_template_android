# env.zsh

export ZEGOIS_ANDROID_ROOT="$(
    cd -- "$(dirname -- "${(%):-%N}")" &&
    pwd
)"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

export ANDROID_MIN_API="${ANDROID_MIN_API:-23}"

export ANDROID_NDK="$(
    find "$ANDROID_HOME/ndk" \
        -mindepth 1 \
        -maxdepth 1 \
        -type d \
        -print |
    sort -V |
    tail -n 1
)"

export ANDROID_TOOLCHAIN="$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64"

export ANDROID_AARCH64_LINKER="$ANDROID_TOOLCHAIN/bin/aarch64-linux-android${ANDROID_MIN_API}-clang"
export ANDROID_X86_64_LINKER="$ANDROID_TOOLCHAIN/bin/x86_64-linux-android${ANDROID_MIN_API}-clang"

export ANDROID_PLATFORM="$(
    find "$ANDROID_HOME/platforms" \
        -mindepth 1 \
        -maxdepth 1 \
        -type d \
        -print |
    sort -V |
    tail -n 1
)"

export ANDROID_JAR="$ANDROID_PLATFORM/android.jar"

export ANDROID_BUILD_TOOLS="$(
    find "$ANDROID_HOME/build-tools" \
        -mindepth 1 \
        -maxdepth 1 \
        -type d \
        -print |
    sort -V |
    tail -n 1
)"

export AAPT2="$ANDROID_BUILD_TOOLS/aapt2"
export D8="$ANDROID_BUILD_TOOLS/d8"
export ZIPALIGN="$ANDROID_BUILD_TOOLS/zipalign"
export APKSIGNER="$ANDROID_BUILD_TOOLS/apksigner"

export RUST_ROOT="$ZEGOIS_ANDROID_ROOT/rust"
export ANDROID_ROOT="$ZEGOIS_ANDROID_ROOT/android"
export BUILD_ROOT="$ZEGOIS_ANDROID_ROOT/build"
export SCRIPTS_ROOT="$ZEGOIS_ANDROID_ROOT/scripts"

export PATH="$ANDROID_HOME/platform-tools:$ANDROID_BUILD_TOOLS:$PATH"

echo "zegois_android environment"
echo "  root:        $ZEGOIS_ANDROID_ROOT"
echo "  rust:        $RUST_ROOT"
echo "  android:     $ANDROID_ROOT"
echo "  build:       $BUILD_ROOT"
echo "  SDK:         $ANDROID_HOME"
echo "  NDK:         $ANDROID_NDK"
echo "  platform:    $ANDROID_PLATFORM"
echo "  build-tools: $ANDROID_BUILD_TOOLS"
echo "  min API:     $ANDROID_MIN_API"
echo "  linker:      $ANDROID_AARCH64_LINKER"

test -f "$ANDROID_JAR" && echo "android.jar OK"
test -x "$ANDROID_AARCH64_LINKER" && echo "linker OK"
test -x "$AAPT2" && echo "aapt2 OK"
test -x "$D8" && echo "d8 OK"
test -x "$ZIPALIGN" && echo "zipalign OK"
test -x "$APKSIGNER" && echo "apksigner OK"
