package com.footprint.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.model.PolylineOptions
import com.footprint.service.LocationTrackingService

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    
    // 扩展权限列表
    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermission = it[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    val isTracking by LocationTrackingService.isTracking.collectAsState()
    val currentLocation by LocationTrackingService.currentLocation.collectAsState()
    val trackingPath by LocationTrackingService.trackingPath.collectAsState()

    // 监听位置，并确保相机移动是基于有效坐标的
    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            if (loc.latitude > 1.0) {
                mapView.map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 17f))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            AndroidView(
                factory = {
                    mapView.apply {
                        map.apply {
                            uiSettings.isMyLocationButtonEnabled = false
                            isMyLocationEnabled = true
                            myLocationStyle = MyLocationStyle().apply {
                                myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                                interval(2000)
                                showMyLocation(true)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { mv ->
                if (trackingPath.isNotEmpty()) {
                    mv.map.clear()
                    val points = trackingPath.map { LatLng(it.latitude, it.longitude) }
                    mv.map.addPolyline(PolylineOptions().addAll(points).width(18f).color(android.graphics.Color.parseColor("#00FF9F")))
                }
            }
        } else {
            PermissionDenyOverlay { launcher.launch(permissionsToRequest) }
        }

        // 定位回正按钮
        Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)) {
            FilledIconButton(
                onClick = {
                    if (currentLocation != null && currentLocation!!.latitude > 1.0) {
                        mapView.map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLocation!!.latitude, currentLocation!!.longitude), 18f))
                    } else {
                        // 强制拉起一次定位
                        LocationTrackingService.startTracking(context)
                    }
                },
                modifier = Modifier.size(56.dp).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Rounded.GpsFixed, null, tint = Color.White)
            }
        }

        // 底部控制
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 110.dp)
                .height(80.dp)
                .fillMaxWidth(),
            color = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("GPS 状态", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                    Text(if (currentLocation == null) "搜索信号..." else "信号良好", color = if (currentLocation == null) Color.Yellow else Color(0xFF00FF9F), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        if (isTracking) LocationTrackingService.stopTracking(context)
                        else LocationTrackingService.startTracking(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isTracking) Color(0xFFFF3B30) else Color(0xFF00FF9F))
                ) {
                    Text(if (isTracking) "停止" else "开启追踪", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PermissionDenyOverlay(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Security, null, tint = Color(0xFF00FF9F), modifier = Modifier.size(64.dp))
            Text("需要定位与通知权限", color = Color.White, modifier = Modifier.padding(top = 16.dp))
            Button(onClick = onRetry, modifier = Modifier.padding(top = 24.dp)) { Text("立即授权") }
        }
    }
}
