package com.jeppe.radm.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jeppe.radm.domain.analysis.ActivityAnalysisData
import com.jeppe.radm.domain.analysis.ActivityAnalysisInteractionSnapshot
import com.jeppe.radm.domain.analysis.AnalysisCoordinateMode
import com.jeppe.radm.domain.analysis.AnalysisPoint
import com.jeppe.radm.domain.analysis.AnalysisSeries
import com.jeppe.radm.domain.analysis.AnalysisSeriesAvailability
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.CadenceStepsPerMinute
import com.jeppe.radm.domain.model.ElevationMetres
import com.jeppe.radm.domain.model.PaceSecondsPerKilometre
import com.jeppe.radm.domain.model.SpeedMetresPerSecond
import com.jeppe.radm.ui.library.ActivityLibraryTestTags
import com.jeppe.radm.ui.map.ActivityRouteMap
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

object ActivityAnalysisTestTags {
    const val SCREEN = "activity_detail"
    const val COORDINATE = "analysis_coordinate"
    const val RANGE = "analysis_range"
    const val COORDINATE_DISTANCE = "analysis_coordinate_distance"
    const val COORDINATE_ELAPSED = "analysis_coordinate_elapsed"
    const val RANGE_START = "analysis_range_start"
    const val RANGE_END = "analysis_range_end"
    const val RANGE_RESET = "analysis_range_reset"
    const val ROUTE_RANGE = "analysis_route_range"
    const val SELECTED_TIME = "analysis_selected_time"
    const val ROUTE = "analysis_route"
    const val ROUTE_UNAVAILABLE = "analysis_route_unavailable"
    const val INSPECTOR = "analysis_inspector"
    const val PACE_CHART = "analysis_pace_chart"
    const val SPEED_CHART = "analysis_speed_chart"
    const val ELEVATION_CHART = "analysis_elevation_chart"
    const val CADENCE_CHART = "analysis_cadence_chart"
    const val PACE_UNAVAILABLE = "analysis_pace_unavailable"
    const val SPEED_UNAVAILABLE = "analysis_speed_unavailable"
    const val ELEVATION_UNAVAILABLE = "analysis_elevation_unavailable"
    const val CADENCE_UNAVAILABLE = "analysis_cadence_unavailable"

    fun interaction(chartTag: String): String = "${chartTag}_interaction"
    fun selection(chartTag: String): String = "${chartTag}_selection"
    fun visibleRange(chartTag: String): String = "${chartTag}_range"
    fun axis(chartTag: String): String = "${chartTag}_axis"
}

