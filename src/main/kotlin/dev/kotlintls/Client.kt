package dev.kotlintls

/**
 * Singleton client holder matching Node initTLS() / destroyTLS() / getInstance().
 */
object Client {
    @Volatile
    private var instance: TlsClient? = null

    /**
     * Initialize the default TLS client (required before using getInstance() or fetch with shared client).
     */
    @JvmStatic
    fun init() {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) instance = TlsClient()
            }
        }
    }

    /**
     * Destroy the default client and clear all sessions.
     */
    @JvmStatic
    fun destroy() {
        synchronized(this) {
            instance?.destroyAll()
            instance = null
        }
    }

    /**
     * Get the default client. Call init() first.
     */
    @JvmStatic
    fun getInstance(): TlsClient {
        return instance ?: throw IllegalStateException("Client not initialized. Call init() first.")
    }

    @JvmStatic
    fun isReady(): Boolean = instance != null
}
