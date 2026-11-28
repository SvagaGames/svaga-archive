package com.svaga.archive

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.Locale

/** IGDB platform ids the Svaga Emulator can run. */

private fun launchEmulator(context: Context, gameId: Int, name: String) {
    val intent = Intent("com.svaga.emulator.PLAY")
        .setPackage("com.svaga.emulator")
        .putExtra("gameId", gameId)
        .putExtra("gameName", name)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.emulator_missing, Toast.LENGTH_LONG).show()
    }
}

@Composable
fun GameScreen(vm: AppViewModel, id: Int, nav: Nav) {
    var addToList by remember { mutableStateOf(false) }
    if (addToList) AddToListDialog(vm, id) { addToList = false }
    LaunchedEffect(id) {
        vm.loadGameDetail(id)
        vm.ensureTranslation(id)
        vm.loadComments(id)
        vm.loadRelated(id)
        vm.loadRu(id)
        vm.extras.loadEmulator(id)
    }
    val game = vm.gamesCache[id]
    LaunchedEffect(id, game?.variantIds) { vm.loadGameStats(id) }
    val entry = vm.library[id]
    var statusSheet by remember { mutableStateOf(false) }
    var pickPlatform by remember { mutableStateOf(false) }
    var share by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    Box(Modifier.fillMaxSize().background(Bg)) {
        if (game == null) {
            Column {
                Row(Modifier.padding(12.dp)) { IconBtn(Icons.AutoMirrored.Filled.ArrowBack, nav::back) }
                LoadingBox()
            }
            return@Box
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // ---------- hero ----------
            Box(Modifier.fillMaxWidth().height(250.dp)) {
                AsyncImage(
                    model = game.screenshots.firstOrNull() ?: game.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(Panel2),
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent, Bg))))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = (-96).dp), verticalAlignment = Alignment.Bottom) {
                Cover(game.cover, Modifier.width(124.dp).aspectRatio(0.75f).border(2.dp, Stroke, CoverShape))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).padding(bottom = 4.dp)) {
                    Text(game.name, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 27.sp, maxLines = 4)
                    val line = listOfNotNull(game.year?.toString(), game.genres.take(2).joinToString(", ") { genreLabel(it) }.ifBlank { null }).joinToString(" · ")
                    if (line.isNotBlank()) Caption(line)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        game.rating?.let { r -> Pill("IGDB $r", ratingColor(r), filled = true) }
                        val st = vm.gameStats[id]
                        if (st?.avgRating != null) {
                            Spacer(Modifier.width(6.dp))
                            Pill("Svaga ★ ${st.avgRating}", Cyan, filled = true)
                        }
                    }
                }
            }
            Column(Modifier.offset(y = (-80).dp)) {
                // ---------- actions ----------
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val st = entry?.status
                    SvButton(
                        if (st != null) statusLabel(st) else stringResource(R.string.add_to_library),
                        { statusSheet = true },
                        Modifier.weight(1f),
                        kind = if (st != null) BtnKind.Ghost else BtnKind.Primary,
                        accent = if (st != null) statusColor(st) else null,
                        icon = if (st != null) statusIcon(st) else Icons.Filled.Add,
                    )
                    Spacer(Modifier.width(10.dp))
                    val fav = entry?.favorite == true
                    IconBtn(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, { vm.setFavorite(id, !fav) }, tint = if (fav) Pink else Ink, size = 50.dp)
                    Spacer(Modifier.width(10.dp))
                    IconBtn(Icons.Filled.Menu, { addToList = true }, size = 50.dp)
                    Spacer(Modifier.width(10.dp))
                    IconBtn(Icons.Filled.Share, { share = true }, size = 50.dp)
                }
                // the Creator decides which games get the Svaga Emulator button
                if (vm.extras.emulator[id]?.enabled == true) {
                    Spacer(Modifier.height(10.dp))
                    SvButton(
                        stringResource(R.string.play_in_emulator),
                        { launchEmulator(ctx, game.id, game.name) },
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        kind = BtnKind.Secondary,
                        icon = Icons.Filled.PlayArrow,
                    )
                }
                if (vm.role == "creator") {
                    Spacer(Modifier.height(10.dp))
                    EmulatorAdminCard(vm, id)
                }
                Spacer(Modifier.height(10.dp))
                PlaytimeRow(vm, id)
                TtbRow(vm, id)

                // ---------- my rating ----------
                SectionHeader(stringResource(R.string.your_rating), Modifier.padding(top = 10.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (n in 1..10) {
                        val on = entry?.rating == n
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .then(if (on) Modifier.background(AccentBrush) else Modifier.background(Panel).border(1.dp, Stroke, CircleShape))
                                .clickable { vm.setRating(id, if (on) null else n) },
                            contentAlignment = Alignment.Center,
                        ) { Text(n.toString(), color = if (on) Color(0xFF071018) else Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                // ---------- community stats ----------
                val st = vm.gameStats[id]
                if (st != null) {
                    SectionHeader(stringResource(R.string.community_title), Modifier.padding(top = 10.dp))
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CommunityStatus("playing", st.playing, R.string.gs_playing, Modifier.weight(1f))
                            CommunityStatus("backlog", st.backlog, R.string.gs_want, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CommunityStatus("completed", st.completed, R.string.gs_completed_people, Modifier.weight(1f))
                            CommunityStatus("dropped", st.dropped, R.string.gs_dropped, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(compact(st.ratingCount), stringResource(R.string.gs_ratings), Modifier.weight(1f), accent = Amber)
                            StatTile(st.avgRating?.toString() ?: "—", stringResource(R.string.gs_avg), Modifier.weight(1f), accent = Pink)
                        }
                    }
                }

                // ---------- stores ----------
                if (game.stores.isNotEmpty()) {
                    SectionHeader(stringResource(R.string.section_stores), Modifier.padding(top = 10.dp))
                    StoreRow(game.stores)
                }

                // ---------- about ----------
                if (!game.summary.isNullOrBlank()) {
                    SectionHeader(stringResource(R.string.section_about), Modifier.padding(top = 10.dp))
                    AboutBlock(vm, game)
                }

                // ---------- screenshots ----------
                if (game.screenshots.isNotEmpty()) {
                    SectionHeader(stringResource(R.string.section_screens), Modifier.padding(top = 10.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(game.screenshots) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.width(280.dp).aspectRatio(16f / 9f).clip(CardShape).background(Panel2),
                            )
                        }
                    }
                }

                // ---------- details ----------
                SectionHeader(stringResource(R.string.details_title), Modifier.padding(top = 10.dp))
                SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    InfoLine(stringResource(R.string.released), game.releaseDate?.let { formatDate(it).ifBlank { it } })
                    InfoLine(stringResource(R.string.developers), game.developers.joinToString(", "))
                    InfoLine(stringResource(R.string.genres), game.genres.joinToString(", ") { genreLabel(it) })
                    InfoLine(stringResource(R.string.platforms), game.platforms.joinToString(", "))
                    if (game.custom) InfoLine(stringResource(R.string.added_by), game.addedBy ?: stringResource(R.string.added_by_community))
                    val langs = game.languages
                    if (langs != null) {
                        InfoLine(stringResource(R.string.lang_audio), langs.audio.joinToString(", "))
                        InfoLine(stringResource(R.string.lang_subtitles), langs.subtitles.joinToString(", "))
                        InfoLine(stringResource(R.string.lang_interface), langs.ui.joinToString(", "))
                    }
                    Spacer(Modifier.height(8.dp))
                    SvButton(stringResource(R.string.platform_add), { pickPlatform = true }, kind = BtnKind.Secondary, icon = Icons.Filled.Add)
                }

                CollectionBlock(vm, game)

                RuTranslationsBlock(vm, id)

                val rel = vm.related[id]
                if (rel != null) {
                    GameShelf(stringResource(R.string.related_prequels), rel.prequels, vm, nav::game, Modifier.padding(top = 10.dp))
                    GameShelf(stringResource(R.string.related_sequels), rel.sequels, vm, nav::game, Modifier.padding(top = 10.dp))
                }

                // ---------- note ----------
                SectionHeader(stringResource(R.string.section_note), Modifier.padding(top = 10.dp))
                var note by rememberSaveable(id, entry?.note) { mutableStateOf(entry?.note.orEmpty()) }
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SvField(note, { note = it.take(2000) }, stringResource(R.string.note_hint), singleLine = false, minLines = 3)
                    if (note.trim() != entry?.note.orEmpty().trim()) {
                        Spacer(Modifier.height(8.dp))
                        SvButton(stringResource(R.string.save), { vm.setNote(id, note) }, kind = BtnKind.Secondary, icon = Icons.Filled.Check)
                    }
                }

                // ---------- comments ----------
                val comments = vm.comments[id].orEmpty()
                SectionHeader(stringResource(R.string.comments_title), Modifier.padding(top = 10.dp), count = comments.size)
                var draft by rememberSaveable(id) { mutableStateOf("") }
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SvField(draft, { draft = it.take(1000) }, stringResource(R.string.comment_hint), singleLine = false, minLines = 2)
                    vm.commentError?.let { Body(stringResource(it), color = Red, size = 14.sp) }
                    SvButton(stringResource(R.string.comment_send), {
                        vm.postComment(id, draft.trim())
                        draft = ""
                    }, Modifier.fillMaxWidth(), enabled = draft.isNotBlank())
                    if (comments.isEmpty()) Caption(stringResource(R.string.comment_empty))
                    comments.forEach { c ->
                        CommentCard(
                            c,
                            canDelete = c.userId == vm.userId || vm.role == "creator",
                            onLike = { vm.toggleLike(c) },
                            onDelete = { vm.deleteComment(c) },
                            onOpenUser = { nav.user(c.userId) },
                        )
                    }
                }

                if (game.custom && vm.role == "creator") {
                    var confirm by remember { mutableStateOf(false) }
                    SvButton(stringResource(R.string.admin_delete_game), { confirm = true }, Modifier.fillMaxWidth().padding(16.dp), kind = BtnKind.Danger, icon = Icons.Filled.Delete)
                    if (confirm) ConfirmDialog(stringResource(R.string.admin_delete_game), game.name, { confirm = false; vm.adminDeleteGame(id) { nav.back() } }, { confirm = false })
                }
                Spacer(Modifier.height(16.dp))
                Caption(stringResource(R.string.data_credit), Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(24.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            IconBtn(Icons.AutoMirrored.Filled.ArrowBack, nav::back)
        }
    }

    val shareGame = game
    if (share && shareGame != null) {
        ShareDialog(shareGame.name + " — " + DOWNLOAD_PAGE, { share = false }) {
            GameShareCard(shareGame, entry, vm.playtimeByGame[id])
        }
    }
    if (statusSheet) StatusSheet(entry?.status, onPick = { vm.setStatus(id, it); statusSheet = false }, onDismiss = { statusSheet = false })
    if (pickPlatform && game != null) {
        PlatformPicker(
            vm.platforms.filter { it.id !in game.platformIds },
            null,
            onSelect = {
                pickPlatform = false
                if (it != null) vm.addPlatform(id, it)
            },
            onDismiss = { pickPlatform = false },
            showAll = false,
            title = stringResource(R.string.platform_add),
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(Modifier.padding(vertical = 6.dp)) {
        Caption(label, maxLines = 1)
        Body(value, size = 15.sp)
    }
}

@Composable
private fun AboutBlock(vm: AppViewModel, game: Game) {
    var showOriginal by rememberSaveable(game.id) { mutableStateOf(false) }
    var expanded by rememberSaveable(game.id) { mutableStateOf(false) }
    val tr = vm.translations["${game.id}:${Lang.code}"]
    val text = if (tr != null && !showOriginal) tr else game.summary.orEmpty()
    Column(Modifier.padding(horizontal = 16.dp)) {
        Body(text, Modifier.clickable { expanded = !expanded }, size = 15.sp, maxLines = if (expanded) Int.MAX_VALUE else 6)
        Row(Modifier.padding(top = 6.dp)) {
            if (tr != null) {
                Body(
                    stringResource(if (showOriginal) R.string.show_translation else R.string.show_original),
                    Modifier.clickable { showOriginal = !showOriginal }.padding(vertical = 6.dp),
                    color = Cyan,
                    size = 14.sp,
                    weight = FontWeight.SemiBold,
                )
            } else if (Lang.code != "en" && !game.custom) {
                val key = "${game.id}:${Lang.code}"
                if (vm.translating[key] == true) {
                    Body(stringResource(R.string.translating), Modifier.padding(vertical = 6.dp), color = Muted, size = 14.sp)
                } else {
                    Body(
                        stringResource(R.string.translate),
                        Modifier.clickable { vm.ensureTranslation(game.id) }.padding(vertical = 6.dp),
                        color = Cyan,
                        size = 14.sp,
                        weight = FontWeight.SemiBold,
                    )
                }
            }
        }
        vm.translationErrors["${game.id}:${Lang.code}"]?.let { if (tr == null) Caption(stringResource(it), color = Red) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusSheet(current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Bg) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            H2(stringResource(R.string.section_status), Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Caption(stringResource(R.string.status_hint), Modifier.padding(horizontal = 20.dp), maxLines = Int.MAX_VALUE)
            Spacer(Modifier.height(8.dp))
            STATUSES.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(s) }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).background(statusColor(s).copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(statusIcon(s), null, tint = statusColor(s), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Body(statusLabel(s), Modifier.weight(1f), size = 17.sp, weight = if (s == current) FontWeight.Bold else FontWeight.Normal)
                    if (s == current) Icon(Icons.Filled.Check, null, tint = statusColor(s))
                }
            }
            if (current != null) {
                Divider1(Modifier.padding(vertical = 6.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(null) }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Delete, null, tint = Red)
                    Spacer(Modifier.width(12.dp))
                    Body(stringResource(R.string.remove_from_library), color = Red, size = 17.sp)
                }
            }
        }
    }
}

