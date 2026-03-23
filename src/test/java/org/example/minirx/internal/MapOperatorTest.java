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
 * Tests the map operator.
 *
 * <p>This class verifies:
 *
 * <ul>
 *   <li>item transformation with {@code map(...)}
 *   <li>successful completion after mapping
 *   <li>error propagation when the mapper fails
 * </ul>
 */
class MapOperatorTest {
  private static final int NUMBER_TWO =
      2; // fixing PMD warning ::avoid using literals in if statements

  /** Verifies that the map operator transforms all items correctly. */
  @Test
  void shouldTransformItemsWithMap() {
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
        .map(number -> number * 10)
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

    assertEquals(List.of(10, 20, 30), receivedItems);
    assertTrue(completed.get());
    assertNull(error.get());
  }

  /** Verifies that an exception thrown inside the mapper is forwarded to {@code onError(...)}. */
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
        .map(
            number -> {
              if (number == NUMBER_TWO) {
                throw new IllegalStateException("Map failure");
              }
              return number * 10;
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
    assertEquals("Map failure", error.get().getMessage());
  }
}
