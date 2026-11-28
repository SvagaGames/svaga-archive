package com.svaga.archive

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

const val DOWNLOAD_PAGE = "https://svagagames.github.io/svaga-archive/"
const val PRIVACY_PAGE = "https://svagagames.github.io/svaga-archive/privacy/"

@Composable
private fun Screen(title: String, nav: Nav, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(title, nav::back, subtitle = subtitle)
        Column(Modifier.weight(1f).fillMaxWidth(), content = content)
    }
}

/** Cover that can be drawn into a bitmap (software image, for share cards). */
@Composable
private fun SoftCover(url: String?, modifier: Modifier, shape: RoundedCornerShape = CoverShape) {
    val ctx = LocalContext.current
    Box(modifier.clip(shape).background(Panel2, shape)) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(url).allowHardware(false).crossfade(false).build(),
                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun money(sum: Double, currency: String): String {
    val n = if (sum % 1.0 == 0.0) String.format(Locale.US, "%,.0f", sum) else String.format(Locale.US, "%,.2f", sum)
    val sign = when (currency.uppercase()) { "RUB" -> "₽"; "USD" -> "$"; "EUR" -> "€"; "" -> ""; else -> currency }
    return "${n.replace(',', ' ')} $sign".trim()
}

// =====================================================================
// Collection
// =====================================================================

private val REGIONS = listOf("PAL", "NTSC-U", "NTSC-J", "RU")
private val CONDITIONS = listOf("mint", "good", "fair", "poor")

@Composable
private fun conditionLabel(c: String?): String = when (c) {
    "mint" -> stringResource(R.string.cond_mint)
    "good" -> stringResource(R.string.cond_good)
    "fair" -> stringResource(R.string.cond_fair)
    "poor" -> stringResource(R.string.cond_poor)
    else -> "—"
}

@Composable
private fun copyLine(c: CollectionCopy): String {
    val parts = mutableListOf<String>()
    c.platform?.let { parts += it }
    c.region?.let { parts += it }
    val kit = listOfNotNull(
        if (c.hasCart) stringResource(R.string.kit_cart) else null,
        if (c.hasBox) stringResource(R.string.kit_box) else null,
        if (c.hasManual) stringResource(R.string.kit_manual) else null,
    )
    if (c.sealed) parts += stringResource(R.string.kit_sealed) else if (kit.isNotEmpty()) parts += kit.joinToString(" + ")
    if (c.condition != null) parts += conditionLabel(c.condition)
    return parts.joinToString(" · ")
}

/** Block on the game page: my physical copies of this game. */
@Composable
fun CollectionBlock(vm: AppViewModel, game: Game) {
    val ex = vm.extras
    LaunchedEffect(game.id) { ex.loadCopies(game.id) }
    var editing by remember { mutableStateOf<CollectionCopy?>(null) }
    var adding by remember { mutableStateOf(false) }
    val list = ex.copies[game.id].orEmpty()
    SectionHeader(stringResource(R.string.collection_title), Modifier.padding(top = 10.dp), count = list.size.takeIf { it > 0 })
    SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (list.isEmpty()) {
            Caption(stringResource(R.string.collection_game_empty))
        } else {
            list.forEach { c ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { editing = c }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(34.dp).background((if (c.complete || c.sealed) Amber else Cyan).copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(if (c.complete || c.sealed) Icons.Filled.Star else Icons.Filled.Check, null, tint = if (c.complete || c.sealed) Amber else Cyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Body(copyLine(c).ifBlank { stringResource(R.string.collection_copy) }, size = 14.sp, maxLines = 2)
                        if (c.price != null) Caption(money(c.price, c.currency ?: ""))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SvButton(stringResource(R.string.collection_add), { adding = true }, kind = BtnKind.Secondary, icon = Icons.Filled.Add)
    }
    if (adding || editing != null) {
        CopyEditorSheet(vm, game, editing) { adding = false; editing = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CopyEditorSheet(vm: AppViewModel, game: Game, copy: CollectionCopy?, onClose: () -> Unit) {
    val ex = vm.extras
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var platform by remember { mutableStateOf(copy?.platform ?: game.platforms.singleOrNull()) }
    var region by remember { mutableStateOf(copy?.region) }
    var cart by remember { mutableStateOf(copy?.hasCart ?: true) }
    var box by remember { mutableStateOf(copy?.hasBox ?: false) }
    var manual by remember { mutableStateOf(copy?.hasManual ?: false) }
    var sealed by remember { mutableStateOf(copy?.sealed ?: false) }
    var condition by remember { mutableStateOf(copy?.condition) }
    var price by remember { mutableStateOf(copy?.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var currency by remember { mutableStateOf(copy?.currency ?: if (Lang.code == "ru" || Lang.code == "be") "RUB" else "USD") }
    var note by remember { mutableStateOf(copy?.note ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = state, containerColor = Bg) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            H2(stringResource(if (copy == null) R.string.collection_add else R.string.collection_edit))
            Caption(game.name)
            if (game.platforms.isNotEmpty()) {
                SheetLabel(stringResource(R.string.platforms))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    game.platforms.forEach { p -> SvChip(p, platform == p, { platform = if (platform == p) null else p }) }
                }
            }
            SheetLabel(stringResource(R.string.collection_region))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                REGIONS.forEach { r -> SvChip(r, region == r, { region = if (region == r) null else r }) }
            }
            SheetLabel(stringResource(R.string.collection_kit))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SvChip(stringResource(R.string.kit_cart), cart, { cart = !cart })
                SvChip(stringResource(R.string.kit_box), box, { box = !box })
                SvChip(stringResource(R.string.kit_manual), manual, { manual = !manual })
                SvChip(stringResource(R.string.kit_sealed), sealed, { sealed = !sealed }, accent = Amber)
            }
            SheetLabel(stringResource(R.string.collection_condition))
            Segmented(CONDITIONS.map { conditionLabel(it) }, CONDITIONS.indexOf(condition), { i -> condition = if (condition == CONDITIONS[i]) null else CONDITIONS[i] })
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                SvField(price, { v -> price = v.filter { it.isDigit() || it == '.' || it == ',' }.take(12) }, stringResource(R.string.collection_price), Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                Spacer(Modifier.width(10.dp))
                Segmented(listOf("RUB", "USD", "EUR"), listOf("RUB", "USD", "EUR").indexOf(currency), { currency = listOf("RUB", "USD", "EUR")[it] }, Modifier.width(170.dp))
            }
            Spacer(Modifier.height(12.dp))
            SvField(note, { note = it.take(500) }, stringResource(R.string.collection_note), singleLine = false, minLines = 2)
            ex.copyError?.let { Spacer(Modifier.height(8.dp)); Body(stringResource(it), color = Red, size = 14.sp) }
            Spacer(Modifier.height(16.dp))
            SvButton(
                stringResource(R.string.save),
                {
                    ex.saveCopy(
                        CollectionCopy(
                            id = copy?.id ?: 0, gameId = game.id, platform = platform, region = region,
                            hasCart = cart, hasBox = box, hasManual = manual, sealed = sealed, condition = condition,
                            price = price.replace(',', '.').toDoubleOrNull(), currency = currency,
                            acquiredOn = copy?.acquiredOn, note = note.trim().ifBlank { null },
                        ),
                        onClose,
                    )
                },
                Modifier.fillMaxWidth(),
                busy = ex.copyBusy,
            )
            if (copy != null) {
                Spacer(Modifier.height(10.dp))
                SvButton(stringResource(R.string.collection_delete), { confirmDelete = true }, Modifier.fillMaxWidth(), kind = BtnKind.Danger, icon = Icons.Filled.Delete)
            }
        }
    }
    if (confirmDelete && copy != null) {
        ConfirmDialog(stringResource(R.string.collection_delete), copyLine(copy), { confirmDelete = false; ex.deleteCopy(copy); onClose() }, { confirmDelete = false })
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

/** "My collection": every physical copy, totals by currency. */
@Composable
fun CollectionScreen(vm: AppViewModel, nav: Nav) {
    val ex = vm.extras
    LaunchedEffect(Unit) { ex.loadCollection() }
    val r = ex.collection
    Screen(stringResource(R.string.collection_mine), nav, subtitle = stringResource(R.string.collection_subtitle)) {
        when {
            r == null && ex.collectionError != null -> ErrorState(stringResource(ex.collectionError!!), { ex.loadCollection() })
            r == null -> LoadingBox()
            r.items.isEmpty() -> EmptyState(stringResource(R.string.collection_empty), hint = stringResource(R.string.collection_empty_hint))
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(compact(r.games), stringResource(R.string.collection_games), Modifier.weight(1f), accent = Cyan)
                        StatTile(compact(r.copies), stringResource(R.string.collection_copies), Modifier.weight(1f))
                        StatTile(compact(r.complete), stringResource(R.string.collection_complete), Modifier.weight(1f), accent = Amber)
                    }
                    if (r.totals.isNotEmpty()) {
                        SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Caption(stringResource(R.string.collection_value))
                            r.totals.forEach { t -> Text(money(t.sum, t.currency), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                items(r.items, key = { it.id }) { c ->
                    val g = vm.gamesCache[c.gameId]
                    Row(
                        Modifier.fillMaxWidth().clickable { nav.game(c.gameId) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Cover(g?.cover, Modifier.width(52.dp).aspectRatio(0.75f))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Body(g?.name ?: "#${c.gameId}", weight = FontWeight.SemiBold, maxLines = 2)
                            Caption(copyLine(c), maxLines = 2)
                        }
                        if (c.price != null) Text(money(c.price, c.currency ?: ""), color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// =====================================================================
// Steam import
// =====================================================================

@Composable
fun SteamImportScreen(vm: AppViewModel, nav: Nav) {
    val ex = vm.extras
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { ex.loadSteam() }
    var profile by remember { mutableStateOf("") }
    var playtime by remember { mutableStateOf(true) }
    Screen(stringResource(R.string.steam_title), nav, subtitle = stringResource(R.string.steam_subtitle)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            val st = ex.steam
            if (st != null && !st.configured) {
                SvCard(Modifier.fillMaxWidth(), border = Amber.copy(alpha = 0.6f)) { Body(stringResource(R.string.err_steam_off), color = Amber, size = 14.sp) }
                Spacer(Modifier.height(12.dp))
            }
            Body(stringResource(R.string.steam_hint), color = Muted, size = 14.sp)
            Spacer(Modifier.height(14.dp))
            SvField(profile, { profile = it.take(200) }, stringResource(R.string.steam_profile))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body(stringResource(R.string.steam_playtime), Modifier.weight(1f), size = 15.sp)
                Switch(playtime, { playtime = it }, colors = SwitchDefaults.colors(checkedTrackColor = Cyan, checkedThumbColor = Color.White))
            }
            Spacer(Modifier.height(8.dp))
            Caption(stringResource(R.string.steam_private_hint), maxLines = Int.MAX_VALUE)
            Spacer(Modifier.height(16.dp))
            SvButton(stringResource(R.string.steam_import), { ex.importSteam(profile, playtime) }, Modifier.fillMaxWidth(), enabled = profile.isNotBlank() && st?.configured != false, busy = ex.steamBusy)
            Spacer(Modifier.height(8.dp))
            SvButton(stringResource(R.string.steam_open_privacy), { openInBrowser(ctx, "https://steamcommunity.com/my/edit/settings") }, Modifier.fillMaxWidth(), kind = BtnKind.Ghost)
            ex.steamError?.let { Spacer(Modifier.height(10.dp)); Body(stringResource(it), color = Red, size = 14.sp) }
            ex.steamResult?.let { r ->
                Spacer(Modifier.height(16.dp))
                SvCard(Modifier.fillMaxWidth(), border = Green.copy(alpha = 0.5f)) {
                    H2(stringResource(R.string.steam_done))
                    Spacer(Modifier.height(8.dp))
                    Body(stringResource(R.string.steam_result, r.owned, r.matched, r.added, r.already), size = 14.sp)
                    if (r.hours > 0) Body(stringResource(R.string.steam_hours, r.hours), size = 14.sp, color = Cyan)
                    if (r.unmatched.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Caption(stringResource(R.string.steam_unmatched) + " " + r.unmatched.joinToString(", "), maxLines = 6)
                    }
                }
            }
            st?.importedAt?.let { Spacer(Modifier.height(12.dp)); Caption(stringResource(R.string.steam_last, relativeTime(it))) }
        }
    }
}

// =====================================================================
// RetroAchievements
// =====================================================================

private val RaGold = Color(0xFFE8B84A)

@Composable
fun RaScreen(vm: AppViewModel, nav: Nav) {
    val ex = vm.extras
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { ex.loadRa() }
    val st = ex.ra
    var name by remember { mutableStateOf("") }
    var unlink by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    Screen("RetroAchievements", nav, subtitle = stringResource(R.string.ra_subtitle)) {
        when {
            st == null && ex.raError != null -> ErrorState(stringResource(ex.raError!!), { ex.loadRa() })
            st == null -> LoadingBox()
            !st.configured -> EmptyState(stringResource(R.string.err_ra_off))
            !st.linked || st.summary == null -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Body(stringResource(R.string.ra_hint), color = Muted, size = 14.sp)
                Spacer(Modifier.height(14.dp))
                SvField(name, { name = it.take(32) }, stringResource(R.string.ra_username))
                Spacer(Modifier.height(14.dp))
                SvButton(stringResource(R.string.ra_link), { ex.linkRa(name) }, Modifier.fillMaxWidth(), enabled = name.trim().length >= 2, busy = ex.raBusy)
                Spacer(Modifier.height(8.dp))
                SvButton(stringResource(R.string.ra_register), { openInBrowser(ctx, "https://retroachievements.org/createaccount.php") }, Modifier.fillMaxWidth(), kind = BtnKind.Ghost)
                ex.raError?.let { Spacer(Modifier.height(10.dp)); Body(stringResource(it), color = Red, size = 14.sp) }
            }
            else -> {
                val s = st.summary!!
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(64.dp).clip(CircleShape).background(Panel2).border(2.dp, RaGold, CircleShape)) {
                                AsyncImage(s.avatar, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                H2(s.user)
                                s.presence?.let { Caption(it, maxLines = 2) }
                            }
                            IconBtn(Icons.Filled.Refresh, { ex.loadRa(refresh = true) })
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(compact(s.points), stringResource(R.string.ra_points), Modifier.weight(1f), accent = RaGold)
                            StatTile(compact(s.beaten), stringResource(R.string.ra_beaten), Modifier.weight(1f), accent = Green)
                            StatTile(compact(s.mastered), stringResource(R.string.ra_mastered), Modifier.weight(1f), accent = Amber)
                        }
                        Spacer(Modifier.height(12.dp))
                        SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Body(stringResource(R.string.ra_import_hint), color = Muted, size = 14.sp)
                            Spacer(Modifier.height(10.dp))
                            SvButton(stringResource(R.string.ra_import), { ex.importRa() }, Modifier.fillMaxWidth(), busy = ex.raBusy)
                            ex.raImport?.let { r ->
                                Spacer(Modifier.height(8.dp))
                                Body(stringResource(R.string.ra_import_result, r.added, r.updated), size = 14.sp, color = Green)
                                if (r.unmatched.isNotEmpty()) Caption(stringResource(R.string.steam_unmatched) + " " + r.unmatched.joinToString(", "), maxLines = 5)
                            }
                            ex.raError?.let { Body(stringResource(it), color = Red, size = 14.sp) }
                        }
                        Spacer(Modifier.height(8.dp))
                        UnderlineTabs(listOf(stringResource(R.string.ra_games), stringResource(R.string.ra_recent)), tab, { tab = it }, Modifier.padding(horizontal = 16.dp))
                    }
                    if (tab == 0) {
                        items(s.games, key = { "g" + it.raId }) { g ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Panel2)) {
                                    AsyncImage(g.icon, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Body(g.title, weight = FontWeight.SemiBold, maxLines = 1, size = 14.sp)
                                    Caption("${g.console} · ${g.earned}/${g.total}", maxLines = 1)
                                    Spacer(Modifier.height(4.dp))
                                    ProgressLine(if (g.total > 0) g.earned.toFloat() / g.total else 0f, if (g.mastered) Amber else if (g.beaten) Green else Cyan)
                                }
                                if (g.award != null) {
                                    Spacer(Modifier.width(10.dp))
                                    Pill(stringResource(if (g.mastered) R.string.ra_mastered_one else R.string.ra_beaten_one), if (g.mastered) Amber else Green)
                                }
                            }
                        }
                    } else {
                        if (s.recent.isEmpty()) item { EmptyState(stringResource(R.string.ra_recent_empty)) }
                        items(s.recent, key = { "a" + it.id + it.at }) { a ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Panel2).border(if (a.hardcore) 2.dp else 0.dp, RaGold, RoundedCornerShape(10.dp))) {
                                    AsyncImage(a.badge, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Body(a.title, weight = FontWeight.SemiBold, maxLines = 1, size = 14.sp)
                                    Caption(a.description, maxLines = 2)
                                    Caption("${a.game} · ${relativeTime(a.at.replace(' ', 'T') + "Z")}", maxLines = 1, color = Faint)
                                }
                                Text("+${a.points}", color = RaGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                        SvButton(stringResource(R.string.ra_unlink), { unlink = true }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), kind = BtnKind.Danger)
                    }
                }
            }
        }
    }
    if (unlink) ConfirmDialog(stringResource(R.string.ra_unlink), null, { unlink = false; ex.unlinkRa() }, { unlink = false })
}

/** Compact RA card for profiles. */
@Composable
fun RaProfileCard(state: RaState?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val s = state?.summary ?: return
    SvCard(modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onClick, border = RaGold.copy(alpha = 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Panel2)) {
                AsyncImage(s.avatar, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Body("RetroAchievements · ${s.user}", weight = FontWeight.SemiBold, size = 14.sp, maxLines = 1)
                Caption(
                    stringResource(R.string.ra_card_line, compact(s.points), s.beaten, s.mastered),
                    maxLines = 1,
                )
            }
        }
        val icons = s.games.filter { it.icon != null }.take(6)
        if (icons.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                icons.forEach { g ->
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Panel2).border(if (g.mastered) 2.dp else 0.dp, Amber, RoundedCornerShape(8.dp))) {
                        AsyncImage(g.icon, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }
        }
    }
}

// =====================================================================
// Share cards
// =====================================================================

private fun shareBitmap(ctx: Context, bmp: Bitmap, text: String) {
    val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "svaga-share.png")
    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, text)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Preview of a share card with a "Share" button; the card is drawn into a PNG. */
@Composable
fun ShareDialog(shareText: String, onDismiss: () -> Unit, card: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var busy by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
            ) { card() }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SvButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f), kind = BtnKind.Secondary)
                SvButton(
                    stringResource(R.string.share),
                    {
                        busy = true
                        scope.launch {
                            try {
                                val bmp = layer.toImageBitmap().asAndroidBitmap()
                                withContext(Dispatchers.IO) { shareBitmap(ctx, bmp, shareText) }
                                onDismiss()
                            } catch (e: Exception) {
                                ctx.startActivity(
                                    Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText), null)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            } finally {
                                busy = false
                            }
                        }
                    },
                    Modifier.weight(1f),
                    busy = busy,
                    icon = Icons.Filled.Share,
                )
            }
        }
    }
}

