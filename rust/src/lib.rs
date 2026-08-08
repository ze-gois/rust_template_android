// A camada nativa deliberadamente usa apenas tipos JNI primitivos.
// A Activity continua sendo dona da UI; Rust fica responsável pelo cálculo.

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_zegois_demo_MainActivity_answer(
    _env: *mut core::ffi::c_void,
    _this: *mut core::ffi::c_void,
) -> i32 {
    420
}

/// Calcula Fibonacci sem alocar memória e retorna um long Java (i64 Rust).
/// O limite 92 evita overflow de i64.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_zegois_demo_MainActivity_fibonacci(
    _env: *mut core::ffi::c_void,
    _this: *mut core::ffi::c_void,
    n: i32,
) -> i64 {
    if n < 0 {
        return 0;
    }

    if n > 92 {
        return i64::MAX;
    }

    let mut previous = 0_i64;
    let mut current = 1_i64;

    for _ in 0..n {
        let next = previous + current;
        previous = current;
        current = next;
    }

    previous
}

/// Calcula um checksum FNV-1a sobre os quatro bytes little-endian de um int.
/// O Java pode usar o resultado para identificar ou validar dados.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_zegois_demo_MainActivity_checksum(
    _env: *mut core::ffi::c_void,
    _this: *mut core::ffi::c_void,
    value: i32,
) -> i32 {
    let mut hash = 0x811c9dc5_u32;

    for byte in value.to_le_bytes() {
        hash ^= u32::from(byte);
        hash = hash.wrapping_mul(0x01000193);
    }

    hash as i32
}

/// Uma transformação inteira sem estado, útil para demonstrar que a lógica
/// executada no Rust pode ser chamada repetidamente pela UI Java.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_zegois_demo_MainActivity_transform(
    _env: *mut core::ffi::c_void,
    _this: *mut core::ffi::c_void,
    value: i32,
) -> i32 {
    let mut result = value as u32;
    result ^= result << 13;
    result ^= result >> 17;
    result ^= result << 5;
    result as i32
}

// Tipos mínimos necessários para acessar um jintArray sem o jni crate.
// Os índices dos dois métodos abaixo correspondem à tabela JNINativeInterface
// do jni.h fornecido pelo NDK: GetIntArrayElements = 187 e
// ReleaseIntArrayElements = 195.
type JNIEnv = *const JNINativeInterface;
type JIntArray = *mut core::ffi::c_void;
type JBoolean = u8;
type JInt = i32;

#[repr(C)]
pub struct JNINativeInterface {
    _prefix: [*const core::ffi::c_void; 187],
    get_int_array_elements:
        unsafe extern "system" fn(*mut JNIEnv, JIntArray, *mut JBoolean) -> *mut JInt,
    _between_arrays: [*const core::ffi::c_void; 7],
    release_int_array_elements: unsafe extern "system" fn(*mut JNIEnv, JIntArray, *mut JInt, JInt),
}

/// Recebe pixels ARGB produzidos pela Camera2, aplica um filtro de cor
/// diretamente no array Java e devolve o número de pixels processados.
///
/// O filtro calcula luminância e cria uma paleta falsa: vermelho representa
/// regiões claras, azul representa regiões escuras. O processamento é
/// intencionalmente simples para que o custo da passagem JNI fique visível.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_zegois_demo_NativeCameraActivity_processFrame(
    env: *mut JNIEnv,
    _this: *mut core::ffi::c_void,
    pixels: JIntArray,
    width: JInt,
    height: JInt,
) -> JInt {
    if env.is_null() || pixels.is_null() || width <= 0 || height <= 0 {
        return 0;
    }

    let count = match (width as usize).checked_mul(height as usize) {
        Some(value) => value,
        None => return 0,
    };

    let table = unsafe { *env };
    if table.is_null() {
        return 0;
    }

    let mut is_copy: JBoolean = 0;
    let raw_pixels = unsafe { ((*table).get_int_array_elements)(env, pixels, &mut is_copy) };
    if raw_pixels.is_null() {
        return 0;
    }

    unsafe {
        let buffer = core::slice::from_raw_parts_mut(raw_pixels, count);
        for pixel in buffer.iter_mut() {
            let red = ((*pixel as u32 >> 16) & 0xff) as u32;
            let green = ((*pixel as u32 >> 8) & 0xff) as u32;
            let blue = (*pixel as u32 & 0xff) as u32;
            let luminance = (77 * red + 150 * green + 29 * blue) >> 8;

            let filtered_red = (luminance * 2).min(255);
            let filtered_green = (luminance * 3 / 4).min(255);
            let filtered_blue = 255 - luminance;
            *pixel =
                (0xff00_0000_u32 | (filtered_red << 16) | (filtered_green << 8) | filtered_blue)
                    as i32;
        }

        // mode 0 = copiar as alterações de volta para o array Java.
        ((*table).release_int_array_elements)(env, pixels, raw_pixels, 0);
    }

    count.min(JInt::MAX as usize) as JInt
}
