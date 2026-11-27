package com.svaga.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.temporal.ChronoUnit

private val SeasonBrush get() = Brush.linearGradient(listOf(Color(0xFF3EE0FF), Color(0xFF4F7CFF), Color(0xFFB98BFF)))

private fun daysUntil(iso: String): Long? =
    runCatching { ChronoUnit.HOURS.between(Instant.now(), Instant.parse(iso)).coerceAtLeast(0) }.getOrNull()?.let { (it + 23) / 24 }

/** "2026Q4" -> "IV · 2026" */
private fun seasonLabel(key: String): String {
    val y = key.take(4)
    val q = key.substringAfter('Q', "").toIntOrNull() ?: return key
    return listOf("I", "II", "III", "IV").getOrElse(q - 1) { "$q" } + " · " + y
}

@Composable
fun SeasonTile(vm: AppViewModel, nav: Nav, modifier: Modifier = Modifier) {
    val s = vm.flair.season
    LaunchedEffect(Unit) { if (vm.flair.season == null) vm.flair.loadSeason() }
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier.clip(shape).background(Brush.linearGradient(listOf(Color(0xFF4F7CFF).copy(alpha = 0.32f), Color(0xFFB98BFF).copy(alpha = 0.14f), Panel)))
            .border(1.dp, Color(0xFF4F7CFF).copy(alpha = 0.6f), shape).clickable { nav.go(Routes.SEASON) }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(SeasonBrush), contentAlignment = Alignment.Center) {
            Text(if (s == null) "🎟️" else "${s.level}", color = Color(0xFF071018), fontSize = if (s == null) 22.sp else 20.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Body(stringResource(R.string.pass_title), weight = FontWeight.Bold, maxLines = 1)
            if (s == null) {
                Caption(stringResource(R.string.season_tile_new), maxLines = 2)
            } else {
                val left = s.quests.count { !it.done }
                Caption(
                    if (left > 0) stringResource(R.string.season_tile_quests, left) else stringResource(R.string.season_tile_done),
                    maxLines = 1,
                )
                Spacer(Modifier.height(5.dp))
                ProgressLine(if (s.level >= s.maxLevel) 1f else (s.xp % s.levelXp).toFloat() / s.levelXp, Color(0xFF7FA2FF))
            }
        }
    }
}

