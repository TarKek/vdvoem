package app.vdvoem.feature.gifts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GiftsScreen(
    state: GiftsUiState,
    onAdd: (title: String, note: String, budget: Long?) -> Unit,
    onUpdate: (id: String, title: String, note: String, budget: Long?, status: GiftStatus) -> Unit,
    onSetStatus: (id: String, status: GiftStatus) -> Unit,
    onDelete: (id: String) -> Unit,
    onQueryChange: (String) -> Unit,
    onResetQuery: () -> Unit,
    onSortChange: (GiftSort) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGift by remember { mutableStateOf<Gift?>(null) }
    var deletingGift by remember { mutableStateOf<Gift?>(null) }
    val ideasTitle = stringResource(R.string.gifts_section_ideas)
    val ideasCaption = stringResource(R.string.gifts_section_ideas_caption)
    val ideasEmpty = stringResource(R.string.gifts_section_ideas_empty)
    val boughtTitle = stringResource(R.string.gifts_section_bought)
    val boughtCaption = stringResource(R.string.gifts_section_bought_caption)
    val boughtEmpty = stringResource(R.string.gifts_section_bought_empty)
    val givenTitle = stringResource(R.string.gifts_section_given)
    val givenCaption = stringResource(R.string.gifts_section_given_caption)
    val givenEmpty = stringResource(R.string.gifts_section_given_empty)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VdvoemSpacing.screenHorizontal,
                end = VdvoemSpacing.screenHorizontal,
                top = VdvoemSpacing.lg,
                bottom = VdvoemSpacing.listBottom
            ),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
        ) {
            if (state.hasGifts && state.ideaBudget > 0) {
                item(key = "summary") {
                    Text(
                        text = stringResource(
                            R.string.gifts_hero_total,
                            formatRubles(state.ideaBudget)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.hasGifts) {
                item(key = "search") {
                    SearchAndSortRow(
                        query = state.query,
                        onQueryChange = onQueryChange,
                        onResetQuery = onResetQuery,
                        sort = state.sort,
                        onSortChange = onSortChange
                    )
                }
            }

            when {
                state.isEmpty -> item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.ShoppingCart,
                        title = stringResource(R.string.gifts_empty_title),
                        message = stringResource(R.string.gifts_empty_message)
                    )
                }

                state.isSearchEmpty -> item(key = "search_empty") {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = stringResource(R.string.gifts_search_empty_title),
                        message = stringResource(
                            R.string.gifts_search_empty_message,
                            state.query.trim()
                        ),
                        actionLabel = stringResource(R.string.gifts_search_empty_action),
                        onAction = onResetQuery
                    )
                }

                else -> {
                    giftSection(
                        key = "ideas",
                        title = ideasTitle,
                        caption = ideasCaption,
                        emptyText = ideasEmpty,
                        gifts = state.ideas,
                        onEdit = { editingGift = it },
                        onSetStatus = onSetStatus,
                        onDeleteRequest = { deletingGift = it }
                    )
                    giftSection(
                        key = "bought",
                        title = boughtTitle,
                        caption = boughtCaption,
                        emptyText = boughtEmpty,
                        gifts = state.bought,
                        onEdit = { editingGift = it },
                        onSetStatus = onSetStatus,
                        onDeleteRequest = { deletingGift = it }
                    )
                    giftSection(
                        key = "given",
                        title = givenTitle,
                        caption = givenCaption,
                        emptyText = givenEmpty,
                        gifts = state.given,
                        onEdit = { editingGift = it },
                        onSetStatus = onSetStatus,
                        onDeleteRequest = { deletingGift = it }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
            },
            text = { Text(stringResource(R.string.gifts_fab)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = VdvoemSpacing.screenHorizontal,
                    bottom = VdvoemSpacing.xl
                )
        )
    }

    if (showAddDialog) {
        GiftDialog(
            gift = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, note, budget, _ ->
                onAdd(title, note, budget)
                showAddDialog = false
            }
        )
    }

    val editing = editingGift
    if (editing != null) {
        GiftDialog(
            gift = editing,
            onDismiss = { editingGift = null },
            onConfirm = { title, note, budget, status ->
                onUpdate(editing.id, title, note, budget, status)
                editingGift = null
            }
        )
    }

    val deleting = deletingGift
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deletingGift = null },
            title = { Text(stringResource(R.string.gifts_delete_title)) },
            text = { Text(stringResource(R.string.gifts_delete_message, deleting.title)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(deleting.id)
                    deletingGift = null
                }) { Text(stringResource(R.string.gifts_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingGift = null }) {
                    Text(stringResource(R.string.gifts_cancel))
                }
            }
        )
    }
}

