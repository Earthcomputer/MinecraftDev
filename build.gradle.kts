/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

import groovy.util.IndentPrinter
import java.io.StringWriter
import java.util.Properties
import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.MarkupBuilder
import net.minecrell.gradle.licenser.header.HeaderStyle
import org.gradle.internal.jvm.Jvm
import org.jetbrains.intellij.Utils
import org.jetbrains.intellij.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        maven("https://dl.bintray.com/jetbrains/intellij-plugin-service")
    }
}

plugins {
    kotlin("jvm") version "1.3.31" // kept in sync with IntelliJ's bundled dep
    groovy
    idea
    id("org.jetbrains.intellij") version "0.4.15"
    id("net.minecrell.licenser") version "0.4.1"
    id("org.jlleitschuh.gradle.ktlint") version "9.1.1"
    `maven-publish`
}

val myCredentials = Properties()
file("credentials.properties").let { if (it.exists()) myCredentials.load(it.bufferedReader()) }

val ideaMajor: String by project
val ideaMinor: String by project
val ideaVersion: String by project
val ideaSinceBuild: String by project
val ideaUntilBuild: String by project
val mcdevVersion: String by project
val downloadIdeaSources: String by project
val gradleToolingExtensionVersion: String by project
val tomlVersion: String by project
val isPublish: String by project
val desc: String by project
val chNotes: String by project

val projectGroup = "com.demonwav.minecraft-dev"
group = projectGroup
version = "$ideaMajor.$ideaMinor-$mcdevVersion"
if (properties["buildNumber"] != null) {
    version = "$version.${properties["buildNumber"]}"
}
val changeNotes0 = chNotes.replace("\$version", version as? String ?: "")

val coroutineVersion = "1.2.1" // Coroutine version also kept in sync with IntelliJ's bundled dep

defaultTasks("build")

val compileKotlin by tasks.existing
val processResources by tasks.existing<AbstractCopyTask>()
val test by tasks.existing<Test>()
val runIde by tasks.existing<RunIdeTask>()
val buildSearchableOptions by tasks.existing<BuildSearchableOptionsTask>()
val buildPlugin by tasks.existing<Zip>()
val verifyPlugin by tasks.existing<VerifyPluginTask>()
val clean by tasks.existing<Delete>()
val patchPluginXml by tasks.existing<PatchPluginXmlTask>()

// configurations
val idea by configurations
val gradleToolingExtension: Configuration by configurations.creating {
    extendsFrom(idea)
}
val jflex: Configuration by configurations.creating
val jflexSkeleton: Configuration by configurations.creating
val grammarKit: Configuration by configurations.creating
val testLibs: Configuration by configurations.creating {
    isTransitive = false
}

val gradleToolingExtensionSourceSet = sourceSets.create("gradle-tooling-extension") {
    configurations.named(compileOnlyConfigurationName) {
        extendsFrom(gradleToolingExtension)
    }
}
val gradleToolingExtensionJar = tasks.register<Jar>(gradleToolingExtensionSourceSet.jarTaskName) {
    from(gradleToolingExtensionSourceSet.output)
    archiveClassifier.set("gradle-tooling-extension")
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/minecraft-dev/maven")
    maven("https://repo.spongepowered.org/maven")
    maven("https://jetbrains.bintray.com/intellij-third-party-dependencies")
    maven("https://maven.extracraftx.com")
}

// Sources aren't provided through the gradle intellij plugin for bundled libs, use compileOnly to attach them
// but not include them in the output artifact
//
// Kept in a separate block for readability
dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    compileOnly("org.apache.commons:commons-lang3:3.9")
}

dependencies {
    // Add tools.jar for the JDI API
    implementation(files(Jvm.current().toolsJar))

    implementation(files(gradleToolingExtensionJar))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutineVersion") {
        isTransitive = false
    }

    implementation("com.extracraftx.minecraft:TemplateMakerFabric:0.2.1")

    jflex("org.jetbrains.idea:jflex:1.7.0-b7f882a")
    jflexSkeleton("org.jetbrains.idea:jflex:1.7.0-c1fdf11:idea@skeleton")
    grammarKit("org.jetbrains.idea:grammar-kit:1.5.1")

    testLibs("org.jetbrains.idea:mockJDK:1.7-4d76c50")
    testLibs("org.spongepowered:mixin:0.7-SNAPSHOT:thin")
    testLibs("com.demonwav.mcdev:all-types-nbt:1.0@nbt")

    // For non-SNAPSHOT versions (unless Jetbrains fixes this...) find the version with:
    // println(intellij.ideaDependency.buildNumber.substring(intellij.type.length + 1))
    gradleToolingExtension("com.jetbrains.intellij.gradle:gradle-tooling-extension:$gradleToolingExtensionVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.1")
}

