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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.diagnostic.ReportMessages
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement

private fun before(major: Int, minor: Int): Boolean {
    val maj = ApplicationInfo.getInstance().majorVersion.toInt()
    val min = ApplicationInfo.getInstance().minorVersion.toInt()
    return maj < major || maj == major && min < minor
}

private fun after(major: Int, minor: Int): Boolean {
    val maj = ApplicationInfo.getInstance().majorVersion.toInt()
    val min = ApplicationInfo.getInstance().minorVersion.toInt()
    return maj > major || maj == major && min > minor
}

fun importGradleProject(projectFilePath: String, project: Project) {
    val clazz = Class.forName("org.jetbrains.plugins.gradle.service.project.open.GradleProjectImportUtil")
    val params: Array<Class<*>?> = arrayOf(String::class.java, Project::class.java)
    if (before(2019, 3)) {
        clazz.invokeStatic("importProject", params, projectFilePath, project)
    } else {
        clazz.invokeStatic("linkAndRefreshGradleProject", params, projectFilePath, project)
    }
}

fun getPlugin(pluginId: PluginId?): IdeaPluginDescriptor? {
    return if (before(2020, 1)) {
        val clazz = Class.forName("com.intellij.ide.plugins.PluginManager")
        clazz.invokeStatic("getPlugin", arrayOf(PluginId::class.java), pluginId) as IdeaPluginDescriptor?
    } else {
        val clazz = Class.forName("com.intellij.ide.plugins.PluginManagerCore")
        clazz.invokeStatic("getPlugin", arrayOf(PluginId::class.java), pluginId) as IdeaPluginDescriptor?
    }
}

fun findEditor(element: PsiElement): Editor? {
    return if (before(2020, 1)) {
        val clazz = Class.forName("com.intellij.psi.util.PsiUtilBase")
        clazz.invokeStatic("findEditor", arrayOf(PsiElement::class.java), element) as Editor?
    } else {
        val clazz = Class.forName("com.intellij.psi.util.PsiEditorUtil")
        clazz.invokeStatic("findEditor", arrayOf(PsiElement::class.java), element) as Editor?
    }
}

fun getReportMessagesErrorReport(): String {
    return if (before(2020, 1)) {
        "Error Report"
    } else {
        ReportMessages::class.java.invokeStatic("getErrorReport", arrayOf()) as String
    }
}

fun AnnotationHolder.createErrorAnnotation0(element: PsiElement, message: String?) {
    if (before(2020, 1)) {
        this.invokeVirtual("createErrorAnnotation", arrayOf(PsiElement::class.java, String::class.java), element, message)
    } else {
        this.newAnnotation(HighlightSeverity.ERROR, message).invokeVirtual("create", arrayOf())
    }
}

fun AnnotationHolder.createWarningAnnotation0(element: PsiElement, message: String?, fix: IntentionAction) {
    if (before(2020, 1)) {
        this.invokeVirtual("createWarningAnnotation", arrayOf(PsiElement::class.java, String::class.java), element, message)!!.
                invokeVirtual("registerFix", arrayOf(IntentionAction::class.java), fix)
    } else {
        this.newAnnotation(HighlightSeverity.WARNING, message).
                invokeVirtual("range", arrayOf(PsiElement::class.java), element)!!.
                invokeVirtual("withFix", arrayOf(IntentionAction::class.java), fix)!!.
                invokeVirtual("create", arrayOf())
    }
}

fun AnnotationHolder.createWarningAnnotation0(range: TextRange, message: String?, fix: IntentionAction) {
    if (before(2020, 1)) {
        this.invokeVirtual("createWarningAnnotation", arrayOf(TextRange::class.java, String::class.java), range, message)!!.
                invokeVirtual("registerFix", arrayOf(IntentionAction::class.java), fix)
    } else {
        this.newAnnotation(HighlightSeverity.WARNING, message).
                invokeVirtual("range", arrayOf(TextRange::class.java), range)!!.
                invokeVirtual("withFix", arrayOf(IntentionAction::class.java), fix)!!.
                invokeVirtual("create", arrayOf())
    }
}

