package app.vdvoem.feature.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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

/** Общие дела пары: синхронизируются между устройствами, поэтому scope SHARED. */
class TasksModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.tasks",
        titleRes = R.string.tasks_module_title,
        descriptionRes = R.string.tasks_module_description,
        category = ModuleCategory.TOGETHER,
        order = 65,
        defaultEnabled = true,
        scope = ModuleScope.SHARED
    )

    override val route = "tasks"
    override val icon: ImageVector = Icons.Filled.CheckCircle

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: TasksViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        TasksScreen(
            state = state,
            onAdd = viewModel::addTask,
            onEdit = viewModel::edit,
            onToggle = viewModel::toggleCompleted,
            onDismiss = viewModel::dismiss,
            onUpdate = viewModel::update,
            onSave = viewModel::save,
            onDeleteRequest = viewModel::requestDelete,
            onDeleteConfirm = viewModel::confirmDelete,
            onDeleteCancel = viewModel::cancelDelete,
            onDismissError = viewModel::dismissError,
            modifier = modifier
        )
    }
}

val tasksFeatureModule: Module = module {
    single { TasksModule() }
    viewModel {
        TasksViewModel(
            repository = get(),
            pairRepository = get(),
            deviceIdentity = get()
        )
    }
}