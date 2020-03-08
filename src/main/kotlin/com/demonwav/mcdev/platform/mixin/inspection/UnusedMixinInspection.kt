/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.inspection.sideonly.Side
import com.demonwav.mcdev.inspection.sideonly.SideOnlyUtil
import com.demonwav.mcdev.platform.mixin.util.MixinConfig
import com.demonwav.mcdev.platform.mixin.MixinModule
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.*
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

class UnusedMixinInspection : MixinInspection() {

    override fun getStaticDescription() = "Ensures that all mixin classes are referenced from a mixin configuration"

    override fun buildVisitor(holder: ProblemsHolder) = Visitor(holder)

    class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitClass(clazz: PsiClass?) {
            val module = clazz?.findModule() ?: return
            if (clazz.isMixin) {
                var errorMessage = "File type: ${FileTypeManager.getInstance().findFileTypeByName("Mixin Configuration") ?: FileTypes.UNKNOWN}\n"
                errorMessage += "Checked ${FileTypeIndex.getFiles(FileTypeManager.getInstance().findFileTypeByName("Mixin Configuration") ?: FileTypes.UNKNOWN, GlobalSearchScope.moduleScope(module))}\n"

                for (config in MixinModule.getMixinConfigs(module.project, GlobalSearchScope.moduleScope(module))) {
                    errorMessage += "${config.pkg}: ${config.qualifiedMixins}, ${config.qualifiedClient}\n"
                    if (config.qualifiedMixins.any { it == clazz.qualifiedName })
                        return
                    if (config.qualifiedClient.any { it == clazz.qualifiedName })
                        return
                    if (config.qualifiedServer.any { it == clazz.qualifiedName })
                        return
                }

                val bestQuickFixConfig = MixinModule.getBestWritableConfigForMixinClass(module.project, GlobalSearchScope.moduleScope(module), clazz.qualifiedName ?: "")
                errorMessage += "Best config: ${bestQuickFixConfig?.pkg}"
                val problematicElement = clazz.nameIdentifier
                if (problematicElement != null) {
                    val bestQuickFixFile = bestQuickFixConfig?.file
                    val qualifiedName = clazz.qualifiedName
                    if (bestQuickFixFile != null && qualifiedName != null) {
                        val quickFix = QuickFix(bestQuickFixFile, qualifiedName, SideOnlyUtil.getSideForClass(clazz))
                        holder.registerProblem(problematicElement, "Mixin not found in any mixin config\n$errorMessage", quickFix)
                    } else {
                        holder.registerProblem(problematicElement, "Mixin not found in any mixin config\n$errorMessage")
                    }
                }
            }
        }
    }

    private class QuickFix(private val quickFixFile: VirtualFile, private val qualifiedName: String, private val side: Side) : LocalQuickFix {
        override fun getName() = "Add to mixin config"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val psiFile = PsiManager.getInstance(project).findFile(quickFixFile) as? JsonFile ?: return
            val root = psiFile.topLevelValue as? JsonObject ?: return
            val config = MixinConfig(project, root)
            val mixinList = when (side) {
                Side.CLIENT -> config.qualifiedClient
                Side.SERVER -> config.qualifiedServer
                else -> config.qualifiedMixins
            }
            mixinList.add(qualifiedName)
        }

        override fun getFamilyName() = name
    }

}
