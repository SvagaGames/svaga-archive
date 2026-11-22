package com.svaga.archive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ShadowC: Color get() = if (Palette.dark) Color(0xFF000000) else Color(0xFF2A2118)

@Composable
fun RetroCard(modifier: Modifier = Modifier, radius: Int = 12, pad: Int = 12, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = modifier.padding(end = 4.dp, bottom = 4.dp)) {
        Box(Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).background(ShadowC, RoundedCornerShape(radius.dp)))
        Column(
            Modifier
                .border(BorderStroke(2.dp, Line), RoundedCornerShape(radius.dp))
                .background(Panel, RoundedCornerShape(radius.dp))
                .padding(pad.dp),
            content = content
        )
    }
}

@Composable
fun RetroChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        label, fontFamily = Vt, fontSize = 14.sp,
        color = if (selected) Bg else Ink,
        modifier = modifier
            .clip(shape).clickable(onClick = onClick)
            .background(if (selected) Cyan else Panel, shape)
            .border(BorderStroke(2.dp, if (selected) Cyan else Line), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
fun RetroButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Cyan) {
    val shape = RoundedCornerShape(10.dp)
    Box(modifier.padding(end = 3.dp, bottom = 3.dp)) {
        Box(Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp).background(ShadowC, shape))
        Text(
            label, fontFamily = Pixel, fontSize = 12.sp, color = Bg,
            modifier = Modifier
                .clip(shape).clickable(onClick = onClick)
                .background(color, shape)
                .border(BorderStroke(2.dp, Line), shape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun RetroOutline(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Cyan) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        label, fontFamily = Pixel, fontSize = 12.sp, color = color,
        modifier = modifier
            .clip(shape).clickable(onClick = onClick)
            .border(BorderStroke(2.dp, color), shape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SectionHead(title: String, subtitle: String? = null) {
    Column {
        Text(title, fontFamily = Pixel, fontSize = 15.sp, color = Cyan, letterSpacing = 1.sp)
        if (subtitle != null) Text(subtitle, fontFamily = Vt, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun PixelTag(label: String, color: Color = Green) {
    Text(
        label, fontFamily = Pixel, fontSize = 10.sp, color = Bg,
        modifier = Modifier.background(color, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

fun Modifier.scanline(): Modifier = this.drawBehind {
    if (Palette.dark) {
        var y = 0f
        while (y < size.height) {
            drawRect(color = Color.Black, alpha = 0.05f, topLeft = Offset(0f, y), size = Size(size.width, 2f))
            y += 6f
        }
    }
}
