import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.compose") version "1.6.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
}

group = "dev.glycoguide"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

val osName: String = System.getProperty("os.name").lowercase()

val libtorrentPlatform = when {
    osName.contains("linux") -> "linux"
    osName.contains("windows") -> "windows"
    osName.contains("mac") -> "macos"
    else -> "linux"
}

dependencies {
    implementation(project(":common"))
    implementation(project(":cloudstream-compat"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // HTTP Client
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.18.1")

    // BitTorrent streaming
    implementation("org.libtorrent4j:libtorrent4j:2.1.0-39")
    implementation("org.libtorrent4j:libtorrent4j-$libtorrentPlatform:2.1.0-39")
}

compose.desktop {
    application {
        mainClass = "dev.glycoguide.tv.MainKt"

        jvmArgs += listOf(
            "-Djava.awt.headless=false",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
            packageName = "CineStream"
            packageVersion = "1.0.0"
            description = "Desktop streaming client with BitTorrent support"
            vendor = "CineStream"

            modules(
                "java.instrument",
                "java.net.http",
                "java.sql",
                "jdk.unsupported",
            )

            linux {
                debMaintainer = "dev@cinestream.app"
            }
        }
    }
}
