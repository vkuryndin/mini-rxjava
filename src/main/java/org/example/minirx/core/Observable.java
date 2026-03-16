package org.example.minirx.core;

import org.example.minirx.internal.CreateEmitter;
import org.example.minirx.scheduler.Scheduler;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a reactive source of items.
 *
 * <p>An observable does not usually store the emitted items directly.
 * Instead, it stores the subscription logic that describes what should
 * happen when an observer subscribes.
 *
 * <p>The source logic is represented by {@link ObservableOnSubscribe}.
 * It receives an {@link Emitter} and uses it to emit items, signal errors,
 * or complete the stream.
 *
 * @param <T> the type of emitted items
 */
public class Observable<T> {

    /**
     * Contains the logic that should run when someone subscribes.
     */
    private final ObservableOnSubscribe<T> source;

    /**
     * Creates a new observable with the given source logic.
     *
     * @param source the source logic of this observable
     */
    public Observable(ObservableOnSubscribe<T> source) {
        this.source = Objects.requireNonNull(source, "source must not be null");
    }

    /**
     * Creates a new observable from the given source logic.
     *
     * @param source the source logic to execute when subscribed
     * @param <T> the type of emitted items
     * @return a new observable instance
     */
    public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        return new Observable<>(source);
    }

    /**
     * Subscribes the given observer to this observable.
     *
     * @param observer the observer that will receive items, errors, and completion
     * @return a disposable that allows cancelling the subscription
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
     * Transforms each emitted item using the given mapper function.
     *
     * @param mapper the function used to transform items
     * @param <R> the type of items emitted by the resulting observable
     * @return a new observable that emits mapped items
     */
    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");

        return Observable.create(emitter ->
                Observable.this.subscribe(new Observer<T>() {
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
     * Emits only those items that match the given predicate.
     *
     * @param predicate the condition used to decide whether an item should pass
     * @return a new observable that emits only matching items
     */
    public Observable<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");

        return Observable.create(emitter ->
                Observable.this.subscribe(new Observer<T>() {
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
     * Transforms each emitted item into a new observable and merges
     * all inner observables into one resulting stream.
     *
     * <p>For every item from the current observable, the mapper returns
     * an inner observable. This operator subscribes to those inner observables
     * and forwards their items downstream.
     *
     * <p>The resulting stream completes only when:
     * <ul>
     *     <li>the outer observable completes, and</li>
     *     <li>all inner observables also complete.</li>
     * </ul>
     *
     * @param mapper the function that converts each item into an inner observable
     * @param <R> the type of items emitted by inner observables
     * @return a new observable that emits items from all inner observables
     */
    public <R> Observable<R> flatMap(Function<? super T, Observable<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");

        return Observable.create(emitter -> {
            AtomicInteger activeSources = new AtomicInteger(1);

            Observable.this.subscribe(new Observer<T>() {
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

                    innerObservable.subscribe(new Observer<R>() {
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
     * Schedules the subscription and source execution on the given scheduler.
     *
     * <p>In this educational implementation, the entire upstream subscription
     * is started inside the scheduler task.
     *
     * @param scheduler the scheduler used to run the subscription
     * @return a new observable that subscribes on the given scheduler
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler must not be null");

        return Observable.create(emitter ->
                scheduler.execute(() -> {
                    if (emitter.isDisposed()) {
                        return;
                    }

                    Observable.this.subscribe(new Observer<T>() {
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
     * Schedules downstream observer notifications on the given scheduler.
     *
     * <p>In this educational implementation, each signal is submitted
     * as a separate scheduler task.
     *
     * <p>For the most predictable event order, prefer using
     * {@code SingleThreadScheduler} with this operator.
     *
     * @param scheduler the scheduler used to deliver observer events
     * @return a new observable that observes on the given scheduler
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler must not be null");

        return Observable.create(emitter ->
                Observable.this.subscribe(new Observer<T>() {
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