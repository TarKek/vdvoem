package app.vdvoem.feature.dates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme

/**
 * Идеи свиданий: фильтр по бюджету, случайная идея по кнопке и полный список под ним.
 *
 * Коллекция статичная и локальная: экран ничего не сохраняет и никуда не отправляет.
 */
@Composable
fun DatesScreen(
    state: DatesUiState,
    onSelectBudget: (Budget?) -> Unit,
    onShowIdea: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = VdvoemSpacing.screenHorizontal,
            end = VdvoemSpacing.screenHorizontal,
            top = VdvoemSpacing.sm,
            bottom = VdvoemSpacing.listBottom
        ),
        verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
    ) {
        item(key = "hero") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dates_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(VdvoemSpacing.xs))
                Text(
                    text = stringResource(R.string.dates_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "filters") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm),
                contentPadding = PaddingValues(vertical = VdvoemSpacing.xs)
            ) {
                item(key = "filter-all") {
                    FilterChip(
                        selected = state.budget == null,
                        onClick = { onSelectBudget(null) },
                        label = { Text(stringResource(R.string.dates_budget_all)) }
                    )
                }
                items(Budget.entries.toList(), key = { it.name }) { budget ->
                    FilterChip(
                        selected = state.budget == budget,
                        onClick = { onSelectBudget(budget) },
                        label = { Text(stringResource(budget.labelRes)) }
                    )
                }
            }
        }

        item(key = "action") {
            Button(
                onClick = onShowIdea,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VdvoemSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.width(VdvoemSizes.iconMd)
                )
                Spacer(Modifier.width(VdvoemSpacing.sm))
                Text(
                    text = stringResource(R.string.dates_show_idea),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        item(key = "picked") {
            val idea = state.randomIdea
            if (idea == null) {
                PromptCard()
            } else {
                IdeaHighlightCard(idea = idea, modifier = Modifier.padding(top = VdvoemSpacing.sm))
            }
        }

        item(key = "list-header") {
            SectionHeader(
                title = stringResource(R.string.dates_all_ideas, state.ideas.size),
                modifier = Modifier.padding(top = VdvoemSpacing.lg, bottom = VdvoemSpacing.xxs)
            )
        }

        items(state.ideas, key = { it.id }) { idea ->
            IdeaCard(idea = idea)
        }
    }
}

@Composable
private fun PromptCard() {
    VdvoemCard(
        modifier = Modifier.padding(top = VdvoemSpacing.sm),
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "💡",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.width(VdvoemSpacing.md))
            Column {
                Text(
                    text = stringResource(R.string.dates_prompt_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(VdvoemSpacing.xxs))
                Text(
                    text = stringResource(R.string.dates_prompt_text),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun IdeaHighlightCard(idea: DateIdea, modifier: Modifier = Modifier) {
    VdvoemCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = idea.emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.width(VdvoemSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(idea.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(VdvoemSpacing.xs))
                Text(
                    text = stringResource(idea.textRes),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(VdvoemSpacing.md))
                InfoPill(text = stringResource(idea.budget.labelRes))
            }
        }
    }
}

@Composable
private fun IdeaCard(idea: DateIdea) {
    VdvoemCard {
        Row(verticalAlignment = Alignment.Top) {
            Text(text = idea.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(VdvoemSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(idea.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(VdvoemSpacing.xxs))
                Text(
                    text = stringResource(idea.textRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(VdvoemSpacing.sm))
                InfoPill(text = stringResource(idea.budget.labelRes))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DatesScreenPreview() {
    VdvoemTheme {
        DatesScreen(
            state = DatesUiState(
                budget = Budget.CHEAP,
                randomIdea = DateIdeas.all.first { it.id == "picnic" },
                shownCount = 1
            ),
            onSelectBudget = {},
            onShowIdea = {}
        )
    }
}