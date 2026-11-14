package app.vdvoem.feature.store

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import java.util.Locale

private data class PlannedModule(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int
)

private val plannedModules = listOf(
    PlannedModule(Icons.Filled.PlayArrow, R.string.store_music_title, R.string.store_music_description),
    PlannedModule(Icons.Filled.Lock, R.string.store_steam_title, R.string.store_steam_description),
    PlannedModule(Icons.Filled.Star, R.string.store_countdown_title, R.string.store_countdown_description),
    PlannedModule(Icons.Filled.Edit, R.string.store_notes_title, R.string.store_notes_description),
    PlannedModule(Icons.AutoMirrored.Filled.List, R.string.store_tasks_title, R.string.store_tasks_description),
    PlannedModule(Icons.Filled.Face, R.string.store_mood_title, R.string.store_mood_description)
)

@Composable
fun StoreScreen(
    registry: ModuleRegistry,
    enabledIds: Set<String>,
    onToggle: (moduleId: String, enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val available = registry.modules.filter { it.manifest.id != StoreModule.ID && !it.manifest.comingSoon }
    val enabledCount = available.count { it.manifest.id in enabledIds }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var showDisableAllDialog by rememberSaveable { mutableStateOf(false) }
    val normalizedQuery = searchQuery.trim().lowercase(Locale.ROOT)
    val visibleModules = available.filter { module ->
        val matchesCategory = selectedCategory == "ALL" || module.manifest.category.name == selectedCategory
        val title = stringResource(module.manifest.titleRes)
        val description = stringResource(module.manifest.descriptionRes)
        val matchesQuery = normalizedQuery.isEmpty() ||
            title.lowercase(Locale.ROOT).contains(normalizedQuery) ||
            description.lowercase(Locale.ROOT).contains(normalizedQuery)
        matchesCategory && matchesQuery
    }
    val categories = listOf(
        "ALL" to stringResource(R.string.store_filter_all),
        ModuleCategory.CORE.name to stringResource(R.string.store_filter_core),
        ModuleCategory.TOGETHER.name to stringResource(R.string.store_filter_together),
        ModuleCategory.PERSONAL.name to stringResource(R.string.store_filter_personal),
        ModuleCategory.FUN.name to stringResource(R.string.store_filter_fun)
    )

    if (showDisableAllDialog) {
        AlertDialog(
            onDismissRequest = { showDisableAllDialog = false },
            title = { Text(stringResource(R.string.store_disable_all_title)) },
            text = { Text(stringResource(R.string.store_disable_all_message, visibleModules.count { it.manifest.id in enabledIds })) },
            confirmButton = {
                TextButton(onClick = {
                    visibleModules.filter { it.manifest.id in enabledIds }
                        .forEach { onToggle(it.manifest.id, false) }
                    showDisableAllDialog = false
                }) {
                    Text(stringResource(R.string.store_disable_all_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableAllDialog = false }) {
                    Text(stringResource(R.string.store_cancel))
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = VdvoemSpacing.screenHorizontal,
            end = VdvoemSpacing.screenHorizontal,
            top = VdvoemSpacing.lg,
            bottom = VdvoemSpacing.listBottom
        ),
        verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
    ) {
        item {
            VdvoemCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(VdvoemSizes.iconLg)
                    )
                    Spacer(modifier = Modifier.size(VdvoemSpacing.md))
                    Text(
                        text = stringResource(R.string.store_hero_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                Text(
                    text = stringResource(R.string.store_hero_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.size(VdvoemSpacing.md))
                InfoPill(text = stringResource(R.string.store_enabled_count, enabledCount, available.size))
                Spacer(modifier = Modifier.size(VdvoemSpacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
                ) {
                    Button(
                        onClick = {
                            visibleModules.filter { it.manifest.id !in enabledIds }
                                .forEach { onToggle(it.manifest.id, true) }
                        },
                        enabled = visibleModules.any { it.manifest.id !in enabledIds },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.store_enable_all))
                    }
                    OutlinedButton(
                        onClick = { showDisableAllDialog = true },
                        enabled = visibleModules.any { it.manifest.id in enabledIds },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.store_disable_all))
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text(stringResource(R.string.store_search_label)) }
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)) {
                Text(
                    text = stringResource(R.string.store_filter_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
                ) {
                    categories.forEach { (category, label) ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(label) }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.store_soon_filter_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (visibleModules.isEmpty()) {
            item(key = "empty_search") {
                EmptyState(
                    icon = Icons.Filled.Search,
                    title = stringResource(R.string.store_no_results_title),
                    message = stringResource(R.string.store_no_results_message),
                    actionLabel = stringResource(R.string.store_reset_filters),
                    onAction = {
                        searchQuery = ""
                        selectedCategory = "ALL"
                    }
                )
            }
        } else {
            ModuleCategory.entries.forEach { category ->
                val categoryModules = visibleModules.filter { it.manifest.category == category }
                if (categoryModules.isEmpty()) return@forEach

                item(key = "header_${category.name}") {
                    SectionHeader(
                        title = stringResource(category.titleRes()),
                        modifier = Modifier.padding(top = VdvoemSpacing.sm)
                    )
                }
                items(categoryModules, key = { it.manifest.id }) { module ->
                    ModuleRow(
                        module = module,
                        enabled = module.manifest.id in enabledIds,
                        onToggle = { checked -> onToggle(module.manifest.id, checked) }
                    )
                }
            }
        }

        item(key = "header_soon") {
            SectionHeader(
                title = stringResource(R.string.store_soon_title),
                modifier = Modifier.padding(top = VdvoemSpacing.lg),
                trailing = { InfoPill(text = stringResource(R.string.store_soon_badge)) }
            )
        }
        items(plannedModules, key = { it.titleRes }) { planned -> PlannedModuleRow(planned) }

        item {
            Text(
                text = stringResource(R.string.store_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = VdvoemSpacing.md)
            )
        }
    }
}

@Composable
private fun ModuleRow(
    module: AppModule,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    VdvoemCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModuleIcon(module.icon)
            Spacer(modifier = Modifier.size(VdvoemSpacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xxs)
            ) {
                Text(
                    text = stringResource(module.manifest.titleRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(module.manifest.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun PlannedModuleRow(planned: PlannedModule) {
    VdvoemCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModuleIcon(planned.icon, muted = true)
            Spacer(modifier = Modifier.size(VdvoemSpacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xxs)
            ) {
                Text(
                    text = stringResource(planned.titleRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(planned.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            InfoPill(text = stringResource(R.string.store_soon_badge))
        }
    }
}

@Composable
private fun ModuleIcon(icon: ImageVector, muted: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = if (muted) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(VdvoemSizes.avatarMd)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(VdvoemSpacing.sm + VdvoemSpacing.xxs)
        )
    }
}

private fun ModuleCategory.titleRes(): Int = when (this) {
    ModuleCategory.CORE -> R.string.store_category_core
    ModuleCategory.TOGETHER -> R.string.store_category_together
    ModuleCategory.PERSONAL -> R.string.store_category_personal
    ModuleCategory.FUN -> R.string.store_category_fun
}
