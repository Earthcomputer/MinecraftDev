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

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class MinecraftFacetDetector : StartupActivity {

    override fun runActivity(project: Project) {
        MinecraftModuleRootListener.doCheck(project)
    }
}
