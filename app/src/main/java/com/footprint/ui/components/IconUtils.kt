package com.footprint.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconUtils {
    fun getIconByName(name: String): ImageVector {
        return when(name) {
            "LocationOn" -> Icons.Default.LocationOn
            "Restaurant" -> Icons.Default.Restaurant
            "LocalCafe" -> Icons.Default.LocalCafe
            "Park" -> Icons.Default.Park
            "Flight" -> Icons.Default.Flight
            "Train" -> Icons.Default.Train
            "DirectionsBike" -> Icons.Default.DirectionsBike
            "ShoppingBag" -> Icons.Default.ShoppingBag
            "CameraAlt" -> Icons.Default.CameraAlt
            "MusicNote" -> Icons.Default.MusicNote
            "Movie" -> Icons.Default.Movie
            "DirectionsRun" -> Icons.Default.DirectionsRun
            "Pets" -> Icons.Default.Pets
            "School" -> Icons.Default.School
            "Work" -> Icons.Default.Work
            "Flag" -> Icons.Default.Flag
            "Star" -> Icons.Default.Star
            "Favorite" -> Icons.Default.Favorite
            "Explore" -> Icons.Default.Explore
            "Brush" -> Icons.Default.Brush
            "Map" -> Icons.Default.Map
            "Landscape" -> Icons.Default.Landscape
            "Hotel" -> Icons.Default.Hotel
            "LocalActivity" -> Icons.Default.LocalActivity
            "Event" -> Icons.Default.Event
            "BeachAccess" -> Icons.Default.BeachAccess
            else -> Icons.Default.Place
        }
    }
}
