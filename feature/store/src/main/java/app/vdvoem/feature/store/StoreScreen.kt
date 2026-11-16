package app.vdvoem.feature.store

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.core.domain.module.ModuleScope
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme

/** Возможность, которая появится в следующих версиях. */
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

/**
 * Экран «Возможности» (маршрут по-прежнему `store`).
 *
 * Магазина больше нет: всё собрано в APK и доступно всегда. Пользователь решает только,
 * показывать ли модуль в навигации — скрытие не удаляет данные. Список разделён по
 * [ModuleScope]: что общее с партнёром и что живёт только на этом телефоне.
 */
@Composable
fun StoreScreen(
    registry: ModuleRegistry,
    hiddenIds: Set<String>,
    onHiddenChange: (moduleId: String, hidden: Boolean) -> Unit,
    order: List<String> = emptyList(),
    onOrderChange: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val available = registry.toggleable.filter { it.manifest.id != StoreModule.ID }
    val baseOrder = order.ifEmpty { available.map { it.manifest.id } }
    val orderIndex = baseOrder.withIndex().associate { (index, id) -> id to index }
    val visibleCount = available.count { it.manifest.id !in hiddenIds }

    // Переставлять можно только показанные модули: скрытые в порядке не участвуют.
    fun reorderableIds(scope: ModuleScope): List<String> = baseOrder.filter { id ->
        id !in hiddenIds && available.any { it.manifest.id == id && it.manifest.scope == scope }
    }

    fun moveModule(scope: ModuleScope, moduleId: String, delta: Int) {
        val section = reorderableIds(scope)
        val index = section.indexOf(moduleId)
        val target = index + delta
        if (index < 0 || target !in section.indices) return
        val neighbour = section[target]
        val ids = baseOrder.toMutableList()
        val from = ids.indexOf(moduleId)
        val to = ids.indexOf(neighbour)
        if (from < 0 || to < 0) return
        ids[from] = neighbour
        ids[to] = moduleId
        onOrderChange(ids)
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
        item(key = "hero") {
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
                InfoPill(text = stringResource(R.string.store_visible_count, visibleCount, available.size))
                Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                Text(
                    text = stringResource(R.string.store_order_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.size(VdvoemSpacing.md))
                Button(
                    onClick = { hiddenIds.forEach { onHiddenChange(it, false) } },
                    enabled = hiddenIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.store_show_all))
                }
            }
        }

        ModuleScope.entries.forEach { scope ->
            val sectionModules = available
                .filter { it.manifest.scope == scope }
                // Внутри раздела — пользовательский порядок, иначе порядок манифеста.
                .sortedBy { orderIndex[it.manifest.id] ?: Int.MAX_VALUE }
            if (sectionModules.isEmpty()) return@forEach

            item(key = "scope_${scope.name}") {
                Column(
                    modifier = Modifier.padding(top = VdvoemSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)
                ) {
                    SectionHeader(title = stringResource(scope.titleRes()))
                    Text(
                        text = stringResource(scope.hintRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val movable = reorderableIds(scope)
            items(sectionModules, key = { it.manifest.id }) { module ->
                val visible = module.manifest.id !in hiddenIds
                val position = movable.indexOf(module.manifest.id)
                ModuleRow(
                    module = module,
                    visible = visible,
                    canMoveUp = visible && position > 0,
                    canMoveDown = visible && position >= 0 && position < movable.lastIndex,
                    onMoveUp = { moveModule(scope, module.manifest.id, -1) },
                    onMoveDown = { moveModule(scope, module.manifest.id, 1) },
                    onVisibleChange = { checked -> onHiddenChange(module.manifest.id, !checked) }
                )
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

        item(key = "footer") {
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
    visible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onVisibleChange: (Boolean) -> Unit
) {
    VdvoemCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModuleIcon(module.icon, muted = !visible)
            Spacer(modifier = Modifier.size(VdvoemSpacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xxs)
            ) {
                Text(
                    text = stringResource(module.manifest.titleRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (visible) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = stringResource(module.manifest.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Переставлять можно только показанные модули: скрытые в порядке не участвуют.
            if (visible) {
                MoveButtons(
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown
                )
            }
        }
        Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.store_visibility_toggle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = visible, onCheckedChange = onVisibleChange)
        }
    }
}

/** Сдвоенные стрелки «выше/ниже»: одна позиция за нажатие. */
@Composable
private fun MoveButtons(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Column(verticalArrangement = Arrangement.Center) {
        IconButton(
            onClick = onMoveUp,
            enabled = canMoveUp,
            modifier = Modifier.size(VdvoemSizes.avatarSm)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.store_move_up),
                tint = if (canMoveUp) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
                }
            )
        }
        IconButton(
            onClick = onMoveDown,
            enabled = canMoveDown,
            modifier = Modifier.size(VdvoemSizes.avatarSm)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.store_move_down),
                tint = if (canMoveDown) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
                }
            )
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

private fun ModuleScope.titleRes(): Int = when (this) {
    ModuleScope.SHARED -> R.string.store_scope_shared
    ModuleScope.LOCAL -> R.string.store_scope_local
}

private fun ModuleScope.hintRes(): Int = when (this) {
    ModuleScope.SHARED -> R.string.store_scope_shared_hint
    ModuleScope.LOCAL -> R.string.store_scope_local_hint
}

/** Прозрачность стрелки, когда двигать некуда. */
private const val DISABLED_ALPHA = 0.38f

/** Модуль-заглушка для превью: реальные экраны здесь не нужны. */
private class PreviewModule(
    override val manifest: ModuleManifest,
    override val icon: ImageVector
) : AppModule {
    override val route: String = manifest.id

    @Composable
    override fun Screen(modifier: Modifier) = Unit
}

private fun previewModule(
    id: String,
    titleRes: Int,
    descriptionRes: Int,
    scope: ModuleScope,
    order: Int,
    icon: ImageVector
): AppModule = PreviewModule(
    manifest = ModuleManifest(
        id = id,
        titleRes = titleRes,
        descriptionRes = descriptionRes,
        category = ModuleCategory.TOGETHER,
        order = order,
        scope = scope
    ),
    icon = icon
)

private val previewRegistry = ModuleRegistry(
    listOf(
        previewModule(
            id = "preview.presents",
            titleRes = R.string.store_tasks_title,
            descriptionRes = R.string.store_tasks_description,
            scope = ModuleScope.SHARED,
            order = 10,
            icon = Icons.AutoMirrored.Filled.List
        ),
        previewModule(
            id = "preview.notes",
            titleRes = R.string.store_notes_title,
            descriptionRes = R.string.store_notes_description,
            scope = ModuleScope.SHARED,
            order = 20,
            icon = Icons.Filled.Edit
        ),
        previewModule(
            id = "preview.countdown",
            titleRes = R.string.store_countdown_title,
            descriptionRes = R.string.store_countdown_description,
            scope = ModuleScope.SHARED,
            order = 30,
            icon = Icons.Filled.Star
        ),
        previewModule(
            id = "preview.mood",
            titleRes = R.string.store_mood_title,
            descriptionRes = R.string.store_mood_description,
            scope = ModuleScope.LOCAL,
            order = 40,
            icon = Icons.Filled.Face
        ),
        previewModule(
            id = "preview.steam",
            titleRes = R.string.store_steam_title,
            descriptionRes = R.string.store_steam_description,
            scope = ModuleScope.LOCAL,
            order = 50,
            icon = Icons.Filled.Lock
        )
    )
)

@Preview(name = "Возможности", showBackground = true, heightDp = 1100)
@Composable
private fun StoreScreenPreview() {
    VdvoemTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            StoreScreen(
                registry = previewRegistry,
                hiddenIds = setOf("preview.steam"),
                onHiddenChange = { _, _ -> }
            )
        }
    }
}

@Preview(name = "Стрелки порядка")
@Composable
private fun MoveButtonsPreview() {
    VdvoemTheme {
        MoveButtons(canMoveUp = true, canMoveDown = false, onMoveUp = {}, onMoveDown = {})
    }
}