intellij {
    // IntelliJ IDEA dependency
    version = ideaVersion
    // Bundled plugin dependencies
    val needRepoSearch = ideaMajor.toInt() >= 2020
    setPlugins(*(listOf(
        "java", "maven", "gradle", "Groovy",
        // needed dependencies for unit tests
        "properties", "junit", if (needRepoSearch) "repository-search" else "",
        // useful to have when running for mods.toml
        "org.toml.lang:$tomlVersion"
    ).filter { it.isNotEmpty() }.toTypedArray()))

    pluginName = "Minecraft Development"
    if (!isPublish.toBoolean())
        updateSinceUntilBuild = true

    downloadSources = downloadIdeaSources.toBoolean()

    sandboxDirectory = project.rootDir.canonicalPath + "/.sandbox"
}

val patchUpdatePlugins = tasks.create("patchUpdatePlugins") {
    val xmlFile = file("updates/updatePlugins-${ideaMajor.substring(2)}$ideaMinor.xml")
    doLast {
        Utils.warn(this, "Writing $xmlFile")
        val sw = StringWriter()
        val ip = IndentPrinter(sw)
        ip.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        val mkpBldr = MarkupBuilder(ip)
        mkpBldr.withGroovyBuilder {
            "plugins" {
                "plugin"(mapOf(
                    "id" to "com.demonwav.minecraft-dev",
                    "url" to "https://dl.bintray.com/earthcomputer/mods/${projectGroup.replace(".", "/")}/minecraft-dev/$version/minecraft-dev-$version.zip",
                    "version" to version
                )) {
                    "idea-version"(mapOf(
                        "since-build" to ideaSinceBuild,
                        "until-build" to ideaUntilBuild
                    ))
                    "name"("Minecraft Development")
                    ip.println()
                    ip.printIndent()
                    ip.print("<description>")
                    ip.incrementIndent()
                    desc.lines().let {
                        ip.println(it[0])
                        for (line in 1 until it.size - 1) {
                            ip.printIndent()
                            ip.println(it[line])
                        }
                        ip.decrementIndent()
                        ip.printIndent()
                        ip.print(it.last())
                    }
                    ip.println("</description>")
                    ip.printIndent()
                    ip.print("<change-notes>")
                    ip.incrementIndent()
                    changeNotes0.lines().let {
                        ip.println(it[0])
                        for (line in 1 until it.size - 1) {
                            ip.printIndent()
                            ip.println(it[line])
                        }
                        ip.decrementIndent()
                        ip.printIndent()
                        ip.print(it.last())
                    }
                    ip.println("</change-notes>")
                }
            }
        }
        xmlFile.writeText(sw.toString())
    }
}

