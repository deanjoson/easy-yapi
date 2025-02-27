package com.itangcent.idea.utils

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.itangcent.idea.plugin.api.export.ClassExportRuleKeys
import com.itangcent.idea.psi.PsiResource
import com.itangcent.intellij.config.rule.RuleComputer
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.util.ActionUtils
import com.itangcent.intellij.logger.traceError
import org.apache.commons.lang3.StringUtils
import java.io.File

@Singleton
class ModuleHelper {

    @Inject
    private val logger: Logger? = null

    @Inject
    private val project: Project? = null

    @Inject
    private val ruleComputer: RuleComputer? = null

    @Inject
    private val actionContext: ActionContext? = null

    //region find module
    fun findModule(resource: Any): String? {
        return actionContext!!.callInReadUI {
            when (resource) {
                is PsiResource -> findModule(resource.resource() ?: resource.resourceClass()
                ?: return@callInReadUI null)
                is PsiMethod -> findModule(resource)
                is PsiClass -> findModule(resource)
                is PsiFile -> findModule(resource)
                else -> null
            }
        }
    }

    fun findModule(psiMethod: PsiMethod): String? {
        val containingClass = psiMethod.containingClass
        if (containingClass != null) {
            return findModule(containingClass)
        }
        val module = ModuleUtil.findModuleForPsiElement(psiMethod)
        if (module != null) {
            return module.name
        }
        return null
    }

    fun findModule(cls: PsiClass): String? {

        val moduleByRule = ruleComputer!!.computer(ClassExportRuleKeys.MODULE, cls)

        if (!moduleByRule.isNullOrBlank()) {
            return moduleByRule
        }

        val module = ModuleUtil.findModuleForPsiElement(cls)
        if (module != null) {
            return module.name
        }

        return findModule(cls.containingFile)
    }

    fun findModule(psiFile: PsiFile): String? {
        var module = ModuleUtil.findModuleForPsiElement(psiFile)
        if (module != null) {
            return module.name
        }
        module = ModuleUtil.findModuleForFile(psiFile.virtualFile, project!!)
        if (module != null) {
            return module.name
        }
        val currentPath = ActionUtils.findCurrentPath(psiFile)
        return findModuleByPath(currentPath)
    }

    fun findModuleByPath(path: String?): String? {
        if (path == null) return null
        var module: String? = null
        try {
            var currentPath = path
            when {
                currentPath.contains(src) -> currentPath = StringUtils.substringBefore(currentPath, src)
                currentPath.contains(main) -> currentPath = StringUtils.substringBefore(currentPath, main)
                currentPath.contains(java) -> currentPath = StringUtils.substringBefore(currentPath, java)
                currentPath.contains(kotlin) -> currentPath = StringUtils.substringBefore(currentPath, kotlin)
            }
            module = StringUtils.substringAfterLast(currentPath, File.separator)
        } catch (e: Exception) {
            logger!!.traceError("error in findCurrentPath",e)

        }
        return module

    }
    //endregion

    companion object {
        private val src = "${File.separator}src${File.separator}"
        private val main = "${File.separator}main${File.separator}"
        private val java = "${File.separator}java${File.separator}"
        private val kotlin = "${File.separator}kotlin${File.separator}"

    }
}