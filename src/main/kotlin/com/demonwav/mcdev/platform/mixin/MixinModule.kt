/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.buildsystem.SourceType
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import javax.swing.Icon

class MixinModule(facet: MinecraftFacet) : AbstractModule(facet) {

    private val mixinFileType = FileTypeManager.getInstance().findFileTypeByName("Mixin Configuration") ?: FileTypes.UNKNOWN

    override val moduleType = MixinModuleType
    override val type = PlatformType.MIXIN
    override val icon: Icon? = null

    fun getMixinConfigs(project: Project, scope: GlobalSearchScope): Collection<PsiFile> {
        return FileTypeIndex.getFiles(mixinFileType, scope).mapNotNull { PsiManager.getInstance(project).findFile(it) }
    }

}
