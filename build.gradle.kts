plugins {
    idea
    kotlin("jvm") version "1.4.10"
    application
}

apply(plugin="java")
apply(plugin="kotlin")
java.sourceCompatibility = JavaVersion.VERSION_14
java.targetCompatibility = JavaVersion.VERSION_14

repositories {
    mavenCentral()
    jcenter()
}

val http4kVersion: String by project

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    api("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("org.http4k:http4k-core:$http4kVersion")

    // contracts for typesafe lenses and generating swagger
    implementation("org.http4k:http4k-contract:$http4kVersion")
    implementation("org.http4k:http4k-format-jackson:$http4kVersion")

    // http4k servers
    implementation("org.http4k:http4k-server-apache:$http4kVersion")
    implementation("org.http4k:http4k-server-undertow:$http4kVersion")

}
sourceSets.main {
    java.srcDirs("src/main/kotlin", "src/main/kotlin")
}

application{
    mainClassName = ""
}


