import io.ktor.plugin.features.*

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
  kotlin("jvm") version "1.9.0"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
  id("io.ktor.plugin") version "2.3.4"
}

group = "coup"
version = "0.0.1"

kotlin {
  jvmToolchain(18)
}

ktor {
  docker {
    externalRegistry.set(DockerImageRegistry.externalRegistry(
      project = provider { "coup" },
      namespace = providers.environmentVariable("IMAGE_NAME"),
      hostname = providers.environmentVariable("REGISTRY"),
      username = providers.environmentVariable("REGISTRY_USERNAME"),
      password = providers.environmentVariable("REGISTRY_PASSWORD"),
    ))
    imageTag.set("latest")
  }
}

application {
  mainClass.set("coup.server.ApplicationKt")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.ktor:ktor-server-core-jvm")
  implementation("io.ktor:ktor-server-websockets-jvm")
  implementation("io.ktor:ktor-server-html-builder-jvm")
  implementation("io.ktor:ktor-server-content-negotiation-jvm")
  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
  implementation("io.ktor:ktor-server-host-common-jvm")
  implementation("io.ktor:ktor-server-netty-jvm")
  implementation("ch.qos.logback:logback-classic:$logback_version")
  testImplementation("io.ktor:ktor-server-tests-jvm")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

