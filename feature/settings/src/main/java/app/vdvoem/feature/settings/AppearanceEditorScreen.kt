package app.vdvoem.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import app.vdvoem.core.domain.settings.AppearanceSettings
import app.vdvoem.core.domain.settings.ColorRole
import app.vdvoem.core.domain.settings.CornerRole
import app.vdvoem.core.domain.settings.ElevationRole
import app.vdvoem.core.domain.settings.SizeRole
import app.vdvoem.core.domain.settings.SpacingRole
import app.vdvoem.core.domain.settings.TypeRole
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.ElevationTokens
import app.vdvoem.core.ui.theme.SizeTokens
import app.vdvoem.core.ui.theme.SpacingTokens
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme

/**
 * Редактор тонкой настройки оформления — «каждый пиксель».
 * Черновик сразу виден в живом предпросмотре; отпускание слайдера и
 * подтверждение диалога сохраняют настройку и применяют её ко всему приложению.
 */
@Composable
fun AppearanceEditorScreen(
    appearance: AppearanceSettings,
    onChange: (AppearanceSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var draft by remember { mutableStateOf(appearance) }
    var editDark by rememberSaveable { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf<ColorDialogState?>(null) }
    var confirmResetAll by rememberSaveable { mutableStateOf(false) }

    fun commit(next: AppearanceSettings) {
        draft = next
        onChange(next)
    }

    val baseSpacing = remember { SpacingTokens() }
    val baseSizes = remember { SizeTokens() }
    val baseType = remember { Typography() }
    val baseElevation = remember { ElevationTokens() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VdvoemSpacing.screenHorizontal, vertical = VdvoemSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.appearance_back)
                )
            }
            Text(
                text = stringResource(R.string.appearance_editor_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = VdvoemSpacing.xs)
            )
            TextButton(onClick = { confirmResetAll = true }) {
                Text(stringResource(R.string.appearance_reset_all))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VdvoemSpacing.screenHorizontal,
                end = VdvoemSpacing.screenHorizontal,
                top = VdvoemSpacing.sm,
                bottom = VdvoemSpacing.listBottom
            ),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
        ) {
            item(key = "preview") {
                AppearancePreview(appearance = draft, dark = editDark)
            }

            // --- Цвета: все роли Material 3, отдельно для светлой и тёмной темы ---
            item(key = "colors_header") {
                SectionWithReset(
                    title = stringResource(R.string.appearance_section_colors),
                    onReset = { commit(draft.copy(lightColors = emptyMap(), darkColors = emptyMap())) }
                )
            }
            item(key = "colors") {
                VdvoemCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)) {
                        FilterChip(
                            selected = !editDark,
                            onClick = { editDark = false },
                            label = { Text(stringResource(R.string.appearance_light)) }
                        )
                        FilterChip(
                            selected = editDark,
                            onClick = { editDark = true },
                            label = { Text(stringResource(R.string.appearance_dark)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
                    ColorRole.entries.forEachIndexed { index, role ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = VdvoemSpacing.xxs))
                        }
                        ColorRoleRow(
                            role = role,
                            draft = draft,
                            dark = editDark,
                            onClick = { colorDialog = ColorDialogState(role, editDark) }
                        )
                    }
                }
            }

            // --- Скругления ---
            item(key = "corners_header") {
                SectionWithReset(
                    title = stringResource(R.string.appearance_section_corners),
                    onReset = { commit(draft.copy(corners = emptyMap())) }
                )
            }
            item(key = "corners") {
                VdvoemCard {
                    CornerRole.entries.forEach { role ->
                        val base = cornerDefault(role)
                        val override = draft.corners[role]
                        val value = override ?: base
                        EditorSliderRow(
                            label = stringResource(role.labelRes()),
                            value = value,
                            valueRange = 0f..48f,
                            valueText = stringResource(R.string.appearance_value_dp, value.toInt()),
                            overridden = override != null,
                            onValueChange = { draft = draft.copy(corners = draft.corners + (role to it)) },
                            onFinished = { onChange(draft) },
                            onReset = { commit(draft.copy(corners = draft.corners - role)) }
                        )
                    }
                }
            }

            // --- Отступы: общий масштаб + каждый токен ---
            item(key = "spacing_header") {
                SectionWithReset(
                    title = stringResource(R.string.appearance_section_spacing),
                    onReset = { commit(draft.copy(spacingScale = 1f, spacing = emptyMap())) }
                )
            }
            item(key = "spacing") {
                VdvoemCard {
                    ScaleSliderRow(
                        label = stringResource(R.string.appearance_scale_spacing),
                        scale = draft.spacingScale,
                        rangePercent = 50f..200f,
                        onScaleChange = { draft = draft.copy(spacingScale = it) },
                        onFinished = { onChange(draft) },
                        onReset = { commit(draft.copy(spacingScale = 1f)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = VdvoemSpacing.xs))
                    SpacingRole.entries.forEach { role ->
                        val base = baseSpacing.baseOf(role)
                        val override = draft.spacing[role]
                        val value = override ?: base.value
                        val effective = value * draft.spacingScale
                        EditorSliderRow(
                            label = stringResource(role.labelRes()),
                            value = value,
                            valueRange = if (role == SpacingRole.LIST_BOTTOM) 0f..160f else 0f..64f,
                            valueText = stringResource(R.string.appearance_value_dp, effective.toInt()),
                            overridden = override != null,
                            onValueChange = { draft = draft.copy(spacing = draft.spacing + (role to it)) },
                            onFinished = { onChange(draft) },
                            onReset = { commit(draft.copy(spacing = draft.spacing - role)) }
                        )
                    }
                }
            }

            // --- Размеры: каждый токен ---
            item(key = "sizes_header") {
                SectionWithReset(
                    title = stringResource(R.string.appearance_section_sizes),
                    onReset = { commit(draft.copy(sizes = emptyMap())) }
                )
            }
            item(key = "sizes") {
                VdvoemCard {
                    SizeRole.entries.forEach { role ->
                        val base = baseSizes.baseOf(role)
                        val override = draft.sizes[role]
                        val value = override ?: base.value
                        EditorSliderRow(
                            label = stringResource(role.labelRes()),
                            value = value,
                            valueRange = if (role == SizeRole.SHEET_MAX) 0f..800f else 0f..128f,
                            valueText = stringResource(R.string.appearance_value_dp, value.toInt()),
                            overridden = override != null,
                            onValueChange = { draft = draft.copy(sizes = draft.sizes + (role to it)) },
                            onFinished = { onChange(draft) },
                            onReset = { commit(draft.copy(sizes = draft.sizes - role)) }
                        )
                    }
                }
            }

            // --- Шрифты: общий масштаб + размер каждого стиля ---
            item(key = "type_header") {
                SectionWithReset(
                    title = stringResource(R.string.appearance_section_type),
                    onReset = { commit(draft.copy(fontScale = 1f, typeSizes = emptyMap())) }
                )
            }
            item(key = "type") {
                VdvoemCard {
                    ScaleSliderRow(
                        label = stringResource(R.string.appearance_scale_type),
                        scale = draft.fontScale,
                        rangePercent = 75f..150f,
                        onScaleChange = { draft = draft.copy(fontScale = it) },
                        onFinished = { onChange(draft) },
                        onReset = { commit(draft.copy(fontScale = 1f)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = VdvoemSpacing.xs))
                    TypeRole.entries.forEach { role ->
                        val base = baseType.baseOf(role).fontSize.value
                        val override = draft.typeSizes[role]
                        val value = override ?: base
                        EditorSliderRow(
                            label = stringResource(role.labelRes()),
                            value = value,
                            valueRange = 8f..64f,
                            valueText = stringResource(R.string.appearance_value_sp, value.toInt()),
                            overridden = override != null,
                            onValueChange = { draft = draft.copy(typeSizes = draft.typeSizes + (role to it)) },
                            onFinished = { onChange(draft) },
                            onReset = { commit(draft.copy(typeSizes = draft.typeSizes - role)) }
                        )
                    }
                }
            }

            // --- Насыщенность шрифтов по стилям ---
            item(key = "type_weight_header") {
                SectionWithReset(
                    title = stringResource(R.string.appearance_section_type_weight),
                    onReset = { commit(draft.copy(typeWeights = emptyMap())) }
                )
            }
            item(key = "type_weight") {
                VdvoemCard {
                    TypeRole.entries.forEach { role ->
                        val base = baseType.baseOf(role).fontWeight?.weight ?: 400
                        val override = draft.typeWeights[role]
                        val value = (override ?: base).toFloat()
                        EditorSliderRow(
                            label = stringResource(role.labelRes()),
                            value = value,
                            valueRange = 100f..900f,
                            step = 100f,
                            valueText = value.toInt().toString(),
                            overridden = override != null,
                            onValueChange = { draft = draft.copy(typeWeights = draft.typeWeights + (role to it.toInt())) },
                            onFinished = { onChange(draft) },
                            onReset = { commit(draft.copy(typeWeights = draft.typeWeights - role)) }
                        )
                    }
                }
            }

            // --- Межбуквенный интервал по стилям ---
            item(key = "type_spacing_header") {
                SectionWithReset(
                    title = stringResource(R.string.appearance_section_type_spacing),
                    onReset = { commit(draft.copy(typeLetterSpacing = emptyMap())) }
                )
            }
            item(key = "type_spacing") {
                VdvoemCard {
                    TypeRole.entries.forEach { role ->
                        val baseStyle = baseType.baseOf(role)
                        val base = if (baseStyle.letterSpacing.isSp) baseStyle.letterSpacing.value else 0f
                        val override = draft.typeLetterSpacing[role]
                        val value = override ?: base
                        EditorSliderRow(
                            label = stringResource(role.labelRes()),
                            value = value,
                            valueRange = -2f..4f,
                            step = 0.1f,
                            valueText = stringResource(R.string.appearance_value_sp_decimal, value),
                            overridden = override != null,
                            onValueChange = { draft = draft.copy(typeLetterSpacing = draft.typeLetterSpacing + (role to it)) },
                            onFinished = { onChange(draft) },
                            onReset = { commit(draft.copy(typeLetterSpacing = draft.typeLetterSpacing - role)) }
                        )
                    }
                }
            }

            // --- Высоты (тени): общий множитель + каждый токен ---
            item(key = "elevation_header") {
                SectionWithReset(
                    title = stringResource(R.string.appearance_section_elevation),
                    onReset = { commit(draft.copy(elevationScale = 1f, elevation = emptyMap())) }
                )
            }
            item(key = "elevation") {
                VdvoemCard {
                    ScaleSliderRow(
                        label = stringResource(R.string.appearance_scale_elevation),
                        scale = draft.elevationScale,
                        rangePercent = 0f..200f,
                        onScaleChange = { draft = draft.copy(elevationScale = it) },
                        onFinished = { onChange(draft) },
                        onReset = { commit(draft.copy(elevationScale = 1f)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = VdvoemSpacing.xs))
                    ElevationRole.entries.forEach { role ->
                        val base = baseElevation.baseOf(role)
                        val override = draft.elevation[role]
                        val value = override ?: base.value
                        val effective = value * draft.elevationScale
                        EditorSliderRow(
                            label = stringResource(role.labelRes()),
                            value = value,
                            valueRange = 0f..24f,
                            valueText = stringResource(R.string.appearance_value_dp, effective.toInt()),
                            overridden = override != null,
                            onValueChange = { draft = draft.copy(elevation = draft.elevation + (role to it)) },
                            onFinished = { onChange(draft) },
                            onReset = { commit(draft.copy(elevation = draft.elevation - role)) }
                        )
                    }
                }
            }
        }
    }

    colorDialog?.let { state ->
        val overrides = if (state.dark) draft.darkColors else draft.lightColors
        val initial = overrides[state.role]?.let { Color(it) }
            ?: MaterialTheme.colorScheme.colorOf(state.role)
        ColorPickerDialog(
            role = state.role,
            initial = initial,
            hasOverride = overrides.containsKey(state.role),
            onDismiss = { colorDialog = null },
            onReset = {
                val next = if (state.dark) {
                    draft.copy(darkColors = draft.darkColors - state.role)
                } else {
                    draft.copy(lightColors = draft.lightColors - state.role)
                }
                commit(next)
                colorDialog = null
            },
            onConfirm = { color ->
                val argb = color.toArgb().toLong() and 0xFFFFFFFFL
                val next = if (state.dark) {
                    draft.copy(darkColors = draft.darkColors + (state.role to argb))
                } else {
                    draft.copy(lightColors = draft.lightColors + (state.role to argb))
                }
                commit(next)
                colorDialog = null
            }
        )
    }

    if (confirmResetAll) {
        AlertDialog(
            onDismissRequest = { confirmResetAll = false },
            title = { Text(stringResource(R.string.appearance_confirm_reset_title)) },
            text = { Text(stringResource(R.string.appearance_confirm_reset_text)) },
            confirmButton = {
                TextButton(onClick = {
                    commit(AppearanceSettings())
                    confirmResetAll = false
                }) {
                    Text(stringResource(R.string.appearance_reset_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetAll = false }) {
                    Text(stringResource(R.string.appearance_cancel))
                }
            }
        )
    }
}

/** Живой предпросмотр: карточка, кнопка, пилюля и типографика в черновой теме. */
@Composable
private fun AppearancePreview(appearance: AppearanceSettings, dark: Boolean) {
    VdvoemTheme(darkTheme = dark, appearance = appearance) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = MaterialTheme.shapes.large
        ) {
            VdvoemCard(modifier = Modifier.padding(VdvoemSpacing.sm)) {
                Text(
                    text = stringResource(R.string.appearance_preview_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
                Text(
                    text = stringResource(R.string.appearance_preview_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
                ) {
                    Button(onClick = {}) {
                        Text(stringResource(R.string.appearance_preview_button))
                    }
                    InfoPill(text = stringResource(R.string.appearance_preview_pill))
                }
                Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
                Text(
                    text = stringResource(R.string.appearance_preview_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionWithReset(title: String, onReset: () -> Unit) {
    SectionHeader(
        title = title,
        trailing = {
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.appearance_reset_section))
            }
        }
    )
}

/** Строка цветовой роли: свотч эффективного цвета, название, hex и маркер перекрытия. */
@Composable
private fun ColorRoleRow(
    role: ColorRole,
    draft: AppearanceSettings,
    dark: Boolean,
    onClick: () -> Unit
) {
    val overrides = if (dark) draft.darkColors else draft.lightColors
    val effective = overrides[role]?.let { Color(it) } ?: MaterialTheme.colorScheme.colorOf(role)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = VdvoemSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(VdvoemSizes.avatarSm)
                .clip(CircleShape)
                .background(effective)
                .border(VdvoemSizes.borderThin, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = VdvoemSpacing.sm)
        ) {
            Text(
                text = stringResource(role.labelRes()),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = hexOf(effective),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (overrides.containsKey(role)) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(VdvoemSizes.iconSm)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class ColorDialogState(val role: ColorRole, val dark: Boolean)

/** Диалог выбора цвета: HSV-слайдеры, ввод HEX, свотчи «было/стало», сброс роли. */
@Composable
private fun ColorPickerDialog(
    role: ColorRole,
    initial: Color,
    hasOverride: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var hue by remember { mutableStateOf(hsv[0]) }
    var saturation by remember { mutableStateOf(hsv[1] * 100f) }
    var brightness by remember { mutableStateOf(hsv[2] * 100f) }
    var hex by remember { mutableStateOf(hexOf(initial)) }

    val current = remember(hue, saturation, brightness) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation / 100f, brightness / 100f)))
    }

    fun applyHsv(h: Float, s: Float, v: Float) {
        hue = h
        saturation = s
        brightness = v
        hex = hexOf(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s / 100f, v / 100f))))
    }

    fun applyHex(text: String) {
        hex = text
        parseHex(text)?.let { color ->
            android.graphics.Color.colorToHSV(color.toArgb(), hsv)
            hue = hsv[0]
            saturation = hsv[1] * 100f
            brightness = hsv[2] * 100f
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(role.labelRes())) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
                ) {
                    ColorSwatchWithCaption(color = initial, caption = stringResource(R.string.appearance_before))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ColorSwatchWithCaption(color = current, caption = stringResource(R.string.appearance_after))
                }
                OutlinedTextField(
                    value = hex,
                    onValueChange = ::applyHex,
                    label = { Text(stringResource(R.string.appearance_hex_label)) },
                    singleLine = true,
                    isError = parseHex(hex) == null,
                    modifier = Modifier.fillMaxWidth()
                )
                HsvSliderRow(
                    label = stringResource(R.string.appearance_hue),
                    value = hue,
                    valueRange = 0f..360f,
                    onValueChange = { applyHsv(it, saturation, brightness) }
                )
                HsvSliderRow(
                    label = stringResource(R.string.appearance_saturation),
                    value = saturation,
                    valueRange = 0f..100f,
                    onValueChange = { applyHsv(hue, it, brightness) }
                )
                HsvSliderRow(
                    label = stringResource(R.string.appearance_brightness),
                    value = brightness,
                    valueRange = 0f..100f,
                    onValueChange = { applyHsv(hue, saturation, it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) {
                Text(stringResource(R.string.appearance_ok))
            }
        },
        dismissButton = {
            Row {
                if (hasOverride) {
                    TextButton(onClick = onReset) {
                        Text(stringResource(R.string.appearance_reset_role))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.appearance_cancel))
                }
            }
        }
    )
}

