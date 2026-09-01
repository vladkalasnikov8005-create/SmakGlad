plugins {
    `java-library`
}

group = "org.examplee"
version = "1.0"
description = "Smakenchant — кастомные зачарования (двойной прыжок + повышение макс. уровня ванильных)"
base {
    archivesName.set("Smakenchant")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.+")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveFileName.set("Smakenchant.jar")
}
