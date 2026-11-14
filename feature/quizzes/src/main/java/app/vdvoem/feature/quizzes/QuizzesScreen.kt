package app.vdvoem.feature.quizzes

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Один экран на три состояния: список тестов, прохождение и результат.
 *
 * Навигации внутри модуля нет — состоянием управляет [QuizzesViewModel].
 */
@Composable
fun QuizzesScreen(
    state: QuizzesUiState,
    onStart: (String) -> Unit,
    onSelectOption: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quiz = state.activeQuiz
    val outcome = state.outcome
    when {
        quiz == null -> QuizList(
            results = state.results,
            onStart = onStart,
            modifier = modifier
        )

        outcome != null -> QuizResultView(
            outcome = outcome,
            onRetry = onRetry,
            onClose = onClose,
            modifier = modifier
        )

        else -> QuizQuestionView(
            quiz = quiz,
            questionIndex = state.questionIndex,
            answers = state.answers,
            onSelectOption = onSelectOption,
            onBack = onBack,
            onNext = onNext,
            modifier = modifier
        )
    }
}

@Composable
private fun QuizList(
    results: Map<String, QuizResult>,
    onStart: (String) -> Unit,
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
            QuizzesHero()
        }

        item(key = "header") {
            SectionHeader(
                title = stringResource(R.string.quizzes_section_list),
                modifier = Modifier.padding(top = VdvoemSpacing.xs)
            )
        }

        items(
            count = QuizCatalog.quizzes.size,
            key = { index -> QuizCatalog.quizzes[index].id }
        ) { index ->
            val quiz = QuizCatalog.quizzes[index]
            QuizCard(
                quiz = quiz,
                result = results[quiz.id],
                onStart = { onStart(quiz.id) }
            )
        }
    }
}

@Composable
private fun QuizzesHero() {
    VdvoemCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(VdvoemSizes.iconLg)
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
        Text(
            text = stringResource(R.string.quizzes_module_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
        Text(
            text = stringResource(R.string.quizzes_hero_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun QuizCard(
    quiz: Quiz,
    result: QuizResult?,
    onStart: () -> Unit
) {
    VdvoemCard(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onStart)
    ) {
        Text(
            text = quiz.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.xxs))
        Text(
            text = quiz.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)) {
            InfoPill(
                text = pluralStringResource(
                    R.plurals.quizzes_questions_count,
                    quiz.questions.size,
                    quiz.questions.size
                )
            )
            InfoPill(
                text = if (result != null) {
                    stringResource(
                        R.string.quizzes_passed_on,
                        formatPassedAt(result.passedAt),
                        result.percent
                    )
                } else {
                    stringResource(R.string.quizzes_not_passed)
                }
            )
        }
        Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
        TextButton(onClick = onStart) {
            Text(stringResource(R.string.quizzes_action_start))
        }
    }
}

@Composable
private fun QuizQuestionView(
    quiz: Quiz,
    questionIndex: Int,
    answers: List<Int>,
    onSelectOption: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = quiz.questions.getOrNull(questionIndex) ?: return
    val selected = answers.getOrNull(questionIndex) ?: -1
    val isLast = questionIndex == quiz.questions.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = VdvoemSpacing.screenHorizontal,
                end = VdvoemSpacing.screenHorizontal,
                top = VdvoemSpacing.lg,
                bottom = VdvoemSpacing.listBottom
            ),
        verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
    ) {
        Text(
            text = quiz.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(
                R.string.quizzes_question_progress,
                questionIndex + 1,
                quiz.questions.size
            ),
            style = MaterialTheme.typography.labelLarge
        )
        LinearProgressIndicator(
            progress = { (questionIndex + 1).toFloat() / quiz.questions.size },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = question.text,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = VdvoemSpacing.sm)
        )

        question.options.forEachIndexed { index, option ->
            QuizOptionRow(
                text = option.text,
                selected = index == selected,
                onClick = { onSelectOption(index) }
            )
        }

        Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.quizzes_action_prev))
            }
            Button(
                onClick = onNext,
                enabled = selected >= 0,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(
                        if (isLast) R.string.quizzes_action_finish else R.string.quizzes_action_next
                    )
                )
            }
        }
    }
}

@Composable
private fun QuizOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    VdvoemCard(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            ),
        containerColor = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected,
                onClick = null
            )
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuizResultView(
    outcome: QuizOutcome,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = VdvoemSpacing.screenHorizontal,
                end = VdvoemSpacing.screenHorizontal,
                top = VdvoemSpacing.lg,
                bottom = VdvoemSpacing.listBottom
            ),
        verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = outcome.quiz.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        VdvoemCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.quizzes_result_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
                Text(
                    text = stringResource(R.string.quizzes_result_percent, outcome.percent),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(
                        R.string.quizzes_result_score,
                        outcome.score,
                        outcome.maxScore
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        VdvoemCard {
            Text(
                text = outcome.interpretation,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Text(
            text = stringResource(R.string.quizzes_result_saved),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.quizzes_action_restart))
        }
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.quizzes_action_back_to_list))
        }
    }
}

private val passedAtFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))

private fun formatPassedAt(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(passedAtFormatter)

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8)
@Composable
private fun QuizListPreview() {
    VdvoemTheme {
        QuizList(
            results = mapOf(
                QuizCatalog.COMPATIBILITY_ID to QuizResult(
                    quizId = QuizCatalog.COMPATIBILITY_ID,
                    percent = 78,
                    passedAt = System.currentTimeMillis()
                )
            ),
            onStart = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8)
@Composable
private fun QuizQuestionPreview() {
    VdvoemTheme {
        val quiz = QuizCatalog.quizzes[1]
        QuizQuestionView(
            quiz = quiz,
            questionIndex = 2,
            answers = listOf(0, 1),
            onSelectOption = {},
            onBack = {},
            onNext = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8)
@Composable
private fun QuizResultPreview() {
    VdvoemTheme {
        val quiz = QuizCatalog.quizzes.first()
        val percent = 78
        QuizResultView(
            outcome = QuizOutcome(
                quiz = quiz,
                score = quiz.maxScore * percent / 100,
                maxScore = quiz.maxScore,
                percent = percent,
                interpretation = quiz.interpretation(percent),
                passedAt = System.currentTimeMillis()
            ),
            onRetry = {},
            onClose = {}
        )
    }
}