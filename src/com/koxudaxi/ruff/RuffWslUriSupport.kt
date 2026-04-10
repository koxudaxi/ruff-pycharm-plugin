package com.koxudaxi.ruff

import java.net.URI

private val wslHosts = setOf("wsl.localhost", "wsl$")

internal fun buildWslFileUri(path: String): String? {
    val normalizedPath = path.replace('\\', '/')
    val host = wslHosts.firstOrNull { normalizedPath.startsWith("//$it/") } ?: return null
    val uriPath = normalizedPath.removePrefix("//$host/").trimStart('/')
    return "file://$host/$uriPath"
}

internal fun buildWslUncPath(fileUri: String): String? {
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
