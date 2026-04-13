package com.koxudaxi.ruff

import java.net.URI

private val wslHosts = setOf("wsl.localhost", "wsl$")

internal fun buildWslFileUri(path: String): String? {
    val normalizedPath = path.replace('\\', '/')
    val host = wslHosts.firstOrNull { normalizedPath.startsWith("//$it/") } ?: return null
    val uriPath = "/${normalizedPath.removePrefix("//$host/").trimStart('/')}"
    return runCatching {
        when {
            '$' in host -> "file://$host${URI(null, null, uriPath, null).rawPath}"
            else -> URI("file", host, uriPath, null).toASCIIString()
        }
    }.getOrNull()
}

internal fun buildWslUncPath(fileUri: String): String? {
    wslHosts.forEach { host ->
        listOf("file://$host/", "file:///$host/").forEach { prefix ->
            if (fileUri.startsWith(prefix)) {
                val encodedPath = fileUri.removePrefix(prefix)
                val decodedPath = runCatching { URI("file://localhost/$encodedPath").path.removePrefix("/") }
                    .getOrDefault(encodedPath)
                return "//$host/$decodedPath"
            }
        }
    }
    val normalizedUri = when {
        fileUri.startsWith("file:///wsl.localhost/") -> "file://wsl.localhost/${fileUri.removePrefix("file:///wsl.localhost/")}"
        fileUri.startsWith("file:///wsl$/") -> "file://wsl$/${fileUri.removePrefix("file:///wsl$/")}"
        else -> fileUri
    }
    val uri = runCatching { URI(normalizedUri) }.getOrNull() ?: return null
    val host = uri.host?.takeIf { it in wslHosts } ?: return null
    val path = uri.path?.removePrefix("/") ?: return null
    return "//$host/$path"
}

internal fun toWindowsWslUncPath(path: String): String? =
    path.replace('\\', '/')
        .takeIf { normalizedPath -> wslHosts.any { normalizedPath.startsWith("//$it/") } }
        ?.replace('/', '\\')
