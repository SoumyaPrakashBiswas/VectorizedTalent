plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Lucene modules
    implementation("org.apache.lucene:lucene-core:9.12.2")
    implementation("org.apache.lucene:lucene-analysis-common:9.12.2")
    implementation("org.apache.lucene:lucene-queryparser:9.12.2")
    implementation("org.apache.lucene:lucene-backward-codecs:9.12.2")

    // Kotlinx serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Ktor HTTP client
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Kotlin standard library
    implementation(kotlin("stdlib"))
}



kotlin {
    jvmToolchain(21)
}