dependencies {
    api(project(":common"))

    api("com.google.guava:guava:31.0.1-jre")
    api("com.formdev:flatlaf:1.6.4")
    api("org.jetbrains.kotlinx:kotlinx-cli:0.3.3")
    api("org.apache.logging.log4j:log4j-core:2.14.1")
    api("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")

    api("io.ktor:ktor-client-apache:1.6.4")
    api("io.ktor:ktor-client-serialization:1.6.4")
    api("io.ktor:ktor-client-logging:1.6.4")
    api("io.ktor:ktor-server-netty:1.6.4")
}

tasks.withType<Jar> {
    manifest.attributes(
        "Main-Class" to "dev.koding.launcher.LauncherKt"
    )
}
