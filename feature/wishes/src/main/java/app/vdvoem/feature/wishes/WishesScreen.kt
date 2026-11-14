package app.vdvoem.feature.wishes

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WishesScreen(
    state: WishesUiState,
    filter: WishFilter,
    sort: WishSort,
    query: String,
    onFilterChange: (WishFilter) -> Unit,
    onSortChange: (WishSort) -> Unit,
    onQueryChange: (String) -> Unit,
    onResetQuery: () -> Unit,
    onAddWish: (title: String, note: String, price: Long?) -> Unit,
    onUpdateWish: (id: String, title: String, note: String, price: Long?) -> Unit,
    onSetStatus: (id: String, status: WishStatus) -> Unit,
    onDeleteWish: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingWish by remember { mutableStateOf<Wish?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Wish?>(null) }

    val visible = remember(state.wishes, filter, sort, query) {
        val needle = query.trim()
        state.wishes
            .filter { wish ->
                when (filter) {
                    WishFilter.ALL -> true
                    WishFilter.WANTED -> wish.status == WishStatus.WANTED
                    WishFilter.RESERVED -> wish.status == WishStatus.RESERVED
                    WishFilter.DONE -> wish.status == WishStatus.DONE
                }
            }
            .filter { wish ->
                needle.isEmpty() ||
                    wish.title.contains(needle, ignoreCase = true) ||
                    wish.note.contains(needle, ignoreCase = true)
            }
            .sortedWith(sort.comparator())
    }

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
            item(key = "hero") {
                WishesHero(count = state.wishes.size)
            }

            item(key = "search") {
                SearchAndSortRow(
                    query = query,
                    onQueryChange = onQueryChange,
                    onResetQuery = onResetQuery,
                    sort = sort,
                    onSortChange = onSortChange,
                    modifier = Modifier.padding(top = VdvoemSpacing.sm)
                )
            }

            item(key = "filters") {
                FilterChipsRow(
                    filter = filter,
                    onFilterChange = onFilterChange
                )
            }

            item(key = "header") {
                SectionHeader(
                    title = stringResource(R.string.wishes_section_list),
                    modifier = Modifier.padding(top = VdvoemSpacing.xs),
                    trailing = {
                        TextButton(onClick = { showAddDialog = true }) {
                            Text(stringResource(R.string.wishes_action_add))
                        }
                    }
                )
            }

            if (state.wishes.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.Favorite,
                        title = stringResource(R.string.wishes_empty_title),
                        message = stringResource(R.string.wishes_empty_message),
                        actionLabel = stringResource(R.string.wishes_empty_action),
                        onAction = { showAddDialog = true }
                    )
                }
            } else if (visible.isEmpty()) {
                if (query.isNotBlank()) {
                    item(key = "empty_search") {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = stringResource(R.string.wishes_search_empty_title),
                            message = stringResource(R.string.wishes_search_empty_message),
                            actionLabel = stringResource(R.string.wishes_search_reset),
                            onAction = onResetQuery
                        )
                    }
                } else {
                    item(key = "empty_filter") {
                        EmptyState(
                            icon = Icons.Filled.Favorite,
                            title = stringResource(R.string.wishes_filter_empty_title),
                            message = stringResource(R.string.wishes_filter_empty_message)
                        )
                    }
                }
            } else {
                items(visible, key = { it.id }) { wish ->
                    WishCard(
                        wish = wish,
                        onEdit = { editingWish = wish },
                        onMarkDone = { onSetStatus(wish.id, WishStatus.DONE) },
                        onDelete = { pendingDelete = wish }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.wishes_fab)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = VdvoemSpacing.screenHorizontal,
                    bottom = VdvoemSpacing.xxl
                )
        )
    }

    if (showAddDialog) {
        WishDialog(
            wish = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, note, price ->
                onAddWish(title, note, price)
                showAddDialog = false
            }
        )
    }

    editingWish?.let { wish ->
        WishDialog(
            wish = wish,
            onDismiss = { editingWish = null },
            onConfirm = { title, note, price ->
                onUpdateWish(wish.id, title, note, price)
                editingWish = null
            }
        )
    }

    pendingDelete?.let { wish ->
        DeleteWishDialog(
            wish = wish,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDeleteWish(wish.id)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun WishesHero(count: Int) {
    VdvoemCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(VdvoemSizes.iconLg)
        )
        Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
        Text(
            text = pluralStringResource(R.plurals.wishes_count, count, count),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
        Text(
            text = stringResource(R.string.wishes_hero_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun SearchAndSortRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onResetQuery: () -> Unit,
    sort: WishSort,
    onSortChange: (WishSort) -> Unit,
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
                            contentDescription = stringResource(R.string.wishes_search_reset),
                            modifier = Modifier.size(VdvoemSizes.iconMd)
                        )
                    }
                }
            },
            label = { Text(stringResource(R.string.wishes_search_label)) },
            placeholder = { Text(stringResource(R.string.wishes_search_hint)) },
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Box {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.wishes_sort),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                WishSort.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(stringResource(item.labelRes())) },
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

@Composable
private fun FilterChipsRow(
    filter: WishFilter,
    onFilterChange: (WishFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
    ) {
        WishFilter.entries.forEach { item ->
            FilterChip(
                selected = item == filter,
                onClick = { onFilterChange(item) },
                label = { Text(stringResource(item.labelRes())) }
            )
        }
    }
}