@Composable
private fun ColorSwatchWithCaption(color: Color, caption: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(VdvoemSizes.avatarMd)
                .clip(CircleShape)
                .background(color)
                .border(VdvoemSizes.borderThin, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HsvSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = ((valueRange.endInclusive - valueRange.start).toInt() - 1).coerceAtLeast(0)
        )
    }
}

/** Слайдер общего масштаба секции: хранится долей (1f = 100%), показывается в процентах. */
@Composable
private fun ScaleSliderRow(
    label: String,
    scale: Float,
    rangePercent: ClosedFloatingPointRange<Float>,
    onScaleChange: (Float) -> Unit,
    onFinished: () -> Unit,
    onReset: () -> Unit
) {
    EditorSliderRow(
        label = label,
        value = scale * 100f,
        valueRange = rangePercent,
        valueText = stringResource(R.string.appearance_value_percent, (scale * 100f).toInt()),
        overridden = scale != 1f,
        onValueChange = { onScaleChange(it / 100f) },
        onFinished = onFinished,
        onReset = onReset
    )
}

/** Строка токена: название, текущее значение, слайдер и сброс перекрытия. */
@Composable
private fun EditorSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    overridden: Boolean,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
    onReset: (() -> Unit)? = null,
    step: Float = 1f
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VdvoemSpacing.xxs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (overridden && onReset != null) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(VdvoemSizes.roundButton)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.appearance_reset_role),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(VdvoemSizes.iconSm)
                    )
                }
            }
        }
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = (((valueRange.endInclusive - valueRange.start) / step).roundToInt() - 1).coerceAtLeast(0),
            onValueChangeFinished = onFinished,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- Базовые значения токенов (дефолты бренда и Material 3) ---

