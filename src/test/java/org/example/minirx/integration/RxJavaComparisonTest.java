package org.example.minirx.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.junit.jupiter.api.Test;

/**
 * Compares the behavior of the educational Mini RxJava implementation with the real RxJava library
 * in key scenarios.
 *
 * <p>This class focuses on semantic behavior rather than performance. The goal is to verify that
 * both implementations produce the same:
 *
 * <ul>
 *   <li>items,
 *   <li>completion behavior,
 *   <li>error behavior.
 * </ul>
 */
class RxJavaComparisonTest {

  private static final int NUMBER_TWO =
      2; // fixing PMD warning ::avoid using literals in if statements

  /**
   * Verifies that a simple source produces the same items and completion behavior in both
   * implementations.
   */
  @Test
  void shouldMatchRxJavaForSimpleCreate() {
    ComparisonResult<Integer> miniRxResult =
        collectFromMiniRx(
            Observable.create(
                emitter -> {
                  emitter.onNext(1);
                  emitter.onNext(2);
                  emitter.onNext(3);
                  emitter.onComplete();
                }));

    ComparisonResult<Integer> rxJavaResult =
        collectFromRxJava(
            io.reactivex.rxjava3.core.Observable.create(
                emitter -> {
                  emitter.onNext(1);
                  emitter.onNext(2);
                  emitter.onNext(3);
                  emitter.onComplete();
                }));

    assertComparisonEquals(miniRxResult, rxJavaResult);
  }

  /** Verifies that the map operator behaves the same in both implementations. */
  @Test
  void shouldMatchRxJavaForMapOperator() {
    ComparisonResult<Integer> miniRxResult =
        collectFromMiniRx(
            Observable.<Integer>create(
                    emitter -> {
                      emitter.onNext(1);
                      emitter.onNext(2);
                      emitter.onNext(3);
                      emitter.onComplete();
                    })
                .map((Integer number) -> number * 10));

    ComparisonResult<Integer> rxJavaResult =
        collectFromRxJava(
            io.reactivex.rxjava3.core.Observable.<Integer>create(
                    emitter -> {
                      emitter.onNext(1);
                      emitter.onNext(2);
                      emitter.onNext(3);
                      emitter.onComplete();
                    })
                .map((Integer number) -> number * 10));

    assertComparisonEquals(miniRxResult, rxJavaResult);
  }

  /** Verifies that the filter operator behaves the same in both implementations. */
  @Test
  void shouldMatchRxJavaForFilterOperator() {
    ComparisonResult<Integer> miniRxResult =
        collectFromMiniRx(
            Observable.<Integer>create(
                    emitter -> {
                      emitter.onNext(1);
                      emitter.onNext(2);
                      emitter.onNext(3);
                      emitter.onNext(4);
                      emitter.onComplete();
                    })
                .filter((Integer number) -> number % 2 == 0));

    ComparisonResult<Integer> rxJavaResult =
        collectFromRxJava(
            io.reactivex.rxjava3.core.Observable.<Integer>create(
                    emitter -> {
                      emitter.onNext(1);
                      emitter.onNext(2);
                      emitter.onNext(3);
                      emitter.onNext(4);
                      emitter.onComplete();
                    })
                .filter((Integer number) -> number % 2 == 0));

    assertComparisonEquals(miniRxResult, rxJavaResult);
  }

  /**
   * Verifies that the flatMap operator behaves the same in both implementations for a simple
   * synchronous scenario.
   */
  @Test
  void shouldMatchRxJavaForFlatMapOperator() {
    ComparisonResult<Integer> miniRxResult =
        collectFromMiniRx(
            Observable.<Integer>create(
                    emitter -> {
                      emitter.onNext(1);
                      emitter.onNext(2);
                      emitter.onNext(3);
                      emitter.onComplete();
                    })
                .flatMap(
                    (Integer number) ->
                        Observable.create(
                            innerEmitter -> {
                              innerEmitter.onNext(number);
                              innerEmitter.onNext(number * 10);
                              innerEmitter.onComplete();
                            })));

    ComparisonResult<Integer> rxJavaResult =
        collectFromRxJava(
            io.reactivex.rxjava3.core.Observable.<Integer>create(
                    emitter -> {
                      emitter.onNext(1);
                      emitter.onNext(2);
                      emitter.onNext(3);
                      emitter.onComplete();
                    })
                .flatMap(
                    (Integer number) ->
                        io.reactivex.rxjava3.core.Observable.create(
                            innerEmitter -> {
                              innerEmitter.onNext(number);
                              innerEmitter.onNext(number * 10);
                              innerEmitter.onComplete();
                            })));

    assertComparisonEquals(miniRxResult, rxJavaResult);
  }

