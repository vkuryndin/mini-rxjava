package org.example.minirx.integration;

import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.example.minirx.scheduler.IOThreadScheduler;
import org.example.minirx.scheduler.SingleThreadScheduler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs extended stress and comparison scenarios for the educational Mini RxJava
 * implementation and the real RxJava library.
 *
 * <p>This class is intended for:
 * <ul>
 *     <li>larger functional comparison scenarios,</li>
 *     <li>basic stress validation,</li>
 *     <li>report-friendly timing collection.</li>
 * </ul>
 *
 * <p>The tests do not require Mini RxJava to be faster than RxJava.
 * They require semantic correctness and collect timing information for analysis.
 */
public class StressComparisonTest {

    /**
     * Maximum time to wait for asynchronous scenarios.
     */
    private static final long TIMEOUT_SECONDS = 10;

    /**
     * Verifies a large map + filter + map pipeline against RxJava.
     */
    @Test
    void shouldMatchRxJavaForLargeMapFilterPipeline() {
        StressResult<Integer> miniRxResult = measureMiniRxSync(() ->
                Observable.<Integer>create(emitter -> {
                            for (int i = 1; i <= 100_000; i++) {
                                emitter.onNext(i);
                            }
                            emitter.onComplete();
                        })
                        .map((Integer number) -> number * 2)
                        .filter((Integer number) -> number % 5 == 0)
                        .map((Integer number) -> number + 1)
        );

        StressResult<Integer> rxJavaResult = measureRxJavaSync(() ->
                io.reactivex.rxjava3.core.Observable.<Integer>create(emitter -> {
                            for (int i = 1; i <= 100_000; i++) {
                                emitter.onNext(i);
                            }
                            emitter.onComplete();
                        })
                        .map((Integer number) -> number * 2)
                        .filter((Integer number) -> number % 5 == 0)
                        .map((Integer number) -> number + 1)
        );

        assertStressEquality(miniRxResult, rxJavaResult);
        printStressMetrics("Large map + filter + map pipeline", miniRxResult, rxJavaResult);
    }

    /**
     * Verifies a larger flatMap expansion scenario against RxJava.
     */
    @Test
    void shouldMatchRxJavaForFlatMapExpansionStress() {
        StressResult<Integer> miniRxResult = measureMiniRxSync(() ->
                Observable.<Integer>create(emitter -> {
                            for (int i = 1; i <= 5_000; i++) {
                                emitter.onNext(i);
                            }
                            emitter.onComplete();
                        })
                        .flatMap((Integer number) -> Observable.create(innerEmitter -> {
                            innerEmitter.onNext(number);
                            innerEmitter.onNext(number * 10);
                            innerEmitter.onNext(number * 100);
                            innerEmitter.onComplete();
                        }))
        );

        StressResult<Integer> rxJavaResult = measureRxJavaSync(() ->
                io.reactivex.rxjava3.core.Observable.<Integer>create(emitter -> {
                            for (int i = 1; i <= 5_000; i++) {
                                emitter.onNext(i);
                            }
                            emitter.onComplete();
                        })
                        .flatMap((Integer number) -> io.reactivex.rxjava3.core.Observable.create(innerEmitter -> {
                            innerEmitter.onNext(number);
                            innerEmitter.onNext(number * 10);
                            innerEmitter.onNext(number * 100);
                            innerEmitter.onComplete();
                        }))
        );

        assertStressEquality(miniRxResult, rxJavaResult);
        printStressMetrics("FlatMap expansion stress", miniRxResult, rxJavaResult);
    }

    /**
     * Verifies a scheduler-heavy scenario against RxJava.
     *
     * <p>This scenario focuses on correctness under asynchronous scheduling.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    @Test
    void shouldMatchRxJavaForSchedulerStressScenario() throws InterruptedException {
        AsyncStressResult<Integer> miniRxResult = measureMiniRxAsync(() ->
                Observable.<Integer>create(emitter -> {
                            for (int i = 1; i <= 20_000; i++) {
                                emitter.onNext(i);
                            }
                            emitter.onComplete();
                        })
                        .subscribeOn(new IOThreadScheduler())
                        .observeOn(new SingleThreadScheduler())
        );

        AsyncStressResult<Integer> rxJavaResult = measureRxJavaAsync(() ->
                io.reactivex.rxjava3.core.Observable.<Integer>create(emitter -> {
                            for (int i = 1; i <= 20_000; i++) {
                                emitter.onNext(i);
                            }
                            emitter.onComplete();
                        })
                        .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                        .observeOn(io.reactivex.rxjava3.schedulers.Schedulers.single())
        );

        assertAsyncStressEquality(miniRxResult, rxJavaResult);
        printAsyncStressMetrics("Scheduler stress scenario", miniRxResult, rxJavaResult);
    }

    /**
     * Measures a synchronous Mini RxJava scenario.
     *
     * @param supplier supplies the observable to run
     * @param <T> the item type
     * @return collected stress result
     */
    private <T> StressResult<T> measureMiniRxSync(MiniRxSupplier<T> supplier) {
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

        return new StressResult<>(items.size(), completed.get(), error.get(), durationNanos);
    }

