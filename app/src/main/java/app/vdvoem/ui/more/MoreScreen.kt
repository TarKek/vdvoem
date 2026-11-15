package app.vdvoem.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.vdvoem.R
import app.vdvoem.core.domain.module.AppModule
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
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        item(key = "more_header") {
            Text(
                text = stringResource(R.string.more_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (overflowModules.isNotEmpty()) {
            items(overflowModules, key = { it.manifest.id }) { module ->
                ModuleRow(
                    title = stringResource(module.manifest.titleRes),
                    description = stringResource(module.manifest.descriptionRes),
                    onClick = { onOpenModule(module) },
                    icon = {
                        Icon(
                            imageVector = module.icon,
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

        item(key = "more_sync") {
            ModuleRow(
                title = stringResource(R.string.more_sync_title),
                description = stringResource(R.string.more_sync_text),
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
    }
}

@Composable
private fun ModuleRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
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
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}