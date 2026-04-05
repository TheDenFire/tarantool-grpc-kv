package ru.thedenfire.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.thedenfire.kv.v1.*;
import ru.thedenfire.model.KeyValue;
import ru.thedenfire.service.KvService;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KvGrpcControllerTest {

    @Mock
    KvService service;

    @InjectMocks
    KvGrpcController controller;

    @Test
    void put_success() {
        @SuppressWarnings("unchecked")
        StreamObserver<PutResponse> observer = mock(StreamObserver.class);
        PutRequest request = PutRequest.newBuilder()
                .setKey("key1")
                .setValue(com.google.protobuf.ByteString.copyFromUtf8("hello"))
                .build();

        controller.put(request, observer);

        verify(service).put(eq("key1"), any());
        ArgumentCaptor<PutResponse> captor = ArgumentCaptor.forClass(PutResponse.class);
        verify(observer).onNext(captor.capture());
        assertThat(captor.getValue().getKey()).isEqualTo("key1");
        verify(observer).onCompleted();
        verify(observer, never()).onError(any());
    }

    @Test
    void put_blankKey_returnsInvalidArgument() {
        @SuppressWarnings("unchecked")
        StreamObserver<PutResponse> observer = mock(StreamObserver.class);
        PutRequest request = PutRequest.newBuilder().setKey("").build();

        controller.put(request, observer);

        verifyInvalidArgument(observer);
        verify(service, never()).put(any(), any());
    }

    @Test
    void put_serviceThrows_returnsInternal() {
        @SuppressWarnings("unchecked")
        StreamObserver<PutResponse> observer = mock(StreamObserver.class);
        PutRequest request = PutRequest.newBuilder()
                .setKey("key1")
                .setValue(com.google.protobuf.ByteString.copyFromUtf8("val"))
                .build();
        doThrow(new IllegalStateException("db error")).when(service).put(any(), any());

        controller.put(request, observer);

        verifyInternalError(observer);
    }

    @Test
    void get_found_returnsEntry() {
        @SuppressWarnings("unchecked")
        StreamObserver<GetResponse> observer = mock(StreamObserver.class);
        GetRequest request = GetRequest.newBuilder().setKey("key1").build();
        when(service.get("key1")).thenReturn(Optional.of(new KeyValue("key1", "val1".getBytes())));

        controller.get(request, observer);

        ArgumentCaptor<GetResponse> captor = ArgumentCaptor.forClass(GetResponse.class);
        verify(observer).onNext(captor.capture());
        assertThat(captor.getValue().getEntry().getKey()).isEqualTo("key1");
        verify(observer).onCompleted();
        verify(observer, never()).onError(any());
    }

    @Test
    void get_notFound_returnsNotFoundStatus() {
        @SuppressWarnings("unchecked")
        StreamObserver<GetResponse> observer = mock(StreamObserver.class);
        GetRequest request = GetRequest.newBuilder().setKey("missing").build();
        when(service.get("missing")).thenReturn(Optional.empty());

        controller.get(request, observer);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) captor.getValue()).getStatus().getCode())
                .isEqualTo(Status.Code.NOT_FOUND);
        verify(observer, never()).onNext(any());
    }

    @Test
    void get_blankKey_returnsInvalidArgument() {
        @SuppressWarnings("unchecked")
        StreamObserver<GetResponse> observer = mock(StreamObserver.class);
        GetRequest request = GetRequest.newBuilder().setKey("  ").build();

        controller.get(request, observer);

        verifyInvalidArgument(observer);
    }

    @Test
    void get_serviceThrows_returnsInternal() {
        @SuppressWarnings("unchecked")
        StreamObserver<GetResponse> observer = mock(StreamObserver.class);
        GetRequest request = GetRequest.newBuilder().setKey("key1").build();
        when(service.get("key1")).thenThrow(new IllegalStateException("db error"));

        controller.get(request, observer);

        verifyInternalError(observer);
    }

    @Test
    void delete_existing_returnsDeletedTrue() {
        @SuppressWarnings("unchecked")
        StreamObserver<DeleteResponse> observer = mock(StreamObserver.class);
        DeleteRequest request = DeleteRequest.newBuilder().setKey("key1").build();
        when(service.delete("key1")).thenReturn(true);

        controller.delete(request, observer);

        ArgumentCaptor<DeleteResponse> captor = ArgumentCaptor.forClass(DeleteResponse.class);
        verify(observer).onNext(captor.capture());
        assertThat(captor.getValue().getDeleted()).isTrue();
        verify(observer).onCompleted();
    }

    @Test
    void delete_nonExistent_returnsDeletedFalse() {
        @SuppressWarnings("unchecked")
        StreamObserver<DeleteResponse> observer = mock(StreamObserver.class);
        DeleteRequest request = DeleteRequest.newBuilder().setKey("missing").build();
        when(service.delete("missing")).thenReturn(false);

        controller.delete(request, observer);

        ArgumentCaptor<DeleteResponse> captor = ArgumentCaptor.forClass(DeleteResponse.class);
        verify(observer).onNext(captor.capture());
        assertThat(captor.getValue().getDeleted()).isFalse();
    }

    @Test
    void delete_blankKey_returnsInvalidArgument() {
        @SuppressWarnings("unchecked")
        StreamObserver<DeleteResponse> observer = mock(StreamObserver.class);
        DeleteRequest request = DeleteRequest.newBuilder().setKey("").build();

        controller.delete(request, observer);

        verifyInvalidArgument(observer);
    }

    @Test
    void count_returnsCount() {
        @SuppressWarnings("unchecked")
        StreamObserver<CountResponse> observer = mock(StreamObserver.class);
        when(service.count()).thenReturn(99L);

        controller.count(CountRequest.getDefaultInstance(), observer);

        ArgumentCaptor<CountResponse> captor = ArgumentCaptor.forClass(CountResponse.class);
        verify(observer).onNext(captor.capture());
        assertThat(captor.getValue().getCount()).isEqualTo(99L);
        verify(observer).onCompleted();
    }

    @Test
    void count_serviceThrows_returnsInternal() {
        @SuppressWarnings("unchecked")
        StreamObserver<CountResponse> observer = mock(StreamObserver.class);
        when(service.count()).thenThrow(new IllegalStateException("db error"));

        controller.count(CountRequest.getDefaultInstance(), observer);

        verifyInternalError(observer);
    }

    @Test
    @SuppressWarnings("unchecked")
    void range_streamsEntries() {
        StreamObserver<RangeResponse> observer = mock(StreamObserver.class);
        RangeRequest request = RangeRequest.newBuilder()
                .setKeyFromInclusive("a")
                .setKeyToExclusive("d")
                .build();

        doAnswer(invocation -> {
            Consumer<KeyValue> consumer = invocation.getArgument(2);
            consumer.accept(new KeyValue("a", "1".getBytes()));
            consumer.accept(new KeyValue("b", "2".getBytes()));
            consumer.accept(new KeyValue("c", "3".getBytes()));
            return null;
        }).when(service).scanRange(eq("a"), eq("d"), any());

        controller.range(request, observer);

        verify(observer, times(3)).onNext(any(RangeResponse.class));
        verify(observer).onCompleted();
        verify(observer, never()).onError(any());
    }

    @Test
    void range_invalidRange_returnsInvalidArgument() {
        @SuppressWarnings("unchecked")
        StreamObserver<RangeResponse> observer = mock(StreamObserver.class);
        RangeRequest request = RangeRequest.newBuilder()
                .setKeyFromInclusive("z")
                .setKeyToExclusive("a")
                .build();

        controller.range(request, observer);

        verifyInvalidArgument(observer);
        verify(service, never()).scanRange(any(), any(), any());
    }

    @Test
    void range_equalKeys_returnsInvalidArgument() {
        @SuppressWarnings("unchecked")
        StreamObserver<RangeResponse> observer = mock(StreamObserver.class);
        RangeRequest request = RangeRequest.newBuilder()
                .setKeyFromInclusive("a")
                .setKeyToExclusive("a")
                .build();

        controller.range(request, observer);

        verifyInvalidArgument(observer);
    }

    @Test
    void range_blankFromKey_returnsInvalidArgument() {
        @SuppressWarnings("unchecked")
        StreamObserver<RangeResponse> observer = mock(StreamObserver.class);
        RangeRequest request = RangeRequest.newBuilder()
                .setKeyFromInclusive("")
                .setKeyToExclusive("z")
                .build();

        controller.range(request, observer);

        verifyInvalidArgument(observer);
    }

    private static void verifyInvalidArgument(StreamObserver<?> observer) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) captor.getValue()).getStatus().getCode())
                .isEqualTo(Status.Code.INVALID_ARGUMENT);
        verify(observer, never()).onNext(any());
    }

    private static void verifyInternalError(StreamObserver<?> observer) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) captor.getValue()).getStatus().getCode())
                .isEqualTo(Status.Code.INTERNAL);
    }
}