private fun cornerDefault(role: CornerRole): Float = when (role) {
    CornerRole.EXTRA_SMALL -> 8f
    CornerRole.SMALL -> 12f
    CornerRole.MEDIUM -> 18f
    CornerRole.LARGE -> 24f
    CornerRole.EXTRA_LARGE -> 32f
}

private fun SpacingTokens.baseOf(role: SpacingRole): Dp = when (role) {
    SpacingRole.XXS -> xxs
    SpacingRole.XS -> xs
    SpacingRole.SM -> sm
    SpacingRole.MD -> md
    SpacingRole.LG -> lg
    SpacingRole.XL -> xl
    SpacingRole.XXL -> xxl
    SpacingRole.XXXL -> xxxl
    SpacingRole.HUGE -> huge
    SpacingRole.SCREEN_HORIZONTAL -> screenHorizontal
    SpacingRole.LIST_BOTTOM -> listBottom
}

private fun SizeTokens.baseOf(role: SizeRole): Dp = when (role) {
    SizeRole.ICON_SM -> iconSm
    SizeRole.ICON_MD -> iconMd
    SizeRole.ICON_LG -> iconLg
    SizeRole.AVATAR_SM -> avatarSm
    SizeRole.AVATAR_MD -> avatarMd
    SizeRole.AVATAR_LG -> avatarLg
    SizeRole.AVATAR_XL -> avatarXl
    SizeRole.TOUCH_TARGET -> touchTarget
    SizeRole.ROUND_BUTTON -> roundButton
    SizeRole.DOT -> dot
    SizeRole.TIME_COLUMN -> timeColumn
    SizeRole.SHEET_MAX -> sheetMax
    SizeRole.AVATAR_ACCOUNT -> avatarAccount
    SizeRole.BORDER_THIN -> borderThin
    SizeRole.WIDGET_WIDTH -> widgetWidth
    SizeRole.WIDGET_HEIGHT -> widgetHeight
    SizeRole.HERO_BADGE -> heroBadge
}

