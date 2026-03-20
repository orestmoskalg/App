package com.example.myapplication2.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication2.presentation.calendar.CalendarRoute
import com.example.myapplication2.presentation.carddetail.CardDetailRoute
import com.example.myapplication2.presentation.dashboard.DashboardRoute
import com.example.myapplication2.presentation.insights.InsightsRoute
import com.example.myapplication2.presentation.learning.LearningRoute
import com.example.myapplication2.presentation.onboarding.OnboardingRoute
import com.example.myapplication2.presentation.root.AppRootState
import com.example.myapplication2.presentation.root.AppRootViewModel
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.presentation.search.SearchRoute
import com.example.myapplication2.presentation.strategy.StrategyRoute
import com.example.myapplication2.ui.theme.AppBackgroundGradient
import com.example.myapplication2.ui.theme.AppBlack
import com.example.myapplication2.ui.theme.AppSurface
import com.example.myapplication2.ui.theme.AppTextMuted

private sealed class RegulationDestination(
    val route: String,
    val label: String,
    val compactLabel: String,
    val shortLabel: String,
) {
    data object Onboarding : RegulationDestination("onboarding", "Onboarding", "Start", "ON")
    data object Dashboard : RegulationDestination("dashboard", "Home", "Home", "HM")
    data object Search : RegulationDestination("search", "Search", "Search", "SR")
    data object Calendar : RegulationDestination("calendar", "Calendar", "Cal", "CL")
    data object Insights : RegulationDestination("insights", "Insights", "Intel", "IN")
    data object Strategy : RegulationDestination("strategy", "Strategy", "Plan", "ST")
    data object Learning : RegulationDestination("learning", "Learning", "Learn", "LN")
    data object CardDetail : RegulationDestination("card_detail/{cardId}", "Card Detail", "Detail", "DT")
}

private val tabDestinations = listOf(
    RegulationDestination.Dashboard,
    RegulationDestination.Search,
    RegulationDestination.Calendar,
    RegulationDestination.Insights,
    RegulationDestination.Strategy,
    RegulationDestination.Learning,
)

