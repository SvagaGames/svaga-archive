package com.svaga.archive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Look of every achievement: emoji on a two-colour label. */
private data class AchLook(val emoji: String, val a: Long, val b: Long)

private val LOOKS = mapOf(
    "collector" to AchLook("📚", 0xFF4F7CFF, 0xFF8B5CF6),
    "conqueror" to AchLook("🏆", 0xFFFFB547, 0xFFFF6B3D),
    "explorer" to AchLook("🧭", 0xFF22C3A6, 0xFF3DDC84),
    "friendship" to AchLook("🤝", 0xFFFF6EA9, 0xFFFF5A6A),
    "social" to AchLook("💬", 0xFF3EE0FF, 0xFF4F7CFF),
    "popular" to AchLook("❤️", 0xFFFF5A6A, 0xFFFF6EA9),
    "activity" to AchLook("🔥", 0xFFFF9F43, 0xFFFF4D4D),
    "expert" to AchLook("⭐", 0xFFFFD84D, 0xFFFFB547),
    "pioneer" to AchLook("🚀", 0xFF6366F1, 0xFF3EE0FF),
    "founder" to AchLook("👑", 0xFFFFD84D, 0xFFB8860B),
    "svaga_explorer" to AchLook("🦊", 0xFFFF8A3D, 0xFF22C3A6),
    "supporter" to AchLook("💎", 0xFF3EE0FF, 0xFFB98BFF),
    "anniversary" to AchLook("🎂", 0xFFFF6EA9, 0xFFB98BFF),
    "player" to AchLook("🎮", 0xFF3EE0FF, 0xFF22C3A6),
    "planner" to AchLook("🗓️", 0xFF6366F1, 0xFF4F7CFF),
    "favorite" to AchLook("💖", 0xFFFF6EA9, 0xFFB98BFF),
    "quitter" to AchLook("🚪", 0xFF9AA3B5, 0xFF5B6375),
    "masterpiece" to AchLook("💯", 0xFFFFD84D, 0xFFFF6B3D),
    "harsh" to AchLook("🧐", 0xFF8B5CF6, 0xFF3B3F4C),
    "chronicler" to AchLook("📝", 0xFF22C3A6, 0xFF4F7CFF),
    "marathon" to AchLook("⏱️", 0xFFFF9F43, 0xFFFFD84D),
    "emulator" to AchLook("🕹️", 0xFF3EE0FF, 0xFF8B5CF6),
    "shelf" to AchLook("📦", 0xFFB8860B, 0xFFFFB547),
    "cib" to AchLook("🎁", 0xFFFFB547, 0xFFFF6EA9),
    "oldschool" to AchLook("👾", 0xFF3DDC84, 0xFF22C3A6),
    "archaeologist" to AchLook("🦴", 0xFFC27B00, 0xFF6E4B1F),
    "mobile" to AchLook("📱", 0xFF4F7CFF, 0xFF3DDC84),
    "archivist" to AchLook("🗄️", 0xFF6366F1, 0xFFB98BFF),
    "cartographer" to AchLook("🗺️", 0xFF22C3A6, 0xFFFFB547),
    "localizer" to AchLook("🇷🇺", 0xFFFF5A6A, 0xFF4F7CFF),
    "generous" to AchLook("👍", 0xFF3DDC84, 0xFF3EE0FF),
    "veteran" to AchLook("🎖️", 0xFFB8860B, 0xFF5B6375),
    "retrohunter" to AchLook("🏅", 0xFFE8B84A, 0xFFFF6B3D),
    "full_house" to AchLook("🃏", 0xFFFF6EA9, 0xFF6366F1),
    "perfectionist" to AchLook("✨", 0xFFFFD84D, 0xFFB98BFF),
    "ra_link" to AchLook("🔗", 0xFFE8B84A, 0xFF4F7CFF),
    "steam_link" to AchLook("♨️", 0xFF4F7CFF, 0xFF14171F),
    "svaga_fan" to AchLook("🦊", 0xFFFF8A3D, 0xFFFF5A6A),
    "hidden_gem" to AchLook("💠", 0xFF3EE0FF, 0xFF6366F1),
)

/**
 * Achievement badge drawn as a game cartridge: dark shell with a cut corner and grip ridges,
 * a bright label with the emoji, and a round tier mark. Locked badges have a dim label and a lock.
 */
@Composable
fun AchievementArt(code: String, tier: Int, unlocked: Boolean, modifier: Modifier = Modifier, size: Dp = 112.dp, hidden: Boolean = false) {
    val look = LOOKS[code] ?: DynAch.map[code]?.let { AchLook(it.emoji, it.a, it.b) } ?: AchLook("❓", 0xFF5B6375, 0xFF262B36)
    val rim = if (unlocked) tierColor(tier.coerceAtLeast(1)) else Faint
    val shell = Panel2
    Box(modifier.size(width = size, height = size * 1.12f)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val r = w * 0.12f
            val cut = w * 0.2f
            val body = Path().apply {
                moveTo(r, 0f)
                lineTo(w - cut, 0f)
                lineTo(w, cut)
                lineTo(w, h - r)
                quadraticBezierTo(w, h, w - r, h)
                lineTo(r, h)
                quadraticBezierTo(0f, h, 0f, h - r)
                lineTo(0f, r)
                quadraticBezierTo(0f, 0f, r, 0f)
                close()
            }
            drawPath(body, shell)
            drawPath(body, rim, style = Stroke(width = w * 0.035f))
            // label
            val lx = w * 0.12f
            val ly = h * 0.14f
            val lw = w * 0.76f
            val lh = h * 0.56f
            val brush = if (unlocked) {
                Brush.linearGradient(listOf(Color(look.a), Color(look.b)), Offset(lx, ly), Offset(lx + lw, ly + lh))
            } else {
                Brush.linearGradient(listOf(Color(0xFF2E3442), Color(0xFF1C2029)))
            }
            drawRoundRect(brush, Offset(lx, ly), Size(lw, lh), CornerRadius(w * 0.08f))
            // grip ridges
            val gy = h * 0.79f
            for (i in 0 until 4) {
                val x = w * (0.3f + i * 0.13f)
                drawLine(rim.copy(alpha = 0.55f), Offset(x, gy), Offset(x, gy + h * 0.1f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
            }
        }
        // emoji on the label
        Box(
            Modifier.fillMaxWidth().fillMaxHeight(0.56f).offset(y = size * 1.12f * 0.14f),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (hidden) "❓" else look.emoji, fontSize = (size.value * 0.36f).sp, modifier = Modifier.alpha(if (unlocked) 1f else 0.25f))
            if (!unlocked) Icon(Icons.Filled.Lock, null, tint = Muted, modifier = Modifier.size(size * 0.22f))
        }
        if (unlocked && tier > 0) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = size * 0.04f, y = size * 0.02f)
                    .size(size * 0.3f)
                    .background(rim, CircleShape)
                    .border(3.dp, Bg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(ROMAN.getOrElse(tier) { tier.toString() }, color = Color(0xFF071018), fontSize = (size.value * 0.11f).sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
