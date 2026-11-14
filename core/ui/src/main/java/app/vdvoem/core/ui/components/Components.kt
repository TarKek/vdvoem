package app.vdvoem.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme

/** Заголовок секции с опциональным действием справа. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Базовая карточка бренда: скруглённая, без тени, с одинаковыми внутренними отступами. */
@Composable
fun VdvoemCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(VdvoemSpacing.xl), content = content)
    }
}

/** Пустое состояние: иконка в круге, заголовок, пояснение и опциональная кнопка. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = VdvoemSpacing.xxxl, horizontal = VdvoemSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(VdvoemSpacing.huge + VdvoemSpacing.xxl)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(VdvoemSpacing.lg)
            )
        }
        Spacer(modifier = Modifier.height(VdvoemSpacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(VdvoemSpacing.xl))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

/** Маленький «бейдж» с текстом и необязательной иконкой. */
@Composable
fun InfoPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = VdvoemSpacing.md, vertical = VdvoemSpacing.xs + VdvoemSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs + VdvoemSpacing.xxs)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconSm)
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F8)
@Composable
private fun InfoPillPreview() {
    VdvoemTheme {
        Column {
            SectionHeader(title = "Ближайшее")
            InfoPill(text = "Через 12 дней", icon = Icons.Filled.Favorite)
            EmptyState(
                icon = Icons.Filled.Favorite,
                title = "Пока пусто",
                message = "Добавьте первое событие, чтобы начать отсчёт",
                actionLabel = "Добавить"
            )
        }
    }
}
