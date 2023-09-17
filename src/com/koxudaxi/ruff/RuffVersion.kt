package com.koxudaxi.ruff

class RuffVersion(private val major: Int, private val minor: Int = 0, private val patch: Int = 0) :
    Comparable<RuffVersion> {
    override fun compareTo(other: RuffVersion): Int =
        when {
            major != other.major -> major - other.major
            minor != other.minor -> minor - other.minor
            else -> patch - other.patch
        }

}
