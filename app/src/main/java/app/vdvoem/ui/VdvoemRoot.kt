package app.vdvoem.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.vdvoem.R
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.feature.store.StoreModule
import app.vdvoem.feature.store.StoreScreen
import app.vdvoem.ui.dashboard.DashboardScreen
import app.vdvoem.ui.dashboard.DashboardViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private object Routes {
    const val HOME = "home"
    const val STORE = "store"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VdvoemRoot(
    registry: ModuleRegistry = koinInject(),
    viewModel: ModulesViewModel = koinViewModel(),
    dashboardViewModel: DashboardViewModel = koinViewModel()
) {
    val enabledIds by viewModel.enabledIds.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    val enabledModules = remember(enabledIds, registry) {
        registry.modules.filter {
            it.manifest.id in enabledIds &&
                it.manifest.id != StoreModule.ID &&
                !it.manifest.comingSoon
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val title = when (currentRoute) {
        Routes.STORE -> stringResource(R.string.title_store)
        Routes.HOME, null -> stringResource(R.string.title_home)
        else -> registry.byRoute(currentRoute)
            ?.let { stringResource(it.manifest.titleRes) }
            ?: stringResource(R.string.app_name)
    }

    LaunchedEffect(enabledIds) {
        val route = navController.currentBackStackEntry?.destination?.route
        val module = registry.byRoute(route)
        if (module != null && module.manifest.id !in enabledIds) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.HOME) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    if (currentRoute == Routes.STORE) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute != Routes.STORE) {
                        IconButton(onClick = { navController.navigate(Routes.STORE) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = stringResource(R.string.action_store)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.HOME,
                    onClick = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.title_home), maxLines = 1) }
                )
                enabledModules.forEach { module ->
                    NavigationBarItem(
                        selected = currentRoute == module.route,
                        onClick = {
                            navController.navigate(module.route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(module.icon, contentDescription = null) },
                        label = {
                            Text(stringResource(module.manifest.titleRes), maxLines = 1)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                DashboardScreen(
                    modules = enabledModules,
                    pair = dashboardState.pair,
                    nextEvent = dashboardState.nextEvent,
                    onOpenModule = { module -> navController.navigate(module.route) },
                    onOpenStore = { navController.navigate(Routes.STORE) },
                    onSavePair = dashboardViewModel::savePair,
                    onOpenCalendar = { navController.navigate("calendar") }
                )
            }
            composable(Routes.STORE) {
                StoreScreen(
                    registry = registry,
                    enabledIds = enabledIds,
                    onToggle = viewModel::setEnabled
                )
            }
            enabledModules.forEach { module ->
                composable(module.route) {
                    module.Screen(Modifier.fillMaxSize())
                }
            }
        }
    }
}
