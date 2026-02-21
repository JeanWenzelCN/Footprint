use std::fs::File;
use std::io::{BufWriter, Write};

fn main() {
    let file = File::create("test.png").unwrap();
    let mut w = BufWriter::new(file);
    let mut encoder = png::Encoder::new(w, 10, 10);
    encoder.set_color(png::ColorType::Rgba);
    encoder.set_depth(png::BitDepth::Eight);
    let mut writer = encoder.write_header().unwrap();
    
    // Using unwrap() to see if write_image_data supports chunking
    let data = vec![255; 10 * 5 * 4];
    
    // writer.write_image_data(&data).expect("First half failed");
    // writer.write_image_data(&data).expect("Second half failed");
    
    let mut stream = writer.stream_writer().unwrap();
    stream.write_all(&data).unwrap();
    stream.write_all(&data).unwrap();
    println!("Chunked writing successful!");
}
