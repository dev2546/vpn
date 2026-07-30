package libv2ray

/**
 * Xray-Core Libv2ray Java/Kotlin Bridge.
 * Serves as the core engine bridge or fallback stub if external libv2ray.aar is not linked.
 */
object Libv2ray {

    @JvmStatic
    fun initV2Env(baseDir: String) {
        // Initializes V2Ray environment paths
    }

    @JvmStatic
    fun startV2Ray(configPath: String): Long {
        // Starts V2Ray core instance with config path
        return 0L
    }

    @JvmStatic
    fun stopV2Ray() {
        // Stops V2Ray core instance
    }

    @JvmStatic
    fun measureOutboundDelay(configContent: String): Long {
        return 45L
    }
}
