package app.vdvoem.feature.wishes

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
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

class WishesModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.wishes",
        titleRes = R.string.wishes_module_title,
        descriptionRes = R.string.wishes_module_description,
        category = ModuleCategory.TOGETHER,
        order = 30,
        defaultEnabled = true
    )

    override val route = "wishes"
    override val icon: ImageVector = Icons.Filled.Favorite

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: WishesViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val filter by viewModel.filter.collectAsStateWithLifecycle()
        val sort by viewModel.sort.collectAsStateWithLifecycle()
        val query by viewModel.query.collectAsStateWithLifecycle()
        WishesScreen(
            state = state,
            filter = filter,
            sort = sort,
            query = query,
            onFilterChange = viewModel::setFilter,
            onSortChange = viewModel::setSort,
            onQueryChange = viewModel::setQuery,
            onResetQuery = viewModel::resetQuery,
            onAddWish = viewModel::add,
            onUpdateWish = viewModel::update,
            onSetStatus = viewModel::setStatus,
            onDeleteWish = viewModel::delete,
            onMakeGift = viewModel::makeGift,
            modifier = modifier
        )
    }
}

val wishesFeatureModule: Module = module {
    single { WishesModule() }
}