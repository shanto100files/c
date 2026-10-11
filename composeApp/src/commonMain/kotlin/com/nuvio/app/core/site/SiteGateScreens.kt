package com.nuvio.app.core.site

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio

/**
 * Shown while the site is asked whether this build is still allowed in.
 *
 * Deliberately content-free: it is the first thing rendered, before anything has been fetched,
 * so it must not promise content it cannot show yet.
 */
@Composable
fun CheckingScreen() {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = tokens.colors.accent,
            modifier = Modifier.size(NuvioTokens.Space.s28),
        )
    }
}

/**
 * The one screen that can say no.
 *
 * The reason is the server's own wording, and the key field is here because a mistyped or
 * rotated key must be recoverable without reinstalling the app - a rejection that cannot be
 * reversed is indistinguishable from a broken build.
 *
 * A transport failure is never routed here; [SiteGate] only reaches this state on a real 403.
 */
@Composable
fun BlockedScreen(
    reason: String,
    onRetry: (String?) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    var apiKey by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = NuvioTokens.Space.s24),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Access blocked",
                color = tokens.colors.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = reason,
                color = tokens.colors.warning,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = tokens.shapes.button,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = tokens.colors.textPrimary,
                    unfocusedTextColor = tokens.colors.textPrimary,
                    focusedBorderColor = tokens.colors.borderFocus,
                    unfocusedBorderColor = tokens.colors.borderDefault,
                    focusedLabelColor = tokens.colors.textSecondary,
                    unfocusedLabelColor = tokens.colors.textMuted,
                    cursorColor = tokens.colors.accent,
                ),
            )
            Button(
                onClick = { onRetry(apiKey.trim().takeIf { it.isNotEmpty() }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NuvioTokens.Space.s56),
                shape = tokens.shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.colors.accent,
                    contentColor = tokens.colors.onAccent,
                ),
            ) {
                Text(
                    text = "Try again",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "If this keeps happening, update the app.",
                color = tokens.colors.textMuted,
                fontSize = 13.sp,
            )
        }
    }
}

/**
 * Holds the [SiteGate] check ahead of whatever the app actually renders.
 *
 * [enabled] is false on the paths that never ask the site anything (previews, the iOS overlay);
 * a gate that cannot ask would only ever show one screen, so it renders the content instead.
 */
@Composable
fun SiteGateHost(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }

    var gate by remember { mutableStateOf<SiteGate>(SiteGate.Checking) }
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt) {
        gate = SiteApi.gate()
    }

    when (val current = gate) {
        SiteGate.Checking -> CheckingScreen()

        is SiteGate.Blocked -> BlockedScreen(
            reason = current.reason,
            onRetry = { key ->
                if (!key.isNullOrBlank()) SiteApi.setApiKey(key)
                gate = SiteGate.Checking
                attempt += 1
            },
        )

        SiteGate.Open -> content()
    }
}
