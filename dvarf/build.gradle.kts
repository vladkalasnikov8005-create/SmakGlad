plugins {
    java
}

group = "org.examplee"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Важно: для совместимости с Purpur 1.21.x
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
}