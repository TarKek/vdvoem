package app.vdvoem.ui.presents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vdvoem.R
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.feature.gifts.GiftsModule
import app.vdvoem.feature.wishes.WishesModule
import org.koin.compose.koinInject

/**
 * Один модуль на две родственные вещи: желания и подарки.
 * Так в нижней панели не появляется две почти одинаковые вкладки.
 */
class PresentsModule : AppModule {

    override val manifest = ModuleManifest(
        id = ID,
        titleRes = R.string.presents_title,
        descriptionRes = R.string.presents_description,
        shortTitleRes = R.string.presents_short_title,
        category = ModuleCategory.TOGETHER,
        order = 30,
        defaultEnabled = true
    )

    override val route: String = ROUTE

    override val icon: ImageVector = Icons.Filled.Favorite

    @Composable
    override fun Screen(modifier: Modifier) {
        PresentsScreen(modifier = modifier)
    }

    companion object {
        const val ID = "core.presents"
        const val ROUTE = "presents"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentsScreen(
    modifier: Modifier = Modifier,
    tabController: PresentsTabController = koinInject()
) {
    val selected by tabController.tab.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = VdvoemSpacing.screenHorizontal,
                    vertical = VdvoemSpacing.sm
                )
        ) {
            val titles = listOf(
                stringResource(R.string.presents_tab_wishes),
                stringResource(R.string.presents_tab_gifts)
            )
            titles.forEachIndexed { index, title ->
                SegmentedButton(
                    selected = selected == index,
                    onClick = { tabController.select(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = titles.size),
                    icon = {}
                ) {
                    Text(title)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            when (selected) {
                0 -> WishesModule().Screen(Modifier.fillMaxSize())
                else -> GiftsModule().Screen(Modifier.fillMaxSize())
            }
        }
    }
}