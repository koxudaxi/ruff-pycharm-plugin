package com.koxudaxi.ruff

import com.intellij.lang.ImportOptimizer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.PyUtil

class RuffImportOptimizer : ImportOptimizer {
    override fun supports(psiFile: PsiFile): Boolean = psiFile.isApplicableTo

    override fun processFile(psiFile: PsiFile): Runnable {
        val project = psiFile.project
        val sourceFile = psiFile.sourceFile
        val optimizeImportsArgs = project.OPTIMIZE_IMPORTS_ARGS

        return Runnable {
            val stdin = sourceFile.asCurrentTextStdin ?: return@Runnable
            runRuffInBackground(project, stdin, project.OPTIMIZE_IMPORTS_ARGS, "Optimize Imports with Ruff") {
                val result = executeOnPooledThread(project, null as String?) {
                    runRuff(sourceFile, optimizeImportsArgs)
                }

                if (result != null && result.isNotBlank()) {
                    ApplicationManager.getApplication().invokeLater {
                        CommandProcessor.getInstance().executeCommand(
                            project,
                            {
                                ApplicationManager.getApplication().runWriteAction {
                                    PyUtil.updateDocumentUnblockedAndCommitted(
                                        psiFile
                                    ) { document ->
                                        if (document.text != result) {
                                            document.setText(result)
                                        }
                                    }
                                }
                            },
                            "Ruff: Optimize Imports",
                            null
                        )
                    }
                }
            }
        }
    }
}