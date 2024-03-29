plugins {
  kotlin("jvm") version "1.7.20"
  kotlin("plugin.serialization") version "1.7.20"
  application
}
group = "me.duffy"
version = "1.5.0"

repositories {
  mavenCentral()
  gradlePluginPortal()
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
  mainClass.set("modules.mx.logic.StartKt")
}

dependencies {
  implementation("ch.qos.logback:logback-classic:1.4.6")
  implementation("com.benasher44:uuid:0.7.0")
  implementation("com.github.ajalt.mordant:mordant:2.0.0-beta4")
  implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.8.0")
  implementation("com.github.komputing.khash:keccak:1.1.1")
  implementation("com.google.firebase:firebase-admin:8.2.0")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
  implementation("com.ionspin.kotlin:bignum:0.3.8")
  implementation("com.sultanofcardio:mailman:3.1.2")
  implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")
  implementation("io.ktor:ktor-server-auth-jwt:2.2.4")
  implementation("io.ktor:ktor-server-auth:2.1.2")
  implementation("io.ktor:ktor-server-content-negotiation:2.2.4")
  implementation("io.ktor:ktor-server-core-jvm:2.2.4")
  implementation("io.ktor:ktor-server-cors:2.2.4")
  implementation("io.ktor:ktor-server-double-receive:2.2.4")
  implementation("io.ktor:ktor-server-freemarker:2.2.4")
  implementation("io.ktor:ktor-server-html-builder:2.2.4")
  implementation("io.ktor:ktor-server-http-redirect:2.2.4")
  implementation("io.ktor:ktor-server-netty-jvm:2.2.4")
  implementation("io.ktor:ktor-server-websockets:2.2.4")
  implementation("org.imgscalr:imgscalr-lib:4.2")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.5.0")
  implementation("io.ktor:ktor-server-auto-head-response-jvm:2.2.4")
}

val mainClassName = "modules.mx.logic.StartKt"
tasks.jar {
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
  manifest {
    attributes(mapOf("Main-Class" to mainClassName))
  }
  exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")

  from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