@Composable
private fun SearchAndSortRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onResetQuery: () -> Unit,
    sort: GiftSort,
    onSortChange: (GiftSort) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onResetQuery) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.gifts_search_reset),
                            modifier = Modifier.size(VdvoemSizes.iconMd)
                        )
                    }
                }
            },
            label = { Text(stringResource(R.string.gifts_search_label)) },
            placeholder = { Text(stringResource(R.string.gifts_search_hint)) },
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Box {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.gifts_sort),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                GiftSort.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(stringResource(item.labelRes)) },
                        onClick = {
                            onSortChange(item)
                            showSortMenu = false
                        },
                        trailingIcon = {
                            if (item == sort) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.gifts_sort_selected),
                                    modifier = Modifier.size(VdvoemSizes.iconMd)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.giftSection(
    key: String,
    title: String,
    caption: String,
    emptyText: String,
    gifts: List<Gift>,
    onEdit: (Gift) -> Unit,
    onSetStatus: (String, GiftStatus) -> Unit,
    onDeleteRequest: (Gift) -> Unit
) {
    item(key = "header_$key") {
        Column(
            modifier = Modifier.padding(top = VdvoemSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xxs)
        ) {
            SectionHeader(
                title = title,
                trailing = { InfoPill(text = gifts.size.toString()) }
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (gifts.isEmpty()) {
        item(key = "empty_$key") {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = VdvoemSpacing.xs)
            )
        }
    } else {
        items(gifts, key = { it.id }) { gift ->
            GiftCard(
                gift = gift,
                onEdit = { onEdit(gift) },
                onSetStatus = onSetStatus,
                onDeleteRequest = { onDeleteRequest(gift) }
            )
        }
    }
}

@Composable
private fun GiftCard(
    gift: Gift,
    onEdit: () -> Unit,
    onSetStatus: (String, GiftStatus) -> Unit,
    onDeleteRequest: () -> Unit
) {
    val budget = gift.budget
    // Новые данные несут прямую ссылку; старые подарки узнаём по метке в заметке.
    val fromWish = gift.wishId != null || gift.note.contains(NOTE_FROM_WISH, ignoreCase = true)
    // Метка-заглушка не должна дублировать пилюлю «Из желаний».
    val visibleNote = if (gift.note.trim() == NOTE_FROM_WISH) "" else gift.note
    VdvoemCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = gift.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = VdvoemSpacing.sm)
            )
            InfoPill(text = stringResource(gift.status.labelRes))
        }

        if (visibleNote.isNotBlank() || budget != null || fromWish) {
            Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (fromWish) {
                    InfoPill(
                        text = stringResource(R.string.gifts_from_wish),
                        modifier = Modifier.padding(end = VdvoemSpacing.sm)
                    )
                }
                if (visibleNote.isNotBlank()) {
                    Text(
                        text = visibleNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (budget != null) {
                    Text(
                        text = formatRubles(budget),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = VdvoemSpacing.sm)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (actionLabel, nextStatus) = when (gift.status) {
                GiftStatus.IDEA -> R.string.gifts_action_bought to GiftStatus.BOUGHT
                GiftStatus.BOUGHT -> R.string.gifts_action_given to GiftStatus.GIVEN
                GiftStatus.GIVEN -> R.string.gifts_action_to_bought to GiftStatus.BOUGHT
            }
            FilledTonalButton(
                onClick = { onSetStatus(gift.id, nextStatus) },
                contentPadding = PaddingValues(
                    horizontal = VdvoemSpacing.md,
                    vertical = VdvoemSpacing.sm
                )
            ) {
                Text(
                    text = stringResource(actionLabel),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDeleteRequest) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.gifts_delete),
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
            }
        }
    }
}

