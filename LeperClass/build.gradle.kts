plugins {
    `java-library`
    // Запуск тестового Paper-сервера: ./gradlew runServer
    // (3.0.2 работает на Gradle 9.4+; если нужен 3.1.0 — поднимите wrapper до 9.7.0)
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "org.examplee"
version = "1.2.0"
description = "LeperClass — класс прокажённых: заражение, благословение, чих, зонт, предметы и интеграция с PalePlugin"
base {
    archivesName.set("LeperClass")
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
    archiveFileName.set("LeperClass.jar")
}

tasks {
    runServer {
        minecraftVersion("1.21.1")
        // Связка Leper -> Pale (PaleHook вызывает PaleEngine.apiInfect рефлексией):
        // если PalePlugin уже собран, его jar подхватится в тестовый сервер
        // (если jar ещё нет — просто пропустится, Leper запустится сам)
        pluginJars.from(file("../PalePlugin/build/libs/PalePlugin.jar"))
    }
}
