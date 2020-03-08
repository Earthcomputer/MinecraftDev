/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.codeInsight.actions.SimpleCodeInsightAction

abstract class ModalCodeInsightAction : SimpleCodeInsightAction() {

    protected fun invokeLater0(func: () -> Unit) = invokeLater(func)

}
