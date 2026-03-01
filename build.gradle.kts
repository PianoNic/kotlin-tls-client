plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
}

group = "dev.kotlintls"
version = project.findProperty("releaseVersion")?.toString() ?: "dev"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.22")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

// Exclude bundled native libraries from the sources JAR (they're binaries, not source)
tasks.named<Jar>("sourcesJar") {
    exclude("dev/kotlintls/natives/**")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "kotlin-tls-client"
            version = project.version.toString()
        }
    }
}
