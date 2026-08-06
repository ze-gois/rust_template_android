// A camada nativa deliberadamente usa apenas tipos JNI primitivos.
// A Activity continua sendo dona da UI; Rust fica responsável pelo cálculo.

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_zegois_demo_MainActivity_answer(
    _env: *mut core::ffi::c_void,
    _this: *mut core::ffi::c_void,
) -> i32 {
    41
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
