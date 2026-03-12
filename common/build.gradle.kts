plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "dev.glycoguide"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // BitTorrent streaming (used via reflection, graceful fallback)
    implementation("org.libtorrent4j:libtorrent4j:2.1.0-39")
}
