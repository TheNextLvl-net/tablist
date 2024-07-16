plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21;
    targetCompatibility = JavaVersion.VERSION_21;
}

group = "net.thenextlvl.tablist"
version = "1.1.2"

repositories {
    mavenCentral()
    maven("https://repo.thenextlvl.net/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("net.luckperms:api:5.4")
    compileOnly("org.projectlombok:lombok:1.18.28")
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")

    implementation("net.thenextlvl.core:files:1.0.5")

    annotationProcessor("org.projectlombok:lombok:1.18.28")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
}

tasks.shadowJar {
    minimize()
}
