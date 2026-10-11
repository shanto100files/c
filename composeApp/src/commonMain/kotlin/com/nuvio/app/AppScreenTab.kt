package com.nuvio.app

import com.nuvio.app.core.ui.NativeNavigationTab

enum class AppScreenTab {
    Home,
    Search,
    Library,
    Downloads,
    Settings,
    ;

    companion object {
        fun fromName(name: String): AppScreenTab =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Home
    }
}

internal fun AppScreenTab.toNativeNavigationTab(): NativeNavigationTab = when (this) {
    AppScreenTab.Home -> NativeNavigationTab.Home
    AppScreenTab.Search -> NativeNavigationTab.Search
    AppScreenTab.Library -> NativeNavigationTab.Library
    AppScreenTab.Settings -> NativeNavigationTab.Settings
    // The native tab bar (Swift, iOS only) declares no Downloads case, so report the
    // closest tab it does understand rather than a value its switches never see.
    // Android draws the Compose bar, which does have this tab.
    AppScreenTab.Downloads -> NativeNavigationTab.Library
}

internal fun NativeNavigationTab.toAppScreenTab(): AppScreenTab = when (this) {
    NativeNavigationTab.Home -> AppScreenTab.Home
    NativeNavigationTab.Search -> AppScreenTab.Search
    NativeNavigationTab.Library -> AppScreenTab.Library
    NativeNavigationTab.Settings -> AppScreenTab.Settings
}