private fun Typography.baseOf(role: TypeRole): TextStyle = when (role) {
    TypeRole.DISPLAY_LARGE -> displayLarge
    TypeRole.DISPLAY_MEDIUM -> displayMedium
    TypeRole.DISPLAY_SMALL -> displaySmall
    TypeRole.HEADLINE_LARGE -> headlineLarge
    TypeRole.HEADLINE_MEDIUM -> headlineMedium
    TypeRole.HEADLINE_SMALL -> headlineSmall
    TypeRole.TITLE_LARGE -> titleLarge
    TypeRole.TITLE_MEDIUM -> titleMedium
    TypeRole.TITLE_SMALL -> titleSmall
    TypeRole.BODY_LARGE -> bodyLarge
    TypeRole.BODY_MEDIUM -> bodyMedium
    TypeRole.BODY_SMALL -> bodySmall
    TypeRole.LABEL_LARGE -> labelLarge
    TypeRole.LABEL_MEDIUM -> labelMedium
    TypeRole.LABEL_SMALL -> labelSmall
}

private fun ElevationTokens.baseOf(role: ElevationRole): Dp = when (role) {
    ElevationRole.CARD -> card
    ElevationRole.FAB -> fab
    ElevationRole.DIALOG -> dialog
    ElevationRole.SHEET -> sheet
}