    /**
     * Measures a synchronous RxJava scenario.
     *
     * @param supplier supplies the observable to run
     * @param <T> the item type
     * @return collected stress result
     */
    private <T> StressResult<T> measureRxJavaSync(RxJavaSupplier<T> supplier) {
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

        return new StressResult<>(items.size(), completed.get(), error.get(), durationNanos);
    }

    /**
     * Measures an asynchronous Mini RxJava scenario.
     *
     * @param supplier supplies the observable to run
     * @param <T> the item type
     * @return collected async stress result
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    private <T> AsyncStressResult<T> measureMiniRxAsync(MiniRxSupplier<T> supplier) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CopyOnWriteArrayList<T> items = new CopyOnWriteArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<String> firstObservedThread = new AtomicReference<>();

        Observable<T> observable = supplier.get();

        long startNanos = System.nanoTime();

        observable.subscribe(new Observer<>() {
            @Override
            public void onNext(T item) {
                if (firstObservedThread.get() == null) {
                    firstObservedThread.set(Thread.currentThread().getName());
                }
                items.add(item);
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

        return new AsyncStressResult<>(items.size(), completed.get(), error.get(), durationNanos, firstObservedThread.get());
    }

    /**
     * Measures an asynchronous RxJava scenario.
     *
     * @param supplier supplies the observable to run
     * @param <T> the item type
     * @return collected async stress result
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    private <T> AsyncStressResult<T> measureRxJavaAsync(RxJavaSupplier<T> supplier) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CopyOnWriteArrayList<T> items = new CopyOnWriteArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<String> firstObservedThread = new AtomicReference<>();

        io.reactivex.rxjava3.core.Observable<T> observable = supplier.get();

        long startNanos = System.nanoTime();

        observable.subscribe(
                item -> {
                    if (firstObservedThread.get() == null) {
                        firstObservedThread.set(Thread.currentThread().getName());
                    }
                    items.add(item);
                },
                throwable -> {
                    error.set(throwable);
                    latch.countDown();
                },
                () -> {
                    completed.set(true);
                    latch.countDown();
                }
        );

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        long durationNanos = System.nanoTime() - startNanos;

        return new AsyncStressResult<>(items.size(), completed.get(), error.get(), durationNanos, firstObservedThread.get());
    }

    /**
     * Verifies equality for synchronous stress scenarios.
     *
     * @param miniRxResult Mini RxJava result
     * @param rxJavaResult RxJava result
     * @param <T> the item type
     */
    private <T> void assertStressEquality(StressResult<T> miniRxResult, StressResult<T> rxJavaResult) {
        assertEquals(rxJavaResult.itemsCount(), miniRxResult.itemsCount());
        assertEquals(rxJavaResult.completed(), miniRxResult.completed());
        assertEquals(errorType(rxJavaResult.error()), errorType(miniRxResult.error()));
        assertEquals(errorMessage(rxJavaResult.error()), errorMessage(miniRxResult.error()));
        assertTrue(miniRxResult.durationNanos() > 0);
        assertTrue(rxJavaResult.durationNanos() > 0);
    }

    /**
     * Verifies equality for asynchronous stress scenarios.
     *
     * @param miniRxResult Mini RxJava result
     * @param rxJavaResult RxJava result
     * @param <T> the item type
     */
    private <T> void assertAsyncStressEquality(AsyncStressResult<T> miniRxResult, AsyncStressResult<T> rxJavaResult) {
        assertEquals(rxJavaResult.itemsCount(), miniRxResult.itemsCount());
        assertEquals(rxJavaResult.completed(), miniRxResult.completed());
        assertEquals(errorType(rxJavaResult.error()), errorType(miniRxResult.error()));
        assertEquals(errorMessage(rxJavaResult.error()), errorMessage(miniRxResult.error()));
        assertTrue(miniRxResult.durationNanos() > 0);
        assertTrue(rxJavaResult.durationNanos() > 0);
        assertTrue(miniRxResult.firstObservedThread() != null && !miniRxResult.firstObservedThread().isBlank());
        assertTrue(rxJavaResult.firstObservedThread() != null && !rxJavaResult.firstObservedThread().isBlank());
    }

