package com.example.myapplication2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.presentation.navigation.NavIntentViewModel
import com.example.myapplication2.presentation.navigation.RegulationNavHost
import com.example.myapplication2.presentation.onboarding.OnboardingScreen
import com.example.myapplication2.presentation.root.AppRootViewModel
import com.example.myapplication2.presentation.root.AppState
import com.example.myapplication2.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ViewModelProvider(this)[NavIntentViewModel::class.java].applyIntent(intent)

        val app = application as RegulationApplication
        val rootVm = AppRootViewModel(app.container, applicationContext)

        setContent {
            AppTheme {
                AnimatedContent(
                    targetState = rootVm.appState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                ) { state ->
                    when (state) {
                        AppState.Loading -> SplashScreen()
                        AppState.Onboarding -> OnboardingScreen(
                            initialProfile = rootVm.onboardingInitialProfile,
                            onComplete = rootVm::completeOnboarding,
                        )
                        AppState.Main -> RegulationNavHost(app.container)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ViewModelProvider(this)[NavIntentViewModel::class.java].applyIntent(intent)
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = PaleTeal,
            ) {
                Icon(
                    Icons.Filled.Security,
                    null,
                    tint = PrimaryTeal,
                    modifier = Modifier.padding(20.dp).size(48.dp),
                )
            }
            Text(
                "Regulatory Assistant",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Preparing your setup…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(color = PrimaryTeal, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}
