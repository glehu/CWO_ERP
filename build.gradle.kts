import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0"
    kotlin("plugin.serialization") version "1.5.0"
}
group = "me.duffy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}
dependencies {
    //testImplementation(kotlin("test-junit"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.2.0")
    implementation("no.tornado:tornadofx:1.7.15")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:0.15.2")
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}