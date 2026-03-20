package org.example.minirx.internal;

import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the map operator.
 *
 * <p>This class verifies:
 * <ul>
 *     <li>item transformation with {@code map(...)}</li>
 *     <li>successful completion after mapping</li>
 *     <li>error propagation when the mapper fails</li>
 * </ul>
 */
public class MapOperatorTest {

    /**
     * Verifies that the map operator transforms all items correctly.
     */
    @Test
    void shouldTransformItemsWithMap() {
        List<Integer> receivedItems = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        observable
                .map(number -> number * 10)
                .subscribe(new Observer<Integer>() {
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

        assertEquals(List.of(10, 20, 30), receivedItems);
        assertTrue(completed.get());
        assertFalse(error.get() != null);
    }

    /**
     * Verifies that an exception thrown inside the mapper
     * is forwarded to {@code onError(...)}.
     */
    @Test
    void shouldForwardMapperErrorToObserver() {
        List<Integer> receivedItems = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        observable
                .map(number -> {
                    if (number == 2) {
                        throw new IllegalStateException("Map failure");
                    }
                    return number * 10;
                })
                .subscribe(new Observer<Integer>() {
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
        assertEquals("Map failure", error.get().getMessage());
    }
}