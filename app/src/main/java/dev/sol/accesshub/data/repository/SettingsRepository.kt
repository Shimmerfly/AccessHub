package dev.sol.accesshub.data.repository

interface SettingsRepository {
    var uiMode: String
    var checkUpdate: Boolean
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var enablePredictiveBack: Boolean
    var enableBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    var showFloatingBottomBarLabels: Boolean
    var enableNavigationBadge: Boolean
    var hideSystemApps: Boolean
    var serviceDescriptionMaxLines: Int
    var pageScale: Float
}
