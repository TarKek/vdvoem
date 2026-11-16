package app.vdvoem.feature.dates

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import app.vdvoem.core.domain.module.ModuleScope
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class DatesModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.dates",
        titleRes = R.string.dates_module_title,
        descriptionRes = R.string.dates_module_description,
        category = ModuleCategory.FUN,
        order = 65,
        defaultEnabled = true,
        scope = ModuleScope.LOCAL
    )

    override val route = "dates"
    override val icon: ImageVector = Icons.Filled.Favorite

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: DatesViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        DatesScreen(
            state = state,
            onSelectBudget = viewModel::selectBudget,
            onShowIdea = viewModel::showIdea,
            modifier = modifier
        )
    }
}

val datesFeatureModule: Module = module {
    single { DatesModule() }
    viewModel { DatesViewModel() }
}