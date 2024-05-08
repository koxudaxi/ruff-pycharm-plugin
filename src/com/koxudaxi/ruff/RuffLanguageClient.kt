package com.koxudaxi.ruff

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl

class RuffLanguageClient(project: Project?) : LanguageClientImpl(project)