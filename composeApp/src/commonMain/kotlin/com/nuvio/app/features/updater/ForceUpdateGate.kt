package com.nuvio.app.features.updater

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.site.SiteUpdate
import com.nuvio.app.core.ui.nuvio
import kotlinx.coroutines.launch

/**
 * Full-screen, non-dismissable gate for the panel's "Force update" toggle.
 *
 * Rendered from [com.nuvio.app.AppGate] (beside the kill switch) once a newer build is published
 * and the operator has turned force-update on. It covers the whole app and offers a single path
 * forward - download and install the update. There is no close button, no ignore, and nothing
 * behind it is reachable, so an installed build cannot keep running past the version the operator
 * has retired.
 */
@Composable
fun ForceUpdateGate(update: SiteUpdate, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<GateState>(GateState.Idle) }

    fun install(path: String) {
        state = GateState.Installing
        AppUpdaterPlatform.installDownloadedApk(path).onFailure { error ->
            state = GateState.Failed(error.message)
        }
    }

    fun startUpdate() {
        if (state is GateState.Downloading || state is GateState.Installing) return
        scope.launch {
            state = GateState.Downloading(progress = 0f)
            val result = AppUpdaterPlatform.downloadApk(
                assetUrl = update.apkUrl,
                assetName = "app-${update.versionName}.apk",
            ) { downloaded, total ->
                state = GateState.Downloading(
                    progress = if (total != null && total > 0L) {
                        (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    } else {
                        null
                    },
                )
            }
            result.onSuccess { path ->
                if (AppUpdaterPlatform.canRequestPackageInstalls()) {
                    install(path)
                } else {
                    state = GateState.ReadyToInstall(path)
                }
            }.onFailure { error ->
                state = GateState.Failed(error.message)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.nuvio.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Update required",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Version ${update.versionName} is required to keep using the app. " +
                    "Please update now to continue.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (update.releaseNotes.isNotBlank()) {
                Text(
                    text = update.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            when (val current = state) {
                GateState.Idle -> UpdateButton(onClick = { startUpdate() })

                is GateState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { current.progress ?: 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = if (current.progress != null) {
                            "Downloading\u2026 ${(current.progress * 100).toInt()}%"
                        } else {
                            "Downloading\u2026"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                is GateState.ReadyToInstall -> Button(
                    onClick = {
                        if (AppUpdaterPlatform.canRequestPackageInstalls()) {
                            install(current.path)
                        } else {
                            AppUpdaterPlatform.openUnknownSourcesSettings()
                        }
                    },
                ) {
                    Text("Allow & install")
                }

                GateState.Installing -> {
                    CircularProgressIndicator()
                    Text(
                        text = "Installing\u2026",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                is GateState.Failed -> {
                    Text(
                        text = current.message ?: "Update failed. Please try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    UpdateButton(onClick = { startUpdate() }, label = "Retry")
                }
            }
        }
    }
}

@Composable
private fun UpdateButton(onClick: () -> Unit, label: String = "Update now") {
    Button(onClick = onClick) { Text(label) }
}

private sealed interface GateState {
    data object Idle : GateState
    data class Downloading(val progress: Float?) : GateState
    data class ReadyToInstall(val path: String) : GateState
    data object Installing : GateState
    data class Failed(val message: String?) : GateState
}
