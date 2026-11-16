package app.vdvoem.feature.my

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.module.ModuleCategory
import app.vdvoem.core.domain.module.ModuleManifest
import app.vdvoem.core.domain.module.ModuleScope
import app.vdvoem.core.ui.theme.VdvoemTheme
import app.vdvoem.core.ui.theme.spacing
import org.koin.core.module.Module
import org.koin.dsl.module

class MyModule : AppModule {

    override val manifest = ModuleManifest(
        id = "core.my",
        titleRes = R.string.my_module_title,
        descriptionRes = R.string.my_module_description,
        category = ModuleCategory.FUN,
        order = 75,
        defaultEnabled = true,
        // SHARED — данные пары (синкаются), LOCAL — только это устройство.
        scope = ModuleScope.LOCAL
    )

    override val route = "my"
    override val icon: ImageVector = Icons.Filled.Star

    @Composable
    override fun Screen(modifier: Modifier) {
        MyScreen(modifier = modifier)
    }
}

@Composable
fun MyScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.my_module_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.my_module_description),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyScreenPreview() {
    VdvoemTheme {
        MyScreen()
    }
}

val myFeatureModule: Module = module {
    single { MyModule() }
    // viewModel { MyViewModel(repository = get()) }
}