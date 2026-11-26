package com.svaga.archive

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import java.util.Locale

// =====================================================================
// Theme: dark by default (near-black like modern trackers), light on demand
// =====================================================================

/** Theme choice: "system" (default), "dark" or "light". */
object ThemeMode {
    var code by mutableStateOf("system")
        private set

    fun load(context: Context) {
        code = context.getSharedPreferences("svaga_settings", Context.MODE_PRIVATE).getString("theme", "system") ?: "system"
    }

    fun save(context: Context, value: String) {
        context.getSharedPreferences("svaga_settings", Context.MODE_PRIVATE).edit().putString("theme", value).apply()
        code = value
    }
}

object Palette {
    var systemDark by mutableStateOf(true)

    val dark: Boolean
        get() = when (ThemeMode.code) {
            "light" -> false
            "dark" -> true
            else -> systemDark
        }
}

private fun pick(dark: Long, light: Long): Color = Color(if (Palette.dark) dark else light)

val Bg: Color get() = pick(0xFF0A0C11, 0xFFF3F4F7)
val Panel: Color get() = pick(0xFF14171F, 0xFFFFFFFF)
val Panel2: Color get() = pick(0xFF1C2029, 0xFFE9ECF1)
val Stroke: Color get() = pick(0xFF262B36, 0xFFD9DDE5)
val Ink: Color get() = pick(0xFFF3F5F9, 0xFF11141A)
val Muted: Color get() = pick(0xFF9AA3B5, 0xFF5E6676)
val Faint: Color get() = pick(0xFF5B6375, 0xFF9AA1AE)

/** Svaga brand colours. */
val Cyan: Color get() = pick(0xFF3EE0FF, 0xFF0596C2)
val Blue: Color get() = pick(0xFF4F7CFF, 0xFF3461E8)
val Green: Color get() = pick(0xFF3DDC84, 0xFF16A35A)
val Amber: Color get() = pick(0xFFFFB547, 0xFFC27B00)
val Red: Color get() = pick(0xFFFF5A6A, 0xFFD8293C)
val Pink: Color get() = pick(0xFFFF6EA9, 0xFFD03F7C)
val Purple: Color get() = pick(0xFFB98BFF, 0xFF6E45C8)

val AccentBrush: Brush get() = Brush.horizontalGradient(listOf(Cyan, Blue))

val Pixel = FontFamily(Font(R.font.press_start_2p))

val CardShape = RoundedCornerShape(20.dp)
val CoverShape = RoundedCornerShape(14.dp)

fun tierColor(tier: Int): Color = when (tier) {
    1 -> Color(0xFFCD7F32)
    2 -> Color(0xFFB8C0CC)
    3 -> Color(0xFFFFC233)
    4 -> Color(0xFF9FE8D8)
    5 -> Color(0xFF3EE0FF)
    else -> Muted
}

fun statusColor(key: String?): Color = when (key) {
    "backlog" -> Blue
    "wont_play" -> Muted
    "playing" -> Amber
    "completed" -> Green
    "dropped" -> Red
    "fav" -> Pink
    else -> Cyan
}

@Composable
fun statusLabel(key: String): String = stringResource(
    when (key) {
        "backlog" -> R.string.status_backlog
        "wont_play" -> R.string.status_wont_play
        "playing" -> R.string.status_playing
        "completed" -> R.string.status_completed
        "dropped" -> R.string.status_dropped
        "fav" -> R.string.filter_favorites
        else -> R.string.filter_all
    }
)

fun ratingColor(r: Int): Color = when {
    r >= 80 -> Green
    r >= 60 -> Amber
    else -> Red
}

/** Context that keeps the Activity in its wrapper chain but serves resources for the chosen language. */
private class LocaleContext(base: Context, private val localizedResources: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
}

@Composable
fun Localized(content: @Composable () -> Unit) {
    val base = LocalContext.current
    val code = Lang.code
    val pair = remember(code, base) {
        val conf = Configuration(base.resources.configuration)
        conf.setLocale(Locale(code))
        val res = base.createConfigurationContext(conf).resources
        conf to LocaleContext(base, res)
    }
    CompositionLocalProvider(
        LocalConfiguration provides pair.first,
        LocalContext provides pair.second,
    ) {
        content()
    }
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun SvagaTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    SideEffect { Palette.systemDark = systemDark }
    val dark = Palette.dark
    val view = LocalView.current
    SideEffect {
        val window = view.context.findActivity()?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(primary = Cyan, onPrimary = Bg, background = Bg, onBackground = Ink, surface = Panel, onSurface = Ink, surfaceVariant = Panel2)
        } else {
            lightColorScheme(primary = Cyan, onPrimary = Color.White, background = Bg, onBackground = Ink, surface = Panel, onSurface = Ink, surfaceVariant = Panel2)
        },
        content = content,
    )
}

// =====================================================================
// Text helpers
// =====================================================================

