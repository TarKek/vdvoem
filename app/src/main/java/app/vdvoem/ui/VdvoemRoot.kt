package app.vdvoem.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.vdvoem.R
import app.vdvoem.core.domain.model.Poke
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.core.domain.repository.PokeRepository
import app.vdvoem.core.sync.SyncController
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.data.HomePreferences
import app.vdvoem.feature.store.StoreModule
import app.vdvoem.feature.store.StoreScreen
import app.vdvoem.ui.dashboard.DashboardScreen
import app.vdvoem.ui.dashboard.DashboardViewModel
import app.vdvoem.ui.dashboard.PairDialog
import app.vdvoem.ui.more.MoreScreen
import app.vdvoem.ui.pair.ConnectPairScreen
import app.vdvoem.ui.pair.ConnectPairViewModel
import app.vdvoem.ui.presents.PresentsModule
import app.vdvoem.ui.presents.PresentsTabController
import app.vdvoem.ui.sync.BackupViewModel
import app.vdvoem.ui.sync.ImportResult
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.UUID

private object Routes {
    const val HOME = "home"
    const val STORE = "store"
    const val MORE = "more"
    const val CONNECT = "connect"
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
    val pokeRepository: PokeRepository = koinInject()
    val syncController: SyncController = koinInject()
    val presentsTabController: PresentsTabController = koinInject()
    val widgets by homePreferences.widgets
        .collectAsStateWithLifecycle(initialValue = HomePreferences.DEFAULT_WIDGETS)
    val lastPoke = dashboardState.lastPoke?.text
    val navController = rememberNavController()

    // Каждое возвращение в приложение — повод сходить на сервер: без этого изменения
    // партнёра ждали до ближайшего фонового прохода (15 минут) и казалось, что синка нет.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) syncController.syncInBackground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pairDialogOpen by remember { mutableStateOf(false) }

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
    // Material 3: в нижней навигации не больше пяти пунктов — поэтому «Ещё».
    val bottomModules = remember(enabledModules) { enabledModules.take(3) }
    val overflowModules = remember(enabledModules) { enabledModules.drop(3) }
    val enabledRoutes = remember(enabledModules) { enabledModules.map { it.route }.toSet() }

