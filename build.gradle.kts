import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.30"
    kotlin("plugin.serialization") version "1.5.30"
    application
}
group = "me.duffy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

application {
    mainClassName = "modules.mx.logic.MXStartKt"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("io.ktor:ktor-network-tls-certificates:1.6.3")
    implementation("io.ktor:ktor-server-core:1.6.3")
    implementation("io.ktor:ktor-server-netty:1.6.3")
    implementation("io.ktor:ktor-client-core:1.6.3")
    implementation("io.ktor:ktor-client-cio:1.6.3")
    implementation("io.ktor:ktor-client-auth:1.6.3")
    implementation("io.ktor:ktor-client-serialization:1.6.3")
    implementation("io.ktor:ktor-html-builder:1.6.2")
    implementation("io.ktor:ktor-freemarker:1.6.2")
    implementation("io.ktor:ktor-auth:1.6.3")
    implementation("io.ktor:ktor-client-auth:1.6.3")
    implementation("io.ktor:ktor-serialization:1.6.3")
    implementation("ch.qos.logback:logback-classic:1.2.5")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.1.0")
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}