@Composable
fun ActivityAnalysisScreen(
    data: ActivityAnalysisData,
    interaction: ActivityAnalysisInteractionSnapshot,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSelectFraction: (Double) -> Unit,
    onCoordinateMode: (AnalysisCoordinateMode) -> Unit,
    onRange: (Double, Double) -> Unit,
    onRestoreFullRange: () -> Unit,
    onSelectRoute: (latitude: Double, longitude: Double, toleranceMetres: Double) -> Unit,
    onEdit: (ActivityType, String?, String?) -> Unit,
    onDelete: () -> Unit,
) {
    val activity = data.activity
    val summary = data.summary
    var editing by rememberSaveable(activity.id.value) { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var type by rememberSaveable(activity.id.value, editing) { mutableStateOf(activity.type) }
    var title by rememberSaveable(activity.id.value, editing) { mutableStateOf(activity.title.orEmpty()) }
    var notes by rememberSaveable(activity.id.value, editing) { mutableStateOf(activity.notes.orEmpty()) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag(ActivityAnalysisTestTags.SCREEN),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextButton(onClick = onBack) { Text("Back to activities") }
                Text("Activity analysis", style = MaterialTheme.typography.headlineLarge)
                Text(activity.title ?: activity.type.displayName(), style = MaterialTheme.typography.titleLarge)
                Text("${activity.type.displayName()} · ${formatDate(activity.startedAt.value)}")
                Text("Active time: ${formatDuration(activity.activeDuration.value)}")
                Text("Distance: ${summary?.distance?.value?.let(::formatDistance) ?: "Unavailable"}")
                activity.notes?.let { Text("Notes: $it") }
                summary?.averagePace?.value?.let { Text("Average pace: ${formatPace(it)}") }
                summary?.averageSpeed?.value?.let { Text("Average speed: ${formatSpeed(it)}") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

            item {
                AnalysisStateSummary(
                    data = data,
                    interaction = interaction,
                    onCoordinateMode = onCoordinateMode,
                    onRange = onRange,
                    onRestoreFullRange = onRestoreFullRange,
                )
            }

            item {
                RouteSummary(data, interaction)
            }

            if (data.routeSegments.isNotEmpty()) {
                item {
                    ActivityRouteMap(
                        interaction = interaction,
                        onSelectRoute = onSelectRoute,
                    )
                }
            }

            when (activity.type) {
                ActivityType.RUNNING,
                ActivityType.CROSS_COUNTRY_SKIING,
                -> item {
                    MetricSection(
                        name = "Pace",
                        unit = "min/km",
                        series = data.pace,
                        interaction = interaction,
                        onSelectFraction = onSelectFraction,
                        chartTag = ActivityAnalysisTestTags.PACE_CHART,
                        unavailableTag = ActivityAnalysisTestTags.PACE_UNAVAILABLE,
                        valueToY = { -it.value / 60.0 },
                        yFormatter = { formatPace(abs(it) * 60.0) },
                    )
                }

                ActivityType.CYCLING -> item {
                    MetricSection(
                        name = "Speed",
                        unit = "km/h",
                        series = data.speed,
                        interaction = interaction,
                        onSelectFraction = onSelectFraction,
                        chartTag = ActivityAnalysisTestTags.SPEED_CHART,
                        unavailableTag = ActivityAnalysisTestTags.SPEED_UNAVAILABLE,
                        valueToY = { it.value * 3.6 },
                        yFormatter = { String.format(Locale.getDefault(), "%.1f", it) },
                    )
                }
            }

            item {
                MetricSection(
                    name = "Elevation",
                    unit = "m",
                    series = data.elevation,
                    interaction = interaction,
                    onSelectFraction = onSelectFraction,
                    chartTag = ActivityAnalysisTestTags.ELEVATION_CHART,
                    unavailableTag = ActivityAnalysisTestTags.ELEVATION_UNAVAILABLE,
                    valueToY = { it.value },
                    yFormatter = { String.format(Locale.getDefault(), "%.0f", it) },
                )
            }

            if (activity.type == ActivityType.RUNNING) {
                item {
                    MetricSection(
                        name = "Cadence",
                        unit = "spm",
                        series = data.cadence,
                        interaction = interaction,
                        onSelectFraction = onSelectFraction,
                        chartTag = ActivityAnalysisTestTags.CADENCE_CHART,
                        unavailableTag = ActivityAnalysisTestTags.CADENCE_UNAVAILABLE,
                        valueToY = { it.value },
                        yFormatter = { String.format(Locale.getDefault(), "%.0f", it) },
                    )
                }
            }

            item {
                SelectedPositionInspector(data, interaction)
            }

            if (editing) {
                item {
                    Text("Edit activity", style = MaterialTheme.typography.titleLarge)
                    ActivityType.entries.forEach { activityType ->
                        FilterChip(
                            selected = type == activityType,
                            onClick = { type = activityType },
                            label = { Text(activityType.displayName()) },
                            modifier = Modifier.testTag("activity_edit_type_${activityType.name}"),
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ActivityLibraryTestTags.EDIT_TITLE),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ActivityLibraryTestTags.EDIT_NOTES),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onEdit(type, title, notes)
                                editing = false
                            },
                            enabled = !saving,
                            modifier = Modifier.testTag(ActivityLibraryTestTags.EDIT_SAVE),
                        ) { Text("Save changes") }
                        TextButton(onClick = { editing = false }) { Text("Cancel") }
                    }
                }
            } else {
                item {
                    Button(
                        onClick = { editing = true },
                        modifier = Modifier.testTag(ActivityLibraryTestTags.EDIT),
                    ) { Text("Edit metadata") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { deleteConfirmation = true },
                        modifier = Modifier.testTag(ActivityLibraryTestTags.DELETE),
                    ) { Text("Delete activity") }
                }
            }
        }
    }

    if (deleteConfirmation) {
        AlertDialog(
            onDismissRequest = { deleteConfirmation = false },
            title = { Text("Delete activity?") },
            text = { Text("This permanently removes this activity and its recorded data.") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmation = false
                        onDelete()
                    },
                    modifier = Modifier.testTag(ActivityLibraryTestTags.DELETE_CONFIRM),
                ) { Text("Delete permanently") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmation = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AnalysisStateSummary(
    data: ActivityAnalysisData,
    interaction: ActivityAnalysisInteractionSnapshot,
    onCoordinateMode: (AnalysisCoordinateMode) -> Unit,
    onRange: (Double, Double) -> Unit,
    onRestoreFullRange: () -> Unit,
) {
    val state = interaction.state
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Analysis view", style = MaterialTheme.typography.titleMedium)
            Text(
                "Horizontal coordinate: " + when (state.coordinateMode) {
                    AnalysisCoordinateMode.DISTANCE -> "Distance"
                    AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME -> "Active Elapsed Time"
                },
                modifier = Modifier.testTag(ActivityAnalysisTestTags.COORDINATE),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.coordinateMode == AnalysisCoordinateMode.DISTANCE,
                    onClick = { onCoordinateMode(AnalysisCoordinateMode.DISTANCE) },
                    enabled = data.distance.availability == AnalysisSeriesAvailability.AVAILABLE,
                    label = { Text("Distance") },
                    modifier = Modifier.testTag(ActivityAnalysisTestTags.COORDINATE_DISTANCE),
                )
                FilterChip(
                    selected = state.coordinateMode == AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME,
                    onClick = { onCoordinateMode(AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME) },
                    label = { Text("Active elapsed time") },
                    modifier = Modifier.testTag(ActivityAnalysisTestTags.COORDINATE_ELAPSED),
                )
            }
            Text(
                "Selected range: ${formatRange(state.range, state.coordinateMode)}",
                modifier = Modifier.testTag(ActivityAnalysisTestTags.RANGE),
            )
            Text("Range start")
            Slider(
                value = interaction.rangeStartFraction.toFloat(),
                onValueChange = {
                    onRange(
                        it.toDouble().coerceAtMost(interaction.rangeEndFraction),
                        interaction.rangeEndFraction,
                    )
                },
                valueRange = 0f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ActivityAnalysisTestTags.RANGE_START),
            )
            Text("Range end")
            Slider(
                value = interaction.rangeEndFraction.toFloat(),
                onValueChange = {
                    onRange(
                        interaction.rangeStartFraction,
                        it.toDouble().coerceAtLeast(interaction.rangeStartFraction),
                    )
                },
                valueRange = 0f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ActivityAnalysisTestTags.RANGE_END),
            )
            TextButton(
                onClick = onRestoreFullRange,
                modifier = Modifier.testTag(ActivityAnalysisTestTags.RANGE_RESET),
            ) { Text("Restore full range") }
        }
    }
}

