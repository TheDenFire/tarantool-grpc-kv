package ru.thedenfire.controller;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.thedenfire.kv.v1.*;
import ru.thedenfire.model.KeyValue;
import ru.thedenfire.service.KvService;

import java.util.Objects;
import java.util.Optional;

public final class KvGrpcController extends KeyValueServiceGrpc.KeyValueServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(KvGrpcController.class);

    private final KvService service;

    public KvGrpcController(KvService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        try {
            validateKey(request.getKey(), "key");
            service.put(request.getKey(), request.hasValue() ? request.getValue().toByteArray() : null);
            responseObserver.onNext(PutResponse.newBuilder().setKey(request.getKey()).build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("put key='{}' failed", request.getKey(), e);
            responseObserver.onError(Status.INTERNAL.withDescription("Failed to put key").withCause(e).asRuntimeException());
        }
    }

    @Override
    public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
        try {
            validateKey(request.getKey(), "key");
            Optional<KeyValue> result = service.get(request.getKey());
            if (result.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Key not found").asRuntimeException());
                return;
            }
            responseObserver.onNext(GetResponse.newBuilder()
                    .setEntry(toEntry(result.get()))
                    .build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("get key='{}' failed", request.getKey(), e);
            responseObserver.onError(Status.INTERNAL.withDescription("Failed to get key").withCause(e).asRuntimeException());
        }
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            validateKey(request.getKey(), "key");
            responseObserver.onNext(DeleteResponse.newBuilder()
                    .setDeleted(service.delete(request.getKey()))
                    .build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("delete key='{}' failed", request.getKey(), e);
            responseObserver.onError(Status.INTERNAL.withDescription("Failed to delete key").withCause(e).asRuntimeException());
        }
    }

    @Override
    public void range(RangeRequest request, StreamObserver<RangeResponse> responseObserver) {
        try {
            validateKey(request.getKeyFromInclusive(), "key_from_inclusive");
            validateKey(request.getKeyToExclusive(), "key_to_exclusive");
            if (request.getKeyFromInclusive().compareTo(request.getKeyToExclusive()) >= 0) {
                throw new IllegalArgumentException("key_from_inclusive must be less than key_to_exclusive");
            }

            service.scanRange(
                    request.getKeyFromInclusive(),
                    request.getKeyToExclusive(),
                    keyValue -> responseObserver.onNext(
                            RangeResponse.newBuilder()
                                    .setEntry(toEntry(keyValue))
                                    .build()
                    )
            );
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("range [{}, {}) failed", request.getKeyFromInclusive(), request.getKeyToExclusive(), e);
            responseObserver.onError(Status.INTERNAL.withDescription("Failed to stream range").withCause(e).asRuntimeException());
        }
    }

    @Override
    public void count(CountRequest request, StreamObserver<CountResponse> responseObserver) {
        try {
            responseObserver.onNext(CountResponse.newBuilder()
                    .setCount(service.count())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("count failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Failed to count records").withCause(e).asRuntimeException());
        }
    }

    @Override
    public void test(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        try {
            service.test();

            responseObserver.onNext(
                    TestResponse.newBuilder()
                            .setInserted(5_000_000)
                            .build()
            );
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("test failed", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to run test")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    private static void validateKey(String key, String fieldName) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static KeyValueEntry toEntry(KeyValue keyValue) {
        KeyValueEntry.Builder builder = KeyValueEntry.newBuilder()
                .setKey(keyValue.getKey());
        if (keyValue.getValue() != null) {
            builder.setValue(ByteString.copyFrom(keyValue.getValue()));
        }
        return builder.build();
    }
}
