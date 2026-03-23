package org.example.minirx.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.example.minirx.core.Emitter;
import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.example.minirx.scheduler.ComputationScheduler;
import org.example.minirx.scheduler.IOThreadScheduler;
import org.example.minirx.scheduler.SingleThreadScheduler;
import org.junit.jupiter.api.Test;

/**
 * Verifies additional null contracts and defensive checks that are not covered by the main
 * operator-focused test classes.
 */
class ContractValidationTest {

  /** Verifies that the Observable constructor rejects a null source callback. */
  @Test
  void shouldRejectNullSourceInObservableConstructor() {
    assertThrows(NullPointerException.class, () -> new Observable<Integer>(null));
  }

  /** Verifies that Observable.create rejects a null source callback. */
  @Test
  void shouldRejectNullSourceInCreate() {
    assertThrows(NullPointerException.class, () -> Observable.create(null));
  }

  /** Verifies that subscribeOn rejects a null scheduler. */
  @Test
  void shouldRejectNullSchedulerInSubscribeOn() {
    Observable<Integer> observable = Observable.create(Emitter::onComplete);

    assertThrows(NullPointerException.class, () -> observable.subscribeOn(null));
  }

  /** Verifies that observeOn rejects a null scheduler. */
  @Test
  void shouldRejectNullSchedulerInObserveOn() {
    Observable<Integer> observable = Observable.create(Emitter::onComplete);

    assertThrows(NullPointerException.class, () -> observable.observeOn(null));
  }

  /**
   * Verifies that flatMap forwards an error when the mapper returns null instead of an inner
   * observable.
   */
  @Test
  void shouldSignalErrorWhenFlatMapMapperReturnsNull() {
    AtomicReference<Throwable> error = new AtomicReference<>();
    AtomicBoolean completed = new AtomicBoolean(false);

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              emitter.onNext(1);
              emitter.onComplete();
            });

    observable
        .flatMap(number -> null)
        .subscribe(
            new Observer<>() {
              @Override
              public void onNext(Object item) {
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

    assertEquals(NullPointerException.class, error.get() == null ? null : error.get().getClass());
    assertEquals("mapper returned null", error.get() == null ? null : error.get().getMessage());
    assertFalse(completed.get());
  }

  /** Verifies that scheduler implementations reject null tasks. */
  @Test
  void shouldRejectNullTaskInSchedulerImplementations() {
    assertThrows(NullPointerException.class, () -> new IOThreadScheduler().execute(null));
    assertThrows(NullPointerException.class, () -> new ComputationScheduler().execute(null));
    assertThrows(NullPointerException.class, () -> new SingleThreadScheduler().execute(null));
  }
}
