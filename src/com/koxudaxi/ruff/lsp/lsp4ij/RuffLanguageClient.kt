package com.koxudaxi.ruff.lsp.lsp4ij

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl

class RuffLanguageClient(project: Project?) : LanguageClientImpl(project)