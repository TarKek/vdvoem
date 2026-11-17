package app.vdvoem.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import app.vdvoem.BuildConfig
import app.vdvoem.update.ApkInstaller
import app.vdvoem.update.UpdateChecker
import app.vdvoem.update.UpdateInfo
import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.domain.model.Poke
import app.vdvoem.core.domain.module.ModuleRegistry
import app.vdvoem.core.domain.repository.PokeRepository
import app.vdvoem.core.sync.SyncController
import app.vdvoem.core.sync.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
import kotlinx.coroutines.withContext
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
    // Модули не включаются и не выключаются: всё из APK доступно всегда.
    // Пользователь решает только, показывать модуль в навигации или скрыть.
    val hiddenIds by viewModel.hiddenIds.collectAsStateWithLifecycle()
    val orderIds by viewModel.order.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.state.collectAsStateWithLifecycle()
    val homePreferences: HomePreferences = koinInject()
    val pokeRepository: PokeRepository = koinInject()
    val syncController: SyncController = koinInject()
    val presentsTabController: PresentsTabController = koinInject()
    val widgets by homePreferences.widgets
        .collectAsStateWithLifecycle(initialValue = HomePreferences.DEFAULT_WIDGETS)
    val lastPoke = dashboardState.lastPoke?.text
    val sentPokeIds by homePreferences.sentPokeIds
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val syncStatus by syncController.status.collectAsStateWithLifecycle(initialValue = SyncStatus.Idle)
    val pendingSyncCount by syncController.pendingCount.collectAsStateWithLifecycle()
    val syncing = syncStatus is SyncStatus.Running
    val navController = rememberNavController()

    // Автообновление: при запуске смотрим последний релиз на GitHub.
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateChecking by remember { mutableStateOf(false) }
    var updateDownloading by remember { mutableStateOf(false) }
    var updateFailed by remember { mutableStateOf(false) }

    suspend fun checkUpdates() {
        updateChecking = true
        val result = runCatching { UpdateChecker.check(BuildConfig.VERSION_NAME) }
        updateInfo = result.getOrNull()
        updateFailed = result.isFailure
        updateChecking = false
    }

    LaunchedEffect(Unit) { checkUpdates() }

    // Любая локальная правка (событие, желание, подарок, сердечко) сразу уезжает на сервер:
    // раньше приходилось ждать ближайший фоновый проход и казалось, что синка нет.
    LaunchedEffect(Unit) {
        LocalChangeSignal.changes.collectLatest {
            delay(LOCAL_CHANGE_SYNC_DELAY_MS)
            syncController.syncInBackground()
        }
    }

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
    var pairDialogOpen by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = runCatching {
                withContext(Dispatchers.IO) {
                    val json = backupViewModel.buildExport()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray())
                    } ?: error("output stream is null")
                }
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
            val result = withContext(Dispatchers.IO) {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                }.getOrNull()
                if (text.isNullOrBlank()) {
                    ImportResult(ok = false)
                } else {
                    backupViewModel.import(text)
                }
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

    // Пользовательский порядок из магазина: известные модули идут как сохранили,
    // остальные — по манифесту. Тот же список использует магазин.
    val orderedModules = remember(orderIds, registry) { registry.ordered(orderIds) }
    val effectiveOrder = remember(orderedModules) { orderedModules.map { it.manifest.id } }
    // Маршруты всех модулей, включая скрытые: восстановление back stack после поворота
    // или смерти процесса не должно падать на маршруте, которого нет в графе.
    val allNavModules = remember(orderedModules) {
        orderedModules.filter {
            it.manifest.id != StoreModule.ID && !it.manifest.comingSoon
        }
    }
    // Модули для навигации: всё собранное в APK за вычетом скрытых пользователем.
    // Скрытие не удаляет данные — модуль просто пропадает из вкладок и «Ещё».
    val visibleModules = remember(orderedModules, hiddenIds) {
        registry.visible(orderedModules, hiddenIds).filter {
            it.manifest.id != StoreModule.ID && !it.manifest.comingSoon
        }
    }
    val visibleRoutes = remember(visibleModules) { visibleModules.map { it.route }.toSet() }
    // Магазин дополнений работает на старой сигнатуре enabledIds/onToggle: «включено» в нём
    // теперь означает «видно в навигации», то есть модуль не скрыт пользователем.
    val visibleIds = remember(orderedModules, hiddenIds) {
        orderedModules.map { it.manifest.id }.toSet() - hiddenIds
    }
    // «Главная» и «Ещё» закреплены, между ними — две настраиваемые вкладки (не больше четырёх всего).
    val navRoutes by homePreferences.navRoutes
        .collectAsStateWithLifecycle(initialValue = HomePreferences.DEFAULT_NAV_ROUTES)
    val bottomModules = remember(visibleModules, navRoutes) {
        val byRoute = visibleModules.associateBy { it.route }
        val position = visibleModules.withIndex().associate { (index, module) -> module.route to index }
        // Какие вкладки выбраны — решает navRoutes, а их порядок берём из магазина.
        // Скрытая вкладка просто выпадает из нижней панели, а не роняет навигацию.
        navRoutes.mapNotNull { byRoute[it] }
            .distinctBy { it.route }
            .sortedBy { position[it.route] ?: Int.MAX_VALUE }
            .take(HomePreferences.NAV_SLOTS)
    }
    val overflowModules = remember(visibleModules, bottomModules) {
        val inBottomBar = bottomModules.map { it.route }.toSet()
        visibleModules.filterNot { it.route in inBottomBar }
    }

    /**
     * Единый переход к вкладке/модулю: не новый уровень, а переключение вкладки.
     * Модуль скрыт — ведём в магазин дополнений, чтобы его можно было вернуть.
     */
    fun openTab(route: String) {
        val target = when {
            route == Routes.HOME || route == Routes.MORE || route == Routes.STORE -> route
            route in visibleRoutes -> route
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

    // Модуль скрыли, пока он открыт, — уводим на «Главную», чтобы не остаться в пустоте.
    LaunchedEffect(hiddenIds) {
        val route = navController.currentBackStackEntry?.destination?.route
        val module = registry.byRoute(route)
        if (module != null && module.manifest.id in hiddenIds) {
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
            enterTransition = {
                slideInHorizontally(animationSpec = tween(260)) { it / 6 } +
                    fadeIn(animationSpec = tween(200))
            },
            exitTransition = { fadeOut(animationSpec = tween(140)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(240)) { it / 6 } +
                    fadeOut(animationSpec = tween(160))
            }
        ) {
            composable(Routes.HOME) {
                DashboardScreen(
                    modules = visibleModules,
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
                    onOpenCycle = { openTab("cycle") },
                    onOpenPairEditor = { pairDialogOpen = true },
                    widgets = widgets,
                    lastPoke = lastPoke,
                    pokes = dashboardState.pokes,
                    sentPokeIds = sentPokeIds,
                    cycleStats = dashboardState.cycleStats,
                    refreshing = syncing,
                    onRefresh = { syncController.syncInBackground() },
                    onWidgetsChange = { list ->
                        scope.launch { homePreferences.setWidgets(list) }
                    },
                    onAddWidget = { type ->
                        scope.launch { homePreferences.addWidget(type) }
                    },
                    onSendPoke = { text ->
                        scope.launch {
                            val pokeId = UUID.randomUUID().toString()
                            homePreferences.addSentPokeId(pokeId)
                            pokeRepository.save(
                                Poke(
                                    id = pokeId,
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
                    hiddenIds = hiddenIds,
                    onHiddenChange = viewModel::setHidden,
                    order = effectiveOrder,
                    onOrderChange = viewModel::setOrder
                )
            }
            composable(Routes.MORE) {
                MoreScreen(
                    overflowModules = overflowModules,
                    onOpenModule = { module -> openTab(module.route) },
                    onOpenStore = { openTab(Routes.STORE) },
                    onOpenConnect = { navController.navigate(Routes.CONNECT) { launchSingleTop = true } },
                    bottomModules = bottomModules,
                    navModules = visibleModules,
                    onNavRoutesChange = { routes ->
                        scope.launch { homePreferences.setNavRoutes(routes) }
                    },
                    pairLabel = dashboardState.pair
                        ?.takeIf { it.sinceMillis > 0 && it.nameA.isNotBlank() && it.nameB.isNotBlank() }
                        ?.let { "${it.nameA} и ${it.nameB}" },
                    onExport = { exportLauncher.launch("vdvoem-backup.json") },
                    onImport = { importLauncher.launch(arrayOf("*/*")) },
                    pendingCount = pendingSyncCount,
                    onSyncNow = { syncController.syncInBackground() },
                    appVersion = BuildConfig.VERSION_NAME,
                    updateVersion = updateInfo?.version,
                    updateChecking = updateChecking,
                    updateDownloading = updateDownloading,
                    updateFailed = updateFailed,
                    onUpdateClick = {
                        val available = updateInfo
                        if (available == null) {
                            scope.launch { checkUpdates() }
                        } else {
                            scope.launch {
                                updateDownloading = true
                                updateFailed = false
                                val downloaded = runCatching {
                                    UpdateChecker.download(context, available.apkUrl)
                                }
                                downloaded
                                    .onSuccess { apk ->
                                        when {
                                            !UpdateChecker.canInstall(context) -> {
                                                UpdateChecker.requestInstallPermission(context)
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.more_update_permission)
                                                )
                                            }
                                            !withContext(Dispatchers.IO) { ApkInstaller.install(context, apk) } -> {
                                                updateFailed = true
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.more_update_failed)
                                                )
                                            }
                                        }
                                    }
                                    .onFailure {
                                        updateFailed = true
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.more_update_failed)
                                        )
                                    }
                                updateDownloading = false
                            }
                        }
                    }
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
                    onSyncNow = connectViewModel::syncNow,
                    onLeavePair = connectViewModel::leavePair,
                    onCancel = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Скрытые модули тоже объявлены в графе — их экран просто никогда не показывается
            // в навигации, а открытый скрытый маршрут уводит на «Главную» (см. LaunchedEffect).
            allNavModules.forEach { module ->
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

/** Пауза перед отправкой после правки: несколько быстрых изменений уедут одним проходом. */
private const val LOCAL_CHANGE_SYNC_DELAY_MS = 900L
