package app.vdvoem.feature.wishes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import org.koin.core.module.Module
import org.koin.dsl.module

class WishesModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.wishes",
        titleRes = R.string.wishes_module_title,
        descriptionRes = R.string.wishes_module_description,
        category = ModuleCategory.TOGETHER,
        order = 30,
        defaultEnabled = true
    )

    override val route = "wishes"
    override val icon: ImageVector = Icons.Filled.Favorite

    @Composable
    override fun Screen(modifier: Modifier) {
        WishesScreen(modifier)
    }
}

val wishesFeatureModule: Module = module {
    single { WishesModule() }
}
