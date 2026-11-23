package com.svaga.archive

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.compose.foundation.layout.fillMaxWidth

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

val Bg: Color get() = if (Palette.dark) Color(0xFF0C1424) else Color(0xFFF3F0E6)
val Panel: Color get() = if (Palette.dark) Color(0xFF111D33) else Color(0xFFFFFFFF)
val Panel2: Color get() = if (Palette.dark) Color(0xFF0A1120) else Color(0xFFEAE5D8)
val Ink: Color get() = if (Palette.dark) Color(0xFFE9EFF8) else Color(0xFF17202F)
val Muted: Color get() = if (Palette.dark) Color(0xFF8FA3BF) else Color(0xFF5C6B80)
val Line: Color get() = if (Palette.dark) Color(0xFF26364F) else Color(0xFFC7CEDA)
val Cyan: Color get() = if (Palette.dark) Color(0xFF3EE0FF) else Color(0xFF0B8DB0)
val Red: Color get() = if (Palette.dark) Color(0xFFFF4D5E) else Color(0xFFD0293A)
val Blue: Color get() = if (Palette.dark) Color(0xFF4DA6FF) else Color(0xFF1E6BD0)
val Green: Color get() = if (Palette.dark) Color(0xFF5DF08D) else Color(0xFF1C9A4E)
val Amber: Color get() = if (Palette.dark) Color(0xFFFFC233) else Color(0xFFB87A00)
val Pink: Color get() = if (Palette.dark) Color(0xFFFF6EA9) else Color(0xFFD03F7C)
val Purple: Color get() = if (Palette.dark) Color(0xFFB98BFF) else Color(0xFF6E45C8)

val Pixel = FontFamily(Font(R.font.press_start_2p))
val Vt = FontFamily(Font(R.font.body_font))

fun tierColor(tier: Int): Color = when (tier) {
    1 -> Color(0xFFCD7F32)
    2 -> Color(0xFFB8C0CC)
    3 -> Amber
    4 -> Color(0xFF9FE8D8)
    5 -> Cyan
    else -> Muted
}

fun statusColor(key: String?): Color = when (key) {
    "backlog" -> Blue
    "wont_play" -> Muted
    "playing" -> Amber
    "completed" -> Green
    "dropped" -> Red
    else -> Muted
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

private fun Context.findActivity(): Activity? {
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
            darkColorScheme(primary = Cyan, onPrimary = Bg, background = Bg, onBackground = Ink, surface = Panel, onSurface = Ink)
        } else {
            lightColorScheme(primary = Cyan, onPrimary = Bg, background = Bg, onBackground = Ink, surface = Panel, onSurface = Ink)
        },
        content = content,
    )
}

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Cyan,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val bg = if (filled) accent else Panel2
    val fg = if (filled) Bg else accent
    Box(modifier = modifier.alpha(if (enabled) 1f else 0.4f).padding(end = 3.dp, bottom = 3.dp)) {
        Box(Modifier.fillMaxSize().offset(x = 3.dp, y = 3.dp).background(RetroShadow, RoundedCornerShape(10.dp)))
        Box(
            modifier = Modifier
                .background(bg, RoundedCornerShape(10.dp))
                .border(2.dp, accent, RoundedCornerShape(10.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = fg, fontFamily = Pixel, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun PixelChip(
    text: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(if (selected) accent else Panel2, RoundedCornerShape(8.dp))
            .border(2.dp, if (selected) accent else Line, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Bg else Ink, fontFamily = Vt, fontSize = 17.sp)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Cyan,
        fontFamily = Pixel,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
fun PixelField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontFamily = Vt, fontSize = 15.sp) },
        singleLine = singleLine,
        shape = RectangleShape,
        trailingIcon = if (password) {
            {
                Text(
                    stringResource(if (visible) R.string.hide_password else R.string.show_password),
                    color = Cyan,
                    fontFamily = Vt,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { visible = !visible }.padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        } else {
            null
        },
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
        textStyle = TextStyle(fontFamily = Vt, fontSize = 17.sp, color = Ink),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cyan,
            unfocusedBorderColor = Line,
            focusedLabelColor = Cyan,
            unfocusedLabelColor = Muted,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            cursorColor = Cyan,
            focusedContainerColor = Panel2,
            unfocusedContainerColor = Panel2,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
