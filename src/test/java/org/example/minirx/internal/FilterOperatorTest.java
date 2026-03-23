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
 * Tests the filter operator.
 *
 * <p>This class verifies:
 *
 * <ul>
 *   <li>item selection with {@code filter(...)}
 *   <li>successful completion after filtering
 *   <li>error propagation when the predicate fails
 * </ul>
 */
class FilterOperatorTest {
  private static final int NUMBER_TWO =
      2; // fixing PMD warning ::avoid using literals in if statements

  /** Verifies that the filter operator passes only matching items. */
  @Test
  void shouldPassOnlyMatchingItems() {
    List<Integer> receivedItems = new ArrayList<>();
    AtomicBoolean completed = new AtomicBoolean(false);
    AtomicReference<Throwable> error = new AtomicReference<>();

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              emitter.onNext(1);
              emitter.onNext(2);
              emitter.onNext(3);
              emitter.onNext(4);
              emitter.onComplete();
            });

    observable
        .filter(number -> number % 2 == 0)
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

    assertEquals(List.of(2, 4), receivedItems);
    assertTrue(completed.get());
    assertNull(error.get());
  }

  /**
   * Verifies that an exception thrown inside the predicate is forwarded to {@code onError(...)}.
   */
  @Test
  void shouldForwardPredicateErrorToObserver() {
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
        .filter(
            number -> {
              if (number == NUMBER_TWO) {
                throw new IllegalArgumentException("Filter failure");
              }
              return number % 2 != 0;
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

    assertEquals(List.of(1), receivedItems);
    assertFalse(completed.get());
    assertNotNull(error.get());
    assertEquals("Filter failure", error.get().getMessage());
  }
}
