package dev.kotlintls

data class PriorityParam(
    val streamDep: Int,
    val exclusive: Boolean,
    val weight: Int
)

data class PriorityFrame(
    val streamID: Int,
    val priorityParam: PriorityParam
)
