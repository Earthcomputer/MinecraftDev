/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric

import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.*

object FabricTemplate {

    private fun Properties.setNullable(key: String, value: String?) {
        value?.let {
            if (it.isNotBlank())
                this.setProperty(key, it)
        }
    }

    fun applyBuildGradleTemplate(
        project: Project,
        file: VirtualFile,
        prop: VirtualFile,
        settingsGradle: VirtualFile,
        groupId: String,
        artifactId: String,
        configuration: FabricProjectConfiguration
    ) {
        val properties = Properties()
        properties.setNullable("GROUP_ID", groupId)
        properties.setNullable("ARTIFACT_ID", artifactId)
        properties.setNullable("VERSION", configuration.base?.pluginVersion)
        properties.setNullable("MC_VERSION", configuration.mcVersion)
        properties.setNullable("YARN_MAPPINGS", configuration.yarnVersion)
        properties.setNullable("YARN_CLASSIFIER", configuration.yarnClassifier)
        properties.setNullable("LOADER_VERSION", configuration.loaderVersion)
        properties.setNullable("API_VERSION", configuration.apiVersion)
        properties.setNullable("API_MAVEN_LOCATION", configuration.apiMavenLocation)
        properties.setNullable("LOOM_VERSION", configuration.gradleLoomVersion)

        BaseTemplate.applyTemplate(
            project,
            prop,
            MinecraftFileTemplateGroupFactory.FABRIC_GRADLE_PROPERTIES_TEMPLATE,
            properties
        )

        BaseTemplate.applyTemplate(
            project,
            file,
            MinecraftFileTemplateGroupFactory.FABRIC_BUILD_GRADLE_TEMPLATE,
            properties
        )

        BaseTemplate.applyTemplate(
            project,
            settingsGradle,
            MinecraftFileTemplateGroupFactory.FABRIC_SETTINGS_GRADLE_TEMPLATE,
            properties
        )
    }

    fun applyMainClassTemplate(
        project: Project,
        file: VirtualFile,
        templateName: String,
        packageName: String,
        className: String,
        interfaces: List<String>
    ) {
        val properties = Properties()
        properties.setNullable("PACKAGE_NAME", packageName)
        properties.setNullable("CLASS_NAME", className)
        properties.setNullable("IMPORTS", interfaces
                .map { it.removeRange(it.lastIndexOf('.'), it.length) }
                .distinct()
                .joinToString("\n") { "import $it;" })
        properties.setNullable("INTERFACES", interfaces.distinct()
                .joinToString(", ") { it.substring(it.lastIndexOf('.') + 1) })
        BaseTemplate.applyTemplate(project, file, templateName, properties)
    }

    fun applyModJsonTemplate(
        project: Project,
        file: VirtualFile,
        artifactId: String,
        config: FabricProjectConfiguration,
        baseConfigs: ProjectConfiguration.BaseConfigs
    ) {
        val properties = Properties()
        properties.setNullable("ARTIFACT_ID", artifactId)
        properties.setNullable("MOD_NAME", baseConfigs.pluginName)
        properties.setNullable("MOD_DESCRIPTION", baseConfigs.description)
        if (baseConfigs.authors.size != 0) {
            properties.setNullable("MOD_AUTHORS", baseConfigs.authors.joinToString(", ") { "\"$it\"" })
        }
        properties.setNullable("MOD_HOMEPAGE", baseConfigs.website)
        properties.setNullable("MOD_REPO", config.modRepo)
        properties.setNullable("MOD_ENVIRONMENT", config.environment.pattern)
        val entryPoints = config.entryPoints.groupBy { it.name }.entries.joinToString(",\n") { entry ->
            val elements = entry.value.joinToString(",\n") { "\"${it.clazz}\"" }
            "\"${entry.key}\": [\n$elements\n]"
        }
        if (config.entryPoints.isNotEmpty())
            properties.setNullable("ENTRY_POINTS", entryPoints)
        properties.setNullable("MIXINS", if (config.mixins) "true" else null)
        properties.setNullable("LOADER_VERSION", config.loaderVersion)
        properties.setNullable("API_VERSION", config.apiVersion)
        properties.setNullable("MC_VERSION", config.mcVersion)
        properties.setNullable("NORMALIZED_MC_VERSION", config.mcVersion)

        BaseTemplate.applyTemplate(
            project,
            file,
            MinecraftFileTemplateGroupFactory.FABRIC_MOD_JSON_TEMPLATE,
            properties
        )
    }

    fun applyMixinConfigTemplate(
        project: Project,
        file: VirtualFile,
        packageName: String
    ) {
        val properties = Properties()
        properties.setNullable("PACKAGE_NAME", packageName)
        BaseTemplate.applyTemplate(
            project,
            file,
            MinecraftFileTemplateGroupFactory.FABRIC_MIXINS_JSON_TEMPLATE,
            properties
        )
    }

}
