package com.svaga.archive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Release calendar: games from the library with a future date, or every upcoming release. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(vm: AppViewModel, nav: Nav) {
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 my library, 1 all releases
    LaunchedEffect(Unit) { if (vm.upcoming.isEmpty()) vm.loadUpcoming() }
    val today = LocalDate.now()
    val from = today.minusDays(30)
    val mine = vm.library.values
        .filter { it.status != null || it.favorite }
        .mapNotNull { vm.gamesCache[it.gameId] }
        .plus(vm.upcoming.filter { vm.library[it.id]?.status != null })
        .distinctBy { it.id }
    val source = if (mode == 0) mine else vm.upcoming
    val dated = source.mapNotNull { g -> releaseDateOf(g)?.let { it to g } }
        .filter { !it.first.isBefore(from) }
        .sortedBy { it.first }
    val byMonth = dated.groupBy { YearMonth.from(it.first) }
    val locale = Locale(Lang.code)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomBarSpace)) {
        item {
            Column(Modifier.padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
                H1(stringResource(R.string.calendar_title))
                Caption(stringResource(R.string.calendar_hint), maxLines = Int.MAX_VALUE)
            }
        }
        item {
            Segmented(
                listOf(stringResource(R.string.calendar_mine), stringResource(R.string.calendar_all)),
                mode,
                { mode = it },
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (dated.isEmpty()) {
            item {
                if (vm.upcomingLoading) {
                    LoadingBox()
                } else {
                    EmptyState(
                        stringResource(R.string.calendar_empty),
                        hint = stringResource(if (mode == 0) R.string.calendar_empty_hint else R.string.no_results),
                        action = if (mode == 0) stringResource(R.string.calendar_all) else null,
                        onAction = { mode = 1 },
                    )
                }
            }
        }
        byMonth.forEach { (month, list) ->
            stickyHeader(key = "m_${month}_$mode") {
                Row(Modifier.fillMaxWidth().background(Bg).padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
                    H2(if (month.year == today.year) name else "$name ${month.year}")
                    Spacer(Modifier.width(10.dp))
                    Text(
                        list.size.toString(),
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.background(Panel, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
            items(list, key = { "g_${it.second.id}_$mode" }) { (date, g) ->
                CalendarRow(date, g, today, locale, vm.library[g.id]?.status) { nav.game(g.id) }
            }
        }
        if (mode == 1 && vm.upcomingMore) {
            item {
                SvButton(
                    stringResource(R.string.load_more),
                    { vm.loadUpcoming(append = true) },
                    Modifier.fillMaxWidth().padding(16.dp),
                    kind = BtnKind.Secondary,
                    busy = vm.upcomingLoading,
                )
            }
        }
    }
}

@Composable
private fun CalendarRow(date: LocalDate, g: Game, today: LocalDate, locale: Locale, status: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date.dayOfMonth.toString(), color = if (date == today) Cyan else Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.width(120.dp).height(72.dp)) {
            Cover(g.screenshots.firstOrNull() ?: g.cover, Modifier.fillMaxSize())
            if (status != null) Pill(statusLabel(status), statusColor(status), Modifier.align(Alignment.TopEnd).padding(4.dp), filled = true)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Body(g.name, size = 16.sp, weight = FontWeight.SemiBold, maxLines = 2)
            val plats = g.platforms.take(3).joinToString(", ")
            if (plats.isNotBlank()) Caption(plats, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        val days = ChronoUnit.DAYS.between(today, date).toInt()
        Text(
            when {
                days == 0 -> stringResource(R.string.cal_today)
                days > 0 -> stringResource(R.string.cal_in_days, days)
                else -> date.format(DateTimeFormatter.ofPattern("d MMM", locale))
            },
            color = if (days in 0..7) Cyan else Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
