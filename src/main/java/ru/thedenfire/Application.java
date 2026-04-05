package ru.thedenfire;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.factory.TarantoolBoxClientBuilder;
import io.tarantool.client.factory.TarantoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.thedenfire.config.AppConfig;
import ru.thedenfire.controller.KvGrpcController;
import ru.thedenfire.repository.KvRepository;
import ru.thedenfire.repository.TarantoolKvRepository;
import ru.thedenfire.service.KvService;
import ru.thedenfire.service.KvServiceImpl;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();

        TarantoolBoxClientBuilder builder = TarantoolFactory.box()
                .withHost(config.tarantoolHost)
                .withPort(config.tarantoolPort)
                .withUser(config.tarantoolUser)
                .withConnectTimeout(config.connectTimeoutMs)
                .withEventLoopThreadsCount(Math.max(1, config.connections));

        if (config.tarantoolPassword != null && !config.tarantoolPassword.isEmpty()) {
            builder = builder.withPassword(config.tarantoolPassword);
        }

        TarantoolBoxClient client = builder.build();

        KvRepository repository = new TarantoolKvRepository(client);
        KvService service = new KvServiceImpl(repository);
        Server server = ServerBuilder.forPort(config.grpcPort)
                .addService(new KvGrpcController(service))
                .addService(ProtoReflectionService.newInstance())
                .addService(ProtoReflectionServiceV1.newInstance())
                .build()
                .start();

        log.info("gRPC server started on 0.0.0.0:{}", config.grpcPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            service.close();
        }));

        server.awaitTermination();
    }
}
