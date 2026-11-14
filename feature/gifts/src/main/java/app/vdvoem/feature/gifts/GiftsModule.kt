package app.vdvoem.feature.gifts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import org.koin.core.module.Module
import org.koin.dsl.module

class GiftsModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.gifts",
        titleRes = R.string.gifts_module_title,
        descriptionRes = R.string.gifts_module_description,
        category = ModuleCategory.TOGETHER,
        order = 40,
        defaultEnabled = true
    )

    override val route = "gifts"
    override val icon: ImageVector = Icons.Filled.ShoppingCart

    @Composable
    override fun Screen(modifier: Modifier) {
        GiftsScreen(modifier)
    }
}

val giftsFeatureModule: Module = module {
    single<AppModule> { GiftsModule() }
}
