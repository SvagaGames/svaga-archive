package com.svaga.archive

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource

/** App language: Russian by default, independent of the phone language. */
object Lang {
    var code by mutableStateOf("ru")
        private set

    fun load(context: Context) {
        code = context.getSharedPreferences("svaga_settings", Context.MODE_PRIVATE).getString("lang", "ru") ?: "ru"
    }

    fun save(context: Context, value: String) {
        context.getSharedPreferences("svaga_settings", Context.MODE_PRIVATE).edit().putString("lang", value).apply()
        code = value
    }
}

fun isRussian(): Boolean = Lang.code == "ru"

val LANGUAGES = listOf(
    "ru" to "Русский",
    "en" to "English",
    "uk" to "Українська",
    "be" to "Беларуская",
    "de" to "Deutsch",
    "es" to "Español",
    "fr" to "Français",
    "it" to "Italiano",
    "pl" to "Polski",
    "pt" to "Português",
    "tr" to "Türkçe",
    "zh" to "中文",
    "ja" to "日本語",
    "ko" to "한국어",
)

private val ACH_TITLE: Map<String, Int> = mapOf(
    "collector" to R.string.ach2_collector_t,
    "conqueror" to R.string.ach2_conqueror_t,
    "explorer" to R.string.ach2_explorer_t,
    "friendship" to R.string.ach2_friendship_t,
    "social" to R.string.ach2_social_t,
    "popular" to R.string.ach2_popular_t,
    "activity" to R.string.ach2_activity_t,
    "expert" to R.string.ach2_expert_t,
    "pioneer" to R.string.ach2_pioneer_t,
    "founder" to R.string.ach2_founder_t,
    "svaga_explorer" to R.string.ach2_svaga_explorer_t,
    "supporter" to R.string.ach2_supporter_t,
    "anniversary" to R.string.ach2_anniversary_t,
    "player" to R.string.ach2_player_t,
    "planner" to R.string.ach2_planner_t,
    "favorite" to R.string.ach2_favorite_t,
    "quitter" to R.string.ach2_quitter_t,
    "masterpiece" to R.string.ach2_masterpiece_t,
    "harsh" to R.string.ach2_harsh_t,
    "chronicler" to R.string.ach2_chronicler_t,
    "marathon" to R.string.ach2_marathon_t,
    "emulator" to R.string.ach2_emulator_t,
    "shelf" to R.string.ach2_shelf_t,
    "cib" to R.string.ach2_cib_t,
    "oldschool" to R.string.ach2_oldschool_t,
    "archaeologist" to R.string.ach2_archaeologist_t,
    "mobile" to R.string.ach2_mobile_t,
    "archivist" to R.string.ach2_archivist_t,
    "cartographer" to R.string.ach2_cartographer_t,
    "localizer" to R.string.ach2_localizer_t,
    "generous" to R.string.ach2_generous_t,
    "veteran" to R.string.ach2_veteran_t,
    "retrohunter" to R.string.ach2_retrohunter_t,
    "full_house" to R.string.ach2_full_house_t,
    "perfectionist" to R.string.ach2_perfectionist_t,
    "ra_link" to R.string.ach2_ra_link_t,
    "steam_link" to R.string.ach2_steam_link_t,
    "svaga_fan" to R.string.ach2_svaga_fan_t,
    "hidden_gem" to R.string.ach2_hidden_gem_t,
)

private val ACH_DESC: Map<String, Int> = mapOf(
    "collector" to R.string.ach2_collector_d,
    "conqueror" to R.string.ach2_conqueror_d,
    "friendship" to R.string.ach2_friendship_d,
    "social" to R.string.ach2_social_d,
    "popular" to R.string.ach2_popular_d,
    "activity" to R.string.ach2_activity_d,
    "expert" to R.string.ach2_expert_d,
    "pioneer" to R.string.ach2_pioneer_d,
    "founder" to R.string.ach2_founder_d,
    "svaga_explorer" to R.string.ach2_svaga_explorer_d,
    "supporter" to R.string.ach2_supporter_d,
    "anniversary" to R.string.ach2_anniversary_d,
    "player" to R.string.ach2_player_d,
    "planner" to R.string.ach2_planner_d,
    "favorite" to R.string.ach2_favorite_d,
    "quitter" to R.string.ach2_quitter_d,
    "masterpiece" to R.string.ach2_masterpiece_d,
    "harsh" to R.string.ach2_harsh_d,
    "chronicler" to R.string.ach2_chronicler_d,
    "marathon" to R.string.ach2_marathon_d,
    "emulator" to R.string.ach2_emulator_d,
    "shelf" to R.string.ach2_shelf_d,
    "cib" to R.string.ach2_cib_d,
    "oldschool" to R.string.ach2_oldschool_d,
    "archaeologist" to R.string.ach2_archaeologist_d,
    "mobile" to R.string.ach2_mobile_d,
    "archivist" to R.string.ach2_archivist_d,
    "cartographer" to R.string.ach2_cartographer_d,
    "localizer" to R.string.ach2_localizer_d,
    "generous" to R.string.ach2_generous_d,
    "veteran" to R.string.ach2_veteran_d,
    "retrohunter" to R.string.ach2_retrohunter_d,
    "full_house" to R.string.ach2_full_house_d,
    "perfectionist" to R.string.ach2_perfectionist_d,
    "ra_link" to R.string.ach2_ra_link_d,
    "steam_link" to R.string.ach2_steam_link_d,
    "svaga_fan" to R.string.ach2_svaga_fan_d,
    "hidden_gem" to R.string.ach2_hidden_gem_d,
)

