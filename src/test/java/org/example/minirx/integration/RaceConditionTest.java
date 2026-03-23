package org.example.minirx.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.example.minirx.core.Disposable;
import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.example.minirx.scheduler.SingleThreadScheduler;
import org.junit.jupiter.api.Test;

/**
 * Runs race-condition-oriented tests for the Mini RxJava implementation.
 *
 * <p>These tests do not prove the absence of race conditions, but they help detect typical
 * concurrency issues under stress.
 *
 * <p>If such tests fail intermittently, that is an important signal that the implementation may
 * have a thread-safety problem.
 */
class RaceConditionTest {

  /** Maximum time to wait for asynchronous completion. */
  private static final long TIMEOUT_SECONDS = 5;

  /**
   * Verifies that concurrent inner observables in flatMap do not lose items.
   *
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  @Test
  void shouldHandleConcurrentFlatMapInnerStreamsWithoutLosingItems() throws InterruptedException {
    int itemCount = 500;

    List<Integer> receivedItems = new CopyOnWriteArrayList<>();
    AtomicBoolean completed = new AtomicBoolean(false);
    AtomicReference<Throwable> error = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              for (int i = 1; i <= itemCount; i++) {
                emitter.onNext(i);
              }
              emitter.onComplete();
            });

    observable
        .flatMap(
            (Integer number) ->
                Observable.<Integer>create(
                    innerEmitter -> {
                      Thread worker =
                          new Thread(
                              () -> {
                                innerEmitter.onNext(number);
                                innerEmitter.onComplete();
                              });
                      worker.setDaemon(true);
                      worker.start();
                    }))
        .subscribe(
            new Observer<>() {
              @Override
              public void onNext(Integer item) {
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

    Set<Integer> uniqueItems = new HashSet<>(receivedItems);

    assertEquals(itemCount, receivedItems.size());
    assertEquals(itemCount, uniqueItems.size());
    assertTrue(completed.get());
    assertNull(error.get());
  }

  /**
   * Verifies that flatMap completion is signaled only once in a concurrent inner-stream scenario.
   *
   * <p>If this test fails intermittently, it may indicate a terminal-state race.
   *
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  @Test
  void shouldSignalCompletionOnlyOnceUnderConcurrentFlatMapLoad() throws InterruptedException {
    int itemCount = 300;

    AtomicInteger completionCount = new AtomicInteger(0);
    AtomicReference<Throwable> error = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              for (int i = 1; i <= itemCount; i++) {
                emitter.onNext(i);
              }
              emitter.onComplete();
            });

    observable
        .flatMap(
            (Integer number) ->
                Observable.<Integer>create(
                    innerEmitter -> {
                      Thread worker =
                          new Thread(
                              () -> {
                                innerEmitter.onNext(number);
                                innerEmitter.onComplete();
                              });
                      worker.setDaemon(true);
                      worker.start();
                    }))
        .subscribe(
            new Observer<>() {
              @Override
              public void onNext(Integer item) {
                // No-op.
              }

              @Override
              public void onError(Throwable throwable) {
                error.set(throwable);
                latch.countDown();
              }

              @Override
              public void onComplete() {
                completionCount.incrementAndGet();
                latch.countDown();
              }
            });

    assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

    Thread.sleep(200);

    assertNull(error.get());
    assertEquals(1, completionCount.get());
  }

  /**
   * Verifies that observeOn with a single-thread scheduler preserves item order even under higher
   * load.
   *
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  @Test
  void shouldPreserveOrderWithObserveOnSingleThreadUnderLoad() throws InterruptedException {
    int itemCount = 2_000;

    List<Integer> expectedItems = new ArrayList<>();
    for (int i = 1; i <= itemCount; i++) {
      expectedItems.add(i);
    }

    List<Integer> receivedItems = new CopyOnWriteArrayList<>();
    AtomicBoolean completed = new AtomicBoolean(false);
    AtomicReference<Throwable> error = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              for (int i = 1; i <= itemCount; i++) {
                emitter.onNext(i);
              }
              emitter.onComplete();
            });

    observable
        .observeOn(new SingleThreadScheduler())
        .subscribe(
            new Observer<>() {
              @Override
              public void onNext(Integer item) {
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

    assertEquals(expectedItems, receivedItems);
    assertTrue(completed.get());
    assertNull(error.get());
  }

  /**
   * Verifies that disposal during concurrent emission stops the stream without crashing the
   * observer.
   *
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  @Test
  void shouldHandleDisposeDuringConcurrentEmissionWithoutCrashing() throws InterruptedException {
    List<Integer> receivedItems = new CopyOnWriteArrayList<>();
    AtomicReference<Throwable> error = new AtomicReference<>();
    AtomicBoolean completed = new AtomicBoolean(false);
    CountDownLatch firstHundredLatch = new CountDownLatch(1);

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              Thread worker =
                  new Thread(
                      () -> {
                        for (int i = 1; i <= 10_000; i++) {
                          if (emitter.isDisposed()) {
                            return;
                          }

                          emitter.onNext(i);
                        }

                        emitter.onComplete();
                      });

              worker.setDaemon(true);
              worker.start();
            });

    final Disposable[] disposableHolder = new Disposable[1];

    disposableHolder[0] =
        observable.subscribe(
            new Observer<>() {
              @Override
              public void onNext(Integer item) {
                receivedItems.add(item);

                if (receivedItems.size() >= 100
                    && disposableHolder[0] != null
                    && !disposableHolder[0].isDisposed()) {
                  disposableHolder[0].dispose();
                  firstHundredLatch.countDown();
                }
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

    assertTrue(firstHundredLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

    Thread.sleep(300);

    assertTrue(disposableHolder[0].isDisposed());
    assertNull(error.get());
    assertFalse(completed.get());
    assertTrue(receivedItems.size() >= 100);
    assertTrue(receivedItems.size() < 10_000);
  }
}