    /**
     * Единый переход к вкладке/модулю: не новый уровень, а переключение вкладки.
     * Модуль выключен — ведём в магазин дополнений, чтобы его можно было включить.
     */
    fun openTab(route: String) {
        val target = when {
            route == Routes.HOME || route == Routes.MORE || route == Routes.STORE -> route
            route in enabledRoutes -> route
            else -> Routes.STORE
        }
        navController.navigate(target) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val title = when (currentRoute) {
        Routes.STORE -> stringResource(R.string.title_store)
        Routes.MORE -> stringResource(R.string.more_title)
        Routes.CONNECT -> stringResource(R.string.connect_title)
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
                    when (currentRoute) {
                        Routes.STORE, Routes.CONNECT -> {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        }
                        Routes.HOME -> {
                            val pairAction = stringResource(R.string.pair_account_action)
                            val initials = listOfNotNull(
                                dashboardState.pair?.nameA?.firstOrNull(),
                                dashboardState.pair?.nameB?.firstOrNull()
                            ).joinToString("").uppercase().ifEmpty { "?" }
                            IconButton(
                                onClick = { pairDialogOpen = true },
                                modifier = Modifier.semantics { contentDescription = pairAction }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(
                                        width = VdvoemSizes.borderThin,
                                        color = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.size(VdvoemSizes.avatarAccount)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = initials.take(2),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.HOME,
                    onClick = { openTab(Routes.HOME) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.title_home)) },
                    label = { Text(stringResource(R.string.title_home), maxLines = 1) },
                    alwaysShowLabel = true
                )
                bottomModules.forEach { module ->
                    val selected = currentRoute == module.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { openTab(module.route) },
                        icon = { Icon(module.icon, contentDescription = stringResource(module.manifest.titleRes)) },
                        label = {
                            val labelRes = module.manifest.shortTitleRes
                                .takeIf { it != 0 } ?: module.manifest.titleRes
                            Text(stringResource(labelRes), maxLines = 1)
                        },
                        alwaysShowLabel = true
                    )
                }
                val moreSelected = currentRoute == Routes.MORE ||
                    overflowModules.any { it.route == currentRoute }
                NavigationBarItem(
                    selected = moreSelected,
                    onClick = { openTab(Routes.MORE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.more_title)
                        )
                    },
                    label = { Text(stringResource(R.string.more_title), maxLines = 1) },
                    alwaysShowLabel = true
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(180)) },
            exitTransition = { fadeOut(animationSpec = tween(120)) }
        ) {
            composable(Routes.HOME) {
                DashboardScreen(
                    modules = enabledModules,
                    pair = dashboardState.pair,
                    events = dashboardState.events,
                    eventCount = dashboardState.eventCount,
                    wishCount = dashboardState.wishCount,
                    giftCount = dashboardState.giftCount,
                    onOpenModule = { module -> openTab(module.route) },
                    onOpenStore = { openTab(Routes.STORE) },
                    onOpenWishes = {
                        presentsTabController.select(PresentsTabController.TAB_WISHES)
                        openTab(PresentsModule.ROUTE)
                    },
                    onOpenGifts = {
                        presentsTabController.select(PresentsTabController.TAB_GIFTS)
                        openTab(PresentsModule.ROUTE)
                    },
                    onOpenQuizzes = { openTab("quizzes") },
                    onOpenPairEditor = { pairDialogOpen = true },
                    widgets = widgets,
                    lastPoke = lastPoke,
                    onWidgetsChange = { list ->
                        scope.launch { homePreferences.setWidgets(list) }
                    },
                    onAddWidget = { type ->
                        scope.launch { homePreferences.addWidget(type) }
                    },
                    onSendPoke = { text ->
                        scope.launch {
                            pokeRepository.save(
                                Poke(
                                    id = UUID.randomUUID().toString(),
                                    pairId = dashboardState.pair?.id.orEmpty(),
                                    text = text
                                )
                            )
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.dashboard_poke_sent)
                            )
                            syncController.syncInBackground()
                        }
                    },
                    onOpenCalendar = { openTab("calendar") }
                )
            }
            composable(Routes.STORE) {
                StoreScreen(
                    registry = registry,
                    enabledIds = enabledIds,
                    onToggle = viewModel::setEnabled
                )
            }
            composable(Routes.MORE) {
                MoreScreen(
                    overflowModules = overflowModules,
                    onOpenModule = { module -> openTab(module.route) },
                    onOpenStore = { openTab(Routes.STORE) },
                    onOpenConnect = { navController.navigate(Routes.CONNECT) { launchSingleTop = true } },
                    onExport = { exportLauncher.launch("vdvoem-backup.json") },
                    onImport = { importLauncher.launch(arrayOf("*/*")) }
                )
            }
            composable(Routes.CONNECT) {
                val connectViewModel: ConnectPairViewModel = koinViewModel()
                val connectState by connectViewModel.state.collectAsStateWithLifecycle()
                ConnectPairScreen(
                    state = connectState,
                    onServerChange = connectViewModel::onServerChange,
                    onInviteCodeChange = connectViewModel::onInviteCodeChange,
                    onNamesChange = connectViewModel::onNamesChange,
                    onSinceChange = connectViewModel::onSinceChange,
                    onCreatePair = connectViewModel::createPair,
                    onJoin = { code -> connectViewModel.join(code) },
                    onCancel = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            enabledModules.forEach { module ->
                composable(module.route) {
                    module.Screen(Modifier.fillMaxSize())
                }
            }
        }
    }

    if (pairDialogOpen) {
        PairDialog(
            pair = dashboardState.pair,
            onDismiss = { pairDialogOpen = false },
            onConfirm = { nameA, nameB, sinceMillis, birthdayA, birthdayB ->
                dashboardViewModel.savePair(nameA, nameB, sinceMillis, birthdayA, birthdayB)
                pairDialogOpen = false
                syncController.syncInBackground()
            }
        )
    }
}