  /** Verifies that a source error is propagated the same way in both implementations. */
  @Test
  void shouldMatchRxJavaForSourceError() {
    ComparisonResult<Integer> miniRxResult =
        collectFromMiniRx(
            Observable.create(
                emitter -> {
                  emitter.onNext(1);
                  throw new RuntimeException("Source failure");
                }));

    ComparisonResult<Integer> rxJavaResult =
        collectFromRxJava(
            io.reactivex.rxjava3.core.Observable.create(
                emitter -> {
                  emitter.onNext(1);
                  throw new RuntimeException("Source failure");
                }));

    assertComparisonEquals(miniRxResult, rxJavaResult);
  }

  /** Verifies that a mapper error is propagated the same way in both implementations. */
  @Test
  void shouldMatchRxJavaForMapError() {
    ComparisonResult<Integer> miniRxResult =
        collectFromMiniRx(
            Observable.<Integer>create(
                    emitter -> {
                      emitter.onNext(1);
                      emitter.onNext(2);
                      emitter.onComplete();
                    })
                .map(
                    (Integer number) -> {
                      if (number == NUMBER_TWO) {
                        throw new IllegalStateException("Map failure");
                      }
                      return number * 10;
                    }));

    ComparisonResult<Integer> rxJavaResult =
        collectFromRxJava(
            io.reactivex.rxjava3.core.Observable.<Integer>create(
                    emitter -> {
                      emitter.onNext(1);
                      emitter.onNext(2);
                      emitter.onComplete();
                    })
                .map(
                    (Integer number) -> {
                      if (number == NUMBER_TWO) {
                        throw new IllegalStateException("Map failure");
                      }
                      return number * 10;
                    }));

    assertComparisonEquals(miniRxResult, rxJavaResult);
  }

  /**
   * Collects all signals from the Mini RxJava observable.
   *
   * @param observable the observable to collect from
   * @param <T> the item type
   * @return collected comparison data
   */
  private <T> ComparisonResult<T> collectFromMiniRx(Observable<T> observable) {
    List<T> items = new ArrayList<>();
    AtomicBoolean completed = new AtomicBoolean(false);
    AtomicReference<Throwable> error = new AtomicReference<>();

    observable.subscribe(
        new Observer<>() {
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

    return new ComparisonResult<>(items, completed.get(), error.get());
  }

  /**
   * Collects all signals from the real RxJava observable.
   *
   * @param observable the RxJava observable to collect from
   * @param <T> the item type
   * @return collected comparison data
   */
  private <T> ComparisonResult<T> collectFromRxJava(
      io.reactivex.rxjava3.core.Observable<T> observable) {
    List<T> items = new ArrayList<>();
    AtomicBoolean completed = new AtomicBoolean(false);
    AtomicReference<Throwable> error = new AtomicReference<>();

    io.reactivex.rxjava3.disposables.Disposable disposable =
        observable.subscribe(items::add, error::set, () -> completed.set(true));

    if (!disposable.isDisposed()) {
      disposable.dispose();
    }

    return new ComparisonResult<>(items, completed.get(), error.get());
  }

  /**
   * Verifies that two comparison results are equivalent.
   *
   * @param miniRxResult the result from the educational implementation
   * @param rxJavaResult the result from the real RxJava implementation
   * @param <T> the item type
   */
  private <T> void assertComparisonEquals(
      ComparisonResult<T> miniRxResult, ComparisonResult<T> rxJavaResult) {
    assertEquals(rxJavaResult.items(), miniRxResult.items());
    assertEquals(rxJavaResult.completed(), miniRxResult.completed());
    assertEquals(errorType(rxJavaResult.error()), errorType(miniRxResult.error()));
    assertEquals(errorMessage(rxJavaResult.error()), errorMessage(miniRxResult.error()));
  }

  /**
   * Returns the simple class name of the given error, or null if no error exists.
   *
   * @param throwable the error
   * @return the simple class name or null
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
   * Stores collected observable execution results for comparison.
   *
   * @param items the received items
   * @param completed whether the stream completed successfully
   * @param error the received error, if any
   * @param <T> the item type
   */
  private record ComparisonResult<T>(List<T> items, boolean completed, Throwable error) {}
}
