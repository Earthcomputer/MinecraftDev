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

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.inspection.sideonly.Side
import com.demonwav.mcdev.inspection.sideonly.SideOnlyUtil
import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.platform.mixin.config.reference.MixinClass
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ide.util.EditorHelper
import com.intellij.json.psi.*
import com.intellij.lang.LanguageImportStatements
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class UnusedMixinInspection : MixinInspection() {

    override fun getStaticDescription() = "Ensures that all mixin classes are referenced from a mixin configuration"

    override fun buildVisitor(holder: ProblemsHolder) = Visitor(holder)

    class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitClass(clazz: PsiClass?) {
            val module = clazz?.findModule() ?: return
            val facet = MinecraftFacet.getInstance(module) ?: return
            val mixinModule = facet.getModuleOfType(MixinModuleType) ?: return
            if (clazz.isMixin) {
                var bestQuickFixConfig: PsiFile? = null
                var longestPackageLength = Integer.MIN_VALUE
                for (config in mixinModule.getMixinConfigs(module.project, clazz.resolveScope)) {
                    val root = (config as? JsonFile)?.topLevelValue as? JsonObject ?: continue

                    if (referencesClass(root.findProperty("mixins"), clazz))
                        return
                    if (referencesClass(root.findProperty("client"), clazz))
                        return
                    if (referencesClass(root.findProperty("server"), clazz))
                        return

                    val pkg = (root.findProperty("package")?.value as? JsonStringLiteral)?.value ?: continue
                    if (clazz.qualifiedName?.startsWith("$pkg.") != true) continue
                    if (config.isWritable && pkg.length > longestPackageLength) {
                        bestQuickFixConfig = config
                        longestPackageLength = pkg.length
                    }
                }

                val problematicElement = clazz.nameIdentifier
                if (problematicElement != null) {
                    val bestQuickFixFile = bestQuickFixConfig?.virtualFile
                    val qualifiedName = clazz.qualifiedName
                    if (bestQuickFixFile != null && qualifiedName != null) {
                        val type = when (SideOnlyUtil.getSideForClass(clazz)) {
                            Side.CLIENT -> "client"
                            Side.SERVER -> "server"
                            else -> "mixins"
                        }
                        val quickFix = QuickFix(bestQuickFixFile, qualifiedName, type)
                        holder.registerProblem(problematicElement, "Mixin not found in any mixin config", quickFix)
                    } else {
                        holder.registerProblem(problematicElement, "Mixin not found in any mixin config")
                    }
                }
            }
        }

        private fun referencesClass(property: JsonProperty?, clazz: PsiClass): Boolean {
            val values = property?.value as? JsonArray ?: return false
            return values.valueList.any valueTester@{ v ->
                val pkg = MixinClass.getBasePackage(v) ?: return@valueTester false
                val strVal = v as? JsonStringLiteral ?: return@valueTester false
                "$pkg.${strVal.value}" == clazz.qualifiedName
            }
        }
    }

    private class QuickFix(private val quickFixFile: VirtualFile, private val qualifiedName: String, private val type: String) : LocalQuickFix {
        override fun getName() = "Add to mixin config"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val psiFile = PsiManager.getInstance(project).findFile(quickFixFile) as? JsonFile ?: return
            val root = psiFile.topLevelValue as? JsonObject ?: return
            val pkg = (root.findProperty("package")?.value as? JsonStringLiteral)?.value ?: return
            var mixinArray = root.findProperty(type)?.value as? JsonArray
            if (mixinArray == null) {
                val added = JsonPsiUtil.addProperty(root, JsonElementGenerator(project).createProperty(type, "[]"), false)
                mixinArray = (added as? JsonProperty)?.value as? JsonArray
                if (mixinArray == null)
                    return
            }
            val name = qualifiedName.substring(pkg.length + 1)
            if (mixinArray.valueList.isNotEmpty()) {
                mixinArray.addBefore(JsonElementGenerator(project).createComma(), mixinArray.lastChild)
            }
            mixinArray.addBefore(JsonElementGenerator(project).createStringLiteral(name), mixinArray.lastChild)
            LanguageImportStatements.INSTANCE.forFile(psiFile).forEach { it.processFile(psiFile).run() }
            val addedValue = ((psiFile.topLevelValue as? JsonObject)?.findProperty(type)?.value as? JsonArray)?.valueList?.first { (it as? JsonStringLiteral)?.value == name } ?: psiFile
            EditorHelper.openInEditor(addedValue, true, true)
        }

        override fun getFamilyName() = name
    }

}
