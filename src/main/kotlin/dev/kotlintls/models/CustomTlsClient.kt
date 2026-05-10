package dev.kotlintls.models

data class CustomTlsClient(
    val ja3String: String,
    val h2Settings: Map<String, UInt> = emptyMap(),
    val h2SettingsOrder: List<String> = emptyList(),
    val h3Settings: Map<String, ULong>? = null,
    val h3SettingsOrder: List<String>? = null,
    val h3PseudoHeaderOrder: List<String>? = null,
    val headerPriority: PriorityParam? = null,
    val certCompressionAlgos: List<String> = emptyList(),
    val keyShareCurves: List<String> = emptyList(),
    val supportedSignatureAlgorithms: List<String> = emptyList(),
    val supportedVersions: List<String> = emptyList(),
    val pseudoHeaderOrder: List<String> = emptyList(),
    val priorityFrames: List<PriorityFrame> = emptyList(),
    val connectionFlow: UInt = 0u,
    val streamId: UInt = 0u,
    val h3PriorityParam: UInt = 0u,
    val h3SendGreaseFrames: Boolean = false,
    val allowHttp: Boolean = false,
    val alpnProtocols: List<String> = emptyList(),
    val alpsProtocols: List<String> = emptyList()
)
