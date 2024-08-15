import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.lsp4ij)
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    alias(libs.plugins.kotlinSerialization) // Kotlin serialization support
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
    maven { url = uri("https://repo.eclipse.org/content/repositories/lsp4mp-snapshots") }
    maven { url = uri("https://repo.eclipse.org/content/repositories/lsp4mp-releases") }
    intellijPlatform {
        defaultRepositories()
        snapshots()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    compileOnly(libs.tuweni)
    compileOnly(libs.ini4j)
    compileOnly(libs.kotlinxSerialization)
    testImplementation(kotlin("test"))
    intellijPlatform {
        val type = properties("platformType")
        val version = properties("platformVersion")
        val bundledPlugins = properties("platformBundledPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
        create(type, version, useInstaller = false)
        bundledPlugins(bundledPlugins)
        instrumentationTools()
        testFramework(TestFrameworkType.Bundled)
        pluginVerifier()
        zipSigner()
    }
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(17)
        @Suppress("UnstableApiUsage")
        vendor = JvmVendorSpec.JETBRAINS
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
        version = properties("pluginVersion")
        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")

        }
        description =  providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with (it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(project.changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    verifyPlugin {
        ides {
            recommended()
        }
    }
    signing {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = environment("JETBRAINS_TOKEN")
    }
    changelog {
        groups.empty()
        repositoryUrl = properties("pluginRepositoryUrl")
    }
    instrumentCode = true
}


// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath = provider { file(".qodana").canonicalPath }
    reportPath = provider { file("build/reports/inspections").canonicalPath }
    saveReport = true
    showReport = environment("QODANA_SHOW_REPORT").map { it.toBoolean() }.getOrElse(false)
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
koverReport {
    defaults {
        xml {
            onCheck = true
        }
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }
}


sourceSets {
    main {
        java.srcDir("src")
        resources.srcDir("resources")
    }
    test {
        java.srcDir("testSrc")
        resources.srcDir("testData")
    }
}
