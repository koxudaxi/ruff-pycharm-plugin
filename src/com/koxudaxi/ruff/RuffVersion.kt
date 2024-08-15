package com.koxudaxi.ruff

class RuffVersion(private val major: Int, private val minor: Int = 0, private val patch: Int = 0) :
    Comparable<RuffVersion> {
    override fun compareTo(other: RuffVersion): Int =
        when {
            major != other.major -> major - other.major
            minor != other.minor -> minor - other.minor
            else -> patch - other.patch
        }

    val hasFormatter: Boolean get() = this >= SUPPORT_FORMAT_VERSION

    val hasOutputFormat: Boolean get() = this >= SUPPORT_OUTPUT_FORMAT_VERSION

    val hasCheck: Boolean get() = this >= SUPPORT_CHECK_VERSION

    val hasLsp: Boolean get() = this >= SUPPORT_LSP_VERSION
    companion object {
        val SUPPORT_FORMAT_VERSION = RuffVersion(0, 0, 289)
        val SUPPORT_OUTPUT_FORMAT_VERSION = RuffVersion(0, 0, 291)
        val SUPPORT_CHECK_VERSION = RuffVersion(0, 3, 0)
        val SUPPORT_LSP_VERSION = RuffVersion(0, 4, 5)

    }
}
