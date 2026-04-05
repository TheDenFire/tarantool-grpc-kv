package ru.thedenfire.config;

public final class AppConfig {

    public final int grpcPort;

    public final String tarantoolHost;
    public final int    tarantoolPort;
    public final String tarantoolUser;
    public final String tarantoolPassword;
    public final int    connectTimeoutMs;
    public final int    readTimeoutMs;
    public final int    requestTimeoutMs;
    public final int    connections;
    public final String kvSpaceName;

    public final int rangeBatchSize;

    public AppConfig() {
        grpcPort         = intEnv("GRPC_PORT",              9090);

        tarantoolHost    = env("TARANTOOL_HOST",            "localhost");
        tarantoolPort    = intEnv("TARANTOOL_PORT",         3301);
        tarantoolUser    = env("TARANTOOL_USER",            "guest");
        tarantoolPassword= env("TARANTOOL_PASSWORD",        "");
        connectTimeoutMs = intEnv("TARANTOOL_CONNECT_TIMEOUT_MS", 10_000);
        readTimeoutMs    = intEnv("TARANTOOL_READ_TIMEOUT_MS",    10_000);
        requestTimeoutMs = intEnv("TARANTOOL_REQUEST_TIMEOUT_MS",  5_000);
        connections      = intEnv("TARANTOOL_CONNECTIONS",         4);
        kvSpaceName      = env("KV_SPACE_NAME",              "KV");

        rangeBatchSize   = intEnv("KV_RANGE_BATCH_SIZE",    5_000);
    }

    private static String env(String name, String defaultValue) {
        String val = System.getenv(name);
        return val != null && !val.isBlank() ? val : defaultValue;
    }

    private static int intEnv(String name, int defaultValue) {
        String val = System.getenv(name);
        if (val == null || val.isBlank()) return defaultValue;
        return Integer.parseInt(val);
    }
}
