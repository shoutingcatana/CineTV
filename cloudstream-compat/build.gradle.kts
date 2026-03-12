plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":common"))

    // HTTP Client (needed by CloudStream extensions)
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // OkHttp (many CloudStream extensions depend on it)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // HTML parsing
    implementation("org.jsoup:jsoup:1.18.1")

    // Serialization + Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}
