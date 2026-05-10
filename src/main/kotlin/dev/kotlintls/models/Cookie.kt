package dev.kotlintls.models

data class Cookie(
    val name: String,
    val value: String,
    val path: String = "/",
    val domain: String = "",
    val expires: Long = 0L,
    val maxAge: Int = -1,
    val secure: Boolean = false,
    val httpOnly: Boolean = false
)
