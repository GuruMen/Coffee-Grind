package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoffeeAmber
import com.example.ui.theme.CoffeeAmberLight
import com.example.ui.theme.CoffeeDarkBorder
import com.example.ui.theme.CoffeeDarkElevated
import com.example.ui.theme.CoffeeGreen
import com.example.ui.theme.CoffeeTeal
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.CoffeeGrindViewModel

@Composable
fun DatabaseScreen(
    viewModel: CoffeeGrindViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf("ALL") }
    val garageMap by viewModel.garageSettings.collectAsState()

    val filteredGrinders = remember(searchQuery, selectedBrand, garageMap) {
        val q = searchQuery.trim().lowercase()
        viewModel.repository.grinders.filter { g ->
            val matchBrand = selectedBrand == "ALL" || g.brand == selectedBrand
            val matchQuery = q.isEmpty() ||
                g.fullName.lowercase().contains(q) ||
                g.unit.lowercase().contains(q) ||
                g.brand.lowercase().contains(q) ||
                g.model.lowercase().contains(q)
            matchBrand && matchQuery
        }.sortedByDescending { garageMap[it.key]?.isFavorite == true }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search & Count Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "База кофемолок (${filteredGrinders.size} из ${viewModel.repository.grinders.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Поиск: Comandante, Fellow, K-Ultra, Timemore...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("db_search_input"),
            shape = RoundedCornerShape(8.dp)
        )

        // Brand scrollable chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedBrand == "ALL",
                onClick = { selectedBrand = "ALL" },
                label = { Text("Все бренды (${viewModel.repository.brands.size})", fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CoffeeAmber.copy(alpha = 0.2f),
                    selectedLabelColor = CoffeeAmberLight
                )
            )
            viewModel.repository.brands.forEach { b ->
                FilterChip(
                    selected = selectedBrand == b,
                    onClick = { selectedBrand = b },
                    label = { Text(b, fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CoffeeAmber.copy(alpha = 0.2f),
                        selectedLabelColor = CoffeeAmberLight
                    )
                )
            }
        }

        Text(
            text = "Нажмите на любую карточку, чтобы открыть её в калькуляторе помола:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // List of grinders
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredGrinders, key = { it.idx }) { g ->
                val isFav = garageMap[g.key]?.isFavorite == true
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            viewModel.selectGrinder(g)
                            viewModel.switchTab(AppTab.CALCULATOR)
                        },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isFav) CoffeeAmber else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isFav) {
                                        Text("★ ", color = CoffeeAmberLight, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "${g.brand} ${g.model}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isFav) CoffeeAmberLight else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = g.unit,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${g.minUm}–${g.maxUm} μm",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CoffeeTeal
                                )
                                Text(
                                    text = "Шкала: ${g.minScale}–${g.maxScale}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Method settings pills
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CoffeeDarkElevated,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    "espresso" to "Эспрессо",
                                    "v60" to "V60",
                                    "steep_and_release" to "Switch",
                                    "aeropress" to "Аэропресс",
                                    "french_press" to "Френч-пресс",
                                    "cold_brew" to "Колд Брю"
                                ).forEach { (mid, mName) ->
                                    val s = g.methods[mid]
                                    if (s != null && s != "N/A") {
                                        Text(
                                            text = "$mName: $s",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
