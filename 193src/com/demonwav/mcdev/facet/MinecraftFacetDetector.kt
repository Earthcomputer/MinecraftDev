/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.util.AbstractProjectComponent
import com.intellij.ProjectTopics
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager

class MinecraftFacetDetector(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        val manager = StartupManager.getInstance(project)
        val connection = project.messageBus.connect()

        manager.registerStartupActivity {
            MinecraftModuleRootListener.doCheck(project)
        }

        // Register a module root listener to check when things change
        manager.registerPostStartupActivity {
            connection.subscribe(ProjectTopics.PROJECT_ROOTS, MinecraftModuleRootListener)
        }
    }

}
