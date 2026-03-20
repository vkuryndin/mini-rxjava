package org.example.minirx.metrics;

import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.example.minirx.scheduler.IOThreadScheduler;
import org.example.minirx.scheduler.SingleThreadScheduler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Collects basic functional and timing metrics for the educational Mini RxJava
 * implementation and compares them with the real RxJava library.
 *
 * <p>This class is intentionally focused on:
 * <ul>
 *     <li>functional correctness,</li>
 *     <li>completion correctness,</li>
 *     <li>error propagation correctness,</li>
 *     <li>basic scheduler behavior,</li>
 *     <li>basic execution time measurement.</li>
 * </ul>
 *
 * <p>It does not require Mini RxJava to be faster than RxJava.
 * The goal is to measure and document behavior, not to enforce
 * production-grade performance.
 */
public class MetricsTest {

    /**
     * Maximum time to wait for asynchronous metric collection.
     */
    private static final long TIMEOUT_SECONDS = 5;

    /**
     * Collects metrics for a synchronous map + filter scenario
     * and compares Mini RxJava with RxJava.
     */
    @Test
    void shouldCollectMetricsForMapFilterScenario() {
        MetricsResult<Integer> miniRxResult = measureMiniRx(() ->
                Observable.<Integer>create(emitter -> {
                            for (int i = 1; i <= 10_000; i++) {
                                emitter.onNext(i);
                            }
                            emitter.onComplete();
                        })
                        .map((Integer number) -> number * 2)
                        .filter((Integer number) -> number % 3 == 0)
        );

        MetricsResult<Integer> rxJavaResult = measureRxJava(() ->
                io.reactivex.rxjava3.core.Observable.<Integer>create(emitter -> {
                            for (int i = 1; i <= 10_000; i++) {
                                emitter.onNext(i);
                            }
                            emitter.onComplete();
                        })
                        .map((Integer number) -> number * 2)
                        .filter((Integer number) -> number % 3 == 0)
        );

        assertFunctionalEquality(miniRxResult, rxJavaResult);
        assertTrue(miniRxResult.durationNanos() > 0);
        assertTrue(rxJavaResult.durationNanos() > 0);

        printMetrics("Map + Filter scenario", miniRxResult, rxJavaResult);
    }

    /**
     * Collects metrics for a synchronous flatMap scenario
     * and compares Mini RxJava with RxJava.
     */
    @Test
    void shouldCollectMetricsForFlatMapScenario() {
        MetricsResult<Integer> miniRxResult = measureMiniRx(() ->
                Observable.<Integer>create(emitter -> {
                            emitter.onNext(1);
                            emitter.onNext(2);
                            emitter.onNext(3);
                            emitter.onNext(4);
                            emitter.onComplete();
                        })
                        .flatMap((Integer number) -> Observable.create(innerEmitter -> {
                            innerEmitter.onNext(number);
                            innerEmitter.onNext(number * 10);
                            innerEmitter.onComplete();
                        }))
        );

        MetricsResult<Integer> rxJavaResult = measureRxJava(() ->
                io.reactivex.rxjava3.core.Observable.<Integer>create(emitter -> {
                            emitter.onNext(1);
                            emitter.onNext(2);
                            emitter.onNext(3);
                            emitter.onNext(4);
                            emitter.onComplete();
                        })
                        .flatMap((Integer number) -> io.reactivex.rxjava3.core.Observable.create(innerEmitter -> {
                            innerEmitter.onNext(number);
                            innerEmitter.onNext(number * 10);
                            innerEmitter.onComplete();
                        }))
        );

        assertFunctionalEquality(miniRxResult, rxJavaResult);
        assertTrue(miniRxResult.durationNanos() > 0);
        assertTrue(rxJavaResult.durationNanos() > 0);

        printMetrics("FlatMap scenario", miniRxResult, rxJavaResult);
    }

