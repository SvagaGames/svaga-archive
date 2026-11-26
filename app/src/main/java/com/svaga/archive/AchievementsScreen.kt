package com.svaga.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun shortDate(iso: String?): String? {
    val t = parseInstant(iso) ?: return null
    return t.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale(Lang.code)))
}

private fun AchItem.titleTier(): String? = if (secret || tier <= 0) null else ROMAN.getOrNull(tier)

/** Achievements as a gallery of cartridges: open ones first, locked ones dimmed; tap for details. */
@Composable
fun AchievementsScreen(vm: AppViewModel, nav: Nav) {
    LaunchedEffect(Unit) { vm.loadAchievements() }
    var category by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableIntStateOf(0) }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf<String?>(null) }
    val ach = vm.ach
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(
            stringResource(R.string.ach_title),
            nav::back,
            subtitle = ach?.let { a -> stringResource(R.string.ach_counts, a.items.count { it.tier > 0 }, a.items.size) },
        )
        if (ach == null) {
            LoadingBox()
            return@Column
        }
        val visible = ach.items.filter { !it.hidden || it.secret }
        var list = visible.filter { category == "" || it.category == category }
        list = when (kind) {
            1 -> list.filter { it.tier > 0 }
            2 -> list.filter { !it.maxed && !it.secret }
            3 -> list.filter { it.secret }
            else -> list
        }
        list = when (sort) {
            1 -> list.sortedBy { if (it.rarity <= 0.0) 101.0 else it.rarity }
            2 -> list.sortedByDescending { if (it.maxed) 1.0 else it.progress.toDouble() / it.goal.coerceAtLeast(1) }
            else -> list.sortedWith(compareByDescending<AchItem> { it.tier > 0 }.thenByDescending { it.unlockedAt ?: "" })
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SvCard(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(stringResource(R.string.level_n, ach.level), Cyan, filled = true)
                            Spacer(Modifier.width(10.dp))
                            Body(levelName(ach.level), Modifier.weight(1f), weight = FontWeight.SemiBold, maxLines = 1)
                        }
                        Spacer(Modifier.height(10.dp))
                        ProgressLine(if (ach.levelTo > ach.levelFrom) (ach.sp - ach.levelFrom).toFloat() / (ach.levelTo - ach.levelFrom) else 1f, Cyan)
                        Spacer(Modifier.height(6.dp))
                        Caption(stringResource(R.string.level_progress, ach.sp, ach.levelTo))
                    }
                    Spacer(Modifier.height(6.dp))
                    ChipRow(Modifier.padding(horizontal = 0.dp)) {
                        listOf(R.string.ach_f_all, R.string.ach_f_received, R.string.ach_f_unfinished, R.string.ach_f_secret).forEachIndexed { i, r ->
                            SvChip(stringResource(r), kind == i, { kind = i })
                        }
                    }
                    ChipRow {
                        SvChip(stringResource(R.string.ach_cat_all), category == "", { category = "" }, accent = Amber)
                        ACH_CATEGORIES.forEach { c ->
                            SvChip(if (c == "secret") stringResource(R.string.ach_f_secret) else achTitle(c), category == c, { category = c }, accent = Amber)
                        }
                    }
                    ChipRow {
                        listOf(R.string.ach_sort_default, R.string.ach_sort_rarity, R.string.ach_sort_progress).forEachIndexed { i, r ->
                            SvChip(stringResource(r), sort == i, { sort = i }, accent = Pink)
                        }
                    }
                    if (list.isEmpty()) EmptyState(stringResource(R.string.ach_none))
                }
            }
            items(list, key = { it.code }) { a -> AchTile(a) { opened = a.code } }
        }
    }
    val current = ach?.items?.firstOrNull { it.code == opened }
    if (current != null) AchDetailSheet(current, onToggleFeatured = { vm.toggleFeatured(current.code) }, onDismiss = { opened = null })
}

