package org.example.minirx.integration;

import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests boundary cases and robustness behavior of the Mini RxJava implementation.
 *
 * <p>This class verifies:
 * <ul>
 *     <li>empty stream behavior,</li>
 *     <li>operators on boundary scenarios,</li>
 *     <li>null argument validation,</li>
 *     <li>exception handling inside observers,</li>
 *     <li>that subscription code does not crash outward in expected failure scenarios.</li>
 * </ul>
 */
public class BoundaryAndRobustnessTest {

    /**
     * Verifies that an empty stream completes successfully without emitting items.
     */
    @Test
    void shouldCompleteEmptyStream() {
        List<Integer> receivedItems = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<Integer> observable = Observable.<Integer>create(emitter -> {
            emitter.onComplete();
        });

        observable.subscribe(new Observer<Integer>() {
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

        assertEquals(List.of(), receivedItems);
        assertTrue(completed.get());
        assertNull(error.get());
    }

    /**
     * Verifies that filter can remove all items and still complete normally.
     */
    @Test
    void shouldHandleFilterRemovingAllItems() {
        List<Integer> receivedItems = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<Integer> observable = Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        observable
                .filter((Integer number) -> false)
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

        assertEquals(List.of(), receivedItems);
        assertTrue(completed.get());
        assertNull(error.get());
    }

    /**
     * Verifies that flatMap can work with empty inner observables.
     */
    @Test
    void shouldHandleFlatMapWithEmptyInnerObservables() {
        List<Integer> receivedItems = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<Integer> observable = Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        observable
                .flatMap((Integer number) -> Observable.<Integer>create(innerEmitter -> {
                    innerEmitter.onComplete();
                }))
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

        assertEquals(List.of(), receivedItems);
        assertTrue(completed.get());
        assertNull(error.get());
    }

    /**
     * Verifies that subscribe rejects a null observer.
     */
    @Test
    void shouldRejectNullObserverInSubscribe() {
        Observable<Integer> observable = Observable.<Integer>create(emitter -> {
            emitter.onComplete();
        });

        assertThrows(NullPointerException.class, () -> observable.subscribe(null));
    }

    /**
     * Verifies that map rejects a null mapper.
     */
    @Test
    void shouldRejectNullMapper() {
        Observable<Integer> observable = Observable.<Integer>create(emitter -> {
            emitter.onComplete();
        });

        assertThrows(NullPointerException.class, () -> observable.map(null));
    }

    /**
     * Verifies that filter rejects a null predicate.
     */
    @Test
    void shouldRejectNullPredicate() {
        Observable<Integer> observable = Observable.<Integer>create(emitter -> {
            emitter.onComplete();
        });

        assertThrows(NullPointerException.class, () -> observable.filter(null));
    }

    /**
     * Verifies that flatMap rejects a null mapper.
     */
    @Test
    void shouldRejectNullFlatMapMapper() {
        Observable<Integer> observable = Observable.<Integer>create(emitter -> {
            emitter.onComplete();
        });

        assertThrows(NullPointerException.class, () -> observable.flatMap(null));
    }

    /**
     * Verifies that an exception thrown by observer.onNext does not escape
     * from subscribe and is redirected to onError.
     */
    @Test
    void shouldNotCrashOutwardWhenObserverOnNextThrows() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable<Integer> observable = Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onComplete();
        });

        assertDoesNotThrow(() ->
                observable.subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        throw new IllegalStateException("Observer onNext failure");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        error.set(throwable);
                    }

                    @Override
                    public void onComplete() {
                        completed.set(true);
                    }
                })
        );

        assertEquals("Observer onNext failure", error.get() == null ? null : error.get().getMessage());
        assertFalse(completed.get());
    }

    /**
     * Verifies that an exception thrown by observer.onError does not escape
     * from subscribe.
     */
    @Test
    void shouldNotCrashOutwardWhenObserverOnErrorThrows() {
        AtomicBoolean errorCallbackEntered = new AtomicBoolean(false);

        Observable<Integer> observable = Observable.<Integer>create(emitter -> {
            emitter.onError(new RuntimeException("Source failure"));
        });

        assertDoesNotThrow(() ->
                observable.subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        // No-op.
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        errorCallbackEntered.set(true);
                        throw new IllegalStateException("Observer onError failure");
                    }

                    @Override
                    public void onComplete() {
                        // No-op.
                    }
                })
        );

        assertTrue(errorCallbackEntered.get());
    }
}