@Composable
fun H1(text: String, modifier: Modifier = Modifier, color: Color = Ink) =
    Text(text, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp, modifier = modifier)

@Composable
fun H2(text: String, modifier: Modifier = Modifier, color: Color = Ink) =
    Text(text, color = color, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, modifier = modifier)

@Composable
fun Body(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ink,
    size: TextUnit = 15.sp,
    maxLines: Int = Int.MAX_VALUE,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign? = null,
) = Text(
    text,
    color = color,
    fontSize = size,
    lineHeight = (size.value * 1.38f).sp,
    fontWeight = weight,
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
    textAlign = align,
    modifier = modifier,
)

@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = Muted, maxLines: Int = 2) =
    Body(text, modifier, color, 13.sp, maxLines)

/** "SVAGA ARCHIVE" logo: pixel font, brand colours. */
@Composable
fun Logo(modifier: Modifier = Modifier, size: TextUnit = 14.sp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("SVAGA", color = Cyan, fontFamily = Pixel, fontSize = size)
        Spacer(Modifier.width(8.dp))
        Text("ARCHIVE", color = Ink, fontFamily = Pixel, fontSize = size * 0.72f)
    }
}

// =====================================================================
// Buttons, chips, fields
// =====================================================================

enum class BtnKind { Primary, Secondary, Ghost, Danger }

@Composable
fun SvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: BtnKind = BtnKind.Primary,
    enabled: Boolean = true,
    busy: Boolean = false,
    icon: ImageVector? = null,
    accent: Color? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val base = modifier
        .alpha(if (enabled) 1f else 0.45f)
        .heightIn(min = 50.dp)
        .clip(shape)
    val styled = when (kind) {
        BtnKind.Primary -> if (accent != null) base.background(accent, shape) else base.background(AccentBrush, shape)
        BtnKind.Secondary -> base.background(Panel2, shape).border(1.dp, Stroke, shape)
        BtnKind.Ghost -> base.border(1.dp, accent ?: Stroke, shape)
        BtnKind.Danger -> base.background(Red.copy(alpha = 0.14f), shape).border(1.dp, Red.copy(alpha = 0.5f), shape)
    }
    val fg = when (kind) {
        BtnKind.Primary -> Color(0xFF071018)
        BtnKind.Secondary -> Ink
        BtnKind.Ghost -> accent ?: Ink
        BtnKind.Danger -> Red
    }
    Box(
        styled.clickable(enabled = enabled && !busy, onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

/** Round icon button (top bars, cards). */
@Composable
fun IconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ink,
    badge: Int = 0,
    size: Dp = 44.dp,
    filled: Boolean = true,
) {
    Box(modifier) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .then(if (filled) Modifier.background(Panel, CircleShape).border(1.dp, Stroke, CircleShape) else Modifier)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
        if (badge > 0) CountBadge(badge, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp))
    }
}

@Composable
fun CountBadge(n: Int, modifier: Modifier = Modifier) {
    Text(
        if (n > 99) "99+" else n.toString(),
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(Red, RoundedCornerShape(10.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

/** Pill chip. Selected chips are filled with the accent colour. */
@Composable
fun SvChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Cyan,
    count: Int? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.18f) else Panel, shape)
            .border(1.dp, if (selected) accent else Stroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = if (selected) accent else Ink, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(count.toString(), color = if (selected) accent else Muted, fontSize = 13.sp)
        }
    }
}

/** Underlined tabs (like the reference trackers): "News · Friends · Comments". */
@Composable
fun UnderlineTabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, badges: List<Int> = emptyList()) {
    Row(
        modifier.fillMaxWidth().horizontalScrollSafe().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        titles.forEachIndexed { i, t ->
            Column(
                Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSelect(i) }.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(t, color = if (i == selected) Ink else Faint, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    val b = badges.getOrNull(i) ?: 0
                    if (b > 0) CountBadge(b, Modifier.padding(start = 4.dp))
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.height(3.dp).width(36.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i == selected) Cyan else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun Modifier.horizontalScrollSafe(): Modifier =
    this.then(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()))

/** Segmented control: equal-width options in a rounded track. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(modifier.fillMaxWidth().background(Panel, shape).border(1.dp, Stroke, shape).padding(4.dp)) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(if (sel) Modifier.background(AccentBrush) else Modifier)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(o, color = if (sel) Color(0xFF071018) else Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

/** Text field in a rounded panel; label floats above. */
@Composable
fun SvField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        val shape = RoundedCornerShape(16.dp)
        Row(
            Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, Stroke, shape).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = singleLine,
                minLines = if (singleLine) 1 else minLines,
                textStyle = TextStyle(color = Ink, fontSize = 16.sp),
                cursorBrush = SolidColor(Cyan),
                visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
                modifier = Modifier.weight(1f),
            )
            if (password) {
                Text(
                    stringResource(if (visible) R.string.hide_password else R.string.show_password),
                    color = Cyan,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { visible = !visible }.padding(start = 10.dp),
                )
            }
        }
    }
}

