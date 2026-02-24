package com.footprint

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.footprint.data.model.FootprintEntry
import com.footprint.data.model.Mood
import com.footprint.data.model.TravelGoal
import com.footprint.data.repository.FootprintAnalytics
import com.footprint.data.repository.FootprintRepository
import com.footprint.ui.state.FilterState
import com.footprint.ui.state.FootprintUiState
import com.footprint.ui.theme.ThemeMode
import com.footprint.utils.ApiKeyManager
import com.footprint.utils.PreferenceManager
import java.io.InputStreamReader
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FootprintViewModel(
        application: Application,
        private val repository: FootprintRepository =
                (application as FootprintApplication).repository
) : AndroidViewModel(application) {

    private val preferenceManager = PreferenceManager(application)
    private val gson =
            com.google.gson.GsonBuilder()
                    .registerTypeAdapter(
                            LocalDate::class.java,
                            com.google.gson.JsonSerializer<LocalDate> { src, _, _ ->
                                com.google.gson.JsonPrimitive(src.toString())
                            }
                    )
                    .registerTypeAdapter(
                            LocalDate::class.java,
                            com.google.gson.JsonDeserializer { json, _, _ ->
                                LocalDate.parse(json.asString)
                            }
                    )
                    .create()

    private val moodFilter = MutableStateFlow<Mood?>(null)
    private val searchQuery = MutableStateFlow("")
    private val yearFilter = MutableStateFlow(LocalDate.now().year)
    private val themeMode = MutableStateFlow(preferenceManager.themeMode)
    private val themeStyle = MutableStateFlow(preferenceManager.themeStyle)
    private val nickname = MutableStateFlow(preferenceManager.nickname)
    private val avatarId = MutableStateFlow(preferenceManager.avatarId)
    private val blurStrength = MutableStateFlow(preferenceManager.blurStrength)
    private val hapticFeedback = MutableStateFlow(preferenceManager.hapticFeedbackEnabled)
    private val artAuthorName = MutableStateFlow(preferenceManager.artAuthorName)
    private val artFontName = MutableStateFlow(preferenceManager.artFontName)
    private val artColorStyle = MutableStateFlow(preferenceManager.artColorStyle)
    private val artTextColor = MutableStateFlow(preferenceManager.artTextColor)
    private val artTextItalic = MutableStateFlow(preferenceManager.artTextItalic)
    private val artTextBorder = MutableStateFlow(preferenceManager.artTextBorder)
    private val polaroidFrameStyle = MutableStateFlow(preferenceManager.polaroidFrameStyle)
    private val polaroidFramePadding = MutableStateFlow(preferenceManager.polaroidFramePadding)
    private val polaroidInnerBorder = MutableStateFlow(preferenceManager.polaroidInnerBorder)
    private val woodType = MutableStateFlow(preferenceManager.woodType)
    private val engravingDepth = MutableStateFlow(preferenceManager.engravingDepth)
    private val canvasGrain = MutableStateFlow(preferenceManager.canvasGrain)
    private val _amapKey = MutableStateFlow(ApiKeyManager.getApiKey(application) ?: "")
    val amapKey: StateFlow<String> = _amapKey.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val yearlyTrackPointCount: Flow<Int> =
            yearFilter.flatMapLatest { year ->
                flow<Int> { emit(repository.getTrackPointCount(year)) }
            }

    private val monthlyTrackPointCount: Flow<Int> =
            flow<Int> {
                val now = LocalDate.now()
                emit(repository.getTrackPointCount(now.year, now.monthValue))
            }

    // 定义显式的数据组结构
    private data class DataGroup(
            val entries: List<FootprintEntry>,
            val goals: List<TravelGoal>,
            val yPoints: Int,
            val mPoints: Int
    )
    private data class FilterGroup(val mood: Mood?, val search: String, val year: Int)
    private data class PrefsGroup(
            val theme: ThemeMode,
            val style: com.footprint.ui.theme.AppThemeStyle,
            val nk: String,
            val av: String,
            val blur: Float,
            val haptic: Boolean,
            val artName: String,
            val artFont: String,
            val artColor: String,
            val artTextColor: String,
            val artTextItalic: Boolean,
            val artTextBorder: Boolean,
            val polFrameStyle: String,
            val polFramePadding: Float,
            val polInnerBorder: Float,
            val woodType: com.footprint.ui.screens.art.WoodType,
            val engravingDepth: Float,
            val canvasGrain: Float
    )

    // 强类型合并流
    private val dataFlow: Flow<DataGroup> =
            combine(
                    repository.observeEntries(),
                    repository.observeGoals(),
                    yearlyTrackPointCount,
                    monthlyTrackPointCount
            ) { entries, goals, yPoints, mPoints -> DataGroup(entries, goals, yPoints, mPoints) }

    private val filterFlow: Flow<FilterGroup> =
            combine(moodFilter, searchQuery, yearFilter) { mood, search, year ->
                FilterGroup(mood, search, year)
            }

    private val appearanceFlow =
            combine(themeMode, themeStyle, blurStrength) { mode, style, blur ->
                Triple(mode, style, blur)
            }

    private val userFlow =
            combine(nickname, avatarId, hapticFeedback) { nk, av, haptic -> Triple(nk, av, haptic) }

    private val artBaseFlow =
            combine(artAuthorName, artFontName, artColorStyle) { name, font, color ->
                Triple(name, font, color)
            }

    private val artTextFlow =
            combine(artTextColor, artTextItalic, artTextBorder) { color, italic, border ->
                Triple(color, italic, border)
            }

    private val polaroidFlow =
            combine(
                    polaroidFrameStyle,
                    polaroidFramePadding,
                    polaroidInnerBorder,
                    woodType,
                    engravingDepth,
                    canvasGrain
            ) { args: Array<Any?> ->
                val style = args[0] as String
                val padding = args[1] as Float
                val border = args[2] as Float
                val wType = args[3] as com.footprint.ui.screens.art.WoodType
                val eDepth = args[4] as Float
                val cGrain = args[5] as Float
                Triple(style, padding, border) to Triple(wType, eDepth, cGrain)
            }

    private val artCombinedFlow = combine(artBaseFlow, artTextFlow) { base, text -> base to text }

    private val prefsFlow: Flow<PrefsGroup> =
            combine(appearanceFlow, userFlow, artCombinedFlow, polaroidFlow) {
                    appearance,
                    user,
                    artCombined,
                    polCombined ->
                val (artBase, artText) = artCombined
                val (polBase, polWood) = polCombined
                PrefsGroup(
                        theme = appearance.first,
                        style = appearance.second,
                        blur = appearance.third,
                        nk = user.first,
                        av = user.second,
                        haptic = user.third,
                        artName = artBase.first,
                        artFont = artBase.second,
                        artColor = artBase.third,
                        artTextColor = artText.first,
                        artTextItalic = artText.second,
                        artTextBorder = artText.third,
                        polFrameStyle = polBase.first,
                        polFramePadding = polBase.second,
                        polInnerBorder = polBase.third,
                        woodType = polWood.first,
                        engravingDepth = polWood.second,
                        canvasGrain = polWood.third
                )
            }

    // 最终合并，参数减少到 3 个，编译器推断不再压力
    val uiState: StateFlow<FootprintUiState> =
            combine(dataFlow, filterFlow, prefsFlow) { data, filter, prefs ->
                        val visibleEntries =
                                data.entries
                                        .filter { filter.mood == null || it.mood == filter.mood }
                                        .filter {
                                            if (filter.search.isBlank()) true
                                            else {
                                                val queryText = filter.search.trim().lowercase()
                                                it.title.lowercase().contains(queryText) ||
                                                        it.location
                                                                .lowercase()
                                                                .contains(queryText) ||
                                                        it.tags.any { tag ->
                                                            tag.lowercase().contains(queryText)
                                                        }
                                            }
                                        }

                        val visibleGoals =
                                data.goals.filter {
                                    if (filter.search.isBlank()) true
                                    else {
                                        val queryText = filter.search.trim().lowercase()
                                        it.title.lowercase().contains(queryText) ||
                                                it.targetLocation.lowercase().contains(queryText) ||
                                                it.notes.lowercase().contains(queryText)
                                    }
                                }

                        val today = LocalDate.now()
                        val historicalMemories =
                                data.entries.filter {
                                    it.happenedOn.monthValue == today.monthValue &&
                                            it.happenedOn.dayOfMonth == today.dayOfMonth &&
                                            it.happenedOn.year < today.year
                                }

                        val randomMemory =
                                if (historicalMemories.isNotEmpty()) historicalMemories.random()
                                else null

                        val memoryQuote =
                                if (randomMemory == null) {
                                    com.footprint.utils.DailyQuoteManager.getDailyQuote()
                                } else null

                        FootprintUiState(
                                entries = data.entries,
                                visibleEntries = visibleEntries,
                                goals = visibleGoals,
                                yearlyEntries =
                                        data.entries.filter { it.happenedOn.year == filter.year },
                                yearlyGoals =
                                        data.goals.filter { it.targetDate.year == filter.year },
                                summary =
                                        FootprintAnalytics.buildSummary(
                                                data.entries,
                                                filter.year,
                                                data.yPoints,
                                                data.mPoints
                                        ),
                                filterState = FilterState(filter.mood, filter.search, filter.year),
                                themeMode = prefs.theme,
                                themeStyle = prefs.style,
                                userNickname = prefs.nk,
                                userAvatarId = prefs.av,
                                blurStrength = prefs.blur,
                                hapticFeedbackEnabled = prefs.haptic,
                                artAuthorName = prefs.artName,
                                artFontName = prefs.artFont,
                                artColorStyle = prefs.artColor,
                                artTextColor = prefs.artTextColor,
                                artTextItalic = prefs.artTextItalic,
                                artTextBorder = prefs.artTextBorder,
                                polaroidFrameStyle = prefs.polFrameStyle,
                                polaroidFramePadding = prefs.polFramePadding,
                                polaroidInnerBorder = prefs.polInnerBorder,
                                woodType = prefs.woodType,
                                engravingDepth = prefs.engravingDepth,
                                canvasGrain = prefs.canvasGrain,
                                randomMemory = randomMemory,
                                memoryQuote = memoryQuote,
                                isLoading = false
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5_000),
                            initialValue =
                                    FootprintUiState(
                                            themeMode = preferenceManager.themeMode,
                                            themeStyle = preferenceManager.themeStyle,
                                            artAuthorName = preferenceManager.artAuthorName,
                                            artFontName = preferenceManager.artFontName,
                                            artColorStyle = preferenceManager.artColorStyle,
                                            artTextColor = preferenceManager.artTextColor,
                                            artTextItalic = preferenceManager.artTextItalic,
                                            artTextBorder = preferenceManager.artTextBorder,
                                            polaroidFrameStyle =
                                                    preferenceManager.polaroidFrameStyle,
                                            polaroidFramePadding =
                                                    preferenceManager.polaroidFramePadding,
                                            polaroidInnerBorder =
                                                    preferenceManager.polaroidInnerBorder,
                                            woodType = preferenceManager.woodType,
                                            engravingDepth = preferenceManager.engravingDepth,
                                            canvasGrain = preferenceManager.canvasGrain
                                    )
                    )

    init {
        repository.ensureSeedData()
    }

    fun updateProfile(newNickname: String, newAvatarId: String) {
        nickname.value = newNickname
        avatarId.value = newAvatarId
        preferenceManager.nickname = newNickname
        preferenceManager.avatarId = newAvatarId
    }

    fun setBlurStrength(strength: Float) {
        blurStrength.value = strength
        preferenceManager.blurStrength = strength
    }

    fun setHapticFeedback(enabled: Boolean) {
        hapticFeedback.value = enabled
        preferenceManager.hapticFeedbackEnabled = enabled
    }

    fun updateArtSettings(
            name: String,
            font: String,
            color: String,
            textColor: String,
            italic: Boolean,
            border: Boolean
    ) {
        artAuthorName.value = name
        artFontName.value = font
        artColorStyle.value = color
        artTextColor.value = textColor
        artTextItalic.value = italic
        artTextBorder.value = border

        preferenceManager.artAuthorName = name
        preferenceManager.artFontName = font
        preferenceManager.artColorStyle = color
        preferenceManager.artTextColor = textColor
        preferenceManager.artTextItalic = italic
        preferenceManager.artTextBorder = border
    }

    fun updatePolaroidSettings(frameStyle: String, padding: Float, border: Float) {
        polaroidFrameStyle.value = frameStyle
        polaroidFramePadding.value = padding
        polaroidInnerBorder.value = border

        preferenceManager.polaroidFrameStyle = frameStyle
        preferenceManager.polaroidFramePadding = padding
        preferenceManager.polaroidInnerBorder = border
    }

    fun updateWoodSettings(
            type: com.footprint.ui.screens.art.WoodType,
            depth: Float,
            grain: Float
    ) {
        woodType.value = type
        engravingDepth.value = depth
        canvasGrain.value = grain

        preferenceManager.woodType = type
        preferenceManager.engravingDepth = depth
        preferenceManager.canvasGrain = grain
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val path =
                        com.footprint.utils.ImageUtils.saveImageToInternalStorage(
                                getApplication(),
                                uri
                        )
                if (path != null) {
                    withContext(Dispatchers.Main) { updateProfile(nickname.value, path) }
                }
            }
        }
    }

    fun exportData(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    // 1. Prepare Backup Data
                    val backup = repository.prepareBackup()

                    // 2. Create Temporary Directory
                    val tempDir =
                            java.io.File(
                                    context.cacheDir,
                                    "backup_temp_${System.currentTimeMillis()}"
                            )
                    if (tempDir.exists()) com.footprint.utils.FileUtils.deleteRecursively(tempDir)
                    tempDir.mkdirs()

                    val imagesDir = java.io.File(tempDir, "images")
                    imagesDir.mkdirs()

                    // 3. Process Images & Copy to Temp
                    val processedFootprints =
                            backup.footprints.map { footprint ->
                                val newPhotos =
                                        footprint.photos.mapNotNull { photoPath ->
                                            try {
                                                val originalFile = java.io.File(photoPath)
                                                if (originalFile.exists()) {
                                                    val destFile =
                                                            java.io.File(
                                                                    imagesDir,
                                                                    originalFile.name
                                                            )
                                                    com.footprint.utils.FileUtils.copyFile(
                                                            originalFile,
                                                            destFile
                                                    )
                                                    "images/${originalFile.name}" // Relative path
                                                    // in backup
                                                } else {
                                                    null // Skip missing files
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                null
                                            }
                                        }
                                footprint.copy(photos = newPhotos)
                            }

                    val processedBackup = backup.copy(footprints = processedFootprints)
                    val json = gson.toJson(processedBackup)

                    // 4. Save JSON
                    val jsonFile = java.io.File(tempDir, "backup_data.json")
                    java.io.FileWriter(jsonFile).use { it.write(json) }

                    // 5. Zip Everything
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        com.footprint.utils.FileUtils.zipDirectory(tempDir, outputStream)
                    }

                    // 6. Cleanup
                    com.footprint.utils.FileUtils.deleteRecursively(tempDir)
                }
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "导出失败: ${e.javaClass.simpleName}")
            }
        }
    }

    fun importData(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    // 1. Create Temp Work Dir
                    val tempDir =
                            java.io.File(
                                    context.cacheDir,
                                    "import_temp_${System.currentTimeMillis()}"
                            )
                    if (tempDir.exists()) com.footprint.utils.FileUtils.deleteRecursively(tempDir)
                    tempDir.mkdirs()

                    var isZip = false
                    try {
                        // 2. Try Unzip
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            // Peek first few bytes to check for PK signature?
                            // For now, rely on try-catch but ensure we reset stream or re-open for
                            // JSON
                            try {
                                com.footprint.utils.FileUtils.unzip(inputStream, tempDir)
                                if (tempDir.listFiles()?.isNotEmpty() == true) {
                                    isZip = true
                                }
                            } catch (e: Exception) {
                                // Not a zip or unzip failed -> treat as JSON
                                isZip = false
                            }
                        }
                    } catch (e: Exception) {
                        isZip = false
                    }

                    val backup: com.footprint.data.model.BackupData

                    if (isZip) {
                        // 3a. Read JSON from Zip
                        val jsonFile = java.io.File(tempDir, "backup_data.json")
                        if (jsonFile.exists()) {
                            val json = java.io.FileReader(jsonFile).use { it.readText() }
                            backup =
                                    gson.fromJson(
                                            json,
                                            com.footprint.data.model.BackupData::class.java
                                    )
                        } else {
                            // Valid zip but missing metadata - unlikely, but falback to direct read
                            // just in case
                            // or verify if there is a json file directly in root?
                            // For now, if no backup_data.json in zip, assume user error or
                            // fallthrough
                            isZip = false
                            // Fallthrough to direct read below won't work easily if we consumed
                            // stream.
                            // Actually, we need to re-open stream for direct read.
                            val json =
                                    context.contentResolver.openInputStream(uri)?.use {
                                        InputStreamReader(it).use { reader -> reader.readText() }
                                    }
                                            ?: throw Exception("无法读取文件")
                            backup =
                                    gson.fromJson(
                                            json,
                                            com.footprint.data.model.BackupData::class.java
                                    )
                        }
                    } else {
                        // 3b. Legacy JSON Import (Direct read)
                        val json =
                                context.contentResolver.openInputStream(uri)?.use {
                                    InputStreamReader(it).use { reader -> reader.readText() }
                                }
                                        ?: throw Exception("无法读取文件")
                        backup =
                                gson.fromJson(json, com.footprint.data.model.BackupData::class.java)
                    }

                    // 4. Restore Images (Only if Zip)
                    val restoredFootprints =
                            if (isZip) {
                                val appImagesDir =
                                        java.io.File(context.filesDir, "footprint_images")
                                if (!appImagesDir.exists()) appImagesDir.mkdirs()

                                backup.footprints.map { footprint ->
                                    val restoredPhotos =
                                            footprint.photos.map { relativePath ->
                                                if (relativePath.startsWith("images/")) {
                                                    val imageName =
                                                            relativePath.substringAfter("images/")
                                                    val sourceFile =
                                                            java.io.File(tempDir, relativePath)
                                                    if (sourceFile.exists()) {
                                                        val destFile =
                                                                java.io.File(
                                                                        appImagesDir,
                                                                        imageName
                                                                )
                                                        com.footprint.utils.FileUtils.copyFile(
                                                                sourceFile,
                                                                destFile
                                                        )
                                                        destFile.absolutePath
                                                    } else {
                                                        relativePath
                                                    }
                                                } else relativePath
                                            }
                                    footprint.copy(photos = restoredPhotos)
                                }
                            } else {
                                backup.footprints // Legacy: keep paths as-is
                            }

                    // 5. Restore Data to DB
                    // If track points are missing (legacy backup), generate them from footprint
                    // entries
                    val finalTrackPoints =
                            if (backup.trackPoints.isEmpty()) {
                                restoredFootprints
                                        .filter { it.latitude != null && it.longitude != null }
                                        .map { fp ->
                                            com.footprint.data.local.TrackPointEntity(
                                                    latitude = fp.latitude!!,
                                                    longitude = fp.longitude!!,
                                                    timestamp =
                                                            fp.happenedOn
                                                                    .atStartOfDay(
                                                                            java.time.ZoneOffset.UTC
                                                                    )
                                                                    .toInstant()
                                                                    .toEpochMilli(),
                                                    speed = 0f,
                                                    accuracy = 0f,
                                                    altitude = fp.altitude ?: 0.0
                                            )
                                        }
                            } else {
                                backup.trackPoints
                            }

                    val finalBackup =
                            backup.copy(
                                    footprints = restoredFootprints,
                                    trackPoints = finalTrackPoints
                            )
                    repository.restoreFromBackup(finalBackup)

                    // 6. Cleanup
                    com.footprint.utils.FileUtils.deleteRecursively(tempDir)
                }
                onSuccess()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg =
                        if (e is java.io.FileNotFoundException) {
                            "文件未找到或无法访问: ${e.message}"
                        } else if (e is com.google.gson.JsonSyntaxException) {
                            "JSON 格式错误: ${e.message?.take(50)}..."
                        } else {
                            "导入失败 (${e.javaClass.simpleName}): ${e.message}"
                        }
                onError(errorMsg)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
        preferenceManager.themeMode = mode
    }

    fun setThemeStyle(style: com.footprint.ui.theme.AppThemeStyle) {
        themeStyle.value = style
        preferenceManager.themeStyle = style
    }

    fun toggleMoodFilter(mood: Mood?) {
        moodFilter.value = if (moodFilter.value == mood) null else mood
    }

    fun updateSearch(query: String) {
        searchQuery.value = query
    }

    fun shiftYear(delta: Int) {
        val maxYear = LocalDate.now().year + 5
        yearFilter.value = (yearFilter.value + delta).coerceIn(1970, maxYear)
    }

    fun updateFootprint(entry: com.footprint.data.model.FootprintEntry) {
        viewModelScope.launch { repository.saveEntry(entry) }
    }

    fun saveAmapKey(key: String) {
        _amapKey.value = key
        ApiKeyManager.setApiKey(getApplication(), key)
    }

    fun deleteFootprint(entry: com.footprint.data.model.FootprintEntry) {
        viewModelScope.launch { repository.deleteEntry(entry.id) }
    }

    fun addFootprint(
            title: String,
            location: String,
            detail: String,
            mood: Mood,
            tags: List<String>,
            distanceKm: Double,
            photos: List<String>,
            energyLevel: Int,
            date: LocalDate,
            latitude: Double? = null,
            longitude: Double? = null,
            icon: String = "LocationOn",
            weather: String? = null
    ) {
        viewModelScope.launch {
            val entry =
                    FootprintEntry(
                            title = title,
                            location = location,
                            detail = detail,
                            mood = mood,
                            tags = tags,
                            distanceKm = distanceKm,
                            photos = photos,
                            energyLevel = energyLevel,
                            happenedOn = date,
                            latitude = latitude,
                            longitude = longitude,
                            icon = icon,
                            weather = weather
                    )
            repository.saveEntry(entry)
        }
    }

    fun addGoal(
            title: String,
            targetLocation: String,
            targetDate: LocalDate,
            notes: String,
            icon: String = "Flag"
    ) {
        viewModelScope.launch {
            val goal =
                    TravelGoal(
                            title = title,
                            targetLocation = targetLocation,
                            targetDate = targetDate,
                            notes = notes,
                            isCompleted = false,
                            progress = 5,
                            icon = icon
                    )
            repository.saveGoal(goal)
        }
    }

    fun updateGoal(goal: TravelGoal) {
        viewModelScope.launch { repository.saveGoal(goal) }
    }

    fun toggleGoal(goal: TravelGoal) {
        viewModelScope.launch { repository.updateGoalCompletion(goal, !goal.isCompleted) }
    }

    fun deleteGoal(goal: TravelGoal) {
        viewModelScope.launch { repository.deleteGoal(goal.id) }
    }

    fun getTrackPoints(start: Long, end: Long) = repository.getTrackPoints(start, end)

    // --- Time Capsule Logic ---
    val unlockedCapsules =
            repository
                    .observeUnlockedCapsules()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lockedCapsules =
            repository
                    .observeLockedCapsules()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun buryTimeCapsule(
            message: String,
            contentUri: String?,
            latitude: Double,
            longitude: Double,
            unlockDurationMs: Long,
            radius: Double = 50.0
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val capsule =
                    com.footprint.data.local.TimeCapsuleEntity(
                            latitude = latitude,
                            longitude = longitude,
                            message = message,
                            contentUri = contentUri,
                            creationTime = now,
                            unlockTime = now + unlockDurationMs,
                            isUnlocked = false,
                            radius = radius
                    )
            repository.saveTimeCapsule(capsule)
        }
    }

    fun checkTimeCapsuleUnlock(location: android.location.Location) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val readyCapsules = repository.getReadyToUnlockCapsules(now)

            readyCapsules.forEach { capsule ->
                val results = floatArrayOf(0f)
                android.location.Location.distanceBetween(
                        location.latitude,
                        location.longitude,
                        capsule.latitude,
                        capsule.longitude,
                        results
                )
                if (results[0] <= capsule.radius) {
                    repository.unlockCapsule(capsule.id)
                }
            }
        }
    }

    // For Heatmap: Get all points from last year
    fun getHeatmapPoints(): Flow<List<com.footprint.data.local.TrackPointEntity>> {
        val end = System.currentTimeMillis()
        val start = end - 365L * 24 * 60 * 60 * 1000 // 1 year
        return repository.getTrackPoints(start, end)
    }
    // ----------------

    companion object {
        val Factory =
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(
                            modelClass: Class<T>,
                            extras: CreationExtras
                    ): T {
                        val application =
                                checkNotNull(
                                        extras[
                                                ViewModelProvider.AndroidViewModelFactory
                                                        .APPLICATION_KEY]
                                ) { "Application was not provided in ViewModel extras" }
                        @Suppress("UNCHECKED_CAST")
                        return FootprintViewModel(application as FootprintApplication) as T
                    }
                }
    }
}
