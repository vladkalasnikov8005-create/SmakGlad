plugins {
    `java-library`
}

group = "org.examplee"
version = "1.2"
description = "PalePlugin — Бледный лес: заражение биомов, обереги, карты заражения, соль/вода/огниво и админ-команды"
base {
    archivesName.set("PalePlugin")
}

java {
    // Сборка на JDK 25
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    // Байткод уровня Java 21 — как в оригинале; работает и на серверах на Java 25
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

tasks.jar {
    archiveFileName.set("PalePlugin.jar")
}

tasks {
    runServer {
        minecraftVersion("1.21.1")
        // Softdepend в обе стороны: если LeperClass уже собран, его jar
        // тоже подхватится в тестовый сервер (совместимость на одном сервере)
        pluginJars.from(file("../LeperClass/build/libs/LeperClass.jar"))
    }
}
