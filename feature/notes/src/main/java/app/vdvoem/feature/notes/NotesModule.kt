package app.vdvoem.feature.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import app.vdvoem.core.domain.module.ModuleScope
import app.vdvoem.feature.notes.data.NotesDatabase
import app.vdvoem.feature.notes.data.NotesRepository
import app.vdvoem.feature.notes.data.RoomNotesRepository
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Локальный плагин «Личные заметки».
 *
 * [ModuleScope.LOCAL] — данные не синхронизируются и не уходят на сервер:
 * заметки лежат в отдельной Room-базе внутри модуля.
 */
class NotesModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.notes",
        titleRes = R.string.notes_module_title,
        descriptionRes = R.string.notes_module_description,
        category = ModuleCategory.PERSONAL,
        order = 70,
        defaultEnabled = true,
        scope = ModuleScope.LOCAL
    )

    override val route = "notes"
    override val icon: ImageVector = Icons.Filled.Edit

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: NotesViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        NotesScreen(
            state = state,
            onAdd = viewModel::addNote,
            onEdit = viewModel::edit,
            onDismiss = viewModel::dismiss,
            onUpdate = viewModel::update,
            onSave = viewModel::save,
            onDeleteRequest = viewModel::requestDelete,
            onDeleteConfirm = viewModel::confirmDelete,
            onDeleteCancel = viewModel::cancelDelete,
            modifier = modifier
        )
    }
}

val notesFeatureModule: Module = module {
    single { NotesModule() }
    single {
        Room.databaseBuilder(
            androidContext(),
            NotesDatabase::class.java,
            NotesDatabase.NAME
        ).build()
    }
    single<NotesRepository> { RoomNotesRepository(dao = get<NotesDatabase>().noteDao()) }
    viewModel { NotesViewModel(repository = get()) }
}