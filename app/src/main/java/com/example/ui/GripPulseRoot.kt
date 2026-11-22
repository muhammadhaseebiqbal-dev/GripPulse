package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.LiquidGlassFloatingNavBar
import com.example.ui.components.LiquidGlassFloatingRail
import com.example.ui.components.LiquidNavItem
import com.example.ui.screens.CalibrateScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GripsScreen
import com.example.ui.screens.WidgetsScreen
import com.example.ui.theme.LocalGripAccents

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_GRIPS = "grips"
const val ROUTE_WIDGETS = "widgets"
const val ROUTE_CALIBRATE = "calibrate"

@Composable
fun GripPulseRoot(
    viewModel: GripPulseViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val accents = LocalGripAccents.current

    val navItems = listOf(
        LiquidNavItem(
            route = ROUTE_DASHBOARD,
            label = stringResource(R.string.nav_today),
            selectedIcon = Icons.Rounded.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard,
            accentColor = accents.leftHand,
            testTag = "nav_tab_today"
        ),
        LiquidNavItem(
            route = ROUTE_GRIPS,
            label = stringResource(R.string.nav_grips),
            selectedIcon = Icons.Rounded.Analytics,
            unselectedIcon = Icons.Outlined.Analytics,
            accentColor = accents.rightHand,
            testTag = "nav_tab_grips"
        ),
        LiquidNavItem(
            route = ROUTE_WIDGETS,
            label = stringResource(R.string.nav_widgets),
            selectedIcon = Icons.Rounded.Widgets,
            unselectedIcon = Icons.Outlined.Widgets,
            accentColor = accents.bothHands,
            testTag = "nav_tab_widgets"
        ),
        LiquidNavItem(
            route = ROUTE_CALIBRATE,
            label = stringResource(R.string.nav_calibrate),
            selectedIcon = Icons.Rounded.Tune,
            unselectedIcon = Icons.Outlined.Tune,
            accentColor = accents.highlight,
            testTag = "nav_tab_calibrate"
        )
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.toggleBackgroundTracking(context)
    }

    val handleToggleTracking: () -> Unit = {
        if (state.isBackgroundTracking) {
            viewModel.toggleBackgroundTracking(context)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.toggleBackgroundTracking(context)
        }
    }

    LaunchedEffect(state.statusMessage) {
        val message = state.statusMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Live atmospheric ambient tint reacting gently to current detected hand
    val ambientHandColor by animateColorAsState(
        targetValue = accents.colorForHand(state.liveSnapshot.hand),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ambient_hand_tint"
    )
    val bgColor = MaterialTheme.colorScheme.background

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .drawBehind {
                // Expressive atmospheric top-corner ambient light bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ambientHandColor.copy(alpha = if (accents.isDark) 0.16f else 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.08f),
                        radius = size.minDimension * 0.95f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.08f),
                    radius = size.minDimension * 0.95f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accents.bothHands.copy(alpha = if (accents.isDark) 0.10f else 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.82f),
                        radius = size.minDimension * 0.85f
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.82f),
                    radius = size.minDimension * 0.85f
                )
            }
    ) {
        val useFloatingRail = maxWidth >= 600.dp

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            if (useFloatingRail) {
                LiquidGlassFloatingRail(
                    items = navItems,
                    selectedRoute = currentRoute,
                    onSelect = navigateToTab
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DASHBOARD,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                ) {
                    composable(ROUTE_DASHBOARD) {
                        DashboardScreen(
                            state = state,
                            onToggleBackgroundTracking = handleToggleTracking,
                            onTeachCurrentHand = viewModel::teachCurrentHand,
                            onOpenGripsTab = { navigateToTab(ROUTE_GRIPS) },
                            onOpenWidgetsTab = { navigateToTab(ROUTE_WIDGETS) }
                        )
                    }
                    composable(ROUTE_GRIPS) {
                        GripsScreen(
                            state = state,
                            onSelectDate = viewModel::selectDate
                        )
                    }
                    composable(ROUTE_WIDGETS) {
                        WidgetsScreen(
                            state = state,
                            onToggleBackgroundTracking = handleToggleTracking,
                            onSyncWidgetsNow = viewModel::syncWidgetsNow,
                            onPinResultMessage = viewModel::postMessage
                        )
                    }
                    composable(ROUTE_CALIBRATE) {
                        CalibrateScreen(
                            state = state,
                            onCaptureCalibration = viewModel::captureCalibrationFor,
                            onRunAiPatternStudy = viewModel::runAiPatternStudy,
                            onUpdateDeadzone = viewModel::updateDeadzone,
                            onResetCalibration = viewModel::resetCalibration,
                            onToggleBackgroundTracking = handleToggleTracking,
                            onResetTodayStats = viewModel::resetTodayStats
                        )
                    }
                }
            }
        }

        // Snackbar positioned gracefully above the floating liquid glass pill
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (useFloatingRail) 16.dp else 94.dp, start = 16.dp, end = 16.dp)
        )

        // Floating Liquid Glass Pill Bottom Navigation Bar
        if (!useFloatingRail) {
            LiquidGlassFloatingNavBar(
                items = navItems,
                selectedRoute = currentRoute,
                onSelect = navigateToTab,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