private fun ColorScheme.colorOf(role: ColorRole): Color = when (role) {
    ColorRole.PRIMARY -> primary
    ColorRole.ON_PRIMARY -> onPrimary
    ColorRole.PRIMARY_CONTAINER -> primaryContainer
    ColorRole.ON_PRIMARY_CONTAINER -> onPrimaryContainer
    ColorRole.INVERSE_PRIMARY -> inversePrimary
    ColorRole.SECONDARY -> secondary
    ColorRole.ON_SECONDARY -> onSecondary
    ColorRole.SECONDARY_CONTAINER -> secondaryContainer
    ColorRole.ON_SECONDARY_CONTAINER -> onSecondaryContainer
    ColorRole.TERTIARY -> tertiary
    ColorRole.ON_TERTIARY -> onTertiary
    ColorRole.TERTIARY_CONTAINER -> tertiaryContainer
    ColorRole.ON_TERTIARY_CONTAINER -> onTertiaryContainer
    ColorRole.BACKGROUND -> background
    ColorRole.ON_BACKGROUND -> onBackground
    ColorRole.SURFACE -> surface
    ColorRole.ON_SURFACE -> onSurface
    ColorRole.SURFACE_VARIANT -> surfaceVariant
    ColorRole.ON_SURFACE_VARIANT -> onSurfaceVariant
    ColorRole.SURFACE_TINT -> surfaceTint
    ColorRole.INVERSE_SURFACE -> inverseSurface
    ColorRole.INVERSE_ON_SURFACE -> inverseOnSurface
    ColorRole.ERROR -> error
    ColorRole.ON_ERROR -> onError
    ColorRole.ERROR_CONTAINER -> errorContainer
    ColorRole.ON_ERROR_CONTAINER -> onErrorContainer
    ColorRole.OUTLINE -> outline
    ColorRole.OUTLINE_VARIANT -> outlineVariant
    ColorRole.SCRIM -> scrim
    ColorRole.SURFACE_BRIGHT -> surfaceBright
    ColorRole.SURFACE_DIM -> surfaceDim
    ColorRole.SURFACE_CONTAINER -> surfaceContainer
    ColorRole.SURFACE_CONTAINER_HIGH -> surfaceContainerHigh
    ColorRole.SURFACE_CONTAINER_HIGHEST -> surfaceContainerHighest
    ColorRole.SURFACE_CONTAINER_LOW -> surfaceContainerLow
    ColorRole.SURFACE_CONTAINER_LOWEST -> surfaceContainerLowest
}

