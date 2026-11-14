package app.vdvoem.feature.wishes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSpacing

@Composable
fun WishesScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = VdvoemSpacing.screenHorizontal,
            end = VdvoemSpacing.screenHorizontal,
            top = VdvoemSpacing.lg,
            bottom = VdvoemSpacing.listBottom
        ),
        verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.lg)
    ) {
        item {
            VdvoemCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                Text(
                    text = stringResource(R.string.wishes_hero_title),
                    style = MaterialTheme.typography.titleMedium,
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

        item {
            EmptyState(
                icon = Icons.Filled.Favorite,
                title = stringResource(R.string.wishes_empty_title),
                message = stringResource(R.string.wishes_empty_message)
            )
        }
    }
}