@Composable
private fun CardFrame(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier
            .width(320.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF141A26), Color(0xFF0A0C11))))
            .border(1.dp, Color(0xFF262B36), shape)
            .padding(20.dp),
        content = content,
    )
}

@Composable
private fun CardFooter() {
    Spacer(Modifier.height(16.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF262B36)))
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("SVAGA", color = Color(0xFF3EE0FF), fontFamily = Pixel, fontSize = 11.sp)
        Spacer(Modifier.width(6.dp))
        Text("ARCHIVE", color = Color(0xFFF3F5F9), fontFamily = Pixel, fontSize = 8.sp)
        Spacer(Modifier.weight(1f))
        Text("svagagames.github.io", color = Color(0xFF9AA3B5), fontSize = 11.sp)
    }
}

@Composable
fun GameShareCard(game: Game, entry: LibraryItem?, seconds: Long?) {
    CardFrame {
        Row {
            SoftCover(game.cover, Modifier.width(110.dp).aspectRatio(0.75f))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(game.name, color = Color(0xFFF3F5F9), fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp, maxLines = 4)
                val line = listOfNotNull(game.year?.toString(), game.platforms.firstOrNull()).joinToString(" · ")
                if (line.isNotBlank()) Text(line, color = Color(0xFF9AA3B5), fontSize = 13.sp, maxLines = 2)
                Spacer(Modifier.height(10.dp))
                entry?.status?.let { s -> StatusPill(s) }
                entry?.rating?.let { r ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = ratingColor(r), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("$r/10", color = ratingColor(r), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (seconds != null && seconds >= 3600) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.hours_short, hours(seconds)), color = Color(0xFF3EE0FF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        CardFooter()
    }
}

@Composable
fun ProfileShareCard(vm: AppViewModel) {
    val lib = vm.library.values.filter { it.status != null || it.favorite }
    val name = vm.displayName.ifBlank { stringResource(R.string.player) }
    val covers = lib.filter { it.status == "completed" || it.status == "playing" }.sortedByDescending { it.updatedAt }
        .mapNotNull { vm.gamesCache[it.gameId]?.cover }.take(4)
    CardFrame {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1C2029)).border(2.dp, AccentBrush, CircleShape), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), color = Color(0xFF3EE0FF), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (vm.avatarVersion > 0) SoftCover(avatarUrl(vm.userId, vm.avatarVersion), Modifier.fillMaxSize(), CircleShapeRounded)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, color = Color(0xFFF3F5F9), fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(stringResource(R.string.level_n, vm.ach?.level ?: vm.level) + " · " + levelName(vm.ach?.level ?: vm.level), color = Color(0xFF9AA3B5), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            ShareNumber(compact(lib.size), stringResource(R.string.num_games), Modifier.weight(1f))
            ShareNumber(compact(lib.count { it.status == "completed" }), stringResource(R.string.num_completed), Modifier.weight(1f))
            ShareNumber(vm.playSeconds?.let { hours(it) } ?: "—", stringResource(R.string.num_hours), Modifier.weight(1f))
            ShareNumber(compact(vm.ach?.items.orEmpty().count { it.tier > 0 }), stringResource(R.string.ach_title), Modifier.weight(1f))
        }
        if (covers.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                covers.forEach { SoftCover(it, Modifier.width(64.dp).aspectRatio(0.75f), RoundedCornerShape(10.dp)) }
            }
        }
        CardFooter()
    }
}

