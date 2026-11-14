package app.vdvoem.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.vdvoem.R
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing

@Composable
fun DashboardScreen(
    modules: List<AppModule>,
    onOpenModule: (AppModule) -> Unit,
    onOpenStore: () -> Unit,
    onSetupPair: () -> Unit,
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
        item(key = "hero") {
            VdvoemCard(containerColor = MaterialTheme.colorScheme.primary) {
                Text(
                    text = stringResource(R.string.dashboard_hero_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                Text(
                    text = stringResource(R.string.dashboard_hero_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
                )
                Spacer(modifier = Modifier.size(VdvoemSpacing.xl))
                FilledTonalButton(
                    onClick = onSetupPair,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(R.string.dashboard_setup_pair))
                }
            }
        }

        item(key = "modules_header") {
            SectionHeader(
                title = stringResource(R.string.dashboard_modules_title),
                modifier = Modifier.padding(top = VdvoemSpacing.sm),
                trailing = {
                    TextButton(onClick = onOpenStore) {
                        Text(text = stringResource(R.string.dashboard_modules_all))
                    }
                }
            )
        }

        if (modules.isEmpty()) {
            item(key = "modules_empty") {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = stringResource(R.string.dashboard_modules_empty_title),
                    message = stringResource(R.string.dashboard_modules_empty_message),
                    actionLabel = stringResource(R.string.dashboard_open_store),
                    onAction = onOpenStore
                )
            }
        } else {
            items(modules, key = { it.manifest.id }) { module ->
                ModuleQuickRow(module = module, onClick = { onOpenModule(module) })
            }
        }

        item(key = "upcoming_header") {
            SectionHeader(
                title = stringResource(R.string.dashboard_upcoming_title),
                modifier = Modifier.padding(top = VdvoemSpacing.lg)
            )
        }

        item(key = "upcoming_empty") {
            EmptyState(
                icon = Icons.Filled.DateRange,
                title = stringResource(R.string.dashboard_upcoming_empty_title),
                message = stringResource(R.string.dashboard_upcoming_empty_message)
            )
        }
    }
}

@Composable
private fun ModuleQuickRow(
    module: AppModule,
    onClick: () -> Unit
) {
    VdvoemCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(VdvoemSizes.avatarMd)
            ) {
                Icon(
                    imageVector = module.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(VdvoemSpacing.sm + VdvoemSpacing.xxs)
                )
            }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
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
