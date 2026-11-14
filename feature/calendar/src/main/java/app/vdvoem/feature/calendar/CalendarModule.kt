package app.vdvoem.feature.calendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import org.koin.androidx.compose.koinViewModel
import org.koin.dsl.module

class CalendarModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.calendar",
        titleRes = R.string.calendar_module_title,
        descriptionRes = R.string.calendar_module_description,
        category = ModuleCategory.TOGETHER,
        order = 20,
        defaultEnabled = true
    )

    override val route = "calendar"
    override val icon = Icons.Filled.DateRange

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: CalendarViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        CalendarScreen(
            state = state,
            onFilterChange = viewModel::setFilter,
            onSortChange = viewModel::setSort,
            onSaveEvent = viewModel::saveEvent,
            onDeleteEvent = viewModel::deleteEvent,
            modifier = modifier
        )
    }
}

val calendarFeatureModule = module {
    single { CalendarModule() }
}
