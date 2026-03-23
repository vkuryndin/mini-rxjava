package org.example.minirx.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Tests subscription cancellation through {@link Disposable}.
 *
 * <p>This class verifies:
 *
 * <ul>
 *   <li>manual disposal of a subscription,
 *   <li>stopping further item delivery after disposal,
 *   <li>disposal state tracking.
 * </ul>
 */
class DisposableTest {

  /** Maximum time to wait for asynchronous test completion. */
  private static final long TIMEOUT_SECONDS = 3;

  private static final int NUMBER_ONE = 1;

  /**
   * Verifies that disposing a subscription stops further item delivery.
   *
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  @Test
  void shouldStopReceivingItemsAfterDispose() throws InterruptedException {
    List<Integer> receivedItems = new CopyOnWriteArrayList<>();
    AtomicBoolean completed = new AtomicBoolean(false);
    AtomicBoolean errorReceived = new AtomicBoolean(false);
    CountDownLatch firstItemLatch = new CountDownLatch(1);

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              Thread worker =
                  new Thread(
                      () -> {
                        for (int i = 1; i <= 10; i++) {
                          if (emitter.isDisposed()) {
                            return;
                          }

                          emitter.onNext(i);

                          try {
                            Thread.sleep(100);
                          } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            return;
                          }
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
                firstItemLatch.countDown();

                if (item == NUMBER_ONE) {
                  disposableHolder[0].dispose();
                }
              }

              @Override
              public void onError(Throwable throwable) {
                errorReceived.set(true);
              }

              @Override
              public void onComplete() {
                completed.set(true);
              }
            });

    assertTrue(firstItemLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

    Thread.sleep(500);

    assertEquals(List.of(1), receivedItems);
    assertTrue(disposableHolder[0].isDisposed());
    assertFalse(completed.get());
    assertFalse(errorReceived.get());
  }

  /** Verifies that a new subscription is not disposed by default. */
  @Test
  void shouldNotBeDisposedImmediatelyAfterSubscribe() {
    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              // No signals are emitted in this test.
            });

    Disposable disposable =
        observable.subscribe(
            new Observer<>() {
              @Override
              public void onNext(Integer item) {
                // No-op.
              }

              @Override
              public void onError(Throwable throwable) {
                // No-op.
              }

              @Override
              public void onComplete() {
                // No-op.
              }
            });

    assertFalse(disposable.isDisposed());
  }

  /** Verifies that calling dispose marks the subscription as disposed. */
  @Test
  void shouldMarkDisposableAsDisposedAfterDisposeCall() {
    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              // No signals are emitted in this test.
            });

    Disposable disposable =
        observable.subscribe(
            new Observer<>() {
              @Override
              public void onNext(Integer item) {
                // No-op.
              }

              @Override
              public void onError(Throwable throwable) {
                // No-op.
              }

              @Override
              public void onComplete() {
                // No-op.
              }
            });

    disposable.dispose();

    assertTrue(disposable.isDisposed());
  }
}
