package app.vdvoem.ui.more

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.vdvoem.R
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.data.HomePreferences
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing

/**
 * Вкладка «Ещё»: сюда попадают всё, что не влезло в нижнюю панель
 * (в навигации по гайдлайнам Material 3 — не больше пяти пунктов).
 */
@Composable
fun MoreScreen(
    overflowModules: List<AppModule>,
    onOpenModule: (AppModule) -> Unit,
    onOpenStore: () -> Unit,
    onOpenConnect: () -> Unit,
    pairLabel: String? = null,
    bottomModules: List<AppModule> = emptyList(),
    navModules: List<AppModule> = emptyList(),
    onNavRoutesChange: (List<String>) -> Unit = {},
    onExport: () -> Unit,
    onImport: () -> Unit,
    updateVersion: String? = null,
    updateChecking: Boolean = false,
    updateDownloading: Boolean = false,
    updateFailed: Boolean = false,
    appVersion: String = "",
    onUpdateClick: () -> Unit = {},
    pendingCount: Int = 0,
    onSyncNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var navSheetOpen by rememberSaveable { mutableStateOf(false) }
    var modulesSheetOpen by rememberSaveable { mutableStateOf(false) }
    if (navSheetOpen) {
        NavTabsSheet(
            selected = bottomModules.map { it.route },
            available = navModules,
            onChange = onNavRoutesChange,
            onDismiss = { navSheetOpen = false }
        )
    }
    if (modulesSheetOpen) {
        AllModulesSheet(
            modules = overflowModules,
            onOpen = { module ->
                modulesSheetOpen = false
                onOpenModule(module)
            },
            onDismiss = { modulesSheetOpen = false }
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
        if (overflowModules.isNotEmpty()) {
            item(key = "more_modules") {
                ModuleRow(
                    title = stringResource(R.string.more_all_modules),
                    description = pluralStringResource(
                        R.plurals.more_modules_count,
                        overflowModules.size,
                        overflowModules.size
                    ),
                    onClick = { modulesSheetOpen = true },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(VdvoemSizes.iconMd)
                        )
                    }
                )
            }
        }

        item(key = "more_store_header") {
            SectionHeader(
                title = stringResource(R.string.more_addons_title),
                modifier = Modifier.padding(top = VdvoemSpacing.sm)
            )
        }

        item(key = "more_store") {
            ModuleRow(
                title = stringResource(R.string.more_store_title),
                description = stringResource(R.string.more_store_text),
                onClick = onOpenStore,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(VdvoemSizes.iconMd)
                    )
                }
            )
        }

        item(key = "more_sync_header") {
            SectionHeader(
                title = stringResource(R.string.more_sync_section),
                modifier = Modifier.padding(top = VdvoemSpacing.sm)
            )
        }
        if (pendingCount > 0) {
            item(key = "more_sync_now") {
                ModuleRow(
                    title = stringResource(R.string.more_sync_now),
                    description = stringResource(R.string.more_sync_pending_hint),
                    onClick = onSyncNow,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(VdvoemSizes.iconMd)
                        )
                    }
                )
            }
        }
        item(key = "more_sync") {
            ModuleRow(
                title = pairLabel ?: stringResource(R.string.more_sync_title),
                description = if (pairLabel != null) {
                    stringResource(R.string.more_sync_connected_text)
                } else {
                    stringResource(R.string.more_sync_text)
                },
                // Настройки пары доступны всегда, отправка вынесена в отдельную строку.
                footer = if (pendingCount > 0) {
                    stringResource(R.string.more_sync_pending, pendingCount)
                } else {
                    null
                },
                onClick = onOpenConnect,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(VdvoemSizes.iconMd)
                    )
                }
            )
        }

        item(key = "more_data_header") {
            SectionHeader(
                title = stringResource(R.string.more_data_title),
                modifier = Modifier.padding(top = VdvoemSpacing.sm)
            )
        }

        item(key = "more_export") {
            ModuleRow(
                title = stringResource(R.string.backup_export),
                description = stringResource(R.string.more_export_text),
                onClick = onExport,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(VdvoemSizes.iconMd)
                    )
                }
            )
        }

        item(key = "more_import") {
            ModuleRow(
                title = stringResource(R.string.backup_import),
                description = stringResource(R.string.more_import_text),
                onClick = onImport,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(VdvoemSizes.iconMd)
                    )
                }
            )
        }

        item(key = "more_nav_header") {
            SectionHeader(
                title = stringResource(R.string.more_nav_section),
                modifier = Modifier.padding(top = VdvoemSpacing.sm)
            )
        }

        item(key = "more_nav") {
            val current = bottomModules.map { stringResource(it.manifest.titleRes) }.joinToString(", ")
            ModuleRow(
                title = stringResource(R.string.more_nav_title),
                description = stringResource(R.string.more_nav_text, current),
                onClick = { navSheetOpen = true },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(VdvoemSizes.iconMd)
                    )
                }
            )
        }

        item(key = "more_update_header") {
            SectionHeader(
                title = stringResource(R.string.more_update_section),
                modifier = Modifier.padding(top = VdvoemSpacing.sm)
            )
        }

        item(key = "more_update") {
            val description = when {
                updateDownloading -> stringResource(R.string.more_update_downloading)
                updateChecking -> stringResource(R.string.more_update_checking)
                updateFailed -> stringResource(R.string.more_update_error)
                updateVersion != null -> stringResource(R.string.more_update_available, updateVersion)
                else -> stringResource(R.string.more_update_current, appVersion)
            }
            ModuleRow(
                title = stringResource(R.string.more_update_title),
                description = description,
                onClick = onUpdateClick,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(VdvoemSizes.iconMd)
                    )
                }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AllModulesSheet(
    modules: List<AppModule>,
    onOpen: (AppModule) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = VdvoemSpacing.xl)
        ) {
            Text(
                text = stringResource(R.string.more_all_modules),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = VdvoemSpacing.screenHorizontal)
            )
            Text(
                text = stringResource(R.string.more_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = VdvoemSpacing.screenHorizontal)
                    .padding(top = VdvoemSpacing.xs, bottom = VdvoemSpacing.md)
            )
            modules.forEach { module ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(module) }
                        .padding(
                            horizontal = VdvoemSpacing.screenHorizontal,
                            vertical = VdvoemSpacing.md
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(VdvoemSizes.iconLg)
                    )
                    Spacer(modifier = Modifier.size(VdvoemSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NavTabsSheet(
    selected: List<String>,
    available: List<AppModule>,
    onChange: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = VdvoemSpacing.screenHorizontal,
                    end = VdvoemSpacing.screenHorizontal,
                    bottom = VdvoemSpacing.xl
                )
        ) {
            Text(
                text = stringResource(R.string.nav_sheet_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.nav_sheet_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = VdvoemSpacing.xs)
            )

            available.forEach { module ->
                val route = module.route
                val checked = route in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Больше двух нельзя: новый раздел вытесняет самый старый выбор.
                            val next = if (checked) {
                                selected - route
                            } else {
                                (selected + route).takeLast(HomePreferences.NAV_SLOTS)
                            }
                            onChange(next)
                        }
                        .padding(vertical = VdvoemSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(VdvoemSizes.iconMd)
                    )
                    Spacer(modifier = Modifier.size(VdvoemSpacing.md))
                    Text(
                        text = stringResource(module.manifest.titleRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            val next = if (checked) {
                                selected - route
                            } else {
                                (selected + route).takeLast(HomePreferences.NAV_SLOTS)
                            }
                            onChange(next)
                        }
                    )
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = VdvoemSpacing.sm)
            ) {
                Text(stringResource(R.string.nav_sheet_done))
            }
        }
    }
}

@Composable
private fun ModuleRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    footer: String? = null,
    hint: String? = null
) {
    VdvoemCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.size(VdvoemSpacing.md))
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (footer != null) {
                    Text(
                        text = footer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (hint != null) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}