package app.vdvoem.ui.pair

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.vdvoem.R
import app.vdvoem.core.domain.time.Dates
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import kotlinx.coroutines.delay

/**
 * Онбординг синхронизации: один общий сервер и код приглашения на двоих.
 *
 * Экран не ходит в сеть сам — адрес и код меняются через колбэки, а результат
 * приходит в [ConnectState]. В режиме [ConnectState.Created] показываем код,
 * который партнёр введёт у себя.
 *
 * @param onCancel закрывает экран на любом шаге (в том числе после создания пары).
 */
@Composable
fun ConnectPairScreen(
    state: ConnectState,
    onServerChange: (String) -> Unit,
    onInviteCodeChange: (String) -> Unit,
    onNamesChange: (String, String) -> Unit,
    onSinceChange: (Long) -> Unit,
    onCreatePair: () -> Unit,
    onJoin: (String) -> Unit,
    onSyncNow: () -> Unit = {},
    onLeavePair: () -> Unit = {},
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = VdvoemSpacing.screenHorizontal,
                end = VdvoemSpacing.screenHorizontal,
                top = VdvoemSpacing.sm,
                bottom = VdvoemSpacing.listBottom
            ),
        verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
    ) {
        if (state !is ConnectState.Connected) HeroCard()

        when (state) {
            is ConnectState.Form -> FormContent(
                state = state,
                onServerChange = onServerChange,
                onInviteCodeChange = onInviteCodeChange,
                onNamesChange = onNamesChange,
                onSinceChange = onSinceChange,
                onCreatePair = onCreatePair,
                onJoin = onJoin,
                onCancel = onCancel
            )

            is ConnectState.Created -> CreatedContent(
                state = state,
                onCancel = onCancel
            )

            is ConnectState.Connected -> ConnectedContent(
                state = state,
                onSyncNow = onSyncNow,
                onLeavePair = onLeavePair,
                onCancel = onCancel
            )
        }
    }
}