@Composable
private fun AchTile(a: AchItem, onClick: () -> Unit) {
    val hiddenSecret = a.secret && a.hidden
    val open = a.tier > 0
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            AchievementArt(a.code, if (a.secret) 0 else a.tier, unlocked = open, size = 112.dp, hidden = hiddenSecret)
            if (a.featured) {
                Icon(Icons.Filled.Favorite, null, tint = Pink, modifier = Modifier.align(Alignment.TopStart).size(20.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        val title = if (hiddenSecret) stringResource(R.string.ach_secret_title) else achTitle(a.code) + (a.titleTier()?.let { " $it" } ?: "")
        Text(title, color = if (open) Ink else Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            if (open) shortDate(a.unlockedAt) ?: "" else stringResource(R.string.ach_locked),
            color = Faint, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        if (!hiddenSecret) {
            val descTier = if (a.maxed) a.tiers.size.coerceAtLeast(1) else (a.tier + 1)
            Text(achDescription(a.code, descTier, a.goal), color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 4, lineHeight = 17.sp)
            if (!a.secret && !a.maxed) {
                Spacer(Modifier.height(8.dp))
                ProgressLine(a.progress.toFloat() / a.goal.coerceAtLeast(1), if (open) tierColor(a.tier) else Cyan, Modifier.padding(horizontal = 18.dp))
                Spacer(Modifier.height(3.dp))
                Text("${a.progress} / ${a.goal}", color = Faint, fontSize = 12.sp)
            }
        } else {
            Text(stringResource(R.string.ach_secret_hint), color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 4)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchDetailSheet(a: AchItem, onToggleFeatured: () -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hiddenSecret = a.secret && a.hidden
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Bg) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AchievementArt(a.code, if (a.secret) 0 else a.tier, unlocked = a.tier > 0, size = 150.dp, hidden = hiddenSecret)
            Spacer(Modifier.height(14.dp))
            H2(if (hiddenSecret) stringResource(R.string.ach_secret_title) else achTitle(a.code) + (a.titleTier()?.let { " $it" } ?: ""))
            if (a.tier > 0) {
                Caption(
                    (if (a.secret) stringResource(R.string.ach_f_secret) else tierName(a.tier)) + (shortDate(a.unlockedAt)?.let { " · $it" } ?: ""),
                    color = tierColor(a.tier.coerceAtLeast(1)), maxLines = 1,
                )
            }
            if (a.rarity > 0) Caption(stringResource(R.string.ach_rarity, a.rarity.toString()), maxLines = 1)
            Spacer(Modifier.height(12.dp))
            if (hiddenSecret) {
                Body(stringResource(R.string.ach_secret_hint), color = Muted, align = TextAlign.Center)
            } else if (a.secret || a.tiers.isEmpty()) {
                Body(achDescription(a.code, 1, a.goal), color = Muted, align = TextAlign.Center)
            } else {
                // every tier with its goal, reward and state
                a.tiers.forEach { t ->
                    val done = t.tier <= a.tier
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).background(if (done) tierColor(t.tier) else Panel2, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done) Icon(Icons.Filled.Check, null, tint = Bg, modifier = Modifier.size(18.dp))
                            else Text(ROMAN.getOrElse(t.tier) { "" }, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Body(tierName(t.tier) + " " + ROMAN.getOrElse(t.tier) { "" }, size = 15.sp, weight = FontWeight.SemiBold, color = if (done) Ink else Muted)
                            Caption(achDescription(a.code, t.tier, t.goal), maxLines = 2)
                        }
                        Text("+${t.sp} SP", color = if (done) Green else Faint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (!a.maxed) {
                    Spacer(Modifier.height(8.dp))
                    ProgressLine(a.progress.toFloat() / a.goal.coerceAtLeast(1), Cyan)
                    Spacer(Modifier.height(4.dp))
                    Caption("${a.progress} / ${a.goal}", maxLines = 1)
                }
            }
            if (a.tier > 0) {
                Spacer(Modifier.height(18.dp))
                Caption(stringResource(R.string.ach_earned, a.earnedSp), color = Green, maxLines = 1)
                Spacer(Modifier.height(10.dp))
                SvButton(
                    stringResource(if (a.featured) R.string.ach_unpin else R.string.ach_pin),
                    onToggleFeatured,
                    Modifier.fillMaxWidth(),
                    kind = if (a.featured) BtnKind.Secondary else BtnKind.Primary,
                    icon = if (a.featured) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                )
                Caption(stringResource(R.string.ach_featured_hint), Modifier.padding(top = 8.dp), maxLines = 3)
            }
        }
    }
}
