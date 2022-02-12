plugins {
    id("dev.koding.chimp-launcher.gradle")
}

dependencies {
    api(project(":common"))

    api("com.google.guava:guava:31.0.1-jre")
    api("org.jetbrains.kotlinx:kotlinx-cli:0.3.3")
    api("org.apache.logging.log4j:log4j-core:2.16.0")
    api("org.apache.logging.log4j:log4j-slf4j-impl:2.16.0")
    api("org.apache.commons:commons-text:1.9")

    api("io.ktor:ktor-client-apache:1.6.4")
    api("io.ktor:ktor-client-serialization:1.6.4")
    api("io.ktor:ktor-client-logging:1.6.4")
    api("io.ktor:ktor-server-netty:1.6.4")

    api("org.ow2.asm:asm:9.2")
    api("org.ow2.asm:asm-tree:9.2")
    api("org.ow2.asm:asm-commons:9.2")
    api("org.ow2.asm:asm-util:9.2")
    api("org.ow2.asm:asm-analysis:9.2")
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(zipTree(project(":common").tasks.withType<Jar>().first().archiveFile))

    manifest.attributes(
        "Main-Class" to "dev.koding.launcher.LauncherKt",
//        "Main-Class" to "dev.koding.launcher.custom.FeatherClientKt",
    )
}