private val CircleShapeRounded = RoundedCornerShape(50)

@Composable
private fun ShareNumber(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, color = Color(0xFFF3F5F9), fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Color(0xFF9AA3B5), fontSize = 11.sp, maxLines = 1)
    }
}

// =====================================================================
// Account deletion, updates, privacy (settings)
// =====================================================================

@Composable
fun DeleteAccountDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val ex = vm.extras
    var typed by remember { mutableStateOf("") }
    val word = stringResource(R.string.delete_word)
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(CardShape).background(Panel).padding(20.dp)) {
            H2(stringResource(R.string.delete_account), color = Red)
            Spacer(Modifier.height(10.dp))
            Body(stringResource(R.string.delete_account_text), color = Muted, size = 14.sp)
            Spacer(Modifier.height(12.dp))
            SvField(typed, { typed = it.take(20) }, stringResource(R.string.delete_type_word, word))
            ex.deleteError?.let { Spacer(Modifier.height(8.dp)); Body(stringResource(it), color = Red, size = 14.sp) }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SvButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f), kind = BtnKind.Secondary)
                SvButton(
                    stringResource(R.string.delete_forever), { ex.deleteAccount() }, Modifier.weight(1f), kind = BtnKind.Danger,
                    enabled = typed.trim().equals(word, ignoreCase = true), busy = ex.deleting,
                )
            }
        }
    }
}