@Composable
private fun RouteSummary(
    data: ActivityAnalysisData,
    interaction: ActivityAnalysisInteractionSnapshot,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Route", style = MaterialTheme.typography.titleMedium)
            if (data.routeSegments.isEmpty()) {
                Text(
                    "Route unavailable — no recorded positions",
                    modifier = Modifier.testTag(ActivityAnalysisTestTags.ROUTE_UNAVAILABLE),
                )
            } else {
                val count = data.routeSegments.sumOf { it.samples.size }
                Text(
                    "$count recorded positions · ${data.routeSegments.size} route " +
                        if (data.routeSegments.size == 1) "segment" else "segments",
                    modifier = Modifier.testTag(ActivityAnalysisTestTags.ROUTE),
                )
                Text("Route geometry is available locally; recorded gaps remain separate.")
                val range = interaction.state.range
                val selectedCount = data.routeSegments.sumOf { segment ->
                    segment.samples.count { sample ->
                        sample.activeElapsedTime in
                            range.start.activeElapsedTime..range.endInclusive.activeElapsedTime
                    }
                }
                Text(
                    "Route subsection: $selectedCount of $count recorded positions",
                    modifier = Modifier.testTag(ActivityAnalysisTestTags.ROUTE_RANGE),
                )
            }
        }
    }
}

@Composable
private fun <T : Any> MetricSection(
    name: String,
    unit: String,
    series: AnalysisSeries<T>,
    interaction: ActivityAnalysisInteractionSnapshot,
    onSelectFraction: (Double) -> Unit,
    chartTag: String,
    unavailableTag: String,
    valueToY: (T) -> Double,
    yFormatter: (Double) -> String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("$name · $unit", style = MaterialTheme.typography.titleMedium)
            if (series.availability == AnalysisSeriesAvailability.AVAILABLE) {
                val coordinateMode = interaction.state.coordinateMode
                Text(
                    "Horizontal axis: " + if (coordinateMode == AnalysisCoordinateMode.DISTANCE) {
                        "Distance (km)"
                    } else {
                        "Active Elapsed Time"
                    },
                    modifier = Modifier.testTag(ActivityAnalysisTestTags.axis(chartTag)),
                )
                Text(
                    "Cursor: ${formatDuration(interaction.state.selectedPosition.activeElapsedTime.value)}",
                    modifier = Modifier.testTag(ActivityAnalysisTestTags.selection(chartTag)),
                )
                Text(
                    "Visible: ${formatRange(interaction.state.range, coordinateMode)}",
                    modifier = Modifier.testTag(ActivityAnalysisTestTags.visibleRange(chartTag)),
                )
                StaticMetricChart(
                    points = series.points,
                    interaction = interaction,
                    onSelectFraction = onSelectFraction,
                    interactionTag = ActivityAnalysisTestTags.interaction(chartTag),
                    valueToY = valueToY,
                    yFormatter = yFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .testTag(chartTag),
                )
            } else {
                Text(
                    "$name unavailable — ${series.availability.explanation()}",
                    modifier = Modifier.testTag(unavailableTag),
                )
            }
        }
    }
}

