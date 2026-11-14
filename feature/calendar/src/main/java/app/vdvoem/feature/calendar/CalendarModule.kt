package app.vdvoem.feature.calendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import org.koin.core.module.Module
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
    override val icon: ImageVector = Icons.Filled.DateRange

    @Composable
    override fun Screen(modifier: Modifier) {
        CalendarScreen(modifier)
    }
}

val calendarFeatureModule: Module = module {
    single<AppModule> { CalendarModule() }
}
