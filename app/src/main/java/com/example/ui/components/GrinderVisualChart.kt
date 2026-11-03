package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrewMethod
import com.example.data.model.Grinder
import com.example.data.model.GrinderCalculator
import com.example.ui.theme.CoffeeAmber
import com.example.ui.theme.CoffeeAmberLight
import com.example.ui.theme.CoffeeDarkBorder
import com.example.ui.theme.CoffeeDarkElevated
import com.example.ui.theme.CoffeeTeal

@Composable
fun GrinderVisualChart(
    grinder: Grinder,
    brewMethods: List<BrewMethod>,
    selectedMethodId: String,
    targetMicrons: Int,
    zeroOffset: Float,
    onMethodSelect: (BrewMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "Шкала помола по 13 методам",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Нажмите на любой метод для выбора • Диапазон 0–1400 μm",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Scale ticks header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Метод",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(105.dp)
            )
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("0", "300", "600", "900", "1200", "1400").forEach { tick ->
                        Text(
                            text = tick,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Text(
                text = "Настройка",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(75.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Chart rows
        val maxUm = 1400f
        brewMethods.forEach { bm ->
            val isSelected = bm.id == selectedMethodId
            val mr = grinder.methodRanges[bm.id]
            val isSupported = mr != null && grinder.methods[bm.id] != null && grinder.methods[bm.id] != "N/A"
            val displayRange = if (isSupported) {
                GrinderCalculator.formatRangeWithOffset(mr, grinder, zeroOffset)
            } else {
                "Вне шкалы"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) CoffeeAmber.copy(alpha = 0.15f) else Color.Transparent
                    )
                    .clickable { onMethodSelect(bm) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.width(105.dp)) {
                    Text(
                        text = bm.nameRu,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        ),
                        color = if (isSelected) CoffeeAmberLight else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${bm.minUm}–${bm.maxUm} μm",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Visual bar
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(CoffeeDarkElevated)
                        .border(1.dp, CoffeeDarkBorder, RoundedCornerShape(5.dp))
                ) {
                    val fullWidth = maxWidth

                    // 1. Grinder span background
                    val gStart = (grinder.minUm / maxUm).coerceIn(0f, 1f) * fullWidth.value
                    val gEnd = (grinder.maxUm / maxUm).coerceIn(0f, 1f) * fullWidth.value
                    val gWidth = (gEnd - gStart).coerceAtLeast(0f)

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(gWidth.dp)
                            .padding(start = gStart.dp)
                            .background(CoffeeTeal.copy(alpha = 0.08f))
                    )

                    // 2. Method range span
                    val mStart = (bm.minUm / maxUm).coerceIn(0f, 1f) * fullWidth.value
                    val mEnd = (bm.maxUm / maxUm).coerceIn(0f, 1f) * fullWidth.value
                    val mWidth = (mEnd - mStart).coerceAtLeast(0f)

                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.6f)
                            .width(mWidth.dp)
                            .padding(start = mStart.dp)
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                    )

                    // 3. Overlap span (actual available range)
                    if (isSupported && mr != null) {
                        val oStart = (mr.lowUm / maxUm).coerceIn(0f, 1f) * fullWidth.value
                        val oEnd = (mr.highUm / maxUm).coerceIn(0f, 1f) * fullWidth.value
                        val oWidth = (oEnd - oStart).coerceAtLeast(4f)

                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.8f)
                                .width(oWidth.dp)
                                .padding(start = oStart.dp)
                                .background(
                                    brush = Brush.horizontalGradient(listOf(CoffeeAmber, CoffeeAmberLight)),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }

                    // 4. Current Target Micron marker
                    val markerX = (targetMicrons / maxUm).coerceIn(0f, 1f) * fullWidth.value
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.5.dp)
                            .padding(start = markerX.dp)
                            .background(CoffeeTeal)
                    )
                }

                Text(
                    text = displayRange,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.5.sp
                    ),
                    color = if (isSupported) {
                        if (isSelected) CoffeeAmberLight else MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.width(75.dp),
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }
    }
}
