package app.vdvoem.feature.calendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import app.vdvoem.core.domain.settings.ReminderLeadSettings
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

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
        // Напоминания подключаются здесь, чтобы модуль работал без правок сборки приложения.
        LaunchedEffect(viewModel) {
            val koin = KoinPlatform.getKoin()
            viewModel.bindReminders(
                scheduler = koin.get(),
                preferences = koin.get()
            )
        }
        val state by viewModel.state.collectAsStateWithLifecycle()
        CalendarScreen(
            state = state,
            onFilterChange = viewModel::setFilter,
            onSortChange = viewModel::setSort,
            onSaveEvent = viewModel::saveEvent,
            onDeleteEvent = viewModel::deleteEvent,
            onRemindersChange = viewModel::setRemindersEnabled,
            onLeadDaysChange = viewModel::setReminderLeadDays,
            onReminderHourChange = viewModel::setReminderHour,
            modifier = modifier
        )
    }
}

val calendarFeatureModule = module {
    single { CalendarModule() }
    single { ReminderPreferences(context = androidContext()) }
    // «Главная» просит только срок показа событий, не зная про этот модуль.
    single<ReminderLeadSettings> { get<ReminderPreferences>() }
    single { ReminderScheduler(context = androidContext()) }
    single {
        CalendarReminderObserver(
            repository = get(),
            preferences = get(),
            scheduler = get()
        )
    }
}