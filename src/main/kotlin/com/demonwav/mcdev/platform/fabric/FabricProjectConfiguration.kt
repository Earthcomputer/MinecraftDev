/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

@file:Suppress("Duplicates")

package com.demonwav.mcdev.platform.fabric

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

class FabricProjectConfiguration : ProjectConfiguration() {

    var yarnVersion = ""
    var mcVersion = ""
    var loaderVersion = "0.7.8+build.184"
    var apiVersion: String? = null
    var gradleLoomVersion = "0.2.6"
    var environment = Environment.BOTH
    var mainClass: String? = null
    var clientClass: String? = null
    var modRepo: String? = null
    var mixins = false

    override var type = PlatformType.FABRIC

    override fun create(project: Project, buildSystem: BuildSystem, indicator: ProgressIndicator) {
        if (project.isDisposed) {
            return
        }

        val baseConfig = base ?: return
        val dirs = buildSystem.directories ?: return

        runWriteTask {
            indicator.text = "Writing main class"

            val mainClassFile = writeClass(project, dirs, mainClass, MinecraftFileTemplateGroupFactory.FABRIC_MAIN_CLASS_TEMPLATAE)
            val clientClassFile = writeClass(project, dirs, clientClass, MinecraftFileTemplateGroupFactory.FABRIC_CLIENT_CLASS_TEMPLATE)

            FabricTemplate.applyModJsonTemplate(
                project,
                dirs.resourceDirectory.findOrCreateChildData(this, FabricConstants.FABRIC_MOD_JSON),
                buildSystem.artifactId,
                this,
                baseConfig
            )

            if (mixins) {
                val packageName = (mainClass ?: clientClass)
                        ?.let { it.removeRange(it.lastIndexOf('.'), it.length) }
                        ?: "${buildSystem.groupId}.${buildSystem.artifactId}"

                FabricTemplate.applyMixinConfigTemplate(
                    project,
                    dirs.resourceDirectory.findOrCreateChildData(this, "${buildSystem.artifactId}.mixins.json"),
                    packageName
                )

                getOrCreateDirectories(packageName.split(".").toTypedArray(), dirs.sourceDirectory)
            }

            (mainClassFile ?: clientClassFile)?.let { file ->
                PsiManager.getInstance(project).findFile(file)?.let {mainClassPsi ->
                    EditorHelper.openInEditor(mainClassPsi)
                }
            }
        }
    }

    private fun writeClass(
        project: Project,
        dirs: BuildSystem.DirectorySet,
        clazz: String?,
        templateName: String
    ): VirtualFile? {
        if (clazz == null)
            return null

        var file = dirs.sourceDirectory
        val files = clazz.split(".").toTypedArray()
        val className = files.last()
        val packageName = clazz.substring(0, clazz.length - className.length - 1)
        file = getOrCreateDirectories(files, file)

        val mainClassFile = file.findOrCreateChildData(this, "$className.java")
        FabricTemplate.applyMainClassTemplate(project, mainClassFile, templateName, packageName, className)

        return mainClassFile
    }

    override fun setupDependencies(buildSystem: BuildSystem) {}

}
