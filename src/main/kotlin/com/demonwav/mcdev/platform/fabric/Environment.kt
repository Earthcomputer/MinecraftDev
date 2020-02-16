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

enum class Environment(val pattern: String) {
    BOTH("*"),
    CLIENT("client"),
    SERVER("server"),
    ;

    val allowClient
        get() = this != SERVER
    val allowServer
        get() = this != CLIENT

    companion object {
        fun byName(name: String?): Environment? {
            return values().firstOrNull { it.name == name }
        }
    }
}
