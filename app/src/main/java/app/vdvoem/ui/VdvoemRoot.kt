package app.vdvoem.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.vdvoem.R
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.data.HomePreferences
import app.vdvoem.feature.store.StoreModule
import app.vdvoem.feature.store.StoreScreen
import app.vdvoem.ui.dashboard.DashboardScreen
import app.vdvoem.ui.dashboard.DashboardViewModel
import app.vdvoem.ui.sync.BackupViewModel
import app.vdvoem.ui.sync.ImportResult
import kotlinx.coroutines.launch
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
    dashboardViewModel: DashboardViewModel = koinViewModel(),
    backupViewModel: BackupViewModel = koinViewModel()
) {
    val enabledIds by viewModel.enabledIds.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.state.collectAsStateWithLifecycle()
    val homePreferences: HomePreferences = koinInject()
    val widgetIds by homePreferences.widgetIds
        .collectAsStateWithLifecycle(initialValue = HomePreferences.DEFAULT_WIDGETS)
    val lastPoke by homePreferences.lastPoke
        .collectAsStateWithLifecycle(initialValue = null)
    val navController = rememberNavController()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = backupViewModel.buildExport()
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray())
                } ?: error("output stream is null")
            }.isSuccess
            snackbarHostState.showSnackbar(
                context.getString(
                    if (saved) R.string.backup_export_done else R.string.backup_failed
                )
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            }.getOrNull()
            val result = if (text.isNullOrBlank()) {
                ImportResult(ok = false)
            } else {
                backupViewModel.import(text)
            }
            snackbarHostState.showSnackbar(
                if (result.ok) {
                    context.getString(R.string.backup_import_done, result.imported)
                } else {
                    context.getString(R.string.backup_failed)
                }
            )
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.action_more)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.backup_export)) },
                            onClick = {
                                menuExpanded = false
                                exportLauncher.launch("vdvoem-backup.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.backup_import)) },
                            onClick = {
                                menuExpanded = false
                                importLauncher.launch(arrayOf("*/*"))
                            }
                        )
                    }
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
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.title_home)) },
                    label = { Text(stringResource(R.string.title_home), maxLines = 1) },
                    alwaysShowLabel = false
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
                        icon = { Icon(module.icon, contentDescription = stringResource(module.manifest.titleRes)) },
                        label = {
                            Text(stringResource(module.manifest.titleRes), maxLines = 1)
                        },
                        alwaysShowLabel = false
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
                    eventCount = dashboardState.eventCount,
                    wishCount = dashboardState.wishCount,
                    giftCount = dashboardState.giftCount,
                    onOpenModule = { module -> navController.navigate(module.route) },
                    onOpenStore = { navController.navigate(Routes.STORE) },
                    onOpenWishes = { navController.navigate("wishes") },
                    onOpenQuizzes = { navController.navigate("quizzes") },
                    widgetIds = widgetIds,
                    lastPoke = lastPoke,
                    onWidgetIdsChange = { ids ->
                        scope.launch { homePreferences.setWidgetIds(ids) }
                    },
                    onSendPoke = { text ->
                        scope.launch {
                            homePreferences.saveLastPoke(text)
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.dashboard_poke_sent)
                            )
                        }
                    },
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
