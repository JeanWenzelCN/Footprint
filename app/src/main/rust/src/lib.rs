#![allow(non_snake_case)]

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jdouble, jfloat};

mod renderer;

#[no_mangle]
pub extern "system" fn Java_com_footprint_utils_NativeRenderer_generateGigapixelMap<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    output_file_path: JString<'local>,
    trace_json: JString<'local>,
    theme: JString<'local>,
    trace_color_hex: JString<'local>,
    glow_radius: jfloat,
    width: jint,
    height: jint,
    center_lat: jdouble,
    center_lng: jdouble,
    zoom: jdouble,
) -> jint {
    let output_path: String = match env.get_string(&output_file_path) {
        Ok(s) => s.into(),
        Err(_) => return -1, // false
    };

    let json_data: String = match env.get_string(&trace_json) {
        Ok(s) => s.into(),
        Err(_) => return -1,
    };
    
    let map_theme: String = match env.get_string(&theme) {
        Ok(s) => s.into(),
        Err(_) => "light".to_string(),
    };

    let color_hex: String = match env.get_string(&trace_color_hex) {
        Ok(s) => s.into(),
        Err(_) => "#FF453A".to_string(), // Default Red
    };

    // Spin up the async renderer
    let rt = tokio::runtime::Runtime::new().unwrap();
    let result = rt.block_on(async {
        renderer::render_map(
            output_path,
            json_data,
            map_theme,
            color_hex,
            glow_radius as f32,
            width as u32,
            height as u32,
            center_lat,
            center_lng,
            zoom
        ).await
    });

    result as jint
}