    /**
     * Collects metrics for an error scenario and compares Mini RxJava with RxJava.
     */
    @Test
    void shouldCollectMetricsForErrorScenario() {
        MetricsResult<Integer> miniRxResult = measureMiniRx(() ->
                Observable.<Integer>create(emitter -> {
                    emitter.onNext(1);
                    emitter.onNext(2);
                    emitter.onComplete();
                }).map((Integer number) -> {
                    if (number == 2) {
                        throw new IllegalStateException("Map failure");
                    }
                    return number * 10;
                })
        );

        MetricsResult<Integer> rxJavaResult = measureRxJava(() ->
                io.reactivex.rxjava3.core.Observable.<Integer>create(emitter -> {
                    emitter.onNext(1);
                    emitter.onNext(2);
                    emitter.onComplete();
                }).map((Integer number) -> {
                    if (number == 2) {
                        throw new IllegalStateException("Map failure");
                    }
                    return number * 10;
                })
        );

        assertFunctionalEquality(miniRxResult, rxJavaResult);
        assertTrue(miniRxResult.durationNanos() > 0);
        assertTrue(rxJavaResult.durationNanos() > 0);

        printMetrics("Error scenario", miniRxResult, rxJavaResult);
    }

    /**
     * Collects metrics for subscribeOn + observeOn behavior in Mini RxJava.
     *
     * <p>This test focuses on scheduler semantics rather than direct
     * time comparison with RxJava.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    @Test
    void shouldCollectMetricsForSchedulerScenario() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> sourceThread = new AtomicReference<>();
        AtomicReference<String> observerThread = new AtomicReference<>();
        List<Integer> receivedItems = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        long startNanos = System.nanoTime();

        Observable<Integer> observable = Observable.create(emitter -> {
            sourceThread.set(Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        observable
                .subscribeOn(new IOThreadScheduler())
                .observeOn(new SingleThreadScheduler())
                .subscribe(new Observer<>() {
                    @Override
                    public void onNext(Integer item) {
                        if (observerThread.get() == null) {
                            observerThread.set(Thread.currentThread().getName());
                        }
                        receivedItems.add(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        error.set(throwable);
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        completed.set(true);
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        long durationNanos = System.nanoTime() - startNanos;

        assertEquals(List.of(1, 2, 3), receivedItems);
        assertTrue(completed.get());
        assertNull(error.get());
        assertNotNull(sourceThread.get());
        assertNotNull(observerThread.get());
        assertTrue(sourceThread.get().startsWith("mini-rx-io-"));
        assertTrue(observerThread.get().startsWith("mini-rx-single-"));
        assertTrue(durationNanos > 0);

        System.out.println();
        System.out.println("=== Metrics: Scheduler scenario ===");
        System.out.println("Mini RxJava items: " + receivedItems);
        System.out.println("Mini RxJava completed: " + completed.get());
        System.out.println("Mini RxJava error type: " + errorType(error.get()));
        System.out.println("Mini RxJava error message: " + errorMessage(error.get()));
        System.out.println("Mini RxJava source thread: " + sourceThread.get());
        System.out.println("Mini RxJava observer thread: " + observerThread.get());
        System.out.println("Mini RxJava duration (ns): " + durationNanos);
    }

    /**
     * Measures a Mini RxJava observable scenario.
     *
     * @param supplier supplies the observable to execute
     * @param <T> the item type
     * @return collected metrics
     */
    private <T> MetricsResult<T> measureMiniRx(MiniRxSupplier<T> supplier) {
        List<T> items = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<T> observable = supplier.get();

        long startNanos = System.nanoTime();

        observable.subscribe(new Observer<>() {
            @Override
            public void onNext(T item) {
                items.add(item);
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

        long durationNanos = System.nanoTime() - startNanos;

        return new MetricsResult<>(items, completed.get(), error.get(), durationNanos);
    }

    /**
     * Measures a real RxJava observable scenario.
     *
     * @param supplier supplies the observable to execute
     * @param <T> the item type
     * @return collected metrics
     */
    private <T> MetricsResult<T> measureRxJava(RxJavaSupplier<T> supplier) {
        List<T> items = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        io.reactivex.rxjava3.core.Observable<T> observable = supplier.get();

        long startNanos = System.nanoTime();

        io.reactivex.rxjava3.disposables.Disposable ignored = observable.subscribe(
                items::add,
                error::set,
                () -> completed.set(true)
        );

        long durationNanos = System.nanoTime() - startNanos;

        return new MetricsResult<>(items, completed.get(), error.get(), durationNanos);
    }

    /**
     * Verifies that Mini RxJava and RxJava produced the same observable outcome.
     *
     * @param miniRxResult the Mini RxJava metrics
     * @param rxJavaResult the RxJava metrics
     * @param <T> the item type
     */
    private <T> void assertFunctionalEquality(MetricsResult<T> miniRxResult, MetricsResult<T> rxJavaResult) {
        assertEquals(rxJavaResult.items(), miniRxResult.items());
        assertEquals(rxJavaResult.completed(), miniRxResult.completed());
        assertEquals(errorType(rxJavaResult.error()), errorType(miniRxResult.error()));
        assertEquals(errorMessage(rxJavaResult.error()), errorMessage(miniRxResult.error()));
    }

    /**
     * Prints a metrics summary for README/report usage.
     *
     * @param scenarioName the scenario label
     * @param miniRxResult the Mini RxJava metrics
     * @param rxJavaResult the RxJava metrics
     * @param <T> the item type
     */
    private <T> void printMetrics(String scenarioName, MetricsResult<T> miniRxResult, MetricsResult<T> rxJavaResult) {
        System.out.println();
        System.out.println("=== Metrics: " + scenarioName + " ===");
        System.out.println("Mini RxJava items count: " + miniRxResult.items().size());
        System.out.println("Mini RxJava completed: " + miniRxResult.completed());
        System.out.println("Mini RxJava error type: " + errorType(miniRxResult.error()));
        System.out.println("Mini RxJava error message: " + errorMessage(miniRxResult.error()));
        System.out.println("Mini RxJava duration (ns): " + miniRxResult.durationNanos());

        System.out.println("RxJava items count: " + rxJavaResult.items().size());
        System.out.println("RxJava completed: " + rxJavaResult.completed());
        System.out.println("RxJava error type: " + errorType(rxJavaResult.error()));
        System.out.println("RxJava error message: " + errorMessage(rxJavaResult.error()));
        System.out.println("RxJava duration (ns): " + rxJavaResult.durationNanos());
    }

    /**
     * Returns the simple class name of the given error, or null if no error exists.
     *
     * @param throwable the error
     * @return the error type name or null
     */
    private String errorType(Throwable throwable) {
        return throwable == null ? null : throwable.getClass().getSimpleName();
    }

    /**
     * Returns the message of the given error, or null if no error exists.
     *
     * @param throwable the error
     * @return the error message or null
     */
    private String errorMessage(Throwable throwable) {
        return throwable == null ? null : throwable.getMessage();
    }

    /**
     * Supplies a Mini RxJava observable.
     *
     * @param <T> the item type
     */
    @FunctionalInterface
    private interface MiniRxSupplier<T> {

        /**
         * Creates a Mini RxJava observable.
         *
         * @return a new observable
         */
        Observable<T> get();
    }

    /**
     * Supplies a real RxJava observable.
     *
     * @param <T> the item type
     */
    @FunctionalInterface
    private interface RxJavaSupplier<T> {

        /**
         * Creates a real RxJava observable.
         *
         * @return a new RxJava observable
         */
        io.reactivex.rxjava3.core.Observable<T> get();
    }

    /**
     * Stores collected metrics for one observable execution.
     *
     * @param items received items
     * @param completed whether the stream completed successfully
     * @param error received error, if any
     * @param durationNanos execution time in nanoseconds
     * @param <T> the item type
     */
    private record MetricsResult<T>(List<T> items, boolean completed, Throwable error, long durationNanos) {
    }
}