/** Russian translations (fan patches) of a game; anyone can add one, Svaga's are marked. */
@Composable
fun RuTranslationsBlock(vm: AppViewModel, gameId: Int) {
    val uri = LocalUriHandler.current
    val list = vm.ruList[gameId].orEmpty()
    var adding by rememberSaveable(gameId) { mutableStateOf(false) }
    var studio by rememberSaveable(gameId) { mutableStateOf("") }
    var platform by rememberSaveable(gameId) { mutableStateOf("") }
    var version by rememberSaveable(gameId) { mutableStateOf("") }
    var url by rememberSaveable(gameId) { mutableStateOf("") }

    SectionHeader(stringResource(R.string.ru_title), Modifier.padding(top = 10.dp), count = list.size)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (list.isEmpty()) Caption(stringResource(R.string.ru_empty))
        list.forEach { t ->
            SvCard(Modifier.fillMaxWidth(), border = if (t.svaga) Amber else null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Body(t.studio, Modifier.weight(1f), color = if (t.svaga) Amber else Ink, size = 16.sp, weight = FontWeight.SemiBold)
                    if (t.svaga) Pill("Svaga", Amber, filled = true)
                }
                val line = listOfNotNull(t.platform, t.version?.let { "v$it" }).joinToString(" · ")
                if (line.isNotBlank()) Caption(line)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    val link = t.url
                    if (!link.isNullOrBlank()) {
                        Body(stringResource(R.string.ru_open), Modifier.clickable { runCatching { uri.openUri(link) } }, color = Cyan, size = 14.sp, weight = FontWeight.SemiBold)
                    }
                    if (t.mine || vm.role == "creator") {
                        Body(stringResource(R.string.comment_delete), Modifier.clickable { vm.deleteRu(gameId, t.id) }, color = Red, size = 14.sp)
                    }
                }
            }
        }
        if (!adding) {
            SvButton(stringResource(R.string.ru_add), { adding = true }, kind = BtnKind.Secondary, icon = Icons.Filled.Add)
        } else {
            SvField(studio, { studio = it.take(80) }, stringResource(R.string.ru_studio))
            SvField(platform, { platform = it.take(40) }, stringResource(R.string.ru_platform))
            SvField(version, { version = it.take(30) }, stringResource(R.string.ru_version))
            SvField(url, { url = it.trim() }, stringResource(R.string.ru_url))
            vm.ruError?.let { Body(stringResource(it), color = Red, size = 14.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SvButton(stringResource(R.string.save), {
                    vm.addRu(gameId, studio, platform, version, url) {
                        adding = false
                        studio = ""
                        platform = ""
                        version = ""
                        url = ""
                    }
                }, Modifier.weight(1f), enabled = studio.trim().length >= 2)
                SvButton(stringResource(R.string.cancel), { adding = false }, Modifier.weight(1f), kind = BtnKind.Secondary)
            }
        }
    }
}

