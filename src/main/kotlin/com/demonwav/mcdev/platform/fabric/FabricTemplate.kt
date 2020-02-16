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

    fun applyBuildGradleTemplate(
        project: Project,
        file: VirtualFile,
        prop: VirtualFile,
        groupId: String,
        artifactId: String,
        configuration: FabricProjectConfiguration
    ) {
        val properties = Properties()
        properties.setProperty("GROUP_ID", groupId)
        properties.setProperty("ARTIFACT_ID", artifactId)
        properties.setProperty("VERSION", configuration.base?.pluginVersion)
        properties.setProperty("MC_VERSION", configuration.mcVersion)
        properties.setProperty("YARN_MAPPINGS", configuration.yarnVersion)
        properties.setProperty("LOADER_VERSION", configuration.loaderVersion)
        properties.setProperty("API_VERSION", configuration.apiVersion)
        properties.setProperty("LOOM_VERSION", configuration.gradleLoomVersion)

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
    }

    fun applyMainClassTemplate(
        project: Project,
        file: VirtualFile,
        templateName: String,
        packageName: String,
        className: String
    ) {
        val properties = Properties()
        properties.setProperty("PACKAGE_NAME", packageName)
        properties.setProperty("CLASS_NAME", className)
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
        properties.setProperty("ARTIFACT_ID", artifactId)
        properties.setProperty("MOD_NAME", baseConfigs.pluginName)
        properties.setProperty("MOD_DESCRIPTION", baseConfigs.description)
        if (baseConfigs.authors.size != 0) {
            properties.setProperty("MOD_AUTHORS", baseConfigs.authors.map { "\"$it\"" }.joinToString(", "))
        }
        properties.setProperty("MOD_HOMEPAGE", baseConfigs.website)
        properties.setProperty("MOD_REPO", config.modRepo)
        properties.setProperty("MOD_ENVIRONMENT", config.environment.pattern)
        properties.setProperty("CLASS_NAME", config.mainClass)
        properties.setProperty("CLIENT_CLASS_NAME", config.clientClass)
        properties.setProperty("MIXINS", if (config.mixins) "true" else null)
        properties.setProperty("LOADER_VERSION", config.loaderVersion)
        properties.setProperty("API_VERSION", config.apiVersion)
        properties.setProperty("MC_VERSION", config.mcVersion)

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
        properties.setProperty("PACKAGE_NAME", packageName)
        BaseTemplate.applyTemplate(
            project,
            file,
            MinecraftFileTemplateGroupFactory.FABRIC_MIXINS_JSON_TEMPLATE,
            properties
        )
    }

}