private val GiftStatus.labelRes: Int
    get() = when (this) {
        GiftStatus.IDEA -> R.string.gifts_status_idea
        GiftStatus.BOUGHT -> R.string.gifts_status_bought
        GiftStatus.GIVEN -> R.string.gifts_status_given
    }

private val GiftSort.labelRes: Int
    get() = when (this) {
        GiftSort.NEWEST -> R.string.gifts_sort_newest
        GiftSort.BUDGET_DESC -> R.string.gifts_sort_budget
        GiftSort.ALPHABET -> R.string.gifts_sort_alphabet
    }

/** Один диалог на добавление и правку: `gift == null` — режим создания. */
@Composable
private fun GiftDialog(
    gift: Gift?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, note: String, budget: Long?, status: GiftStatus) -> Unit
) {
    var title by rememberSaveable(gift?.id) { mutableStateOf(gift?.title.orEmpty()) }
    var note by rememberSaveable(gift?.id) { mutableStateOf(gift?.note.orEmpty()) }
    var budgetText by rememberSaveable(gift?.id) { mutableStateOf(gift?.budget?.toString().orEmpty()) }
    var status by rememberSaveable(gift?.id) { mutableStateOf(gift?.status ?: GiftStatus.IDEA) }
    val budget = budgetText.toLongOrNull()
    val canSave = title.isNotBlank() && (budgetText.isBlank() || budget != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (gift == null) R.string.gifts_dialog_title else R.string.gifts_edit_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.gifts_field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.gifts_field_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) budgetText = it },
                    label = { Text(stringResource(R.string.gifts_field_budget)) },
                    supportingText = { Text(stringResource(R.string.gifts_field_budget_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.gifts_field_status),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
                ) {
                    GiftStatus.entries.forEach { item ->
                        FilterChip(
                            selected = status == item,
                            onClick = { status = item },
                            label = { Text(stringResource(item.labelRes)) },
                            leadingIcon = if (status == item) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(VdvoemSizes.iconSm)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.gifts_field_status_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, note, budget, status) },
                enabled = canSave
            ) { Text(stringResource(R.string.gifts_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.gifts_cancel)) }
        }
    )
}

private fun formatRubles(value: Long): String =
    NumberFormat.getIntegerInstance(Locale("ru", "RU")).format(value) + " ₽"

/** Метка, которую ставит модуль желаний, когда желание превращают в подарок. */
private const val NOTE_FROM_WISH = "Из желаний"

@Preview(showBackground = true)
@Composable
private fun GiftsScreenPreview() {
    VdvoemTheme {
        GiftsScreen(
            state = GiftsUiState(
                ideas = listOf(Gift("1", "local", "Книга", "Любимый автор", 12000)),
                bought = listOf(Gift("2", "local", "Свеча", "", 1500, GiftStatus.BOUGHT)),
                ideaBudget = 12000,
                hasGifts = true
            ),
            onAdd = { _, _, _ -> },
            onUpdate = { _, _, _, _, _ -> },
            onSetStatus = { _, _ -> },
            onDelete = {},
            onQueryChange = {},
            onResetQuery = {},
            onSortChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GiftCardPreview() {
    VdvoemTheme {
        GiftCard(
            gift = Gift("1", "local", "Сертификат в спа", "Из желаний", 8000),
            onEdit = {},
            onSetStatus = { _, _ -> },
            onDeleteRequest = {}
        )
    }
}