@Suppress("unused")
private fun fmt1(v: Double): String = String.format(Locale.US, "%.1f", v)

/** "🎮 Играют 99" — a status with its icon and how many players have it. */
@Composable
private fun CommunityStatus(status: String, n: Int, label: Int, modifier: Modifier = Modifier) {
    val c = statusColor(status)
    Row(
        modifier.clip(RoundedCornerShape(18.dp)).background(Panel).border(1.dp, com.svaga.archive.Stroke, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(statusIcon(status), null, tint = c, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(label), color = Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(compact(n.toLong()), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private val STORE_LOOKS = mapOf(
    "steam" to Triple("Steam", 0xFF1B2838, 0xFF66C0F4),
    "epic" to Triple("Epic Games Store", 0xFF2A2A2A, 0xFFFFFFFF),
    "gog" to Triple("GOG", 0xFF5C2D91, 0xFFE4C7FF),
    "playstation" to Triple("PlayStation Store", 0xFF00439C, 0xFFFFFFFF),
    "xbox" to Triple("Xbox Store", 0xFF107C10, 0xFFFFFFFF),
    "microsoft" to Triple("Microsoft Store", 0xFF0067B8, 0xFFFFFFFF),
    "nintendo" to Triple("Nintendo eShop", 0xFFE60012, 0xFFFFFFFF),
    "itch" to Triple("itch.io", 0xFFFA5C5C, 0xFFFFFFFF),
    "appstore" to Triple("App Store", 0xFF0D84FF, 0xFFFFFFFF),
    "googleplay" to Triple("Google Play", 0xFF01875F, 0xFFFFFFFF),
    "amazon" to Triple("Amazon", 0xFF232F3E, 0xFFFF9900),
    "gamejolt" to Triple("Game Jolt", 0xFF2F7F6F, 0xFFCCFF00),
)

/** Where the game is sold: a chip per store; "search" links are a store search by name, shown outlined. */
@Composable
private fun StoreRow(stores: List<StoreLink>) {
    val ctx = LocalContext.current
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(stores, key = { it.store }) { s ->
            val look = STORE_LOOKS[s.store] ?: Triple(s.store, 0xFF2A2F3A, 0xFFFFFFFF)
            val bg = Color(look.second)
            val fg = Color(look.third)
            val shape = RoundedCornerShape(50)
            Row(
                Modifier.clip(shape)
                    .then(if (s.search) Modifier.border(1.5.dp, bg.copy(alpha = 0.9f).takeIf { look.second != 0xFF2A2A2A } ?: Ink.copy(alpha = 0.5f), shape) else Modifier.background(bg, shape))
                    .clickable { openInBrowser(ctx, s.url) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (s.search) Icons.Filled.Search else Icons.Filled.ShoppingCart, null, tint = if (s.search) Ink else fg, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(look.first, color = if (s.search) Ink else fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
    Caption(stringResource(R.string.stores_hint), Modifier.padding(horizontal = 20.dp, vertical = 6.dp), maxLines = Int.MAX_VALUE)
}