@Composable
fun SeasonScreen(vm: AppViewModel, nav: Nav) {
    val f = vm.flair
    var friends by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        f.loadSeason(fresh = true)
        if (f.cos == null) f.loadCosmetics()
    }
    LaunchedEffect(friends) { f.loadSeasonTop(friends) }
    val s = f.season
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(
            stringResource(R.string.pass_title), nav::back,
            subtitle = s?.let {
                val d = daysUntil(it.endsAt)
                stringResource(R.string.season_subtitle, seasonLabel(it.season)) + (d?.let { n -> " · " + stringResource(R.string.pass_days_left, n) } ?: "")
            },
        )
        when {
            s == null && f.seasonError != null -> { ErrorState(stringResource(f.seasonError!!), { f.loadSeason(true) }); return@Column }
            s == null -> { LoadingBox(); return@Column }
        }
        s!!
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SeasonHero(s)
            // ----- weekly quests -----
            val wd = daysUntil(s.weekEndsAt)
            SectionHeader(stringResource(R.string.season_quests), Modifier.padding(top = 8.dp))
            if (wd != null) Caption(stringResource(R.string.season_quests_reset, wd), Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            s.quests.forEach { QuestRow(it) }
            // ----- reward track -----
            SectionHeader(stringResource(R.string.season_track), Modifier.padding(top = 12.dp))
            RewardTrack(vm, s)
            // ----- where XP comes from -----
            SectionHeader(stringResource(R.string.season_sources), Modifier.padding(top = 12.dp))
            SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 14.dp) {
                s.sources.filter { it.kind in SOURCE_NAMES }.forEach { src ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(SOURCE_NAMES[src.kind]!!.first, fontSize = 18.sp, modifier = Modifier.width(32.dp))
                        Body(stringResource(SOURCE_NAMES[src.kind]!!.second), Modifier.weight(1f), maxLines = 1)
                        Caption("×${src.count}", maxLines = 1)
                        Spacer(Modifier.width(10.dp))
                        Text("+${src.xp}", color = if (src.xp > 0) Cyan else Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            // ----- top -----
            SectionHeader(stringResource(R.string.season_top), Modifier.padding(top = 12.dp))
            Segmented(
                listOf(stringResource(R.string.season_top_all), stringResource(R.string.season_top_friends)),
                if (friends) 1 else 0, { friends = it == 1 }, Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            val top = f.seasonTop
            when {
                top == null -> LoadingBox()
                top.isEmpty() -> Caption(stringResource(R.string.season_top_empty), Modifier.padding(20.dp))
                else -> top.forEach { r -> TopRow(r) { nav.user(r.userId) } }
            }
            Spacer(Modifier.height(BottomBarSpace))
        }
    }
}

private val SOURCE_NAMES = mapOf(
    "completed" to ("🏁" to R.string.season_src_completed),
    "quests" to ("🎯" to R.string.season_src_quests),
    "quiz" to ("🧩" to R.string.season_src_quiz),
    "achievements" to ("🏅" to R.string.season_src_achievements),
    "bingo" to ("🎱" to R.string.season_src_bingo),
    "days" to ("📅" to R.string.season_src_days),
    "rated" to ("⭐" to R.string.season_src_rated),
    "comments" to ("💬" to R.string.season_src_comments),
)

@Composable
private fun SeasonHero(s: SeasonPass) {
    val max = s.level >= s.maxLevel
    val into = s.xp % s.levelXp
    Column(
        Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF13234A), Color(0xFF221548), Panel)))
            .border(1.dp, Color(0xFF4F7CFF).copy(alpha = 0.5f), RoundedCornerShape(24.dp)).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(SeasonBrush), contentAlignment = Alignment.Center) {
                Text("${s.level}", color = Color(0xFF071018), fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Caption(stringResource(R.string.season_level_of, s.maxLevel), maxLines = 1)
                Text(stringResource(R.string.season_xp, s.xp), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Caption(
                    if (max) stringResource(R.string.season_max) else stringResource(R.string.season_next, s.levelXp - into, s.level + 1),
                    maxLines = 2,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        ProgressLine(if (max) 1f else into.toFloat() / s.levelXp, Color(0xFF7FA2FF))
        val next = s.rewards.firstOrNull { !it.unlocked }
        if (next != null) {
            Spacer(Modifier.height(10.dp))
            Caption(stringResource(R.string.season_next_reward, next.level, rewardName(next)), maxLines = 2)
        }
    }
}

@Composable
private fun QuestRow(q: SeasonQuest) {
    SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), padding = 12.dp, border = if (q.done) Green.copy(alpha = 0.6f) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(q.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Body(q.title, weight = FontWeight.SemiBold, maxLines = 2)
                Spacer(Modifier.height(5.dp))
                ProgressLine(q.progress.toFloat() / q.goal.coerceAtLeast(1), if (q.done) Green else Cyan)
                Spacer(Modifier.height(3.dp))
                Caption("${q.progress}/${q.goal}", maxLines = 1)
            }
            Spacer(Modifier.width(12.dp))
            if (q.done) Icon(Icons.Filled.Check, null, tint = Green, modifier = Modifier.size(26.dp))
            else Text("+${q.xp}", color = Cyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun rewardName(r: SeasonReward): String = when (r.kind) {
    "badge" -> stringResource(R.string.season_reward_badge, stringResource(BADGES[r.code]?.second ?: R.string.frame_none))
    "frame" -> stringResource(R.string.season_reward_frame, stringResource(FRAME_NAMES[r.code] ?: R.string.frame_none))
    else -> stringResource(R.string.season_reward_bg, stringResource(BG_NAMES[r.code] ?: R.string.bg_none))
}

@Composable
private fun RewardTrack(vm: AppViewModel, s: SeasonPass) {
    val byLevel = s.rewards.associateBy { it.level }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (s.level - 2).coerceIn(0, s.maxLevel - 1))
    LazyRow(state = state, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items((1..s.maxLevel).toList(), key = { it }) { lvl ->
            val reached = s.level >= lvl
            val r = byLevel[lvl]
            Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(76.dp, 92.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (r != null && reached) Color(0xFF4F7CFF).copy(alpha = 0.22f) else Panel)
                        .border(if (r != null) 1.5.dp else 1.dp, if (r != null) Color(0xFF7FA2FF).copy(alpha = if (reached) 1f else 0.4f) else com.svaga.archive.Stroke, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        r == null -> Text(if (reached) "✓" else "·", color = if (reached) Cyan else Faint, fontSize = 20.sp)
                        r.kind == "badge" -> Text(BADGES[r.code]?.first ?: "?", fontSize = 34.sp)
                        r.kind == "frame" -> FramedAvatar(vm.userId, vm.avatarVersion, vm.displayName, 56.dp, r.code)
                        else -> Box(Modifier.size(60.dp, 46.dp).clip(RoundedCornerShape(8.dp)).background(Bg).profileBackground(r.code))
                    }
                    if (r != null && !reached) {
                        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Lock, null, tint = Ink, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.level_n, lvl), color = if (reached) Cyan else Muted, fontSize = 12.sp,
                    fontWeight = if (lvl == s.level) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TopRow(r: SeasonTopRow, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (r.me) Cyan.copy(alpha = 0.08f) else Color.Transparent).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(when (r.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "${r.rank}" }, color = Muted, fontSize = 16.sp, modifier = Modifier.width(36.dp))
        Avatar(r.userId, r.avatarVersion, r.name, 36.dp)
        Spacer(Modifier.width(10.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Body(r.name, Modifier.weight(1f, fill = false), weight = FontWeight.SemiBold, maxLines = 1, color = if (r.me) Cyan else Ink)
            NickBadge(r.badge)
        }
        Caption(stringResource(R.string.level_n, r.level), maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Body("${r.xp}", color = Amber, weight = FontWeight.Bold)
    }
}
