package org.example.minirx.demo;

import org.example.minirx.core.Disposable;
import org.example.minirx.core.Observable;
import org.example.minirx.core.Observer;
import org.example.minirx.scheduler.IOThreadScheduler;
import org.example.minirx.scheduler.SingleThreadScheduler;

/**
 * Demonstrates basic usage of the Mini RxJava library.
 *
 * <p>This class contains simple examples of:
 * <ul>
 *     <li>successful stream completion,</li>
 *     <li>error handling,</li>
 *     <li>subscription cancellation with {@link Disposable},</li>
 *     <li>data transformation with {@code map(...)},</li>
 *     <li>data filtering with {@code filter(...)},</li>
 *     <li>stream expansion with {@code flatMap(...)},</li>
 *     <li>subscription scheduling with {@code subscribeOn(...)},</li>
 *     <li>observer scheduling with {@code observeOn(...)}.</li>
 * </ul>
 *
 * <p>The asynchronous examples use sleeps only for demo visibility.
 */
public final class DemoMain {

    /**
     * Prevents creating utility class instances.
     */
    private DemoMain() {
    }

    /**
     * Runs all demonstration examples.
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

    /**
     * Demonstrates a simple successful stream.
     */
    private static void demoSuccessfulStream() {
        System.out.println("=== Demo 1: Successful stream ===");

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        observable.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                System.out.println("Received item: " + item);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("Stream completed successfully");
            }
        });

        System.out.println();
    }

    /**
     * Demonstrates stream termination with an error.
     */
    private static void demoStreamWithError() {
        System.out.println("=== Demo 2: Stream with error ===");

        Observable<String> observable = Observable.create(emitter -> {
            emitter.onNext("First value");
            throw new RuntimeException("Something went wrong in the source");
        });

        observable.subscribe(new Observer<String>() {
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
                System.out.println("Stream completed successfully");
            }
        });

        System.out.println();
    }

    /**
     * Demonstrates cancellation of a running stream.
     */
    private static void demoDispose() {
        System.out.println("=== Demo 3: Dispose subscription ===");

        Observable<Integer> observable = Observable.create(emitter -> {
            Thread worker = new Thread(() -> {
                for (int i = 1; i <= 10; i++) {
                    if (emitter.isDisposed()) {
                        System.out.println("Source stopped because the subscription was disposed");
                        return;
                    }

                    emitter.onNext(i);
                    sleep(300);
                }

                emitter.onComplete();
            });

            worker.start();
        });

        Disposable disposable = observable.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                System.out.println("Received item: " + item);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("Stream completed successfully");
            }
        });

        sleep(1000);
        disposable.dispose();
        System.out.println("Main thread disposed the subscription");

        sleep(1000);
        System.out.println();
    }

    /**
     * Demonstrates the map operator.
     */
    private static void demoMapOperator() {
        System.out.println("=== Demo 4: Map operator ===");

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        observable
                .map(number -> number * 10)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        System.out.println("Mapped item: " + item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Error: " + throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("Stream completed successfully");
                    }
                });

        System.out.println();
    }

    /**
     * Demonstrates the filter operator.
     */
    private static void demoFilterOperator() {
        System.out.println("=== Demo 5: Filter operator ===");

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onNext(4);
            emitter.onNext(5);
            emitter.onComplete();
        });

        observable
                .filter(number -> number % 2 == 0)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        System.out.println("Filtered item: " + item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Error: " + throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("Stream completed successfully");
                    }
                });

        System.out.println();
    }

    /**
     * Demonstrates chaining of map and filter operators.
     */
    private static void demoMapAndFilterChain() {
        System.out.println("=== Demo 6: Map + Filter chain ===");

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onNext(4);
            emitter.onComplete();
        });

        observable
                .map(number -> number * 10)
                .filter(number -> number > 20)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        System.out.println("Chained item: " + item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Error: " + throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("Stream completed successfully");
                    }
                });

        System.out.println();
    }

    /**
     * Demonstrates the flatMap operator.
     */
    private static void demoFlatMapOperator() {
        System.out.println("=== Demo 7: FlatMap operator ===");

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        observable
                .flatMap(number -> Observable.<Integer>create(innerEmitter -> {
                    innerEmitter.onNext(number);
                    innerEmitter.onNext(number * 10);
                    innerEmitter.onComplete();
                }))
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        System.out.println("FlatMapped item: " + item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Error: " + throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("Stream completed successfully");
                    }
                });

        System.out.println();
    }

    /**
     * Demonstrates the subscribeOn operator.
     */
    private static void demoSubscribeOn() {
        System.out.println("=== Demo 8: subscribeOn ===");

        Observable<Integer> observable = Observable.create(emitter -> {
            System.out.println("Source thread: " + Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onComplete();
        });

        observable
                .subscribeOn(new IOThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        System.out.println("Observer received " + item + " on thread: "
                                + Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Error: " + throwable.getMessage());
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
     * Demonstrates the observeOn operator.
     */
    private static void demoObserveOn() {
        System.out.println("=== Demo 9: observeOn ===");

        Observable<Integer> observable = Observable.create(emitter -> {
            System.out.println("Source thread: " + Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        observable
                .observeOn(new SingleThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        System.out.println("Observed item " + item + " on thread: "
                                + Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Error: " + throwable.getMessage());
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
     * Pauses the current thread for the given number of milliseconds.
     *
     * @param millis the pause duration in milliseconds
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}