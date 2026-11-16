package app.vdvoem.feature.cycle

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

/**
 * Модуль «Цикл»: трекер менструального цикла для пары.
 *
 * Данные общие ([ModuleScope.SHARED]): оба партнёра видят цикл, симптомы и прогноз.
 */
class CycleModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.cycle",
        titleRes = R.string.cycle_module_title,
        descriptionRes = R.string.cycle_module_description,
        category = ModuleCategory.TOGETHER,
        order = 45,
        defaultEnabled = true,
        scope = ModuleScope.SHARED
    )

    override val route = "cycle"

    override val icon: ImageVector = Icons.Filled.Favorite

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: CycleViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        CycleScreen(
            state = state,
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onOpenPeriodSheet = viewModel::openPeriodSheet,
            onOpenSymptoms = viewModel::openSymptoms,
            onOpenDay = viewModel::openDay,
            onStartPeriodToday = viewModel::startPeriodToday,
            onEndPeriodToday = viewModel::endPeriodToday,
            onStartPeriodAt = viewModel::startPeriodAt,
            onDismissSheet = viewModel::dismissSheet,
            onUpdateDraft = viewModel::updateDraft,
            onSaveDraft = viewModel::saveDraft,
            onClearDay = viewModel::clearDay,
            modifier = modifier
        )
    }
}

val cycleFeatureModule: Module = module {
    single { CycleModule() }
    viewModel { CycleViewModel(repository = get()) }
}