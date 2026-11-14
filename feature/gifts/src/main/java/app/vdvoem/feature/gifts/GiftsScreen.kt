package app.vdvoem.feature.gifts

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.ui.components.EmptyState
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
    val boughtTitle = stringResource(R.string.gifts_section_bought)
    val givenTitle = stringResource(R.string.gifts_section_given)

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
            item(key = "hero") { GiftsHero(total = state.ideaBudget) }

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
                        message = stringResource(R.string.gifts_empty_message),
                        actionLabel = stringResource(R.string.gifts_empty_action),
                        onAction = { showAddDialog = true }
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
                        title = ideasTitle,
                        gifts = state.ideas,
                        onEdit = { editingGift = it },
                        onSetStatus = onSetStatus,
                        onDeleteRequest = { deletingGift = it }
                    )
                    giftSection(
                        title = boughtTitle,
                        gifts = state.bought,
                        onEdit = { editingGift = it },
                        onSetStatus = onSetStatus,
                        onDeleteRequest = { deletingGift = it }
                    )
                    giftSection(
                        title = givenTitle,
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
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.gifts_fab)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = VdvoemSpacing.screenHorizontal, bottom = VdvoemSpacing.xxl)
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
private fun GiftsHero(total: Long) {
    VdvoemCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                modifier = Modifier.size(VdvoemSizes.heroBadge)
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(VdvoemSpacing.md)
                )
            }
            Spacer(modifier = Modifier.size(VdvoemSpacing.md))
            Column {
                Text(
                    text = stringResource(R.string.gifts_hero_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.gifts_hero_total, formatRubles(total)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
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
                                    contentDescription = null,
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
    title: String,
    gifts: List<Gift>,
    onEdit: (Gift) -> Unit,
    onSetStatus: (String, GiftStatus) -> Unit,
    onDeleteRequest: (Gift) -> Unit
) {
    item(key = "header_$title") {
        SectionHeader(title = title, modifier = Modifier.padding(top = VdvoemSpacing.sm))
    }
    if (gifts.isEmpty()) {
        item(key = "empty_$title") {
            Text(
                text = stringResource(R.string.gifts_section_empty),
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
    VdvoemCard(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onEdit)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gift.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (gift.note.isNotBlank()) {
                    Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
                    Text(
                        text = gift.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (budget != null) {
                    Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                    Text(
                        text = stringResource(R.string.gifts_budget_value, formatRubles(budget)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDeleteRequest) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.gifts_delete)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(gift.status.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            when (gift.status) {
                GiftStatus.IDEA -> OutlinedButton(onClick = { onSetStatus(gift.id, GiftStatus.BOUGHT) }) {
                    Text(stringResource(R.string.gifts_action_bought))
                }
                GiftStatus.BOUGHT -> OutlinedButton(onClick = { onSetStatus(gift.id, GiftStatus.GIVEN) }) {
                    Text(stringResource(R.string.gifts_action_given))
                }
                GiftStatus.GIVEN -> Unit
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

@Preview(showBackground = true)
@Composable
private fun GiftsScreenPreview() {
    VdvoemTheme {
        GiftsScreen(
            state = GiftsUiState(
                ideas = listOf(Gift("1", "local", "Книга", "Любимый автор", 12000)),
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