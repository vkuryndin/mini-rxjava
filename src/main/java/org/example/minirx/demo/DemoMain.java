package org.example.minirx.demo;

import org.example.minirx.core.Disposable;
import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.example.minirx.scheduler.IOThreadScheduler;
import org.example.minirx.scheduler.SingleThreadScheduler;

/**
 * Small set of examples for the Mini RxJava API.
 *
 * <p>The demos show normal completion, error handling, disposal, basic operators, and simple
 * scheduling.
 */
@SuppressWarnings("PMD.SystemPrintln") // suppress PMD warning about System.out.println()
public final class DemoMain {

  /** Utility class. */
  private DemoMain() {}

  private static final String ERROR_PREFIX = "Error: "; // fixing PMD comments

  private static final String STREAM_COMPLETED_MESSAGE =
      "Stream completed successfully"; // fixing PMD comments

  /**
   * Runs all demo scenarios.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    demoSuccessfulStream();
    demoStreamWithError();
    demoDispose();
    demoMapOperator();
    demoFilterOperator();
    demoMapAndFilterChain();
    demoFlatMapOperator();
    demoSubscribeOn();
    demoObserveOn();
  }

  /** Example of a source that completes normally. */
  private static void demoSuccessfulStream() {
    System.out.println("=== Demo 1: Successful stream ===");

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              emitter.onNext(1);
              emitter.onNext(2);
              emitter.onNext(3);
              emitter.onComplete();
            });

    observable.subscribe(
        new Observer<>() {
          @Override
          public void onNext(Integer item) {
            System.out.println("Received item: " + item);
          }

          @Override
          public void onError(Throwable throwable) {
            System.out.println(ERROR_PREFIX + throwable.getMessage());
          }

          @Override
          public void onComplete() {
            System.out.println(STREAM_COMPLETED_MESSAGE);
          }
        });

    System.out.println();
  }

  /** Example of a source that finishes with an error. */
  private static void demoStreamWithError() {
    System.out.println("=== Demo 2: Stream with error ===");

    Observable<String> observable =
        Observable.create(
            emitter -> {
              emitter.onNext("First value");
              throw new RuntimeException("Something went wrong in the source");
            });

    observable.subscribe(
        new Observer<>() {
          @Override
          public void onNext(String item) {
            System.out.println("Received item: " + item);
          }

          @Override
          public void onError(Throwable throwable) {
            System.out.println("Error received: " + throwable.getMessage());
          }

          @Override
          public void onComplete() {
            System.out.println(STREAM_COMPLETED_MESSAGE);
          }
        });

    System.out.println();
  }

  /** Example showing subscription disposal during execution. */
  private static void demoDispose() {
    System.out.println("=== Demo 3: Dispose subscription ===");

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              Thread worker =
                  new Thread(
                      () -> {
                        for (int i = 1; i <= 10; i++) {
                          if (emitter.isDisposed()) {
                            System.out.println(
                                "Source stopped because the subscription was disposed");
                            return;
                          }

                          emitter.onNext(i);
                          sleep(300);
                        }

                        emitter.onComplete();
                      });

              worker.start();
            });

    Disposable disposable =
        observable.subscribe(
            new Observer<>() {
              @Override
              public void onNext(Integer item) {
                System.out.println("Received item: " + item);
              }

              @Override
              public void onError(Throwable throwable) {
                System.out.println(ERROR_PREFIX + throwable.getMessage());
              }

              @Override
              public void onComplete() {
                System.out.println(STREAM_COMPLETED_MESSAGE);
              }
            });

    sleep(1000);
    disposable.dispose();
    System.out.println("Main thread disposed the subscription");

    sleep(1000);
    System.out.println();
  }

  /** Example of the map operator. */
  private static void demoMapOperator() {
    System.out.println("=== Demo 4: Map operator ===");

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
                System.out.println("Mapped item: " + item);
              }

              @Override
              public void onError(Throwable throwable) {
                System.out.println(ERROR_PREFIX + throwable.getMessage());
              }

              @Override
              public void onComplete() {
                System.out.println(STREAM_COMPLETED_MESSAGE);
              }
            });

    System.out.println();
  }

  /** Example of the filter operator. */
  private static void demoFilterOperator() {
    System.out.println("=== Demo 5: Filter operator ===");

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              emitter.onNext(1);
              emitter.onNext(2);
              emitter.onNext(3);
              emitter.onNext(4);
              emitter.onNext(5);
              emitter.onComplete();
            });

    observable
        .filter(number -> number % 2 == 0)
        .subscribe(
            new Observer<>() {
              @Override
              public void onNext(Integer item) {
                System.out.println("Filtered item: " + item);
              }

              @Override
              public void onError(Throwable throwable) {
                System.out.println(ERROR_PREFIX + throwable.getMessage());
              }

              @Override
              public void onComplete() {
                System.out.println(STREAM_COMPLETED_MESSAGE);
              }
            });

    System.out.println();
  }

  /** Example of chaining map and filter. */
  private static void demoMapAndFilterChain() {
    System.out.println("=== Demo 6: Map + Filter chain ===");

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
        .map(number -> number * 10)
        .filter(number -> number > 20)
        .subscribe(
            new Observer<>() {
              @Override
              public void onNext(Integer item) {
                System.out.println("Chained item: " + item);
              }

              @Override
              public void onError(Throwable throwable) {
                System.out.println(ERROR_PREFIX + throwable.getMessage());
              }

              @Override
              public void onComplete() {
                System.out.println(STREAM_COMPLETED_MESSAGE);
              }
            });

    System.out.println();
  }

  /** Example of the flatMap operator. */
  private static void demoFlatMapOperator() {
    System.out.println("=== Demo 7: FlatMap operator ===");

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
                System.out.println("FlatMapped item: " + item);
              }

              @Override
              public void onError(Throwable throwable) {
                System.out.println(ERROR_PREFIX + throwable.getMessage());
              }

              @Override
              public void onComplete() {
                System.out.println(STREAM_COMPLETED_MESSAGE);
              }
            });

    System.out.println();
  }

  /** Example of subscribeOn. */
  private static void demoSubscribeOn() {
    System.out.println("=== Demo 8: subscribeOn ===");

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              System.out.println("Source thread: " + Thread.currentThread().getName());
              emitter.onNext(1);
              emitter.onNext(2);
              emitter.onComplete();
            });

    observable
        .subscribeOn(new IOThreadScheduler())
        .subscribe(
            new Observer<>() {
              @Override
              public void onNext(Integer item) {
                System.out.println(
                    "Observer received "
                        + item
                        + " on thread: "
                        + Thread.currentThread().getName());
              }

              @Override
              public void onError(Throwable throwable) {
                System.out.println(ERROR_PREFIX + throwable.getMessage());
              }

              @Override
              public void onComplete() {
                System.out.println("Completed on thread: " + Thread.currentThread().getName());
              }
            });

    sleep(1000);
    System.out.println();
  }

  /** Example of observeOn. */
  private static void demoObserveOn() {
    System.out.println("=== Demo 9: observeOn ===");

    Observable<Integer> observable =
        Observable.create(
            emitter -> {
              System.out.println("Source thread: " + Thread.currentThread().getName());
              emitter.onNext(1);
              emitter.onNext(2);
              emitter.onNext(3);
              emitter.onComplete();
            });

    observable
        .observeOn(new SingleThreadScheduler())
        .subscribe(
            new Observer<>() {
              @Override
              public void onNext(Integer item) {
                System.out.println(
                    "Observed item " + item + " on thread: " + Thread.currentThread().getName());
              }

              @Override
              public void onError(Throwable throwable) {
                System.out.println(ERROR_PREFIX + throwable.getMessage());
              }

              @Override
              public void onComplete() {
                System.out.println("Completed on thread: " + Thread.currentThread().getName());
              }
            });

    sleep(1000);
    System.out.println();
  }

  /**
   * Sleeps for the specified number of milliseconds.
   *
   * @param millis sleep duration in milliseconds
   */
  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }
}
