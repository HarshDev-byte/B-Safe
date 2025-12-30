package com.safeguard.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safeguard.app.ui.screens.*
import com.safeguard.app.ui.viewmodels.MainViewModel

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Contacts : Screen("contacts")
    object AddContact : Screen("add_contact")
    object EditContact : Screen("edit_contact/{contactId}") {
        fun createRoute(contactId: Long) = "edit_contact/$contactId"
    }
    object Settings : Screen("settings")
    object TriggerSettings : Screen("trigger_settings")
    object SOSSettings : Screen("sos_settings")
    object PrivacySettings : Screen("privacy_settings")
    object History : Screen("history")
    object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: Long) = "event_detail/$eventId"
    }
    object DangerZones : Screen("danger_zones")
    object CheckIns : Screen("check_ins")
    object FakeCall : Screen("fake_call")
    object StealthMode : Screen("stealth_mode")
    object Dashboard : Screen("dashboard")
    object NearbyPlaces : Screen("nearby_places")
    object LiveLocation : Screen("live_location")
    object SafeRoute : Screen("safe_route")
    object SafetyScore : Screen("safety_score")
    object Journey : Screen("journey")
}

@Composable
fun SafeGuardNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Flow: SignIn -> Onboarding -> Home
    // If already signed in and onboarding complete -> Home
    // If signed in but onboarding not complete -> Onboarding
    // If not signed in -> SignIn
    val startDestination = when {
        currentUser != null && uiState.isOnboardingComplete -> Screen.Home.route
        currentUser != null && !uiState.isOnboardingComplete -> Screen.Onboarding.route
        else -> Screen.SignIn.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.SignIn.route) {
            SignInScreen(
                viewModel = viewModel,
                onSignInSuccess = {
                    // After sign in, check if onboarding is complete
                    if (uiState.isOnboardingComplete) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    }
                },
                onSkip = {
                    // Allow skip but still go to onboarding if not complete
                    if (uiState.isOnboardingComplete) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = viewModel,
                onComplete = {
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToFakeCall = { navController.navigate(Screen.FakeCall.route) },
                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToNearbyPlaces = { navController.navigate(Screen.NearbyPlaces.route) },
                onNavigateToLiveLocation = { navController.navigate(Screen.LiveLocation.route) },
                onNavigateToSafeRoute = { navController.navigate(Screen.SafeRoute.route) },
                onNavigateToSafetyScore = { navController.navigate(Screen.SafetyScore.route) },
                onNavigateToJourney = { navController.navigate(Screen.Journey.route) },
                onSignIn = { navController.navigate(Screen.SignIn.route) }
            )
        }

        composable(Screen.Contacts.route) {
            ContactsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onAddContact = { navController.navigate(Screen.AddContact.route) },
                onEditContact = { contactId ->
                    navController.navigate(Screen.EditContact.createRoute(contactId))
                }
            )
        }

        composable(Screen.AddContact.route) {
            AddEditContactScreen(
                viewModel = viewModel,
                contactId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EditContact.route) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId")?.toLongOrNull()
            AddEditContactScreen(
                viewModel = viewModel,
                contactId = contactId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTriggerSettings = { navController.navigate(Screen.TriggerSettings.route) },
                onNavigateToSOSSettings = { navController.navigate(Screen.SOSSettings.route) },
                onNavigateToPrivacySettings = { navController.navigate(Screen.PrivacySettings.route) },
                onNavigateToDangerZones = { navController.navigate(Screen.DangerZones.route) },
                onNavigateToCheckIns = { navController.navigate(Screen.CheckIns.route) },
                onNavigateToStealthMode = { navController.navigate(Screen.StealthMode.route) }
            )
        }

        composable(Screen.TriggerSettings.route) {
            TriggerSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SOSSettings.route) {
            SOSSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToContacts = { navController.navigate(Screen.Contacts.route) }
            )
        }

        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onEventClick = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                }
            )
        }

        composable(Screen.EventDetail.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")?.toLongOrNull() ?: 0L
            EventDetailScreen(
                viewModel = viewModel,
                eventId = eventId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DangerZones.route) {
            DangerZonesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CheckIns.route) {
            CheckInsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FakeCall.route) {
            FakeCallScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StealthMode.route) {
            StealthModeScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            SafetyDashboardScreen(
                viewModel = viewModel,
                insights = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NearbyPlaces.route) {
            NearbyPlacesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LiveLocation.route) {
            LiveLocationScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SafeRoute.route) {
            SafeRouteScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SafetyScore.route) {
            SafetyScoreScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToTriggers = { navController.navigate(Screen.TriggerSettings.route) }
            )
        }

        composable(Screen.Journey.route) {
            JourneyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
