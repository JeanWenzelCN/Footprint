package com.footprint.utils

import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.footprint.ui.screens.art.ArtLayout
import org.json.JSONArray
import org.json.JSONObject

object HolographicExportUtils {

    /**
     * Douglas-Peucker algorithm to simplify trace points.
     * Reduces file size while preserving visual fidelity of the path.
     */
    private fun simplify(points: List<LatLng>, epsilon: Double): List<LatLng> {
        if (points.size < 3) return points
        var maxDist = 0.0
        var maxIdx = 0
        val first = points.first()
        val last = points.last()
        for (i in 1 until points.size - 1) {
            val d = perpendicularDistance(points[i], first, last)
            if (d > maxDist) { maxDist = d; maxIdx = i }
        }
        return if (maxDist > epsilon) {
            val left = simplify(points.subList(0, maxIdx + 1), epsilon)
            val right = simplify(points.subList(maxIdx, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(p: LatLng, a: LatLng, b: LatLng): Double {
        val dx = b.longitude - a.longitude
        val dy = b.latitude - a.latitude
        val len = Math.sqrt(dx * dx + dy * dy)
        if (len == 0.0) return Math.sqrt(
            (p.longitude - a.longitude).let { it * it } +
            (p.latitude - a.latitude).let { it * it }
        )
        return Math.abs(dy * p.longitude - dx * p.latitude + b.longitude * a.latitude - b.latitude * a.longitude) / len
    }

    /**
     * Generates a self-contained "Holographic Scroll" HTML string.
     *
     * No template file is used — the entire HTML is built in Kotlin
     * to avoid IDE auto-formatting corruption of placeholders.
     */
    fun generateHolographicScrollContent(
            context: android.content.Context,
            title: String,
            metadata: String,
            tracePoints: List<LatLng>,
            mapStyle: String,
            traceColor: String,
            hasGlow: Boolean,
            initialCenter: LatLng,
            initialZoom: Double,
            uiState: com.footprint.ui.state.FootprintUiState,
            artLayout: ArtLayout,
            bounds: LatLngBounds? = null
    ): String? {
        return try {
            // 1. Douglas-Peucker simplification (epsilon ~0.00005 ≈ 5m)
            val simplified = if (tracePoints.size > 500) simplify(tracePoints, 0.00005) else tracePoints

            // 2. Build GeoJSON
            val coordsArray = JSONArray()
            simplified.forEach { pt ->
                val c = JSONArray()
                c.put(pt.longitude)
                c.put(pt.latitude)
                coordsArray.put(c)
            }
            val geometry = JSONObject().put("type", "LineString").put("coordinates", coordsArray)
            val feature = JSONObject().put("type", "Feature").put("geometry", geometry).put("properties", JSONObject())
            val geoJson = JSONObject().put("type", "FeatureCollection").put("features", JSONArray().put(feature))
            val geoJsonStr = geoJson.toString()

            // 3. Build initial state JSON
            val stateObj = JSONObject()
                .put("lat", initialCenter.latitude)
                .put("lng", initialCenter.longitude)
                .put("zoom", initialZoom)
            if (bounds != null) {
                stateObj.put("bounds", JSONObject()
                    .put("west", bounds.southwest.longitude)
                    .put("south", bounds.southwest.latitude)
                    .put("east", bounds.northeast.longitude)
                    .put("north", bounds.northeast.latitude))
            }
            val stateStr = stateObj.toString()

            // 4. Map UI state to CSS values
            val fontCss = when (uiState.artFontName) {
                "MaShanZheng" -> "'Ma Shan Zheng', cursive"
                "ZhiMangXing" -> "'Zhi Mang Xing', cursive"
                "LongCang" -> "'Long Cang', cursive"
                "LiuJianMaoCao" -> "'Liu Jian Mao Cao', cursive"
                "ZCOOLXiaoWei" -> "'ZCOOL Xiao Wei', serif"
                "Serif" -> "serif"
                "Monospace" -> "monospace"
                "Cursive" -> "cursive"
                else -> "sans-serif"
            }
            val textColorHex = when (uiState.artTextColor) {
                "Black" -> "#000000"
                "Gold" -> "#FFCC00"
                "Deep Blue" -> "#007AFF"
                else -> "#FFFFFF"
            }
            val shadow = if (uiState.artTextBorder)
                "0 0 10px rgba(0,0,0,0.8), 0 0 5px rgba(0,0,0,1)"
            else
                "0 2px 10px rgba(0,0,0,0.8)"
            val fontStyle = if (uiState.artTextItalic) "italic" else "normal"
            val layoutName = artLayout.name
            val styleKey = mapStyle.lowercase()
            val hasGlowStr = hasGlow.toString()
            val safeTitle = title.replace("\"", "&quot;").replace("<", "&lt;")
            val safeMeta = metadata.replace("\"", "&quot;").replace("<", "&lt;")

            // 5. Build the complete HTML — no template file needed
            val html = buildString {
                append("<!DOCTYPE html>")
                append("<html><head>")
                append("<meta charset=\"utf-8\"/>")
                append("<title>$safeTitle - Footprint Art</title>")
                append("<meta name=\"viewport\" content=\"initial-scale=1,maximum-scale=1,user-scalable=no\"/>")
                // Google Fonts
                append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">")
                append("<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>")
                append("<link href=\"https://fonts.googleapis.com/css2?family=Ma+Shan+Zheng&family=Zhi+Mang+Xing&family=Long+Cang&family=Liu+Jian+Mao+Cao&family=ZCOOL+Xiao+Wei&display=swap\" rel=\"stylesheet\">")
                // MapLibre GL JS
                append("<script src=\"https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.js\"></script>")
                append("<link href=\"https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.css\" rel=\"stylesheet\"/>")
                // CSS
                append("<style>")
                append("*{margin:0;padding:0;box-sizing:border-box}")
                append("html,body{height:100%;width:100%;overflow:hidden;background:#000;color:#fff;")
                append("font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif}")
                append("#map{position:absolute;top:0;left:0;width:100%;height:100%}")
                append(".ov{position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none;display:flex;flex-direction:column;z-index:10}")
                append(".ov.fs{justify-content:flex-end;align-items:center;padding-bottom:80px}")
                append(".ov.pol{justify-content:flex-end;align-items:center;padding-bottom:60px}")
                append(".pol-frame{position:absolute;top:0;left:0;right:0;bottom:0;border-style:solid;border-color:#FAFAFA;border-width:10vh 5vw 25vh 5vw;box-shadow:inset 0 0 10px rgba(0,0,0,0.1)}")
                append(".ov.geek{justify-content:flex-start;align-items:flex-end;padding:24px}")
                append(".geek-box{background:rgba(0,0,0,0.7);padding:16px;border-radius:8px;border-right:4px solid $traceColor;backdrop-filter:blur(4px);text-align:right}")
                append(".ttl{font-size:38px;margin-bottom:4px;letter-spacing:2px;color:$textColorHex;font-family:$fontCss;font-style:$fontStyle;text-shadow:$shadow}")
                append(".met{font-size:14px;opacity:0.8;letter-spacing:1px;color:#fff;font-family:$fontCss}")
                append(".maplibregl-ctrl-attrib{display:none!important}")
                append(".corner{position:absolute;width:40px;height:40px;border:2px solid rgba(255,255,255,0.3);pointer-events:none}")
                append(".tl{top:20px;left:20px;border-right:0;border-bottom:0}")
                append(".br{bottom:20px;right:20px;border-left:0;border-top:0}")
                append("</style>")
                append("</head><body>")
                // Map container
                append("<div id=\"map\"></div>")
                // Overlay — structure depends on layout
                when (layoutName) {
                    "POLAROID" -> {
                        append("<div class=\"ov pol\">")
                        append("<div class=\"pol-frame\"></div>")
                        append("<div><div class=\"ttl\">$safeTitle</div><div class=\"met\">$safeMeta</div></div>")
                        append("</div>")
                    }
                    "GEEK_STATS" -> {
                        append("<div class=\"ov geek\">")
                        append("<div class=\"geek-box\"><div class=\"ttl\" style=\"font-size:16px\">$safeTitle</div><div class=\"met\">$safeMeta</div><div class=\"met\">MODE: TRACKING</div></div>")
                        append("<div class=\"corner tl\"></div><div class=\"corner br\"></div>")
                        append("</div>")
                    }
                    else -> { // FULLCREEN_A24
                        append("<div class=\"ov fs\">")
                        append("<div><div class=\"ttl\">$safeTitle</div><div class=\"met\">$safeMeta</div></div>")
                        append("</div>")
                    }
                }
                // JavaScript
                append("<script>")
                append("var D=")
                append(geoJsonStr)
                append(";var S=")
                append(stateStr)
                append(";var TC=\"$traceColor\";var HG=$hasGlowStr;")
                // Map styles
                append("var MS={")
                append("void:{version:8,sources:{},layers:[{id:'bg',type:'background',paint:{'background-color':'#000'}}]},")
                append("dark:'https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json',")
                append("light:'https://basemaps.cartocdn.com/gl/positron-gl-style/style.json',")
                append("satellite:{version:8,sources:{sat:{type:'raster',tiles:['https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}'],tileSize:256}},layers:[{id:'sat',type:'raster',source:'sat'}]}")
                append("};")
                // Init map
                append("var m=new maplibregl.Map({container:'map',style:MS['$styleKey']||MS.dark,center:[S.lng,S.lat],zoom:S.zoom});")
                append("m.on('load',function(){")
                append("m.addSource('t',{type:'geojson',data:D});")
                if (hasGlow) {
                    append("m.addLayer({id:'tg',type:'line',source:'t',layout:{'line-join':'round','line-cap':'round'},paint:{'line-color':TC,'line-width':15,'line-blur':10,'line-opacity':0.4}});")
                }
                append("m.addLayer({id:'tl',type:'line',source:'t',layout:{'line-join':'round','line-cap':'round'},paint:{'line-color':TC,'line-width':4}});")
                append("if(S.bounds){m.fitBounds([[S.bounds.west,S.bounds.south],[S.bounds.east,S.bounds.north]],{padding:{top:50,bottom:200,left:50,right:50},animate:false})}")
                append("});")
                append("</script>")
                append("</body></html>")
            }

            html
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