fun AnnotationHolder.createEnforcedInfoAnnotation0(element: PsiElement, message: String?, textAttributes: TextAttributes) {
    if (before(2020, 1)) {
        this.invokeVirtual("createInfoAnnotation", arrayOf(PsiElement::class.java, String::class.java), element, message)!!.
                invokeVirtual("setEnforcedTextAttributes", arrayOf(TextAttributes::class.java), textAttributes)
    } else {
        this.newAnnotation(HighlightSeverity.INFORMATION, message).
                invokeVirtual("range", arrayOf(PsiElement::class.java), element)!!.
                invokeVirtual("enforcedTextAttributes", arrayOf(TextAttributes::class.java), textAttributes)!!.
                invokeVirtual("create", arrayOf())
    }
}

fun AnnotationHolder.createInfoAnnotation0(element: PsiElement, message: String?) {
    if (before(2020, 1)) {
        this.invokeVirtual("createInfoAnnotation", arrayOf(PsiElement::class.java, String::class.java), element, message)
    } else {
        this.newAnnotation(HighlightSeverity.INFORMATION, message).
                invokeVirtual("range", arrayOf(PsiElement::class.java), element)!!.
                invokeVirtual("create", arrayOf())
    }
}

fun AnnotationHolder.createInfoAnnotation0(range: TextRange, message: String?, textAttributes: TextAttributesKey) {
    if (before(2020, 1)) {
        this.invokeVirtual("createInfoAnnotation", arrayOf(TextRange::class.java, String::class.java), range, message)!!.
                invokeVirtual("setTextAttributes", arrayOf(TextAttributesKey::class.java), textAttributes)
    } else {
        this.newAnnotation(HighlightSeverity.INFORMATION, message).
                invokeVirtual("range", arrayOf(TextRange::class.java), range)!!.
                invokeVirtual("textAttributes", arrayOf(TextAttributesKey::class.java), textAttributes)!!.
                invokeVirtual("create", arrayOf())
    }
}

private fun AnnotationHolder.newAnnotation(severity: HighlightSeverity, message: String?): Any {
    return if (message == null)
        this.invokeVirtual("newSilentAnnotation", arrayOf(HighlightSeverity::class.java), severity)!!
    else
        this.invokeVirtual("newAnnotation", arrayOf(HighlightSeverity::class.java, String::class.java), severity, message)!!
}

fun ToolWindowManager.registerToolWindow0(id: String, canCloseContent: Boolean, anchor: ToolWindowAnchor): ToolWindow {
    if (before(2020, 1)) {
        this.invokeVirtual("unregisterToolWindow", arrayOf(String::class.java), id)
        return this.invokeVirtual("registerToolWindow", arrayOf(String::class.java, Boolean::class.java, ToolWindowAnchor::class.java), id, canCloseContent, anchor) as ToolWindow
    } else {
        val window = this.getToolWindow(id)
        if (window != null)
            return window
        val rtwtClass = Class.forName("com.intellij.openapi.wm.RegisterToolWindowTask")
        val rtwtcClass = Class.forName("com.intellij.openapi.wm.RegisterToolWindowTask\$Companion")
        val task = rtwtcClass.invokeStatic("closeable", arrayOf(String::class.java, ToolWindowAnchor::class.java), id, anchor)!!
        return this.invokeVirtual("registerToolWindow", arrayOf(rtwtClass), task) as ToolWindow
    }
}

fun javaErrorMessage(key: String, vararg params: Any): String {
    return if (before(2020, 1)) {
        val clazz = Class.forName("com.intellij.codeInsight.daemon.JavaErrorMessages")
        clazz.invokeStatic("message", arrayOf(String::class.java, Array<Any>::class.java), key, params) as String
    } else {
        val clazz = Class.forName("com.intellij.codeInsight.daemon.JavaErrorBundle")
        clazz.invokeStatic("message", arrayOf(String::class.java, Array<Any>::class.java), key, params) as String
    }
}
