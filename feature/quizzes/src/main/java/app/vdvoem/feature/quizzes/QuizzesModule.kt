package app.vdvoem.feature.quizzes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class QuizzesModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.quizzes",
        titleRes = R.string.quizzes_module_title,
        descriptionRes = R.string.quizzes_module_description,
        category = ModuleCategory.FUN,
        order = 50,
        defaultEnabled = true
    )

    override val route = "quizzes"
    override val icon: ImageVector = Icons.Filled.Star

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: QuizzesViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        QuizzesScreen(
            state = state,
            onStart = viewModel::start,
            onSelectOption = viewModel::selectOption,
            onBack = viewModel::back,
            onNext = viewModel::next,
            onRetry = viewModel::retry,
            onClose = viewModel::close,
            modifier = modifier
        )
    }
}

val quizzesFeatureModule: Module = module {
    single { QuizResultStore(context = androidContext()) }
    single { QuizzesModule() }
    viewModel { QuizzesViewModel(resultsStore = get(), scoreRepository = get()) }
}