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
import com.demonwav.mcdev.platform.fabric.FabricConstants
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Pair
import com.intellij.psi.*
import java.util.Arrays
import java.util.LinkedList

object SideOnlyUtil {

    fun beginningCheck(element: PsiElement): Boolean {
        // We need the module to get the MinecraftModule
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false

        // Check that the MinecraftModule
        //   1. Exists
        //   2. Is a ForgeModuleType or FabricModuleType
        val facet = MinecraftFacet.getInstance(module) ?: return false
        return when {
            facet.isOfType(ForgeModuleType) -> facet.getModuleOfType(ForgeModuleType)?.mcmod != null
            facet.isOfType(FabricModuleType) -> true
            else -> false
        }
    }

    private fun getAmbientSide(element: PsiElement): Side {
        if (!element.isWritable)
            return Side.NONE
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return Side.NONE
        val facet = MinecraftFacet.getInstance(module) ?: return Side.NONE
        when {
            facet.isOfType(ForgeModuleType) -> return Side.NONE
            facet.isOfType(FabricModuleType) -> {
                val fabJsonFile = facet.getModuleOfType(FabricModuleType)?.fabricJson ?: return Side.NONE
                val fabJson = PsiManager.getInstance(module.project).findFile(fabJsonFile) as? JsonFile ?: return Side.NONE
                val environment = ((fabJson.topLevelValue as? JsonObject)?.findProperty("environment")?.value as? JsonStringLiteral)?.value
                @Suppress("MoveVariableDeclarationIntoWhen")
                return when (environment) {
                    "client" -> Side.CLIENT
                    "server" -> Side.SERVER
                    else -> Side.NONE
                }
            }
            else -> return Side.NONE
        }
    }

    fun getSideOnlyName(element: PsiElement): String {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return "SideOnly"
        val facet = MinecraftFacet.getInstance(module) ?: return "SideOnly"
        when {
            facet.isOfType(ForgeModuleType) -> {
                val forgeModule = facet.getModuleOfType(ForgeModuleType) ?: return "SideOnly"
                return if (forgeModule.mcmod == null)
                    "OnlyIn"
                else
                    "SideOnly"
            }
            facet.isOfType(FabricModuleType) -> return "Environment"
            else -> return "SideOnly"
        }
    }

    private fun normalize(text: String): String {
        for (annotation in arrayOf(ForgeConstants.SIDE_ANNOTATION,
                                   ForgeConstants.DIST_ANNOTATION,
                                   FabricConstants.ENV_TYPE_ANNOTATION)) {
            if (text.startsWith(annotation)) {
                // Remove the package
                return text.substring(annotation.lastIndexOf(".") + 1)
            }
        }
        return text
    }

    fun checkMethod(method: PsiMethod): Side {
        val methodAnnotation =
            // It's not annotated, which would be invalid if the element was annotated
            getSideAnnotation(method.modifierList)
            // (which, if we've gotten this far, is true)
                ?: return getAmbientSide(method)

        // Check the value of the annotation
        val methodValue =
            // The annotation has no value yet, IntelliJ will give it's own error because a value is required
            methodAnnotation.findAttributeValue("value")
                ?: return Side.INVALID

        // Return the value of the annotation
        return getFromName(methodValue.text)
    }

    fun checkElementInMethod(element: PsiElement): Side {
        var changingElement = element
        // Maybe there is a better way of doing this, I don't know, but crawl up the PsiElement stack in search of the
        // method this element is in. If it's not in a method it won't find one and the PsiMethod will be null
        var method: PsiMethod? = null
        while (method == null && changingElement.parent != null) {
            val parent = changingElement.parent

            if (parent is PsiMethod) {
                method = parent
            } else {
                changingElement = parent
            }

            if (parent is PsiClass) {
                break
            }
        }

        // No method was found
        if (method == null) {
            return Side.INVALID
        }

        return checkMethod(method)
    }

    fun checkClassHierarchy(psiClass: PsiClass): List<Pair<Side, PsiClass>> {
        val classList = LinkedList<PsiClass>()
        classList.add(psiClass)

        var parent: PsiElement = psiClass
        while (parent.parent != null) {
            parent = parent.parent

            if (parent is PsiClass) {
                classList.add(parent)
            }
        }

        // We want to use an array list so indexing into the list is not expensive
        return classList.map { checkClass(it) }
    }

    fun getSideForClass(psiClass: PsiClass): Side {
        return getFirstSide(checkClassHierarchy(psiClass))
    }

    private fun checkClass(psiClass: PsiClass): Pair<Side, PsiClass> {
        val side = psiClass.getUserData(Side.KEY)
        if (side != null) {
            return Pair(side, psiClass)
        }

        val modifierList = psiClass.modifierList ?: return Pair(getAmbientSide(psiClass), psiClass)

        // Check for the annotation, if it's not there then we return none, but this is
        // usually irrelevant for classes
        val annotation = getSideAnnotation(modifierList)
        if (annotation == null) {
            if (psiClass.supers.isEmpty()) {
                return Pair(getAmbientSide(psiClass), psiClass)
            }

            // check the classes this class extends
            return psiClass.supers.asSequence()
                .filter {
                    // Prevent stack-overflow on cyclic dependencies
                    psiClass != it
                }
                .map { checkClassHierarchy(it) }
                .firstOrNull { it.isNotEmpty() }?.let { Pair(it[0].getFirst(), psiClass) } ?: Pair(getAmbientSide(psiClass), psiClass)
        }

        // Check the value on the annotation. If it's not there, IntelliJ will throw
        // it's own error
        val value = annotation.findAttributeValue("value") ?: return Pair(Side.INVALID, psiClass)

        return Pair(getFromName(value.text), psiClass)
    }

    fun checkField(field: PsiField): Side {
        // We check if this field has the @SideOnly annotation we are looking for
        // If it doesn't, we aren't worried about it
        val modifierList = field.modifierList ?: return getAmbientSide(field)
        val annotation = getSideAnnotation(modifierList) ?: return getAmbientSide(field)

        // The value may not necessarily be set, but that will give an error by default as "value" is a
        // required value for @SideOnly
        val value = annotation.findAttributeValue("value") ?: return Side.INVALID

        // Finally, get the value of the SideOnly
        return SideOnlyUtil.getFromName(value.text)
    }

    fun getSideAnnotation(modifierList: PsiAnnotationOwner): PsiAnnotation? {
        return modifierList.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION) ?:
                modifierList.findAnnotation(ForgeConstants.ONLY_IN_ANNOTATION) ?:
                modifierList.findAnnotation(FabricConstants.ENVIRONMENT_ANNOTATION)
    }

    private fun getFromName(name: String): Side {
        return when (normalize(name)) {
            "Side.SERVER" -> Side.SERVER
            "Side.CLIENT" -> Side.CLIENT
            "Dist.DEDICATED_SERVER" -> Side.SERVER
            "Dist.CLIENT" -> Side.CLIENT
            "EnvType.SERVER" -> Side.SERVER
            "EnvType.CLIENT" -> Side.CLIENT
            else -> Side.INVALID
        }
    }

    fun getFirstSide(list: List<Pair<Side, PsiClass>>): Side {
        return list.firstOrNull { it.first !== Side.NONE }?.first ?: Side.NONE
    }

    fun <T : Any?> getSubArray(infos: Array<T>): Array<T> {
        return Arrays.copyOfRange(infos, 1, infos.size - 1)
    }
}
