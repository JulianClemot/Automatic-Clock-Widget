@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.julian.automaticclockwidget.widgets

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Alignment.Companion.CenterHorizontally
import androidx.glance.layout.Alignment.Companion.CenterVertically
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.julian.automaticclockwidget.R
import com.julian.automaticclockwidget.clocks.ClockDisplayFormatter
import com.julian.automaticclockwidget.clocks.StoredClock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Instant

class AutomaticClockWidget : GlanceAppWidget(), KoinComponent {

    private val clocksRepo: com.julian.automaticclockwidget.clocks.ClocksPreferencesRepository by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                ClockWidgetContent()
            }
        }
    }

    @Composable
    private fun ClockWidgetContent() {
        val size = LocalSize.current

        // Load stored clocks and map to display values
        val stored = clocksRepo.getClocks().getOrElse { emptyList() }

        // Calculate how many clocks can fit based on widget size
        val clockWidth = 78.dp
        val clockHeight = 78.dp

        val clocksPerRow = maxOf(1, (size.width / clockWidth).toInt())
        val maxRows = maxOf(1, (size.height / clockHeight).toInt())
        val maxClocks = clocksPerRow * maxRows

        // Take only the clocks that can fit
        val visibleClocks = stored.take(maxClocks)
        Column(
            modifier = GlanceModifier
                .fillMaxSize(),
            verticalAlignment = CenterVertically,
            horizontalAlignment = CenterHorizontally
        ) {
            if (visibleClocks.isEmpty()) {
                Column(
                    verticalAlignment = CenterVertically,
                    horizontalAlignment = CenterHorizontally,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(25.dp) // Rounded corners like the reference
                        .background(
                            ColorProvider(
                                day = Color.White,
                                night = Color.Black
                            )
                        )
                        .padding(4.dp) // Inner padding for content

                ) {
                    Text(
                        "No clocks stored yet", style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(
                                day = Color.Black,
                                night = Color.White
                            ),
                        )
                    )
                }
            } else {
                // Group clocks into rows
                val rows = visibleClocks.chunked(clocksPerRow)

                rows.forEach { rowClocks ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = CenterHorizontally,
                        verticalAlignment = CenterVertically,
                    ) {
                        rowClocks.forEach { storedClock ->
                            Clock(storedClock = storedClock, clockWidth, clockHeight)
                            if (rowClocks.last() != storedClock) {
                                Spacer(GlanceModifier.width(8.dp))
                            }
                        }
                    }
                    if (rows.last() != rowClocks) {
                        Spacer(GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun Clock(storedClock: StoredClock, width: Dp, height: Dp) {
        val packageName = LocalContext.current.packageName
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val disp = ClockDisplayFormatter.format(storedClock.name, storedClock.timezoneId, now)
        Column(
            verticalAlignment = CenterVertically,
            horizontalAlignment = CenterHorizontally,
            modifier = GlanceModifier
                .size(width, height)
                .cornerRadius(25.dp) // Rounded corners like the reference
                .background(
                    ColorProvider(
                        day = Color.White,
                        night = Color.Black
                    )
                )
                .padding(4.dp) // Inner padding for content

        ) {
            Text(
                storedClock.name, style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(
                        day = Color.Black,
                        night = Color.White
                    ),
                ),
                maxLines = 1
            )
            Spacer(GlanceModifier.height(2.dp))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(26.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidRemoteViews(
                    RemoteViews(
                        packageName,
                        R.layout.clock
                    ).apply {
                        setString(R.id.clock, "setTimeZone", storedClock.timezoneId)
                    }
                )
            }
            Spacer(GlanceModifier.height(2.dp))
            Text(
                disp.day,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(
                        day = Color.Black,
                        night = Color.White
                    ),
                ),
                maxLines = 1,
            )
        }
    }

    override val sizeMode = SizeMode.Exact

    override suspend fun providePreview(
        context: Context,
        widgetCategory: Int
    ) {
        provideContent {
            GlanceTheme {
                ClockWidgetContent()
            }
        }
    }
}