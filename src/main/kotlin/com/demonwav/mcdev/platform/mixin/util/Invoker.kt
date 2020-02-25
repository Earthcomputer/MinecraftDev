/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findMatchingMethod
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.Contract
import java.util.*

@Contract(pure = true)
fun PsiMember.findInvokerTarget(): PsiMember? {
    val accessor = findAnnotation(MixinConstants.Annotations.INVOKER) ?: return null
    val containingClass = containingClass ?: return null
    val targetClasses = containingClass.mixinTargets.ifEmpty { return null }
    return resolveInvokerTarget(accessor, targetClasses, this)
}

@Contract(pure = true)
fun resolveInvokerTarget(
        invoker: PsiAnnotation,
        targetClasses: Collection<PsiClass>,
        member: PsiMember
): PsiMember? {
    val name = getInvokerTargetName(invoker, member) ?: return null
    return when (member) {
        is PsiMethod -> targetClasses.mapFirstNotNull { it.findMatchingMethod(member, false, name) }
        else -> null
    }
}

fun getInvokerTargetName(invoker: PsiAnnotation, member: PsiMember): String? {
    val value = invoker.findDeclaredAttributeValue("value")?.constantStringValue
    if (value != null)
        return value

    val memberName = member.name ?: return null
    val result = PATTERN.matchEntire(memberName) ?: return null
    val prefix = result.groupValues[1]
    if (prefix == "new" || prefix == "create")
        return "<init>"
    val name = result.groupValues[2]
    if (name.toUpperCase(Locale.ROOT) != name)
        return name.decapitalize()
    return name
}

private val PATTERN = Regex("(call|invoke|new|create)([A-Z].*?)(_\\\$md.*)?")
