package com.nativOS.settings

import android.content.Context

object NativOSPreferences {
    enum class OperatingMode { DESKTOP_APP, HOME_LAUNCHER, DEGOOGLED }

    private const val FILE = "nativos_settings"
    private const val HIDE_SYSTEM_BARS = "hide_system_bars"
    private const val SHOW_ANDROID_APPS = "show_android_apps"
    private const val OPERATING_MODE = "operating_mode"

    fun hideSystemBars(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(HIDE_SYSTEM_BARS, false)

    fun setHideSystemBars(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(HIDE_SYSTEM_BARS, enabled).apply()
    }

    fun showAndroidApps(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(SHOW_ANDROID_APPS, true)

    fun setShowAndroidApps(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(SHOW_ANDROID_APPS, enabled).apply()
    }

    fun operatingMode(context: Context): OperatingMode {
        val stored = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(OPERATING_MODE, OperatingMode.DESKTOP_APP.name)
        val migrated = if (stored == "MINIMAL_ANDROID") OperatingMode.DEGOOGLED.name else stored
        return runCatching { OperatingMode.valueOf(migrated.orEmpty()) }
            .getOrDefault(OperatingMode.DESKTOP_APP)
    }

    fun setOperatingMode(context: Context, mode: OperatingMode) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(OPERATING_MODE, mode.name).apply()
    }

    private const val PREFERRED_DISTRO = "preferred_distro"

    fun preferredDistro(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(PREFERRED_DISTRO, "alpine") ?: "alpine"

    fun setPreferredDistro(context: Context, distro: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(PREFERRED_DISTRO, distro).apply()
    }

    private const val HIGH_REFRESH_RATE = "high_refresh_rate"
    private const val DISPLAY_SCALE = "display_scale"
    private const val GPU_DRIVER = "gpu_driver"
    private const val SYSTEM_LANGUAGE = "system_language"
    private const val SOFTWARE_PROFILE = "software_profile"
    private const val SETUP_WIZARD_COMPLETED = "setup_wizard_completed"

    fun highRefreshRate(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(HIGH_REFRESH_RATE, true)

    fun setHighRefreshRate(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(HIGH_REFRESH_RATE, enabled).apply()
    }

    /** Display scale factor: 0f = auto, or 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f */
    fun displayScale(context: Context): Float =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getFloat(DISPLAY_SCALE, 0f)

    fun setDisplayScale(context: Context, scale: Float) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putFloat(DISPLAY_SCALE, scale).apply()
    }

    fun gpuDriver(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(GPU_DRIVER, "auto") ?: "auto"

    fun setGpuDriver(context: Context, driver: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(GPU_DRIVER, driver).apply()
    }

    fun systemLanguage(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(SYSTEM_LANGUAGE, "system") ?: "system"

    fun setSystemLanguage(context: Context, lang: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(SYSTEM_LANGUAGE, lang).apply()
    }

    fun softwareProfile(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(SOFTWARE_PROFILE, "minimal") ?: "minimal"

    fun setSoftwareProfile(context: Context, profile: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(SOFTWARE_PROFILE, profile).apply()
    }

    private const val DESKTOP_ENVIRONMENT = "desktop_environment"
    private const val TOUCH_MODE = "touch_mode"

    fun desktopEnvironment(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(DESKTOP_ENVIRONMENT, "phosh") ?: "phosh"

    fun setDesktopEnvironment(context: Context, env: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(DESKTOP_ENVIRONMENT, env).apply()
    }

    /** Touch mode: "touch" (Multi-Touch direct 120Hz), "simulated" (Mouse clicks), "trackpad" (Trackpad cursor) */
    fun touchMode(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(TOUCH_MODE, "touch") ?: "touch"

    fun setTouchMode(context: Context, mode: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(TOUCH_MODE, mode).apply()
    }

    fun isSetupWizardCompleted(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(SETUP_WIZARD_COMPLETED, false)

    fun setSetupWizardCompleted(context: Context, completed: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(SETUP_WIZARD_COMPLETED, completed).apply()
    }
}
