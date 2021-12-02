dependencies {
    api(project(":common"))
    api("org.slf4j:slf4j-simple:2.0.0-alpha5")
}

tasks.withType<Jar> {
    manifest.attributes(
        "Main-Class" to "dev.koding.launcher.bootstrap.BootstrapKt"
    )
}