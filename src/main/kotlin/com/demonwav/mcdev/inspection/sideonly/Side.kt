/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.inspection.sideonly

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement

enum class Side(private val sideOnlyAnnotation: String,
                private val onlyInAnnotation: String,
                private val environmentAnnotation: String) {

    CLIENT("SideOnly.CLIENT", "OnlyIn.CLIENT", "Environment.CLIENT"),
    SERVER("SideOnly.SERVER", "OnlyIn.DEDICATED_SERVER", "Environment.SERVER"),
    NONE("NONE", "NONE", "NONE"),
    INVALID("INVALID", "INVALID", "INVALID");

    fun getAnnotation(element: PsiElement): String {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return sideOnlyAnnotation
        val facet = MinecraftFacet.getInstance(module) ?: return sideOnlyAnnotation
        when {
            facet.isOfType(ForgeModuleType) -> {
                val forgeModule = facet.getModuleOfType(ForgeModuleType) ?: return sideOnlyAnnotation
                return if (forgeModule.mcmod == null)
                    onlyInAnnotation
                else
                    sideOnlyAnnotation
            }
            facet.isOfType(FabricModuleType) -> return environmentAnnotation
            else -> return sideOnlyAnnotation
        }
    }

    companion object {
        val KEY = Key<Side>("MC_DEV_SIDE")
    }
}
