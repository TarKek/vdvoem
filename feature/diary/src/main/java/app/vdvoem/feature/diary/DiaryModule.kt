package app.vdvoem.feature.diary

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Общий дневник пары: записи синхронизируются с партнёром, напоминания приходят локально. */
class DiaryModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.diary",
        titleRes = R.string.diary_module_title,
        descriptionRes = R.string.diary_module_description,
        category = ModuleCategory.TOGETHER,
        order = 35,
        defaultEnabled = true
    )

    override val route = "diary"
    override val icon = Icons.Filled.Edit

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: DiaryViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        DiaryScreen(
            state = state,
            onSave = viewModel::saveEntry,
            onDelete = viewModel::deleteEntry,
            onSaveReminder = viewModel::saveReminder,
            onDeleteReminder = viewModel::deleteReminder,
            onReminderDone = viewModel::setReminderDone,
            modifier = modifier
        )
    }
}

val diaryFeatureModule: Module = module {
    single { DiaryModule() }
    viewModel {
        DiaryViewModel(
            diaryRepository = get(),
            diaryReminderRepository = get(),
            pairRepository = get()
        )
    }
    // Уведомления: планировщик и подписка на напоминания живут вне экрана.
    single { DiaryReminderScheduler(context = androidContext()) }
    single { DiaryReminderObserver(repository = get(), scheduler = get()) }
}