private val EXPLORER_DESC = listOf(
    R.string.ach2_explorer_d1, R.string.ach2_explorer_d2, R.string.ach2_explorer_d3,
    R.string.ach2_explorer_d4, R.string.ach2_explorer_d5,
)

private val LEVEL_NAMES = listOf(
    R.string.lvl_1, R.string.lvl_2, R.string.lvl_3, R.string.lvl_4, R.string.lvl_5,
    R.string.lvl_6, R.string.lvl_7, R.string.lvl_8, R.string.lvl_9, R.string.lvl_10,
)

private val TIER_NAMES = listOf(R.string.tier_1, R.string.tier_2, R.string.tier_3, R.string.tier_4, R.string.tier_5)

val ACH_CATEGORIES = listOf("collector", "conqueror", "explorer", "friendship", "social", "popular", "activity", "expert", "player", "planner", "favorite", "quitter", "masterpiece", "harsh", "chronicler", "marathon", "emulator", "shelf", "cib", "oldschool", "archaeologist", "mobile", "archivist", "cartographer", "localizer", "generous", "veteran", "retrohunter", "theme", "season", "secret")

@Composable
fun achTitle(code: String): String = when (code) {
    "theme" -> stringResource(R.string.ach_cat_theme)
    "season" -> stringResource(R.string.ach_cat_season)
    else -> ACH_TITLE[code]?.let { stringResource(it) } ?: DynAch.map[code]?.title ?: code
}

/** Text for the tier the player is working on (or for the last one when everything is done). */
@Composable
fun achDescription(code: String, tier: Int, goal: Int): String {
    if (code == "explorer") return stringResource(EXPLORER_DESC[(tier - 1).coerceIn(0, 4)])
    DynAch.map[code]?.let { d ->
        val text = d.description ?: return stringResource(R.string.ach_dyn_default, goal)
        return runCatching { String.format(text, goal) }.getOrDefault(text)
    }
    val res = ACH_DESC[code] ?: return ""
    return if (code in setOf("pioneer", "founder", "svaga_explorer", "supporter", "anniversary", "full_house", "perfectionist", "ra_link", "steam_link", "svaga_fan", "hidden_gem")) stringResource(res)
    else stringResource(res, goal)
}

/** Levels 11..100: nine ranks of ten steps each (I..X). */
private val RANK_NAMES = listOf(
    R.string.rank_1, R.string.rank_2, R.string.rank_3, R.string.rank_4, R.string.rank_5,
    R.string.rank_6, R.string.rank_7, R.string.rank_8, R.string.rank_9,
)
private val ROMAN10 = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

@Composable
fun levelName(level: Int): String = when {
    level <= LEVEL_NAMES.size -> stringResource(LEVEL_NAMES[(level - 1).coerceAtLeast(0)])
    level <= 100 -> stringResource(RANK_NAMES[(level - 11) / 10]) + " " + ROMAN10[(level - 11) % 10]
    else -> stringResource(R.string.rank_beyond, level - 100)
}

@Composable
fun tierName(tier: Int): String = stringResource(TIER_NAMES[(tier - 1).coerceIn(0, 4)])

val ROMAN = listOf("", "I", "II", "III", "IV", "V")

data class FilterItem(val id: Int, val en: String, val ru: String) {
    fun label(): String = if (isRussian()) ru else en
}

val SECTION_KEYS = listOf("popular", "svaga", "top", "new", "upcoming", "retro")

@Composable
fun sectionLabel(key: String): String = stringResource(
    when (key) {
        "svaga" -> R.string.sec_svaga
        "top" -> R.string.sec_top
        "new" -> R.string.sec_new
        "upcoming" -> R.string.sec_upcoming
        "retro" -> R.string.sec_retro
        else -> R.string.sec_popular
    }
)

val GENRE_FILTERS = listOf(
    FilterItem(5, "Shooter", "Шутер"),
    FilterItem(12, "RPG", "РПГ"),
    FilterItem(31, "Adventure", "Приключения"),
    FilterItem(10, "Racing", "Гонки"),
    FilterItem(15, "Strategy", "Стратегия"),
    FilterItem(14, "Sport", "Спорт"),
    FilterItem(13, "Simulator", "Симулятор"),
    FilterItem(9, "Puzzle", "Головоломка"),
    FilterItem(8, "Platformer", "Платформер"),
    FilterItem(4, "Fighting", "Файтинг"),
    FilterItem(32, "Indie", "Инди"),
    FilterItem(33, "Arcade", "Аркада"),
    FilterItem(25, "Beat em up", "Beat em up"),
    FilterItem(2, "Point-and-click", "Квест"),
    FilterItem(16, "Turn-based", "Пошаговая"),
    FilterItem(24, "Tactical", "Тактика"),
    FilterItem(34, "Visual novel", "Визуальная новелла"),
)

