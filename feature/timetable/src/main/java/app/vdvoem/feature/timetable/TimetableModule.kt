package app.vdvoem.feature.timetable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class TimetableModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.timetable",
        titleRes = R.string.timetable_module_title,
        descriptionRes = R.string.timetable_module_description,
        category = ModuleCategory.FUN,
        order = 55,
        defaultEnabled = true
    )

    override val route = "timetable"
    override val icon: ImageVector = Icons.Filled.DateRange

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: TimetableViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        TimetableScreen(
            state = state,
            onAdd = { viewModel.addLesson() },
            onEdit = viewModel::edit,
            onDismiss = viewModel::dismiss,
            onUpdate = viewModel::update,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onSelectDate = viewModel::selectDate,
            modifier = modifier
        )
    }
}

val timetableFeatureModule: Module = module {
    single { TimetableModule() }
    viewModel { TimetableViewModel(repository = get(), pairRepository = get()) }
}