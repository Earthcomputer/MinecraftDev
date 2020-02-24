/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.openapi.project.Project

fun importGradleProject(projectFilePath: String, project: Project) {
    val clazz = Class.forName("org.jetbrains.plugins.gradle.service.project.open.GradleProjectImportUtil")
    val params: Array<Class<*>?> = arrayOf(String::class.java, Project::class.java)
    try {
        clazz.invokeStatic("linkAndRefreshGradleProject", params, projectFilePath, project)
    } catch (e: NoSuchMethodException) {
        clazz.invokeStatic("importProject", params, projectFilePath, project)
    }
}