/** Big rounded search bar. When [onClick] is set the bar is a button that opens the search screen. */
@Composable
fun SearchBar(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(Panel, shape)
            .border(1.dp, Stroke, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = Muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (onClick != null) {
                Text(hint, color = Faint, fontSize = 16.sp, maxLines = 1)
            } else {
                if (value.isEmpty()) Text(hint, color = Faint, fontSize = 16.sp, maxLines = 1)
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Ink, fontSize = 16.sp),
                    cursorBrush = SolidColor(Cyan),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (trailing != null) trailing()
    }
}

// =====================================================================
// Surfaces and layout pieces
// =====================================================================

@Composable
fun SvCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    border: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(CardShape)
            .background(Panel, CardShape)
            .border(1.dp, border ?: Stroke, CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Section header: big bold title, optional counter and chevron action (like "Достижения 69 >"). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    onMore: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onMore != null) Modifier.clickable(onClick = onMore) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        H2(title)
        if (count != null) {
            Spacer(Modifier.width(10.dp))
            Text(count.toString(), color = Faint, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.weight(1f))
        if (trailing != null) trailing()
        if (onMore != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted, modifier = Modifier.size(28.dp))
    }
}

/** Top bar for inner screens: round back button, title, actions. */
@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconBtn(Icons.AutoMirrored.Filled.ArrowBack, onBack)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Caption(subtitle, maxLines = 1)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Cyan, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
    }
}

/** Empty state card: title, hint and an optional action (like "Здесь пока нет игр"). */
@Composable
fun EmptyState(title: String, modifier: Modifier = Modifier, hint: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    SvCard(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), padding = 24.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Body(title, color = Cyan, size = 18.sp, weight = FontWeight.SemiBold, align = TextAlign.Center)
            if (hint != null) {
                Spacer(Modifier.height(6.dp))
                Body(hint, color = Muted, size = 14.sp, align = TextAlign.Center)
            }
            if (action != null) {
                Spacer(Modifier.height(16.dp))
                SvButton(action, onAction)
            }
        }
    }
}

@Composable
fun ErrorState(text: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Body(text, color = Red, align = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        SvButton(stringResource(R.string.retry), onRetry, kind = BtnKind.Secondary)
    }
}

/** Big number with a label under it (profile header "16.8K серий"). */
@Composable
fun StatNumber(value: String, label: String, modifier: Modifier = Modifier, color: Color = Ink) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Muted, fontSize = 14.sp, maxLines = 1)
    }
}

/** Tile of the 2x2 status grid. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Color = Ink, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(Panel, shape)
            .border(1.dp, Stroke, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 18.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = accent, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Small pill with text, e.g. a status or "IGDB 87". */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    Text(
        text,
        color = if (filled) Color(0xFF071018) else color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .background(if (filled) color else color.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/** Rating badge "★ 8.4". */
@Composable
fun StarBadge(value: String, modifier: Modifier = Modifier, color: Color = Amber) {
    Row(
        modifier.background(Color(0xCC0A0C11), RoundedCornerShape(10.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Star, null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Cover(url: String?, modifier: Modifier, shape: RoundedCornerShape = CoverShape) {
    Box(modifier.clip(shape).background(Panel2, shape)) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text("?", color = Faint, fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
        }
    }
}

/** Round avatar with an optional brand-coloured ring; falls back to the first letter. */
@Composable
fun Avatar(userId: String, version: Int, name: String, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val inner = if (ring) size - 8.dp else size
    Box(
        modifier
            .size(size)
            .then(if (ring) Modifier.border(3.dp, AccentBrush, CircleShape).padding(4.dp) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(inner).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
            Text(
                name.trim().take(1).uppercase().ifEmpty { "?" },
                color = Cyan,
                fontSize = (inner.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
            )
            if (version > 0 && userId.isNotEmpty()) {
                AsyncImage(
                    model = avatarUrl(userId, version),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun ProgressLine(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Panel2)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

/** Settings-style row: icon, title, subtitle, trailing value or chevron. */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    tint: Color = Ink,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).background(Panel2, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Body(title, color = tint, size = 16.sp, weight = FontWeight.SemiBold, maxLines = 1)
            if (subtitle != null) Caption(subtitle)
        }
        if (value != null) Text(value, color = Cyan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (trailing != null) trailing()
        if (onClick != null && value == null && trailing == null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted)
        }
    }
}

@Composable
fun Divider1(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Stroke))
}

/** Compact "1.2K" formatting for big numbers. */
fun compact(n: Long): String = when {
    n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0).replace(".0M", "M")
    n >= 10_000 -> String.format(Locale.US, "%.1fK", n / 1000.0).replace(".0K", "K")
    else -> n.toString()
}

fun compact(n: Int): String = compact(n.toLong())