/** Верхняя карточка с назначением экрана. */
@Composable
private fun HeroCard() {
    VdvoemCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(VdvoemSizes.iconLg)
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.md))
        Text(
            text = stringResource(R.string.connect_hero_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
        Text(
            text = stringResource(R.string.connect_hero_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormContent(
    state: ConnectState.Form,
    onServerChange: (String) -> Unit,
    onInviteCodeChange: (String) -> Unit,
    onNamesChange: (String, String) -> Unit,
    onSinceChange: (Long) -> Unit,
    onCreatePair: () -> Unit,
    onJoin: (String) -> Unit,
    onCancel: () -> Unit
) {
    val canJoin = !state.busy &&
        state.inviteCode.length == ConnectState.INVITE_CODE_LENGTH
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    VdvoemCard {
        Text(
            text = stringResource(R.string.connect_create_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onServerChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy,
            singleLine = true,
            label = { Text(stringResource(R.string.connect_server_label)) },
            placeholder = { Text(stringResource(R.string.connect_server_hint)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            )
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
        Text(
            text = stringResource(R.string.connect_server_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
        ) {
            OutlinedTextField(
                value = state.nameA,
                onValueChange = { onNamesChange(it, state.nameB) },
                modifier = Modifier.weight(1f),
                enabled = !state.busy,
                singleLine = true,
                label = { Text(stringResource(R.string.pair_field_you)) }
            )
            OutlinedTextField(
                value = state.nameB,
                onValueChange = { onNamesChange(state.nameA, it) },
                modifier = Modifier.weight(1f),
                enabled = !state.busy,
                singleLine = true,
                label = { Text(stringResource(R.string.pair_field_partner)) }
            )
        }
        Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy
        ) {
            Text(
                text = stringResource(
                    R.string.pair_since_value,
                    Dates.format(Dates.toLocalDate(state.sinceMillis))
                )
            )
        }
        Spacer(modifier = Modifier.height(VdvoemSpacing.lg))
        Button(
            onClick = onCreatePair,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VdvoemSpacing.huge + VdvoemSpacing.sm),
            enabled = !state.busy && state.serverUrl.isNotBlank()
        ) {
            Text(stringResource(R.string.connect_create))
        }
    }

    SectionHeader(
        title = stringResource(R.string.connect_join_title),
        modifier = Modifier.padding(top = VdvoemSpacing.sm)
    )

    VdvoemCard {
        Text(
            text = stringResource(R.string.connect_join_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
        OutlinedTextField(
            value = state.inviteCode,
            onValueChange = { raw -> onInviteCodeChange(ConnectState.normalizeInviteCode(raw)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy,
            singleLine = true,
            label = { Text(stringResource(R.string.connect_code_hint)) },
            supportingText = {
                Text(
                    text = stringResource(
                        R.string.connect_code_counter,
                        state.inviteCode.length,
                        ConnectState.INVITE_CODE_LENGTH
                    )
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            )
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.md))
        Button(
            onClick = { onJoin(state.inviteCode) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VdvoemSpacing.huge + VdvoemSpacing.sm),
            enabled = canJoin
        ) {
            Text(stringResource(R.string.connect_join))
        }
    }

    if (state.busy) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(VdvoemSizes.iconLg),
                strokeWidth = VdvoemSpacing.xxs
            )
            Spacer(modifier = Modifier.width(VdvoemSpacing.sm))
            Text(
                text = stringResource(R.string.connect_busy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    state.error?.let { message ->
        if (message.isNotBlank()) {
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    TextButton(
        onClick = onCancel,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VdvoemSpacing.huge + VdvoemSpacing.sm)
    ) {
        Text(stringResource(R.string.pair_cancel))
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = Dates.toPickerMillis(state.sinceMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { picked ->
                            onSinceChange(Dates.fromPickerMillis(picked))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.pair_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.pair_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Пара создана: крупный код, копирование и пояснение, что делать дальше. */
@Composable
private fun CreatedContent(
    state: ConnectState.Created,
    onCancel: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by rememberSaveable(state.inviteCode) { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MS)
            copied = false
        }
    }

    VdvoemCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            text = stringResource(R.string.connect_created_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.md))
        Text(
            text = state.inviteCode.chunked(INVITE_CODE_GROUP).joinToString(" "),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
        Text(
            text = stringResource(R.string.connect_created_text),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.lg))
        Button(
            onClick = {
                clipboard.setText(AnnotatedString(state.inviteCode))
                copied = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VdvoemSpacing.huge + VdvoemSpacing.sm)
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(VdvoemSizes.iconSm)
            )
            Spacer(modifier = Modifier.width(VdvoemSpacing.sm))
            Text(
                stringResource(
                    if (copied) R.string.connect_copied else R.string.connect_copy
                )
            )
        }
    }

    TextButton(
        onClick = onCancel,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VdvoemSpacing.huge + VdvoemSpacing.sm)
    ) {
        Text(stringResource(R.string.connect_done))
    }
}

/**
 * Разрешение «не экономить заряд» — без него Samsung/Xiaomi усыпляют приложение,
 * и уведомления партнёра ждут, пока человек сам откроет «Вдвоём».
 * Оформление как у остальных строк: заголовок, объяснение, кнопка.
 */
@Composable
private fun BackgroundDeliveryCard() {
    val context = LocalContext.current
    var exempt by remember { mutableStateOf(isBatteryExempt(context)) }
    // Человек возвращается из системных настроек — сразу показываем новый статус.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { exempt = isBatteryExempt(context) }

    VdvoemCard {
        SectionHeader(title = stringResource(R.string.connect_background_title))
        Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
        Text(
            text = stringResource(
                if (exempt) R.string.connect_background_ok else R.string.connect_background_text
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!exempt) {
            Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
            OutlinedButton(
                onClick = { requestBatteryExemption(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = VdvoemSpacing.huge)
            ) {
                Text(stringResource(R.string.connect_background_action))
            }
        }
    }
}

private fun isBatteryExempt(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val power = context.getSystemService(PowerManager::class.java) ?: return true
    return power.isIgnoringBatteryOptimizations(context.packageName)
}

/** Системный диалог «разрешить работу в фоне»; если прошивка его не знает — общий список. */
private fun requestBatteryExemption(context: Context) {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        .setData(Uri.parse("package:${context.packageName}"))
    val opened = runCatching { context.startActivity(direct) }.isSuccess
    if (!opened) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}

/**
 * Пара уже подключена: показываем состав пары, код для второго телефона,
 * ручную синхронизацию и выход из пары. Раньше здесь снова была форма создания —
 * из-за этого можно было случайно завести вторую пару и потерять данные партнёра.
 */
@Composable
private fun ConnectedContent(
    state: ConnectState.Connected,
    onSyncNow: () -> Unit,
    onLeavePair: () -> Unit,
    onCancel: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by rememberSaveable(state.inviteCode) { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MS)
            copied = false
        }
    }

    VdvoemCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(VdvoemSizes.iconMd)
            )
            Spacer(modifier = Modifier.width(VdvoemSpacing.sm))
            Text(
                text = stringResource(R.string.connect_connected_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
        Text(
            text = stringResource(
                R.string.connect_connected_names,
                state.nameA.ifBlank { stringResource(R.string.connect_connected_name_a) },
                state.nameB.ifBlank { stringResource(R.string.connect_connected_name_b) }
            ),
            style = MaterialTheme.typography.bodyLarge
        )
        if (state.sinceMillis > 0) {
            Spacer(modifier = Modifier.height(VdvoemSpacing.xxs))
            Text(
                text = stringResource(
                    R.string.connect_connected_since,
                    Dates.format(Dates.toLocalDate(state.sinceMillis))
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(VdvoemSpacing.xxs))
        Text(
            text = state.serverUrl,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (state.inviteCode.isNotBlank()) {
        VdvoemCard {
            SectionHeader(title = stringResource(R.string.connect_invite_title))
            Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
            Text(
                text = state.inviteCode.chunked(INVITE_CODE_GROUP).joinToString(" "),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
            Text(
                text = stringResource(R.string.connect_invite_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(state.inviteCode))
                    copied = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = VdvoemSpacing.huge)
            ) {
                Text(
                    stringResource(if (copied) R.string.connect_copied else R.string.connect_copy)
                )
            }
        }
    }

    VdvoemCard {
        Button(
            onClick = onSyncNow,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VdvoemSpacing.huge + VdvoemSpacing.sm),
            enabled = !state.syncing
        ) {
            if (state.syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(VdvoemSizes.iconSm),
                    strokeWidth = VdvoemSpacing.xxs
                )
                Spacer(modifier = Modifier.width(VdvoemSpacing.sm))
            }
            Text(
                stringResource(
                    if (state.syncing) R.string.connect_syncing else R.string.connect_sync_now
                )
            )
        }
        Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
        Text(
            text = stringResource(R.string.connect_sync_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    BackgroundDeliveryCard()

    state.error?.let { message ->
        if (message.isNotBlank()) {
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    TextButton(
        onClick = onCancel,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VdvoemSpacing.huge + VdvoemSpacing.sm)
    ) {
        Text(stringResource(R.string.connect_done))
    }

    TextButton(
        onClick = { confirmLeave = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.connect_leave),
            color = MaterialTheme.colorScheme.error
        )
    }

    if (confirmLeave) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.connect_leave_title)) },
            text = { Text(stringResource(R.string.connect_leave_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLeave = false
                        onLeavePair()
                    }
                ) {
                    Text(stringResource(R.string.connect_leave_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) {
                    Text(stringResource(R.string.pair_cancel))
                }
            }
        )
    }
}

/** Код читаемее группами по 4 символа, копируется всегда без пробелов. */
private const val INVITE_CODE_GROUP = 4

/** Сколько держим подпись «Скопировано». */
private const val COPIED_FEEDBACK_MS = 2_000L

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8, locale = "ru")
@Composable
private fun ConnectPairFormPreview() {
    VdvoemTheme {
        ConnectPairScreen(
            state = ConnectState.Form(
                serverUrl = "https://sync.example.com",
                inviteCode = "AB12"
            ),
            onServerChange = {},
            onInviteCodeChange = {},
            onNamesChange = { _, _ -> },
            onSinceChange = {},
            onCreatePair = {},
            onJoin = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8, locale = "ru")
@Composable
private fun ConnectPairBusyPreview() {
    VdvoemTheme {
        ConnectPairScreen(
            state = ConnectState.Form(
                serverUrl = "https://sync.example.com",
                inviteCode = "AB12CD34",
                busy = true
            ),
            onServerChange = {},
            onInviteCodeChange = {},
            onNamesChange = { _, _ -> },
            onSinceChange = {},
            onCreatePair = {},
            onJoin = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8, locale = "ru")
@Composable
private fun ConnectPairCreatedPreview() {
    VdvoemTheme {
        ConnectPairScreen(
            state = ConnectState.Created(
                pairId = "pair-preview",
                inviteCode = "AB12CD34"
            ),
            onServerChange = {},
            onInviteCodeChange = {},
            onNamesChange = { _, _ -> },
            onSinceChange = {},
            onCreatePair = {},
            onJoin = {},
            onCancel = {}
        )
    }
}