@Composable
private fun WishCard(
    wish: Wish,
    onEdit: () -> Unit,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit
) {
    VdvoemCard(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onEdit)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wish.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (wish.note.isNotBlank()) {
                    Spacer(modifier = Modifier.size(VdvoemSpacing.xxs))
                    Text(
                        text = wish.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.size(VdvoemSpacing.md))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
                ) {
                    wish.priceEstimate?.let { price ->
                        Text(
                            text = stringResource(R.string.wishes_price, formatPrice(price)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    StatusPill(status = wish.status)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.wishes_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (wish.status != WishStatus.DONE) {
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            TextButton(onClick = onMarkDone) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconSm)
                )
                Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
                Text(stringResource(R.string.wishes_mark_done))
            }
        }
    }
}

@Composable
private fun StatusPill(status: WishStatus) {
    val container = when (status) {
        WishStatus.WANTED -> MaterialTheme.colorScheme.secondaryContainer
        WishStatus.RESERVED -> MaterialTheme.colorScheme.tertiaryContainer
        WishStatus.DONE -> MaterialTheme.colorScheme.primaryContainer
    }
    val content = when (status) {
        WishStatus.WANTED -> MaterialTheme.colorScheme.onSecondaryContainer
        WishStatus.RESERVED -> MaterialTheme.colorScheme.onTertiaryContainer
        WishStatus.DONE -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        Text(
            text = stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(
                horizontal = VdvoemSpacing.md,
                vertical = VdvoemSpacing.xs
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishDialog(
    wish: Wish?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, note: String, price: Long?) -> Unit
) {
    val editing = wish != null
    var title by remember(wish?.id) { mutableStateOf(wish?.title.orEmpty()) }
    var note by remember(wish?.id) { mutableStateOf(wish?.note.orEmpty()) }
    var price by remember(wish?.id) {
        mutableStateOf(wish?.priceEstimate?.toString().orEmpty())
    }
    val canSave = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editing) R.string.wishes_dialog_edit_title else R.string.wishes_dialog_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.wishes_field_title)) },
                    placeholder = { Text(stringResource(R.string.wishes_field_title_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    minLines = 2,
                    label = { Text(stringResource(R.string.wishes_field_note)) },
                    placeholder = { Text(stringResource(R.string.wishes_field_note_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { value ->
                        price = value.filter(Char::isDigit).take(MAX_PRICE_DIGITS)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.wishes_field_price)) },
                    placeholder = { Text(stringResource(R.string.wishes_field_price_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, note, price.toLongOrNull()) },
                enabled = canSave
            ) {
                Text(stringResource(R.string.wishes_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.wishes_cancel))
            }
        }
    )
}

@Composable
private fun DeleteWishDialog(
    wish: Wish,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wishes_delete_title)) },
        text = {
            Text(stringResource(R.string.wishes_delete_message, wish.title))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.wishes_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.wishes_cancel))
            }
        }
    )
}

private fun WishFilter.labelRes(): Int = when (this) {
    WishFilter.ALL -> R.string.wishes_filter_all
    WishFilter.WANTED -> R.string.wishes_status_wanted
    WishFilter.RESERVED -> R.string.wishes_status_reserved
    WishFilter.DONE -> R.string.wishes_status_done
}

private fun WishSort.labelRes(): Int = when (this) {
    WishSort.NEWEST -> R.string.wishes_sort_newest
    WishSort.PRICE_DESC -> R.string.wishes_sort_price
    WishSort.ALPHABET -> R.string.wishes_sort_alphabet
}

private fun WishSort.comparator(): Comparator<Wish> = when (this) {
    WishSort.NEWEST -> compareByDescending<Wish> { it.updatedAt }
    WishSort.PRICE_DESC -> compareByDescending<Wish> { it.priceEstimate ?: Long.MIN_VALUE }
    WishSort.ALPHABET -> compareBy<Wish, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
}

private fun WishStatus.labelRes(): Int = when (this) {
    WishStatus.WANTED -> R.string.wishes_status_wanted
    WishStatus.RESERVED -> R.string.wishes_status_reserved
    WishStatus.DONE -> R.string.wishes_status_done
}

private fun formatPrice(price: Long): String =
    NumberFormat.getIntegerInstance(Locale("ru", "RU")).format(price)

private const val MAX_PRICE_DIGITS = 12

@Preview(showBackground = true)
@Composable
private fun WishesScreenPreview() {
    VdvoemTheme {
        WishesScreen(
            state = WishesUiState(
                wishes = listOf(
                    Wish(
                        id = "1",
                        pairId = "local",
                        title = "Наушники",
                        note = "Беспроводные, лучше с шумоподавлением",
                        priceEstimate = 3500,
                        status = WishStatus.WANTED
                    ),
                    Wish(
                        id = "2",
                        pairId = "local",
                        title = "Поездка в Карелию",
                        note = "На майские праздники",
                        status = WishStatus.RESERVED
                    )
                ),
                loading = false
            ),
            filter = WishFilter.ALL,
            sort = WishSort.NEWEST,
            query = "",
            onFilterChange = {},
            onSortChange = {},
            onQueryChange = {},
            onResetQuery = {},
            onAddWish = { _, _, _ -> },
            onUpdateWish = { _, _, _, _ -> },
            onSetStatus = { _, _ -> },
            onDeleteWish = {},
            modifier = Modifier
        )
    }
}