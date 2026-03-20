package org.example.minirx.core;

import org.example.minirx.internal.CreateEmitter;
import org.example.minirx.scheduler.Scheduler;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Main reactive source type in the project.
 *
 * <p>An {@code Observable} keeps the subscription logic instead of storing
 * items in advance. When a subscriber appears, that logic is executed and
 * pushes signals through an {@link Emitter}.
 *
 * @param <T> type of emitted items
 */
public class Observable<T> {

    /**
     * Logic executed for each new subscriber.
     */
    private final ObservableOnSubscribe<T> source;

    /**
     * Creates an observable with the given source logic.
     *
     * @param source source callback
     */
    public Observable(ObservableOnSubscribe<T> source) {
        this.source = Objects.requireNonNull(source, "source must not be null");
    }

    /**
     * Factory method for creating an observable.
     *
     * @param source source callback to run on subscription
     * @param <T> type of emitted items
     * @return new observable instance
     */
    public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        return new Observable<>(source);
    }

    /**
     * Subscribes the given observer to this source.
     *
     * @param observer observer that will receive signals
     * @return disposable handle for the subscription
     */
    public Disposable subscribe(Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer must not be null");

        CreateEmitter<T> emitter = new CreateEmitter<>(observer);

        try {
            source.subscribe(emitter);
        } catch (Throwable throwable) {
            emitter.onError(throwable);
        }

        return emitter;
    }

    /**
     * Applies a mapping function to each emitted item.
     *
     * @param mapper mapping function
     * @param <R> type of items produced by the resulting observable
     * @return observable with mapped items
     */
    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");

        return Observable.create(emitter ->
                Observable.this.subscribe(new Observer<>() {
                    @Override
                    public void onNext(T item) {
                        if (emitter.isDisposed()) {
                            return;
                        }

                        R mappedItem;

                        try {
                            mappedItem = mapper.apply(item);
                        } catch (Throwable throwable) {
                            emitter.onError(throwable);
                            return;
                        }

                        emitter.onNext(mappedItem);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        emitter.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        emitter.onComplete();
                    }
                })
        );
    }

    /**
     * Lets through only items that match the predicate.
     *
     * @param predicate filter condition
     * @return observable that emits only accepted items
     */
    public Observable<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");

        return Observable.create(emitter ->
                Observable.this.subscribe(new Observer<>() {
                    @Override
                    public void onNext(T item) {
                        if (emitter.isDisposed()) {
                            return;
                        }

                        boolean shouldEmit;

                        try {
                            shouldEmit = predicate.test(item);
                        } catch (Throwable throwable) {
                            emitter.onError(throwable);
                            return;
                        }

                        if (shouldEmit) {
                            emitter.onNext(item);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        emitter.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        emitter.onComplete();
                    }
                })
        );
    }

    /**
     * Maps each item to an inner observable and merges their output.
     *
     * <p>The resulting stream is completed only after the outer source and all
     * currently active inner sources have completed.
     *
     * @param mapper function that creates an inner observable for each item
     * @param <R> type emitted by inner observables
     * @return merged observable
     */
    public <R> Observable<R> flatMap(Function<? super T, Observable<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");

        return Observable.create(emitter -> {
            AtomicInteger activeSources = new AtomicInteger(1);

            Observable.this.subscribe(new Observer<>() {
                @Override
                public void onNext(T item) {
                    if (emitter.isDisposed()) {
                        return;
                    }

                    Observable<R> innerObservable;

                    try {
                        innerObservable = Objects.requireNonNull(
                                mapper.apply(item),
                                "mapper returned null"
                        );
                    } catch (Throwable throwable) {
                        emitter.onError(throwable);
                        return;
                    }

                    activeSources.incrementAndGet();

                    innerObservable.subscribe(new Observer<>() {
                        @Override
                        public void onNext(R innerItem) {
                            if (emitter.isDisposed()) {
                                return;
                            }

                            emitter.onNext(innerItem);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            emitter.onError(throwable);
                        }

                        @Override
                        public void onComplete() {
                            if (activeSources.decrementAndGet() == 0) {
                                emitter.onComplete();
                            }
                        }
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    emitter.onError(throwable);
                }

                @Override
                public void onComplete() {
                    if (activeSources.decrementAndGet() == 0) {
                        emitter.onComplete();
                    }
                }
            });
        });
    }

    /**
     * Runs subscription and upstream execution on the given scheduler.
     *
     * <p>Here the whole upstream subscription starts inside the scheduled task.
     *
     * @param scheduler scheduler used for subscription
     * @return observable with scheduled subscription
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler must not be null");

        return Observable.create(emitter ->
                scheduler.execute(() -> {
                    if (emitter.isDisposed()) {
                        return;
                    }

                    Observable.this.subscribe(new Observer<>() {
                        @Override
                        public void onNext(T item) {
                            if (emitter.isDisposed()) {
                                return;
                            }

                            emitter.onNext(item);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            emitter.onError(throwable);
                        }

                        @Override
                        public void onComplete() {
                            emitter.onComplete();
                        }
                    });
                })
        );
    }

    /**
     * Delivers downstream signals on the given scheduler.
     *
     * <p>In this implementation each signal is submitted as a separate task.
     * For stable ordering, a single-thread scheduler is the safest option.
     *
     * @param scheduler scheduler used for downstream notifications
     * @return observable with scheduled observer callbacks
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler must not be null");

        return Observable.create(emitter ->
                Observable.this.subscribe(new Observer<>() {
                    @Override
                    public void onNext(T item) {
                        scheduler.execute(() -> {
                            if (emitter.isDisposed()) {
                                return;
                            }

                            emitter.onNext(item);
                        });
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        scheduler.execute(() -> emitter.onError(throwable));
                    }

                    @Override
                    public void onComplete() {
                        scheduler.execute(emitter::onComplete);
                    }
                })
        );
    }
}