    /**
     * Prints metrics for synchronous stress scenarios.
     *
     * @param scenarioName the scenario name
     * @param miniRxResult Mini RxJava result
     * @param rxJavaResult RxJava result
     * @param <T> the item type
     */
    private <T> void printStressMetrics(String scenarioName, StressResult<T> miniRxResult, StressResult<T> rxJavaResult) {
        System.out.println();
        System.out.println("=== Stress Metrics: " + scenarioName + " ===");
        System.out.println("Mini RxJava items count: " + miniRxResult.itemsCount());
        System.out.println("Mini RxJava completed: " + miniRxResult.completed());
        System.out.println("Mini RxJava error type: " + errorType(miniRxResult.error()));
        System.out.println("Mini RxJava error message: " + errorMessage(miniRxResult.error()));
        System.out.println("Mini RxJava duration (ns): " + miniRxResult.durationNanos());

        System.out.println("RxJava items count: " + rxJavaResult.itemsCount());
        System.out.println("RxJava completed: " + rxJavaResult.completed());
        System.out.println("RxJava error type: " + errorType(rxJavaResult.error()));
        System.out.println("RxJava error message: " + errorMessage(rxJavaResult.error()));
        System.out.println("RxJava duration (ns): " + rxJavaResult.durationNanos());
    }

    /**
     * Prints metrics for asynchronous stress scenarios.
     *
     * @param scenarioName the scenario name
     * @param miniRxResult Mini RxJava result
     * @param rxJavaResult RxJava result
     * @param <T> the item type
     */
    private <T> void printAsyncStressMetrics(String scenarioName, AsyncStressResult<T> miniRxResult, AsyncStressResult<T> rxJavaResult) {
        System.out.println();
        System.out.println("=== Stress Metrics: " + scenarioName + " ===");
        System.out.println("Mini RxJava items count: " + miniRxResult.itemsCount());
        System.out.println("Mini RxJava completed: " + miniRxResult.completed());
        System.out.println("Mini RxJava error type: " + errorType(miniRxResult.error()));
        System.out.println("Mini RxJava error message: " + errorMessage(miniRxResult.error()));
        System.out.println("Mini RxJava first observed thread: " + miniRxResult.firstObservedThread());
        System.out.println("Mini RxJava duration (ns): " + miniRxResult.durationNanos());

        System.out.println("RxJava items count: " + rxJavaResult.itemsCount());
        System.out.println("RxJava completed: " + rxJavaResult.completed());
        System.out.println("RxJava error type: " + errorType(rxJavaResult.error()));
        System.out.println("RxJava error message: " + errorMessage(rxJavaResult.error()));
        System.out.println("RxJava first observed thread: " + rxJavaResult.firstObservedThread());
        System.out.println("RxJava duration (ns): " + rxJavaResult.durationNanos());
    }

    /**
     * Returns the simple class name of the error, or null if absent.
     *
     * @param throwable the error
     * @return the error type name or null
     */
    private String errorType(Throwable throwable) {
        return throwable == null ? null : throwable.getClass().getSimpleName();
    }

    /**
     * Returns the error message, or null if absent.
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
     * Stores stress metrics for synchronous scenarios.
     *
     * @param itemsCount number of received items
     * @param completed completion status
     * @param error error, if any
     * @param durationNanos execution time in nanoseconds
     * @param <T> the item type
     */
    private record StressResult<T>(int itemsCount, boolean completed, Throwable error, long durationNanos) {
    }

    /**
     * Stores stress metrics for asynchronous scenarios.
     *
     * @param itemsCount number of received items
     * @param completed completion status
     * @param error error, if any
     * @param durationNanos execution time in nanoseconds
     * @param firstObservedThread name of the first thread that delivered an item
     * @param <T> the item type
     */
    private record AsyncStressResult<T>(
            int itemsCount,
            boolean completed,
            Throwable error,
            long durationNanos,
            String firstObservedThread
    ) {
    }
}