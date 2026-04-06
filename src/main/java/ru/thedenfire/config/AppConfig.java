package ru.thedenfire.config;

import lombok.Getter;

@Getter
public final class AppConfig {

    private final int grpcPort;

    private final String tarantoolHost;
    private final int    tarantoolPort;
    private final String tarantoolUser;
    private final String tarantoolPassword;
    private final int    connectTimeoutMs;
    private final int    connections;
    private final String kvSpaceName;
    private final int    rangeBatchSize;

    public AppConfig() {
        grpcPort         = intEnv("GRPC_PORT",                    9090);

        tarantoolHost    = env("TARANTOOL_HOST",                  "localhost");
        tarantoolPort    = intEnv("TARANTOOL_PORT",               3301);
        tarantoolUser    = env("TARANTOOL_USER",                  "guest");
        tarantoolPassword= env("TARANTOOL_PASSWORD",              "");
        connectTimeoutMs = intEnv("TARANTOOL_CONNECT_TIMEOUT_MS", 10_000);
        connections      = intEnv("TARANTOOL_CONNECTIONS",        4);
        kvSpaceName      = env("KV_SPACE_NAME",                   "kv");
        rangeBatchSize   = intEnv("KV_RANGE_BATCH_SIZE",          5_000);
    }

//    public int    getGrpcPort()          { return grpcPort; }
//    public String getTarantoolHost()     { return tarantoolHost; }
//    public int    getTarantoolPort()     { return tarantoolPort; }
//    public String getTarantoolUser()     { return tarantoolUser; }
//    public String getTarantoolPassword() { return tarantoolPassword; }
//    public int    getConnectTimeoutMs()  { return connectTimeoutMs; }
//    public int    getConnections()       { return connections; }
//    public String getKvSpaceName()       { return kvSpaceName; }
//    public int    getRangeBatchSize()    { return rangeBatchSize; }

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
