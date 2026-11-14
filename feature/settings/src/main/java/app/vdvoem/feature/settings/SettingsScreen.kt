package app.vdvoem.feature.settings

import android.content.Context
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.domain.settings.AppSettings
import app.vdvoem.core.domain.settings.ThemeMode
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme

@Composable
fun SettingsScreen(
    state: AppSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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
            SettingsHero()
        }

        item(key = "appearance_header") {
            SectionHeader(
                title = stringResource(R.string.settings_section_appearance),
                modifier = Modifier.padding(top = VdvoemSpacing.sm)
            )
        }

        item(key = "appearance") {
            VdvoemCard {
                Text(
                    text = stringResource(R.string.settings_section_appearance_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
                ThemeMode.entries.forEachIndexed { index, mode ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = VdvoemSpacing.xs))
                    }
                    ThemeOptionRow(
                        mode = mode,
                        selected = state.themeMode == mode,
                        onClick = { onThemeModeChange(mode) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = VdvoemSpacing.xs))
                DynamicColorRow(
                    checked = state.dynamicColor && dynamicColorSupported,
                    enabled = dynamicColorSupported,
                    onCheckedChange = onDynamicColorChange
                )
            }
        }

        item(key = "pair_header") {
            SectionHeader(
                title = stringResource(R.string.settings_section_pair_data),
                modifier = Modifier.padding(top = VdvoemSpacing.sm)
            )
        }

        item(key = "pair_data") {
            VdvoemCard {
                Text(
                    text = stringResource(R.string.settings_section_pair_data_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "about_header") {
            SectionHeader(
                title = stringResource(R.string.settings_section_about),
                modifier = Modifier.padding(top = VdvoemSpacing.sm)
            )
        }

        item(key = "about") {
            AboutCard()
        }
    }
}

@Composable
private fun SettingsHero() {
    VdvoemCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(
            text = stringResource(R.string.settings_hero_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
        Text(
            text = stringResource(R.string.settings_hero_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeOptionRow(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = VdvoemSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(mode.labelRes()),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = VdvoemSpacing.sm)
        )
    }
}

@Composable
private fun DynamicColorRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = VdvoemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_dynamic_title),
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = stringResource(R.string.settings_dynamic_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun AboutCard() {
    val version = rememberAppVersion()
    VdvoemCard {
        Text(
            text = stringResource(R.string.settings_app_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
        Text(
            text = stringResource(R.string.settings_version, version),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.md))
        Text(
            text = stringResource(R.string.settings_copyright),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun rememberAppVersion(): String {
    val context = LocalContext.current
    return remember(context) { appVersion(context) }
}

@Suppress("DEPRECATION")
private fun appVersion(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull().orEmpty()

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8)
@Composable
private fun SettingsScreenPreview() {
    VdvoemTheme {
        SettingsScreen(
            state = AppSettings(),
            onThemeModeChange = {},
            onDynamicColorChange = {}
        )
    }
}