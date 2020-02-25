/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.util.localFile
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.JarFile

class FabricPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(FABRIC_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.FABRIC_ICON

    override fun detect(classesRoots: MutableList<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            JarFile(classesRoot.localFile).use { jar ->
                if (jar.entries().asSequence().any { it.name.startsWith("net/fabricmc/") && it.name.endsWith(".class") })
                    return LibraryVersionProperties()
            }
        }
        return null
    }
}
