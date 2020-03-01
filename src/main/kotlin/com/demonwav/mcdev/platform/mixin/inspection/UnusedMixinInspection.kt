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
import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.platform.mixin.config.reference.MixinClass
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.*
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass

class UnusedMixinInspection : MixinInspection() {

    override fun getStaticDescription() = "Ensures that all mixin classes are referenced from a mixin configuration"

    override fun buildVisitor(holder: ProblemsHolder) = Visitor(holder)

    class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitClass(clazz: PsiClass?) {
            val module = clazz?.findModule() ?: return
            val facet = MinecraftFacet.getInstance(module) ?: return
            val mixinModule = facet.getModuleOfType(MixinModuleType) ?: return
            if (clazz.isMixin) {
                if (mixinModule.getMixinConfigs(module.project, clazz.resolveScope).none fileTester@{ file ->
                    val root = (file as? JsonFile)?.topLevelValue as? JsonObject ?: return@fileTester false
                    return@fileTester referencesClass(root.findProperty("mixins"), clazz) ||
                            referencesClass(root.findProperty("client"), clazz) ||
                            referencesClass(root.findProperty("server"), clazz)
                }) {
                    clazz.nameIdentifier?.let {
                        holder.registerProblem(it, "Mixin not found in any mixin config")
                    }
                }
            }
        }

        private fun referencesClass(property: JsonProperty?, clazz: PsiClass): Boolean {
            val values = property?.value as? JsonArray ?: return false
            return values.valueList.any { v ->
                MixinClass.findClasses(v, clazz.resolveScope).any { it.qualifiedName == clazz.qualifiedName }
            }
        }
    }

}
