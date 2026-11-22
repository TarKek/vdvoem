package app.vdvoem.core.domain.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.junit.Test
import kotlin.test.assertEquals

class ModuleRegistryTest {

    private val registry = ModuleRegistry(
        listOf(
            module(id = "third", order = 30),
            module(id = "first", order = 10),
            module(id = "second", order = 20)
        )
    )

    @Test
    fun `ordered preserves repeated ids and their order`() {
        val orderIds = listOf("second", "first", "second", "missing")

        val ordered = registry.ordered(orderIds)

        assertEquals(listOf("second", "first", "second", "third"), ordered.map { it.manifest.id })
        assertEquals(ordered.map { it.manifest.id }, registry.effectiveOrder(orderIds))
    }

    private companion object {
        val icon: ImageVector = ImageVector.Builder(
            name = "test",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).build()

        fun module(id: String, order: Int): AppModule = object : AppModule {
            override val manifest = ModuleManifest(
                id = id,
                titleRes = 0,
                descriptionRes = 0,
                category = ModuleCategory.CORE,
                order = order
            )
            override val route: String = id
            override val icon: ImageVector = Companion.icon

            @Composable
            override fun Screen(modifier: Modifier) = Unit
        }
    }
}
