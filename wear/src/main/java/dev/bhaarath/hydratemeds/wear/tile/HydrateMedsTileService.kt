package dev.bhaarath.hydratemeds.wear.tile

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.bhaarath.hydratemeds.shared.database.toModel
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.shared.runtime.QuickLogWaterReceiver
import dev.bhaarath.hydratemeds.shared.runtime.ReminderExtras
import dev.bhaarath.hydratemeds.shared.runtime.ReminderRuntime
import dev.bhaarath.hydratemeds.shared.schedule.HydrateMedsScheduleConfig
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class HydrateMedsTileService : TileService() {
    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> {
        val state = loadTileState()
        val layout = LayoutElementBuilders.Layout.Builder()
            .setRoot(tileContent(state))
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("2")
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(layout)
                            .build(),
                    )
                    .build(),
            )
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion("2")
                .build(),
        )

    private fun tileContent(state: TileState): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId("quick_water")
                            .setOnClick(quickLogPendingIntent())
                            .build(),
                    )
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.ColorProp.Builder(0xFF061719.toInt()).build())
                            .build(),
                    )
                    .build(),
            )
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .setWidth(DimensionBuilders.wrap())
                    .setHeight(DimensionBuilders.wrap())
                    .addContent(
                        tileText("${state.waterDone} / ${state.waterTarget}", 0xFF80E0CF.toInt(), 24f)
                            .build(),
                    )
                    .addContent(
                        tileText("Next ${state.nextTime}", 0xFFFFFFFF.toInt(), 13f)
                            .build(),
                    )
                    .build(),
            )
            .build()

    private fun quickLogPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            77,
            Intent(this, QuickLogWaterReceiver::class.java)
                .setAction(ReminderExtras.actionQuickLogWater),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun loadTileState(): TileState = runBlocking {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val logs = ReminderRuntime.database(applicationContext)
            .acknowledgmentLogDao()
            .getForLocalDate(today.toString())
            .map { it.toModel() }
        val next = HydrateMedsScheduleConfig.nextEventAfter(ZonedDateTime.now(zoneId), zoneId)
        TileState(
            waterDone = logs.count { it.reminderType == ReminderType.Water },
            waterTarget = HydrateMedsScheduleConfig.dailyWaterTarget,
            nextTime = next.localTime.format(DateTimeFormatter.ofPattern("h:mm a")),
        )
    }

    private fun tileText(
        value: String,
        color: Int,
        size: Float,
    ): LayoutElementBuilders.Text.Builder =
        LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setColor(ColorBuilders.ColorProp.Builder(color).build())
                    .setSize(DimensionBuilders.sp(size))
                    .build(),
            )
}

private data class TileState(
    val waterDone: Int,
    val waterTarget: Int,
    val nextTime: String,
)
