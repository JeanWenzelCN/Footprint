package com.footprint.utils

object NativeRenderer {
    init {
        System.loadLibrary("footprint_renderer")
    }

    /**
     * Renders a gigapixel high-resolution footprint map directly to disk using Rust's streaming PNG
     * encoder. This avoids OutOfMemoryErrors completely by rendering in horizontal bands.
     *
     * @param outputFilePath The absolute path to the output PNG file.
     * @param traceJson JSON serialized array of LatLng objects (e.g., [{"lat":39.9,"lng":116.3},
     * ...])
     * @param theme Map style (e.g., "dark", "light", "satellite").
     * @param width Final image width in pixels.
     * @param height Final image height in pixels.
     * @param centerLat Center latitude of the canvas.
     * @param centerLng Center longitude of the canvas.
     * @param zoom The base map zoom level to download tiles from (e.g., 18.0).
     */
    external fun generateGigapixelMap(
            outputFilePath: String,
            traceJson: String,
            theme: String,
            traceColorHex: String,
            glowRadius: Float,
            width: Int,
            height: Int,
            centerLat: Double,
            centerLng: Double,
            zoom: Double
    ): Int
}