private fun hexOf(color: Color): String = String.format("#%06X", 0xFFFFFF and color.toArgb())

private fun parseHex(text: String): Color? {
    val cleaned = text.trim().removePrefix("#")
    if (!Regex("^[0-9a-fA-F]{6}$").matches(cleaned)) return null
    return runCatching { Color(android.graphics.Color.parseColor("#$cleaned")) }.getOrNull()
}

// --- Названия ролей (строковые ресурсы ru/en) ---

private fun ColorRole.labelRes(): Int = when (this) {
    ColorRole.PRIMARY -> R.string.appearance_color_primary
    ColorRole.ON_PRIMARY -> R.string.appearance_color_on_primary
    ColorRole.PRIMARY_CONTAINER -> R.string.appearance_color_primary_container
    ColorRole.ON_PRIMARY_CONTAINER -> R.string.appearance_color_on_primary_container
    ColorRole.INVERSE_PRIMARY -> R.string.appearance_color_inverse_primary
    ColorRole.SECONDARY -> R.string.appearance_color_secondary
    ColorRole.ON_SECONDARY -> R.string.appearance_color_on_secondary
    ColorRole.SECONDARY_CONTAINER -> R.string.appearance_color_secondary_container
    ColorRole.ON_SECONDARY_CONTAINER -> R.string.appearance_color_on_secondary_container
    ColorRole.TERTIARY -> R.string.appearance_color_tertiary
    ColorRole.ON_TERTIARY -> R.string.appearance_color_on_tertiary
    ColorRole.TERTIARY_CONTAINER -> R.string.appearance_color_tertiary_container
    ColorRole.ON_TERTIARY_CONTAINER -> R.string.appearance_color_on_tertiary_container
    ColorRole.BACKGROUND -> R.string.appearance_color_background
    ColorRole.ON_BACKGROUND -> R.string.appearance_color_on_background
    ColorRole.SURFACE -> R.string.appearance_color_surface
    ColorRole.ON_SURFACE -> R.string.appearance_color_on_surface
    ColorRole.SURFACE_VARIANT -> R.string.appearance_color_surface_variant
    ColorRole.ON_SURFACE_VARIANT -> R.string.appearance_color_on_surface_variant
    ColorRole.SURFACE_TINT -> R.string.appearance_color_surface_tint
    ColorRole.INVERSE_SURFACE -> R.string.appearance_color_inverse_surface
    ColorRole.INVERSE_ON_SURFACE -> R.string.appearance_color_inverse_on_surface
    ColorRole.ERROR -> R.string.appearance_color_error
    ColorRole.ON_ERROR -> R.string.appearance_color_on_error
    ColorRole.ERROR_CONTAINER -> R.string.appearance_color_error_container
    ColorRole.ON_ERROR_CONTAINER -> R.string.appearance_color_on_error_container
    ColorRole.OUTLINE -> R.string.appearance_color_outline
    ColorRole.OUTLINE_VARIANT -> R.string.appearance_color_outline_variant
    ColorRole.SCRIM -> R.string.appearance_color_scrim
    ColorRole.SURFACE_BRIGHT -> R.string.appearance_color_surface_bright
    ColorRole.SURFACE_DIM -> R.string.appearance_color_surface_dim
    ColorRole.SURFACE_CONTAINER -> R.string.appearance_color_surface_container
    ColorRole.SURFACE_CONTAINER_HIGH -> R.string.appearance_color_surface_container_high
    ColorRole.SURFACE_CONTAINER_HIGHEST -> R.string.appearance_color_surface_container_highest
    ColorRole.SURFACE_CONTAINER_LOW -> R.string.appearance_color_surface_container_low
    ColorRole.SURFACE_CONTAINER_LOWEST -> R.string.appearance_color_surface_container_lowest
}

