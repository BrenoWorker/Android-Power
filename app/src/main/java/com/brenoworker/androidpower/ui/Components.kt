package com.brenoworker.androidpower.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brenoworker.androidpower.ui.theme.LevelGood
import com.brenoworker.androidpower.ui.theme.LevelLow
import com.brenoworker.androidpower.ui.theme.LevelMedium

fun levelColor(level: Int): Color = when {
    level <= 15 -> LevelLow
    level <= 40 -> LevelMedium
    else -> LevelGood
}

@Composable
fun LevelRing(level: Int, caption: String, modifier: Modifier = Modifier) {
    val color = levelColor(level)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val sweep by animateFloatAsState(targetValue = level / 100f * 360f, label = "level")
    Box(modifier.size(210.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
            drawArc(color, -90f, sweep, false, Offset(inset, inset), arcSize, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$level%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, positive: Boolean? = null) {
    val valueColor = when (positive) {
        true -> MaterialTheme.colorScheme.primary
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurface
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
fun LineChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    minValue: Float? = null,
    maxValue: Float? = null
) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val rows = 4
        for (i in 0..rows) {
            val y = size.height * i / rows
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        if (values.size < 2) return@Canvas
        var low = minValue ?: values.min()
        var high = maxValue ?: values.max()
        if (high - low < 1f) {
            low -= 1f
            high += 1f
        }
        val range = high - low
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / (values.size - 1)
            val y = size.height - (value.coerceIn(low, high) - low) / range * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
