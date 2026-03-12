pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenCentral()
    }
}

rootProject.name = "CineStream"

include(":common")
include(":extensions:vidsrc-provider")

// Include Android module only when Android SDK is available
if (System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null
    || file("local.properties").let { it.exists() && it.readText().contains("sdk.dir") }) {
    include(":android")
}