private fun CornerRole.labelRes(): Int = when (this) {
    CornerRole.EXTRA_SMALL -> R.string.appearance_corner_extra_small
    CornerRole.SMALL -> R.string.appearance_corner_small
    CornerRole.MEDIUM -> R.string.appearance_corner_medium
    CornerRole.LARGE -> R.string.appearance_corner_large
    CornerRole.EXTRA_LARGE -> R.string.appearance_corner_extra_large
}

private fun SpacingRole.labelRes(): Int = when (this) {
    SpacingRole.XXS -> R.string.appearance_spacing_xxs
    SpacingRole.XS -> R.string.appearance_spacing_xs
    SpacingRole.SM -> R.string.appearance_spacing_sm
    SpacingRole.MD -> R.string.appearance_spacing_md
    SpacingRole.LG -> R.string.appearance_spacing_lg
    SpacingRole.XL -> R.string.appearance_spacing_xl
    SpacingRole.XXL -> R.string.appearance_spacing_xxl
    SpacingRole.XXXL -> R.string.appearance_spacing_xxxl
    SpacingRole.HUGE -> R.string.appearance_spacing_huge
    SpacingRole.SCREEN_HORIZONTAL -> R.string.appearance_spacing_screen_horizontal
    SpacingRole.LIST_BOTTOM -> R.string.appearance_spacing_list_bottom
}

