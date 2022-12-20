package com.koxudaxi.ruff

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.credentialStore.toByteArrayAndClear
import com.intellij.openapi.editor.Document
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.sdk.pythonSdk

class RuffInspection : PyInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ): PsiElementVisitor = Visitor(holder, PyInspectionVisitor.getContext(session))

    inner class Visitor(holder: ProblemsHolder, context: TypeEvalContext) :
        PyInspectionVisitor(holder, context) {
        override fun visitPyFile(node: PyFile) {
            super.visitPyFile(node)

            inspectFile(node)
        }

        private fun inspectFile(pyFile: PyFile) {
            val document = PsiDocumentManager.getInstance(pyFile.project).getDocument(pyFile) ?: return
            val fileName = pyFile.name
            val module = ModuleUtil.findModuleForPsiElement(pyFile) ?: return
            val sdk = module.pythonSdk ?: return
            val args =
                listOf("--exit-zero", "--no-cache", "--no-fix", "--format", "json", "--stdin-filename", fileName, "-")
            val stdin = pyFile.textToCharArray().toByteArrayAndClear()

            val response = executeOnPooledThread(null) {
                runRuff(sdk, stdin, *args.toTypedArray())
            } ?: return
            parseJsonResponse(response).forEach {
                val pyElement = getPyElement(it, pyFile, document) ?: return@forEach
                registerProblem(
                    pyElement,
                    it.message,
                    it.fix?.let { fix ->
                        RuffQuickFix.create(fix, document)
                    })
            }

        }

        private fun getPyElement(result: Result, pyFile: PyFile, document: Document): PyElement? {
            val start = document.getLineStartOffset(result.location.row - 1) + result.location.column - 1
            val end = document.getLineStartOffset(result.endLocation.row - 1) + result.endLocation.column - 1
            return PsiTreeUtil.findElementOfClassAtRange(pyFile, start, end, PyElement::class.java)
        }
    }

}