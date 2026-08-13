package com.knightspace.airswing.debug

import com.knightspace.airswing.sensor.SensorFrame

private data class TimelineRow(
    val rowType: String,
    val timestampNs: Long,
    val frame: SensorFrame? = null,
    val detectorState: String = "",
    val swingScore: Float? = null,
    val impactScore: Float? = null,
    val eventTimestampNs: Long? = null,
    val detail: String = "",
)

/** Debug-only consumer: callers must guard all use with BuildConfig.DEBUG. */
class SensorRecorder(
    private val maxRows: Int = 120_000,
    private val metadata: Map<String, String> = emptyMap(),
) {
    private val rows = ArrayDeque<TimelineRow>()
    var isRecording: Boolean = false
        private set
    val frameCount: Int get() = rows.count { it.rowType == "sensor" }
    val rowCount: Int get() = rows.size

    fun start() {
        rows.clear()
        isRecording = true
    }

    fun stop() {
        isRecording = false
    }

    fun record(frame: SensorFrame) = recordSensor(frame)

    fun recordSensor(
        frame: SensorFrame,
        detectorState: String = "",
        swingScore: Float? = null,
        impactScore: Float? = null,
    ) = append(TimelineRow(
        rowType = "sensor",
        timestampNs = frame.timestampNs,
        frame = frame,
        detectorState = detectorState,
        swingScore = swingScore,
        impactScore = impactScore,
    ))

    fun recordEvent(
        rowType: String,
        timestampNs: Long,
        detectorState: String = "",
        swingScore: Float? = null,
        impactScore: Float? = null,
        eventTimestampNs: Long? = null,
        detail: String = "",
    ) = append(TimelineRow(
        rowType = rowType,
        timestampNs = timestampNs,
        detectorState = detectorState,
        swingScore = swingScore,
        impactScore = impactScore,
        eventTimestampNs = eventTimestampNs,
        detail = detail,
    ))

    private fun append(row: TimelineRow) {
        if (!isRecording) return
        rows.addLast(row)
        if (rows.size > maxRows) rows.removeFirst()
    }

    fun toCsv(): String = buildString {
        appendLine("# airswing_csv_version=3")
        metadata.toSortedMap().forEach { (key, value) ->
            append("# ").append(key).append('=').append(value.replace("\n", " ")).append('\n')
        }
        appendLine("row_type,timestamp_ns,ax,ay,az,gx,gy,gz,world_linear_ax,world_linear_ay,world_linear_az,detector_state,swing_score,impact_score,event_timestamp_ns,detail")
        rows.forEach { row ->
            appendCsv(row.rowType).append(',').append(row.timestampNs).append(',')
            val frame = row.frame
            if (frame != null) {
                append(frame.ax).append(',').append(frame.ay).append(',').append(frame.az).append(',')
                append(frame.gx).append(',').append(frame.gy).append(',').append(frame.gz).append(',')
                appendFinite(frame.worldLinearAx).append(',')
                appendFinite(frame.worldLinearAy).append(',')
                appendFinite(frame.worldLinearAz)
            } else {
                append(",,,,,,,,")
            }
            append(',').appendCsv(row.detectorState).append(',')
            row.swingScore?.let(::append)
            append(',')
            row.impactScore?.let(::append)
            append(',')
            row.eventTimestampNs?.let(::append)
            append(',').appendCsv(row.detail).append('\n')
        }
    }

    private fun StringBuilder.appendFinite(value: Float): StringBuilder =
        if (value.isFinite()) append(value) else this

    private fun StringBuilder.appendCsv(value: String): StringBuilder {
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            append('"').append(value.replace("\"", "\"\"")).append('"')
        } else {
            append(value)
        }
        return this
    }
}
