package app.vdvoem.feature.gifts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

class GiftsModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.gifts",
        titleRes = R.string.gifts_module_title,
        descriptionRes = R.string.gifts_module_description,
        category = ModuleCategory.TOGETHER,
        order = 40,
        defaultEnabled = true
    )

    override val route = "gifts"
    override val icon: ImageVector = Icons.Filled.ShoppingCart

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: GiftsViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        GiftsScreen(
            state = state,
            onAdd = viewModel::add,
            onUpdate = viewModel::update,
            onSetStatus = viewModel::setStatus,
            onDelete = viewModel::delete,
            onQueryChange = viewModel::setQuery,
            onResetQuery = viewModel::resetQuery,
            onSortChange = viewModel::setSort,
            onSetBudget = viewModel::setBudget,
            modifier = modifier
        )
    }
}

val giftsFeatureModule: Module = module {
    single { GiftsModule() }
    single { GiftBudgetPreferences(context = androidContext()) }
}