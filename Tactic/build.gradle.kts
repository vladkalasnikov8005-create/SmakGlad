plugins {
    `java-library`
}

group = "org.examplee"
version = "1.6"
description = "Tactic — Маска, боевые предметы, кальян, ритуальный костёр с голограммой и командами (26.2)"
base {
    archivesName.set("Tactic")
}

java {
    // Paper 26.2 требует Java 25
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

// Берём версию на этапе конфигурации (НЕ во время выполнения таска) — фиксит configuration-cache
val projectVersion = version.toString()

tasks.processResources {
    // Регистрируем входное свойство (для инкрементальной сборки)
    inputs.property("pluginVersion", projectVersion)
    // Один вызов expand() на все ресурсы — без лямбды и без обращения к project внутри
    expand("version" to projectVersion)
}

tasks.jar {
    archiveFileName.set("Tactic.jar")
}
