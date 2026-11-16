package app.vdvoem.feature.wishes

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

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
    onAddWish: (title: String, note: String, price: Long?, priority: Int, url: String, surprise: Boolean) -> Unit,
    onUpdateWish: (
        id: String,
        title: String,
        note: String,
        price: Long?,
        priority: Int,
        url: String,
        surprise: Boolean
    ) -> Unit,
    onSetStatus: (id: String, status: WishStatus) -> Unit,
    onDeleteWish: (id: String) -> Unit,
    onMakeGift: (Wish) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingWish by remember { mutableStateOf<Wish?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Wish?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val giftAddedMessage = stringResource(R.string.wishes_gift_added)

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
            if (!state.loading && state.wishes.isNotEmpty()) {
                item(key = "summary") {
                    val doneCount = state.wishes.count { it.status == WishStatus.DONE }
                    Text(
                        text = stringResource(
                            R.string.wishes_summary,
                            pluralStringResource(
                                R.plurals.wishes_count,
                                state.wishes.size,
                                state.wishes.size
                            ),
                            stringResource(R.string.wishes_summary_done, doneCount)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "search") {
                SearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onResetQuery = onResetQuery
                )
            }

            item(key = "filters") {
                WishesControls(
                    filter = filter,
                    onFilterChange = onFilterChange,
                    sort = sort,
                    onSortChange = onSortChange
                )
            }

            when {
                state.loading -> item(key = "loading") {
                    HintText(stringResource(R.string.wishes_loading))
                }

                state.wishes.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.Favorite,
                        title = stringResource(R.string.wishes_empty_title),
                        message = stringResource(R.string.wishes_empty_message)
                    )
                }

                visible.isEmpty() && query.isNotBlank() -> item(key = "empty_search") {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = stringResource(R.string.wishes_search_empty_title),
                        message = stringResource(R.string.wishes_search_empty_message),
                        actionLabel = stringResource(R.string.wishes_search_reset),
                        onAction = onResetQuery
                    )
                }

                visible.isEmpty() -> item(key = "empty_filter") {
                    EmptyState(
                        icon = Icons.Filled.Favorite,
                        title = stringResource(R.string.wishes_filter_empty_title),
                        message = stringResource(R.string.wishes_filter_empty_message),
                        actionLabel = stringResource(R.string.wishes_filter_reset),
                        onAction = { onFilterChange(WishFilter.ALL) }
                    )
                }

                else -> items(visible, key = { it.id }) { wish ->
                    WishCard(
                        wish = wish,
                        onEdit = { editingWish = wish },
                        onMarkDone = { onSetStatus(wish.id, WishStatus.DONE) },
                        onMakeGift = {
                            onMakeGift(wish)
                            scope.launch { snackbarHostState.showSnackbar(giftAddedMessage) }
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VdvoemSpacing.listBottom)
        )

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
            },
            text = { Text(stringResource(R.string.wishes_fab)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = VdvoemSpacing.screenHorizontal,
                    bottom = VdvoemSpacing.xl
                )
        )
    }

    if (showAddDialog) {
        WishDialog(
            wish = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, note, price, priority, url, surprise ->
                onAddWish(title, note, price, priority, url, surprise)
                showAddDialog = false
            },
            onDelete = null
        )
    }

    editingWish?.let { wish ->
        WishDialog(
            wish = wish,
            onDismiss = { editingWish = null },
            onConfirm = { title, note, price, priority, url, surprise ->
                onUpdateWish(wish.id, title, note, price, priority, url, surprise)
                editingWish = null
            },
            onDelete = {
                editingWish = null
                pendingDelete = wish
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
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VdvoemSpacing.xl)
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onResetQuery: () -> Unit
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
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WishesControls(
    filter: WishFilter,
    onFilterChange: (WishFilter) -> Unit,
    sort: WishSort,
    onSortChange: (WishSort) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)) {
        Text(
            text = stringResource(R.string.wishes_filter_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
        ) {
            WishFilter.entries.forEach { item ->
                FilterChip(
                    selected = item == filter,
                    onClick = { onFilterChange(item) },
                    label = { Text(stringResource(item.labelRes())) },
                    leadingIcon = if (item == filter) {
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
        SortMenuButton(sort = sort, onSortChange = onSortChange)
    }
}

/** Сортировка подписана текстом и открывает меню с пунктами и галочкой. */
@Composable
private fun SortMenuButton(
    sort: WishSort,
    onSortChange: (WishSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                modifier = Modifier.size(VdvoemSizes.iconMd)
            )
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            Text(
                text = stringResource(
                    R.string.wishes_sort_button,
                    stringResource(sort.labelRes())
                )
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WishSort.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(stringResource(item.labelRes())) },
                    onClick = {
                        onSortChange(item)
                        expanded = false
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

@Composable
private fun WishCard(
    wish: Wish,
    onEdit: () -> Unit,
    onMarkDone: () -> Unit,
    onMakeGift: () -> Unit
) {
    val editHint = stringResource(R.string.wishes_card_edit_hint, wish.title)
    VdvoemCard(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(
                onClickLabel = stringResource(R.string.wishes_card_edit_label),
                onClick = onEdit
            )
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wish.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (wish.note.isNotBlank()) {
                    Spacer(modifier = Modifier.size(VdvoemSpacing.xxs))
                    Text(
                        text = wish.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
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
                if (wish.safePriority != Wish.PRIORITY_NORMAL || wish.surprise) {
                    Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)) {
                        if (wish.safePriority != Wish.PRIORITY_NORMAL) {
                            WishPill(
                                text = stringResource(priorityLabelRes(wish.safePriority)),
                                container = priorityContainer(wish.safePriority),
                                content = priorityContent(wish.safePriority)
                            )
                        }
                        if (wish.surprise) {
                            WishPill(
                                text = stringResource(R.string.wishes_pill_surprise),
                                container = MaterialTheme.colorScheme.secondaryContainer,
                                content = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                if (wish.url.isNotBlank()) {
                    Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
                    WishLink(url = wish.url)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = editHint,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = VdvoemSpacing.xxs)
                    .size(VdvoemSizes.iconLg)
            )
        }

        if (wish.status != WishStatus.DONE) {
            Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (wish.status == WishStatus.WANTED) {
                    FilledTonalButton(
                        onClick = onMakeGift,
                        contentPadding = PaddingValues(
                            horizontal = VdvoemSpacing.md,
                            vertical = VdvoemSpacing.sm
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.wishes_action_make_gift),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
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

@Composable
private fun WishPill(text: String, container: Color, content: Color) {
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(
                horizontal = VdvoemSpacing.md,
                vertical = VdvoemSpacing.xs
            )
        )
    }
}

/** Ссылка на товар: тап открывает браузер, а не диалог правки. */
@Composable
private fun WishLink(url: String) {
    val context = LocalContext.current
    val openLabel = stringResource(R.string.wishes_open_link)
    Text(
        text = url,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClickLabel = openLabel) { openLink(context, url) }
    )
}

/** Схему добавляем сами: люди часто вставляют ссылку без `https://`. */
private fun openLink(context: Context, url: String) {
    val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized))) }
        .onFailure { error ->
            android.util.Log.w(TAG, "Не удалось открыть ссылку $normalized: ${error.message}")
        }
}

@Composable
private fun priorityContainer(priority: Int): Color = when (priority) {
    Wish.PRIORITY_HIGH -> MaterialTheme.colorScheme.tertiaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun priorityContent(priority: Int): Color = when (priority) {
    Wish.PRIORITY_HIGH -> MaterialTheme.colorScheme.onTertiaryContainer
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun priorityLabelRes(priority: Int): Int = when (priority) {
    Wish.PRIORITY_HIGH -> R.string.wishes_priority_high
    Wish.PRIORITY_LOW -> R.string.wishes_priority_low
    else -> R.string.wishes_priority_normal
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishDialog(
    wish: Wish?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, note: String, price: Long?, priority: Int, url: String, surprise: Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    val editing = wish != null
    var title by remember(wish?.id) { mutableStateOf(wish?.title.orEmpty()) }
    var note by remember(wish?.id) { mutableStateOf(wish?.note.orEmpty()) }
    var price by remember(wish?.id) {
        mutableStateOf(wish?.priceEstimate?.toString().orEmpty())
    }
    var priority by remember(wish?.id) { mutableStateOf(wish?.safePriority ?: Wish.PRIORITY_NORMAL) }
    var url by remember(wish?.id) { mutableStateOf(wish?.url.orEmpty()) }
    var surprise by remember(wish?.id) { mutableStateOf(wish?.surprise == true) }
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
            ) {
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
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.trim() },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    label = { Text(stringResource(R.string.wishes_field_url)) },
                    placeholder = { Text(stringResource(R.string.wishes_field_url_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                PriorityPicker(priority = priority, onPriorityChange = { priority = it })
                SurpriseSwitch(surprise = surprise, onSurpriseChange = { surprise = it })
                if (onDelete != null) {
                    Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(VdvoemSizes.iconMd)
                        )
                        Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                        Text(stringResource(R.string.wishes_action_delete))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, note, price.toLongOrNull(), priority, url, surprise) },
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

/** Приоритет — три чипа: низкий, обычный, высокий. */
@Composable
private fun PriorityPicker(priority: Int, onPriorityChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)) {
        Text(
            text = stringResource(R.string.wishes_field_priority),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
        ) {
            PRIORITY_OPTIONS.forEach { value ->
                FilterChip(
                    selected = value == priority,
                    onClick = { onPriorityChange(value) },
                    label = { Text(stringResource(priorityLabelRes(value))) }
                )
            }
        }
    }
}

/** Сюрприз скрывает желание от партнёра: он увидит его только после вручения. */
@Composable
private fun SurpriseSwitch(surprise: Boolean, onSurpriseChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.wishes_field_surprise),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.wishes_field_surprise_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = surprise, onCheckedChange = onSurpriseChange)
    }
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
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
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
    WishSort.PRIORITY -> R.string.wishes_sort_priority
    WishSort.ALPHABET -> R.string.wishes_sort_alphabet
}

private fun WishSort.comparator(): Comparator<Wish> = when (this) {
    WishSort.NEWEST -> compareByDescending<Wish> { it.updatedAt }
    WishSort.PRICE_DESC -> compareByDescending<Wish> { it.priceEstimate ?: Long.MIN_VALUE }
    WishSort.PRIORITY -> compareByDescending<Wish> { it.safePriority }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
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

private const val TAG = "WishesScreen"

/** Порядок чипов в диалоге: от низкого к высокому. */
private val PRIORITY_OPTIONS = listOf(Wish.PRIORITY_LOW, Wish.PRIORITY_NORMAL, Wish.PRIORITY_HIGH)

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
                        status = WishStatus.WANTED,
                        priority = Wish.PRIORITY_HIGH,
                        url = "https://example.com/headphones",
                        surprise = true,
                        ownerDeviceId = "local-device"
                    ),
                    Wish(
                        id = "2",
                        pairId = "local",
                        title = "Поездка в Карелию",
                        note = "На майские праздники",
                        status = WishStatus.RESERVED,
                        priority = Wish.PRIORITY_LOW
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
            onAddWish = { _, _, _, _, _, _ -> },
            onUpdateWish = { _, _, _, _, _, _, _ -> },
            onSetStatus = { _, _ -> },
            onDeleteWish = {},
            onMakeGift = {},
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WishesScreenEmptyPreview() {
    VdvoemTheme {
        WishesScreen(
            state = WishesUiState(loading = false),
            filter = WishFilter.ALL,
            sort = WishSort.NEWEST,
            query = "",
            onFilterChange = {},
            onSortChange = {},
            onQueryChange = {},
            onResetQuery = {},
            onAddWish = { _, _, _, _, _, _ -> },
            onUpdateWish = { _, _, _, _, _, _, _ -> },
            onSetStatus = { _, _ -> },
            onDeleteWish = {},
            onMakeGift = {},
            modifier = Modifier
        )
    }
}