val doPublishPlugin = tasks.create("doPublishPlugin") {
    dependsOn(buildPlugin)
    dependsOn(verifyPlugin)
    dependsOn(patchUpdatePlugins)
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifact(buildPlugin.get()) {
                builtBy(doPublishPlugin)
                artifactId = "minecraft-dev"
            }
        }
    }

    repositories {
        maven {
            url = uri("https://api.bintray.com/maven/earthcomputer/mods/minecraft-dev/")
            credentials {
                username = myCredentials.getProperty("bintrayUser", "foo")
                password = myCredentials.getProperty("bintrayPass", "bar")
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs = listOf("-proc:none")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

tasks.withType<GroovyCompile>().configureEach {
    options.compilerArgs = listOf("-proc:none")
}

if (ideaMajor.toInt() >= 2020) {
    tasks.withType<BuildSearchableOptionsTask>().configureEach {
        // These tasks are failing for some reason with IDEA 2020.1
        enabled = false
    }
}

val prePatchPluginXml = tasks.create("prePatchPluginXml") {
    val pluginXmlFiles = patchPluginXml.get().pluginXmlFiles
    inputs.files("pluginXmlFiles", pluginXmlFiles)
    val dependencyChanges = hashMapOf<String, String>()
    inputs.property("dependencyChanges", dependencyChanges)
    val removeNodes = arrayListOf<String>()
    inputs.property("removePostStartupActivities", removeNodes)
    val destinationDir = File(project.buildDir, "prePatchedPluginXmlFiles")
    outputs.dir(destinationDir)

    doLast {
        pluginXmlFiles.forEach { file ->
            val pluginXml = Utils.parseXml(file) ?: return@doLast

            (pluginXml.get("depends") as? NodeList)?.forEach {
                (it as? Node)?.let { node ->
                    ((node.value() as? String) ?: (node.value() as? NodeList)?.text())?.let { v ->
                        dependencyChanges[v]?.let { newVal ->
                            Utils.warn(this, "Pre-patch plugin.xml: dependency $v -> $newVal")
                        }
                        node.setValue(dependencyChanges.getOrDefault(v, v))
                    }
                }
            }

            fun removeNode(parent: Node, path: String) {
                if (path.contains('/')) {
                    val parts = path.split("/".toRegex(), 2)
                    (parent.get(parts[0]) as? NodeList)?.forEach {
                        (it as? Node)?.let { node -> removeNode(node, parts[1]) }
                    }
                } else {
                    (parent.get(path) as? NodeList)?.forEach {
                        (it as? Node)?.replaceNode(closureOf<Node?> {})
                    }
                }
            }

            for (nodeToRemove in removeNodes) {
                Utils.warn(this, "Pre-patch plugin.xml: deleting all $nodeToRemove")
                removeNode(pluginXml, nodeToRemove)
            }

            PatchPluginXmlTask.writePatchedPluginXml(pluginXml, File(destinationDir, file.name))
        }
    }

    if (ideaMajor.toInt() < 2019 || (ideaMajor.toInt() == 2019 && ideaMinor.toInt() <= 2))
        dependencyChanges["com.intellij.gradle"] = "org.jetbrains.plugins.gradle"
    if (ideaMajor.toInt() < 2020) {
        removeNodes.add("extensions/postStartupActivity")
        removeNodes.add("applicationListeners")
        removeNodes.add("projectListeners")
    } else {
        removeNodes.add("application-components")
        removeNodes.add("project-components")
    }
}

patchPluginXml {
    dependsOn(prePatchPluginXml)
    setPluginXmlFiles(pluginXmlFiles.files.map { File(File(project.buildDir, "prePatchedPluginXmlFiles"), it.name) })
    sinceBuild(ideaSinceBuild)
    untilBuild(ideaUntilBuild)
    pluginDescription(desc)
    changeNotes(changeNotes0)
}

processResources {
    for (lang in arrayOf("", "_en")) {
        from("src/main/resources/messages.MinecraftDevelopment_en_US.properties") {
            rename { "messages.MinecraftDevelopment$lang.properties" }
        }
    }
}

test {
    dependsOn(testLibs)
    useJUnitPlatform()
    doFirst {
        testLibs.resolvedConfiguration.resolvedArtifacts.forEach {
            systemProperty("testLibs.${it.name}", it.file.absolutePath)
        }
    }
    if (JavaVersion.current().isJava9Compatible) {
        jvmArgs(
            "--add-opens", "java.base/java.io=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
            "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED",
            "--add-opens", "java.desktop/javax.swing.plaf.basic=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.font=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.swing=ALL-UNNAMED"
        )
    }
}

idea {
    module {
        generatedSourceDirs.add(file("gen"))
        excludeDirs.add(file(intellij.sandboxDirectory))
    }
}

license {
    header = file("copyright.txt")
    style["flex"] = HeaderStyle.BLOCK_COMMENT.format
    style["bnf"] = HeaderStyle.BLOCK_COMMENT.format

    include(
        "**/*.java",
        "**/*.kt",
        "**/*.kts",
        "**/*.groovy",
        "**/*.gradle",
        "**/*.xml",
        "**/*.properties",
        "**/*.html",
        "**/*.flex",
        "**/*.bnf"
    )
    exclude(
        "com/demonwav/mcdev/platform/mcp/at/gen/**",
        "com/demonwav/mcdev/nbt/lang/gen/**",
        "com/demonwav/mcdev/i18n/lang/gen/**"
    )

    tasks {
        register("gradle") {
            files = project.files("build.gradle.kts", "settings.gradle.kts", "gradle.properties")
        }
        register("grammars") {
            files = project.fileTree("src/main/grammars")
        }
    }
}

tasks.register("format") {
    group = "minecraft"
    description = "Formats source code according to project style"
    val licenseFormat by tasks.existing
    val ktlintFormat by tasks.existing
    dependsOn(licenseFormat, ktlintFormat)
}

// Credit for this intellij-rust
// https://github.com/intellij-rust/intellij-rust/blob/d6b82e6aa2f64b877a95afdd86ec7b84394678c3/build.gradle#L131-L181
fun generateLexer(name: String, flex: String, pack: String) = tasks.register<JavaExec>(name) {
    val src = "src/main/grammars/$flex.flex"
    val dst = "gen/com/demonwav/mcdev/$pack"
    val output = "$dst/$flex.java"

    classpath = jflex
    main = "jflex.Main"

    doFirst {
        args(
            "--skel", jflexSkeleton.singleFile.absolutePath,
            "-d", dst,
            src
        )

        // Delete current lexer
        delete(output)
    }

    inputs.files(src, jflexSkeleton)
    outputs.file(output)
}

fun generatePsiAndParser(name: String, bnf: String, pack: String) = tasks.register<JavaExec>(name) {
    val src = "src/main/grammars/$bnf.bnf".replace('/', File.separatorChar)
    val dstRoot = "gen"
    val dst = "$dstRoot/com/demonwav/mcdev/$pack".replace('/', File.separatorChar)
    val psiDir = "$dst/psi/".replace('/', File.separatorChar)
    val parserDir = "$dst/parser/".replace('/', File.separatorChar)

    doFirst {
        delete(psiDir, parserDir)
    }

    classpath = grammarKit
    main = "org.intellij.grammar.Main"

    if (JavaVersion.current().isJava9Compatible) {
        jvmArgs(
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED"
        )
    }

    args(dstRoot, src)

    inputs.file(src)
    outputs.dirs(
        mapOf(
            "psi" to psiDir,
            "parser" to parserDir
        )
    )
}

val generateAtLexer = generateLexer("generateAtLexer", "AtLexer", "platform/mcp/at/gen/")
val generateAtPsiAndParser = generatePsiAndParser("generateAtPsiAndParser", "AtParser", "platform/mcp/at/gen")

val generateNbttLexer = generateLexer("generateNbttLexer", "NbttLexer", "nbt/lang/gen/")
val generateNbttPsiAndParser = generatePsiAndParser("generateNbttPsiAndParser", "NbttParser", "nbt/lang/gen")

val generateI18nLexer = generateLexer("generateI18nLexer", "I18nLexer", "i18n/lang/gen/")
val generateI18nPsiAndParser = generatePsiAndParser("generateI18nPsiAndParser", "I18nParser", "i18n/lang/gen")

val generateI18nTemplateLexer = generateLexer("generateI18nTemplateLexer", "I18nTemplateLexer", "i18n/lang/gen/")

val generate by tasks.registering {
    group = "minecraft"
    description = "Generates sources needed to compile the plugin."
    dependsOn(
        generateAtLexer,
        generateAtPsiAndParser,
        generateNbttLexer,
        generateNbttPsiAndParser,
        generateI18nLexer,
        generateI18nPsiAndParser,
        generateI18nTemplateLexer
    )
    outputs.dir("gen")
}

sourceSets.named("main") {
    java.srcDir(generate)
    // TODO: (urgently) implement a better way to do this
    if (ideaMajor.toInt() >= 2020) {
        java.srcDir("201src")
    } else {
        java.srcDir("193src")
    }
}

// Remove gen directory on clean
clean { delete(generate) }

runIde {
    maxHeapSize = "2G"

    System.getProperty("debug")?.let {
        systemProperty("idea.ProcessCanceledException", "disabled")
        systemProperty("idea.debug.mode", "true")
    }
}

inline fun <reified T : Task> TaskContainer.existing() = existing(T::class)
inline fun <reified T : Task> TaskContainer.register(name: String, configuration: Action<in T>) =
    register(name, T::class, configuration)
