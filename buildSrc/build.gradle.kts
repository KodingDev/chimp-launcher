plugins {
    `kotlin-dsl`
}

repositories {
    maven { url = project.uri("../assets/maven") }
    mavenCentral()
}

dependencies {
    implementation("dev.koding.launcher:bootstrap:LOCAL") {
        exclude(group = "org.jetbrains.kotlin")
    }
}