package org.example.minirx.internal;

import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests error propagation and terminal-state behavior.
 *
 * <p>This class verifies:
 * <ul>
 *     <li>that an error terminates the stream,</li>
 *     <li>that signals after an error are ignored,</li>
 *     <li>that a null error is replaced with a {@link NullPointerException}.</li>
 * </ul>
 */
public class ErrorHandlingTest {

    /**
     * Verifies that after an error is emitted, the stream is terminated
     * and later signals are ignored.
     */
    @Test
    void shouldIgnoreSignalsAfterError() {
        List<Integer> receivedItems = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onError(new IllegalStateException("Failure"));
            emitter.onNext(2);
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

        assertEquals(List.of(1), receivedItems);
        assertNotNull(error.get());
        assertEquals("Failure", error.get().getMessage());
        assertFalse(completed.get());
    }

    /**
     * Verifies that calling onError with null is converted
     * into a {@link NullPointerException}.
     */
    @Test
    void shouldReplaceNullErrorWithNullPointerException() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable<Integer> observable = Observable.create(emitter -> emitter.onError(null));

        observable.subscribe(new Observer<>() {
            @Override
            public void onNext(Integer item) {
                // No-op.
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

        assertNotNull(error.get());
        assertInstanceOf(NullPointerException.class, error.get());
        assertEquals("throwable must not be null", error.get().getMessage());
        assertFalse(completed.get());
    }

    /**
     * Verifies that a stream with an error does not complete successfully.
     */
    @Test
    void shouldNotCallOnCompleteAfterError() {
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<String> observable = Observable.create(emitter -> {
            emitter.onNext("A");
            emitter.onError(new RuntimeException("Broken stream"));
        });

        observable.subscribe(new Observer<>() {
            @Override
            public void onNext(String item) {
                // No-op.
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

        assertNotNull(error.get());
        assertEquals("Broken stream", error.get().getMessage());
        assertFalse(completed.get());
    }

    /**
     * Verifies that a normal stream still completes successfully
     * when no error occurs.
     */
    @Test
    void shouldCompleteNormallyWhenNoErrorOccurs() {
        List<String> receivedItems = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<String> observable = Observable.create(emitter -> {
            emitter.onNext("A");
            emitter.onNext("B");
            emitter.onComplete();
        });

        observable.subscribe(new Observer<>() {
            @Override
            public void onNext(String item) {
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

        assertEquals(List.of("A", "B"), receivedItems);
        assertTrue(completed.get());
        assertNull(error.get());
    }
}