/** Row in settings: current version, check for updates (when the store build can), store page. */
@Composable
fun UpdatesRow() {
    val u = UpdateState.updater
    val subtitle = when {
        UpdateState.checking -> stringResource(R.string.update_checking)
        UpdateState.ready -> stringResource(R.string.update_ready)
        UpdateState.offer != null -> UpdateState.offer?.let { offerLabel(it) }?.let { stringResource(R.string.update_available_v, it) } ?: stringResource(R.string.update_available)
        UpdateState.upToDate == true -> stringResource(R.string.update_latest)
        UpdateState.failed -> stringResource(R.string.update_failed)
        else -> stringResource(R.string.update_store_hint)
    }
    SettingRow(
        Icons.Filled.Refresh,
        stringResource(R.string.update_title) + " · " + BuildConfig.VERSION_NAME,
        subtitle = subtitle,
        onClick = when {
            u == null || !u.canCheck -> null
            UpdateState.ready -> ({ u.install() })
            UpdateState.offer != null -> ({ UpdateState.dismissed = false; UpdateState.failed = false; u.start() })
            else -> ({ UpdateState.failed = false; u.check(manual = true) })
        },
    )
}

// =====================================================================
// Creator: Svaga Emulator button and download for a game
// =====================================================================

@Composable
fun EmulatorAdminCard(vm: AppViewModel, gameId: Int) {
    val ex = vm.extras
    val info = ex.emulator[gameId]
    var open by remember { mutableStateOf(false) }
    var enabled by remember(info) { mutableStateOf(info?.enabled ?: false) }
    var url by remember(info) { mutableStateOf(info?.romUrl ?: "") }
    var name by remember(info) { mutableStateOf(info?.romName ?: "") }
    SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), border = Amber.copy(alpha = 0.5f)) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Body(stringResource(R.string.emu_admin_title), color = Amber, weight = FontWeight.SemiBold, size = 14.sp)
                Caption(
                    when {
                        info?.enabled != true -> stringResource(R.string.emu_admin_off)
                        info.romUrl != null -> stringResource(R.string.emu_admin_on_download)
                        else -> stringResource(R.string.emu_admin_on)
                    },
                )
            }
            Text(if (open) "▲" else "▼", color = Amber)
        }
        if (open) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body(stringResource(R.string.emu_admin_button), Modifier.weight(1f), size = 14.sp)
                Switch(enabled, { enabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = Amber, checkedThumbColor = Color.White))
            }
            Spacer(Modifier.height(8.dp))
            SvField(url, { url = it.trim().take(500) }, stringResource(R.string.emu_admin_url))
            Spacer(Modifier.height(8.dp))
            SvField(name, { name = it.take(120) }, stringResource(R.string.emu_admin_name))
            Spacer(Modifier.height(6.dp))
            Caption(stringResource(R.string.emu_admin_hint), color = Faint, maxLines = 5)
            ex.emuError?.let { Body(stringResource(it), color = Red, size = 14.sp) }
            Spacer(Modifier.height(10.dp))
            SvButton(stringResource(R.string.save), { ex.saveEmulator(gameId, enabled, url, name) { open = false } }, Modifier.fillMaxWidth(), busy = ex.emuBusy, accent = Amber)
        }
    }
}

