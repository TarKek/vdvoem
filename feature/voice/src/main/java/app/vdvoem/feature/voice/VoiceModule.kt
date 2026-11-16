package app.vdvoem.feature.voice

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import app.vdvoem.core.domain.module.ModuleScope
import app.vdvoem.feature.voice.audio.VoiceNotePlayer
import app.vdvoem.feature.voice.audio.VoiceNoteRecorder
import app.vdvoem.feature.voice.data.VoiceNoteStore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Модуль голосовых заметок.
 *
 * [ModuleScope.LOCAL]: аудио пишется на телефон и не синхронизируется с партнёром,
 * поэтому заметки живут только в приватном хранилище устройства.
 */
class VoiceModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.voice",
        titleRes = R.string.voice_module_title,
        descriptionRes = R.string.voice_module_description,
        category = ModuleCategory.PERSONAL,
        order = 75,
        defaultEnabled = true,
        scope = ModuleScope.LOCAL
    )

    override val route = "voice"
    override val icon: ImageVector = Icons.Filled.PlayArrow

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: VoiceViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        VoiceScreen(
            state = state,
            onStartRecording = viewModel::startRecording,
            onStopRecording = viewModel::stopRecording,
            onTogglePlay = viewModel::togglePlay,
            onDeleteRequest = viewModel::requestDelete,
            onDeleteConfirm = viewModel::confirmDelete,
            onDeleteCancel = viewModel::cancelDelete,
            onDismissError = viewModel::dismissError,
            modifier = modifier
        )
    }
}

val voiceFeatureModule: Module = module {
    single { VoiceModule() }
    single { VoiceNoteStore(androidContext()) }
    single { VoiceNoteRecorder(androidContext()) }
    single { VoiceNotePlayer() }
    viewModel { VoiceViewModel(store = get(), recorder = get(), player = get()) }
}