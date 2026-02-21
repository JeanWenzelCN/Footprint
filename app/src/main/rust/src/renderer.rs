use serde::Deserialize;
use std::f64::consts::PI;
use std::fs::File;
use std::io::BufWriter;
use std::path::Path;

const TILE_SIZE: f64 = 256.0;
const BAND_HEIGHT: u32 = 512; // Render 512 pixels high at a time

#[derive(Deserialize, Debug)]
struct LatLng {
    lat: f64,
    lng: f64,
}

// Global pixel coordinate
#[derive(Debug, Clone, Copy)]
struct Point {
    x: f64,
    y: f64,
}

fn project(lat: f64, lng: f64, zoom: f64) -> Point {
    let lat_rad = lat * PI / 180.0;
    let n = f64::powf(2.0, zoom);
    let x = (lng + 180.0) / 360.0 * n * TILE_SIZE;
    let y = (1.0 - ((lat_rad.tan() + (1.0 / lat_rad.cos())).ln() / PI)) / 2.0 * n * TILE_SIZE;
    Point { x, y }
}

pub async fn render_map(
    output_path: String,
    trace_json: String,
    theme: String,
    trace_color: String,
    glow_radius: f32,
    width: u32,
    height: u32,
    center_lat: f64,
    center_lng: f64,
    zoom: f64,
) -> i32 {
    // 1. Force integer zoom for tile alignment
    let export_zoom = zoom.round();
    let trace: Vec<LatLng> = serde_json::from_str(&trace_json).unwrap_or_default();

    // 2. Global bounds of the entire poster
    let center_pt = project(center_lat, center_lng, export_zoom);
    let poster_left = center_pt.x - (width as f64 / 2.0);
    let poster_top = center_pt.y - (height as f64 / 2.0);

    // 3. Setup PNG encoder
    let file = match File::create(Path::new(&output_path)) {
        Ok(f) => f,
        Err(e) => {
            println!("Failed to create output file: {}", e);
            return 2;
        }
    };
    let ref mut w = BufWriter::new(file);
    let mut encoder = png::Encoder::new(w, width, height);
    encoder.set_color(png::ColorType::Rgba);
    encoder.set_depth(png::BitDepth::Eight);
    let mut writer = match encoder.write_header() {
        Ok(w) => w,
        Err(_) => return 3,
    };
    
    let mut stream_writer = match writer.stream_writer() {
        Ok(s) => s,
        Err(_) => return 3,
    };

    let mut headers = reqwest::header::HeaderMap::new();
    headers.insert(reqwest::header::USER_AGENT, reqwest::header::HeaderValue::from_static("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
    headers.insert(reqwest::header::REFERER, reqwest::header::HeaderValue::from_static("https://www.amap.com/"));
    let client = reqwest::Client::builder().default_headers(headers).build().unwrap_or_default();
    
    let theme_lower = theme.to_lowercase();
    let map_style = match theme_lower.as_str() {
        "satellite" => Some("6"),
        "light" => Some("7"),
        "dark" => Some("8"),
        _ => None, // void or unknown
    };

    // 4. Render Horizontal Bands
    for band_y in (0..height).step_by(BAND_HEIGHT as usize) {
        let current_band_height = if band_y + BAND_HEIGHT > height {
            height - band_y
        } else {
            BAND_HEIGHT
        };

        let mut pixmap = match tiny_skia::Pixmap::new(width, current_band_height) {
            Some(p) => p,
            None => return 4,
        };

        // Fill background based on theme
        let bg_color = match theme_lower.as_str() {
            "dark" | "void" => tiny_skia::Color::from_rgba8(27, 27, 27, 255),
            _ => tiny_skia::Color::from_rgba8(242, 242, 242, 255),
        };
        pixmap.fill(bg_color);

        let band_top = poster_top + band_y as f64;
        let band_bottom = band_top + current_band_height as f64;

        // Find intersecting tiles
        let tile_start_x = (poster_left / TILE_SIZE).floor() as i64;
        let tile_end_x = ((poster_left + width as f64) / TILE_SIZE).floor() as i64;
        let tile_start_y = (band_top / TILE_SIZE).floor() as i64;
        let tile_end_y = (band_bottom / TILE_SIZE).floor() as i64;

        // Download and draw tiles
        if let Some(style) = map_style {
            for tx in tile_start_x..=tile_end_x {
                for ty in tile_start_y..=tile_end_y {
                    let url = format!(
                        "http://wprd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style={}&x={}&y={}&z={}",
                        style, tx, ty, export_zoom as i32
                    );

                    if let Ok(resp) = client.get(&url).send().await {
                        if let Ok(bytes) = resp.bytes().await {
                            if let Ok(dyn_img) = image::load_from_memory(&bytes) {
                                let rgba_img = dyn_img.into_rgba8();
                                let tile_w = rgba_img.width();
                                let tile_h = rgba_img.height();
                                let raw_bytes = rgba_img.into_raw();
                                
                                // Amap tiles are fully opaque, so we can mock premultiplied RGBA bytes directly
                                if let Some(tile_pixmap) = tiny_skia::Pixmap::from_vec(
                                    raw_bytes,
                                    tiny_skia::IntSize::from_wh(tile_w, tile_h).unwrap()
                                ) {
                                    let draw_x = (tx as f64 * TILE_SIZE) - poster_left;
                                    let draw_y = (ty as f64 * TILE_SIZE) - band_top;

                                    pixmap.draw_pixmap(
                                        draw_x as i32,
                                        draw_y as i32,
                                        tile_pixmap.as_ref(),
                                        &tiny_skia::PixmapPaint::default(),
                                        tiny_skia::Transform::identity(),
                                        None,
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }

        // Draw path segments intersecting this band
        if trace.len() > 1 {
            let mut pb = tiny_skia::PathBuilder::new();
            let mut first = true;

            for point in &trace {
                let pt = project(point.lat, point.lng, export_zoom);
                let local_x = (pt.x - poster_left) as f32;
                let local_y = (pt.y - band_top) as f32; // Negative if above band, positive if below

                if first {
                    pb.move_to(local_x, local_y);
                    first = false;
                } else {
                    pb.line_to(local_x, local_y);
                }
            }

            if let Some(path) = pb.finish() {
                let mut paint = tiny_skia::Paint::default();
                
                // Parse hex color "#rrggbb"
                let r = u8::from_str_radix(&trace_color[1..3], 16).unwrap_or(255);
                let g = u8::from_str_radix(&trace_color[3..5], 16).unwrap_or(69);
                let b = u8::from_str_radix(&trace_color[5..7], 16).unwrap_or(58);
                paint.set_color_rgba8(r, g, b, 255); 
                paint.anti_alias = true;
                
                let mut stroke = tiny_skia::Stroke::default();
                stroke.width = 5.0; // Reduced core width to avoid covering roads
                stroke.line_cap = tiny_skia::LineCap::Round;
                stroke.line_join = tiny_skia::LineJoin::Round;

                if glow_radius > 0.0 {
                    let mut glow_paint = tiny_skia::Paint::default();
                    glow_paint.set_color_rgba8(r, g, b, 80); // Reduced alpha for subtlety
                    glow_paint.anti_alias = true;
                    let mut glow_stroke = tiny_skia::Stroke::default();
                    glow_stroke.width = 5.0 + (glow_radius * 5.0); // Reduced spread
                    glow_stroke.line_cap = tiny_skia::LineCap::Round;
                    glow_stroke.line_join = tiny_skia::LineJoin::Round;
                    pixmap.stroke_path(&path, &glow_paint, &glow_stroke, tiny_skia::Transform::identity(), None);
                }

                pixmap.stroke_path(&path, &paint, &stroke, tiny_skia::Transform::identity(), None);
            }
        }

        // 5. Stream the chunk bytes to PNG encoder row by row
        let bytes = pixmap.data();
        use std::io::Write;
        if let Err(e) = stream_writer.write_all(bytes) {
            println!("Failed to write chunk: {}", e);
            return 5;
        }
    }

    0
}
