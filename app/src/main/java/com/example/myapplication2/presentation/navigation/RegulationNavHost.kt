package com.example.myapplication2.presentation.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.*
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.presentation.calendar.CalendarScreen
import com.example.myapplication2.presentation.calendar.CalendarViewModel
import com.example.myapplication2.presentation.carddetail.CardDetailScreen
import com.example.myapplication2.presentation.carddetail.CardDetailViewModel
import com.example.myapplication2.presentation.dashboard.DashboardScreen
import com.example.myapplication2.presentation.dashboard.DashboardViewModel
import com.example.myapplication2.presentation.knowledge.KnowledgeScreen
import com.example.myapplication2.presentation.knowledge.KnowledgeViewModel
import com.example.myapplication2.presentation.search.SearchScreen
import com.example.myapplication2.presentation.search.SearchViewModel
import com.example.myapplication2.presentation.settings.MainSettingsScreen
import com.example.myapplication2.presentation.settings.MainSettingsViewModel
import com.example.myapplication2.presentation.tools.GlossaryScreen
import com.example.myapplication2.presentation.tools.ComplianceChecklistScreen
import com.example.myapplication2.ui.theme.BottomNavBg
import com.example.myapplication2.ui.theme.BottomNavSelBg
import com.example.myapplication2.ui.theme.BottomNavSelIcon
import com.example.myapplication2.ui.theme.BottomNavUnsel

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isBottomTab: Boolean = true,
) {
    object Home       : Screen("home",      "Home",         Icons.Filled.Home,           Icons.Outlined.Home)
    object Search     : Screen("search",    "Search",       Icons.Filled.AutoAwesome,    Icons.Outlined.Search)
    object Calendar   : Screen("calendar",  "Calendar",     Icons.Filled.CalendarMonth,  Icons.Outlined.CalendarMonth)
    object Knowledge  : Screen("knowledge", "Knowledge",    Icons.Filled.School,         Icons.Outlined.School)
    object AppSettings: Screen("appsettings","Settings",   Icons.Filled.Settings,      Icons.Outlined.Settings)

    object Glossary   : Screen("glossary",  "Glossary",    Icons.Filled.MenuBook,       Icons.Outlined.MenuBook,       false)
    object Checklist  : Screen("checklist", "Checklist",    Icons.Filled.CheckCircle,    Icons.Outlined.CheckCircle,    false)

    object CardDetail : Screen("card/{cardId}", "Card", Icons.Filled.Info, Icons.Outlined.Info, false) {
        fun route(id: String) = "card/$id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegulationNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val activity = LocalActivity.current as ComponentActivity
    val navIntentVm = viewModel<NavIntentViewModel>(viewModelStoreOwner = activity)
    val pendingRoute by navIntentVm.pendingRoute.collectAsStateWithLifecycle()

    LaunchedEffect(pendingRoute) {
        val r = pendingRoute ?: return@LaunchedEffect
        when (r) {
            is PendingNotificationRoute.CardDetail ->
                navController.navigate(Screen.CardDetail.route(r.cardId)) {
                    launchSingleTop = true
                }
            PendingNotificationRoute.CalendarTab ->
                navController.navigate(Screen.Calendar.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
        }
        navIntentVm.consumePendingRoute()
    }

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val bottomTabs = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Calendar,
        Screen.Knowledge,
        Screen.AppSettings,
    )
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = BottomNavBg) {
                    bottomTabs.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(if (selected) screen.selectedIcon else screen.unselectedIcon, screen.label) },
                            label = {
                                Text(
                                    screen.label,
                                    style      = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = BottomNavSelIcon,
                                selectedTextColor   = BottomNavSelIcon,
                                indicatorColor      = BottomNavSelBg,
                                unselectedIconColor = BottomNavUnsel,
                                unselectedTextColor = BottomNavUnsel,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(pad),
            enterTransition  = { fadeIn() + slideInHorizontally { 40 } },
            exitTransition   = { fadeOut() },
            popEnterTransition  = { fadeIn() + slideInHorizontally { -40 } },
            popExitTransition   = { fadeOut() },
        ) {
            composable(Screen.Home.route) { entry ->
                val vm = remember(entry) { DashboardViewModel(container) }
                DashboardScreen(
                    vm = vm,
                    onCardClick         = { navController.navigate(Screen.CardDetail.route(it)) },
                    onNavigateSearch    = { navController.navigate(Screen.Search.route) },
                    onNavigateCalendar  = { navController.navigate(Screen.Calendar.route) },
                    onNavigateKnowledge = { navController.navigate(Screen.Knowledge.route) },
                    onNavigateGlossary  = { navController.navigate(Screen.Glossary.route) },
                    onNavigateChecklist = { navController.navigate(Screen.Checklist.route) },
                )
            }

            composable(Screen.Search.route) { entry ->
                val vm = remember(entry) { SearchViewModel(container) }
                SearchScreen(vm = vm, onCardClick = { navController.navigate(Screen.CardDetail.route(it)) })
            }

            composable(Screen.Calendar.route) { entry ->
                val vm = remember(entry) { CalendarViewModel(container) }
                CalendarScreen(vm = vm, onCardClick = { navController.navigate(Screen.CardDetail.route(it)) })
            }

            composable(Screen.Knowledge.route) { entry ->
                val vm = remember(entry) { KnowledgeViewModel(container) }
                KnowledgeScreen(vm = vm, onCardClick = { navController.navigate(Screen.CardDetail.route(it)) })
            }

            // Main settings screen (instead of Profile)
            composable(Screen.AppSettings.route) { entry ->
                val vm = remember(entry) { MainSettingsViewModel(container) }
                MainSettingsScreen(
                    vm                  = vm,
                    onNavigateGlossary  = { navController.navigate(Screen.Glossary.route) },
                    onNavigateChecklist = { navController.navigate(Screen.Checklist.route) },
                )
            }

            composable(Screen.Glossary.route) {
                GlossaryScreen(onBack = { navController.popBackStack() }, container = container)
            }

            composable(Screen.Checklist.route) {
                ComplianceChecklistScreen(
                    onBack    = { navController.popBackStack() },
                    container = container,
                )
            }

            composable(
                Screen.CardDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("cardId") {
                        type = androidx.navigation.NavType.StringType
                    }
                ),
            ) { backStack ->
                val cardId = backStack.arguments?.getString("cardId") ?: return@composable
                val vm = remember(cardId) { CardDetailViewModel(cardId, container) }
                CardDetailScreen(vm = vm, onBack = { navController.popBackStack() })
            }
        }
    }
}
