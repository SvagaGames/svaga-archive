package com.svaga.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun MetaLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = Muted)) { append("$label: ") }
            withStyle(SpanStyle(color = Ink)) { append(value) }
        },
        fontFamily = Vt,
        fontSize = 17.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
fun MiniGameRow(title: String, games: List<Game>, onOpen: (Int) -> Unit) {
    if (games.isEmpty()) return
    SectionTitle(title)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(games, key = { it.id }) { g ->
            Column(
                Modifier
                    .width(110.dp)
                    .background(Panel)
                    .border(2.dp, Line)
                    .clickable { onOpen(g.id) },
            ) {
                Cover(g.cover, Modifier.fillMaxWidth().aspectRatio(0.75f))
                Text(
                    g.name,
                    color = Ink,
                    fontFamily = Vt,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(6.dp),
                )
                g.year?.let { Text(it.toString(), color = Muted, fontFamily = Vt, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)) }
            }
        }
    }
}

@Composable
fun RuTranslationsBlock(vm: AppViewModel, gameId: Int) {
    val uri = LocalUriHandler.current
    val list = vm.ruList[gameId].orEmpty()
    var adding by rememberSaveable(gameId) { mutableStateOf(false) }
    var studio by rememberSaveable(gameId) { mutableStateOf("") }
    var platform by rememberSaveable(gameId) { mutableStateOf("") }
    var version by rememberSaveable(gameId) { mutableStateOf("") }
    var url by rememberSaveable(gameId) { mutableStateOf("") }

    SectionTitle(stringResource(R.string.ru_title))
    if (list.isEmpty()) {
        Text(stringResource(R.string.ru_empty), color = Muted, fontFamily = Vt, fontSize = 17.sp)
    }
    list.forEach { t ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .background(Panel)
                .border(2.dp, if (t.svaga) Amber else Line)
                .padding(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t.studio, color = if (t.svaga) Amber else Ink, fontFamily = Vt, fontSize = 19.sp)
                if (t.svaga) Text("★", color = Amber, fontSize = 16.sp)
            }
            val line = listOfNotNull(t.platform, t.version?.let { "v$it" }).joinToString(" · ")
            if (line.isNotBlank()) Text(line, color = Muted, fontFamily = Vt, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (!t.url.isNullOrBlank()) {
                    Text(
                        stringResource(R.string.ru_open),
                        color = Blue,
                        fontFamily = Vt,
                        fontSize = 17.sp,
                        modifier = Modifier.clickable { runCatching { uri.openUri(t.url) } },
                    )
                }
                if (t.mine || vm.role == "creator") {
                    Text(
                        stringResource(R.string.comment_delete),
                        color = Red,
                        fontFamily = Vt,
                        fontSize = 17.sp,
                        modifier = Modifier.clickable { vm.deleteRu(gameId, t.id) },
                    )
                }
            }
        }
    }
    if (!adding) {
        PixelButton(stringResource(R.string.ru_add), { adding = true })
    } else {
        PixelField(studio, { studio = it.take(80) }, stringResource(R.string.ru_studio))
        Spacer(Modifier.height(8.dp))
        PixelField(platform, { platform = it.take(40) }, stringResource(R.string.ru_platform))
        Spacer(Modifier.height(8.dp))
        PixelField(version, { version = it.take(30) }, stringResource(R.string.ru_version))
        Spacer(Modifier.height(8.dp))
        PixelField(url, { url = it.trim() }, stringResource(R.string.ru_url))
        vm.ruError?.let {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(it), color = Red, fontFamily = Vt, fontSize = 17.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelButton(
                stringResource(R.string.save),
                {
                    vm.addRu(gameId, studio, platform, version, url) {
                        adding = false
                        studio = ""
                        platform = ""
                        version = ""
                        url = ""
                    }
                },
                filled = true,
                enabled = studio.trim().length >= 2,
            )
            PixelButton(stringResource(R.string.cancel), { adding = false }, accent = Red)
        }
    }
}