@Composable
private fun <T : Any> StaticMetricChart(
    points: List<AnalysisPoint<T>>,
    interaction: ActivityAnalysisInteractionSnapshot,
    onSelectFraction: (Double) -> Unit,
    interactionTag: String,
    valueToY: (T) -> Double,
    yFormatter: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    val coordinateMode = interaction.state.coordinateMode
    val segments = remember(points, coordinateMode, interaction.state.range) {
        chartSegments(points, coordinateMode, interaction.state.range, valueToY)
    }
    val currentOnSelectFraction by rememberUpdatedState(onSelectFraction)
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(segments) {
        modelProducer.runTransaction {
            lineSeries {
                segments.forEach { segment -> series(segment.x, segment.y) }
            }
        }
    }
    val horizontalFormatter = remember(coordinateMode) {
        CartesianValueFormatter { _, value, _ ->
            if (coordinateMode == AnalysisCoordinateMode.DISTANCE) {
                String.format(Locale.getDefault(), "%.1f", value)
            } else {
                formatShortDuration((value * 60_000.0).toLong())
            }
        }
    }
    val verticalFormatter = remember(interactionTag) {
        CartesianValueFormatter { _, value, _ -> yFormatter(value) }
    }
    val chartStart = interaction.visibleCoordinateExtent.start.toChartCoordinate(coordinateMode)
    val requestedChartEnd = interaction.visibleCoordinateExtent.endInclusive.toChartCoordinate(coordinateMode)
    val chartEnd = if (requestedChartEnd > chartStart) requestedChartEnd else chartStart + 0.001
    val rangeProvider = remember(chartStart, chartEnd) {
        CartesianLayerRangeProvider.fixed(minX = chartStart, maxX = chartEnd)
    }
    val cursorColor = MaterialTheme.colorScheme.primary
    Box(modifier = modifier) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(rangeProvider = rangeProvider),
                startAxis = VerticalAxis.rememberStart(valueFormatter = verticalFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = horizontalFormatter),
            ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 8.dp, top = 8.dp, bottom = 28.dp)
                .testTag(interactionTag)
                .pointerInput(interaction.visibleCoordinateExtent) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        currentOnSelectFraction((down.position.x / size.width).toDouble())
                        var pressed = down.pressed
                        while (pressed) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
                                currentOnSelectFraction((change.position.x / size.width).toDouble())
                                change.consume()
                            }
                            pressed = event.changes.any { it.pressed }
                        }
                    }
                },
        ) {
            interaction.cursorFraction?.let { fraction ->
                val x = size.width * fraction.toFloat()
                drawLine(
                    color = cursorColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 3.dp.toPx(),
                )
            }
        }
    }
}

@Composable
private fun SelectedPositionInspector(
    data: ActivityAnalysisData,
    interaction: ActivityAnalysisInteractionSnapshot,
) {
    val inspector = interaction.inspector
    Card(modifier = Modifier.fillMaxWidth().testTag(ActivityAnalysisTestTags.INSPECTOR)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Selected position", style = MaterialTheme.typography.titleMedium)
            Text("Distance: ${inspector.position.cumulativeDistance?.value?.let(::formatDistance) ?: "—"}")
            Text(
                "Active elapsed time: ${formatDuration(inspector.position.activeElapsedTime.value)}",
                modifier = Modifier.testTag(ActivityAnalysisTestTags.SELECTED_TIME),
            )
            when (data.activity.type) {
                ActivityType.RUNNING,
                ActivityType.CROSS_COUNTRY_SKIING,
                -> Text("Pace: ${inspector.pace?.value?.let(::formatPace) ?: "—"}")
                ActivityType.CYCLING -> Text("Speed: ${inspector.speed?.value?.let(::formatSpeed) ?: "—"}")
            }
            Text("Elevation: ${inspector.elevation?.value?.let { "${it.toLong()} m" } ?: "—"}")
            if (data.activity.type == ActivityType.RUNNING) {
                Text("Cadence: ${inspector.cadence?.value?.let { "${it.toLong()} spm" } ?: "—"}")
            }
        }
    }
}

