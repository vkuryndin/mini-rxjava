package org.example.minirx.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.junit.jupiter.api.Test;

/**
 * Tests the flatMap operator.
 *
 * <p>This class verifies:
 *
 * <ul>
 *   <li>flattening of inner observables,
 *   <li>error propagation from the mapper,
 *   <li>error propagation from inner observables,
 *   <li>proper completion after inner observables finish.
 * </ul>
 */
class FlatMapOperatorTest {
  private static final int NUMBER_TWO =
      2; // fixing PMD warning ::avoid using literals in if statements

  /**
   * Verifies that flatMap transforms each source item into an inner observable and merges all
   * emitted inner items.
   */
  @Test
  void shouldFlattenInnerObservables() {
    List<Integer> receivedItems = new ArrayList<>();
    AtomicBoolean completed = new AtomicBoolean(false);
    AtomicReference<Throwable> error = new AtomicReference<>();

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              emitter.onNext(1);
              emitter.onNext(2);
              emitter.onNext(3);
              emitter.onComplete();
            });

    observable
        .flatMap(
            number ->
                Observable.<Integer>create(
                    innerEmitter -> {
                      innerEmitter.onNext(number);
                      innerEmitter.onNext(number * 10);
                      innerEmitter.onComplete();
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
              }

              @Override
              public void onComplete() {
                completed.set(true);
              }
            });

    assertEquals(List.of(1, 10, 2, 20, 3, 30), receivedItems);
    assertTrue(completed.get());
    assertNull(error.get());
  }

  /**
   * Verifies that an exception thrown by the flatMap mapper is forwarded to {@code onError(...)}.
   */
  @Test
  void shouldForwardMapperErrorToObserver() {
    List<Integer> receivedItems = new ArrayList<>();
    AtomicBoolean completed = new AtomicBoolean(false);
    AtomicReference<Throwable> error = new AtomicReference<>();

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              emitter.onNext(1);
              emitter.onNext(2);
              emitter.onNext(3);
              emitter.onComplete();
            });

    observable
        .flatMap(
            number -> {
              if (number == NUMBER_TWO) {
                throw new IllegalStateException("FlatMap mapper failure");
              }

              return Observable.<Integer>create(
                  innerEmitter -> {
                    innerEmitter.onNext(number * 10);
                    innerEmitter.onComplete();
                  });
            })
        .subscribe(
            new Observer<>() {
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
    assertEquals("FlatMap mapper failure", error.get().getMessage());
  }

  /**
   * Verifies that an error signaled by an inner observable is forwarded to {@code onError(...)}.
   */
  @Test
  void shouldForwardInnerObservableErrorToObserver() {
    List<Integer> receivedItems = new ArrayList<>();
    AtomicBoolean completed = new AtomicBoolean(false);
    AtomicReference<Throwable> error = new AtomicReference<>();

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              emitter.onNext(1);
              emitter.onNext(2);
              emitter.onComplete();
            });

    observable
        .flatMap(
            number ->
                Observable.<Integer>create(
                    innerEmitter -> {
                      innerEmitter.onNext(number);

                      if (number == NUMBER_TWO) {
                        innerEmitter.onError(new RuntimeException("Inner observable failure"));
                        return;
                      }

                      innerEmitter.onComplete();
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
              }

              @Override
              public void onComplete() {
                completed.set(true);
              }
            });

    assertEquals(List.of(1, 2), receivedItems);
    assertFalse(completed.get());
    assertNotNull(error.get());
    assertEquals("Inner observable failure", error.get().getMessage());
  }

  /**
   * Verifies that the resulting stream completes only after the outer observable and all inner
   * observables complete.
   */
  @Test
  void shouldCompleteAfterOuterAndInnerObservablesComplete() {
    List<String> receivedItems = new ArrayList<>();
    AtomicBoolean completed = new AtomicBoolean(false);
    AtomicReference<Throwable> error = new AtomicReference<>();

    Observable<String> observable =
        Observable.create(
            emitter -> {
              emitter.onNext("A");
              emitter.onNext("B");
              emitter.onComplete();
            });

    observable
        .flatMap(
            letter ->
                Observable.<String>create(
                    innerEmitter -> {
                      innerEmitter.onNext(letter + "1");
                      innerEmitter.onNext(letter + "2");
                      innerEmitter.onComplete();
                    }))
        .subscribe(
            new Observer<>() {
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

    assertEquals(List.of("A1", "A2", "B1", "B2"), receivedItems);
    assertTrue(completed.get());
    assertNull(error.get());
  }
}