// =====================================================================
// Hours played, entered by hand (consoles, PC, other emulators)
// =====================================================================

@Composable
fun PlaytimeRow(vm: AppViewModel, gameId: Int) {
    val secs = vm.playtimeByGame[gameId] ?: 0L
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(shape).background(Panel, shape).border(1.dp, Stroke, shape)
            .clickable { open = true }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⏱", fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Body(stringResource(R.string.hours_title), weight = FontWeight.SemiBold, size = 15.sp)
            Caption(if (secs > 0) hoursText(secs) else stringResource(R.string.hours_none))
        }
        Text(stringResource(if (secs > 0) R.string.hours_edit else R.string.hours_add), color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
    if (open) {
        var h by remember { mutableStateOf(if (secs > 0) (secs / 3600).toString() else "") }
        var m by remember { mutableStateOf(if (secs > 0) ((secs % 3600) / 60).toString() else "") }
        Dialog(onDismissRequest = { open = false }) {
            Column(Modifier.fillMaxWidth().clip(CardShape).background(Panel).padding(20.dp)) {
                H2(stringResource(R.string.hours_title))
                Spacer(Modifier.height(6.dp))
                Caption(stringResource(R.string.hours_hint), maxLines = 4)
                Spacer(Modifier.height(14.dp))
                Row {
                    SvField(h, { h = it.filter(Char::isDigit).take(5) }, stringResource(R.string.hours_h), Modifier.weight(1f), keyboard = KeyboardType.Number)
                    Spacer(Modifier.width(10.dp))
                    SvField(m, { v -> m = v.filter(Char::isDigit).take(2).let { if ((it.toIntOrNull() ?: 0) > 59) "59" else it } }, stringResource(R.string.hours_m), Modifier.weight(1f), keyboard = KeyboardType.Number)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 5, 10, 25).forEach { add ->
                        SvChip("+$add", false, { h = ((h.toIntOrNull() ?: 0) + add).toString() })
                    }
                }
                vm.extras.hoursError?.let { Spacer(Modifier.height(8.dp)); Body(stringResource(it), color = Red, size = 14.sp) }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (secs > 0) SvButton(stringResource(R.string.hours_reset), { vm.extras.setHours(gameId, 0.0) { open = false } }, Modifier.weight(1f), kind = BtnKind.Danger)
                    SvButton(stringResource(R.string.save), {
                        val total = (h.toIntOrNull() ?: 0) + (m.toIntOrNull() ?: 0) / 60.0
                        vm.extras.setHours(gameId, total) { open = false }
                    }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun hoursText(secs: Long): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    return if (h > 0) stringResource(R.string.hours_hm, h, m) else stringResource(R.string.hours_m_only, m)
}
