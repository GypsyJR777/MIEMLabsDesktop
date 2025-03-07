import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.github.gypsyjr777"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jogamp.org/deployment/maven")
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("io.ktor:ktor-server-status-pages:3.1.0")
    implementation("io.ktor:ktor-server-core-jvm:3.1.0")
    implementation("io.ktor:ktor-server-netty-jvm:3.1.0")
    implementation("io.ktor:ktor-server-config-yaml:3.1.0")
    implementation("io.ktor:ktor-client-core:3.1.0")
    implementation("io.ktor:ktor-client-cio:3.1.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.0")
    implementation("io.ktor:ktor-serialization-jackson:3.1.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("io.ktor:ktor-client-cio-jvm:3.1.0")
//    implementation("io.ktor:ktor-server-auth:3.1.0")
//    implementation("io.github.kevinnzou:compose-webview-multiplatform:1.9.40")
//    implementation("io.github.kevinnzou:compose-webview-multiplatform:1.9.40")
//    implementation("dev.datlag:kcef:$version")
//    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha08")
}

compose.desktop {
    application {
        mainClass = "com.github.gypsyjr777.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe, TargetFormat.Rpm)
            packageName = "MIEMLabsDesktop"
            packageVersion = "1.0.0"
        }

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED") // recommended but not necessary

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}