val PLATFORM_FILTERS = listOf(
    FilterItem(6, "PC", "PC"),
    FilterItem(167, "PS5", "PS5"),
    FilterItem(48, "PS4", "PS4"),
    FilterItem(169, "Xbox Series", "Xbox Series"),
    FilterItem(49, "Xbox One", "Xbox One"),
    FilterItem(130, "Switch", "Switch"),
    FilterItem(9, "PS3", "PS3"),
    FilterItem(12, "Xbox 360", "Xbox 360"),
    FilterItem(8, "PS2", "PS2"),
    FilterItem(7, "PS1", "PS1"),
    FilterItem(5, "Wii", "Wii"),
    FilterItem(18, "NES", "NES / Dendy"),
    FilterItem(19, "SNES", "SNES"),
    FilterItem(4, "N64", "N64"),
    FilterItem(33, "Game Boy", "Game Boy"),
    FilterItem(22, "GBC", "GBC"),
    FilterItem(24, "GBA", "GBA"),
    FilterItem(20, "NDS", "NDS"),
    FilterItem(29, "Genesis", "Mega Drive"),
    FilterItem(23, "Dreamcast", "Dreamcast"),
    FilterItem(34, "Android", "Android"),
    FilterItem(39, "iOS", "iOS"),
)

private val GENRE_NAMES_RU = mapOf(
    "Shooter" to "Шутер",
    "Role-playing (RPG)" to "РПГ",
    "Adventure" to "Приключения",
    "Racing" to "Гонки",
    "Strategy" to "Стратегия",
    "Sport" to "Спорт",
    "Simulator" to "Симулятор",
    "Puzzle" to "Головоломка",
    "Platform" to "Платформер",
    "Fighting" to "Файтинг",
    "Indie" to "Инди",
    "Arcade" to "Аркада",
    "Point-and-click" to "Квест",
    "Turn-based strategy (TBS)" to "Пошаговая стратегия",
    "Real Time Strategy (RTS)" to "Стратегия в реальном времени",
    "Tactical" to "Тактика",
    "Visual Novel" to "Визуальная новелла",
    "Card & Board Game" to "Карточная / настольная",
    "Music" to "Музыкальная",
    "Quiz/Trivia" to "Викторина",
    "Pinball" to "Пинбол",
)

fun genreLabel(name: String): String = if (isRussian()) GENRE_NAMES_RU[name] ?: name else name


val GENRE_IGDB: Map<Int, List<String>> = mapOf(
    5 to listOf("Shooter"),
    12 to listOf("Role-playing (RPG)"),
    31 to listOf("Adventure"),
    10 to listOf("Racing"),
    15 to listOf("Strategy", "Real Time Strategy (RTS)", "Turn-based strategy (TBS)"),
    14 to listOf("Sport"),
    13 to listOf("Simulator"),
    9 to listOf("Puzzle"),
    8 to listOf("Platform"),
    4 to listOf("Fighting"),
    32 to listOf("Indie"),
    33 to listOf("Arcade"),
    25 to listOf("Hack and slash/Beat 'em up"),
    2 to listOf("Point-and-click"),
    16 to listOf("Turn-based strategy (TBS)"),
    24 to listOf("Tactical"),
    34 to listOf("Visual Novel"),
)

data class YearRange(val from: Int?, val to: Int?, val label: String)

val YEAR_RANGES = listOf(
    YearRange(null, 1989, "< 1990"),
    YearRange(1990, 1999, "1990-99"),
    YearRange(2000, 2009, "2000-09"),
    YearRange(2010, 2019, "2010-19"),
    YearRange(2020, null, "2020+"),
)

val RATING_STEPS = listOf(50, 60, 70, 80, 90)

/** Filters of game lists: year, genre, rating (and platform for lists that are filtered on the phone). */
data class GameFilter(
    val genre: Int? = null,
    val platform: Int? = null,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val minRating: Int? = null,
) {
    val active: Boolean get() = genre != null || platform != null || yearFrom != null || yearTo != null || minRating != null
}

fun Game.matches(f: GameFilter): Boolean {
    if (f.genre != null) {
        val names = GENRE_IGDB[f.genre] ?: emptyList()
        if (genres.none { it in names }) return false
    }
    if (f.platform != null && f.platform !in platformIds) return false
    val y = year
    if (f.yearFrom != null && (y == null || y < f.yearFrom)) return false
    if (f.yearTo != null && (y == null || y > f.yearTo)) return false
    if (f.minRating != null && (rating == null || rating < f.minRating)) return false
    return true
}