private data class ChartSegment(
    val x: List<Double>,
    val y: List<Double>,
)

private fun <T : Any> chartSegments(
    points: List<AnalysisPoint<T>>,
    coordinateMode: AnalysisCoordinateMode,
    range: com.jeppe.radm.domain.analysis.AnalysisRange,
    valueToY: (T) -> Double,
): List<ChartSegment> {
    val result = mutableListOf<ChartSegment>()
    var group: Long? = null
    var xValues = mutableListOf<Double>()
    var yValues = mutableListOf<Double>()

    fun flush() {
        if (xValues.isNotEmpty()) result += ChartSegment(xValues, yValues)
        xValues = mutableListOf()
        yValues = mutableListOf()
    }

    points.forEach { point ->
        if (point.position.activeElapsedTime !in
            range.start.activeElapsedTime..range.endInclusive.activeElapsedTime
        ) {
            flush()
            group = null
            return@forEach
        }
        val x = when (coordinateMode) {
            AnalysisCoordinateMode.DISTANCE -> point.position.cumulativeDistance?.value?.div(1_000.0)
            AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME -> point.position.activeElapsedTime.value / 60_000.0
        }
        val value = point.value
        if (x == null || value == null) {
            flush()
            group = null
            return@forEach
        }
        if (group != null && group != point.continuityGroup) flush()
        group = point.continuityGroup
        val y = valueToY(value)
        if (xValues.lastOrNull() == x) {
            yValues[yValues.lastIndex] = y
        } else if (xValues.lastOrNull()?.let { x > it } != false) {
            xValues += x
            yValues += y
        }
    }
    flush()
    return result
}

private fun Double.toChartCoordinate(mode: AnalysisCoordinateMode): Double = when (mode) {
    AnalysisCoordinateMode.DISTANCE -> this / 1_000.0
    AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME -> this / 60_000.0
}

private fun formatRange(
    range: com.jeppe.radm.domain.analysis.AnalysisRange,
    mode: AnalysisCoordinateMode,
): String = when (mode) {
    AnalysisCoordinateMode.DISTANCE -> {
        val start = range.start.cumulativeDistance?.value
        val end = range.endInclusive.cumulativeDistance?.value
        if (start == null || end == null) "Distance unavailable" else {
            "${formatDistance(start)}–${formatDistance(end)}"
        }
    }
    AnalysisCoordinateMode.ACTIVE_ELAPSED_TIME ->
        "${formatDuration(range.start.activeElapsedTime.value)}–" +
            formatDuration(range.endInclusive.activeElapsedTime.value)
}

private fun AnalysisSeriesAvailability.explanation(): String = when (this) {
    AnalysisSeriesAvailability.AVAILABLE -> error("Available data has no unavailable explanation")
    AnalysisSeriesAvailability.UNAVAILABLE_NO_SOURCE -> "no suitable recorded data"
    AnalysisSeriesAvailability.UNAVAILABLE_NOT_CURRENT -> "current processed data is not available"
    AnalysisSeriesAvailability.UNAVAILABLE_NOT_APPLICABLE -> "not applicable to this activity type"
}

private fun ActivityType.displayName(): String = when (this) {
    ActivityType.RUNNING -> "Running"
    ActivityType.CYCLING -> "Cycling"
    ActivityType.CROSS_COUNTRY_SKIING -> "Cross-country skiing"
}

private fun formatDate(utcMillis: Long): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .format(Instant.ofEpochMilli(utcMillis).atZone(ZoneId.systemDefault()))

private fun formatDistance(metres: Double): String =
    String.format(Locale.getDefault(), "%.2f km", metres / 1_000.0)

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1_000L
    return String.format(
        Locale.getDefault(),
        "%d:%02d:%02d",
        seconds / 3_600L,
        (seconds % 3_600L) / 60L,
        seconds % 60L,
    )
}

private fun formatShortDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1_000L
    return String.format(Locale.getDefault(), "%d:%02d", seconds / 60L, seconds % 60L)
}

private fun formatPace(secondsPerKilometre: Double): String {
    val seconds = secondsPerKilometre.toLong()
    return String.format(Locale.getDefault(), "%d:%02d/km", seconds / 60L, seconds % 60L)
}

private fun formatSpeed(metresPerSecond: Double): String =
    String.format(Locale.getDefault(), "%.1f km/h", metresPerSecond * 3.6)
