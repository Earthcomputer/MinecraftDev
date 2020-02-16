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

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.buildsystem.SourceType
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.nullable
import com.intellij.psi.*

class FabricModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet) {

    var fabricJson by nullable { facet.findFile(FabricConstants.FABRIC_MOD_JSON, SourceType.RESOURCE) }
        private set

    override val moduleType = FabricModuleType
    override val type = PlatformType.FABRIC
    override val icon = PlatformAssets.FABRIC_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) = true

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) = ""

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        if (element !is PsiIdentifier) {
            return false
        }

        val psiClass = (element.parent as? PsiClass) ?: return false

        val interfaces = psiClass.interfaces
        return interfaces.any { it.qualifiedName == FabricConstants.MOD_INITIALIZER || it.qualifiedName == FabricConstants.CLIENT_MOD_INITIALIZER }
    }

    override fun dispose() {
        super.dispose()
        fabricJson = null
    }
}
