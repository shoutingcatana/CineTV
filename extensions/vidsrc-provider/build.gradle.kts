plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "dev.glycoguide.extensions"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Depend on common module for the ContentProvider interface and models
    compileOnly(project(":common"))
    // HTTP + parsing (same versions as main)
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

tasks.jar {
    // Fat JAR with all dependencies baked in
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    // ServiceLoader registration
    manifest {
        attributes["Implementation-Title"] = "VidSrc Provider Extension"
        attributes["Implementation-Version"] = version
    }
}
