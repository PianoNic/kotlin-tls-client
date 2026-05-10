import java.net.HttpURLConnection
import java.net.URI

plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
}

group = "dev.kotlintls"
version = project.findProperty("releaseVersion")?.toString() ?: "dev"

val nativesVersion = file("natives-version.txt").readText().trim()
val nativesDir = layout.buildDirectory.dir("natives")

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.dev.jna:jna:5.16.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.22")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

// ── Download native libraries from fork releases ────────────────────────────

val platforms = listOf(
    "android-arm64-v8a", "android-armeabi-v7a", "android-x86", "android-x86_64",
    "windows-x86_64", "windows-arm64",
    "linux-x86_64", "linux-aarch64", "linux-arm",
    "macos-arm64", "macos-x86_64",
    "freebsd-x86_64"
)

val downloadNatives by tasks.registering {
    description = "Download native Go libraries from PianoNic/kotlin-tls-client-natives releases"
    val outDir = nativesDir
    outputs.dir(outDir)

    doLast {
        val base = "https://github.com/PianoNic/kotlin-tls-client-natives/releases/download/v$nativesVersion"
        val tmpDir = temporaryDir

        platforms.forEach { platform ->
            val zipFile = File(tmpDir, "$platform.zip")
            val targetDir = outDir.get().dir("dev/kotlintls/natives/$platform").asFile

            if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
                logger.lifecycle("Natives already present: $platform")
                return@forEach
            }

            logger.lifecycle("Downloading $platform natives...")
            val url = URI("$base/$platform.zip").toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connect()

            // GitHub redirects to S3 — follow manually if needed
            val realStream = if (conn.responseCode in 301..302) {
                val redirect = conn.getHeaderField("Location")
                URI(redirect).toURL().openStream()
            } else {
                conn.inputStream
            }

            realStream.use { input -> zipFile.outputStream().use { input.copyTo(it) } }

            targetDir.mkdirs()
            copy {
                from(zipTree(zipFile))
                into(targetDir)
            }
        }
    }
}

// ── Tests & packaging ───────────────────────────────────────────────────────

// Native libraries are NOT bundled into the published JAR. Users download the
// binary for their target platform from kotlin-tls-client-natives releases and
// place it where the JVM can find it (java.library.path, jniLibs/ on Android).
// The downloaded natives are still wired onto the test classpath so the test
// suite can load them locally.
tasks.test {
    useJUnitPlatform()
    dependsOn(downloadNatives)
    classpath += files(nativesDir)
}

java {
    withSourcesJar()
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
