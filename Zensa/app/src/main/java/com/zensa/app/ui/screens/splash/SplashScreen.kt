package com.zensa.app.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zensa.app.R
import com.zensa.app.core.constants.AppConstants
import com.zensa.app.ui.theme.ZensaTheme
import kotlinx.coroutines.delay

/**
 * SplashScreen — the first thing the user sees when Zensa launches.
 *
 * Currently a static placeholder: shows the logo and tagline for
 * SPLASH_DELAY_MS milliseconds, then calls onNavigateNext.
 *
 * Later this will check whether a valid JWT token exists and route
 * to Login or Dashboard accordingly.
 */
@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit
) {
    // Wait, then signal navigation is ready.
    LaunchedEffect(Unit) {
        delay(AppConstants.SPLASH_DELAY_MS)
        onNavigateNext()
    }

    // ----- Layout -----
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_zensa_logo),
                contentDescription = "Zensa logo",
                modifier = Modifier.size(180.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = AppConstants.APP_NAME,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = AppConstants.APP_TAGLINE,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    ZensaTheme {
        SplashScreen(onNavigateNext = {})
    }
}
