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

import com.demonwav.mcdev.creator.MinecraftModuleWizardStep

data class EntryPoint(var name: String, var clazz: String, var interfaces: String) {

    private fun isIdentifierValid(clazz: String): Boolean {
        return (clazz.isNotEmpty()
                && clazz.all { it.isJavaIdentifierPart() }
                && clazz.first().isJavaIdentifierStart()
                && !MinecraftModuleWizardStep.keywords.contains(clazz))
    }

    private fun isPackageValid(pkg: String): Boolean {
        return pkg.trim().split(".").all { isIdentifierValid(it) }
    }

    private fun isInterfacesValid(): Boolean {
        return interfaces.isBlank() || interfaces.split(",").all { isPackageValid(it) }
    }

    val valid: Boolean
        get() = name.isNotBlank() && isPackageValid(clazz) && isInterfacesValid()

}