@Composable
fun GameDetailScreen(vm: AppViewModel, id: Int, onBack: () -> Unit, onOpenGame: (Int) -> Unit) {
    LaunchedEffect(id) {
        vm.loadGameDetail(id)
        vm.ensureTranslation(id)
        vm.loadComments(id)
        vm.loadRelated(id)
        vm.loadRu(id)
    }
    var showOriginal by rememberSaveable { mutableStateOf(false) }
    var pickPlatform by rememberSaveable { mutableStateOf(false) }
    val game = vm.gamesCache[id]
    val entry = vm.library[id]
    LaunchedEffect(id, game?.variantIds) { vm.loadGameStats(id) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        PixelButton("< " + stringResource(R.string.back), onBack)

        if (game == null) {
            Text(
                stringResource(R.string.loading),
                color = Muted,
                fontFamily = Vt,
                fontSize = 19.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            Row(Modifier.padding(top = 16.dp)) {
                Cover(game.cover, Modifier.width(120.dp).aspectRatio(0.75f).border(2.dp, Line))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(game.name, color = Ink, fontFamily = Pixel, fontSize = 12.sp, lineHeight = 18.sp)
                    MetaLine(stringResource(R.string.released), game.releaseDate)
                    MetaLine(stringResource(R.string.developers), game.developers.joinToString(", "))
                    if (game.custom) MetaLine(stringResource(R.string.added_by), game.addedBy ?: stringResource(R.string.added_by_community))
                    game.rating?.let { r ->
                        Text(
                            "IGDB $r",
                            color = Bg,
                            fontFamily = Pixel,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .background(ratingColor(r))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            val st = vm.gameStats[id]
            Spacer(Modifier.height(10.dp))
            Text(
                if (st != null && st.avgRating != null) {
                    stringResource(R.string.svaga_rating, st.avgRating.toString(), st.ratingCount)
                } else {
                    stringResource(R.string.svaga_rating_none)
                },
                color = Amber,
                fontFamily = Vt,
                fontSize = 17.sp,
            )

            val playCtx = androidx.compose.ui.platform.LocalContext.current
            if (game.platformIds.any { it in EMULATED_PLATFORMS }) {
                PixelButton(
                    text = stringResource(R.string.play_in_emulator),
                    onClick = { launchEmulator(playCtx, game.id, game.name) },
                    accent = Green,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }
            SectionTitle(stringResource(R.string.section_status))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                STATUSES.forEach { s ->
                    PixelChip(
                        text = statusLabel(s),
                        selected = entry?.status == s,
                        accent = statusColor(s),
                        onClick = { vm.setStatus(id, if (entry?.status == s) null else s) },
                    )
                }
            }
            Text(
                stringResource(R.string.status_hint),
                color = Muted,
                fontFamily = Vt,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(12.dp))
            val isFav = entry?.favorite == true
            PixelButton(
                text = stringResource(if (isFav) R.string.favorite_remove else R.string.favorite_add),
                onClick = { vm.setFavorite(id, !isFav) },
                accent = Pink,
                filled = isFav,
            )

            SectionTitle(stringResource(R.string.section_rating))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (n in 1..10) {
                    PixelChip(
                        text = n.toString(),
                        selected = entry?.rating == n,
                        accent = Amber,
                        onClick = { vm.setRating(id, if (entry?.rating == n) null else n) },
                    )
                }
            }

            SectionTitle(stringResource(R.string.section_note))
            var noteText by remember(entry?.note) { mutableStateOf(entry?.note ?: "") }
            PixelField(noteText, { noteText = it }, stringResource(R.string.note_hint), singleLine = false)
            Spacer(Modifier.height(8.dp))
            PixelButton(stringResource(R.string.save), onClick = { vm.setNote(id, noteText) })

            if (!game.summary.isNullOrBlank()) {
                SectionTitle(stringResource(R.string.section_about))
                val ru = vm.translations["$id:${Lang.code}"]
                val shown = if (ru != null && !showOriginal) ru else game.summary
                Text(shown, color = Ink, fontSize = 14.sp, lineHeight = 20.sp)
                if (ru != null) {
                    Text(
                        stringResource(if (showOriginal) R.string.show_translation else R.string.show_original),
                        color = Blue,
                        fontFamily = Vt,
                        fontSize = 17.sp,
                        modifier = Modifier
                            .clickable { showOriginal = !showOriginal }
                            .padding(vertical = 8.dp),
                    )
                } else if (Lang.code != "en" && !game.custom) {
                    Text(
                        stringResource(R.string.translation_unavailable),
                        color = Muted,
                        fontFamily = Vt,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            MetaLine(stringResource(R.string.genres), game.genres.joinToString(", ") { genreLabel(it) })
            MetaLine(stringResource(R.string.platforms), game.platforms.joinToString(", "))
            Spacer(Modifier.height(8.dp))
            PixelButton(stringResource(R.string.platform_add), { pickPlatform = true }, accent = Blue)
            if (pickPlatform) {
                PlatformPicker(
                    platforms = vm.platforms.filter { it.id !in game.platformIds },
                    selected = null,
                    showAll = false,
                    onSelect = {
                        pickPlatform = false
                        if (it != null) vm.addPlatform(id, it)
                    },
                    onDismiss = { pickPlatform = false },
                )
            }

            val langs = game.languages
            if (langs != null && (langs.audio.isNotEmpty() || langs.subtitles.isNotEmpty() || langs.ui.isNotEmpty())) {
                SectionTitle(stringResource(R.string.game_languages_title))
                MetaLine(stringResource(R.string.lang_audio), langs.audio.joinToString(", "))
                MetaLine(stringResource(R.string.lang_subtitles), langs.subtitles.joinToString(", "))
                MetaLine(stringResource(R.string.lang_interface), langs.ui.joinToString(", "))
            }

            RuTranslationsBlock(vm, id)

            if (game.screenshots.isNotEmpty()) {
                SectionTitle(stringResource(R.string.section_screens))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(game.screenshots) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(260.dp)
                                .aspectRatio(16f / 9f)
                                .background(Panel2)
                                .border(2.dp, Line),
                        )
                    }
                }
            }

            SectionTitle(stringResource(R.string.comments_title))
            val commentList = vm.comments[id].orEmpty()
            if (commentList.isEmpty()) {
                Text(stringResource(R.string.comment_empty), color = Muted, fontFamily = Vt, fontSize = 17.sp)
                Spacer(Modifier.height(10.dp))
            } else {
                commentList.forEach { c ->
                    CommentRow(
                        c = c,
                        canDelete = c.userId == vm.userId || vm.role == "creator",
                        onLike = { vm.toggleLike(c) },
                        onDelete = { vm.deleteComment(c) },
                    )
                }
            }
            var draft by rememberSaveable { mutableStateOf("") }
            PixelField(draft, { draft = it.take(1000) }, stringResource(R.string.comment_hint), singleLine = false)
            vm.commentError?.let { res ->
                Spacer(Modifier.height(6.dp))
                Text(stringResource(res), color = Red, fontFamily = Vt, fontSize = 17.sp)
            }
            Spacer(Modifier.height(8.dp))
            PixelButton(
                text = stringResource(R.string.comment_send),
                onClick = {
                    vm.postComment(id, draft.trim())
                    draft = ""
                },
                enabled = draft.isNotBlank(),
            )

            val rel = vm.related[id]
            if (rel != null) {
                MiniGameRow(stringResource(R.string.related_prequels), rel.prequels, onOpenGame)
                MiniGameRow(stringResource(R.string.related_sequels), rel.sequels, onOpenGame)
            }

            if (game.custom && vm.role == "creator") {
                Spacer(Modifier.height(16.dp))
                PixelButton(stringResource(R.string.admin_delete_game), { vm.adminDeleteGame(id) { onBack() } }, accent = Red)
            }

            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.data_credit), color = Muted, fontFamily = Vt, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** IGDB platform ids the Svaga Emulator can run: NES, SNES, Mega Drive, Game Boy, GBC, GBA. */
private val EMULATED_PLATFORMS = setOf(18, 19, 29, 33, 22, 24)

private fun launchEmulator(context: android.content.Context, gameId: Int, name: String) {
    val intent = android.content.Intent("com.svaga.emulator.PLAY")
        .setPackage("com.svaga.emulator")
        .putExtra("gameId", gameId)
        .putExtra("gameName", name)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, R.string.emulator_missing, android.widget.Toast.LENGTH_LONG).show()
    }
}
