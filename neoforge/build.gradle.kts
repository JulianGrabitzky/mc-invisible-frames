plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.148"
}

group = providers.gradleProperty("group_id").get()
version = providers.gradleProperty("mod_version").get()
base.archivesName.set("invisibleframes")
java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

sourceSets.main {
    java.srcDir(rootProject.file("common/src/main/java"))
    resources.srcDir(rootProject.file("common/src/main/resources"))
}

val gametest = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations[gametest.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[gametest.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())

val acceptGameTestEula = tasks.register("acceptGameTestEula") {
    val eula = layout.projectDirectory.file("run-gametest/eula.txt")
    outputs.file(eula)
    doLast {
        eula.asFile.parentFile.mkdirs()
        eula.asFile.writeText("# Accepted for disposable automated GameTest worlds.\neula=true\n")
    }
}

neoForge {
    version = providers.gradleProperty("neo_version").get()
    addModdingDependenciesTo(gametest)
    mods {
        create("invisibleframes") { sourceSet(sourceSets.main.get()) }
        create("invisibleframes_tests") { sourceSet(gametest) }
    }
    runs {
        create("client") {
            client()
            loadedMods.set(listOf(mods["invisibleframes"]))
        }
        create("server") {
            server()
            programArgument("--nogui")
            loadedMods.set(listOf(mods["invisibleframes"]))
        }
        create("gameTestServer") {
            type = "gameTestServer"
            sourceSet = gametest
            gameDirectory = file("run-gametest")
            systemProperty("neoforge.enabledGameTestNamespaces", "invisibleframes")
            taskBefore(acceptGameTestEula)
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    val properties = mapOf("mod_version" to project.version.toString(),
        "neo_version" to providers.gradleProperty("neo_version").get())
    inputs.properties(properties)
    filesMatching("META-INF/neoforge.mods.toml") { expand(properties) }
}

tasks.jar {
    archiveClassifier.set("neoforge")
    from(rootProject.file("LICENSE"))
}
