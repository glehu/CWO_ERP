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
    maven {
        name = "komputing/KHash GitHub Packages"
        url = uri("https://maven.pkg.github.com/komputing/KHash")
        credentials {
            username = "token"
            password =
                "\u0039\u0032\u0037\u0034\u0031\u0064\u0038\u0033\u0064\u0036\u0039\u0061\u0063\u0061\u0066\u0031\u0062\u0034\u0061\u0030\u0034\u0035\u0033\u0061\u0063\u0032\u0036\u0038\u0036\u0062\u0036\u0032\u0035\u0065\u0034\u0061\u0065\u0034\u0032\u0062"
        }
    }
    maven { url = uri("https://repo.sultanofcardio.com/artifactory/sultanofcardio") }
}

application {
    mainClassName = "modules.mx.logic.MXStartKt"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("io.ktor:ktor-network-tls-certificates:1.6.6")
    implementation("io.ktor:ktor-server-core:1.6.6")
    implementation("io.ktor:ktor-server-netty:1.6.6")
    implementation("io.ktor:ktor-client-core:1.6.6")
    implementation("io.ktor:ktor-client-cio:1.6.6")
    implementation("io.ktor:ktor-client-auth:1.6.6")
    implementation("io.ktor:ktor-client-serialization:1.6.6")
    implementation("io.ktor:ktor-html-builder:1.6.6")
    implementation("io.ktor:ktor-freemarker:1.6.6")
    implementation("io.ktor:ktor-auth:1.6.6")
    implementation("io.ktor:ktor-auth-jwt:1.6.6")
    implementation("io.ktor:ktor-client-auth:1.6.6")
    implementation("io.ktor:ktor-serialization:1.6.6")
    implementation("io.ktor:ktor-network:1.6.6")
    implementation("ch.qos.logback:logback-classic:1.2.7")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.2.0")
    implementation("com.github.komputing.khash:keccak:1.1.1")
    implementation("com.benasher44:uuid:0.3.1")
    implementation("com.google.api-client:google-api-client:1.32.2")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.32.1")
    implementation("com.google.apis:google-api-services-gmail:v1-rev20211108-1.32.1")
    implementation("com.sultanofcardio:mailman:3.1.2")
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
