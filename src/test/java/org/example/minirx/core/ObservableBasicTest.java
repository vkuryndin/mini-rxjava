package org.example.minirx.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests basic observable behavior.
 *
 * <p>This class verifies:
 * <ul>
 *     <li>item delivery through {@code onNext(...)}</li>
 *     <li>successful completion through {@code onComplete()}</li>
 *     <li>error delivery through {@code onError(...)}</li>
 * </ul>
 */
public class ObservableBasicTest {

    /**
     * Verifies that a simple observable emits all items
     * and completes successfully.
     */
    @Test
    void shouldEmitItemsAndComplete() {
        List<Integer> receivedItems = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        observable.subscribe(new Observer<>() {
            @Override
            public void onNext(Integer item) {
                receivedItems.add(item);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }
        });

        assertEquals(List.of(1, 2, 3), receivedItems);
        assertTrue(completed.get());
        assertNull(error.get());
    }

    /**
     * Verifies that an exception thrown by the source
     * is forwarded to {@code onError(...)}.
     */
    @Test
    void shouldForwardSourceErrorToObserver() {
        List<Integer> receivedItems = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(10);
            throw new RuntimeException("Source failure");
        });

        observable.subscribe(new Observer<>() {
            @Override
            public void onNext(Integer item) {
                receivedItems.add(item);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }
        });

        assertEquals(List.of(10), receivedItems);
        assertFalse(completed.get());
        assertNotNull(error.get());
        assertEquals("Source failure", error.get().getMessage());
    }

    /**
     * Verifies that no items are delivered after completion.
     */
    @Test
    void shouldIgnoreItemsAfterCompletion() {
        List<Integer> receivedItems = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onComplete();
            emitter.onNext(2);
            emitter.onNext(3);
        });

        observable.subscribe(new Observer<>() {
            @Override
            public void onNext(Integer item) {
                receivedItems.add(item);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }
        });

        assertEquals(List.of(1), receivedItems);
        assertTrue(completed.get());
        assertNull(error.get());
    }
}