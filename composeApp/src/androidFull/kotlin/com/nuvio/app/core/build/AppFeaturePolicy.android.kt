package com.nuvio.app.core.build

import com.nuvio.app.core.site.OtaRepository

// Each flag is OTA-overridable: the panel's `features.<name>` patch wins when set, otherwise the
// built-in default below applies untouched. That lets the operator flip a feature on installed
// apps without shipping a new APK. Reads are cheap (a cached StateFlow value) and a missing or
// malformed patch always falls through to the exact previous behaviour, so a bad patch can never
// change anything the panel did not explicitly name.
actual object AppFeaturePolicy {
    actual val pluginsEnabled: Boolean
        get() = OtaRepository.boolAt("features", "pluginsEnabled", true)
    actual val supportersContributorsPageEnabled: Boolean
        get() = OtaRepository.boolAt("features", "supportersContributorsPageEnabled", true)
    actual val donationActionsEnabled: Boolean
        get() = OtaRepository.boolAt("features", "donationActionsEnabled", true)
    actual val donationProgressEnabled: Boolean
        get() = OtaRepository.boolAt("features", "donationProgressEnabled", false)
    actual val accountDeletionEnabled: Boolean
        get() = OtaRepository.boolAt("features", "accountDeletionEnabled", false)
    actual val personalMediaAddonCopyEnabled: Boolean
        get() = OtaRepository.boolAt("features", "personalMediaAddonCopyEnabled", false)
    actual val p2pEnabled: Boolean
        get() = OtaRepository.boolAt("features", "p2pEnabled", true)
    // Not a boolean, so it stays a fixed build-time choice rather than an OTA switch.
    actual val trailerPlaybackMode: TrailerPlaybackMode = TrailerPlaybackMode.IN_APP
    actual val heroTrailerPlaybackSupported: Boolean
        get() = OtaRepository.boolAt("features", "heroTrailerPlaybackSupported", true)
    actual val inAppUpdaterEnabled: Boolean
        get() = OtaRepository.boolAt("features", "inAppUpdaterEnabled", true)
    actual val imdbRatingLogoEnabled: Boolean
        get() = OtaRepository.boolAt("features", "imdbRatingLogoEnabled", true)
    actual val mediaPlaybackForegroundServiceEnabled: Boolean
        get() = OtaRepository.boolAt("features", "mediaPlaybackForegroundServiceEnabled", true)
    actual val downloadForegroundServiceEnabled: Boolean
        get() = OtaRepository.boolAt("features", "downloadForegroundServiceEnabled", true)
    actual val customServerConnectionsEnabled: Boolean
        get() = OtaRepository.boolAt("features", "customServerConnectionsEnabled", true)
}
