package org.example.minirx;

import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.example.minirx.scheduler.ComputationScheduler;
import org.example.minirx.scheduler.IOThreadScheduler;
import org.example.minirx.scheduler.Scheduler;
import org.example.minirx.scheduler.SingleThreadScheduler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests scheduler behavior and integration with observable operators.
 *
 * <p>This class verifies:
 * <ul>
 *     <li>direct task execution on different scheduler implementations,</li>
 *     <li>source execution with {@code subscribeOn(...)},</li>
 *     <li>observer notification delivery with {@code observeOn(...)},</li>
 *     <li>combined behavior of {@code subscribeOn(...)} and {@code observeOn(...)}.</li>
 * </ul>
 */
public class SchedulerTest {

    /**
     * Maximum time to wait for asynchronous test completion.
     */
    private static final long TIMEOUT_SECONDS = 3;

    /**
     * Verifies that the IO scheduler executes tasks on an IO thread.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    @Test
    void shouldExecuteTaskOnIoSchedulerThread() throws InterruptedException {
        Scheduler scheduler = new IOThreadScheduler();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        scheduler.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertNotNull(threadName.get());
        assertTrue(threadName.get().startsWith("mini-rx-io-"));
    }

    /**
     * Verifies that the computation scheduler executes tasks on a computation thread.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    @Test
    void shouldExecuteTaskOnComputationSchedulerThread() throws InterruptedException {
        Scheduler scheduler = new ComputationScheduler();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        scheduler.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertNotNull(threadName.get());
        assertTrue(threadName.get().startsWith("mini-rx-computation-"));
    }

    /**
     * Verifies that the single-thread scheduler executes tasks sequentially
     * on the same dedicated thread.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    @Test
    void shouldExecuteTasksSequentiallyOnSingleThreadScheduler() throws InterruptedException {
        Scheduler scheduler = new SingleThreadScheduler();
        CountDownLatch latch = new CountDownLatch(2);
        List<String> threadNames = new CopyOnWriteArrayList<>();
        List<Integer> order = new CopyOnWriteArrayList<>();

        scheduler.execute(() -> {
            threadNames.add(Thread.currentThread().getName());
            order.add(1);
            latch.countDown();
        });

        scheduler.execute(() -> {
            threadNames.add(Thread.currentThread().getName());
            order.add(2);
            latch.countDown();
        });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(List.of(1, 2), order);
        assertEquals(2, threadNames.size());
        assertEquals(threadNames.get(0), threadNames.get(1));
        assertTrue(threadNames.get(0).startsWith("mini-rx-single-"));
    }

    /**
     * Verifies that subscribeOn moves source execution to the scheduler thread.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    @Test
    void shouldRunSourceOnSubscribeOnSchedulerThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> sourceThreadName = new AtomicReference<>();
        AtomicReference<String> observerThreadName = new AtomicReference<>();

        Observable<Integer> observable = Observable.create(emitter -> {
            sourceThreadName.set(Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onComplete();
        });

        observable
                .subscribeOn(new IOThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        observerThreadName.set(Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertNotNull(sourceThreadName.get());
        assertNotNull(observerThreadName.get());
        assertTrue(sourceThreadName.get().startsWith("mini-rx-io-"));
        assertTrue(observerThreadName.get().startsWith("mini-rx-io-"));
    }

    /**
     * Verifies that observeOn moves observer notifications
     * to the given scheduler thread.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    @Test
    void shouldDeliverObserverSignalsOnObserveOnSchedulerThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        String testThreadName = Thread.currentThread().getName();
        AtomicReference<String> sourceThreadName = new AtomicReference<>();
        AtomicReference<String> firstObservedThreadName = new AtomicReference<>();
        List<Integer> receivedItems = new CopyOnWriteArrayList<>();

        Observable<Integer> observable = Observable.create(emitter -> {
            sourceThreadName.set(Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onComplete();
        });

        observable
                .observeOn(new SingleThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        if (firstObservedThreadName.get() == null) {
                            firstObservedThreadName.set(Thread.currentThread().getName());
                        }
                        receivedItems.add(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(List.of(1, 2), receivedItems);
        assertEquals(testThreadName, sourceThreadName.get());
        assertNotNull(firstObservedThreadName.get());
        assertTrue(firstObservedThreadName.get().startsWith("mini-rx-single-"));
    }

    /**
     * Verifies that subscribeOn and observeOn can use different scheduler threads.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    @Test
    void shouldUseDifferentThreadsForSubscribeOnAndObserveOn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> sourceThreadName = new AtomicReference<>();
        AtomicReference<String> observedThreadName = new AtomicReference<>();

        Observable<Integer> observable = Observable.create(emitter -> {
            sourceThreadName.set(Thread.currentThread().getName());
            emitter.onNext(100);
            emitter.onComplete();
        });

        observable
                .subscribeOn(new IOThreadScheduler())
                .observeOn(new SingleThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        observedThreadName.set(Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertNotNull(sourceThreadName.get());
        assertNotNull(observedThreadName.get());
        assertTrue(sourceThreadName.get().startsWith("mini-rx-io-"));
        assertTrue(observedThreadName.get().startsWith("mini-rx-single-"));
    }
}