private fun SizeRole.labelRes(): Int = when (this) {
    SizeRole.ICON_SM -> R.string.appearance_size_icon_sm
    SizeRole.ICON_MD -> R.string.appearance_size_icon_md
    SizeRole.ICON_LG -> R.string.appearance_size_icon_lg
    SizeRole.AVATAR_SM -> R.string.appearance_size_avatar_sm
    SizeRole.AVATAR_MD -> R.string.appearance_size_avatar_md
    SizeRole.AVATAR_LG -> R.string.appearance_size_avatar_lg
    SizeRole.AVATAR_XL -> R.string.appearance_size_avatar_xl
    SizeRole.TOUCH_TARGET -> R.string.appearance_size_touch_target
    SizeRole.ROUND_BUTTON -> R.string.appearance_size_round_button
    SizeRole.DOT -> R.string.appearance_size_dot
    SizeRole.TIME_COLUMN -> R.string.appearance_size_time_column
    SizeRole.SHEET_MAX -> R.string.appearance_size_sheet_max
    SizeRole.AVATAR_ACCOUNT -> R.string.appearance_size_avatar_account
    SizeRole.BORDER_THIN -> R.string.appearance_size_border_thin
    SizeRole.WIDGET_WIDTH -> R.string.appearance_size_widget_width
    SizeRole.WIDGET_HEIGHT -> R.string.appearance_size_widget_height
    SizeRole.HERO_BADGE -> R.string.appearance_size_hero_badge
}

private fun TypeRole.labelRes(): Int = when (this) {
    TypeRole.DISPLAY_LARGE -> R.string.appearance_type_display_large
    TypeRole.DISPLAY_MEDIUM -> R.string.appearance_type_display_medium
    TypeRole.DISPLAY_SMALL -> R.string.appearance_type_display_small
    TypeRole.HEADLINE_LARGE -> R.string.appearance_type_headline_large
    TypeRole.HEADLINE_MEDIUM -> R.string.appearance_type_headline_medium
    TypeRole.HEADLINE_SMALL -> R.string.appearance_type_headline_small
    TypeRole.TITLE_LARGE -> R.string.appearance_type_title_large
    TypeRole.TITLE_MEDIUM -> R.string.appearance_type_title_medium
    TypeRole.TITLE_SMALL -> R.string.appearance_type_title_small
    TypeRole.BODY_LARGE -> R.string.appearance_type_body_large
    TypeRole.BODY_MEDIUM -> R.string.appearance_type_body_medium
    TypeRole.BODY_SMALL -> R.string.appearance_type_body_small
    TypeRole.LABEL_LARGE -> R.string.appearance_type_label_large
    TypeRole.LABEL_MEDIUM -> R.string.appearance_type_label_medium
    TypeRole.LABEL_SMALL -> R.string.appearance_type_label_small
}

private fun ElevationRole.labelRes(): Int = when (this) {
    ElevationRole.CARD -> R.string.appearance_elevation_card
    ElevationRole.FAB -> R.string.appearance_elevation_fab
    ElevationRole.DIALOG -> R.string.appearance_elevation_dialog
    ElevationRole.SHEET -> R.string.appearance_elevation_sheet
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8)
@Composable
private fun AppearanceEditorScreenPreview() {
    VdvoemTheme {
        AppearanceEditorScreen(
            appearance = AppearanceSettings(),
            onChange = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1113)
@Composable
private fun AppearanceEditorScreenDarkPreview() {
    VdvoemTheme(darkTheme = true) {
        AppearanceEditorScreen(
            appearance = AppearanceSettings(),
            onChange = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorPickerDialogPreview() {
    VdvoemTheme {
        ColorPickerDialog(
            role = ColorRole.PRIMARY,
            initial = Color(0xFF8E4A5F),
            hasOverride = true,
            onDismiss = {},
            onReset = {},
            onConfirm = {}
        )
    }
}