plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.compileJava {
    options.release.set(21)
}

group = "net.thenextlvl.tablist"
version = "1.1.3"

repositories {
    mavenCentral()
    maven("https://repo.thenextlvl.net/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("net.luckperms:api:5.4")
    compileOnly("org.projectlombok:lombok:1.18.36")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    implementation("net.thenextlvl.core:files:2.0.2")

    annotationProcessor("org.projectlombok:lombok:1.18.36")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}

tasks.shadowJar {
    minimize()
}