@Composable
fun RegulationApp(
    modifier: Modifier = Modifier,
) {
    val appContainer = rememberAppContainer()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    CompositionLocalProvider(LocalAppContainer provides appContainer) {
        val appRootViewModel: AppRootViewModel = regulationViewModel()
        val appState by appRootViewModel.state.collectAsStateWithLifecycle()
        val showBottomBar = currentDestination?.route in tabDestinations.map { it.route }

        LaunchedEffect(appState) {
            when (appState) {
                AppRootState.Loading -> Unit
                AppRootState.Onboarding -> if (currentDestination?.route != RegulationDestination.Onboarding.route) {
                    navController.navigate(RegulationDestination.Onboarding.route) {
                        launchSingleTop = true
                    }
                }
                is AppRootState.Ready -> navController.navigate(RegulationDestination.Dashboard.route) {
                    popUpTo(RegulationDestination.Onboarding.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }

        Scaffold(
            modifier = modifier,
            containerColor = AppBlack,
            bottomBar = {
                if (showBottomBar) {
                    PremiumBottomBar(
                        currentDestination = currentDestination,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(RegulationDestination.Dashboard.route) {
                                    saveState = true
                                }
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackgroundGradient)
                    .padding(innerPadding),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = RegulationDestination.Onboarding.route,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(RegulationDestination.Onboarding.route) { OnboardingRoute() }
                    composable(RegulationDestination.Dashboard.route) {
                        DashboardRoute(
                            onOpenCard = { card ->
                                if (card.type == CardType.SEARCH_HISTORY) {
                                    PendingCardHolder.card = card
                                }
                                navController.navigate(cardDetailRoute(card.id))
                            },
                            onOpenSearch = { navController.navigate(RegulationDestination.Search.route) { launchSingleTop = true } },
                            onOpenCalendar = { navController.navigate(RegulationDestination.Calendar.route) { launchSingleTop = true } },
                            onOpenInsights = { navController.navigate(RegulationDestination.Insights.route) { launchSingleTop = true } },
                            onOpenStrategy = { navController.navigate(RegulationDestination.Strategy.route) { launchSingleTop = true } },
                            onOpenLearning = { navController.navigate(RegulationDestination.Learning.route) { launchSingleTop = true } },
                        )
                    }
                    composable(RegulationDestination.Search.route) {
                        SearchRoute(
                            onOpenCard = { card ->
                                if (card.type == CardType.SEARCH_HISTORY) {
                                    PendingCardHolder.card = card
                                }
                                navController.navigate(cardDetailRoute(card.id))
                            },
                        )
                    }
                    composable(RegulationDestination.Calendar.route) {
                        CalendarRoute(
                            onOpenCard = { card ->
                                if (card.type == CardType.REGULATORY_EVENT) {
                                    PendingCardHolder.card = card
                                }
                                navController.navigate(cardDetailRoute(card.id))
                            },
                        )
                    }
                    composable(RegulationDestination.Insights.route) {
                        InsightsRoute(
                            onOpenCard = { card ->
                                navController.navigate(cardDetailRoute(card.id))
                            },
                        )
                    }
                    composable(RegulationDestination.Strategy.route) {
                        StrategyRoute(
                            onOpenCard = { card ->
                                navController.navigate(cardDetailRoute(card.id))
                            },
                        )
                    }
                    composable(RegulationDestination.Learning.route) {
                        LearningRoute(
                            onOpenCard = { card ->
                                navController.navigate(cardDetailRoute(card.id))
                            },
                        )
                    }
                    composable(
                        route = RegulationDestination.CardDetail.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("cardId") { type = NavType.StringType },
                        ),
                    ) { backStackEntry ->
                        val cardId = backStackEntry.arguments?.getString("cardId").orEmpty()
                        if (cardId.isBlank()) {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                        } else {
                            CardDetailRoute(
                                cardId = cardId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun cardDetailRoute(cardId: String): String = "card_detail/$cardId"

@Composable
private fun PremiumBottomBar(
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = Color.Transparent,
        shadowElevation = 14.dp,
        shape = RoundedCornerShape(30.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(AppSurface.copy(alpha = 0.98f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tabDestinations.forEach { destination ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationPill(
                        destination = destination,
                        selected = selected,
                        onClick = { onNavigate(destination.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationPill(
    destination: RegulationDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shadowElevation = if (selected) 10.dp else 0.dp,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) DockSelected else Color.Transparent)
                .padding(horizontal = 6.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            NavigationIcon(
                destination = destination,
                tint = if (selected) Color.White else AppTextMuted.copy(alpha = 0.95f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun NavigationIcon(
    destination: RegulationDestination,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (destination) {
            RegulationDestination.Dashboard -> {
                val gap = size.width * 0.08f
                val cell = (size.width - gap) / 2f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0f, 0f),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(6f, 6f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cell + gap, 0f),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(6f, 6f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0f, cell + gap),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(6f, 6f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cell + gap, cell + gap),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }

            RegulationDestination.Search -> {
                val radius = size.minDimension * 0.28f
                val center = Offset(size.width * 0.42f, size.height * 0.42f)
                drawCircle(
                    color = tint,
                    radius = radius,
                    center = center,
                    style = Stroke(width = size.minDimension * 0.12f),
                )
                drawLine(
                    color = tint,
                    start = Offset(center.x + radius * 0.55f, center.y + radius * 0.55f),
                    end = Offset(size.width * 0.92f, size.height * 0.92f),
                    strokeWidth = size.minDimension * 0.12f,
                    cap = StrokeCap.Round,
                )
            }

            RegulationDestination.Calendar -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.1f, size.height * 0.14f),
                    size = Size(size.width * 0.8f, size.height * 0.76f),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = size.minDimension * 0.1f),
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.1f, size.height * 0.34f),
                    end = Offset(size.width * 0.9f, size.height * 0.34f),
                    strokeWidth = size.minDimension * 0.1f,
                    cap = StrokeCap.Round,
                )
                drawCircle(color = tint, radius = size.minDimension * 0.05f, center = Offset(size.width * 0.3f, size.height * 0.56f))
                drawCircle(color = tint, radius = size.minDimension * 0.05f, center = Offset(size.width * 0.5f, size.height * 0.56f))
                drawCircle(color = tint, radius = size.minDimension * 0.05f, center = Offset(size.width * 0.7f, size.height * 0.56f))
            }

            RegulationDestination.Insights -> {
                val barWidth = size.width * 0.16f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.5f),
                    size = Size(barWidth, size.height * 0.3f),
                    cornerRadius = CornerRadius(6f, 6f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.42f, size.height * 0.32f),
                    size = Size(barWidth, size.height * 0.48f),
                    cornerRadius = CornerRadius(6f, 6f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.72f, size.height * 0.18f),
                    size = Size(barWidth, size.height * 0.62f),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }

            RegulationDestination.Strategy -> {
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.14f, size.height * 0.74f),
                    end = Offset(size.width * 0.42f, size.height * 0.5f),
                    strokeWidth = size.minDimension * 0.12f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.42f, size.height * 0.5f),
                    end = Offset(size.width * 0.62f, size.height * 0.58f),
                    strokeWidth = size.minDimension * 0.12f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.62f, size.height * 0.58f),
                    end = Offset(size.width * 0.88f, size.height * 0.24f),
                    strokeWidth = size.minDimension * 0.12f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.72f, size.height * 0.24f),
                    end = Offset(size.width * 0.88f, size.height * 0.24f),
                    strokeWidth = size.minDimension * 0.12f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.88f, size.height * 0.24f),
                    end = Offset(size.width * 0.88f, size.height * 0.4f),
                    strokeWidth = size.minDimension * 0.12f,
                    cap = StrokeCap.Round,
                )
            }

            RegulationDestination.Learning -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.08f, size.height * 0.14f),
                    size = Size(size.width * 0.36f, size.height * 0.72f),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = size.minDimension * 0.09f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.56f, size.height * 0.14f),
                    size = Size(size.width * 0.36f, size.height * 0.72f),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = size.minDimension * 0.09f),
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.5f, size.height * 0.14f),
                    end = Offset(size.width * 0.5f, size.height * 0.86f),
                    strokeWidth = size.minDimension * 0.08f,
                    cap = StrokeCap.Round,
                )
            }

            RegulationDestination.Onboarding -> Unit
            RegulationDestination.CardDetail -> Unit
        }
    }
}

private val DockSelected = Color(0xFF121212)
