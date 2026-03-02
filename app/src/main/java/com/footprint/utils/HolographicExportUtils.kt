package com.footprint.utils

import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.footprint.ui.screens.art.ArtLayout
import org.json.JSONArray
import org.json.JSONObject

object HolographicExportUtils {

        /**
         * Douglas-Peucker algorithm to simplify trace points. Reduces file size while preserving
         * visual fidelity of the path.
         */
        private fun simplify(points: List<LatLng>, epsilon: Double): List<LatLng> {
                if (points.size < 3) return points
                var maxDist = 0.0
                var maxIdx = 0
                val first = points.first()
                val last = points.last()
                for (i in 1 until points.size - 1) {
                        val d = perpendicularDistance(points[i], first, last)
                        if (d > maxDist) {
                                maxDist = d
                                maxIdx = i
                        }
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
                if (len == 0.0)
                        return Math.sqrt(
                                (p.longitude - a.longitude).let { it * it } +
                                        (p.latitude - a.latitude).let { it * it }
                        )
                return Math.abs(
                        dy * p.longitude - dx * p.latitude + b.longitude * a.latitude -
                                b.latitude * a.longitude
                ) / len
        }

        /**
         * Generates a self-contained "Holographic Scroll" HTML string.
         *
         * No template file is used — the entire HTML is built in Kotlin to avoid IDE
         * auto-formatting corruption of placeholders.
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
                totalDistanceKm: Double,
                bounds: LatLngBounds? = null
        ): String? {
                return try {
                        // 1. Douglas-Peucker simplification (epsilon ~0.00005 ≈ 5m)
                        val simplified =
                                if (tracePoints.size > 500) simplify(tracePoints, 0.00005)
                                else tracePoints

                        // 2. Build GeoJSON
                        val coordsArray = JSONArray()
                        simplified.forEach { pt ->
                                val c = JSONArray()
                                c.put(pt.longitude)
                                c.put(pt.latitude)
                                coordsArray.put(c)
                        }
                        val geometry =
                                JSONObject()
                                        .put("type", "LineString")
                                        .put("coordinates", coordsArray)
                        val feature =
                                JSONObject()
                                        .put("type", "Feature")
                                        .put("geometry", geometry)
                                        .put("properties", JSONObject())
                        val geoJson =
                                JSONObject()
                                        .put("type", "FeatureCollection")
                                        .put("features", JSONArray().put(feature))
                        val geoJsonStr = geoJson.toString()

                        // 3. Build initial state JSON
                        val stateObj =
                                JSONObject()
                                        .put("lat", initialCenter.latitude)
                                        .put("lng", initialCenter.longitude)
                                        .put("zoom", initialZoom)
                        if (bounds != null) {
                                stateObj.put(
                                        "bounds",
                                        JSONObject()
                                                .put("west", bounds.southwest.longitude)
                                                .put("south", bounds.southwest.latitude)
                                                .put("east", bounds.northeast.longitude)
                                                .put("north", bounds.northeast.latitude)
                                )
                        }
                        val stateStr = stateObj.toString()

                        // 4. Map UI state to CSS values
                        val fontCss =
                                when (uiState.artFontName) {
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
                        val textColorHex =
                                when (uiState.artTextColor) {
                                        "Black" -> "#000000"
                                        "Gold" -> "#FFCC00"
                                        "Deep Blue" -> "#007AFF"
                                        else -> "#FFFFFF"
                                }
                        val shadow =
                                if (uiState.artTextBorder)
                                        "0 0 10px rgba(0,0,0,0.8), 0 0 5px rgba(0,0,0,1)"
                                else "0 2px 10px rgba(0,0,0,0.8)"
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
                                append(
                                        "<meta name=\"viewport\" content=\"initial-scale=1,maximum-scale=1,user-scalable=no\"/>"
                                )
                                // Google Fonts
                                append(
                                        "<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">"
                                )
                                append(
                                        "<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>"
                                )
                                append(
                                        "<link href=\"https://fonts.googleapis.com/css2?family=Ma+Shan+Zheng&family=Zhi+Mang+Xing&family=Long+Cang&family=Liu+Jian+Mao+Cao&family=ZCOOL+Xiao+Wei&display=swap\" rel=\"stylesheet\">"
                                )
                                // MapLibre GL JS
                                append(
                                        "<script src=\"https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.js\"></script>"
                                )
                                append(
                                        "<link href=\"https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.css\" rel=\"stylesheet\"/>"
                                )
                                // CSS
                                append("<style>")
                                append("*{margin:0;padding:0;box-sizing:border-box}")
                                append(
                                        "html,body{height:100%;width:100%;overflow:hidden;background:#000;color:#fff;"
                                )
                                append(
                                        "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif}"
                                )
                                // CSS - add dark mode filter class
                                append(
                                        "#map{position:absolute;top:0;left:0;width:100%;height:100%}"
                                )
                                // Dark mode: invert light tiles to create a proper dark map
                                // appearance
                                append(
                                        "#map.dark-filter{filter:invert(1) hue-rotate(180deg) brightness(0.95) contrast(0.9)}"
                                )
                                // The overlay must NOT be inverted, so it sits above the filtered
                                // map
                                append(
                                        ".ov{position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none;display:flex;flex-direction:column;z-index:10}"
                                )
                                append(
                                        ".ov.fs{justify-content:flex-end;align-items:center;padding-bottom:80px}"
                                )
                                append(
                                        ".ov.pol{justify-content:flex-end;align-items:center;padding:0}"
                                )

                                // Dynamic Polaroid Frame logic
                                val p = uiState.polaroidFramePadding
                                val sideP = 4 + (15 - 4) * p
                                val topP = sideP
                                val bottomP = 15 + (30 - 15) * p

                                val frameBg =
                                        when (uiState.polaroidFrameStyle) {
                                                "CLASSIC_BLACK" -> "#1A1A1A"
                                                else -> "#FAFAFA"
                                        }
                                val glassEffect = ""

                                append(
                                        ".pol-frame{position:absolute;top:0;left:0;right:0;bottom:0;display:grid;grid-template-columns:${sideP}vw 1fr ${sideP}vw;grid-template-rows:${topP}vh 1fr ${bottomP}vh;pointer-events:none}"
                                )
                                append(".pol-part{background:$frameBg;$glassEffect}")
                                if (uiState.polaroidInnerBorder > 0) {
                                        val bc =
                                                when (uiState.polaroidFrameStyle) {
                                                        "CLASSIC_BLACK" -> "rgba(255,255,255,0.2)"
                                                        else -> "rgba(0,0,0,0.1)"
                                                }
                                        append(
                                                ".pol-map-area{border:${uiState.polaroidInnerBorder}px solid $bc;box-shadow: 0 0 5px rgba(0,0,0,0.1);}"
                                        )
                                }

                                // Watermark Styles
                                val pColor =
                                        if (uiState.polaroidFrameStyle == "CLASSIC_BLACK") "#fff"
                                        else "#000"
                                append(
                                        ".pol-wm{grid-column:1 / span 3;grid-row:3;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:0 20px;color:$pColor;font-family:$fontCss}"
                                )
                                val polTtlExtra = ""
                                append(
                                        ".pol-ttl{font-size:24px;font-weight:bold;margin-bottom:8px;opacity:0.9;$polTtlExtra}"
                                )
                                append(
                                        ".pol-div{width:40px;height:1px;background:$pColor;opacity:0.2;margin:8px 0}"
                                )
                                append(
                                        ".pol-row{width:100%;display:flex;justify-content:space-between;align-items:flex-end;margin-top:10px}"
                                )
                                append(".pol-meta{text-align:left;font-size:10px;opacity:0.6}")
                                append(".pol-stamp-box{text-align:right}")
                                append(
                                        ".pol-coord{font-size:8px;opacity:0.4;font-family:monospace;margin-bottom:4px}"
                                )
                                append(
                                        ".pol-stamp{display:inline-block;border:1px solid #FF453A;color:#FF453A;padding:2px 6px;font-size:10px;font-weight:bold;border-radius:2px;text-transform:uppercase}"
                                )

                                append(
                                        ".ov.geek{justify-content:flex-start;align-items:flex-end;padding:24px}"
                                )
                                append(
                                        ".geek-box{background:rgba(0,0,0,0.7);padding:16px;border-radius:8px;border-right:4px solid $traceColor;backdrop-filter:blur(4px);text-align:right}"
                                )
                                val ttlExtra = "text-shadow: $shadow;"
                                append(
                                        ".ttl{font-size:22px;margin-bottom:3px;letter-spacing:1.5px;color:$textColorHex;font-family:$fontCss;font-style:$fontStyle;$ttlExtra}"
                                )
                                append(
                                        ".met{font-size:11px;opacity:0.8;letter-spacing:0.5px;color:#fff;font-family:$fontCss}"
                                )
                                append(".maplibregl-ctrl-attrib{display:none!important}")
                                append(
                                        ".corner{position:absolute;width:40px;height:40px;border:2px solid rgba(255,255,255,0.3);pointer-events:none}"
                                )
                                append(".tl{top:20px;left:20px;border-right:0;border-bottom:0}")
                                append(".br{bottom:20px;right:20px;border-left:0;border-top:0}")
                                append("</style>")
                                append("</head><body>")
                                // Map container — add dark-filter class for dark mode
                                if (styleKey == "dark") {
                                        append("<div id=\"map\" class=\"dark-filter\"></div>")
                                } else {
                                        append("<div id=\"map\"></div>")
                                }
                                // Overlay — structure depends on layout
                                when (layoutName) {
                                        "POLAROID" -> {
                                                append("<div class=\"ov pol\">")
                                                append("<div class=\"pol-frame\">")
                                                append(
                                                        "<div class=\"pol-part\"></div><div class=\"pol-part\"></div><div class=\"pol-part\"></div>"
                                                )
                                                append(
                                                        "<div class=\"pol-part\"></div><div class=\"pol-map-area\"></div><div class=\"pol-part\"></div>"
                                                )
                                                append("<div class=\"pol-wm\">")
                                                append("<div class=\"pol-ttl\">$safeTitle</div>")
                                                append("<div class=\"pol-div\"></div>")
                                                append("<div class=\"pol-row\">")
                                                val now = java.time.LocalTime.now()
                                                val timeStr =
                                                        now.format(
                                                                java.time.format.DateTimeFormatter
                                                                        .ofPattern("HH:mm:ss")
                                                        )
                                                append(
                                                        "<div class=\"pol-meta\"><div>$safeMeta</div><div style=\"font-family:monospace;font-size:8px;margin-top:2px\">TIMESTAMP: $timeStr</div><div style=\"font-family:monospace;font-size:8px;margin-top:2px\">TOTAL DISTANCE: %.2f KM</div></div>".format(
                                                                totalDistanceKm
                                                        )
                                                )
                                                append(
                                                        "<div class=\"pol-stamp-box\"><div class=\"pol-coord\">COORD: 31.23&deg;N, 121.47&deg;E</div><div class=\"pol-stamp\">${uiState.userNickname}</div></div>"
                                                )
                                                append("</div>")
                                                append("</div>")
                                                append("</div>")
                                                append("</div>")
                                        }
                                        "GEEK_STATS" -> {
                                                append("<div class=\"ov geek\">")
                                                append(
                                                        "<div class=\"geek-box\"><div class=\"ttl\" style=\"font-size:13px\">$safeTitle</div><div class=\"met\">$safeMeta</div><div class=\"met\">MODE: TRACKING</div></div>"
                                                )
                                                append(
                                                        "<div class=\"corner tl\"></div><div class=\"corner br\"></div>"
                                                )
                                                append("</div>")
                                        }
                                        else -> { // FULLCREEN_A24
                                                append("<div class=\"ov fs\">")
                                                append(
                                                        "<div><div class=\"ttl\">$safeTitle</div><div class=\"met\">$safeMeta</div></div>"
                                                )
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
                                // Map styles — all use AMap raster tiles with Chinese labels
                                // Light & Dark both use style=7 (standard map with Chinese labels)
                                // Dark appearance is achieved via CSS filter on the #map div
                                // Satellite uses style=6 (imagery) + style=7 (label overlay)
                                // Void is a pure black background (no tiles)
                                val amapTileUrl =
                                        "https://wprd01.is.autonavi.com/appmaptile?x={x}&y={y}&z={z}&lang=zh_cn&size=1&scl=1&style=7"
                                val amapSatUrl =
                                        "https://wprd01.is.autonavi.com/appmaptile?x={x}&y={y}&z={z}&lang=zh_cn&size=1&scl=1&style=6"
                                // For satellite overlay, use style=8 with ltype=4 (transparent
                                // annotation layer)
                                val amapSatLabelUrl =
                                        "https://wprd01.is.autonavi.com/appmaptile?x={x}&y={y}&z={z}&lang=zh_cn&size=1&scl=1&style=8&ltype=4"
                                append(
                                        "var amapStyle={version:8,sources:{amap:{type:'raster',tiles:['$amapTileUrl'],tileSize:256}},layers:[{id:'amap',type:'raster',source:'amap'}]};"
                                )
                                append(
                                        "var satStyle={version:8,sources:{sat:{type:'raster',tiles:['$amapSatUrl'],tileSize:256},label:{type:'raster',tiles:['$amapSatLabelUrl'],tileSize:256}},layers:[{id:'sat',type:'raster',source:'sat'},{id:'label',type:'raster',source:'label'}]};"
                                )
                                append(
                                        "var voidStyle={version:8,sources:{},layers:[{id:'bg',type:'background',paint:{'background-color':'#000'}}]};"
                                )
                                // Select style based on key
                                append("var styleKey='$styleKey';")
                                append(
                                        "var mapSt=styleKey==='void'?voidStyle:styleKey==='satellite'?satStyle:amapStyle;"
                                )
                                // Init map
                                append(
                                        "var m=new maplibregl.Map({container:'map',style:mapSt,center:[S.lng,S.lat],zoom:S.zoom});"
                                )
                                append("m.on('load',function(){")
                                append("m.addSource('t',{type:'geojson',data:D});")
                                if (hasGlow) {
                                        append(
                                                "m.addLayer({id:'tg',type:'line',source:'t',layout:{'line-join':'round','line-cap':'round'},paint:{'line-color':TC,'line-width':15,'line-blur':10,'line-opacity':0.4}});"
                                        )
                                }
                                append(
                                        "m.addLayer({id:'tl',type:'line',source:'t',layout:{'line-join':'round','line-cap':'round'},paint:{'line-color':TC,'line-width':4}});"
                                )
                                append(
                                        "if(S.bounds){m.fitBounds([[S.bounds.west,S.bounds.south],[S.bounds.east,S.bounds.north]],{padding:{top:50,bottom:200,left:50,right:50},animate:false})}"
                                )
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
